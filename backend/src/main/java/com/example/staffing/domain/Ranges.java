package com.example.staffing.domain;

import java.time.LocalDate;

/**
 * Inclusive date ranges. An allocation occupies every day from its start through its end, so two
 * ranges intersect when they share at least one day — a range ending on the other's start day still
 * blocks (G6). Written once here because the gates, the profile and the manual form all ask it.
 */
public final class Ranges {

  private Ranges() {}

  /** True when {@code [start, end]} and {@code [otherStart, otherEnd]} share at least one day. */
  public static boolean intersects(
      LocalDate start, LocalDate end, LocalDate otherStart, LocalDate otherEnd) {
    return !end.isBefore(otherStart) && !otherEnd.isBefore(start);
  }
}
