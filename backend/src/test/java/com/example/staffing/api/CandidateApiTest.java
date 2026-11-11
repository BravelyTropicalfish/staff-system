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
 * T19, the suitable list: everyone who passes every gate, ordered by name and then by the employee
 * number with no score anywhere, the exclusions with their first failing gate and a count per gate,
 * and the search kept from every lead account.
 *
 * <p>Each test starts from a dropped and rebuilt schema; the context is discarded after the class
 * so no other class inherits this database.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class CandidateApiTest {

  private static final String POST_START = "2027-05-01";
  private static final String POST_END = "2027-07-31";

  @Autowired private TestRestTemplate rest;

  @Autowired private JdbcTemplate jdbc;

  private ApiSession coordinator;

  private ApiSession lead;

  @BeforeEach
  void freshSchema() throws Exception {
    try (var connection = jdbc.getDataSource().getConnection()) {
      connection.createStatement().execute("DROP ALL OBJECTS");
      ScriptUtils.executeSqlScript(connection, new ClassPathResource("schema.sql"));
      ScriptUtils.executeSqlScript(connection, new ClassPathResource("data.sql"));
    }
    coordinator = ApiSession.signIn(rest, "coordinator");
    lead = ApiSession.signIn(rest, "lead.one");
  }

  @Test
  @SuppressWarnings("unchecked")
  void theFirstFailingGateIsTheReportedReason() {
    long atlas = givenProject("Atlas", "lead.one");
    long post = givenPost(atlas, "DEV", "Senior", "none", POST_START, POST_END);
    givenSkill(post, "后端开发", "REQUIRED", "Practitioner");
    givenPerson("E100", "Ada", "DEV", "Junior", "full", true);
    givenBand("E100", "后端开发", "Novice");

    assertThat(suitable(post)).isEmpty();
    assertThat(excluded(post))
        .singleElement()
        .satisfies(
            row -> {
              assertThat(row).containsEntry("employeeNo", "E100").containsEntry("gate", "G3");
              assertThat((String) row.get("reason"))
                  .isEqualTo("职级 Junior 低于要求的 Senior");
            });
    assertThat(count(post, "G3")).isEqualTo(1);
  }

  @Test
  @SuppressWarnings("unchecked")
  void aBonusShortfallNeverExcludes() {
    long post = postWithAShortBonusBand();

    List<Map<String, Object>> rows = suitable(post);

    assertThat(rows).singleElement().satisfies(row -> assertThat(row).containsEntry("employeeNo", "E100"));
    assertThat(excluded(post)).isEmpty();
    assertThat(counts(post).values()).allSatisfy(value -> assertThat(((Number) value).intValue()).isZero());
  }

  @Test
  @SuppressWarnings("unchecked")
  void aBonusShortfallIsShownNotApplied() {
    long post = postWithAShortBonusBand();

    Map<String, Object> row = suitable(post).get(0);
    List<Map<String, Object>> skills = (List<Map<String, Object>>) row.get("skills");

    assertThat(skills).hasSize(2);
    assertThat(skills.get(0))
        .containsEntry("skill", "后端开发")
        .containsEntry("kind", "required")
        .containsEntry("band", "Practitioner");
    assertThat(skills.get(1))
        .containsEntry("skill", "测试")
        .containsEntry("kind", "bonus")
        .containsEntry("band", "Novice")
        .containsEntry("target", "Practitioner");
  }

  @Test
  void roleEqualityIsByTheIdentity() {
    long atlas = givenProject("Atlas", "lead.one");
    long post = givenPost(atlas, "QA", "Senior", "none", POST_START, POST_END);
    givenSkill(post, "测试", "REQUIRED", "Practitioner");
    givenPerson("E100", "Ada", "QA", "Senior", "full", true);
    givenBand("E100", "测试", "Practitioner");
    givenPerson("E900", "Grace", "DEV", "Principal", "full", true);
    givenBand("E900", "后端开发", "Expert");

    assertThat(employeeNumbers(suitable(post))).containsExactly("E100");

    jdbc.update("UPDATE role SET active = FALSE WHERE name = 'QA'");

    assertThat(employeeNumbers(suitable(post))).containsExactly("E100");
    assertThat(excluded(post)).isEmpty();
  }

  @Test
  @SuppressWarnings("unchecked")
  void onlyAnIntersectingRangeBlocks() {
    long atlas = givenProject("Atlas", "lead.one");
    long later = givenPost(atlas, "DEV", "Senior", "none", POST_START, POST_END);
    long touching = givenPost(atlas, "DEV", "Senior", "none", "2027-03-31", "2027-05-31");
    givenSkill(later, "后端开发", "REQUIRED", "Practitioner");
    givenSkill(touching, "后端开发", "REQUIRED", "Practitioner");
    givenPerson("E100", "Ada", "DEV", "Senior", "full", true);
    givenBand("E100", "后端开发", "Practitioner");
    givenAllocation("E100", atlas, "2027-01-01", "2027-03-31");

    assertThat(employeeNumbers(suitable(later))).containsExactly("E100");

    assertThat(suitable(touching)).isEmpty();
    assertThat(excluded(touching))
        .singleElement()
        .satisfies(
            row -> {
              assertThat(row).containsEntry("gate", "G6");
              assertThat((String) row.get("reason"))
                  .contains("2027-01-01")
                  .contains("2027-03-31");
            });
  }

  @Test
  @SuppressWarnings("unchecked")
  void everyoneWhoPassesIsListedWithNoScore() {
    long atlas = givenProject("Atlas", "lead.one");
    long post = givenPost(atlas, "DEV", "Senior", "limited", POST_START, POST_END);
    givenSkill(post, "后端开发", "REQUIRED", "Practitioner");
    givenPerson("E100", "Ada", "DEV", "Senior", "full", true);
    givenBand("E100", "后端开发", "Practitioner");
    givenPerson("E200", "Grace", "DEV", "Lead", "full", true);
    givenBand("E200", "后端开发", "Expert");
    givenPerson("E300", "Linus", "DEV", "Senior", "full", true);
    givenBand("E300", "后端开发", "Proficient");

    Map<String, Object> answer = search(post);
    List<Map<String, Object>> rows = (List<Map<String, Object>>) answer.get("suitable");

    assertThat(rows).hasSize(3);
    assertThat(answer).doesNotContainKeys("score", "grade", "rank");
    for (Map<String, Object> row : rows) {
      assertThat(row)
          .containsKeys("employeeNo", "name", "role", "jobLevel", "travel", "gates", "skills");
      assertThat(row.keySet())
          .containsExactlyInAnyOrder(
              "employeeNo", "name", "role", "jobLevel", "travel", "gates", "skills");
      assertThat(row).containsEntry("role", "DEV").containsEntry("travel", "full");
      assertThat(row.get("gates"))
          .isEqualTo(List.of("G1", "G2", "G3", "G4", "G5", "G6"));
    }
  }

  @Test
  void theOrderComesFromTheNameNotTheFit() {
    long atlas = givenProject("Atlas", "lead.one");
    long post = givenPost(atlas, "DEV", "Senior", "none", POST_START, POST_END);
    givenSkill(post, "后端开发", "REQUIRED", "Practitioner");
    givenPerson("E300", "Zoe", "DEV", "Principal", "full", true);
    givenBand("E300", "后端开发", "Expert");
    givenPerson("E200", "Ada", "DEV", "Senior", "full", true);
    givenBand("E200", "后端开发", "Practitioner");
    givenPerson("E100", "Ada", "DEV", "Senior", "full", true);
    givenBand("E100", "后端开发", "Practitioner");

    assertThat(employeeNumbers(suitable(post))).containsExactly("E100", "E200", "E300");
  }

  @Test
  void nothingPassesTheGates() {
    long atlas = givenProject("Atlas", "lead.one");
    long post = givenPost(atlas, "DEV", "Senior", "full", POST_START, POST_END);
    givenPerson("E100", "Ada", "DEV", "Senior", "none", true);
    givenPerson("E200", "Grace", "DEV", "Lead", "limited", true);

    assertThat(suitable(post)).isEmpty();
    assertThat(excluded(post)).hasSize(2);
    assertThat(count(post, "G5")).isEqualTo(2);
  }

  @Test
  @SuppressWarnings("unchecked")
  void anInactivePersonIsExcludedByG1() {
    long atlas = givenProject("Atlas", "lead.one");
    long post = givenPost(atlas, "DEV", "Senior", "none", POST_START, POST_END);
    givenPerson("E100", "Ada", "DEV", "Lead", "full", false);

    assertThat(suitable(post)).isEmpty();
    assertThat(excluded(post))
        .singleElement()
        .satisfies(row -> assertThat(row).containsEntry("gate", "G1"));
    assertThat(count(post, "G1")).isEqualTo(1);
  }

  @Test
  void aLeadCannotOpenTheSuitableList() {
    long atlas = givenProject("Atlas", "lead.one");
    long post = givenPost(atlas, "DEV", "Senior", "none", POST_START, POST_END);

    assertThat(lead.get("/api/posts/" + post + "/candidates", Map.class).getStatusCode())
        .isEqualTo(HttpStatus.FORBIDDEN);
    assertThat(coordinator.get("/api/posts/" + post + "/candidates", Map.class).getStatusCode())
        .isEqualTo(HttpStatus.OK);
    assertThat(coordinator.get("/api/posts/9999/candidates", Map.class).getStatusCode())
        .isEqualTo(HttpStatus.NOT_FOUND);
  }

  /** A post requiring 后端开发 at `Practitioner` and rating 测试 as bonus at the same band. */
  private long postWithAShortBonusBand() {
    long atlas = givenProject("Atlas", "lead.one");
    long post = givenPost(atlas, "DEV", "Senior", "none", POST_START, POST_END);
    givenSkill(post, "后端开发", "REQUIRED", "Practitioner");
    givenSkill(post, "测试", "BONUS", "Practitioner");
    givenPerson("E100", "Ada", "DEV", "Senior", "full", true);
    givenBand("E100", "后端开发", "Practitioner");
    givenBand("E100", "测试", "Novice");
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

  private void givenAllocation(String employeeNo, long projectId, String start, String end) {
    jdbc.update(
        "INSERT INTO allocation (person_no, project_id, start_date, end_date, source)"
            + " VALUES (?, ?, ?, ?, 'MANUAL')",
        employeeNo,
        projectId,
        Date.valueOf(LocalDate.parse(start)),
        Date.valueOf(LocalDate.parse(end)));
  }

  private Map<String, Object> search(long postId) {
    ResponseEntity<Map> response =
        coordinator.get("/api/posts/" + postId + "/candidates", Map.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    return body(response);
  }

  @SuppressWarnings("unchecked")
  private List<Map<String, Object>> suitable(long postId) {
    return (List<Map<String, Object>>) search(postId).get("suitable");
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
