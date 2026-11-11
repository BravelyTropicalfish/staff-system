package com.example.staffing.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * A post as the form sends it. Every field is required on create; on an edit a null field means
 * "leave it as it is", and an empty skill list means "no skill requirement left". The headcount is
 * a decimal so a fractional number can be refused with its own reason instead of being truncated.
 */
public record PostDraft(
    String title,
    String role,
    String minJobLevel,
    BigDecimal headcount,
    LocalDate startDate,
    LocalDate endDate,
    String travel,
    List<SkillDraft> skills) {}
