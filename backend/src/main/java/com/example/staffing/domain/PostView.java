package com.example.staffing.domain;

import java.util.List;

/**
 * A post as the list and the form see it. The status and the remaining headcount are derived from
 * the confirmed people, never read from a field somebody could edit.
 */
public record PostView(
    long id,
    long projectId,
    String project,
    String title,
    String role,
    String minJobLevel,
    int headcount,
    String startDate,
    String endDate,
    String travel,
    String status,
    int confirmed,
    int remaining,
    List<PostSkillView> skills) {}
