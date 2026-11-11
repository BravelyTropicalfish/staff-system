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
 * T06, the catalog API: the five seeded roles and six skills, a role added later with no domain,
 * a rename allowed only before the first reference, a delete that always refuses, deactivation
 * that touches no stored reference, and a domain change that leaves people and skills as they are.
 *
 * <p>Each test starts from a dropped and rebuilt schema, so a test leaves nothing behind for the
 * next one; the context is discarded after the class so no other class inherits this database.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class CatalogApiTest {

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
  void theSeededCatalogsAreTheOnlyChoicesAtFirstUse() {
    assertThat(roles())
        .extracting(role -> role.get("name"))
        .containsExactlyInAnyOrder("BA", "DEV", "PM", "QA", "UX");
    assertThat(roles()).allSatisfy(role -> assertThat(role.get("active")).isEqualTo(true));
    assertThat(skills(true))
        .extracting(skill -> skill.get("name"))
        .containsExactlyInAnyOrder("后端开发", "前端开发", "项目管理", "业务分析", "测试", "UI设计");
  }

  @Test
  void theSeededMappingIsReadablePerRole() {
    assertThat(domainOf("DEV")).containsExactlyInAnyOrder("后端开发", "前端开发");
    assertThat(domainOf("QA")).containsExactly("测试");
    assertThat(domainOf("BA")).containsExactly("业务分析");
    assertThat(domainOf("PM")).containsExactly("项目管理");
    assertThat(domainOf("UX")).containsExactly("UI设计");

    int claimed =
        roles().stream().mapToInt(role -> skillsOf(role).size()).sum();
    assertThat(claimed).isEqualTo(6);
  }

  @Test
  void aRoleAddedLaterStartsWithAnEmptyDomain() {
    ResponseEntity<Map> created = createRole("SRE");

    assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(skillsOf(created.getBody())).isEmpty();
    assertThat(domainOf("SRE")).isEmpty();
    assertThat(skills(false))
        .noneMatch(skill -> "SRE".equals(skill.get("roleDomainName")));

    ResponseEntity<Map> skill =
        coordinator.post(
            "/api/skills",
            Map.of("name", "站点可靠性", "roleDomainId", idOf(created)),
            Map.class);

    assertThat(skill.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(skill.getBody()).containsEntry("roleDomainName", "SRE");
    assertThat(domainOf("SRE")).containsExactly("站点可靠性");
  }

  @Test
  void renameIsAllowedBeforeTheFirstReference() {
    long sre = idOf(createRole("SRE"));

    ResponseEntity<Map> renamedRole =
        coordinator.patch("/api/roles/" + sre, Map.of("name", "SRE2"), Map.class);
    assertThat(renamedRole.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(renamedRole.getBody()).containsEntry("name", "SRE2");

    long skill =
        idOf(
            coordinator.post(
                "/api/skills", Map.of("name", "站点可靠性", "roleDomainId", sre), Map.class));

    ResponseEntity<Map> renamedSkill =
        coordinator.patch("/api/skills/" + skill, Map.of("name", "SRE 实践"), Map.class);
    assertThat(renamedSkill.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(renamedSkill.getBody())
        .containsEntry("name", "SRE 实践")
        .containsEntry("roleDomainName", "SRE2");
  }

  @Test
  void renamingAnItemToTheNameItAlreadyHoldsChangesNothing() {
    long sre = idOf(createRole("SRE"));
    long skill =
        idOf(
            coordinator.post(
                "/api/skills", Map.of("name", "Go", "roleDomainId", roleId("DEV")), Map.class));

    ResponseEntity<Map> role = coordinator.patch("/api/roles/" + sre, Map.of("name", "SRE"), Map.class);
    ResponseEntity<Map> unchanged =
        coordinator.patch("/api/skills/" + skill, Map.of("name", "Go"), Map.class);

    assertThat(role.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(role.getBody()).containsEntry("name", "SRE");
    assertThat(unchanged.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(unchanged.getBody()).containsEntry("name", "Go");
  }

  @Test
  void anUnknownRoleIsNotFoundOnDeleteToo() {
    ResponseEntity<Map> deleted = coordinator.delete("/api/roles/999999", Map.class);

    assertThat(deleted.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(deleted.getBody()).containsEntry("code", "not found");
  }

  @Test
  void deleteAndRenameAreBothRefusedOnAReferencedSkill() {
    long backend = skillId("后端开发");
    long dev = roleId("DEV");
    personHolding("E100", dev, backend);
    personHolding("E101", dev, backend);
    postRequiring(dev, backend);

    ResponseEntity<Map> deleted = coordinator.delete("/api/skills/" + backend, Map.class);
    assertThat(deleted.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(deleted.getBody()).containsEntry("code", "delete refused");

    ResponseEntity<Map> renamed =
        coordinator.patch("/api/skills/" + backend, Map.of("name", "后端"), Map.class);
    assertThat(renamed.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(renamed.getBody()).containsEntry("code", "referenced");

    assertThat(skillId("后端开发")).isEqualTo(backend);
  }

  @Test
  void aRoleReferencedByAPersonCannotBeRenamed() {
    long dev = roleId("DEV");
    personHolding("E100", dev, skillId("后端开发"));

    ResponseEntity<Map> renamed =
        coordinator.patch("/api/roles/" + dev, Map.of("name", "DEVELOPER"), Map.class);

    assertThat(renamed.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(renamed.getBody()).containsEntry("code", "referenced");
    assertThat(roleNamed("DEV")).containsEntry("name", "DEV");
  }

  @Test
  void aDeleteAlwaysOffersDeactivationInstead() {
    long unreferenced = skillId("UI设计");

    ResponseEntity<Map> deleted = coordinator.delete("/api/skills/" + unreferenced, Map.class);

    assertThat(deleted.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(deleted.getBody()).containsEntry("code", "delete refused");
    assertThat((String) deleted.getBody().get("message")).contains("active flag");
  }

  @Test
  void aRoleIsNeverDeletedEither() {
    long qa = roleId("QA");
    int before = count("SELECT COUNT(*) FROM role");

    ResponseEntity<Map> deleted = coordinator.delete("/api/roles/" + qa, Map.class);
    ResponseEntity<Map> byLead =
        ApiSession.signIn(rest, "lead.one").delete("/api/roles/" + qa, Map.class);

    assertThat(deleted.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(deleted.getBody()).containsEntry("code", "delete refused");
    assertThat(byLead.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    assertThat(count("SELECT COUNT(*) FROM role")).isEqualTo(before);
  }

  @Test
  void renamingToANameAnotherItemHoldsIsRefused() {
    long sre = idOf(createRole("SRE"));
    long go =
        idOf(
            coordinator.post(
                "/api/skills", Map.of("name", "Go", "roleDomainId", roleId("DEV")), Map.class));

    ResponseEntity<Map> role = coordinator.patch("/api/roles/" + sre, Map.of("name", "QA"), Map.class);
    ResponseEntity<Map> skill =
        coordinator.patch("/api/skills/" + go, Map.of("name", "测试"), Map.class);

    assertThat(role.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(role.getBody()).containsEntry("code", "duplicate name");
    assertThat(skill.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(skill.getBody()).containsEntry("code", "duplicate name");
    assertThat(roleNamed("SRE")).containsEntry("name", "SRE");
    assertThat(skillId("Go")).isPositive();
  }

  @Test
  void deactivationHidesAnItemFromNewWorkOnly() {
    long backend = skillId("后端开发");
    personHolding("E100", roleId("DEV"), backend);

    ResponseEntity<Map> off =
        coordinator.patch("/api/skills/" + backend, Map.of("active", false), Map.class);
    assertThat(off.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(off.getBody()).containsEntry("active", false);

    assertThat(skills(true)).extracting(skill -> skill.get("name")).doesNotContain("后端开发");
    assertThat(skills(false))
        .filteredOn(skill -> "后端开发".equals(skill.get("name")))
        .singleElement()
        .satisfies(skill -> assertThat(skill.get("active")).isEqualTo(false));
    assertThat(
            count(
                "SELECT COUNT(*) FROM person_skill WHERE employee_no = 'E100' AND skill_id = "
                    + backend))
        .isEqualTo(1);

    coordinator.patch("/api/skills/" + backend, Map.of("active", true), Map.class);
    assertThat(skills(true)).extracting(skill -> skill.get("name")).contains("后端开发");
  }

  @Test
  void deactivatingARoleKeepsItsDomainReadable() {
    long ux = roleId("UX");

    ResponseEntity<Map> off = coordinator.patch("/api/roles/" + ux, Map.of("active", false), Map.class);

    assertThat(off.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(roleNamed("UX")).containsEntry("active", false);
    assertThat(domainOf("UX")).containsExactly("UI设计");
  }

  @Test
  void aDomainChangeReusesStoredDataUntouched() {
    long qa = roleId("QA");
    long ux = roleId("UX");
    long testing = skillId("测试");
    personHolding("E100", qa, testing);

    ResponseEntity<Map> moved =
        coordinator.patch("/api/skills/" + testing, Map.of("roleDomainId", ux), Map.class);

    assertThat(moved.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(moved.getBody()).containsEntry("roleDomainName", "UX");
    assertThat(domainOf("QA")).isEmpty();
    assertThat(domainOf("UX")).contains("测试");
    assertThat(
            jdbc.queryForObject(
                "SELECT band FROM person_skill WHERE employee_no = 'E100' AND skill_id = ?",
                String.class,
                testing))
        .isEqualTo("Practitioner");
    assertThat(count("SELECT COUNT(*) FROM person WHERE employee_no = 'E100'")).isEqualTo(1);
  }

  @Test
  void duplicateNamesAreRefused() {
    ResponseEntity<Map> role = createRole("DEV");
    assertThat(role.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(role.getBody()).containsEntry("code", "duplicate name");

    ResponseEntity<Map> skill =
        coordinator.post(
            "/api/skills", Map.of("name", "测试", "roleDomainId", roleId("QA")), Map.class);
    assertThat(skill.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(skill.getBody()).containsEntry("code", "duplicate name");
  }

  @Test
  void aBlankNameAndAnUnknownDomainAreUnprocessable() {
    ResponseEntity<Map> blank = createRole("   ");
    assertThat(blank.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    assertThat(blank.getBody()).containsEntry("code", "invalid");

    ResponseEntity<Map> unknownDomain =
        coordinator.post(
            "/api/skills", Map.of("name", "新技能", "roleDomainId", 999_999), Map.class);
    assertThat(unknownDomain.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    assertThat(unknownDomain.getBody()).containsEntry("code", "invalid role domain");
  }

  @Test
  void unknownIdsAreNotFound() {
    assertThat(
            coordinator
                .patch("/api/roles/999999", Map.of("active", false), Map.class)
                .getStatusCode())
        .isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(coordinator.delete("/api/skills/999999", Map.class).getStatusCode())
        .isEqualTo(HttpStatus.NOT_FOUND);
  }

  @Test
  void aLeadReadsTheCatalogsButCannotMaintainThem() {
    ApiSession lead = ApiSession.signIn(rest, "lead.one");
    int rolesBefore = count("SELECT COUNT(*) FROM role");

    assertThat(lead.get("/api/roles", List.class).getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(lead.get("/api/skills?active=true", List.class).getStatusCode())
        .isEqualTo(HttpStatus.OK);

    assertThat(lead.post("/api/roles", Map.of("name", "SRE"), Map.class).getStatusCode())
        .isEqualTo(HttpStatus.FORBIDDEN);
    assertThat(
            lead.patch("/api/skills/" + skillId("测试"), Map.of("active", false), Map.class)
                .getStatusCode())
        .isEqualTo(HttpStatus.FORBIDDEN);
    assertThat(lead.delete("/api/roles/" + roleId("UX"), Map.class).getStatusCode())
        .isEqualTo(HttpStatus.FORBIDDEN);

    assertThat(count("SELECT COUNT(*) FROM role")).isEqualTo(rolesBefore);
    assertThat(roleNamed("UX")).containsEntry("active", true);
  }

  @Test
  @SuppressWarnings("unchecked")
  void theThreeScalesAreReadableByBothRoles() {
    ApiSession lead = ApiSession.signIn(rest, "lead.one");

    for (ApiSession reader : List.of(coordinator, lead)) {
      ResponseEntity<Map> response = reader.get("/api/bands", Map.class);
      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
      assertThat((List<String>) response.getBody().get("jobLevels"))
          .containsExactly("Junior", "Senior", "Lead", "Principal");
      assertThat((List<String>) response.getBody().get("proficiencies"))
          .containsExactly(
              "Novice", "Advanced beginner", "Practitioner", "Proficient", "Expert");
      assertThat((List<String>) response.getBody().get("travel"))
          .containsExactly("none", "limited", "full");
    }
  }

  @SuppressWarnings("unchecked")
  private List<Map<String, Object>> roles() {
    return (List<Map<String, Object>>) coordinator.get("/api/roles", List.class).getBody();
  }

  @SuppressWarnings("unchecked")
  private List<Map<String, Object>> skills(boolean onlyActive) {
    return (List<Map<String, Object>>)
        coordinator.get("/api/skills?active=" + onlyActive, List.class).getBody();
  }

  private Map<String, Object> roleNamed(String name) {
    return roles().stream()
        .filter(role -> name.equals(role.get("name")))
        .findFirst()
        .orElseThrow(() -> new AssertionError("no role named " + name));
  }

  @SuppressWarnings("unchecked")
  private List<Map<String, Object>> skillsOf(Map<String, Object> role) {
    return (List<Map<String, Object>>) role.get("skills");
  }

  private List<String> domainOf(String roleName) {
    return skillsOf(roleNamed(roleName)).stream()
        .map(skill -> (String) skill.get("name"))
        .toList();
  }

  private long roleId(String name) {
    return idOf(roleNamed(name));
  }

  private long skillId(String name) {
    return skills(false).stream()
        .filter(skill -> name.equals(skill.get("name")))
        .map(CatalogApiTest::idOf)
        .findFirst()
        .orElseThrow(() -> new AssertionError("no skill named " + name));
  }

  private static long idOf(Map<String, Object> row) {
    return ((Number) row.get("id")).longValue();
  }

  private static long idOf(ResponseEntity<Map> response) {
    return ((Number) response.getBody().get("id")).longValue();
  }

  private ResponseEntity<Map> createRole(String name) {
    return coordinator.post("/api/roles", Map.of("name", name), Map.class);
  }

  private void personHolding(String employeeNo, long roleId, long skillId) {
    jdbc.update(
        "INSERT INTO person (employee_no, name, role_id, job_level, travel, active)"
            + " VALUES (?, ?, ?, 'Senior', 'full', TRUE)",
        employeeNo,
        "Person " + employeeNo,
        roleId);
    jdbc.update(
        "INSERT INTO person_skill (employee_no, skill_id, band) VALUES (?, ?, 'Practitioner')",
        employeeNo,
        skillId);
  }

  private void postRequiring(long roleId, long skillId) {
    long lead =
        jdbc.queryForObject(
            "SELECT id FROM app_user WHERE username = 'lead.one'", Long.class);
    jdbc.update(
        "INSERT INTO project (name, lead_user_id, start_date, end_date, status)"
            + " VALUES ('Atlas', ?, DATE '2027-01-01', DATE '2027-12-31', 'OPEN')",
        lead);
    long project = jdbc.queryForObject("SELECT id FROM project WHERE name = 'Atlas'", Long.class);
    jdbc.update(
        "INSERT INTO post (project_id, title, role_id, min_job_level, headcount,"
            + " start_date, end_date, travel_required, status)"
            + " VALUES (?, 'DEV 1', ?, 'Senior', 1, DATE '2027-01-01', DATE '2027-03-31',"
            + " 'full', 'open')",
        project,
        roleId);
    long post = jdbc.queryForObject("SELECT id FROM post WHERE title = 'DEV 1'", Long.class);
    jdbc.update(
        "INSERT INTO post_skill (post_id, skill_id, kind, min_band)"
            + " VALUES (?, ?, 'REQUIRED', 'Practitioner')",
        post,
        skillId);
  }

  private int count(String sql) {
    Integer value = jdbc.queryForObject(sql, Integer.class);
    return value == null ? 0 : value;
  }
}
