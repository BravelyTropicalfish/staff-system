package com.example.staffing.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.staffing.support.ApiSession;
import com.fasterxml.jackson.databind.ObjectMapper;
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
 * T22, the proposal: a recommendation is stored `proposed` with the reason snapshot taken then, a
 * filled or cancelled post refuses one, a person admitted through the fallback carries its evidence
 * into the snapshot, and one person may wait on two posts at once without being reserved.
 *
 * <p>Each test starts from a dropped and rebuilt schema; the context is discarded after the class
 * so no other class inherits this database.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class RecommendationApiTest {

  private static final String POST_START = "2027-05-01";
  private static final String POST_END = "2027-07-31";

  @Autowired private TestRestTemplate rest;

  @Autowired private JdbcTemplate jdbc;

  private ApiSession coordinator;

  @BeforeEach
  void freshSchema() throws Exception {
    try (var connection = jdbc.getDataSource().getConnection()) {
      connection.createStatement().execute("DROP ALL OBJECTS");
      ScriptUtils.executeSqlScript(connection, new ClassPathResource("schema.sql"));
      ScriptUtils.executeSqlScript(connection, new ClassPathResource("data.sql"));
    }
    coordinator = ApiSession.signIn(rest, "coordinator");
  }

  @Test
  @SuppressWarnings("unchecked")
  void aRecommendationCarriesItsSnapshot() throws Exception {
    long post = qaPost("Senior");
    givenPerson("E100", "Ada", "QA", "Senior", "full", true);
    givenBand("E100", "测试", "Practitioner");

    ResponseEntity<Map> response = recommend(post, "E100");

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    Map<String, Object> answer = body(response);
    assertThat(answer)
        .containsEntry("state", "proposed")
        .containsEntry("personNo", "E100")
        .containsEntry("proposedBy", "coordinator");
    assertThat(((Number) answer.get("postId")).longValue()).isEqualTo(post);
    assertThat((String) answer.get("proposedAt")).isNotBlank();

    Map<String, Object> snapshot = (Map<String, Object>) answer.get("snapshot");
    assertThat(snapshot)
        .containsEntry("personNo", "E100")
        .containsEntry("role", "QA")
        .containsEntry("jobLevel", "Senior")
        .containsEntry("travel", "full");
    assertThat((List<String>) snapshot.get("gates"))
        .containsExactly("G1", "G2", "G3", "G4", "G5", "G6");
    assertThat(snapshot.get("crossRole")).isNull();
    assertThat((List<Map<String, Object>>) snapshot.get("skills"))
        .singleElement()
        .satisfies(
            row -> assertThat(row)
                .containsEntry("skill", "测试")
                .containsEntry("kind", "required")
                .containsEntry("band", "Practitioner")
                .containsEntry("target", null));

    String stored =
        jdbc.queryForObject(
            "SELECT snapshot_json FROM recommendation WHERE post_id = ?", String.class, post);
    assertThat(new ObjectMapper().readValue(stored, Map.class))
        .as("the answer echoes the snapshot the database holds")
        .isEqualTo(snapshot);
    assertThat(
            jdbc.queryForObject(
                "SELECT state FROM recommendation WHERE post_id = ?", String.class, post))
        .isEqualTo("proposed");
    assertThat(count("SELECT COUNT(*) FROM assignment WHERE person_no = 'E100'")).isZero();
  }

  @Test
  void aFilledPostAcceptsNoRecommendation() {
    long post = qaPost("Senior");
    givenPerson("E100", "Ada", "QA", "Senior", "full", true);
    givenBand("E100", "测试", "Practitioner");
    givenPerson("E200", "Bob", "QA", "Senior", "full", true);
    givenBand("E200", "测试", "Practitioner");
    givenConfirmed(post, "E100");

    ResponseEntity<Map> response = recommend(post, "E200");

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(body(response)).containsEntry("code", "post filled");
    assertThat(count("SELECT COUNT(*) FROM recommendation")).isZero();
  }

  @Test
  void aCancelledPostAcceptsNoRecommendation() {
    long post = qaPost("Senior");
    givenPerson("E100", "Ada", "QA", "Senior", "full", true);
    givenBand("E100", "测试", "Practitioner");
    jdbc.update("UPDATE post SET status = 'cancelled' WHERE id = ?", post);

    ResponseEntity<Map> response = recommend(post, "E100");

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(body(response)).containsEntry("code", "post cancelled");
    assertThat(count("SELECT COUNT(*) FROM recommendation")).isZero();
  }

  @Test
  void aPostThatDoesNotExistTakesNoRecommendation() {
    ResponseEntity<Map> response =
        coordinator.post(
            "/api/posts/9999/recommendations", Map.of("personNo", "E100"), Map.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(body(response)).containsEntry("code", "not found");
    assertThat(count("SELECT COUNT(*) FROM recommendation")).isZero();
  }

  @Test
  void aRejectionNeedsNoBody() {
    long post = qaPost("Senior");
    givenPerson("E100", "Ada", "QA", "Senior", "full", true);
    givenBand("E100", "测试", "Practitioner");
    long id = idOf(recommend(post, "E100"));
    ApiSession lead = ApiSession.signIn(rest, "lead.one");

    ResponseEntity<Map> response =
        lead.post("/api/recommendations/" + id + "/reject", null, Map.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(body(response))
        .as("a rejection without a note still closes the proposal")
        .containsEntry("state", "rejected");
    assertThat(
            jdbc.queryForObject(
                "SELECT state FROM recommendation WHERE id = ?", String.class, id))
        .isEqualTo("rejected");
    assertThat(
            jdbc.queryForObject(
                "SELECT note FROM recommendation WHERE id = ?", String.class, id))
        .as("the absent body leaves no note behind")
        .isNull();
  }

  @Test
  @SuppressWarnings("unchecked")
  void aCrossRoleRecommendationCarriesItsEvidence() {
    long post = qaPost("Senior");
    givenPerson("E100", "Ada", "QA", "Junior", "full", true);
    givenPerson("E900", "Grace", "DEV", "Lead", "full", true);
    givenBand("E900", "测试", "Practitioner");

    ResponseEntity<Map> response = recommend(post, "E900");

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    Map<String, Object> snapshot = (Map<String, Object>) body(response).get("snapshot");
    assertThat(snapshot).containsEntry("role", "DEV").containsEntry("jobLevel", "Lead");
    assertThat((List<String>) snapshot.get("gates"))
        .containsExactly("G1", "G5", "G6", "X1", "X2");
    assertThat((List<?>) snapshot.get("skills")).isEmpty();
    assertThat((Map<String, Object>) snapshot.get("crossRole"))
        .containsEntry("bandsAbove", 1)
        .containsEntry("skill", "测试")
        .containsEntry("band", "Practitioner");

    assertThat(
            jdbc.queryForObject(
                "SELECT r.name FROM person p JOIN role r ON r.id = p.role_id"
                    + " WHERE p.employee_no = 'E900'",
                String.class))
        .as("the recommendation never moves the person into the post's role")
        .isEqualTo("DEV");
  }

  @Test
  void twoProposalsForOnePerson() {
    long atlas = givenProject("Atlas", "lead.one");
    long borealis = givenProject("Borealis", "lead.two");
    long first = qaPost(atlas);
    long second = qaPost(borealis);
    givenPerson("E100", "Ada", "QA", "Senior", "full", true);
    givenBand("E100", "测试", "Practitioner");

    assertThat(recommend(first, "E100").getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(recommend(second, "E100").getStatusCode()).isEqualTo(HttpStatus.CREATED);

    assertThat(
            count(
                "SELECT COUNT(*) FROM recommendation"
                    + " WHERE person_no = 'E100' AND state = 'proposed'"))
        .isEqualTo(2);
    assertThat(count("SELECT COUNT(*) FROM assignment WHERE person_no = 'E100'"))
        .as("a proposal reserves nobody")
        .isZero();
    assertThat(count("SELECT COUNT(*) FROM allocation WHERE person_no = 'E100'")).isZero();
  }

  @Test
  void theSamePersonTwiceOnOnePostIsRefused() {
    long post = qaPost("Senior");
    givenPerson("E100", "Ada", "QA", "Senior", "full", true);
    givenBand("E100", "测试", "Practitioner");

    assertThat(recommend(post, "E100").getStatusCode()).isEqualTo(HttpStatus.CREATED);

    ResponseEntity<Map> again = recommend(post, "E100");
    assertThat(again.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(body(again)).containsEntry("code", "already proposed");
    assertThat(count("SELECT COUNT(*) FROM recommendation")).isEqualTo(1);
  }

  @Test
  void aPersonWhoNoLongerPassesIsRefused() {
    long post = qaPost("Senior");
    givenPerson("E100", "Ada", "QA", "Senior", "full", true);
    givenBand("E100", "测试", "Practitioner");
    jdbc.update("UPDATE person SET active = FALSE WHERE employee_no = 'E100'");

    ResponseEntity<Map> response = recommend(post, "E100");

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(body(response))
        .containsEntry("code", "G1")
        .containsEntry("message", "the person is not active");
    assertThat(count("SELECT COUNT(*) FROM recommendation")).isZero();
  }

  @Test
  void aPersonOutsideTheListsIsRefused() {
    long post = qaPost("Senior");
    givenPerson("E100", "Ada", "QA", "Senior", "full", true);
    givenBand("E100", "测试", "Practitioner");
    givenPerson("E900", "Grace", "DEV", "Lead", "full", true);
    givenBand("E900", "测试", "Expert");

    ResponseEntity<Map> closed = recommend(post, "E900");
    assertThat(closed.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(body(closed)).containsEntry("code", "not a candidate");

    ResponseEntity<Map> unknown = recommend(post, "E999");
    assertThat(unknown.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(body(unknown)).containsEntry("code", "not found");
    assertThat(count("SELECT COUNT(*) FROM recommendation")).isZero();
  }

  @Test
  void aLeadCannotRecommend() {
    long post = qaPost("Senior");
    givenPerson("E100", "Ada", "QA", "Senior", "full", true);
    givenBand("E100", "测试", "Practitioner");
    ApiSession lead = ApiSession.signIn(rest, "lead.one");

    ResponseEntity<Map> response =
        lead.post(
            "/api/posts/" + post + "/recommendations", Map.of("personNo", "E100"), Map.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    assertThat(count("SELECT COUNT(*) FROM recommendation")).isZero();
  }

  @Test
  @SuppressWarnings("unchecked")
  void theCoordinatorReadsEveryProposalWithItsSnapshot() {
    long post = qaPost("Senior");
    givenPerson("E100", "Ada", "QA", "Senior", "full", true);
    givenBand("E100", "测试", "Practitioner");
    givenPerson("E200", "Bob", "QA", "Senior", "full", true);
    givenBand("E200", "测试", "Practitioner");
    long openId = idOf(recommend(post, "E100"));
    long closedId = idOf(recommend(post, "E200"));
    ApiSession lead = ApiSession.signIn(rest, "lead.one");
    assertThat(
            lead.post(
                    "/api/recommendations/" + closedId + "/reject",
                    Map.of("note", "not this one"),
                    Map.class)
                .getStatusCode())
        .isEqualTo(HttpStatus.OK);

    ResponseEntity<List> response =
        coordinator.get("/api/posts/" + post + "/recommendations", List.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    List<Map<String, Object>> rows = response.getBody();
    assertThat(rows).hasSize(2);

    Map<String, Object> open = rows.get(0);
    assertThat(open)
        .containsEntry("personNo", "E100")
        .containsEntry("personName", "Ada")
        .containsEntry("personActive", true)
        .containsEntry("state", "proposed")
        .containsEntry("proposedBy", "coordinator");
    assertThat(((Number) open.get("id")).longValue()).isEqualTo(openId);
    assertThat((String) open.get("proposedAt")).isNotBlank();
    assertThat(open.get("voidReason")).isNull();
    Map<String, Object> snapshot = (Map<String, Object>) open.get("snapshot");
    assertThat(snapshot).containsEntry("role", "QA").containsEntry("jobLevel", "Senior");
    assertThat((List<String>) snapshot.get("gates"))
        .containsExactly("G1", "G2", "G3", "G4", "G5", "G6");

    Map<String, Object> closed = rows.get(1);
    assertThat(closed)
        .as("the open proposal comes first, the closed history follows")
        .containsEntry("personNo", "E200")
        .containsEntry("state", "rejected");
    assertThat(closed.get("snapshot"))
        .as("the coordinator still reads the evidence of a closed proposal")
        .isNotNull();

    ResponseEntity<Map> unknown =
        coordinator.get("/api/posts/99999/recommendations", Map.class);
    assertThat(unknown.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
  }

  @Test
  @SuppressWarnings("unchecked")
  void theLeadReadsTheProposalWithoutItsSnapshot() {
    long post = qaPost("Senior");
    givenPerson("E100", "Ada", "QA", "Senior", "full", true);
    givenBand("E100", "测试", "Practitioner");
    long id = idOf(recommend(post, "E100"));

    ApiSession lead = ApiSession.signIn(rest, "lead.one");
    ResponseEntity<List> response =
        lead.get("/api/posts/" + post + "/recommendations", List.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    List<Map<String, Object>> rows = response.getBody();
    assertThat(rows).singleElement().satisfies(row -> {
      assertThat(row)
          .containsEntry("personNo", "E100")
          .containsEntry("personName", "Ada")
          .containsEntry("personActive", true)
          .containsEntry("state", "proposed")
          .containsEntry("proposedBy", "coordinator");
      assertThat(((Number) row.get("id")).longValue()).isEqualTo(id);
    });
    assertThat(rows.get(0).get("snapshot"))
        .as("the reason snapshot is the coordinator's alone")
        .isNull();

    ApiSession other = ApiSession.signIn(rest, "lead.two");
    ResponseEntity<Map> refused = other.get("/api/posts/" + post + "/recommendations", Map.class);
    assertThat(refused.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    assertThat(body(refused)).containsEntry("code", "forbidden");
  }

  /** A QA post needing 测试 at `Practitioner`, which the seed puts in the QA domain. */
  private long qaPost(String level) {
    return qaPost(givenProject("Atlas", "lead.one"), level);
  }

  private long qaPost(long projectId, String level) {
    long post = givenPost(projectId, "QA", level);
    givenSkill(post, "测试", "REQUIRED", "Practitioner");
    return post;
  }

  private long qaPost(long projectId) {
    return qaPost(projectId, "Senior");
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

  private long givenPost(long projectId, String role, String level) {
    jdbc.update(
        "INSERT INTO post (project_id, title, role_id, min_job_level, headcount,"
            + " start_date, end_date, travel_required, status)"
            + " VALUES (?, NULL, (SELECT id FROM role WHERE name = ?), ?, 1, ?, ?, 'full', 'open')",
        projectId,
        role,
        level,
        Date.valueOf(LocalDate.parse(POST_START)),
        Date.valueOf(LocalDate.parse(POST_END)));
    return jdbc.queryForObject(
        "SELECT MAX(id) FROM post WHERE project_id = ?", Long.class, projectId);
  }

  private void givenSkill(long postId, String skill, String kind, String band) {
    jdbc.update(
        "INSERT INTO post_skill (post_id, skill_id, kind, min_band, target_band)"
            + " VALUES (?, (SELECT id FROM skill WHERE name = ?), ?, ?, ?)",
        postId,
        skill,
        kind,
        "REQUIRED".equals(kind) ? band : null,
        "BONUS".equals(kind) ? band : null);
  }

  private void givenPerson(
      String employeeNo, String name, String role, String level, String travel, boolean active) {
    jdbc.update(
        "INSERT INTO person (employee_no, name, email, role_id, job_level, travel, active)"
            + " VALUES (?, ?, NULL, (SELECT id FROM role WHERE name = ?), ?, ?, ?)",
        employeeNo,
        name,
        role,
        level,
        travel,
        active);
  }

  private void givenBand(String employeeNo, String skill, String band) {
    jdbc.update(
        "INSERT INTO person_skill (employee_no, skill_id, band)"
            + " VALUES (?, (SELECT id FROM skill WHERE name = ?), ?)",
        employeeNo,
        skill,
        band);
  }

  /** A confirmed seat: the assignment plus the allocation it backs, both from one confirmation. */
  private void givenConfirmed(long postId, String employeeNo) {
    jdbc.update("INSERT INTO assignment (post_id, person_no) VALUES (?, ?)", postId, employeeNo);
    long assignment =
        jdbc.queryForObject(
            "SELECT id FROM assignment WHERE post_id = ? AND person_no = ?",
            Long.class,
            postId,
            employeeNo);
    jdbc.update(
        "INSERT INTO allocation (person_no, project_id, start_date, end_date, source, assignment_id)"
            + " SELECT ?, project_id, ?, ?, 'CONFIRMATION', ? FROM post WHERE id = ?",
        employeeNo,
        Date.valueOf(LocalDate.parse(POST_START)),
        Date.valueOf(LocalDate.parse(POST_END)),
        assignment,
        postId);
  }

  private ResponseEntity<Map> recommend(long postId, String personNo) {
    return coordinator.post(
        "/api/posts/" + postId + "/recommendations", Map.of("personNo", personNo), Map.class);
  }

  private int count(String sql) {
    return jdbc.queryForObject(sql, Integer.class);
  }

  @SuppressWarnings("unchecked")
  private static Map<String, Object> body(ResponseEntity<Map> response) {
    return (Map<String, Object>) response.getBody();
  }

  private static long idOf(ResponseEntity<Map> response) {
    return ((Number) body(response).get("id")).longValue();
  }
}
