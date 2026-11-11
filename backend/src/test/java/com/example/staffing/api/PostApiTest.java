package com.example.staffing.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.staffing.support.ApiSession;
import java.time.LocalDate;
import java.util.HashMap;
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
 * T15, posts: the fields validated on save, the status the confirmed count derives, the gate-bearing
 * lock and the headcount ceiling, the changes that void pending recommendations, cancel and its
 * read-only end, and the actor rules that keep the coordinator to reading.
 *
 * <p>Each test starts from a dropped and rebuilt schema; the context is discarded after the class
 * so no other class inherits this database.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class PostApiTest {

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
  void aPostIsReadableByItsLeadAndTheCoordinator() {
    long project = givenProject("Atlas", "lead.one");
    long post = givenPost(project, "DEV", 2);
    givenPerson("E100");
    givenConfirmed(post, "E100");

    for (ApiSession reader : List.of(lead, coordinator)) {
      ResponseEntity<Map> response = reader.get("/api/posts/" + post, Map.class);
      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
      assertThat(body(response))
          .containsEntry("id", (int) post)
          .containsEntry("projectId", (int) project)
          .containsEntry("status", "partially filled")
          .containsEntry("headcount", 2)
          .containsEntry("confirmed", 1)
          .containsEntry("remaining", 1);
    }
  }

  @Test
  void anotherLeadsPostIsOutOfReachForTheSingleRead() {
    long post = givenPost(givenProject("Atlas", "lead.one"), "DEV", 1);
    ApiSession other = ApiSession.signIn(rest, "lead.two");

    ResponseEntity<Map> refused = other.get("/api/posts/" + post, Map.class);

    assertThat(refused.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    assertThat(body(refused)).containsEntry("code", "forbidden");
    assertThat(lead.get("/api/posts/9999", Map.class).getStatusCode())
        .isEqualTo(HttpStatus.NOT_FOUND);
  }

  @Test
  void aValidPostIsCreatedOpen() {
    long atlas = givenProject("Atlas", "lead.one");

    ResponseEntity<Map> response =
        lead.post("/api/projects/" + atlas + "/posts", post("DEV", "Senior", 2, "limited"), Map.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(body(response))
        .containsEntry("project", "Atlas")
        .containsEntry("role", "DEV")
        .containsEntry("minJobLevel", "Senior")
        .containsEntry("headcount", 2)
        .containsEntry("confirmed", 0)
        .containsEntry("remaining", 2)
        .containsEntry("status", "open")
        .containsEntry("travel", "limited")
        .containsEntry("startDate", "2027-02-01")
        .containsEntry("endDate", "2027-06-30");
    assertThat(postStatus(id(response))).isEqualTo("open");
  }

  @Test
  void aFractionalHeadcountIsRefused() {
    long atlas = givenProject("Atlas", "lead.one");

    ResponseEntity<Map> response =
        lead.post("/api/projects/" + atlas + "/posts", post("DEV", "Senior", 1.5, "none"), Map.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    assertThat(body(response))
        .containsEntry("code", "invalid post")
        .containsEntry("message", "headcount must be a whole number of at least 1");
    assertThat(count("SELECT COUNT(*) FROM post")).isZero();
  }

  @Test
  void aDeactivatedRoleDoesNotBlockTheTitle() {
    long atlas = givenProject("Atlas", "lead.one");
    long post =
        id(lead.post("/api/projects/" + atlas + "/posts", post("QA", "Senior", 1, "none"), Map.class));
    jdbc.update("UPDATE role SET active = FALSE WHERE name = 'QA'");
    jdbc.update("INSERT INTO role (name, active) VALUES ('OPS', FALSE)");

    ResponseEntity<Map> renamed =
        lead.patch("/api/posts/" + post, Map.of("title", "Regression lead"), Map.class);
    ResponseEntity<Map> moved = lead.patch("/api/posts/" + post, Map.of("role", "OPS"), Map.class);

    assertThat(renamed.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(body(renamed))
        .containsEntry("title", "Regression lead")
        .containsEntry("role", "QA");
    assertThat(moved.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    assertThat(body(moved)).containsEntry("code", "invalid post");
    assertThat(storedRole(post)).isEqualTo("QA");
  }

  @Test
  @SuppressWarnings("unchecked")
  void requiredAndBonusRowsAreDistinguished() {
    long atlas = givenProject("Atlas", "lead.one");
    Map<String, Object> body = post("DEV", "Senior", 1, "none");
    body.put(
        "skills",
        List.of(
            skillRow("后端开发", "required", "Practitioner"),
            skillRow("测试", "bonus", "Practitioner")));

    ResponseEntity<Map> response = lead.post("/api/projects/" + atlas + "/posts", body, Map.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    List<Map<String, Object>> skills = (List<Map<String, Object>>) body(response).get("skills");
    assertThat(skills)
        .containsExactly(
            Map.of(
                "skill", "后端开发", "kind", "required", "band", "Practitioner", "informational", false),
            Map.of("skill", "测试", "kind", "bonus", "band", "Practitioner", "informational", true));
  }

  @Test
  void aPostWithoutSkillsIsValid() {
    long atlas = givenProject("Atlas", "lead.one");

    ResponseEntity<Map> response =
        lead.post("/api/projects/" + atlas + "/posts", post("DEV", "Senior", 1, "none"), Map.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat((List<?>) body(response).get("skills")).isEmpty();
    assertThat(count("SELECT COUNT(*) FROM post WHERE id = " + id(response))).isEqualTo(1);
  }

  @Test
  void fillingTheLastSlotLeavesTheStillShortList() {
    long atlas = givenProject("Atlas", "lead.one");
    long post = givenPost(atlas, "DEV", 2);
    givenPerson("E100");
    givenConfirmed(post, "E100");

    assertThat(gapRow(atlas, post))
        .containsEntry("status", "partially filled")
        .containsEntry("confirmed", 1)
        .containsEntry("remaining", 1)
        .containsEntry("stillShort", true);

    givenPerson("E101");
    givenConfirmed(post, "E101");

    assertThat(gapRow(atlas, post))
        .containsEntry("status", "filled")
        .containsEntry("confirmed", 2)
        .containsEntry("remaining", 0)
        .containsEntry("stillShort", false);
  }

  @Test
  @SuppressWarnings("unchecked")
  void anInactiveConfirmedPersonKeepsThePostFilled() {
    long atlas = givenProject("Atlas", "lead.one");
    long post = givenPost(atlas, "DEV", 1);
    givenPerson("E100");
    givenConfirmed(post, "E100");
    jdbc.update("UPDATE person SET active = FALSE WHERE employee_no = 'E100'");

    Map<String, Object> row = gapRow(atlas, post);

    assertThat(row).containsEntry("status", "filled").containsEntry("stillShort", false);
    assertThat((List<Map<String, Object>>) row.get("people"))
        .singleElement()
        .satisfies(
            person ->
                assertThat(person).containsEntry("personNo", "E100").containsEntry("active", false));
  }

  @Test
  void datesCannotMoveUnderAConfirmedPerson() {
    long atlas = givenProject("Atlas", "lead.one");
    long post = givenPost(atlas, "DEV", 1);
    givenPerson("E100");
    givenConfirmed(post, "E100");
    givenPerson("E101");
    long pending = givenPending(post, "E101");

    ResponseEntity<Map> response =
        lead.patch("/api/posts/" + post, Map.of("endDate", "2027-07-31"), Map.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(body(response)).containsEntry("code", "release first");
    assertThat(body(response).get("message").toString()).contains("E100").contains("release");
    assertThat(postEndDate(post)).isEqualTo(LocalDate.parse("2027-06-30"));
    assertThat(jdbc.queryForObject("SELECT state FROM recommendation WHERE id = ?", String.class, pending))
        .isEqualTo("proposed");
    assertThat(count("SELECT COUNT(*) FROM assignment WHERE post_id = " + post)).isEqualTo(1);
    assertThat(allocationEndDate(post)).isEqualTo(LocalDate.parse("2027-06-30"));
  }

  @Test
  void theLockSpreadsToEveryGateBearingField() {
    long atlas = givenProject("Atlas", "lead.one");
    long post = givenPost(atlas, "DEV", 1);
    jdbc.update(
        "INSERT INTO post_skill (post_id, skill_id, kind, min_band, target_band)"
            + " SELECT ?, id, 'REQUIRED', 'Practitioner', NULL FROM skill WHERE name = '后端开发'",
        post);
    givenPerson("E100");
    givenConfirmed(post, "E100");
    String path = "/api/posts/" + post;

    assertReleaseFirst(lead.patch(path, Map.of("role", "QA"), Map.class));
    assertReleaseFirst(lead.patch(path, Map.of("minJobLevel", "Lead"), Map.class));
    assertReleaseFirst(
        lead.patch(
            path, Map.of("skills", List.of(skillRow("后端开发", "required", "Expert"))), Map.class));
    assertReleaseFirst(lead.patch(path, Map.of("startDate", "2027-02-15"), Map.class));
    assertReleaseFirst(lead.patch(path, Map.of("travel", "full"), Map.class));

    assertThat(storedRole(post)).isEqualTo("DEV");
    assertThat(
            jdbc.queryForObject(
                "SELECT min_job_level FROM post WHERE id = ?", String.class, post))
        .isEqualTo("Senior");
    assertThat(
            jdbc.queryForObject("SELECT start_date FROM post WHERE id = ?", java.sql.Date.class, post)
                .toLocalDate())
        .isEqualTo(LocalDate.parse("2027-02-01"));
    assertThat(
            jdbc.queryForObject(
                "SELECT travel_required FROM post WHERE id = ?", String.class, post))
        .isEqualTo("none");
    assertThat(
            jdbc.queryForObject(
                "SELECT min_band FROM post_skill WHERE post_id = ? AND kind = 'REQUIRED'",
                String.class,
                post))
        .isEqualTo("Practitioner");
  }

  @Test
  void raisingTheHeadcountReopensAFilledPost() {
    long atlas = givenProject("Atlas", "lead.one");
    long post = givenPost(atlas, "DEV", 2);
    givenPerson("E100");
    givenConfirmed(post, "E100");
    givenPerson("E101");
    givenConfirmed(post, "E101");
    assertThat(gapRow(atlas, post)).containsEntry("status", "filled");

    ResponseEntity<Map> response = lead.patch("/api/posts/" + post, Map.of("headcount", 3), Map.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(body(response))
        .containsEntry("status", "partially filled")
        .containsEntry("headcount", 3)
        .containsEntry("remaining", 1);
    assertThat(gapRow(atlas, post)).containsEntry("stillShort", true).containsEntry("remaining", 1);
  }

  @Test
  void loweringBelowTheConfirmedCountIsRefused() {
    long atlas = givenProject("Atlas", "lead.one");
    long post = givenPost(atlas, "DEV", 2);
    givenPerson("E100");
    givenConfirmed(post, "E100");
    givenPerson("E101");
    givenConfirmed(post, "E101");

    ResponseEntity<Map> response = lead.patch("/api/posts/" + post, Map.of("headcount", 1), Map.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(body(response)).containsEntry("code", "release first");
    assertThat(
            jdbc.queryForObject("SELECT headcount FROM post WHERE id = ?", Integer.class, post))
        .isEqualTo(2);
    assertThat(count("SELECT COUNT(*) FROM assignment WHERE post_id = " + post)).isEqualTo(2);
  }

  @Test
  @SuppressWarnings("unchecked")
  void aBonusTargetChangeVoidsOnlyPendingRecommendations() {
    long atlas = givenProject("Atlas", "lead.one");
    long post = givenPost(atlas, "DEV", 2);
    jdbc.update(
        "INSERT INTO post_skill (post_id, skill_id, kind, min_band, target_band)"
            + " SELECT ?, id, 'BONUS', NULL, 'Practitioner' FROM skill WHERE name = '测试'",
        post);
    givenPerson("E100");
    givenConfirmed(post, "E100");
    givenPerson("E101");
    long pending = givenPending(post, "E101");

    ResponseEntity<Map> response =
        lead.patch(
            "/api/posts/" + post,
            Map.of("skills", List.of(skillRow("测试", "bonus", "Proficient"))),
            Map.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat((List<Map<String, Object>>) body(response).get("skills"))
        .singleElement()
        .satisfies(
            skill ->
                assertThat(skill)
                    .containsEntry("kind", "bonus")
                    .containsEntry("band", "Proficient"));
    assertThat(jdbc.queryForObject("SELECT state FROM recommendation WHERE id = ?", String.class, pending))
        .isEqualTo("voided");
    assertThat(
            jdbc.queryForObject(
                "SELECT void_reason FROM recommendation WHERE id = ?", String.class, pending))
        .isEqualTo("post changed");
    assertThat(
            count(
                "SELECT COUNT(*) FROM notice WHERE post_id = "
                    + post
                    + " AND person_no = 'E101' AND reason = 'post changed'"))
        .isEqualTo(1);
    assertThat(count("SELECT COUNT(*) FROM assignment WHERE post_id = " + post)).isEqualTo(1);
    assertThat(count("SELECT COUNT(*) FROM allocation WHERE person_no = 'E100'")).isEqualTo(1);
    assertThat(allocationEndDate(post)).isEqualTo(LocalDate.parse("2027-06-30"));
  }

  @Test
  void cancelIsRefusedThenAllowed() {
    long atlas = givenProject("Atlas", "lead.one");
    long post = givenPost(atlas, "DEV", 1);
    givenPerson("E100");
    givenConfirmed(post, "E100");
    givenPerson("E101");
    givenPending(post, "E101");
    String path = "/api/posts/" + post + "/cancel";

    ResponseEntity<Map> refused = lead.post(path, null, Map.class);

    assertThat(refused.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(body(refused)).containsEntry("code", "release first");
    assertThat(body(refused).get("message").toString()).contains("E100");
    assertThat(postStatus(post)).isEqualTo("open");

    // The release is T24's route; here the fixture removes the seat the way a release does.
    jdbc.update(
        "DELETE FROM allocation WHERE assignment_id IN"
            + " (SELECT id FROM assignment WHERE post_id = ?)",
        post);
    jdbc.update("DELETE FROM assignment WHERE post_id = ?", post);

    ResponseEntity<Map> cancelled = lead.post(path, null, Map.class);

    assertThat(cancelled.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(body(cancelled)).containsEntry("status", "cancelled").containsEntry("remaining", 0);
    assertThat(
            jdbc.queryForObject(
                "SELECT void_reason FROM recommendation WHERE post_id = ?", String.class, post))
        .isEqualTo("post cancelled");
    assertThat(
            count("SELECT COUNT(*) FROM notice WHERE post_id = " + post + " AND reason = 'post cancelled'"))
        .isEqualTo(1);

    ResponseEntity<Map> edit = lead.patch("/api/posts/" + post, Map.of("title", "Backend"), Map.class);
    ResponseEntity<Map> again = lead.post(path, null, Map.class);

    assertThat(edit.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(body(edit)).containsEntry("code", "post cancelled");
    assertThat(jdbc.queryForObject("SELECT title FROM post WHERE id = ?", String.class, post))
        .isNull();
    assertThat(again.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(body(again)).containsEntry("code", "post cancelled");
  }

  @Test
  void aCoordinatorCannotEditAPost() {
    long atlas = givenProject("Atlas", "lead.one");
    long post = givenPost(atlas, "DEV", 1);

    ResponseEntity<List> posts = coordinator.get("/api/projects/" + atlas + "/posts", List.class);
    ResponseEntity<List> gap = coordinator.get("/api/projects/" + atlas + "/gap", List.class);
    ResponseEntity<Map> create =
        coordinator.post("/api/projects/" + atlas + "/posts", post("DEV", "Senior", 1, "none"), Map.class);
    ResponseEntity<Map> save = coordinator.patch("/api/posts/" + post, Map.of("title", "x"), Map.class);
    ResponseEntity<Map> cancel = coordinator.post("/api/posts/" + post + "/cancel", null, Map.class);

    assertThat(posts.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(posts.getBody()).hasSize(1);
    assertThat(gap.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(create.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    assertThat(save.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    assertThat(cancel.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    assertThat(count("SELECT COUNT(*) FROM post")).isEqualTo(1);
    assertThat(jdbc.queryForObject("SELECT title FROM post WHERE id = ?", String.class, post))
        .isNull();
    assertThat(postStatus(post)).isEqualTo("open");
  }

  @Test
  void onlyTheOwningLeadWritesAPost() {
    long atlas = givenProject("Atlas", "lead.one");
    long post = givenPost(atlas, "DEV", 1);
    ApiSession other = ApiSession.signIn(rest, "lead.two");

    assertThat(other.get("/api/projects/" + atlas + "/posts", Map.class).getStatusCode())
        .isEqualTo(HttpStatus.FORBIDDEN);
    assertThat(other.get("/api/projects/" + atlas + "/gap", Map.class).getStatusCode())
        .isEqualTo(HttpStatus.FORBIDDEN);
    assertThat(
            other
                .post(
                    "/api/projects/" + atlas + "/posts",
                    post("DEV", "Senior", 1, "none"),
                    Map.class)
                .getStatusCode())
        .isEqualTo(HttpStatus.FORBIDDEN);
    assertThat(other.patch("/api/posts/" + post, Map.of("title", "x"), Map.class).getStatusCode())
        .isEqualTo(HttpStatus.FORBIDDEN);
    assertThat(other.post("/api/posts/" + post + "/cancel", null, Map.class).getStatusCode())
        .isEqualTo(HttpStatus.FORBIDDEN);
    assertThat(count("SELECT COUNT(*) FROM post")).isEqualTo(1);
  }

  @Test
  void aClosedProjectTakesNoNewPost() {
    long atlas = givenProject("Atlas", "lead.one");
    lead.post("/api/projects/" + atlas + "/end", null, Map.class);

    ResponseEntity<Map> response =
        lead.post("/api/projects/" + atlas + "/posts", post("DEV", "Senior", 1, "none"), Map.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(body(response))
        .containsEntry("code", "project closed")
        .containsEntry("message", "the project Atlas is closed");
    assertThat(count("SELECT COUNT(*) FROM post")).isZero();
  }

  @Test
  @SuppressWarnings("unchecked")
  void theGapListOmitsACancelledPost() {
    long atlas = givenProject("Atlas", "lead.one");
    long kept = givenPost(atlas, "DEV", 1);
    long cancelled = givenPost(atlas, "QA", 1);
    lead.post("/api/posts/" + cancelled + "/cancel", null, Map.class);

    List<Map<String, Object>> posts =
        lead.get("/api/projects/" + atlas + "/posts", List.class).getBody();
    List<Map<String, Object>> gap =
        lead.get("/api/projects/" + atlas + "/gap", List.class).getBody();

    assertThat(posts)
        .extracting(row -> ((Number) row.get("id")).longValue())
        .containsExactlyInAnyOrder(kept, cancelled);
    assertThat(gap).singleElement().satisfies(row -> assertThat(row).containsEntry("id", (int) kept));
  }

  @Test
  void aSaveThatChangesNothingVoidsNoProposal() {
    long atlas = givenProject("Atlas", "lead.one");
    long post = givenPost(atlas, "DEV", 1);
    jdbc.update("UPDATE post SET title = 'Backend' WHERE id = ?", post);
    jdbc.update(
        "INSERT INTO post_skill (post_id, skill_id, kind, min_band)"
            + " SELECT ?, id, 'REQUIRED', 'Practitioner' FROM skill WHERE name = '后端开发'",
        post);
    givenPerson("E100");
    long pending = givenPending(post, "E100");
    Map<String, Object> same = new HashMap<>();
    same.put("title", "Backend");
    same.put("role", "DEV");
    same.put("minJobLevel", "Senior");
    same.put("headcount", 1);
    same.put("startDate", "2027-02-01");
    same.put("endDate", "2027-06-30");
    same.put("travel", "none");
    same.put("skills", List.of(skillRow("后端开发", "required", "Practitioner")));

    ResponseEntity<Map> response = lead.patch("/api/posts/" + post, same, Map.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(
            jdbc.queryForObject(
                "SELECT state FROM recommendation WHERE id = ?", String.class, pending))
        .isEqualTo("proposed");
    assertThat(count("SELECT COUNT(*) FROM notice")).isZero();
    assertThat(
            jdbc.queryForObject("SELECT title FROM post WHERE id = ?", String.class, post))
        .isEqualTo("Backend");
  }

  @Test
  void anUnknownRoleAndAnUnknownSkillAreRefused() {
    long atlas = givenProject("Atlas", "lead.one");
    long post = givenPost(atlas, "DEV", 1);

    ResponseEntity<Map> role = lead.patch("/api/posts/" + post, Map.of("role", "NOPE"), Map.class);
    Map<String, Object> unknownSkill = post("DEV", "Senior", 1, "none");
    unknownSkill.put("skills", List.of(skillRow("NOPE", "required", "Practitioner")));
    Map<String, Object> namelessSkill = post("DEV", "Senior", 1, "none");
    namelessSkill.put("skills", List.of(skillRow(null, "required", "Practitioner")));

    ResponseEntity<Map> skill =
        lead.post("/api/projects/" + atlas + "/posts", unknownSkill, Map.class);
    ResponseEntity<Map> nameless =
        lead.post("/api/projects/" + atlas + "/posts", namelessSkill, Map.class);

    assertThat(role.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    assertThat(body(role)).containsEntry("message", "role NOPE is not in the catalog");
    assertThat(skill.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    assertThat(body(skill)).containsEntry("message", "skill NOPE is not in the catalog");
    assertThat(nameless.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    assertThat(body(nameless)).containsEntry("message", "skill  is not in the catalog");
    assertThat(count("SELECT COUNT(*) FROM post")).isEqualTo(1);
  }

  @Test
  void aSkillRowRepeatedInTheDraftIsRefused() {
    long atlas = givenProject("Atlas", "lead.one");
    Map<String, Object> body = post("DEV", "Senior", 1, "none");
    body.put(
        "skills",
        List.of(skillRow("测试", "required", "Practitioner"), skillRow("测试", "bonus", "Expert")));

    ResponseEntity<Map> response = lead.post("/api/projects/" + atlas + "/posts", body, Map.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    assertThat(body(response)).containsEntry("message", "skill 测试 appears twice");
    assertThat(count("SELECT COUNT(*) FROM post")).isZero();
  }

  @Test
  void aSkillRowNeedsAKnownKindAndABandFromTheFive() {
    long atlas = givenProject("Atlas", "lead.one");

    assertThat(refusalFor(atlas, skillOnly(skillRow("测试", null, "Practitioner"))))
        .containsEntry("message", "a skill row is required or bonus, not null");
    assertThat(refusalFor(atlas, skillOnly(skillRow("测试", "must", "Practitioner"))))
        .containsEntry("message", "a skill row is required or bonus, not must");
    assertThat(refusalFor(atlas, skillOnly(skillRow("测试", "required", null))))
        .containsEntry("message", " is not one of the five proficiency bands");
    assertThat(refusalFor(atlas, skillOnly(skillRow("测试", "bonus", "Guru"))))
        .containsEntry("message", "Guru is not one of the five proficiency bands");
    assertThat(count("SELECT COUNT(*) FROM post")).isZero();
  }

  @Test
  void theJobLevelTheTravelAndTheRangeAreCheckedOnSave() {
    long atlas = givenProject("Atlas", "lead.one");

    assertThat(refusalOf(lead.post("/api/projects/" + atlas + "/posts", post("DEV", "Guru", 1, "none"), Map.class)))
        .containsEntry("message", "Guru is not one of the four job level bands");
    assertThat(refusalOf(lead.post("/api/projects/" + atlas + "/posts", post("DEV", "Senior", 1, "sometimes"), Map.class)))
        .containsEntry("message", "sometimes is not one of the three travel bands");

    Map<String, Object> inverted = post("DEV", "Senior", 1, "none");
    inverted.put("startDate", "2027-06-30");
    inverted.put("endDate", "2027-02-01");
    assertThat(refusalOf(lead.post("/api/projects/" + atlas + "/posts", inverted, Map.class)))
        .containsEntry("message", "the end date cannot precede the start date");
    assertThat(count("SELECT COUNT(*) FROM post")).isZero();
  }

  @Test
  void theHeadcountIsAWholeNumberWithinAnInt() {
    long atlas = givenProject("Atlas", "lead.one");

    assertThat(refusalOf(lead.post("/api/projects/" + atlas + "/posts", post("DEV", "Senior", 0, "none"), Map.class)))
        .containsEntry("message", "headcount must be a whole number of at least 1");
    assertThat(
            refusalOf(
                lead.post(
                    "/api/projects/" + atlas + "/posts",
                    post("DEV", "Senior", new java.math.BigDecimal("100000000000000000000"), "none"),
                    Map.class)))
        .containsEntry("message", "headcount is too large");
    assertThat(count("SELECT COUNT(*) FROM post")).isZero();
  }

  @Test
  void aBlankTitleIsStoredAsNoTitle() {
    long atlas = givenProject("Atlas", "lead.one");
    Map<String, Object> body = post("DEV", "Senior", 1, "none");
    body.put("title", "   ");

    ResponseEntity<Map> response = lead.post("/api/projects/" + atlas + "/posts", body, Map.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(body(response).get("title")).isNull();
  }

  private static Map<String, Object> post(
      String role, String level, Object headcount, String travel) {
    Map<String, Object> body = new HashMap<>();
    body.put("role", role);
    body.put("minJobLevel", level);
    body.put("headcount", headcount);
    body.put("startDate", "2027-02-01");
    body.put("endDate", "2027-06-30");
    body.put("travel", travel);
    return body;
  }

  private static Map<String, Object> skillRow(String skill, String kind, String band) {
    Map<String, Object> row = new HashMap<>();
    row.put("skill", skill);
    if (kind != null) {
      row.put("kind", kind);
    }
    if (band != null) {
      row.put("band", band);
    }
    return row;
  }

  private static Map<String, Object> skillOnly(Map<String, Object> row) {
    Map<String, Object> body = post("DEV", "Senior", 1, "none");
    body.put("skills", List.of(row));
    return body;
  }

  private Map<String, Object> refusalFor(long projectId, Map<String, Object> body) {
    return refusalOf(lead.post("/api/projects/" + projectId + "/posts", body, Map.class));
  }

  private static Map<String, Object> refusalOf(ResponseEntity<Map> response) {
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    assertThat(body(response)).containsEntry("code", "invalid post");
    return body(response);
  }

  private static void assertReleaseFirst(ResponseEntity<Map> response) {
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(body(response)).containsEntry("code", "release first");
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
            + " SELECT ?, project_id, DATE '2027-02-01', DATE '2027-06-30', 'CONFIRMATION', ?"
            + " FROM post WHERE id = ?",
        employeeNo,
        assignment,
        postId);
  }

  private long givenPending(long postId, String employeeNo) {
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
  private Map<String, Object> gapRow(long projectId, long postId) {
    ResponseEntity<List> response = lead.get("/api/projects/" + projectId + "/gap", List.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    List<Map<String, Object>> rows = response.getBody();
    return rows.stream()
        .filter(row -> ((Number) row.get("id")).longValue() == postId)
        .findFirst()
        .orElseThrow();
  }

  private String postStatus(long postId) {
    return jdbc.queryForObject("SELECT status FROM post WHERE id = ?", String.class, postId);
  }

  private String storedRole(long postId) {
    return jdbc.queryForObject(
        "SELECT r.name FROM post p JOIN role r ON r.id = p.role_id WHERE p.id = ?",
        String.class,
        postId);
  }

  private LocalDate postEndDate(long postId) {
    return jdbc.queryForObject("SELECT end_date FROM post WHERE id = ?", java.sql.Date.class, postId)
        .toLocalDate();
  }

  private LocalDate allocationEndDate(long postId) {
    return jdbc.queryForObject(
            "SELECT end_date FROM allocation WHERE assignment_id ="
                + " (SELECT id FROM assignment WHERE post_id = ?)",
            java.sql.Date.class,
            postId)
        .toLocalDate();
  }

  private int count(String sql) {
    Integer value = jdbc.queryForObject(sql, Integer.class);
    return value == null ? 0 : value;
  }

  private static long id(ResponseEntity<Map> response) {
    return ((Number) body(response).get("id")).longValue();
  }

  @SuppressWarnings("unchecked")
  private static Map<String, Object> body(ResponseEntity<Map> response) {
    return (Map<String, Object>) response.getBody();
  }
}
