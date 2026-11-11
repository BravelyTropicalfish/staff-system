package com.example.staffing.domain;

/**
 * One of the post's skill rows as a candidate holds it: the band held ({@code null} when the person
 * does not hold the skill at all) and, for a bonus row, the target the post asks for. A bonus
 * shortfall travels here as information and never as a reason to exclude.
 */
public record CandidateSkillView(String skill, String kind, String band, String target) {}
