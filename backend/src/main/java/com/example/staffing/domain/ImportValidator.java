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
 * Applies the row rules to the one sheet. It reads and judges but never writes, and it never stops
 * at the first bad row: one upload is one batch, and the coordinator gets the whole list at once.
 *
 * <p>One row is one person plus that person's skills. The `skills` cell holds
 * {@code skill:band;skill:band}; an empty cell means the person carries no skill change. Every value
 * is already in the spelling the catalog stores — a role or a skill that is absent, or deactivated,
 * is a row error, and a cell is never split into more than one value except by that semicolon.
 */
final class ImportValidator {

  private static final String PEOPLE = "people";
  private static final String SKILLS_FORMAT = "技能格需写成 技能:档位，多项用分号分隔";

  ImportResult validate(ParsedWorkbook workbook, ImportReference reference) {
    List<RowError> errors = new ArrayList<>();
    List<PersonImportRow> people = new ArrayList<>();
    List<SkillImportRow> skills = new ArrayList<>();
    Map<String, Integer> employeeRow = new LinkedHashMap<>();

    for (SheetRow row : workbook.people()) {
      int before = errors.size();
      String employeeNo = workbook.peopleColumns().value(row, "employee_no").orElse("");
      PersonImportRow person = person(row, workbook.peopleColumns(), reference, errors);
      List<SkillImportRow> rowSkills =
          skills(row, workbook.peopleColumns(), reference, errors);
      if (errors.size() > before) {
        // One bad cell rejects the whole row: its person and its skills are one unit.
        continue;
      }
      Integer earlier = employeeRow.putIfAbsent(employeeNo, row.rowNo());
      if (earlier != null) {
        errors.add(
            new RowError(
                PEOPLE,
                row.rowNo(),
                employeeNo,
                "工号 " + employeeNo + " 在文件中出现两次（第 " + earlier + " 行也有）"));
        continue;
      }
      people.add(person);
      skills.addAll(rowSkills);
    }

    // Stable: rows stay in sheet order and, inside one row, in the order the rules ran.
    errors.sort(Comparator.comparingInt(RowError::rowNo));
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
      errors.add(new RowError(PEOPLE, row.rowNo(), employeeNo, "角色不能为空"));
    } else if (roleRef == null) {
      errors.add(
          new RowError(
              PEOPLE,
              row.rowNo(),
              employeeNo,
              "角色 " + role + " 必须是目录中启用的一个角色，可选："
                  + String.join("、", reference.roleNames())));
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

  /** The skills cell of one row: empty means none, otherwise `skill:band` pairs split by `;`. */
  private List<SkillImportRow> skills(
      SheetRow row, ColumnMap columns, ImportReference reference, List<RowError> errors) {
    String employeeNo = columns.value(row, "employee_no").orElse("");
    String cell = columns.value(row, "skills").orElse("");
    if (cell.isEmpty()) {
      return List.of();
    }
    List<SkillImportRow> rows = new ArrayList<>();
    Set<Long> seen = new LinkedHashSet<>();
    for (String part : cell.split("[;；]")) {
      String item = part.trim();
      if (item.isEmpty()) {
        continue;
      }
      int at = separator(item);
      if (at < 0) {
        errors.add(new RowError(PEOPLE, row.rowNo(), employeeNo, SKILLS_FORMAT));
        continue;
      }
      String skillName = item.substring(0, at).trim();
      String bandCell = item.substring(at + 1).trim();
      ImportReference.SkillRef skill =
          skillName.isEmpty() ? null : reference.skills().get(ImportReference.key(skillName));
      if (skill == null) {
        errors.add(
            new RowError(
                PEOPLE,
                row.rowNo(),
                employeeNo,
                skillName.isEmpty()
                    ? "技能格要先写技能名再写档位"
                    : "技能 " + skillName + " 不在启用的目录中"));
        continue;
      }
      String band = canonical(PROFICIENCIES, bandCell);
      if (band == null) {
        errors.add(
            new RowError(
                PEOPLE, row.rowNo(), employeeNo, oneOf("proficiency", bandCell, PROFICIENCIES)));
        continue;
      }
      if (!seen.add(skill.id())) {
        errors.add(
            new RowError(
                PEOPLE,
                row.rowNo(),
                employeeNo,
                "技能 " + skill.name() + " 在技能格中出现两次"));
        continue;
      }
      rows.add(new SkillImportRow(row.rowNo(), employeeNo, skill.id(), band));
    }
    return rows;
  }

  private static int separator(String item) {
    int latin = item.indexOf(':');
    int wide = item.indexOf('：');
    if (latin < 0) {
      return wide;
    }
    if (wide < 0) {
      return latin;
    }
    return Math.min(latin, wide);
  }

  private static void required(
      int rowNo, String key, String column, String cell, List<RowError> errors) {
    if (cell.isEmpty()) {
      errors.add(new RowError(PEOPLE, rowNo, key, column + " 不能为空"));
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
        ? column + " 不能为空"
        : column + " 必须是以下之一：" + String.join("、", allowed);
  }
}
