package com.example.staffing.domain;

/**
 * One allocation as the list and the form see it: who, which project, the inclusive range, and how
 * it came about — `MANUAL` is editable by hand, `CONFIRMATION` only goes away with a release or a
 * project end.
 */
public record AllocationView(
    long id, String personNo, String project, String startDate, String endDate, String source) {}
