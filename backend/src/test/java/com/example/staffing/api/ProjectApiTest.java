package com.example.staffing.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.staffing.support.ApiSession;
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
 * T13, projects: a lead creates a project of their own and the coordinator creates one for any lead,
 * a lead only ever sees its own (in the list and in the detail, which the project screen reads), and
 * ending is one terminal transaction — posts cancelled, pending recommendations voided with their
 * notice, allocations deleted, assignments released, the project closed for good.
 *
 * <p>Each test starts from a dropped and rebuilt schema; the context is discarded after the class
 * so no other class inherits this database.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class ProjectApiTest {

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
  void aLeadCreatesAProjectOfTheirOwnOnly() {
    ApiSession lead = ApiSession.signIn(rest, "lead.one");

    ResponseEntity<Map> own = lead.post("/api/projects", project("Atlas", "lead.one"), Map.class);
    ResponseEntity<Map> forAnotherLead =
        lead.post("/api/projects", project("Borealis", "lead.two"), Map.class);

    assertThat(own.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(body(own)).containsEntry("lead", "lead.one").containsEntry("status", "OPEN");
    assertThat(forAnotherLead.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    assertThat(body(forAnotherLead)).containsEntry("code", "forbidden");
    assertThat(count("SELECT COUNT(*) FROM project")).isEqualTo(1);
  }

  @Test
  void theCoordinatorCreatesAnOpenProject() {
    ResponseEntity<Map> response = coordinator.post("/api/projects", project("Atlas", "lead.one"), Map.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(body(response))
        .containsEntry("name", "Atlas")
        .containsEntry("lead", "lead.one")
        .containsEntry("leadName", "Project lead one")
        .containsEntry("status", "OPEN")
        .containsEntry("startDate", "2027-01-01")
        .containsEntry("endDate", "2027-12-31");
    assertThat(jdbc.queryForObject("SELECT status FROM project WHERE name = 'Atlas'", String.class))
        .isEqualTo("OPEN");
  }

  @Test
  void invertedDatesAreRefusedAndNothingIsStored() {
    Map<String, Object> body = project("Atlas", "lead.one");
    body.put("startDate", "2027-06-01");
    body.put("endDate", "2027-03-31");

    ResponseEntity<Map> response = coordinator.post("/api/projects", body, Map.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    assertThat(body(response))
        .containsEntry("code", "invalid project")
        .containsEntry("message", "the end date cannot precede the start date");
    assertThat(count("SELECT COUNT(*) FROM project")).isZero();
  }

  @Test
  void aProjectWhoseLeadIsNotALeadAccountIsRefused() {
    ResponseEntity<Map> unknown = coordinator.post("/api/projects", project("Atlas", "nobody"), Map.class);
    ResponseEntity<Map> wrongRole =
        coordinator.post("/api/projects", project("Borealis", "coordinator"), Map.class);

    assertThat(unknown.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    assertThat(wrongRole.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    assertThat(body(wrongRole).get("message").toString()).contains("not a lead account");
    assertThat(count("SELECT COUNT(*) FROM project")).isZero();
  }

  @Test
  void theListIsScopedToTheLead() {
    givenProject("Atlas", "lead.one");
    givenProject("Borealis", "lead.two");

    assertThat(names(coordinator.get("/api/projects", List.class))).containsExactly("Atlas", "Borealis");
    assertThat(names(ApiSession.signIn(rest, "lead.one").get("/api/projects", List.class)))
        .containsExactly("Atlas");
    assertThat(names(ApiSession.signIn(rest, "lead.two").get("/api/projects", List.class)))
        .containsExactly("Borealis");
  }

  @Test
  void theDetailAnswersTheCoordinatorAndTheOwningLeadOnly() {
    long atlas = givenProject("Atlas", "lead.one");

    ResponseEntity<Map> byCoordinator = coordinator.get("/api/projects/" + atlas, Map.class);
    ResponseEntity<Map> byOwnLead =
        ApiSession.signIn(rest, "lead.one").get("/api/projects/" + atlas, Map.class);
    ResponseEntity<Map> byOtherLead =
        ApiSession.signIn(rest, "lead.two").get("/api/projects/" + atlas, Map.class);

    assertThat(byCoordinator.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(body(byCoordinator))
        .containsEntry("name", "Atlas")
        .containsEntry("lead", "lead.one")
        .containsEntry("status", "OPEN")
        .containsEntry("startDate", "2027-01-01")
        .containsEntry("endDate", "2027-12-31");
    assertThat(byOwnLead.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(byOtherLead.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
  }

  @Test
  void theDetailOfAnUnknownProjectIsNotFound() {
    ResponseEntity<Map> response = coordinator.get("/api/projects/999", Map.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(body(response)).containsEntry("code", "not found");
  }

  @Test
  void endingReleasesEveryoneAndCancelsThePosts() {
    long atlas = givenProject("Atlas", "lead.one");
    long filledOne = givenPost(atlas, "DEV", "filled");
    long filledTwo = givenPost(atlas, "QA", "filled");
    long open = givenPost(atlas, "DEV", "open");
    givenPerson("E100", "DEV");
    givenPerson("E101", "QA");
    givenPerson("E102", "DEV");
    givenConfirmed(filledOne, "E100", atlas);
    givenConfirmed(filledTwo, "E101", atlas);
    long recommendation = givenPendingRecommendation(open, "E102");

    ResponseEntity<Map> response = ApiSession.signIn(rest, "lead.one").post("/api/projects/" + atlas + "/end", null, Map.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(body(response)).containsEntry("status", "CLOSED");
    assertThat(statuses(atlas)).containsOnly("cancelled");
    assertThat(jdbc.queryForObject("SELECT state FROM recommendation WHERE id = ?", String.class, recommendation))
        .isEqualTo("voided");
    assertThat(jdbc.queryForObject("SELECT void_reason FROM recommendation WHERE id = ?", String.class, recommendation))
        .isEqualTo("post cancelled");
    assertThat(count("SELECT COUNT(*) FROM notice WHERE post_id = " + open + " AND reason = 'post cancelled' AND person_no = 'E102'"))
        .isEqualTo(1);
    assertThat(count("SELECT COUNT(*) FROM allocation WHERE project_id = " + atlas)).isZero();
    assertThat(count("SELECT COUNT(*) FROM assignment")).isZero();
  }

  @Test
  void onlyTheProjectsLeadMayEndIt() {
    long atlas = givenProject("Atlas", "lead.one");
    long post = givenPost(atlas, "DEV", "open");

    ResponseEntity<Map> byCoordinator =
        coordinator.post("/api/projects/" + atlas + "/end", null, Map.class);
    ResponseEntity<Map> byOtherLead =
        ApiSession.signIn(rest, "lead.two").post("/api/projects/" + atlas + "/end", null, Map.class);

    assertThat(byCoordinator.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    assertThat(byOtherLead.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    assertThat(projectStatus(atlas)).isEqualTo("OPEN");
    assertThat(jdbc.queryForObject("SELECT status FROM post WHERE id = ?", String.class, post))
        .isEqualTo("open");
  }

  @Test
  void aClosedProjectCannotBeEndedAgainOrReopened() {
    long atlas = givenProject("Atlas", "lead.one");
    ApiSession lead = ApiSession.signIn(rest, "lead.one");
    assertThat(lead.post("/api/projects/" + atlas + "/end", null, Map.class).getStatusCode())
        .isEqualTo(HttpStatus.OK);

    ResponseEntity<Map> again = lead.post("/api/projects/" + atlas + "/end", null, Map.class);
    ResponseEntity<Map> reopen =
        lead.patch("/api/projects/" + atlas, Map.of("status", "OPEN"), Map.class);

    assertThat(again.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(body(again)).containsEntry("code", "project closed");
    // There is no reopen route at all, so the attempt has nowhere to land and nothing changes.
    assertThat(reopen.getStatusCode().is4xxClientError()).isTrue();
    assertThat(projectStatus(atlas)).isEqualTo("CLOSED");
  }

  @Test
  void endingAnUnknownProjectIsNotFound() {
    ResponseEntity<Map> response =
        ApiSession.signIn(rest, "lead.one").post("/api/projects/999/end", null, Map.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(body(response)).containsEntry("code", "not found");
  }

  private static Map<String, Object> project(String name, String lead) {
    return new java.util.HashMap<>(
        Map.of(
            "name", name,
            "lead", lead,
            "startDate", "2027-01-01",
            "endDate", "2027-12-31"));
  }

  private static List<String> names(ResponseEntity<List> response) {
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    return response.getBody().stream()
        .map(row -> String.valueOf(((Map) row).get("name")))
        .toList();
  }

  private long givenProject(String name, String lead) {
    jdbc.update(
        "INSERT INTO project (name, lead_user_id, start_date, end_date, status)"
            + " VALUES (?, (SELECT id FROM app_user WHERE username = ?),"
            + " DATE '2027-01-01', DATE '2027-12-31', 'OPEN')",
        name,
        lead);
    return jdbc.queryForObject("SELECT id FROM project WHERE name = ?", Long.class, name);
  }

  private long givenPost(long projectId, String role, String status) {
    jdbc.update(
        "INSERT INTO post (project_id, title, role_id, min_job_level, headcount,"
            + " start_date, end_date, travel_required, status)"
            + " VALUES (?, NULL, (SELECT id FROM role WHERE name = ?), 'Senior', 1,"
            + " DATE '2027-02-01', DATE '2027-06-30', 'none', ?)",
        projectId,
        role,
        status);
    return jdbc.queryForObject(
        "SELECT MAX(id) FROM post WHERE project_id = ?", Long.class, projectId);
  }

  private void givenPerson(String employeeNo, String role) {
    jdbc.update(
        "INSERT INTO person (employee_no, name, email, role_id, job_level, travel, active)"
            + " VALUES (?, ?, NULL, (SELECT id FROM role WHERE name = ?), 'Senior', 'none', TRUE)",
        employeeNo,
        employeeNo,
        role);
  }

  /** A confirmed seat: the assignment plus the allocation it backs, both from one confirmation. */
  private void givenConfirmed(long postId, String employeeNo, long projectId) {
    jdbc.update(
        "INSERT INTO assignment (post_id, person_no) VALUES (?, ?)", postId, employeeNo);
    long assignment =
        jdbc.queryForObject(
            "SELECT id FROM assignment WHERE post_id = ? AND person_no = ?",
            Long.class,
            postId,
            employeeNo);
    jdbc.update(
        "INSERT INTO allocation (person_no, project_id, start_date, end_date, source, assignment_id)"
            + " VALUES (?, ?, DATE '2027-02-01', DATE '2027-06-30', 'CONFIRMATION', ?)",
        employeeNo,
        projectId,
        assignment);
  }

  private long givenPendingRecommendation(long postId, String employeeNo) {
    jdbc.update(
        "INSERT INTO recommendation (post_id, person_no, state, snapshot_json, proposed_by)"
            + " VALUES (?, ?, 'proposed', '{}', (SELECT id FROM app_user WHERE username = 'coordinator'))",
        postId,
        employeeNo);
    return jdbc.queryForObject(
        "SELECT MAX(id) FROM recommendation WHERE post_id = ?", Long.class, postId);
  }

  private List<String> statuses(long projectId) {
    return jdbc.queryForList(
        "SELECT status FROM post WHERE project_id = ?", String.class, projectId);
  }

  private String projectStatus(long projectId) {
    return jdbc.queryForObject("SELECT status FROM project WHERE id = ?", String.class, projectId);
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
