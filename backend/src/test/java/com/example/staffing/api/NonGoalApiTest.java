package com.example.staffing.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.staffing.support.ApiSession;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.classreading.SimpleMetadataReaderFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.util.ClassUtils;

/**
 * T29, the Non-Goals of `atdd-design.md` §6 answered backwards: each check proves the system
 * visibly does not do the thing, rather than that it does something else. The nine API checks cover
 * rows 1–9; row 10 (no deploy job in the workflow) is a review of `.github/workflows/ci.yml`,
 * recorded with the task's run notes.
 *
 * <p>The request bodies that carry a Non-Goal field are refused as unreadable: an unknown field
 * would otherwise be dropped silently, which is exactly what a Non-Goal must not allow.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class NonGoalApiTest {

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

  /** Non-goal 1: no external integration or scheduled drops, and no route edits a person. */
  @Test
  void nothingScheduledRunsAndNoRouteEditsAPerson() throws Exception {
    assertThat(schedulingEntryPoints()).isEmpty();
    givenPerson("E100");

    Map<String, Object> change = Map.of("jobLevel", "Lead");

    assertThat(coordinator.patch("/api/people/E100", change, Map.class).getStatusCode())
        .isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
    assertThat(coordinator.post("/api/people/E100", change, Map.class).getStatusCode())
        .isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
    assertThat(coordinator.delete("/api/people/E100", Map.class).getStatusCode())
        .isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
    assertThat(column("SELECT job_level FROM person WHERE employee_no = 'E100'"))
        .isEqualTo("Senior");
  }

  /** Non-goal 2: no hand-edited person master data — the band survives a refused edit. */
  @Test
  void aHandEditOfAPersonsBandIsRefused() {
    givenPerson("E100");
    givenBand("E100", "后端开发", "Novice");

    ResponseEntity<Map> refused =
        coordinator.patch(
            "/api/people/E100",
            Map.of("skillBands", List.of(Map.of("skill", "后端开发", "band", "Expert"))),
            Map.class);

    assertThat(refused.getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
    assertThat(column("SELECT band FROM person_skill WHERE employee_no = 'E100'"))
        .isEqualTo("Novice");
  }

  /** Non-goal 3: no part-time staffing — a percentage is refused and nothing is written. */
  @Test
  void aPartTimePercentageIsRefusedAndChangesNothing() {
    long project = givenProject("Atlas", "lead.one");
    givenPerson("E100");
    Map<String, Object> body = new HashMap<>();
    body.put("personNo", "E100");
    body.put("projectId", project);
    body.put("startDate", "2027-03-01");
    body.put("endDate", "2027-03-31");
    body.put("percentage", 50);

    ResponseEntity<Map> refused = coordinator.post("/api/allocations", body, Map.class);

    assertThat(refused.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(body(refused)).containsEntry("code", "unreadable body");
    assertThat(count("SELECT COUNT(*) FROM allocation")).isZero();
  }

  /** Non-goal 4: no automatic assignment — a suitable person leaves the post open and empty. */
  @Test
  void aSuitableCandidateLeavesThePostOpenUntilAHumanConfirms() {
    long post = givenPost(givenProject("Atlas", "lead.one"), "DEV", 1);
    givenPerson("E100");

    assertThat(suitableOf(post)).hasSize(1);

    assertThat(body(lead.get("/api/posts/" + post, Map.class)))
        .containsEntry("status", "open")
        .containsEntry("confirmed", 0)
        .containsEntry("remaining", 1);
    assertThat(count("SELECT COUNT(*) FROM assignment")).isZero();
    assertThat(count("SELECT COUNT(*) FROM allocation")).isZero();
  }

  /** Non-goal 5: no notifications — no mail or chat transport, and no send route. */
  @Test
  void noMailOrChatTransportExistsAndNoRouteSendsANotice() {
    ClassLoader loader = getClass().getClassLoader();

    assertThat(ClassUtils.isPresent("org.springframework.mail.javamail.JavaMailSender", loader))
        .isFalse();
    assertThat(ClassUtils.isPresent("org.springframework.messaging.simp.SimpMessagingTemplate", loader))
        .isFalse();
    assertThat(coordinator.post("/api/notices/1/send", Map.of(), Map.class).getStatusCode())
        .isEqualTo(HttpStatus.NOT_FOUND);
  }

  /** Non-goal 6: no cost, rate, budget or utilisation — refused on write, absent on read. */
  @Test
  void aRateIsRefusedOnWriteAndAbsentFromThePostRead() {
    long project = givenProject("Atlas", "lead.one");
    Map<String, Object> body = postBody();
    body.put("hourlyRate", 100);

    ResponseEntity<Map> refused = lead.post("/api/projects/" + project + "/posts", body, Map.class);

    assertThat(refused.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(body(refused)).containsEntry("code", "unreadable body");
    assertThat(count("SELECT COUNT(*) FROM post")).isZero();

    long post = givenPost(project, "DEV", 1);
    assertThat(body(lead.get("/api/posts/" + post, Map.class)).keySet())
        .noneMatch(key -> key.toLowerCase().matches(".*(rate|cost|budget|utili[sz]ation).*"));
  }

  /** Non-goal 7: no interview or approval chain — proposed becomes confirmed in one call. */
  @Test
  void aProposalIsConfirmedInOneStepWithNoIntermediateState() {
    long post = givenPost(givenProject("Atlas", "lead.one"), "DEV", 1);
    givenPerson("E100");
    long recommendation = givenProposal(post, "E100");

    assertThat(
            lead.patch("/api/recommendations/" + recommendation, Map.of("state", "approved"), Map.class)
                .getStatusCode())
        .isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
    assertThat(firstRecommendation(post)).containsEntry("state", "proposed");

    ResponseEntity<Map> confirmed =
        lead.post("/api/recommendations/" + recommendation + "/confirm", null, Map.class);

    assertThat(confirmed.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat((Map<String, Object>) body(confirmed).get("recommendation"))
        .containsEntry("state", "confirmed");
  }

  /** Non-goal 8: no mobile, offline or multi-tenancy — a tenant parameter changes nothing. */
  @Test
  void aTenantParameterIsNotAcceptedAndChangesNothing() {
    givenPerson("E100");

    ResponseEntity<List> plain = coordinator.get("/api/people", List.class);
    ResponseEntity<List> withTenant = coordinator.get("/api/people?tenant=acme", List.class);

    assertThat(plain.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(withTenant.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(withTenant.getBody()).isEqualTo(plain.getBody());
    assertThat(plain.getBody()).hasSize(1);
  }

  /** Non-goal 9: no destination-specific travel — a destination is refused, the band stays one of three. */
  @Test
  void aTravelDestinationIsRefusedAndTravelIsOneOfTheThreeBands() {
    long project = givenProject("Atlas", "lead.one");
    Map<String, Object> body = postBody();
    body.put("travelDestination", "Berlin");

    ResponseEntity<Map> refused = lead.post("/api/projects/" + project + "/posts", body, Map.class);

    assertThat(refused.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(body(refused)).containsEntry("code", "unreadable body");

    long post = givenPost(project, "DEV", 1);
    Map<String, Object> view = body(lead.get("/api/posts/" + post, Map.class));
    assertThat(view).containsEntry("travel", "none");
    assertThat(view.keySet()).noneMatch(key -> key.toLowerCase().contains("destination"));
  }

  /**
   * Every class under the application's package, read without loading it: an {@code
   * @EnableScheduling} class or a {@code @Scheduled} method would be a scheduled drop.
   */
  private static List<String> schedulingEntryPoints() throws Exception {
    PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
    MetadataReaderFactory readers = new SimpleMetadataReaderFactory();
    List<String> found = new ArrayList<>();
    for (Resource resource : resolver.getResources("classpath*:com/example/staffing/**/*.class")) {
      MetadataReader reader = readers.getMetadataReader(resource);
      String className = reader.getClassMetadata().getClassName();
      if (reader.getAnnotationMetadata().isAnnotated(EnableScheduling.class.getName())) {
        found.add(className);
      }
      reader
          .getAnnotationMetadata()
          .getAnnotatedMethods(Scheduled.class.getName())
          .forEach(method -> found.add(className + "#" + method.getMethodName()));
    }
    return found;
  }

  private static Map<String, Object> postBody() {
    Map<String, Object> body = new HashMap<>();
    body.put("role", "DEV");
    body.put("minJobLevel", "Senior");
    body.put("headcount", 1);
    body.put("startDate", "2027-02-01");
    body.put("endDate", "2027-06-30");
    body.put("travel", "none");
    return body;
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

  private long givenPost(long projectId, String role, int headcount) {
    jdbc.update(
        "INSERT INTO post (project_id, title, role_id, min_job_level, headcount,"
            + " start_date, end_date, travel_required, status)"
            + " VALUES (?, NULL, (SELECT id FROM role WHERE name = ?), 'Senior', ?,"
            + " DATE '2027-02-01', DATE '2027-06-30', 'none', 'open')",
        projectId,
        role,
        headcount);
    return jdbc.queryForObject(
        "SELECT MAX(id) FROM post WHERE project_id = ?", Long.class, projectId);
  }

  private void givenPerson(String employeeNo) {
    jdbc.update(
        "INSERT INTO person (employee_no, name, email, role_id, job_level, travel, active)"
            + " VALUES (?, ?, NULL, (SELECT id FROM role WHERE name = 'DEV'), 'Senior', 'none', TRUE)",
        employeeNo,
        employeeNo);
  }

  private void givenBand(String employeeNo, String skill, String band) {
    jdbc.update(
        "INSERT INTO person_skill (employee_no, skill_id, band)"
            + " VALUES (?, (SELECT id FROM skill WHERE name = ?), ?)",
        employeeNo,
        skill,
        band);
  }

  private long givenProposal(long postId, String employeeNo) {
    jdbc.update(
        "INSERT INTO recommendation (post_id, person_no, state, snapshot_json, proposed_by)"
            + " VALUES (?, ?, 'proposed', '{}',"
            + " (SELECT id FROM app_user WHERE username = 'coordinator'))",
        postId,
        employeeNo);
    return jdbc.queryForObject(
        "SELECT MAX(id) FROM recommendation WHERE post_id = ?", Long.class, postId);
  }

  @SuppressWarnings("unchecked")
  private List<Map<String, Object>> suitableOf(long postId) {
    Map<String, Object> search = body(coordinator.get("/api/posts/" + postId + "/candidates", Map.class));
    return (List<Map<String, Object>>) search.get("suitable");
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> firstRecommendation(long postId) {
    List<Map<String, Object>> rows =
        coordinator.get("/api/posts/" + postId + "/recommendations", List.class).getBody();
    return rows.get(0);
  }

  private String column(String sql) {
    return jdbc.queryForObject(sql, String.class);
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
