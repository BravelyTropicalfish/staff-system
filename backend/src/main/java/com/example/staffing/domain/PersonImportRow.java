package com.example.staffing.domain;

/** A {@code people} row with the catalog spelling already resolved, ready to be written. */
record PersonImportRow(
    int rowNo,
    String employeeNo,
    String name,
    String email,
    long roleId,
    String jobLevel,
    String travel,
    boolean active) {}
