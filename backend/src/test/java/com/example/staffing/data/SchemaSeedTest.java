package com.example.staffing.data;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptUtils;

/**
 * T04: the V1 schema and its seed. Asserts the shape the specs rely on — five roles, six skills
 * with one domain each, one role per person by construction, three demo accounts — and that a
 * second run of the seed (a restart on the file database) changes no count.
 */
@SpringBootTest
class SchemaSeedTest {

  private static final List<String> TABLES =
      List.of(
          "ROLE",
          "APP_USER",
          "SKILL",
          "PERSON",
          "PERSON_SKILL",
          "PROJECT",
          "POST",
          "POST_SKILL",
          "ASSIGNMENT",
          "ALLOCATION",
          "RECOMMENDATION",
          "NOTICE",
          "IMPORT_BATCH",
          "IMPORT_ERROR");

  @Autowired private JdbcTemplate jdbc;

  @Test
  void schemaCreatesEveryTableOfTheDataModel() {
    List<String> present =
        jdbc.queryForList(
            "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = 'PUBLIC'",
            String.class);

    assertThat(present).containsAll(TABLES);
  }

  @Test
  void seedOffersExactlyTheFiveSeededRoles() {
    List<String> roles =
        jdbc.queryForList("SELECT name FROM role WHERE active ORDER BY name", String.class);

    assertThat(roles).containsExactly("BA", "DEV", "PM", "QA", "UX");
  }

  @Test
  void everySeededSkillHasExactlyOneRoleDomain() {
    List<Map<String, Object>> rows =
        jdbc.queryForList(
            "SELECT s.name AS skill, r.name AS domain FROM skill s JOIN role r ON r.id = s.role_domain_id ORDER BY s.name");

    assertThat(rows)
        .extracting(row -> row.get("SKILL") + "=" + row.get("DOMAIN"))
        .containsExactly(
            "UI设计=UX", "业务分析=BA", "前端开发=DEV", "后端开发=DEV", "测试=QA", "项目管理=PM");
    assertThat(count("SELECT COUNT(*) FROM skill WHERE role_domain_id IS NULL")).isZero();
  }

  @Test
  void aPersonCarriesOneRoleByConstruction() {
    List<Map<String, Object>> roleColumns =
        jdbc.queryForList(
            "SELECT COLUMN_NAME, IS_NULLABLE FROM INFORMATION_SCHEMA.COLUMNS"
                + " WHERE TABLE_SCHEMA = 'PUBLIC' AND TABLE_NAME = 'PERSON'"
                + " AND COLUMN_NAME LIKE '%ROLE%'");

    assertThat(roleColumns).hasSize(1);
    assertThat(roleColumns.get(0)).containsEntry("COLUMN_NAME", "ROLE_ID").containsEntry("IS_NULLABLE", "NO");
    assertThat(count("SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = 'PERSON_ROLE'"))
        .isZero();
  }

  @Test
  void seedCreatesOneCoordinatorAndTwoLeadsWithHashedPasswords() {
    List<Map<String, Object>> users =
        jdbc.queryForList(
            "SELECT username, role, password_hash FROM app_user ORDER BY username");

    assertThat(users).hasSize(3);
    assertThat(users).filteredOn(user -> "COORDINATOR".equals(user.get("ROLE"))).hasSize(1);
    assertThat(users).filteredOn(user -> "LEAD".equals(user.get("ROLE"))).hasSize(2);
    assertThat(users)
        .allSatisfy(
            user ->
                assertThat((String) user.get("PASSWORD_HASH"))
                    .startsWith("$2a$")
                    .doesNotContain("demo"));
  }

  @Test
  void rerunningTheSeedChangesNoCount() throws Exception {
    int before = count("SELECT COUNT(*) FROM role");

    try (var connection = jdbc.getDataSource().getConnection()) {
      ScriptUtils.executeSqlScript(connection, new ClassPathResource("data.sql"));
    }

    assertThat(count("SELECT COUNT(*) FROM role")).isEqualTo(before);
    assertThat(count("SELECT COUNT(*) FROM skill")).isEqualTo(6);
    assertThat(count("SELECT COUNT(*) FROM app_user")).isEqualTo(3);
  }

  private int count(String sql) {
    Integer value = jdbc.queryForObject(sql, Integer.class);
    return value == null ? 0 : value;
  }
}
