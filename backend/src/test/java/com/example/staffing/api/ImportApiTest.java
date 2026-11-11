package com.example.staffing.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.staffing.support.ApiSession;
import com.example.staffing.support.WorkbookFixture;
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
 * T09, the import endpoint: a workbook is one unit of work, so a file is either applied whole or
 * recorded as a rejected batch with every bad row listed. Shape errors (one sheet, headers only)
 * leave nothing behind, and an applied file never reaches outside person data.
 *
 * <p>Each test starts from a dropped and rebuilt schema; the context is discarded after the class
 * so no other class inherits this database.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class ImportApiTest {

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
  void aSkillsOnlyRefreshIsAcceptedAndCreatesNoPerson() {
    givenPerson("E100", "DEV", "Senior", "limited", true);
    givenBand("E100", "测试", "Novice");

    ResponseEntity<Map> response =
        upload(WorkbookFixture.workbook().skill("E100", "测试", "Expert").xlsx());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(body(response))
        .containsEntry("status", "APPLIED")
        .containsEntry("personRows", 0)
        .containsEntry("skillRows", 1);
    assertThat(band("E100", "测试")).isEqualTo("Expert");
    assertThat(peopleCount()).isEqualTo(1);
  }

  @Test
  void aWorkbookWithOneSheetIsRefusedAndLeavesNoBatch() {
    byte[] file = WorkbookFixture.workbook().person(person("E200", "DEV")).onlyPeopleSheet().xlsx();

    ResponseEntity<Map> response = upload(file);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    assertThat(body(response)).containsEntry("code", "unreadable workbook");
    assertThat(history()).isEmpty();
  }

  @Test
  void anEmptyWorkbookIsRefusedByName() {
    ResponseEntity<Map> response = upload(WorkbookFixture.workbook().xlsx());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    assertThat(body(response).get("message").toString()).contains("empty");
    assertThat(history()).isEmpty();
  }

  @Test
  void oneBadRowBlocksTheWholeFile() {
    byte[] file =
        WorkbookFixture.workbook()
            .person(person("E201", "DEV"))
            .person(person("E202", "DEV"))
            .person(person("E203", "DEV"))
            .person(person("E204", "DEV"))
            .person("E205", "E205", "", "DEV", "Mid", "full", "active")
            .skill("E201", "Kotlin", "Novice")
            .xlsx();

    ResponseEntity<Map> response = upload(file);

    assertThat(body(response))
        .containsEntry("status", "REJECTED")
        .containsEntry("personRows", 5)
        .containsEntry("skillRows", 1)
        .containsEntry("errorCount", 2);
    assertThat(errorsOf(response))
        .extracting(error -> error.get("sheet") + "!" + error.get("rowNo"))
        .containsExactly("people!6", "skills!2");
    assertThat(peopleCount()).isZero();
  }

  @Test
  void theCorrectedFileIsANewBatchWhileTheRejectedOneKeepsItsErrors() {
    ResponseEntity<Map> rejected =
        upload(
            WorkbookFixture.workbook()
                .person("E200", "E200", "", "DEV", "Mid", "full", "active")
                .xlsx());
    long rejectedId = number(body(rejected).get("id"));

    ResponseEntity<Map> applied = upload(WorkbookFixture.workbook().person(person("E200", "DEV")).xlsx());

    assertThat(body(applied)).containsEntry("status", "APPLIED");
    assertThat(number(body(applied).get("id"))).isNotEqualTo(rejectedId);
    ResponseEntity<Map> earlier = coordinator.get("/api/imports/" + rejectedId, Map.class);
    assertThat(body(earlier)).containsEntry("status", "REJECTED").containsEntry("errorCount", 1);
    assertThat(errorsOf(earlier)).hasSize(1);
    assertThat(peopleCount()).isEqualTo(1);
  }

  @Test
  void aRejectedBatchStaysReadable() {
    ResponseEntity<Map> response =
        upload(
            WorkbookFixture.workbook()
                .person("E200", "E200", "", "Product", "Senior", "full", "active")
                .xlsx());

    Map<String, Object> batch = body(response);
    assertThat(batch)
        .containsEntry("filename", "people.xlsx")
        .containsEntry("uploader", "Resource coordinator")
        .containsEntry("status", "REJECTED")
        .containsEntry("personRows", 1)
        .containsEntry("skillRows", 0)
        .containsEntry("errorCount", 1);
    assertThat(batch.get("uploadedAt")).isNotNull();
    assertThat(errorsOf(response))
        .singleElement()
        .satisfies(
            error -> {
              assertThat(error).containsEntry("sheet", "people").containsEntry("rowNo", 2);
              assertThat(error.get("key")).isEqualTo("E200");
              assertThat(error.get("message").toString()).contains("Product");
            });

    ResponseEntity<Map> again = coordinator.get("/api/imports/" + batch.get("id"), Map.class);
    assertThat(body(again)).isEqualTo(batch);
  }

  @Test
  void lowerCaseIsNormalisedNotRejected() {
    ResponseEntity<Map> response =
        upload(
            WorkbookFixture.workbook()
                .person("E200", "E200", "", "dev", "senior", "FULL", "Active")
                .skill("E200", "测试", "practitioner")
                .xlsx());

    assertThat(body(response)).containsEntry("status", "APPLIED");
    assertThat(storedPerson("E200"))
        .containsEntry("job_level", "Senior")
        .containsEntry("travel", "full")
        .containsEntry("active", true);
    assertThat(band("E200", "测试")).isEqualTo("Practitioner");
  }

  @Test
  void aRoleOutsideTheCatalogBlocksTheFileAndListsTheAcceptedRoles() {
    ResponseEntity<Map> response =
        upload(
            WorkbookFixture.workbook()
                .person("E200", "E200", "", "Product", "Senior", "full", "active")
                .xlsx());

    assertThat(body(response)).containsEntry("status", "REJECTED");
    assertThat(errorsOf(response))
        .singleElement()
        .satisfies(
            error -> assertThat(error.get("message").toString()).contains("BA, DEV, PM, QA, UX"));
  }

  @Test
  void aMultiRoleCellIsARowErrorAndNeverTwoRoles() {
    ResponseEntity<Map> response =
        upload(
            WorkbookFixture.workbook()
                .person("E200", "E200", "", "DEV, QA", "Senior", "full", "active")
                .person("E300", "E300", "", "DEV / QA", "Senior", "full", "active")
                .xlsx());

    assertThat(body(response)).containsEntry("status", "REJECTED");
    assertThat(errorsOf(response))
        .hasSize(2)
        .allSatisfy(
            error ->
                assertThat(error.get("message").toString())
                    .contains("exactly one active catalog role"));
    assertThat(peopleCount()).isZero();
  }

  @Test
  void aDuplicatedSkillRowPutsBothRowsInTheHistory() {
    givenPerson("E100", "QA", "Senior", "none", true);

    ResponseEntity<Map> response =
        upload(
            WorkbookFixture.workbook()
                .skill("E100", "测试", "Novice")
                .skill("E100", "测试", "Expert")
                .xlsx());

    assertThat(body(response)).containsEntry("status", "REJECTED");
    assertThat(errorsOf(response))
        .extracting(error -> error.get("rowNo"))
        .containsExactly(2, 3);
    assertThat(band("E100", "测试")).isNull();
  }

  @Test
  void anOrphanSkillRowSaysPersonNotFound() {
    ResponseEntity<Map> response =
        upload(WorkbookFixture.workbook().skill("E999", "测试", "Novice").xlsx());

    assertThat(body(response)).containsEntry("status", "REJECTED");
    assertThat(errorsOf(response))
        .singleElement()
        .satisfies(
            error -> assertThat(error.get("message")).isEqualTo("person not found"));
  }

  @Test
  void aDuplicatedEmployeeNumberBlocksTheFile() {
    ResponseEntity<Map> response =
        upload(
            WorkbookFixture.workbook()
                .person(person("E200", "DEV"))
                .person(person("E200", "QA"))
                .xlsx());

    assertThat(body(response)).containsEntry("status", "REJECTED");
    assertThat(errorsOf(response))
        .singleElement()
        .satisfies(error -> assertThat(error.get("message").toString()).contains("E200"));
  }

  @Test
  void newAndExistingPeopleAreWrittenInOneFile() {
    givenPerson("E100", "DEV", "Senior", "limited", true);

    ResponseEntity<Map> response =
        upload(
            WorkbookFixture.workbook()
                .person("E100", "E100", "", "DEV", "Lead", "full", "active")
                .person("E200", "E200", "", "QA", "Junior", "none", "inactive")
                .xlsx());

    assertThat(body(response)).containsEntry("status", "APPLIED").containsEntry("personRows", 2);
    assertThat(storedPerson("E100")).containsEntry("job_level", "Lead").containsEntry("travel", "full");
    assertThat(storedPerson("E200"))
        .containsEntry("job_level", "Junior")
        .containsEntry("travel", "none")
        .containsEntry("active", false);
  }

  @Test
  void anInactivePersonIsReactivatedByTheReport() {
    givenPerson("E100", "DEV", "Senior", "limited", false);

    upload(WorkbookFixture.workbook().person(person("E100", "DEV")).xlsx());

    assertThat(storedPerson("E100")).containsEntry("active", true);
  }

  @Test
  void allocationsSurviveAnImport() {
    givenPerson("E100", "DEV", "Senior", "limited", true);
    long project = givenProject("Atlas");
    jdbc.update(
        "INSERT INTO allocation (person_no, project_id, start_date, end_date, source)"
            + " VALUES ('E100', ?, DATE '2026-01-05', DATE '2026-03-27', 'MANUAL')",
        project);

    upload(WorkbookFixture.workbook().person("E100", "E100", "", "DEV", "Senior", "full", "active").xlsx());

    assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM allocation", Integer.class)).isEqualTo(1);
    assertThat(
            jdbc.queryForObject(
                "SELECT end_date FROM allocation WHERE person_no = 'E100'", java.sql.Date.class))
        .isEqualTo(java.sql.Date.valueOf("2026-03-27"));
    assertThat(storedPerson("E100")).containsEntry("travel", "full");
  }

  @Test
  void aLeadCanNeitherUploadNorReadTheHistory() {
    ApiSession lead = ApiSession.signIn(rest, "lead.one");

    assertThat(lead.upload("/api/imports", "people.xlsx", WorkbookFixture.workbook().person(person("E200", "DEV")).xlsx(), Map.class).getStatusCode())
        .isEqualTo(HttpStatus.FORBIDDEN);
    assertThat(lead.get("/api/imports", Map.class).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    assertThat(history()).isEmpty();
  }

  @Test
  void aBatchThatNeverExistedIsNotFound() {
    assertThat(coordinator.get("/api/imports/999", Map.class).getStatusCode())
        .isEqualTo(HttpStatus.NOT_FOUND);
  }

  @Test
  @SuppressWarnings("unchecked")
  void theHistoryAnswersCountsNewestFirstAndKeepsTheRowsForTheDetail() {
    long applied =
        number(
            body(upload(WorkbookFixture.workbook().person(person("E200", "DEV")).xlsx()))
                .get("id"));
    long rejected =
        number(
            body(
                    upload(
                        WorkbookFixture.workbook()
                            .person("E201", "E201", "", "Product", "Senior", "full", "active")
                            .xlsx()))
                .get("id"));

    List<Map<String, Object>> rows = history();

    assertThat(rows)
        .extracting(row -> number(row.get("id")))
        .as("the newest upload comes first")
        .containsExactly(rejected, applied);
    assertThat(rows.get(0)).containsEntry("status", "REJECTED").containsEntry("errorCount", 1);
    assertThat((List<?>) rows.get(0).get("errors"))
        .as("the history answers counts; the detail route answers the rows")
        .isEmpty();
    assertThat(rows.get(1))
        .containsEntry("status", "APPLIED")
        .containsEntry("errorCount", 0)
        .containsEntry("filename", "people.xlsx");
  }

  @Test
  void aDeactivatedRoleOrSkillIsInvisibleToTheNextFile() {
    long role = jdbc.queryForObject("SELECT id FROM role WHERE name = 'QA'", Long.class);
    long skill = jdbc.queryForObject("SELECT id FROM skill WHERE name = '测试'", Long.class);
    coordinator.patch("/api/roles/" + role, Map.of("active", false), Map.class);
    coordinator.patch("/api/skills/" + skill, Map.of("active", false), Map.class);

    ResponseEntity<Map> response =
        upload(
            WorkbookFixture.workbook()
                .person(person("E200", "QA"))
                .skill("E200", "测试", "Novice")
                .xlsx());

    assertThat(body(response)).containsEntry("status", "REJECTED").containsEntry("errorCount", 2);
    assertThat(errorsOf(response))
        .extracting(error -> error.get("message").toString())
        .anySatisfy(message -> assertThat(message).contains("role QA must be exactly one active catalog role"))
        .anySatisfy(message -> assertThat(message).contains("skill 测试 is not in the active catalog"));
    assertThat(peopleCount()).isZero();
  }

  private ResponseEntity<Map> upload(byte[] file) {
    return coordinator.upload("/api/imports", "people.xlsx", file, Map.class);
  }

  @SuppressWarnings("unchecked")
  private static Map<String, Object> body(ResponseEntity<Map> response) {
    return (Map<String, Object>) response.getBody();
  }

  @SuppressWarnings("unchecked")
  private static List<Map<String, Object>> errorsOf(ResponseEntity<Map> response) {
    return (List<Map<String, Object>>) body(response).get("errors");
  }

  @SuppressWarnings("unchecked")
  private List<Map<String, Object>> history() {
    ResponseEntity<List> response = coordinator.get("/api/imports", List.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    return (List<Map<String, Object>>) response.getBody();
  }

  private int peopleCount() {
    return jdbc.queryForObject("SELECT COUNT(*) FROM person", Integer.class);
  }

  private Map<String, Object> storedPerson(String employeeNo) {
    return jdbc.queryForMap("SELECT * FROM person WHERE employee_no = ?", employeeNo);
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

  private void givenPerson(
      String employeeNo, String role, String jobLevel, String travel, boolean active) {
    jdbc.update(
        "INSERT INTO person (employee_no, name, email, role_id, job_level, travel, active)"
            + " VALUES (?, ?, NULL, (SELECT id FROM role WHERE name = ?), ?, ?, ?)",
        employeeNo,
        employeeNo,
        role,
        jobLevel,
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

  private long givenProject(String name) {
    jdbc.update(
        "INSERT INTO project (name, lead_user_id, start_date, end_date, status)"
            + " VALUES (?, (SELECT id FROM app_user WHERE username = 'lead.one'),"
            + " DATE '2027-01-01', DATE '2027-12-31', 'OPEN')",
        name);
    return jdbc.queryForObject("SELECT id FROM project WHERE name = ?", Long.class, name);
  }

  private static String[] person(String employeeNo, String role) {
    return new String[] {
      employeeNo, employeeNo, employeeNo.toLowerCase() + "@example.com", role, "Senior", "full",
      "active"
    };
  }

  private static long number(Object value) {
    return value instanceof Number count ? count.longValue() : Long.parseLong(value.toString());
  }
}
