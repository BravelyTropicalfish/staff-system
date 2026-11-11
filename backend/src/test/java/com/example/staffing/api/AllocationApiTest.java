package com.example.staffing.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.staffing.support.ApiSession;
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
 * T14, the hand-entered allocations: the coordinator records, edits and removes a range, two
 * overlapping ranges simply coexist, and a range a confirmation wrote answers the release-first
 * reason instead of being edited or deleted.
 *
 * <p>Each test starts from a dropped and rebuilt schema; the context is discarded after the class
 * so no other class inherits this database.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class AllocationApiTest {

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
  void aHandRecordedAllocationIsStored() {
    long atlas = givenProject("Atlas", "OPEN");
    givenPerson("E100");

    ResponseEntity<Map> response =
        coordinator.post("/api/allocations", allocation("E100", atlas, "2027-02-01", "2027-04-30"), Map.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(body(response))
        .containsEntry("personNo", "E100")
        .containsEntry("project", "Atlas")
        .containsEntry("startDate", "2027-02-01")
        .containsEntry("endDate", "2027-04-30")
        .containsEntry("source", "MANUAL");
    assertThat(count("SELECT COUNT(*) FROM allocation")).isEqualTo(1);
  }

  @Test
  void invertedDatesAreRefusedAndNothingIsStored() {
    long atlas = givenProject("Atlas", "OPEN");
    givenPerson("E100");

    ResponseEntity<Map> response =
        coordinator.post("/api/allocations", allocation("E100", atlas, "2027-04-30", "2027-02-01"), Map.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    assertThat(body(response)).containsEntry("code", "invalid allocation");
    assertThat(count("SELECT COUNT(*) FROM allocation")).isZero();
  }

  @Test
  void twoOverlappingAllocationsCoexist() {
    long atlas = givenProject("Atlas", "OPEN");
    long borealis = givenProject("Borealis", "OPEN");
    givenPerson("E100");

    ResponseEntity<Map> first =
        coordinator.post("/api/allocations", allocation("E100", atlas, "2027-01-01", "2027-02-28"), Map.class);
    ResponseEntity<Map> second =
        coordinator.post("/api/allocations", allocation("E100", borealis, "2027-02-01", "2027-03-31"), Map.class);

    assertThat(first.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(second.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(count("SELECT COUNT(*) FROM allocation WHERE person_no = 'E100'")).isEqualTo(2);
  }

  @Test
  void aConfirmedAllocationCannotBeRemovedOrEditedByHand() {
    long atlas = givenProject("Atlas", "OPEN");
    givenPerson("E100");
    long allocation = givenConfirmedAllocation("E100", atlas);

    ResponseEntity<Map> removed = coordinator.delete("/api/allocations/" + allocation, Map.class);
    ResponseEntity<Map> edited =
        coordinator.patch("/api/allocations/" + allocation, range("2027-03-01", "2027-03-31"), Map.class);

    assertThat(removed.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(body(removed)).containsEntry("code", "release first");
    assertThat(edited.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(body(edited).get("message").toString()).contains("release the person instead");
    assertThat(count("SELECT COUNT(*) FROM allocation")).isEqualTo(1);
    assertThat(jdbc.queryForObject("SELECT start_date FROM allocation", String.class))
        .isEqualTo("2027-02-01");
  }

  @Test
  void aHandRecordedAllocationCanBeEditedAndRemoved() {
    long atlas = givenProject("Atlas", "OPEN");
    givenPerson("E100");
    long id = givenManualAllocation("E100", atlas, "2027-01-01", "2027-02-28");

    ResponseEntity<Map> edited =
        coordinator.patch("/api/allocations/" + id, range("2027-01-01", "2027-03-31"), Map.class);
    ResponseEntity<Map> removed = coordinator.delete("/api/allocations/" + id, Map.class);

    assertThat(edited.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(body(edited)).containsEntry("endDate", "2027-03-31");
    assertThat(removed.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    assertThat(count("SELECT COUNT(*) FROM allocation")).isZero();
  }

  @Test
  void aLeadNeitherRecordsNorEditsAnAllocation() {
    long atlas = givenProject("Atlas", "OPEN");
    givenPerson("E100");
    long id = givenManualAllocation("E100", atlas, "2027-01-01", "2027-02-28");
    ApiSession lead = ApiSession.signIn(rest, "lead.one");

    ResponseEntity<Map> created =
        lead.post("/api/allocations", allocation("E100", atlas, "2027-05-01", "2027-05-31"), Map.class);
    ResponseEntity<Map> edited =
        lead.patch("/api/allocations/" + id, range("2027-01-01", "2027-03-31"), Map.class);
    ResponseEntity<Map> removed = lead.delete("/api/allocations/" + id, Map.class);

    assertThat(created.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    assertThat(edited.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    assertThat(removed.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    assertThat(count("SELECT COUNT(*) FROM allocation")).isEqualTo(1);
  }

  @Test
  void anUnknownPersonOrProjectIsRefused() {
    long atlas = givenProject("Atlas", "OPEN");
    givenPerson("E100");

    ResponseEntity<Map> unknownPerson =
        coordinator.post("/api/allocations", allocation("E999", atlas, "2027-02-01", "2027-04-30"), Map.class);
    ResponseEntity<Map> unknownProject =
        coordinator.post("/api/allocations", allocation("E100", 999, "2027-02-01", "2027-04-30"), Map.class);

    assertThat(unknownPerson.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    assertThat(body(unknownPerson).get("message").toString()).contains("E999", "not in the pool");
    assertThat(unknownProject.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    assertThat(count("SELECT COUNT(*) FROM allocation")).isZero();
  }

  @Test
  void aClosedProjectTakesNoNewAllocation() {
    long atlas = givenProject("Atlas", "CLOSED");
    givenPerson("E100");

    ResponseEntity<Map> response =
        coordinator.post("/api/allocations", allocation("E100", atlas, "2027-02-01", "2027-04-30"), Map.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(body(response)).containsEntry("code", "project closed");
    assertThat(count("SELECT COUNT(*) FROM allocation")).isZero();
  }

  @Test
  void anUnknownAllocationIsNotFound() {
    ResponseEntity<Map> edited =
        coordinator.patch("/api/allocations/999", range("2027-01-01", "2027-02-01"), Map.class);
    ResponseEntity<Map> removed = coordinator.delete("/api/allocations/999", Map.class);

    assertThat(edited.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(removed.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
  }

  private static Map<String, Object> allocation(String personNo, long projectId, String start, String end) {
    return Map.of(
        "personNo", personNo,
        "projectId", projectId,
        "startDate", start,
        "endDate", end);
  }

  private static Map<String, Object> range(String start, String end) {
    return Map.of("startDate", start, "endDate", end);
  }

  private long givenProject(String name, String status) {
    jdbc.update(
        "INSERT INTO project (name, lead_user_id, start_date, end_date, status)"
            + " VALUES (?, (SELECT id FROM app_user WHERE username = 'lead.one'),"
            + " DATE '2027-01-01', DATE '2027-12-31', ?)",
        name,
        status);
    return jdbc.queryForObject("SELECT id FROM project WHERE name = ?", Long.class, name);
  }

  private void givenPerson(String employeeNo) {
    jdbc.update(
        "INSERT INTO person (employee_no, name, email, role_id, job_level, travel, active)"
            + " VALUES (?, ?, NULL, (SELECT id FROM role WHERE name = 'DEV'), 'Senior', 'none', TRUE)",
        employeeNo,
        employeeNo);
  }

  private long givenManualAllocation(String employeeNo, long projectId, String start, String end) {
    jdbc.update(
        "INSERT INTO allocation (person_no, project_id, start_date, end_date, source)"
            + " VALUES (?, ?, ?, ?, 'MANUAL')",
        employeeNo,
        projectId,
        java.sql.Date.valueOf(start),
        java.sql.Date.valueOf(end));
    return jdbc.queryForObject("SELECT MAX(id) FROM allocation", Long.class);
  }

  /** A seat the confirmation flow wrote: the assignment first, then the allocation behind it. */
  private long givenConfirmedAllocation(String employeeNo, long projectId) {
    jdbc.update(
        "INSERT INTO post (project_id, title, role_id, min_job_level, headcount,"
            + " start_date, end_date, travel_required, status)"
            + " VALUES (?, NULL, (SELECT id FROM role WHERE name = 'DEV'), 'Senior', 1,"
            + " DATE '2027-02-01', DATE '2027-06-30', 'none', 'filled')",
        projectId);
    long post = jdbc.queryForObject("SELECT MAX(id) FROM post", Long.class);
    jdbc.update("INSERT INTO assignment (post_id, person_no) VALUES (?, ?)", post, employeeNo);
    long assignment = jdbc.queryForObject("SELECT MAX(id) FROM assignment", Long.class);
    jdbc.update(
        "INSERT INTO allocation (person_no, project_id, start_date, end_date, source, assignment_id)"
            + " VALUES (?, ?, DATE '2027-02-01', DATE '2027-06-30', 'CONFIRMATION', ?)",
        employeeNo,
        projectId,
        assignment);
    return jdbc.queryForObject("SELECT MAX(id) FROM allocation", Long.class);
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
