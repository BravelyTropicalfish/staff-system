package com.example.staffing.domain;

/** A {@code skills} row with the catalog spelling already resolved, ready to be written. */
record SkillImportRow(int rowNo, String employeeNo, long skillId, String band) {}
