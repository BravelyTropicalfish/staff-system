package com.example.staffing.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.staffing.support.ApiSession;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
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
 * T23, the confirmation: the lead of the post's project confirms under a serializable transaction
 * with the person row locked, G1 and G6 are re-checked there, the assignment and its allocation are
 * written for the post's dates, the post's status is re-derived, and the person's other proposals are
 * voided with `person taken` only where their post's dates intersect. Rejection and withdrawal free
 * the person without writing a seat.
 *
 * <p>Each test starts from a dropped and rebuilt schema; the context is discarded after the class
 * so no other class inherits this database.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class ConfirmationApiTest {

  private static final String ATLAS = "Atlas";
  private static final String BOREALIS = "Borealis";
  private static final String CETUS = "Cetus";

  @Autowired private TestRestTemplate rest;

  @Autowired private JdbcTemplate jdbc;

  private ApiSession coordinator;
  private ApiSession leadOne;
  private ApiSession leadTwo;

  @BeforeEach
  void freshSchema() throws Exception {
    try (var connection = jdbc.getDataSource().getConnection()) {
      connection.createStatement().execute("DROP ALL OBJECTS");
      ScriptUtils.executeSqlScript(connection, new ClassPathResource("schema.sql"));
      ScriptUtils.executeSqlScript(connection, new ClassPathResource("data.sql"));
    }
    coordinator = ApiSession.signIn(rest, "coordinator");
    leadOne = ApiSession.signIn(rest, "lead.one");
    leadTwo = ApiSession.signIn(rest, "lead.two");
  }

  @Test
  @SuppressWarnings("unchecked")
  void confirmationWritesOneAllocationForThePostsDates() {
    long post = qaPost(ATLAS, 2, "2027-05-01", "2027-07-31");
    givenPerson("E100", "QA", "Senior", true);
    givenBand("E100", "测试", "Practitioner");
    long proposal = givenProposal(post, "E100");
    String snapshotBefore = snapshotOf(proposal);

    ResponseEntity<Map> response = leadOne.post("/api/recommendations/" + proposal + "/confirm",
        null, Map.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    Map<String, Object> answer = body(response);
    assertThat((Map<String, Object>) answer.get("recommendation"))
        .containsEntry("state", "confirmed")
        .containsEntry("personNo", "E100");
    assertThat(answer).containsEntry("postStatus", "partially filled").containsEntry("remaining", 1);
    assertThat((List<?>) answer.get("voided")).isEmpty();

    assertThat(count("SELECT COUNT(*) FROM assignment WHERE post_id = " + post)).isEqualTo(1);
    assertThat(count("SELECT COUNT(*) FROM allocation WHERE source = 'CONFIRMATION'")).isEqualTo(1);
    assertThat(
            jdbc.queryForMap(
                "SELECT a.start_date, a.end_date, a.assignment_id FROM allocation a"
                    + " WHERE a.person_no = 'E100'"))
        .containsEntry("start_date", Date.valueOf("2027-05-01"))
        .containsEntry("end_date", Date.valueOf("2027-07-31"))
        .containsEntry("assignment_id", ((Number) answer.get("assignmentId")).longValue());
    assertThat(snapshotOf(proposal))
        .as("a confirmation never rewrites the reason snapshot")
        .isEqualTo(snapshotBefore);
  }

  @Test
  @SuppressWarnings("unchecked")
  void confirmationIsVisibleToBothRoles() {
    long post = qaPost(ATLAS, 2, "2027-05-01", "2027-07-31");
    givenPerson("E100", "QA", "Senior", true);
    givenBand("E100", "测试", "Practitioner");
    long proposal = givenProposal(post, "E100");
    long project = projectOf(post);

    assertThat(leadOne.post("/api/recommendations/" + proposal + "/confirm", null, Map.class)
            .getStatusCode())
        .isEqualTo(HttpStatus.OK);

    Map<String, Object> asLead = gapRow(leadOne, project, post);
    Map<String, Object> asCoordinator = gapRow(coordinator, project, post);
    assertThat(asLead).containsEntry("remaining", 1).containsEntry("status", "partially filled");
    assertThat(asCoordinator)
        .containsEntry("remaining", 1)
        .containsEntry("status", "partially filled");
    assertThat(asLead.get("people")).isEqualTo(asCoordinator.get("people"));
    assertThat((List<Map<String, Object>>) asLead.get("people"))
        .singleElement()
        .satisfies(row -> assertThat(row).containsEntry("personNo", "E100"));
  }

  @Test
  @SuppressWarnings("unchecked")
  void theConfirmedPersonLeavesOtherSearches() {
    long post = qaPost(ATLAS, 1, "2027-05-01", "2027-07-31");
    givenPerson("E100", "QA", "Senior", true);
    givenBand("E100", "测试", "Practitioner");
    long proposal = givenProposal(post, "E100");
    assertThat(leadOne.post("/api/recommendations/" + proposal + "/confirm", null, Map.class)
            .getStatusCode())
        .isEqualTo(HttpStatus.OK);

    long overlapping = qaPost(BOREALIS, 1, "2027-06-01", "2027-08-31");
    assertThat((List<?>) search(overlapping).get("suitable")).isEmpty();
    assertThat(excluded(overlapping))
        .singleElement()
        .satisfies(
            row -> {
              assertThat(row).containsEntry("employeeNo", "E100").containsEntry("gate", "G6");
              assertThat((String) row.get("reason"))
                  .isEqualTo(
                      "已有占用 2027-05-01 至 2027-07-31，与岗位的 2027-06-01 至 2027-08-31 相交");
            });

    long later = qaPost(CETUS, 1, "2027-09-01", "2027-10-31");
    assertThat((List<Map<String, Object>>) search(later).get("suitable"))
        .singleElement()
        .satisfies(row -> assertThat(row).containsEntry("employeeNo", "E100"));
  }

  @Test
  @SuppressWarnings("unchecked")
  void theOtherPendingProposalIsVoided() {
    long atlas = qaPost(ATLAS, 1, "2027-01-01", "2027-03-31");
    long borealis = qaPost(BOREALIS, 1, "2027-02-01", "2027-05-31");
    long cetus = qaPost(CETUS, 1, "2027-06-01", "2027-08-31");
    givenPerson("E100", "QA", "Senior", true);
    givenBand("E100", "测试", "Practitioner");
    long onAtlas = givenProposal(atlas, "E100");
    long onBorealis = givenProposal(borealis, "E100");
    long onCetus = givenProposal(cetus, "E100");

    ResponseEntity<Map> response =
        leadOne.post("/api/recommendations/" + onAtlas + "/confirm", null, Map.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat((List<Map<String, Object>>) body(response).get("voided"))
        .singleElement()
        .satisfies(
            row -> assertThat(row)
                .containsEntry("recommendationId", (int) onBorealis)
                .containsEntry("personNo", "E100")
                .containsEntry("reason", "person taken"));
    assertThat(stateOf(onBorealis)).isEqualTo("voided");
    assertThat(voidReasonOf(onBorealis)).isEqualTo("person taken");
    assertThat(stateOf(onCetus)).as("dates that do not intersect stay proposed").isEqualTo("proposed");
    assertThat(stateOf(onAtlas)).isEqualTo("confirmed");
    assertThat(
            jdbc.queryForList(
                "SELECT reason FROM notice WHERE post_id = ?", String.class, borealis))
        .containsExactly("person taken");
  }

  @Test
  void aPersonDeactivatedAfterTheProposal() {
    long post = qaPost(ATLAS, 1, "2027-05-01", "2027-07-31");
    givenPerson("E100", "QA", "Senior", true);
    givenBand("E100", "测试", "Practitioner");
    long proposal = givenProposal(post, "E100");
    jdbc.update("UPDATE person SET active = FALSE WHERE employee_no = 'E100'");

    ResponseEntity<Map> response =
        leadOne.post("/api/recommendations/" + proposal + "/confirm", null, Map.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(body(response))
        .containsEntry("code", "G1")
        .containsEntry("message", "该人员已停用");
    assertThat(stateOf(proposal)).isEqualTo("proposed");
    assertThat(count("SELECT COUNT(*) FROM assignment")).isZero();
  }

  @Test
  void anAllocationRecordedAfterTheProposal() {
    long post = qaPost(ATLAS, 1, "2027-05-01", "2027-07-31");
    givenPerson("E100", "QA", "Senior", true);
    givenBand("E100", "测试", "Practitioner");
    long proposal = givenProposal(post, "E100");
    long elsewhere = givenProject(BOREALIS, "lead.two");
    jdbc.update(
        "INSERT INTO allocation (person_no, project_id, start_date, end_date, source)"
            + " VALUES ('E100', ?, DATE '2027-04-01', DATE '2027-06-01', 'MANUAL')",
        elsewhere);

    ResponseEntity<Map> response =
        leadOne.post("/api/recommendations/" + proposal + "/confirm", null, Map.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(body(response))
        .containsEntry("code", "person taken")
        .containsEntry(
            "message",
            "已有占用 2027-04-01 至 2027-06-01，与岗位的 2027-05-01 至 2027-07-31 相交");
    assertThat(stateOf(proposal)).isEqualTo("proposed");
    assertThat(count("SELECT COUNT(*) FROM assignment")).isZero();
  }

  @Test
  void anotherLeadsPostIsOutOfReach() {
    long post = qaPost(ATLAS, 1, "2027-05-01", "2027-07-31");
    givenPerson("E100", "QA", "Senior", true);
    givenBand("E100", "测试", "Practitioner");
    long proposal = givenProposal(post, "E100");

    ResponseEntity<Map> otherLead =
        leadTwo.post("/api/recommendations/" + proposal + "/confirm", null, Map.class);
    ResponseEntity<Map> theCoordinator =
        coordinator.post("/api/recommendations/" + proposal + "/confirm", null, Map.class);

    assertThat(otherLead.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    assertThat(theCoordinator.getStatusCode()).as("the coordinator never confirms").isEqualTo(
        HttpStatus.FORBIDDEN);
    assertThat(stateOf(proposal)).isEqualTo("proposed");
    assertThat(count("SELECT COUNT(*) FROM assignment")).isZero();
    assertThat(count("SELECT COUNT(*) FROM allocation")).isZero();
  }

  @Test
  void aConcurrentDoubleConfirmation() throws Exception {
    long atlas = qaPost(ATLAS, 1, "2027-05-01", "2027-07-31");
    long borealis = qaPost(BOREALIS, 1, "2027-06-01", "2027-08-31");
    givenPerson("E100", "QA", "Senior", true);
    givenBand("E100", "测试", "Practitioner");
    long onAtlas = givenProposal(atlas, "E100");
    long onBorealis = givenProposal(borealis, "E100");

    ExecutorService pool = Executors.newFixedThreadPool(2);
    try {
      Future<ResponseEntity<Map>> first =
          pool.submit(
              () ->
                  leadOne.post("/api/recommendations/" + onAtlas + "/confirm", null, Map.class));
      Future<ResponseEntity<Map>> second =
          pool.submit(
              () ->
                  leadTwo.post("/api/recommendations/" + onBorealis + "/confirm", null, Map.class));
      List<ResponseEntity<Map>> answers = List.of(first.get(), second.get());

      assertThat(answers).filteredOn(r -> r.getStatusCode() == HttpStatus.OK).hasSize(1);
      ResponseEntity<Map> loser =
          answers.stream()
              .filter(r -> r.getStatusCode() == HttpStatus.CONFLICT)
              .findFirst()
              .orElseThrow();
      assertThat(body(loser)).containsEntry("code", "person taken");
    } finally {
      pool.shutdownNow();
    }

    assertThat(count("SELECT COUNT(*) FROM assignment WHERE person_no = 'E100'")).isEqualTo(1);
    assertThat(count("SELECT COUNT(*) FROM allocation WHERE person_no = 'E100'")).isEqualTo(1);
    assertThat(count("SELECT COUNT(*) FROM recommendation WHERE state = 'confirmed'")).isEqualTo(1);
    assertThat(count("SELECT COUNT(*) FROM recommendation WHERE state = 'voided'")).isEqualTo(1);
  }

  @Test
  void aLaterPostingSucceedsAnOverlappingOneWaits() {
    long atlas = qaPost(ATLAS, 1, "2027-01-01", "2027-03-31");
    long borealis = qaPost(BOREALIS, 1, "2027-02-01", "2027-05-31");
    givenPerson("E100", "QA", "Senior", true);
    givenBand("E100", "测试", "Practitioner");
    long onBorealis = givenProposal(borealis, "E100");
    // The Atlas seat is allocated after the Borealis proposal, so it never voids that proposal.
    occupy("E100", projectOf(atlas), "2027-01-01", "2027-03-31");

    ResponseEntity<Map> overlapping =
        leadTwo.post("/api/recommendations/" + onBorealis + "/confirm", null, Map.class);
    assertThat(overlapping.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(body(overlapping)).containsEntry("code", "person taken");
    assertThat(stateOf(onBorealis)).isEqualTo("proposed");

    jdbc.update("DELETE FROM allocation WHERE person_no = 'E100'");
    assertThat(leadTwo.post("/api/recommendations/" + onBorealis + "/confirm", null, Map.class)
            .getStatusCode())
        .isEqualTo(HttpStatus.OK);

    // A post that opens after the occupied range needs no release at all.
    givenPerson("E200", "QA", "Senior", true);
    givenBand("E200", "测试", "Practitioner");
    long cetus = givenProject(CETUS, "lead.two");
    occupy("E200", cetus, "2027-01-01", "2027-03-31");
    long later = qaPostIn(cetus, 1, "2027-04-01", "2027-05-31");
    long onLater = givenProposal(later, "E200");
    assertThat(leadTwo.post("/api/recommendations/" + onLater + "/confirm", null, Map.class)
            .getStatusCode())
        .isEqualTo(HttpStatus.OK);
  }

  @Test
  void aRejectionFreesTheCandidate() {
    long post = qaPost(ATLAS, 1, "2027-05-01", "2027-07-31");
    givenPerson("E100", "QA", "Senior", true);
    givenBand("E100", "测试", "Practitioner");
    long proposal = givenProposal(post, "E100");

    ResponseEntity<Map> response =
        leadOne.post(
            "/api/recommendations/" + proposal + "/reject", Map.of("note", "band too thin"), Map.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(body(response)).containsEntry("state", "rejected");
    assertThat(
            jdbc.queryForObject(
                "SELECT note FROM recommendation WHERE id = ?", String.class, proposal))
        .isEqualTo("band too thin");
    assertThat(count("SELECT COUNT(*) FROM assignment")).isZero();

    long otherProject = qaPost(BOREALIS, 1, "2027-05-01", "2027-07-31");
    assertThat(employeeNumbers(search(otherProject).get("suitable"))).containsExactly("E100");
  }

  @Test
  void theCoordinatorWithdrawsAProposal() {
    long post = qaPost(ATLAS, 1, "2027-05-01", "2027-07-31");
    givenPerson("E100", "QA", "Senior", true);
    givenBand("E100", "测试", "Practitioner");
    long proposal = givenProposal(post, "E100");

    assertThat(leadOne.delete("/api/recommendations/" + proposal, Map.class).getStatusCode())
        .as("the lead does not withdraw the coordinator's proposal")
        .isEqualTo(HttpStatus.FORBIDDEN);

    ResponseEntity<Map> withdrawn =
        coordinator.delete("/api/recommendations/" + proposal, Map.class);
    assertThat(withdrawn.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(body(withdrawn)).containsEntry("state", "withdrawn");
    assertThat(count("SELECT COUNT(*) FROM assignment")).isZero();
    assertThat(coordinator.delete("/api/recommendations/" + proposal, Map.class).getStatusCode())
        .isEqualTo(HttpStatus.CONFLICT);
  }

  @Test
  void aFilledPostTakesNoFurtherConfirmation() {
    long post = qaPost(ATLAS, 1, "2027-05-01", "2027-07-31");
    givenPerson("E100", "QA", "Senior", true);
    givenBand("E100", "测试", "Practitioner");
    givenPerson("E101", "QA", "Senior", true);
    givenBand("E101", "测试", "Practitioner");
    long first = givenProposal(post, "E100");
    long second = givenProposal(post, "E101");

    assertThat(leadOne.post("/api/recommendations/" + first + "/confirm", null, Map.class)
            .getStatusCode())
        .isEqualTo(HttpStatus.OK);
    ResponseEntity<Map> refused =
        leadOne.post("/api/recommendations/" + second + "/confirm", null, Map.class);

    assertThat(refused.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(body(refused))
        .containsEntry("code", "post filled")
        .containsEntry("message", "岗位已满，没有可确认的名额");
    assertThat(stateOf(second)).isEqualTo("proposed");
    assertThat(count("SELECT COUNT(*) FROM assignment")).isEqualTo(1);
  }

  @Test
  void aProposalBelongsToTheCoordinatorWhoMadeIt() {
    jdbc.update(
        "INSERT INTO app_user (username, password_hash, role, display_name)"
            + " SELECT 'coordinator.two', password_hash, 'COORDINATOR', 'Coordinator two'"
            + " FROM app_user WHERE username = 'coordinator'");
    long post = qaPost(ATLAS, 1, "2027-05-01", "2027-07-31");
    givenPerson("E100", "QA", "Senior", true);
    givenBand("E100", "测试", "Practitioner");
    jdbc.update(
        "INSERT INTO recommendation (post_id, person_no, state, snapshot_json, proposed_by)"
            + " VALUES (?, 'E100', 'proposed', '{}',"
            + " (SELECT id FROM app_user WHERE username = 'coordinator.two'))",
        post);
    long proposal =
        jdbc.queryForObject(
            "SELECT MAX(id) FROM recommendation WHERE post_id = ?", Long.class, post);

    ResponseEntity<Map> refused = coordinator.delete("/api/recommendations/" + proposal, Map.class);
    ApiSession other = ApiSession.signIn(rest, "coordinator.two");
    ResponseEntity<Map> withdrawn = other.delete("/api/recommendations/" + proposal, Map.class);

    assertThat(refused.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    assertThat(body(refused)).containsEntry("code", "forbidden");
    assertThat(withdrawn.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(body(withdrawn)).containsEntry("state", "withdrawn");
    assertThat(stateOf(proposal)).isEqualTo("withdrawn");
  }

  // --- fixtures -------------------------------------------------------------------------------

  private long qaPost(String projectName, int headcount, String start, String end) {
    return qaPostIn(givenProject(projectName, leadOf(projectName)), headcount, start, end);
  }

  private long qaPostIn(long projectId, int headcount, String start, String end) {
    long post = givenPost(projectId, headcount, start, end);
    jdbc.update(
        "INSERT INTO post_skill (post_id, skill_id, kind, min_band)"
            + " VALUES (?, (SELECT id FROM skill WHERE name = '测试'), 'REQUIRED', 'Practitioner')",
        post);
    return post;
  }

  /** Atlas belongs to lead.one, the other two projects to lead.two. */
  private static String leadOf(String projectName) {
    return ATLAS.equals(projectName) ? "lead.one" : "lead.two";
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

  private long givenPost(long projectId, int headcount, String start, String end) {
    jdbc.update(
        "INSERT INTO post (project_id, title, role_id, min_job_level, headcount,"
            + " start_date, end_date, travel_required, status)"
            + " VALUES (?, NULL, (SELECT id FROM role WHERE name = 'QA'), 'Senior', ?, ?, ?,"
            + " 'full', 'open')",
        projectId,
        headcount,
        Date.valueOf(LocalDate.parse(start)),
        Date.valueOf(LocalDate.parse(end)));
    return jdbc.queryForObject(
        "SELECT MAX(id) FROM post WHERE project_id = ?", Long.class, projectId);
  }

  private void givenPerson(String employeeNo, String role, String level, boolean active) {
    jdbc.update(
        "INSERT INTO person (employee_no, name, email, role_id, job_level, travel, active)"
            + " VALUES (?, ?, NULL, (SELECT id FROM role WHERE name = ?), ?, 'full', ?)",
        employeeNo,
        employeeNo,
        role,
        level,
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

  private long givenProposal(long postId, String personNo) {
    ResponseEntity<Map> response =
        coordinator.post(
            "/api/posts/" + postId + "/recommendations", Map.of("personNo", personNo), Map.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    return ((Number) body(response).get("id")).longValue();
  }

  /** A hand-entered occupancy, the kind a coordinator records without a confirmation. */
  private void occupy(String personNo, long projectId, String start, String end) {
    jdbc.update(
        "INSERT INTO allocation (person_no, project_id, start_date, end_date, source)"
            + " VALUES (?, ?, ?, ?, 'MANUAL')",
        personNo,
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

  private List<Map<String, Object>> excluded(long postId) {
    return castList(search(postId).get("excluded"));
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> gapRow(ApiSession session, long projectId, long postId) {
    ResponseEntity<List> response = session.get("/api/projects/" + projectId + "/gap", List.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    List<Map<String, Object>> rows = (List<Map<String, Object>>) response.getBody();
    return rows.stream()
        .filter(row -> ((Number) row.get("id")).longValue() == postId)
        .findFirst()
        .orElseThrow();
  }

  private long projectOf(long postId) {
    return jdbc.queryForObject("SELECT project_id FROM post WHERE id = ?", Long.class, postId);
  }

  private String stateOf(long recommendationId) {
    return jdbc.queryForObject(
        "SELECT state FROM recommendation WHERE id = ?", String.class, recommendationId);
  }

  private String voidReasonOf(long recommendationId) {
    return jdbc.queryForObject(
        "SELECT void_reason FROM recommendation WHERE id = ?", String.class, recommendationId);
  }

  private String snapshotOf(long recommendationId) {
    return jdbc.queryForObject(
        "SELECT snapshot_json FROM recommendation WHERE id = ?", String.class, recommendationId);
  }

  private int count(String sql) {
    Integer value = jdbc.queryForObject(sql, Integer.class);
    return value == null ? 0 : value;
  }

  private static List<Object> employeeNumbers(Object suitable) {
    return castList(suitable).stream().map(row -> row.get("employeeNo")).toList();
  }

  @SuppressWarnings("unchecked")
  private static List<Map<String, Object>> castList(Object rows) {
    return (List<Map<String, Object>>) rows;
  }

  @SuppressWarnings("unchecked")
  private static Map<String, Object> body(ResponseEntity<Map> response) {
    return (Map<String, Object>) response.getBody();
  }
}
