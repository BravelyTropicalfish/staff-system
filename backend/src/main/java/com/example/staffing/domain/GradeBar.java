package com.example.staffing.domain;

/**
 * One bar on the grade axis. The four bands are always present in band order, so a band with nobody
 * reads zero instead of vanishing; {@code share} is a percentage with one decimal.
 */
public record GradeBar(String grade, int count, double share) {}
