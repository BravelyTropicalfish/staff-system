package com.example.staffing.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.staffing.domain.ImportReference.RoleRef;
import com.example.staffing.domain.ImportReference.SkillRef;
import com.example.staffing.support.WorkbookFixture;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Every row rule of the import contract, applied to both sheets before anything is written. */
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
                .person("E200", "Ann", "", "dev", "senior", "full", "Active")
                .skill("E200", "java", "practitioner"));

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
    assertThat(result.skills()).singleElement().satisfies(skill -> assertThat(skill.band()).isEqualTo("Practitioner"));
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
              assertThat(error.message()).contains("DEV, PM, QA");
            });
    assertThat(result.people()).isEmpty();
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
        .allSatisfy(error -> assertThat(error.message()).contains("exactly one active catalog role"));
    assertThat(result.people()).isEmpty();
  }

  @Test
  void aDuplicatedSkillRowPutsBothRowNumbersInTheErrorList() {
    ImportResult result =
        validate(
            WorkbookFixture.workbook()
                .skill("E100", "测试", "Novice")
                .skill("E100", "测试", "Expert"));

    assertThat(result.errors()).extracting(RowError::rowNo).containsExactly(2, 3);
    assertThat(result.isRejected()).isTrue();
  }

  @Test
  void anOrphanSkillRowSaysPersonNotFound() {
    ImportResult result = validate(WorkbookFixture.workbook().skill("E999", "Java", "Novice"));

    assertThat(result.errors())
        .singleElement()
        .satisfies(
            error -> {
              assertThat(error.rowNo()).isEqualTo(2);
              assertThat(error.key()).isEqualTo("E999");
              assertThat(error.message()).isEqualTo("person not found");
            });
  }

  @Test
  void aSkillRowForAPersonCreatedByTheSameFileIsAccepted() {
    ImportResult result =
        validate(
            WorkbookFixture.workbook()
                .person("E200", "Ann", "", "DEV", "Senior", "full", "active")
                .skill("E200", "Java", "Novice"));

    assertThat(result.errors()).isEmpty();
    assertThat(result.skills()).singleElement().satisfies(skill -> assertThat(skill.employeeNo()).isEqualTo("E200"));
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
              assertThat(error.message()).contains("E200").contains("row 2");
            });
  }

  @Test
  void aJobLevelOutsideTheFourBandsBlocksTheFile() {
    ImportResult result =
        validate(WorkbookFixture.workbook().person("E200", "Ann", "", "DEV", "Mid", "full", "active"));

    assertThat(result.errors())
        .singleElement()
        .satisfies(error -> assertThat(error.message()).contains("job_level").contains("Junior, Senior, Lead, Principal"));
  }

  @Test
  void aSkillOutsideTheCatalogBlocksTheFile() {
    ImportResult result = validate(WorkbookFixture.workbook().skill("E100", "Kotlin", "Novice"));

    assertThat(result.errors())
        .singleElement()
        .satisfies(error -> assertThat(error.message()).contains("Kotlin").contains("active catalog"));
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
            "name is required",
            "role is required",
            "job_level is required",
            "travel_willingness is required",
            "active is required");
  }

  @Test
  void aPeopleRowWithoutAnEmployeeNumberIsRefusedByThatColumn() {
    ImportResult result =
        validate(WorkbookFixture.workbook().person("", "Ann", "", "DEV", "Senior", "full", "active"));

    assertThat(result.errors())
        .singleElement()
        .satisfies(
            error -> {
              assertThat(error.rowNo()).isEqualTo(2);
              assertThat(error.message()).isEqualTo("employee_no is required");
            });
    assertThat(result.people()).isEmpty();
  }

  @Test
  void aSkillRowWithoutAnEmployeeNumberIsOneError() {
    ImportResult result = validate(WorkbookFixture.workbook().skill("", "Java", "Novice"));

    assertThat(result.errors())
        .singleElement()
        .satisfies(
            error -> {
              assertThat(error.sheet()).isEqualTo("skills");
              assertThat(error.key()).isEmpty();
              assertThat(error.message()).isEqualTo("employee_no is required");
            });
  }

  @Test
  void aSkillRowWithoutASkillSaysSkillIsRequired() {
    ImportResult result = validate(WorkbookFixture.workbook().skill("E100", "", "Novice"));

    assertThat(result.errors())
        .singleElement()
        .satisfies(error -> assertThat(error.message()).isEqualTo("skill is required"));
  }

  @Test
  void aProficiencyOutsideTheFiveBandsBlocksTheFile() {
    ImportResult result = validate(WorkbookFixture.workbook().skill("E100", "Java", "Guru"));

    assertThat(result.errors())
        .singleElement()
        .satisfies(
            error ->
                assertThat(error.message())
                    .isEqualTo(
                        "proficiency must be one of Novice, Advanced beginner, Practitioner,"
                            + " Proficient, Expert"));
    assertThat(result.skills()).isEmpty();
  }

  @Test
  void aThirdRowOfOneSkillDoesNotRepeatTheFirstRowsError() {
    ImportResult result =
        validate(
            WorkbookFixture.workbook()
                .skill("E100", "测试", "Novice")
                .skill("E100", "测试", "Expert")
                .skill("E100", "测试", "Proficient"));

    assertThat(result.errors()).extracting(RowError::rowNo).containsExactly(2, 3, 4);
    assertThat(result.errors())
        .extracting(RowError::message)
        .allSatisfy(message -> assertThat(message).contains("appears twice"));
  }

  private static ImportResult validate(WorkbookFixture fixture) {
    return new ImportValidator().validate(WorkbookParser.parse(fixture.xlsx()), REFERENCE);
  }
}
