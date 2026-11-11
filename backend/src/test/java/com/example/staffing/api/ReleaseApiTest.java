package com.example.staffing.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.staffing.support.ApiSession;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
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
 * T24, release: the assignment and its allocation are deleted, the post status is recomputed from
 * the remaining seats and the post returns to the short list, and with the allocation gone the
 * person passes G6 again. The coordinator releases anywhere, a lead only on their own project's
 * posts, and a second release finds no seat and changes nothing.
 *
 * <p>Each test starts from a dropped and rebuilt schema; the context is discarded after the class
 * so no other class inherits this database.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class ReleaseApiTest {

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
  @SuppressWarnings("unchecked")
  void releaseReturnsThePostToTheShortList() {
    long post = qaPost(ATLAS, 1, "2027-05-01", "2027-07-31");
    givenPerson("E100");
    givenBand("E100");
    long assignment = confirmed(givenProposal(post, "E100"));
    long project = projectOf(post);
    assertThat(gapRow(project, post))
        .containsEntry("status", "filled")
        .containsEntry("remaining", 0)
        .containsEntry("stillShort", false);

    ResponseEntity<Map> response =
        coordinator.post("/api/assignments/" + assignment + "/release", null, Map.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(body(response))
        .containsEntry("postId", (int) post)
        .containsEntry("personNo", "E100")
        .containsEntry("postStatus", "open")
        .containsEntry("remaining", 1);
    assertThat(count("SELECT COUNT(*) FROM assignment WHERE id = " + assignment)).isZero();
    assertThat(count("SELECT COUNT(*) FROM allocation WHERE assignment_id = " + assignment))
        .isZero();
    assertThat(gapRow(project, post))
        .containsEntry("status", "open")
        .containsEntry("remaining", 1)
        .containsEntry("stillShort", true);

    long later = qaPost(BOREALIS, 1, "2027-09-01", "2027-10-31");
    assertThat((List<Map<String, Object>>) search(later).get("suitable"))
        .singleElement()
        .satisfies(row -> assertThat(row).containsEntry("employeeNo", "E100"));
  }

  @Test
  void releasingOneOfTwoSeatsKeepsTheOther() {
    long post = qaPost(ATLAS, 2, "2027-05-01", "2027-07-31");
    givenPerson("E100");
    givenBand("E100");
    givenPerson("E200");
    givenBand("E200");
    long first = confirmed(givenProposal(post, "E100"));
    confirmed(givenProposal(post, "E200"));
    long project = projectOf(post);
    assertThat(gapRow(project, post)).containsEntry("status", "filled");

    ResponseEntity<Map> response =
        leadOne.post("/api/assignments/" + first + "/release", null, Map.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(body(response))
        .containsEntry("personNo", "E100")
        .containsEntry("postStatus", "partially filled")
        .containsEntry("remaining", 1);
    assertThat(count("SELECT COUNT(*) FROM assignment WHERE post_id = " + post)).isEqualTo(1);
    assertThat(count("SELECT COUNT(*) FROM allocation WHERE source = 'CONFIRMATION'")).isEqualTo(1);
    assertThat((List<Map<String, Object>>) gapRow(project, post).get("people"))
        .singleElement()
        .satisfies(row -> assertThat(row).containsEntry("personNo", "E200"));
  }

  @Test
  @SuppressWarnings("unchecked")
  void onlyTheOwningLeadOrTheCoordinatorReleases() {
    long post = qaPost(ATLAS, 1, "2027-05-01", "2027-07-31");
    givenPerson("E100");
    givenBand("E100");
    long assignment = confirmed(givenProposal(post, "E100"));

    ResponseEntity<Map> refusals =
        leadTwo.post("/api/assignments/" + assignment + "/release", null, Map.class);

    assertThat(refusals.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    assertThat(body(refusals)).containsEntry("code", "forbidden");
    assertThat(count("SELECT COUNT(*) FROM assignment WHERE id = " + assignment)).isEqualTo(1);
    assertThat(count("SELECT COUNT(*) FROM allocation WHERE assignment_id = " + assignment))
        .isEqualTo(1);

    assertThat(leadOne.post("/api/assignments/" + assignment + "/release", null, Map.class)
            .getStatusCode())
        .isEqualTo(HttpStatus.OK);
  }

  @Test
  void aSecondReleaseIsRefusedAndChangesNothing() {
    long post = qaPost(ATLAS, 1, "2027-05-01", "2027-07-31");
    givenPerson("E100");
    givenBand("E100");
    long assignment = confirmed(givenProposal(post, "E100"));
    long project = projectOf(post);
    assertThat(coordinator.post("/api/assignments/" + assignment + "/release", null, Map.class)
            .getStatusCode())
        .isEqualTo(HttpStatus.OK);

    ResponseEntity<Map> again =
        coordinator.post("/api/assignments/" + assignment + "/release", null, Map.class);

    assertThat(again.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(body(again)).containsEntry("code", "not found");
    assertThat(gapRow(project, post))
        .containsEntry("status", "open")
        .containsEntry("remaining", 1);
    assertThat(coordinator.post("/api/assignments/9999/release", null, Map.class).getStatusCode())
        .isEqualTo(HttpStatus.NOT_FOUND);
  }

  private long qaPost(String projectName, int headcount, String start, String end) {
    jdbc.update(
        "INSERT INTO project (name, lead_user_id, start_date, end_date, status)"
            + " VALUES (?, (SELECT id FROM app_user WHERE username = ?),"
            + " DATE '2027-01-01', DATE '2027-12-31', 'OPEN')",
        projectName,
        leadOf(projectName));
    long project =
        jdbc.queryForObject("SELECT id FROM project WHERE name = ?", Long.class, projectName);
    jdbc.update(
        "INSERT INTO post (project_id, title, role_id, min_job_level, headcount,"
            + " start_date, end_date, travel_required, status)"
            + " VALUES (?, NULL, (SELECT id FROM role WHERE name = 'QA'), 'Senior', ?, ?, ?,"
            + " 'full', 'open')",
        project,
        headcount,
        Date.valueOf(LocalDate.parse(start)),
        Date.valueOf(LocalDate.parse(end)));
    return jdbc.queryForObject("SELECT MAX(id) FROM post WHERE project_id = ?", Long.class, project);
  }

  private static String leadOf(String projectName) {
    return ATLAS.equals(projectName) ? "lead.one" : "lead.two";
  }

  private long projectOf(long postId) {
    return jdbc.queryForObject("SELECT project_id FROM post WHERE id = ?", Long.class, postId);
  }

  private void givenPerson(String employeeNo) {
    jdbc.update(
        "INSERT INTO person (employee_no, name, email, role_id, job_level, travel, active)"
            + " VALUES (?, ?, NULL, (SELECT id FROM role WHERE name = 'QA'), 'Senior', 'full', TRUE)",
        employeeNo,
        employeeNo);
  }

  private void givenBand(String employeeNo) {
    jdbc.update(
        "INSERT INTO person_skill (employee_no, skill_id, band)"
            + " VALUES (?, (SELECT id FROM skill WHERE name = '测试'), 'Practitioner')",
        employeeNo);
  }

  private long givenProposal(long postId, String personNo) {
    ResponseEntity<Map> response =
        coordinator.post(
            "/api/posts/" + postId + "/recommendations", Map.of("personNo", personNo), Map.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    return ((Number) body(response).get("id")).longValue();
  }

  private long confirmed(long recommendationId) {
    ResponseEntity<Map> response =
        leadOne.post("/api/recommendations/" + recommendationId + "/confirm", null, Map.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    return ((Number) body(response).get("assignmentId")).longValue();
  }

  private Map<String, Object> search(long postId) {
    ResponseEntity<Map> response =
        coordinator.get("/api/posts/" + postId + "/candidates", Map.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    return body(response);
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> gapRow(long projectId, long postId) {
    ResponseEntity<List> response = coordinator.get("/api/projects/" + projectId + "/gap", List.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    List<Map<String, Object>> rows = (List<Map<String, Object>>) response.getBody();
    return rows.stream()
        .filter(row -> ((Number) row.get("id")).longValue() == postId)
        .findFirst()
        .orElseThrow();
  }

  private int count(String sql) {
    Integer value = jdbc.queryForObject(sql, Integer.class);
    return value == null ? 0 : value;
  }

  @SuppressWarnings("unchecked")
  private static Map<String, Object> body(ResponseEntity<Map> response) {
    return (Map<String, Object>) response.getBody();
  }
}
