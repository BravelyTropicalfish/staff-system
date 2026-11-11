package com.example.staffing.domain;

/** One person as a list shows them: the master data and the one role, never the role list. */
public record PersonSummaryView(
    String employeeNo, String name, String role, String jobLevel, String travel, boolean active) {}
