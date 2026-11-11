package com.example.staffing.domain;

/**
 * The file could not be opened as the two-sheet workbook. It is refused before a batch exists,
 * because a file the system cannot read is not a batch that was rejected: there is nothing to list.
 */
public class UnreadableWorkbookException extends RuntimeException {

  public UnreadableWorkbookException(String message) {
    super(message);
  }
}
