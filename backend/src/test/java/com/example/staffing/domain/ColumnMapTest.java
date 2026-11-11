package com.example.staffing.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * T30, the reader under the parser. WorkbookParserTest proves it end to end through real sheets;
 * these cases are the three cell shapes a real export produces but a fixture never writes: a column
 * the sheet does not carry, a row shorter than the header, and a cell that holds nothing at all.
 */
class ColumnMapTest {

  private static final List<String> FIELDS = List.of("employee_no", "name", "email");

  @Test
  void aColumnTheSheetDoesNotCarryIsMissingAndItsCellReadsEmpty() {
    ColumnMap columns = ColumnMap.of(List.of("Employee No", "Name"), FIELDS);

    assertThat(columns.missing()).containsExactly("email");
    assertThat(columns.value(new SheetRow(2, List.of("E100", "Ann")), "email")).isEmpty();
  }

  @Test
  void aRowShorterThanTheHeaderReadsAnEmptyCellRatherThanFailing() {
    ColumnMap columns = ColumnMap.of(WorkbookParser.PEOPLE_COLUMNS, WorkbookParser.PEOPLE_COLUMNS);

    assertThat(columns.value(new SheetRow(2, List.of("E100")), "travel_willingness"))
        .contains("");
  }

  @Test
  void aCellThatHoldsNothingReadsEmptyAndAValueIsTrimmed() {
    ColumnMap columns = ColumnMap.of(List.of("employee_no", "name"), FIELDS);

    assertThat(columns.value(new SheetRow(2, Arrays.asList("  E100  ", null)), "employee_no"))
        .contains("E100");
    assertThat(columns.value(new SheetRow(2, Arrays.asList("E100", null)), "name")).contains("");
  }
}
