package com.example.staffing.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.staffing.domain.ImportReference.RoleRef;
import com.example.staffing.domain.ImportReference.SkillRef;
import com.example.staffing.support.WorkbookFixture;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Every row rule of the import contract, applied to the one sheet before anything is written. One
 * row is one person plus that person's skills, written as `skill:band;skill:band`; a role or a skill
 * the catalog does not hold rejects the row.
 */
class ImportValidatorTest {

  private static final ImportReference REFERENCE =
      new ImportReference(
          Map.of(
              "pm", new RoleRef(1, "PM"),
              "dev", new RoleRef(3, "DEV"),
              "qa", new RoleRef(4, "QA")),
          Map.of("java", new SkillRef(1, "Java"), "测试", new SkillRef(2, "测试")),
          Set.of("E100"));

  @Test
  void lowerCaseIsNormalisedToTheCatalogSpelling() {
    ImportResult result =
        validate(
            WorkbookFixture.workbook()
                .person("E200", "Ann", "", "dev", "senior", "full", "Active", "java:practitioner"));

    assertThat(result.errors()).isEmpty();
    assertThat(result.people())
        .singleElement()
        .satisfies(
            person -> {
              assertThat(person.roleId()).isEqualTo(3);
              assertThat(person.jobLevel()).isEqualTo("Senior");
              assertThat(person.travel()).isEqualTo("full");
              assertThat(person.active()).isTrue();
            });
    assertThat(result.skills())
        .singleElement()
        .satisfies(
            skill -> {
              assertThat(skill.employeeNo()).isEqualTo("E200");
              assertThat(skill.skillId()).isEqualTo(1);
              assertThat(skill.band()).isEqualTo("Practitioner");
            });
  }

  @Test
  void oneSkillsCellMayCarrySeveralSkills() {
    ImportResult result =
        validate(
            WorkbookFixture.workbook()
                .person("E200", "Ann", "", "DEV", "Senior", "full", "active", "Java:Novice; 测试:Practitioner"));

    assertThat(result.errors()).isEmpty();
    assertThat(result.people()).hasSize(1);
    assertThat(result.skills())
        .extracting(SkillImportRow::skillId)
        .containsExactly(1L, 2L);
  }

  @Test
  void anEmptySkillsCellMeansThePersonCarriesNoSkill() {
    ImportResult result =
        validate(WorkbookFixture.workbook().person("E200", "Ann", "", "DEV", "Senior", "full", "active"));

    assertThat(result.errors()).isEmpty();
    assertThat(result.people()).hasSize(1);
    assertThat(result.skills()).isEmpty();
  }

  @Test
  void aRoleOutsideTheCatalogBlocksTheFileAndNamesTheAcceptedRoles() {
    ImportResult result =
        validate(WorkbookFixture.workbook().person("E200", "Ann", "", "Product", "Senior", "full", "active"));

    assertThat(result.errors())
        .singleElement()
        .satisfies(
            error -> {
              assertThat(error.sheet()).isEqualTo("people");
              assertThat(error.message()).contains("DEV").contains("PM").contains("QA");
            });
    assertThat(result.people()).isEmpty();
    assertThat(result.skills()).isEmpty();
  }

  @Test
  void aSkillOutsideTheCatalogBlocksTheFile() {
    ImportResult result =
        validate(
            WorkbookFixture.workbook()
                .person("E200", "Ann", "", "DEV", "Senior", "full", "active", "Kotlin:Novice"));

    assertThat(result.errors())
        .singleElement()
        .satisfies(error -> assertThat(error.message()).contains("Kotlin").contains("启用的目录"));
    assertThat(result.people()).isEmpty();
    assertThat(result.skills()).isEmpty();
  }

  @Test
  void aSkillsCellWithoutABandIsRefusedByItsShape() {
    ImportResult result =
        validate(
            WorkbookFixture.workbook()
                .person("E200", "Ann", "", "DEV", "Senior", "full", "active", "Java"));

    assertThat(result.errors())
        .singleElement()
        .satisfies(
            error ->
                assertThat(error.message()).isEqualTo("技能格需写成 技能:档位，多项用分号分隔"));
  }

  @Test
  void theSameSkillTwiceInOneCellIsRefused() {
    ImportResult result =
        validate(
            WorkbookFixture.workbook()
                .person("E200", "Ann", "", "DEV", "Senior", "full", "active", "测试:Novice;测试:Expert"));

    assertThat(result.errors())
        .singleElement()
        .satisfies(
            error -> assertThat(error.message()).contains("测试").contains("出现两次"));
    assertThat(result.skills()).isEmpty();
  }

  @Test
  void aMultiRoleCellIsOneRowErrorTwiceAndNeverTwoRoles() {
    ImportResult result =
        validate(
            WorkbookFixture.workbook()
                .person("E200", "Ann", "", "DEV, QA", "Senior", "full", "active")
                .person("E300", "Bo", "", "DEV / QA", "Senior", "full", "active"));

    assertThat(result.errors()).extracting(RowError::rowNo).containsExactly(2, 3);
    assertThat(result.errors())
        .allSatisfy(error -> assertThat(error.message()).contains("必须是目录中启用的一个角色"));
    assertThat(result.people()).isEmpty();
  }

  @Test
  void aDuplicatedEmployeeNumberNamesTheNumber() {
    ImportResult result =
        validate(
            WorkbookFixture.workbook()
                .person("E200", "Ann", "", "DEV", "Senior", "full", "active")
                .person("E200", "Bob", "", "QA", "Junior", "none", "inactive"));

    assertThat(result.errors())
        .singleElement()
        .satisfies(
            error -> {
              assertThat(error.rowNo()).isEqualTo(3);
              assertThat(error.message()).contains("E200").contains("第 2 行");
            });
    assertThat(result.people()).hasSize(1);
  }

  @Test
  void aJobLevelOutsideTheFourBandsBlocksTheFile() {
    ImportResult result =
        validate(WorkbookFixture.workbook().person("E200", "Ann", "", "DEV", "Mid", "full", "active"));

    assertThat(result.errors())
        .singleElement()
        .satisfies(
            error ->
                assertThat(error.message())
                    .contains("job_level")
                    .contains("Junior、Senior、Lead、Principal"));
  }

  @Test
  void emailMayBeBlankButTheOtherPeopleCellsMayNot() {
    ImportResult blankEmail =
        validate(WorkbookFixture.workbook().person("E200", "Ann", "", "DEV", "Senior", "full", "active"));
    ImportResult missingCells = validate(WorkbookFixture.workbook().person("E200", "", "", "", "", "", ""));

    assertThat(blankEmail.errors()).isEmpty();
    assertThat(missingCells.errors())
        .extracting(RowError::message)
        .containsExactly(
            "name 不能为空",
            "角色不能为空",
            "job_level 不能为空",
            "travel_willingness 不能为空",
            "active 不能为空");
  }

  @Test
  void aRowWithoutAnEmployeeNumberIsRefusedByThatColumn() {
    ImportResult result =
        validate(WorkbookFixture.workbook().person("", "Ann", "", "DEV", "Senior", "full", "active"));

    assertThat(result.errors())
        .singleElement()
        .satisfies(
            error -> {
              assertThat(error.rowNo()).isEqualTo(2);
              assertThat(error.message()).isEqualTo("employee_no 不能为空");
            });
    assertThat(result.people()).isEmpty();
  }

  @Test
  void aProficiencyOutsideTheFiveBandsBlocksTheFile() {
    ImportResult result =
        validate(
            WorkbookFixture.workbook()
                .person("E200", "Ann", "", "DEV", "Senior", "full", "active", "Java:Guru"));

    assertThat(result.errors())
        .singleElement()
        .satisfies(
            error ->
                assertThat(error.message())
                    .isEqualTo(
                        "proficiency 必须是以下之一：Novice、Advanced beginner、Practitioner、"
                            + "Proficient、Expert"));
    assertThat(result.skills()).isEmpty();
  }

  @Test
  void theWideColonSeparatesANameFromItsBandToo() {
    ImportResult result =
        validate(
            WorkbookFixture.workbook()
                .person("E200", "Ann", "", "DEV", "Senior", "full", "active", "测试：Expert"));

    assertThat(result.errors()).isEmpty();
    assertThat(result.skills())
        .singleElement()
        .satisfies(
            skill -> {
              assertThat(skill.skillId()).isEqualTo(2);
              assertThat(skill.band()).isEqualTo("Expert");
            });
  }

  @Test
  void aSkillsCellWithoutANameOrWithoutABandIsRefused() {
    ImportResult noName =
        validate(
            WorkbookFixture.workbook()
                .person("E200", "Ann", "", "DEV", "Senior", "full", "active", ":Novice"));
    ImportResult noBand =
        validate(
            WorkbookFixture.workbook()
                .person("E300", "Bo", "", "DEV", "Senior", "full", "active", "Java:"));

    assertThat(noName.errors())
        .singleElement()
        .satisfies(
            error -> assertThat(error.message()).isEqualTo("技能格要先写技能名再写档位"));
    assertThat(noBand.errors())
        .singleElement()
        .satisfies(error -> assertThat(error.message()).isEqualTo("proficiency 不能为空"));
  }

  @Test
  void aBandWithBothColonsKeepsTheFirstOneAsTheSeparator() {
    ImportResult result =
        validate(
            WorkbookFixture.workbook()
                .person("E200", "Ann", "", "DEV", "Senior", "full", "active", "测试：Expert:Novice"));

    assertThat(result.errors())
        .singleElement()
        .satisfies(error -> assertThat(error.message()).contains("proficiency 必须是以下之一"));
  }

  @Test
  void anEmptySegmentBetweenSemicolonsIsIgnored() {
    ImportResult result =
        validate(
            WorkbookFixture.workbook()
                .person("E200", "Ann", "", "DEV", "Senior", "full", "active", "Java:Novice;;测试:Expert;"));

    assertThat(result.errors()).isEmpty();
    assertThat(result.skills()).extracting(SkillImportRow::skillId).containsExactly(1L, 2L);
  }

  private static ImportResult validate(WorkbookFixture fixture) {
    return new ImportValidator().validate(WorkbookParser.parse(fixture.xlsx()), REFERENCE);
  }
}
