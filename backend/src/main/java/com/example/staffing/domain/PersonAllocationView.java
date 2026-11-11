package com.example.staffing.domain;

/**
 * One allocation on the profile: which project, the inclusive range, how it came about, and whether
 * its end date has passed. Past allocations are listed, not hidden, and only marked.
 */
public record PersonAllocationView(
    long id, String project, String startDate, String endDate, String source, boolean ended) {}
