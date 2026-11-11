package com.example.staffing.domain;

import java.util.List;

/**
 * One person on the suitable list: who they are, what the row must show, the gates they passed and
 * the post's skill rows with the bands they hold. There is no score, grade or rank position — the
 * coordinator decides whom to recommend, and the row does not vote.
 */
public record SuitabilityRow(
    String employeeNo,
    String name,
    String role,
    String jobLevel,
    String travel,
    List<String> gates,
    List<CandidateSkillView> skills) {}
