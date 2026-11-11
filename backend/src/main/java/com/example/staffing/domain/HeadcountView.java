package com.example.staffing.domain;

import java.util.List;

/**
 * The headcount report as one answer: the active pool's total plus one bar per category on each
 * axis. It is derived on every read and never stored, so the totals always describe the pool as it
 * stands and both axis sums equal {@code total}.
 */
public record HeadcountView(int total, List<RoleBar> byRole, List<GradeBar> byGrade) {}
