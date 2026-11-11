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
 * T01–T05, T08 and T09: the headcount report's API. The pool is built through rows the schema already
 * stores, because the report writes nothing — and every test starts from a dropped and rebuilt
 * schema, so no class inherits another's people.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class HeadcountApiTest {

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
  void theTwoAxesDescribeOnePool() {
    givenTheAssignmentPool();

    Map<String, Object> report = report();

    assertThat(report).containsEntry("total", 113);
    assertThat(sum(byRole(report), "count")).isEqualTo(113);
    assertThat(sum(byGrade(report), "count")).isEqualTo(113);
  }

  @Test
  void anInactivePersonIsInNeitherChart() {
    givenPeople("PM", "Senior", 1, "ACT");
    givenPeople("DEV", "Lead", 1, "INA", false);

    Map<String, Object> report = report();

    assertThat(report).containsEntry("total", 1);
    assertThat(roleBar(report, "DEV")).containsEntry("count", 0);
  }

  @Test
  void aBarCarriesItsCountAndItsShare() {
    givenTheAssignmentPool();

    Map<String, Object> report = report();

    assertThat(roleBar(report, "PM")).containsEntry("count", 40).containsEntry("share", 35.4);
    assertThat(gradeBar(report, "Junior")).containsEntry("count", 38).containsEntry("share", 33.6);
  }

  @Test
  void aShareRoundsToOneDecimal() {
    givenPeople("PM", "Senior", 1, "ONE");
    givenPeople("BA", "Senior", 2, "TWO");

    Map<String, Object> report = report();

    assertThat(roleBar(report, "PM")).containsEntry("share", 33.3);
    assertThat(roleBar(report, "BA")).containsEntry("share", 66.7);
  }

  @Test
  void anExactHalfRoundsUp() {
    givenPeople("PM", "Senior", 7, "SEVEN");
    givenPeople("DEV", "Senior", 1, "EIGHTH");

    assertThat(roleBar(report(), "DEV")).containsEntry("share", 12.5);
  }

  @Test
  void aCategoryWithNobodyKeepsItsBar() {
    givenTheAssignmentPool();

    Map<String, Object> report = report();

    assertThat(byRole(report)).hasSize(5);
    assertThat(roleBar(report, "UX")).containsEntry("count", 0).containsEntry("share", 0.0);
  }

  @Test
  void roleBarsFollowCatalogOrder() {
    givenTheAssignmentPool();

    assertThat(names(byRole(report()), "role")).containsExactly("PM", "BA", "DEV", "QA", "UX");
  }

  @Test
  void gradeBarsFollowBandOrder() {
    givenTheAssignmentPool();

    assertThat(names(byGrade(report()), "grade"))
        .containsExactly("Junior", "Senior", "Lead", "Principal");
  }

  @Test
  void aDeactivatedRoleKeepsItsActivePeopleCounted() {
    givenPeople("UX", "Senior", 3, "UXP");
    jdbc.update("UPDATE role SET active = FALSE WHERE name IN ('UX', 'QA')");

    Map<String, Object> report = report();

    assertThat(roleBar(report, "UX")).containsEntry("count", 3);
    assertThat(names(byRole(report), "role")).doesNotContain("QA");
  }

  @Test
  void aLeadSeesNoReport() {
    givenTheAssignmentPool();
    ApiSession lead = ApiSession.signIn(rest, "lead.one");

    ResponseEntity<Map> response = lead.get("/api/headcount", Map.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    assertThat(response.getBody()).containsKey("code").doesNotContainKey("total");
  }

  @Test
  void anEmptyPoolSaysSo() {
    Map<String, Object> report = report();

    assertThat(report).containsEntry("total", 0);
    assertThat(byRole(report)).hasSize(5).allSatisfy(row -> assertThat(row).containsEntry("count", 0));
    assertThat(byGrade(report)).hasSize(4).allSatisfy(row -> assertThat(row).containsEntry("count", 0));
    assertThat(byGrade(report)).allSatisfy(row -> assertThat(row).containsEntry("share", 0.0));
  }

  @Test
  void anImportIsVisibleOnTheNextRead() {
    assertThat(report()).containsEntry("total", 0);

    ResponseEntity<Map> uploaded =
        coordinator.upload(
            "/api/imports",
            "people.xlsx",
            WorkbookFixture.workbook()
                .person("E900", "Import Person", "e900@example.com", "PM", "Senior", "none", "active")
                .xlsx(),
            Map.class);
    assertThat(uploaded.getStatusCode()).isEqualTo(HttpStatus.CREATED);

    Map<String, Object> after = report();

    assertThat(after).containsEntry("total", 1);
    assertThat(roleBar(after, "PM")).containsEntry("count", 1);
    assertThat(gradeBar(after, "Senior")).containsEntry("count", 1);
  }

  @Test
  void noFilterNarrowsTheReport() {
    givenTheAssignmentPool();

    Map<String, Object> plain = report();
    Map<String, Object> filtered = body(coordinator.get("/api/headcount?project=1&role=PM", Map.class));

    assertThat(filtered).isEqualTo(plain);
  }

  @Test
  void noWriteVerbAnswers() {
    assertThat(coordinator.post("/api/headcount", Map.of(), Map.class).getStatusCode())
        .isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
    assertThat(coordinator.patch("/api/headcount", Map.of(), Map.class).getStatusCode())
        .isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
    assertThat(coordinator.delete("/api/headcount", Map.class).getStatusCode())
        .isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
  }

  @Test
  void theAnswerCarriesNoPersonalDetail() {
    givenTheAssignmentPool();

    Map<String, Object> report = report();

    assertThat(report).containsOnlyKeys("total", "byRole", "byGrade");
    assertThat(byRole(report)).allSatisfy(row -> assertThat(row).containsOnlyKeys("roleId", "role", "count", "share"));
    assertThat(byGrade(report)).allSatisfy(row -> assertThat(row).containsOnlyKeys("grade", "count", "share"));
  }

  /**
   * The assignment's example pool: 113 active people over five roles and four bands, so the shares
   * (35.4 / 22.1 / 12.4 / 30.1 / 0.0) and the band order can be asserted against known numbers.
   */
  private void givenTheAssignmentPool() {
    givenPeople("PM", "Junior", 20, "JPM");
    givenPeople("PM", "Senior", 20, "SPM");
    givenPeople("BA", "Senior", 15, "SBA");
    givenPeople("BA", "Lead", 10, "LBA");
    givenPeople("DEV", "Junior", 8, "JDEV");
    givenPeople("DEV", "Lead", 6, "LDEV");
    givenPeople("QA", "Junior", 10, "JQA");
    givenPeople("QA", "Senior", 7, "SQA");
    givenPeople("QA", "Lead", 12, "LQA");
    givenPeople("QA", "Principal", 5, "PQA");
  }

  private void givenPeople(String role, String level, int count, String prefix) {
    givenPeople(role, level, count, prefix, true);
  }

  private void givenPeople(String role, String level, int count, String prefix, boolean active) {
    for (int i = 1; i <= count; i++) {
      jdbc.update(
          "INSERT INTO person (employee_no, name, email, role_id, job_level, travel, active)"
              + " VALUES (?, ?, NULL, (SELECT id FROM role WHERE name = ?), ?, 'none', ?)",
          prefix + i,
          "Person " + prefix + i,
          role,
          level,
          active);
    }
  }

  private Map<String, Object> report() {
    ResponseEntity<Map> response = coordinator.get("/api/headcount", Map.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    return body(response);
  }

  @SuppressWarnings("unchecked")
  private static Map<String, Object> body(ResponseEntity<Map> response) {
    return (Map<String, Object>) response.getBody();
  }

  @SuppressWarnings("unchecked")
  private static List<Map<String, Object>> byRole(Map<String, Object> report) {
    return (List<Map<String, Object>>) report.get("byRole");
  }

  @SuppressWarnings("unchecked")
  private static List<Map<String, Object>> byGrade(Map<String, Object> report) {
    return (List<Map<String, Object>>) report.get("byGrade");
  }

  private static Map<String, Object> roleBar(Map<String, Object> report, String role) {
    return row(byRole(report), "role", role);
  }

  private static Map<String, Object> gradeBar(Map<String, Object> report, String grade) {
    return row(byGrade(report), "grade", grade);
  }

  private static Map<String, Object> row(List<Map<String, Object>> bars, String key, String value) {
    return bars.stream()
        .filter(bar -> value.equals(bar.get(key)))
        .findFirst()
        .orElseThrow(() -> new AssertionError("no bar for " + value));
  }

  private static List<String> names(List<Map<String, Object>> bars, String key) {
    return bars.stream().map(bar -> String.valueOf(bar.get(key))).toList();
  }

  private static int sum(List<Map<String, Object>> bars, String key) {
    return bars.stream().mapToInt(bar -> ((Number) bar.get(key)).intValue()).sum();
  }
}
