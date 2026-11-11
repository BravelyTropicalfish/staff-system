package com.example.staffing.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.staffing.support.ApiSession;
import com.example.staffing.support.WorkbookFixture;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.test.annotation.DirtiesContext;

/**
 * T28, the idempotency NFR. Three writes can be asked twice: one workbook file, one confirmation,
 * one release. Each is either repeated in time or repeated concurrently, and the check is always
 * the same — the second attempt changes no state, so a row count says whether the rule held.
 *
 * <p>Each test starts from a dropped and rebuilt schema; the context is discarded after the class
 * so no other class inherits this database.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class IdempotencyApiTest {

  private static final String ATLAS = "Atlas";
  private static final String BOREALIS = "Borealis";

  @Autowired private TestRestTemplate rest;

  @Autowired private JdbcTemplate jdbc;

  private ApiSession coordinator;
  private ApiSession leadOne;
  private ApiSession leadTwo;

  @BeforeEach
  void freshSchema() throws Exception {
    try (var connection = jdbc.getDataSource().getConnection()) {
      connection.createStatement().execute("DROP ALL OBJECTS");
      ScriptUtils.executeSqlScript(connection, new ClassPathResource("schema.sql"));
      ScriptUtils.executeSqlScript(connection, new ClassPathResource("data.sql"));
    }
    coordinator = ApiSession.signIn(rest, "coordinator");
    leadOne = ApiSession.signIn(rest, "lead.one");
    leadTwo = ApiSession.signIn(rest, "lead.two");
  }

  @Test
  void anIdenticalReuploadAddsABatchAndLeavesEveryRowAsItWas() {
    givenPerson("E100", "DEV", "Senior");
    givenBand("E100", "测试", "Novice");
    byte[] file =
        WorkbookFixture.workbook()
            .person(person("E100", "DEV", "Senior", "full", "active", "测试:Expert"))
            .xlsx();

    ResponseEntity<Map> first = upload(file);
    assertThat(body(first)).containsEntry("status", "APPLIED");
    Map<String, Object> personBefore = jdbc.queryForMap("SELECT * FROM person WHERE employee_no = 'E100'");

    ResponseEntity<Map> second = upload(file);

    assertThat(body(second)).containsEntry("status", "APPLIED");
    assertThat(body(second).get("id"))
        .as("a re-upload is a new batch, never a rewrite of the earlier one")
        .isNotEqualTo(body(first).get("id"));
    assertThat(count("SELECT COUNT(*) FROM import_batch")).isEqualTo(2);
    assertThat(count("SELECT COUNT(*) FROM person")).isEqualTo(1);
    assertThat(count("SELECT COUNT(*) FROM person_skill")).isEqualTo(1);
    assertThat(jdbc.queryForMap("SELECT * FROM person WHERE employee_no = 'E100'"))
        .as("the same file applied twice leaves the person row untouched")
        .isEqualTo(personBefore);
    assertThat(band("E100", "测试")).isEqualTo("Expert");
  }

  @Test
  void aConcurrentDoubleConfirmationLeavesOneAssignment() throws Exception {
    long atlas = qaPost(ATLAS, "lead.one", 1, "2027-05-01", "2027-07-31");
    long borealis = qaPost(BOREALIS, "lead.two", 1, "2027-06-01", "2027-08-31");
    givenPerson("E100", "QA", "Senior");
    givenBand("E100", "测试", "Practitioner");
    long onAtlas = givenProposal(atlas, "E100");
    long onBorealis = givenProposal(borealis, "E100");

    ExecutorService pool = Executors.newFixedThreadPool(2);
    try {
      Future<ResponseEntity<Map>> first =
          pool.submit(
              () -> leadOne.post("/api/recommendations/" + onAtlas + "/confirm", null, Map.class));
      Future<ResponseEntity<Map>> second =
          pool.submit(
              () -> leadTwo.post("/api/recommendations/" + onBorealis + "/confirm", null, Map.class));
      List<ResponseEntity<Map>> answers = List.of(first.get(), second.get());

      assertThat(answers)
          .as("one confirmation wins and the other is refused, whatever the interleaving")
          .filteredOn(answer -> answer.getStatusCode() == HttpStatus.OK)
          .hasSize(1);
      ResponseEntity<Map> loser =
          answers.stream()
              .filter(answer -> answer.getStatusCode() == HttpStatus.CONFLICT)
              .findFirst()
              .orElseThrow();
      assertThat(body(loser)).containsEntry("code", "person taken");
    } finally {
      pool.shutdownNow();
    }

    assertThat(count("SELECT COUNT(*) FROM assignment WHERE person_no = 'E100'")).isEqualTo(1);
    assertThat(count("SELECT COUNT(*) FROM allocation WHERE person_no = 'E100'")).isEqualTo(1);
    assertThat(count("SELECT COUNT(*) FROM recommendation WHERE state = 'confirmed'")).isEqualTo(1);
    assertThat(count("SELECT COUNT(*) FROM recommendation WHERE state = 'voided'")).isEqualTo(1);
    assertThat(count("SELECT COUNT(*) FROM notice")).isEqualTo(1);
  }

  @Test
  void aSecondReleaseIsRefusedAndChangesNothing() {
    long post = qaPost(ATLAS, "lead.one", 2, "2027-05-01", "2027-07-31");
    givenPerson("E100", "QA", "Senior");
    givenBand("E100", "测试", "Practitioner");
    long proposal = givenProposal(post, "E100");
    ResponseEntity<Map> confirmed =
        leadOne.post("/api/recommendations/" + proposal + "/confirm", null, Map.class);
    assertThat(confirmed.getStatusCode()).isEqualTo(HttpStatus.OK);
    long assignmentId = ((Number) body(confirmed).get("assignmentId")).longValue();

    ResponseEntity<Map> first = release(assignmentId);
    assertThat(first.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(body(first)).containsEntry("postStatus", "open").containsEntry("remaining", 2);

    ResponseEntity<Map> second = release(assignmentId);

    assertThat(second.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(count("SELECT COUNT(*) FROM assignment")).isZero();
    assertThat(count("SELECT COUNT(*) FROM allocation")).isZero();
    assertThat(
            jdbc.queryForObject("SELECT status FROM post WHERE id = ?", String.class, post))
        .as("a refused release never rewinds the status")
        .isEqualTo("open");
    assertThat(
            jdbc.queryForObject(
                "SELECT state FROM recommendation WHERE id = ?", String.class, proposal))
        .as("a release takes the seat, it does not move the proposal back")
        .isEqualTo("confirmed");
  }

  // --- fixtures -------------------------------------------------------------------------------

  private ResponseEntity<Map> upload(byte[] file) {
    return coordinator.upload("/api/imports", "people.xlsx", file, Map.class);
  }

  private ResponseEntity<Map> release(long assignmentId) {
    return leadOne.post("/api/assignments/" + assignmentId + "/release", null, Map.class);
  }

  private long qaPost(String project, String lead, int headcount, String start, String end) {
    jdbc.update(
        "INSERT INTO project (name, lead_user_id, start_date, end_date, status)"
            + " VALUES (?, (SELECT id FROM app_user WHERE username = ?),"
            + " DATE '2027-01-01', DATE '2027-12-31', 'OPEN')",
        project,
        lead);
    long projectId = jdbc.queryForObject("SELECT id FROM project WHERE name = ?", Long.class, project);
    jdbc.update(
        "INSERT INTO post (project_id, title, role_id, min_job_level, headcount,"
            + " start_date, end_date, travel_required, status)"
            + " VALUES (?, NULL, (SELECT id FROM role WHERE name = 'QA'), 'Senior', ?, ?, ?,"
            + " 'full', 'open')",
        projectId,
        headcount,
        Date.valueOf(LocalDate.parse(start)),
        Date.valueOf(LocalDate.parse(end)));
    long post = jdbc.queryForObject("SELECT MAX(id) FROM post", Long.class);
    jdbc.update(
        "INSERT INTO post_skill (post_id, skill_id, kind, min_band)"
            + " VALUES (?, (SELECT id FROM skill WHERE name = '测试'), 'REQUIRED', 'Practitioner')",
        post);
    return post;
  }

  private void givenPerson(String employeeNo, String role, String level) {
    jdbc.update(
        "INSERT INTO person (employee_no, name, email, role_id, job_level, travel, active)"
            + " VALUES (?, ?, NULL, (SELECT id FROM role WHERE name = ?), ?, 'full', TRUE)",
        employeeNo,
        employeeNo,
        role,
        level);
  }

  private void givenBand(String employeeNo, String skill, String band) {
    jdbc.update(
        "INSERT INTO person_skill (employee_no, skill_id, band)"
            + " VALUES (?, (SELECT id FROM skill WHERE name = ?), ?)",
        employeeNo,
        skill,
        band);
  }

  private long givenProposal(long postId, String personNo) {
    ResponseEntity<Map> response =
        coordinator.post(
            "/api/posts/" + postId + "/recommendations", Map.of("personNo", personNo), Map.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    return ((Number) body(response).get("id")).longValue();
  }

  private String band(String employeeNo, String skill) {
    List<String> bands =
        jdbc.queryForList(
            "SELECT ps.band FROM person_skill ps JOIN skill s ON s.id = ps.skill_id"
                + " WHERE ps.employee_no = ? AND s.name = ?",
            String.class,
            employeeNo,
            skill);
    return bands.isEmpty() ? null : bands.get(0);
  }

  private int count(String sql) {
    Integer value = jdbc.queryForObject(sql, Integer.class);
    return value == null ? 0 : value;
  }

  private static String[] person(
      String employeeNo, String role, String level, String travel, String active) {
    return person(employeeNo, role, level, travel, active, "");
  }

  private static String[] person(
      String employeeNo, String role, String level, String travel, String active, String skills) {
    return new String[] {
      employeeNo, employeeNo, employeeNo.toLowerCase() + "@example.com", role, level, travel,
      active, skills
    };
  }

  @SuppressWarnings("unchecked")
  private static Map<String, Object> body(ResponseEntity<Map> response) {
    return (Map<String, Object>) response.getBody();
  }
}
