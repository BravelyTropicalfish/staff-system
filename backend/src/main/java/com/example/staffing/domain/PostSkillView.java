package com.example.staffing.domain;

/**
 * One skill requirement as the post is shown: the kind tells required from bonus, and the bonus row
 * carries `informational` so the screen labels it without knowing the vocabulary.
 */
public record PostSkillView(String skill, String kind, String band, boolean informational) {}
