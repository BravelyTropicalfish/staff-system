package com.example.staffing.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import com.example.staffing.support.WorkbookFixture;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

/** The shape of the file: the one sheet, its columns, and which rows count as data. */
class WorkbookParserTest {

  @Test
  void aWorkbookWithoutTheSkillsColumnIsRefused() {
    byte[] bytes =
        WorkbookFixture.workbook()
            .peopleHeader("employee_no", "name", "email", "role", "job_level", "travel_willingness", "active")
            .person(personRow("E200"))
            .xlsx();

    assertThatExceptionOfType(UnreadableWorkbookException.class)
        .isThrownBy(() -> WorkbookParser.parse(bytes))
        .withMessageContaining("skills");
  }

  @Test
  void aWorkbookWithoutThePeopleSheetIsRefused() {
    byte[] bytes = WorkbookFixture.workbook().noPeopleSheet().xlsx();

    assertThatExceptionOfType(UnreadableWorkbookException.class)
        .isThrownBy(() -> WorkbookParser.parse(bytes))
        .withMessageContaining("people");
  }

  @Test
  void aWorkbookWithHeadersOnlyIsRefusedAsEmpty() {
    byte[] bytes = WorkbookFixture.workbook().xlsx();

    assertThatExceptionOfType(UnreadableWorkbookException.class)
        .isThrownBy(() -> WorkbookParser.parse(bytes))
        .withMessageContaining("空的");
  }

  @Test
  void bytesThatAreNotAWorkbookAreRefused() {
    byte[] bytes = "employee_no,name\nE100,Ann".getBytes(StandardCharsets.UTF_8);

    assertThatExceptionOfType(UnreadableWorkbookException.class)
        .isThrownBy(() -> WorkbookParser.parse(bytes))
        .withMessageContaining("不是可读取的 .xlsx 工作簿");
  }

  @Test
  void headerSpellingDoesNotMatter() {
    byte[] bytes =
        WorkbookFixture.workbook()
            .peopleHeader(
                "Employee No",
                "Name",
                "Email",
                "Role",
                "Job Level",
                "Travel Willingness",
                "Active",
                "Skills")
            .person(personRow("E200"))
            .xlsx();

    ParsedWorkbook parsed = WorkbookParser.parse(bytes);

    assertThat(parsed.peopleColumns().missing()).isEmpty();
    assertThat(parsed.peopleColumns().value(parsed.people().get(0), "employee_no"))
        .contains("E200");
  }

  @Test
  void aMissingColumnIsRefusedByName() {
    byte[] bytes =
        WorkbookFixture.workbook()
            .peopleHeader("employee_no", "name", "email", "role", "job_level", "active", "skills")
            .person(personRow("E200"))
            .xlsx();

    assertThatExceptionOfType(UnreadableWorkbookException.class)
        .isThrownBy(() -> WorkbookParser.parse(bytes))
        .withMessageContaining("travel_willingness");
  }

  @Test
  void blankRowsAreSkippedAndNumbersStayAsTheSpreadsheetShowsThem() {
    byte[] bytes =
        WorkbookFixture.workbook()
            .person(personRow("E200"))
            .person("", "", "", "", "", "", "", "")
            .person(personRow("E300"))
            .xlsx();

    ParsedWorkbook parsed = WorkbookParser.parse(bytes);

    assertThat(parsed.people()).extracting(SheetRow::rowNo).containsExactly(2, 4);
  }

  @Test
  void aSheetWithoutAnyHeaderRowIsRefusedByColumn() {
    byte[] bytes = WorkbookFixture.workbook().emptyPeople().xlsx();

    assertThatExceptionOfType(UnreadableWorkbookException.class)
        .isThrownBy(() -> WorkbookParser.parse(bytes))
        .withMessageContaining("people 表缺少这些列")
        .withMessageContaining("employee_no");
  }

  @Test
  void aRowTheSpreadsheetSkipsIsNotARow() throws Exception {
    byte[] bytes;
    try (XSSFWorkbook book = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      Sheet people = book.createSheet("people");
      writeRow(people, 0, WorkbookFixture.PEOPLE_HEADER.toArray(String[]::new));
      // The spreadsheet keeps no row 1 at all; the fixture still writes row 2 as the third line.
      writeRow(people, 2, personRow("E300"));
      book.write(out);
      bytes = out.toByteArray();
    }

    ParsedWorkbook parsed = WorkbookParser.parse(bytes);

    assertThat(parsed.people()).extracting(SheetRow::rowNo).containsExactly(3);
  }

  private static void writeRow(Sheet sheet, int index, String[] cells) {
    Row row = sheet.createRow(index);
    for (int column = 0; column < cells.length; column++) {
      row.createCell(column).setCellValue(cells[column]);
    }
  }

  private static String[] personRow(String employeeNo) {
    return new String[] {
      employeeNo, "Ann", "ann@example.com", "DEV", "Senior", "full", "active", "测试:Novice"
    };
  }
}
