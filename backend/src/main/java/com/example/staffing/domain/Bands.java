package com.example.staffing.domain;

import java.util.List;

/**
 * The three ordered scales of the pool. Nothing stores a numeric level: a band is compared by its
 * position on its scale and displayed by its name. The import rules, the profile and the gates all
 * read the scales from here, so a scale exists once.
 */
public final class Bands {

  public static final List<String> JOB_LEVELS = List.of("Junior", "Senior", "Lead", "Principal");
  public static final List<String> PROFICIENCIES =
      List.of("Novice", "Advanced beginner", "Practitioner", "Proficient", "Expert");
  public static final List<String> TRAVEL = List.of("none", "limited", "full");

  private Bands() {}

  /** Position of the band on its scale; a lower number is a lower band. */
  public static int rank(List<String> scale, String band) {
    int index = scale.indexOf(band);
    if (index < 0) {
      throw new IllegalArgumentException("unknown band " + band);
    }
    return index;
  }

  /** True when {@code band} is at or above {@code required} on the same scale. */
  public static boolean meets(List<String> scale, String band, String required) {
    return rank(scale, band) >= rank(scale, required);
  }
}
