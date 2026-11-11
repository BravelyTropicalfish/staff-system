package com.example.staffing.domain;

import java.util.List;

/**
 * The reason a recommendation was made, frozen at the moment it was made: the gates the person
 * passed and the post's skill rows with the bands held then. A person admitted through the
 * cross-role fallback carries its evidence too — their role, how many bands they sit above the
 * requirement and the domain skill that qualified them — and no same-role skill rows, because the
 * post's required rows never gated that tier.
 *
 * <p>Written once into {@code snapshot_json} and never updated: a later band, a deactivated person
 * or a changed post does not rewrite the reason the coordinator gave.
 */
public record RecommendationSnapshot(
    String personNo,
    String role,
    String jobLevel,
    String travel,
    List<String> gates,
    List<Skill> skills,
    CrossRole crossRole) {

  /** One of the post's skill rows as the person held it; a bonus row carries the target it misses. */
  public record Skill(String skill, String kind, String band, String target) {}

  /** The evidence the fallback admitted a person on: the gap in bands and the qualifying skill. */
  public record CrossRole(int bandsAbove, String skill, String band) {}
}
