package com.example.staffing.domain;

import java.util.List;

/**
 * What the validator made of one file: the rows that are ready to write in stored spelling, and the
 * rows that are not. The caller writes nothing while {@code errors} is not empty.
 */
record ImportResult(
    List<PersonImportRow> people, List<SkillImportRow> skills, List<RowError> errors) {

  boolean isRejected() {
    return !errors.isEmpty();
  }
}
