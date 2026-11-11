package com.example.staffing.support;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * Builds the two-sheet workbook the import contract describes, so a test says what the file holds
 * without talking about cells. Empty strings write no cell at all, which is how a blank cell looks
 * on the wire.
 */
public final class WorkbookFixture {

  public static final List<String> PEOPLE_HEADER =
      List.of("employee_no", "name", "email", "role", "job_level", "travel_willingness", "active");
  public static final List<String> SKILL_HEADER = List.of("employee_no", "skill", "proficiency");

  private final List<List<String>> people = new ArrayList<>();
  private final List<List<String>> skills = new ArrayList<>();
  private List<String> peopleHeader = PEOPLE_HEADER;
  private List<String> skillHeader = SKILL_HEADER;
  private boolean peopleSheet = true;
  private boolean skillSheet = true;
  private boolean peopleHeaderRow = true;

  private WorkbookFixture() {}

  public static WorkbookFixture workbook() {
    return new WorkbookFixture();
  }

  public WorkbookFixture person(String... cells) {
    people.add(List.of(cells));
    return this;
  }

  public WorkbookFixture skill(String... cells) {
    skills.add(List.of(cells));
    return this;
  }

  /** A real export rarely spells headers the way the contract does. */
  public WorkbookFixture peopleHeader(String... header) {
    this.peopleHeader = List.of(header);
    return this;
  }

  public WorkbookFixture onlyPeopleSheet() {
    skillSheet = false;
    return this;
  }

  public WorkbookFixture onlySkillsSheet() {
    peopleSheet = false;
    return this;
  }

  /** A people sheet a spreadsheet tool left blank: no header row and no cells at all. */
  public WorkbookFixture emptyPeople() {
    peopleHeaderRow = false;
    return this;
  }

  public byte[] xlsx() {
    try (Workbook workbook = new XSSFWorkbook();
        ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      if (peopleSheet) {
        if (peopleHeaderRow) {
          sheet(workbook, "people", peopleHeader, people);
        } else {
          workbook.createSheet("people");
        }
      }
      if (skillSheet) {
        sheet(workbook, "skills", skillHeader, skills);
      }
      workbook.write(out);
      return out.toByteArray();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private static void sheet(
      Workbook workbook, String name, List<String> header, List<List<String>> rows) {
    Sheet sheet = workbook.createSheet(name);
    Row headerRow = sheet.createRow(0);
    for (int column = 0; column < header.size(); column++) {
      headerRow.createCell(column).setCellValue(header.get(column));
    }
    for (int index = 0; index < rows.size(); index++) {
      Row row = sheet.createRow(index + 1);
      List<String> cells = rows.get(index);
      for (int column = 0; column < cells.size(); column++) {
        if (!cells.get(column).isEmpty()) {
          row.createCell(column).setCellValue(cells.get(column));
        }
      }
    }
  }
}
