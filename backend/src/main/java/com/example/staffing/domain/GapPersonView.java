package com.example.staffing.domain;

/**
 * A confirmed person in the gap view: the assignment behind the seat — the one id a release takes —
 * when it was confirmed, and whether the report still marks them active. An inactive person keeps
 * the seat filled and is flagged, never dropped.
 */
public record GapPersonView(
    long assignmentId, String personNo, String name, String confirmedAt, boolean active) {}
