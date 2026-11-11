package com.example.staffing.domain;

/**
 * One skill row as the form sends it: a catalog skill, its kind (`required` or `bonus`) and the
 * band — the minimum for a required row, the target for a bonus row.
 */
public record SkillDraft(String skill, String kind, String band) {}
