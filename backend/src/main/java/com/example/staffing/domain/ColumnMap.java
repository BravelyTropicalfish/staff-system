package com.example.staffing.domain;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Reads a sheet by column name, not by position. The people export talks about {@code Employee No}
 * while the contract says {@code employee_no}, so labels are compared on their letters and digits
 * only: case, spaces and underscores do not matter. Keeping that translation here means a real
 * export with a different spelling is a change to this class, never a change to the rules.
 */
final class ColumnMap {

  private final Map<String, Integer> indexes;
  private final List<String> missing;

  private ColumnMap(Map<String, Integer> indexes, List<String> missing) {
    this.indexes = indexes;
    this.missing = missing;
  }

  static ColumnMap of(List<String> headers, List<String> fields) {
    Map<String, Integer> byLabel = new LinkedHashMap<>();
    for (int i = 0; i < headers.size(); i++) {
      byLabel.putIfAbsent(normalise(headers.get(i)), i);
    }
    Map<String, Integer> indexes = new LinkedHashMap<>();
    List<String> missing = new ArrayList<>();
    for (String field : fields) {
      Integer index = byLabel.get(normalise(field));
      if (index == null) {
        missing.add(field);
      } else {
        indexes.put(field, index);
      }
    }
    return new ColumnMap(indexes, missing);
  }

  /** Column names the sheet does not carry; the workbook is refused while this is not empty. */
  List<String> missing() {
    return List.copyOf(missing);
  }

  /** The cell of this row under the named column, trimmed; empty when the column is absent. */
  Optional<String> value(SheetRow row, String field) {
    Integer index = indexes.get(field);
    if (index == null) {
      return Optional.empty();
    }
    if (index >= row.cells().size()) {
      return Optional.of("");
    }
    String cell = row.cells().get(index);
    return Optional.of(cell == null ? "" : cell.trim());
  }

  private static String normalise(String label) {
    StringBuilder letters = new StringBuilder(label.length());
    for (char c : label.toCharArray()) {
      if (Character.isLetterOrDigit(c)) {
        letters.append(Character.toLowerCase(c));
      }
    }
    return letters.toString();
  }
}
