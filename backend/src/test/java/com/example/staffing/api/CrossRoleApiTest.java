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
 * T20, the cross-role fallback: it opens only when no person of the post's role passes the gates, it
 * admits on X1 (a band above the requirement) and X2 (a skill of the post role's domain at
 * `Practitioner` or above) instead of G2, G3 and G4, and it never widens to any skill at all.
 *
 * <p>Each test starts from a dropped and rebuilt schema; the context is discarded after the class
 * so no other class inherits this database.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class CrossRoleApiTest {

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
  void theFallbackOpensWhenThePostRoleHasNobody() {
    long post = qaPost("Senior");
    givenPerson("E100", "Ada", "QA", "Junior", "full", true);
    givenPerson("E900", "Grace", "DEV", "Lead", "full", true);
    givenBand("E900", "测试", "Practitioner");

    Map<String, Object> answer = search(post);
    List<Map<String, Object>> fallback = (List<Map<String, Object>>) answer.get("fallback");

    assertThat((List<?>) answer.get("suitable")).isEmpty();
    assertThat((String) answer.get("fallbackReason")).isEqualTo("no QA person passes the gates");
    assertThat(fallback).singleElement().satisfies(
        row -> assertThat(row)
            .containsEntry("employeeNo", "E900")
            .containsEntry("role", "DEV")
            .containsEntry("bandsAbove", 1)
            .containsEntry("skill", "测试")
            .containsEntry("band", "Practitioner"));

    List<Map<String, Object>> excluded = (List<Map<String, Object>>) answer.get("excluded");
    assertThat(employeeNumbers(excluded)).containsExactly("E100");
    assertThat(excluded.get(0)).containsEntry("gate", "G3");
    assertThat(employeeNumbers(fallback)).doesNotContain("E100");
    assertThat(count(post, "G3")).isEqualTo(1);
    assertThat(count(post, "X1")).isZero();
    assertThat(count(post, "X2")).isZero();
  }

  @Test
  @SuppressWarnings("unchecked")
  void oneSameRoleCandidateClosesTheFallback() {
    long post = qaPost("Senior");
    givenPerson("E100", "Ada", "QA", "Senior", "full", true);
    givenBand("E100", "测试", "Practitioner");
    givenPerson("E900", "Grace", "DEV", "Principal", "full", true);
    givenBand("E900", "测试", "Expert");

    Map<String, Object> answer = search(post);

    assertThat(employeeNumbers((List<Map<String, Object>>) answer.get("suitable")))
        .containsExactly("E100");
    assertThat((List<?>) answer.get("fallback")).isEmpty();
    assertThat(answer.get("fallbackReason")).isNull();
    assertThat((List<?>) answer.get("excluded")).isEmpty();
    assertThat(counts(post).values())
        .allSatisfy(value -> assertThat(((Number) value).intValue()).isZero());
  }

  @Test
  @SuppressWarnings("unchecked")
  void aPersonAtTheRequiredBandIsNotAboveIt() {
    long post = qaPost("Senior");
    givenPerson("E100", "Ada", "QA", "Junior", "full", true);
    givenPerson("E900", "Grace", "DEV", "Senior", "full", true);
    givenBand("E900", "测试", "Expert");

    assertThat((List<?>) search(post).get("fallback")).isEmpty();
    assertThat(excluded(post))
        .filteredOn(row -> "E900".equals(row.get("employeeNo")))
        .singleElement()
        .satisfies(
            row -> {
              assertThat(row).containsEntry("gate", "X1");
              assertThat((String) row.get("reason"))
                  .isEqualTo("cross-role: level not above the requirement");
            });
    assertThat(count(post, "X1")).isEqualTo(1);
  }

  @Test
  @SuppressWarnings("unchecked")
  void aSkillOutsideThePostRoleDomainNeverAdmits() {
    long post = qaPost("Senior");
    givenPerson("E100", "Ada", "QA", "Junior", "full", true);
    givenPerson("E900", "Grace", "PM", "Lead", "full", true);
    givenBand("E900", "项目管理", "Expert");

    assertThat((List<?>) search(post).get("fallback")).isEmpty();
    assertThat(excluded(post))
        .filteredOn(row -> "E900".equals(row.get("employeeNo")))
        .singleElement()
        .satisfies(
            row -> {
              assertThat(row).containsEntry("gate", "X2");
              assertThat((String) row.get("reason"))
                  .isEqualTo("cross-role: no domain skill at Practitioner");
            });
    assertThat(count(post, "X2")).isEqualTo(1);
  }

  @Test
  @SuppressWarnings("unchecked")
  void theTopBandHasNoCrossRoleCandidate() {
    long post = qaPost("Principal");
    givenPerson("E100", "Ada", "QA", "Junior", "full", true);
    givenPerson("E900", "Grace", "DEV", "Principal", "full", true);
    givenBand("E900", "测试", "Expert");

    assertThat((List<?>) search(post).get("fallback")).isEmpty();
    assertThat(excluded(post))
        .filteredOn(row -> "E900".equals(row.get("employeeNo")))
        .singleElement()
        .satisfies(
            row -> {
              assertThat(row).containsEntry("gate", "X1");
              assertThat((String) row.get("reason"))
                  .isEqualTo("cross-role: no band sits above Principal");
            });
    assertThat(count(post, "X1")).isEqualTo(1);
  }

  @Test
  @SuppressWarnings("unchecked")
  void crossRoleEvidenceCarriesTheGapAndTheQualifyingSkill() {
    long post = qaPost("Senior");
    givenPerson("E100", "Ada", "QA", "Junior", "full", true);
    givenPerson("E900", "Grace", "DEV", "Principal", "full", true);
    givenBand("E900", "测试", "Practitioner");
    givenPerson("E800", "Ada", "BA", "Lead", "full", true);
    givenBand("E800", "测试", "Expert");

    List<Map<String, Object>> fallback =
        (List<Map<String, Object>>) search(post).get("fallback");

    assertThat(employeeNumbers(fallback)).containsExactly("E800", "E900");
    assertThat(fallback.get(0))
        .containsEntry("role", "BA")
        .containsEntry("jobLevel", "Lead")
        .containsEntry("bandsAbove", 1)
        .containsEntry("skill", "测试")
        .containsEntry("band", "Expert");
    assertThat(fallback.get(1))
        .containsEntry("role", "DEV")
        .containsEntry("jobLevel", "Principal")
        .containsEntry("bandsAbove", 2)
        .containsEntry("skill", "测试")
        .containsEntry("band", "Practitioner");
    assertThat(fallback.get(0))
        .containsOnlyKeys(
            "employeeNo", "name", "role", "jobLevel", "travel", "bandsAbove", "skill", "band");

    assertThat(
            jdbc.queryForObject(
                "SELECT r.name FROM person p JOIN role r ON r.id = p.role_id"
                    + " WHERE p.employee_no = 'E900'",
                String.class))
        .isEqualTo("DEV");
  }

  /** A QA post needing 测试 at `Practitioner`, which the seed puts in the QA domain. */
  private long qaPost(String level) {
    long atlas = givenProject("Atlas", "lead.one");
    long post = givenPost(atlas, "QA", level, "none", POST_START, POST_END);
    givenSkill(post, "测试", "REQUIRED", "Practitioner");
    return post;
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

  private long givenPost(
      long projectId, String role, String level, String travel, String start, String end) {
    jdbc.update(
        "INSERT INTO post (project_id, title, role_id, min_job_level, headcount,"
            + " start_date, end_date, travel_required, status)"
            + " VALUES (?, NULL, (SELECT id FROM role WHERE name = ?), ?, 1, ?, ?, ?, 'open')",
        projectId,
        role,
        level,
        Date.valueOf(LocalDate.parse(start)),
        Date.valueOf(LocalDate.parse(end)),
        travel);
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

  private Map<String, Object> search(long postId) {
    ResponseEntity<Map> response =
        coordinator.get("/api/posts/" + postId + "/candidates", Map.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    return body(response);
  }

  @SuppressWarnings("unchecked")
  private List<Map<String, Object>> excluded(long postId) {
    return (List<Map<String, Object>>) search(postId).get("excluded");
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> counts(long postId) {
    return (Map<String, Object>) search(postId).get("exclusionCounts");
  }

  private int count(long postId, String gate) {
    return ((Number) counts(postId).get(gate)).intValue();
  }

  private static List<Object> employeeNumbers(List<Map<String, Object>> rows) {
    return rows.stream().map(row -> row.get("employeeNo")).toList();
  }

  @SuppressWarnings("unchecked")
  private static Map<String, Object> body(ResponseEntity<Map> response) {
    return (Map<String, Object>) response.getBody();
  }
}
