package com.example.staffing.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.staffing.support.ApiSession;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
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
 * T24, the notice list: every void leaves one entry naming the post, the person, the reason and the
 * time, the coordinator reads the list and nobody else does, and an entry leaves only when it is
 * dismissed — ninety days later it is still there with its original reason and time.
 *
 * <p>Each test starts from a dropped and rebuilt schema; the context is discarded after the class
 * so no other class inherits this database.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class NoticeApiTest {

  private static final String ATLAS = "Atlas";
  private static final String BOREALIS = "Borealis";

  @Autowired private TestRestTemplate rest;

  @Autowired private JdbcTemplate jdbc;

  private ApiSession coordinator;
  private ApiSession leadOne;

  @BeforeEach
  void freshSchema() throws Exception {
    try (var connection = jdbc.getDataSource().getConnection()) {
      connection.createStatement().execute("DROP ALL OBJECTS");
      ScriptUtils.executeSqlScript(connection, new ClassPathResource("schema.sql"));
      ScriptUtils.executeSqlScript(connection, new ClassPathResource("data.sql"));
    }
    coordinator = ApiSession.signIn(rest, "coordinator");
    leadOne = ApiSession.signIn(rest, "lead.one");
  }

  @Test
  void theNoticeListExplainsEachLoss() {
    long atlas = givenProject(ATLAS, "lead.one");
    long borealis = givenProject(BOREALIS, "lead.two");
    givenPerson("E100");
    givenPerson("E200");
    givenPerson("E300");

    long first = givenPost(atlas, "QA analyst", "2027-05-01", "2027-07-31");
    long lost = givenPost(borealis, "QA partner", "2027-06-01", "2027-08-31");
    long onFirst = givenProposal(first, "E100");
    givenProposal(lost, "E100");
    assertThat(leadOne.post("/api/recommendations/" + onFirst + "/confirm", null, Map.class)
            .getStatusCode())
        .isEqualTo(HttpStatus.OK);

    long edited = givenPost(atlas, "Backend developer", "2027-05-01", "2027-07-31");
    givenProposal(edited, "E200");
    assertThat(leadOne.patch("/api/posts/" + edited, Map.of("title", "Backend developer II"),
            Map.class)
            .getStatusCode())
        .isEqualTo(HttpStatus.OK);

    long cancelled = givenPost(atlas, "QA lead", "2027-09-01", "2027-10-31");
    givenProposal(cancelled, "E300");
    assertThat(leadOne.post("/api/posts/" + cancelled + "/cancel", null, Map.class)
            .getStatusCode())
        .isEqualTo(HttpStatus.OK);

    List<Map<String, Object>> rows = notices();

    assertThat(rows).hasSize(3);
    Map<String, Map<String, Object>> byReason =
        rows.stream()
            .collect(Collectors.toMap(row -> (String) row.get("reason"), row -> row));
    assertThat(byReason).containsOnlyKeys("person taken", "post changed", "post cancelled");
    assertThat(byReason.get("person taken"))
        .containsEntry("postId", (int) lost)
        .containsEntry("postTitle", "QA partner")
        .containsEntry("personNo", "E100")
        .containsEntry("personName", "E100");
    assertThat(byReason.get("post changed"))
        .containsEntry("postId", (int) edited)
        .containsEntry("postTitle", "Backend developer II")
        .containsEntry("personNo", "E200");
    assertThat(byReason.get("post cancelled"))
        .containsEntry("postId", (int) cancelled)
        .containsEntry("postTitle", "QA lead")
        .containsEntry("personNo", "E300");
    rows.forEach(
        row -> {
          assertThat((String) row.get("createdAt")).isNotBlank();
          assertThat(row.get("dismissedAt")).isNull();
        });
    assertThat(rows.get(0)).as("newest first").containsEntry("reason", "post cancelled");
  }

  @Test
  void anEntryStaysUntilItIsDismissed() {
    long atlas = givenProject(ATLAS, "lead.one");
    long borealis = givenProject(BOREALIS, "lead.two");
    givenPerson("E100");
    long first = givenPost(atlas, "QA analyst", "2027-05-01", "2027-07-31");
    long lost = givenPost(borealis, "QA partner", "2027-06-01", "2027-08-31");
    long onFirst = givenProposal(first, "E100");
    givenProposal(lost, "E100");
    assertThat(leadOne.post("/api/recommendations/" + onFirst + "/confirm", null, Map.class)
            .getStatusCode())
        .isEqualTo(HttpStatus.OK);
    long notice = noticeOn(lost);
    jdbc.update(
        "UPDATE notice SET created_at = DATEADD('DAY', -90, CURRENT_TIMESTAMP) WHERE id = ?",
        notice);
    String ninetyDaysAgo =
        jdbc.queryForObject("SELECT created_at FROM notice WHERE id = ?", Timestamp.class, notice)
            .toLocalDateTime()
            .toString();

    List<Map<String, Object>> rows = notices();

    assertThat(rows).singleElement()
        .satisfies(
            row -> {
              assertThat(row).containsEntry("reason", "person taken").containsEntry("id", (int) notice);
              assertThat(row.get("createdAt")).isEqualTo(ninetyDaysAgo);
              assertThat(row.get("dismissedAt")).isNull();
            });

    ResponseEntity<Map> dismissed =
        coordinator.post("/api/notices/" + notice + "/dismiss", null, Map.class);

    assertThat(dismissed.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat((String) body(dismissed).get("dismissedAt")).isNotBlank();
    assertThat(notices()).isEmpty();

    ResponseEntity<Map> again =
        coordinator.post("/api/notices/" + notice + "/dismiss", null, Map.class);
    assertThat(again.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(body(again)).containsEntry("code", "already dismissed");
    assertThat(coordinator.post("/api/notices/9999/dismiss", null, Map.class).getStatusCode())
        .isEqualTo(HttpStatus.NOT_FOUND);
  }

  @Test
  void onlyTheCoordinatorReadsTheList() {
    ResponseEntity<Map> read = leadOne.get("/api/notices", Map.class);
    assertThat(read.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    assertThat(body(read)).containsEntry("code", "forbidden");

    ResponseEntity<Map> dismiss = leadOne.post("/api/notices/1/dismiss", null, Map.class);
    assertThat(dismiss.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    assertThat(body(dismiss)).containsEntry("code", "forbidden");
  }

  @Test
  void aNoticeAddressedToAnotherCoordinatorIsOutOfReach() {
    jdbc.update(
        "INSERT INTO app_user (username, password_hash, role, display_name)"
            + " SELECT 'coordinator.two', password_hash, 'COORDINATOR', 'Coordinator two'"
            + " FROM app_user WHERE username = 'coordinator'");
    long project = givenProject("Atlas", "lead.one");
    long post = givenPost(project, "QA partner", "2027-06-01", "2027-07-31");
    givenPerson("E100");
    jdbc.update(
        "INSERT INTO notice (coordinator_id, person_no, post_id, reason)"
            + " VALUES ((SELECT id FROM app_user WHERE username = 'coordinator.two'),"
            + " 'E100', ?, 'post changed')",
        post);
    long notice = noticeOn(post);

    ResponseEntity<Map> refused =
        coordinator.post("/api/notices/" + notice + "/dismiss", null, Map.class);

    assertThat(refused.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    assertThat(body(refused)).containsEntry("code", "forbidden");
    assertThat(
            jdbc.queryForObject(
                "SELECT dismissed_at FROM notice WHERE id = ?", Timestamp.class, notice))
        .isNull();
  }

  private long givenProject(String name, String leadUsername) {
    jdbc.update(
        "INSERT INTO project (name, lead_user_id, start_date, end_date, status)"
            + " VALUES (?, (SELECT id FROM app_user WHERE username = ?),"
            + " DATE '2027-01-01', DATE '2027-12-31', 'OPEN')",
        name,
        leadUsername);
    return jdbc.queryForObject("SELECT id FROM project WHERE name = ?", Long.class, name);
  }

  private long givenPost(long projectId, String title, String start, String end) {
    jdbc.update(
        "INSERT INTO post (project_id, title, role_id, min_job_level, headcount,"
            + " start_date, end_date, travel_required, status)"
            + " VALUES (?, ?, (SELECT id FROM role WHERE name = 'QA'), 'Senior', 1, ?, ?,"
            + " 'full', 'open')",
        projectId,
        title,
        Date.valueOf(LocalDate.parse(start)),
        Date.valueOf(LocalDate.parse(end)));
    return jdbc.queryForObject(
        "SELECT MAX(id) FROM post WHERE project_id = ?", Long.class, projectId);
  }

  private void givenPerson(String employeeNo) {
    jdbc.update(
        "INSERT INTO person (employee_no, name, email, role_id, job_level, travel, active)"
            + " VALUES (?, ?, NULL, (SELECT id FROM role WHERE name = 'QA'), 'Senior', 'full', TRUE)",
        employeeNo,
        employeeNo);
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

  private long noticeOn(long postId) {
    return jdbc.queryForObject("SELECT id FROM notice WHERE post_id = ?", Long.class, postId);
  }

  @SuppressWarnings("unchecked")
  private List<Map<String, Object>> notices() {
    ResponseEntity<List> response = coordinator.get("/api/notices", List.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    return (List<Map<String, Object>>) response.getBody();
  }

  @SuppressWarnings("unchecked")
  private static Map<String, Object> body(ResponseEntity<Map> response) {
    return (Map<String, Object>) response.getBody();
  }
}
