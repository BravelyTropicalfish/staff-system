package com.example.staffing.domain;

import java.time.LocalDate;
import java.util.List;

/**
 * Everything the gates ask about one post: the role, the minimum job level, the travel willingness
 * it demands, the dates it runs, and its {@code required} skill rows in the order they are held.
 *
 * <p>Bonus rows never reach here, which is how a bonus shortfall stays information: a gate can only
 * refuse what it is given, and it is never given a bonus row.
 */
public record PostRequirement(
    long roleId,
    String role,
    String minJobLevel,
    String travel,
    LocalDate start,
    LocalDate end,
    List<SkillNeed> requiredSkills) {

  /** A {@code required} row: the skill and the band the post will not go below. */
  public record SkillNeed(long skillId, String skill, String band) {}
}
