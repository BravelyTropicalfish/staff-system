package com.example.staffing.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.staffing.domain.Bands;
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
 * T11, the profile read surface: the list and one person. A profile holds one role, every skill
 * with its band by name, and every allocation with the past ones marked — and it answers no write
 * method at all.
 *
 * <p>Each test starts from a dropped and rebuilt schema; the context is discarded after the class
 * so no other class inherits this database.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class PeopleApiTest {

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
  void aPersonHoldsOneRoleAndASkillOfAnotherDomainAddsNoSecondOne() {
    givenPerson("E100", "Anna", "anna@example.com", "DEV", "Senior", "limited", true);
    givenBand("E100", "测试", "Expert");

    Map<String, Object> profile = profile("E100");

    assertThat(profile).containsEntry("role", "DEV");
    assertThat(profile).doesNotContainKey("roles");
    assertThat(skills(profile)).hasSize(1);
    assertThat(skills(profile).getFirst())
        .containsEntry("skill", "测试")
        .containsEntry("roleDomain", "QA")
        .containsEntry("band", "Expert");
  }

  @Test
  void aBandIsOverwrittenInPlace() {
    givenPerson("E100", "Anna", "anna@example.com", "DEV", "Senior", "limited", true);
    givenBand("E100", "后端开发", "Novice");

    upload(
        WorkbookFixture.workbook()
            .person("E100", "Anna", "anna@example.com", "DEV", "Senior", "limited", "active", "后端开发:Practitioner")
            .xlsx());

    assertThat(skills(profile("E100")))
        .singleElement()
        .satisfies(row -> assertThat(row).containsEntry("band", "Practitioner"));
  }

  @Test
  void anOmittedSkillKeepsItsBand() {
    givenPerson("E100", "Anna", "anna@example.com", "DEV", "Senior", "limited", true);
    givenBand("E100", "测试", "Proficient");

    upload(
        WorkbookFixture.workbook()
            .person("E100", "Anna", "anna@example.com", "DEV", "Senior", "limited", "active", "后端开发:Novice")
            .xlsx());

    assertThat(skills(profile("E100")))
        .extracting(row -> row.get("skill") + "=" + row.get("band"))
        .containsExactlyInAnyOrder("测试=Proficient", "后端开发=Novice");
  }

  @Test
  void theProfileListsEveryAllocationAndMarksTheEndedOne() {
    givenPerson("E100", "Anna", "anna@example.com", "DEV", "Senior", "limited", true);
    long atlas = givenProject("Atlas");
    long borealis = givenProject("Borealis");
    givenAllocation("E100", borealis, "2026-01-01", "2026-03-31");
    givenAllocation("E100", atlas, "2027-02-01", "2027-06-30");

    List<Map<String, Object>> allocations = allocations(profile("E100"));

    assertThat(allocations).hasSize(2);
    assertThat(allocations.getFirst())
        .containsEntry("project", "Atlas")
        .containsEntry("startDate", "2027-02-01")
        .containsEntry("endDate", "2027-06-30")
        .containsEntry("ended", false);
    assertThat(allocations.getLast())
        .containsEntry("project", "Borealis")
        .containsEntry("ended", true);
  }

  @Test
  void everyBandIsShownByNameAndNeverAsANumber() {
    givenPerson("E101", "Bo", null, "QA", "Lead", "none", true);
    givenBand("E101", "测试", "Practitioner");

    Map<String, Object> profile = profile("E101");

    assertThat(profile.get("jobLevel")).isIn(Bands.JOB_LEVELS);
    assertThat(profile.get("travel")).isIn(Bands.TRAVEL);
    assertThat(skills(profile))
        .allSatisfy(row -> assertThat(row.get("band")).isIn(Bands.PROFICIENCIES));
    assertThat(skills(profile).getFirst()).containsEntry("band", "Practitioner");
  }

  @Test
  void theListShowsEachPersonWithTheOneRoleTheyHold() {
    givenPerson("E102", "Cai", null, "UX", "Junior", "full", true);
    givenPerson("E100", "Anna", null, "DEV", "Senior", "limited", false);

    ResponseEntity<List> response = coordinator.get("/api/people", List.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody())
        .extracting(row -> ((Map) row).get("employeeNo") + "=" + ((Map) row).get("role"))
        .containsExactly("E100=DEV", "E102=UX");
  }

  @Test
  void theProfileAnswersNoWriteMethod() {
    givenPerson("E100", "Anna", null, "DEV", "Junior", "limited", true);

    assertThat(coordinator.post("/api/people/E100", Map.of("jobLevel", "Lead"), Map.class).getStatusCode())
        .isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
    assertThat(coordinator.patch("/api/people/E100", Map.of("jobLevel", "Lead"), Map.class).getStatusCode())
        .isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
    assertThat(coordinator.delete("/api/people/E100", Map.class).getStatusCode())
        .isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
    assertThat(jobLevel("E100")).isEqualTo("Junior");
  }

  @Test
  void anUnknownEmployeeIsNotFound() {
    ResponseEntity<Map> response = coordinator.get("/api/people/E999", Map.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(body(response)).containsEntry("code", "not found");
  }

  @Test
  void aLeadReadsNeitherTheListNorAProfile() {
    givenPerson("E100", "Anna", null, "DEV", "Junior", "limited", true);
    ApiSession lead = ApiSession.signIn(rest, "lead.one");

    assertThat(lead.get("/api/people", Map.class).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    assertThat(lead.get("/api/people/E100", Map.class).getStatusCode())
        .isEqualTo(HttpStatus.FORBIDDEN);
  }

  private Map<String, Object> profile(String employeeNo) {
    ResponseEntity<Map> response = coordinator.get("/api/people/" + employeeNo, Map.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    return body(response);
  }

  @SuppressWarnings("unchecked")
  private static List<Map<String, Object>> skills(Map<String, Object> profile) {
    return (List<Map<String, Object>>) profile.get("skills");
  }

  @SuppressWarnings("unchecked")
  private static List<Map<String, Object>> allocations(Map<String, Object> profile) {
    return (List<Map<String, Object>>) profile.get("allocations");
  }

  private void upload(byte[] file) {
    ResponseEntity<Map> response = coordinator.upload("/api/imports", "people.xlsx", file, Map.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
  }

  private String jobLevel(String employeeNo) {
    return jdbc.queryForObject(
        "SELECT job_level FROM person WHERE employee_no = ?", String.class, employeeNo);
  }

  private void givenPerson(
      String employeeNo,
      String name,
      String email,
      String role,
      String jobLevel,
      String travel,
      boolean active) {
    jdbc.update(
        "INSERT INTO person (employee_no, name, email, role_id, job_level, travel, active)"
            + " VALUES (?, ?, ?, (SELECT id FROM role WHERE name = ?), ?, ?, ?)",
        employeeNo,
        name,
        email,
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

  private void givenAllocation(String employeeNo, long projectId, String start, String end) {
    jdbc.update(
        "INSERT INTO allocation (person_no, project_id, start_date, end_date, source)"
            + " VALUES (?, ?, ?, ?, 'MANUAL')",
        employeeNo,
        projectId,
        java.sql.Date.valueOf(start),
        java.sql.Date.valueOf(end));
  }

  @SuppressWarnings("unchecked")
  private static Map<String, Object> body(ResponseEntity<Map> response) {
    return (Map<String, Object>) response.getBody();
  }
}
