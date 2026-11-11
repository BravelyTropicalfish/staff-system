package com.example.staffing.domain;

/**
 * One row the import refused. The sheet and the spreadsheet row number say where to look, the key
 * value says which employee the row was about, and the message says what to fix.
 */
public record RowError(String sheet, int rowNo, String key, String message) {}
