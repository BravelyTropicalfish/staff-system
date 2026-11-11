package com.example.staffing.domain;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.poi.UnsupportedFileFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

/**
 * Turns the uploaded bytes into the one sheet the contract names, or refuses the file. Everything
 * here is about shape — the sheet, its header row, blank rows — because a file that has the wrong
 * shape is refused outright, while a file with the right shape and bad rows becomes a rejected
 * batch. A person and their skills are one row: the `skills` cell holds `skill:band;skill:band`.
 */
final class WorkbookParser {

  static final List<String> PEOPLE_COLUMNS =
      List.of(
          "employee_no",
          "name",
          "email",
          "role",
          "job_level",
          "travel_willingness",
          "active",
          "skills");

  private static final DataFormatter FORMATTER = new DataFormatter();

  private WorkbookParser() {}

  static ParsedWorkbook parse(byte[] content) {
    try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(content))) {
      Sheet people = sheet(workbook, "people");
      ColumnMap peopleColumns = columns(people, PEOPLE_COLUMNS);
      List<SheetRow> rows = rows(people);
      if (rows.isEmpty()) {
        throw new UnreadableWorkbookException("工作簿是空的：people 表里没有任何数据行");
      }
      return new ParsedWorkbook(peopleColumns, rows);
    } catch (IOException | UnsupportedFileFormatException e) {
      throw new UnreadableWorkbookException("这个文件不是可读取的 .xlsx 工作簿");
    }
  }

  private static Sheet sheet(Workbook workbook, String name) {
    for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
      Sheet sheet = workbook.getSheetAt(i);
      if (sheet.getSheetName().equalsIgnoreCase(name)) {
        return sheet;
      }
    }
    throw new UnreadableWorkbookException("工作簿里没有 " + name + " 工作表");
  }

  private static ColumnMap columns(Sheet sheet, List<String> fields) {
    Row header = sheet.getRow(sheet.getFirstRowNum());
    ColumnMap columns =
        ColumnMap.of(header == null ? List.of() : cells(header), fields);
    if (!columns.missing().isEmpty()) {
      throw new UnreadableWorkbookException(
          sheet.getSheetName()
              + " 表缺少这些列："
              + String.join("、", columns.missing()));
    }
    return columns;
  }

  /** Data rows below the header, without the rows that carry nothing; row numbers stay 1-based. */
  private static List<SheetRow> rows(Sheet sheet) {
    List<SheetRow> rows = new ArrayList<>();
    for (int i = sheet.getFirstRowNum() + 1; i <= sheet.getLastRowNum(); i++) {
      Row row = sheet.getRow(i);
      if (row == null) {
        continue;
      }
      List<String> cells = cells(row);
      if (cells.stream().allMatch(String::isEmpty)) {
        continue;
      }
      rows.add(new SheetRow(i + 1, cells));
    }
    return rows;
  }

  private static List<String> cells(Row row) {
    List<String> cells = new ArrayList<>();
    for (int i = 0; i < row.getLastCellNum(); i++) {
      Cell cell = row.getCell(i);
      cells.add(cell == null ? "" : FORMATTER.formatCellValue(cell).trim());
    }
    return cells;
  }
}
