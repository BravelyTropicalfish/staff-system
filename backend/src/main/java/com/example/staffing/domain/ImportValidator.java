package com.example.staffing.domain;

import static com.example.staffing.domain.Bands.JOB_LEVELS;
import static com.example.staffing.domain.Bands.PROFICIENCIES;
import static com.example.staffing.domain.Bands.TRAVEL;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Applies the row rules to both sheets. It reads and judges but never writes, and it never stops at
 * the first bad row: one upload is one batch, and the coordinator gets the whole list at once.
 *
 * <p>Every value that reaches a row is already in the spelling the catalog stores. A cell is never
 * split into more than one value, so {@code DEV, QA} in a role cell is a row error rather than two
 * roles.
 */
final class ImportValidator {

  private static final String PEOPLE = "people";
  private static final String SKILLS = "skills";

  ImportResult validate(ParsedWorkbook workbook, ImportReference reference) {
    List<RowError> errors = new ArrayList<>();
    List<PersonImportRow> people = new ArrayList<>();
    Map<String, Integer> employeeRow = new LinkedHashMap<>();
    Set<String> fileEmployees = new LinkedHashSet<>();

    for (SheetRow row : workbook.people()) {
      String employeeNo = workbook.peopleColumns().value(row, "employee_no").orElse("");
      if (!employeeNo.isEmpty()) {
        fileEmployees.add(employeeNo);
      }
      PersonImportRow person = person(row, workbook.peopleColumns(), reference, errors);
      if (person == null) {
        continue;
      }
      Integer earlier = employeeRow.putIfAbsent(employeeNo, row.rowNo());
      if (earlier != null) {
        errors.add(
            new RowError(
                PEOPLE,
                row.rowNo(),
                employeeNo,
                "employee_no " + employeeNo + " appears twice in the file (also on row " + earlier
                    + ")"));
        continue;
      }
      people.add(person);
    }

    List<SkillImportRow> skills = new ArrayList<>();
    Map<String, Integer> skillRow = new LinkedHashMap<>();
    Set<Integer> alreadyReported = new LinkedHashSet<>();
    for (SheetRow row : workbook.skills()) {
      SkillImportRow skill =
          skill(
              row,
              workbook.skillColumns(),
              reference,
              fileEmployees,
              skillRow,
              alreadyReported,
              errors);
      if (skill != null) {
        skills.add(skill);
      }
    }

    errors.sort(
        Comparator.comparingInt((RowError error) -> PEOPLE.equals(error.sheet()) ? 0 : 1)
            .thenComparingInt(RowError::rowNo));
    return new ImportResult(people, skills, errors);
  }

  private PersonImportRow person(
      SheetRow row, ColumnMap columns, ImportReference reference, List<RowError> errors) {
    String employeeNo = columns.value(row, "employee_no").orElse("");
    String name = columns.value(row, "name").orElse("");
    String email = columns.value(row, "email").orElse("");
    String role = columns.value(row, "role").orElse("");
    String jobLevelCell = columns.value(row, "job_level").orElse("");
    String travelCell = columns.value(row, "travel_willingness").orElse("");
    String activeCell = columns.value(row, "active").orElse("");

    int before = errors.size();
    required(row.rowNo(), employeeNo, "employee_no", employeeNo, errors);
    required(row.rowNo(), employeeNo, "name", name, errors);
    // email is the one people column the contract lets a row leave blank.

    ImportReference.RoleRef roleRef =
        role.isEmpty() ? null : reference.roles().get(ImportReference.key(role));
    if (role.isEmpty()) {
      errors.add(new RowError(PEOPLE, row.rowNo(), employeeNo, "role is required"));
    } else if (roleRef == null) {
      errors.add(
          new RowError(
              PEOPLE,
              row.rowNo(),
              employeeNo,
              "role " + role + " must be exactly one active catalog role, one of "
                  + String.join(", ", reference.roleNames())));
    }

    String jobLevel = canonical(JOB_LEVELS, jobLevelCell);
    if (jobLevel == null) {
      errors.add(
          new RowError(PEOPLE, row.rowNo(), employeeNo, oneOf("job_level", jobLevelCell, JOB_LEVELS)));
    }

    String travel = canonical(TRAVEL, travelCell);
    if (travel == null) {
      errors.add(
          new RowError(
              PEOPLE, row.rowNo(), employeeNo, oneOf("travel_willingness", travelCell, TRAVEL)));
    }

    Boolean active = null;
    if ("active".equalsIgnoreCase(activeCell)) {
      active = Boolean.TRUE;
    } else if ("inactive".equalsIgnoreCase(activeCell)) {
      active = Boolean.FALSE;
    }
    if (active == null) {
      errors.add(
          new RowError(
              PEOPLE,
              row.rowNo(),
              employeeNo,
              oneOf("active", activeCell, List.of("active", "inactive"))));
    }

    if (errors.size() > before) {
      return null;
    }
    return new PersonImportRow(
        row.rowNo(), employeeNo, name, email, roleRef.id(), jobLevel, travel, active);
  }

  private SkillImportRow skill(
      SheetRow row,
      ColumnMap columns,
      ImportReference reference,
      Set<String> fileEmployees,
      Map<String, Integer> skillRow,
      Set<Integer> alreadyReported,
      List<RowError> errors) {
    String employeeNo = columns.value(row, "employee_no").orElse("");
    if (employeeNo.isEmpty()) {
      errors.add(new RowError(SKILLS, row.rowNo(), "", "employee_no is required"));
      return null;
    }
    if (!reference.knownEmployees().contains(employeeNo) && !fileEmployees.contains(employeeNo)) {
      errors.add(new RowError(SKILLS, row.rowNo(), employeeNo, "person not found"));
      return null;
    }

    String skillCell = columns.value(row, "skill").orElse("");
    String bandCell = columns.value(row, "proficiency").orElse("");
    ImportReference.SkillRef skill =
        skillCell.isEmpty() ? null : reference.skills().get(ImportReference.key(skillCell));
    if (skill == null) {
      errors.add(
          new RowError(
              SKILLS,
              row.rowNo(),
              employeeNo,
              skillCell.isEmpty()
                  ? "skill is required"
                  : "skill " + skillCell + " is not in the active catalog"));
      return null;
    }
    String band = canonical(PROFICIENCIES, bandCell);
    if (band == null) {
      errors.add(
          new RowError(
              SKILLS, row.rowNo(), employeeNo, oneOf("proficiency", bandCell, PROFICIENCIES)));
      return null;
    }

    Integer first = skillRow.putIfAbsent(employeeNo + '\u0000' + skill.id(), row.rowNo());
    if (first != null) {
      String reason =
          "skill " + skill.name() + " appears twice for " + employeeNo + " (also on row ";
      errors.add(new RowError(SKILLS, row.rowNo(), employeeNo, reason + first + ")"));
      if (alreadyReported.add(first)) {
        errors.add(new RowError(SKILLS, first, employeeNo, reason + row.rowNo() + ")"));
      }
      return null;
    }
    return new SkillImportRow(row.rowNo(), employeeNo, skill.id(), band);
  }

  private static void required(
      int rowNo, String key, String column, String cell, List<RowError> errors) {
    if (cell.isEmpty()) {
      errors.add(new RowError(PEOPLE, rowNo, key, column + " is required"));
    }
  }

  private static String canonical(List<String> allowed, String cell) {
    if (cell.isEmpty()) {
      return null;
    }
    for (String value : allowed) {
      if (value.equalsIgnoreCase(cell)) {
        return value;
      }
    }
    return null;
  }

  private static String oneOf(String column, String cell, List<String> allowed) {
    return cell.isEmpty()
        ? column + " is required"
        : column + " must be one of " + String.join(", ", allowed);
  }
}
