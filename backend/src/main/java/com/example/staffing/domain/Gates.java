package com.example.staffing.domain;

import java.util.ArrayList;
import java.util.List;

/**
 * The gate engine: one walk from G1 to G6 that stops at the first gate a person fails, so that
 * failure is the person's single exclusion reason. Nothing is scored — passing every gate is the
 * whole definition of suitable.
 */
public final class Gates {

  private Gates() {}

  /** What one gate decided: passed, or the reason it refused the person. */
  public record Result(Gate gate, boolean passed, String reason) {}

  /**
   * The gates that ran, in order. The walk stops at the first failure, so the list ends there and
   * its passed prefix is what a row may show.
   */
  public record Evaluation(List<Result> results) {

    public boolean admitted() {
      return firstFailure() == null;
    }

    /** The reported exclusion reason, or null when the person passed every gate. */
    public Result firstFailure() {
      return results.stream().filter(result -> !result.passed()).findFirst().orElse(null);
    }
  }

  public static Evaluation evaluate(Candidate candidate, PostRequirement post) {
    List<Result> results = new ArrayList<>();
    for (Gate gate : Gate.values()) {
      String reason = refuse(gate, candidate, post);
      results.add(new Result(gate, reason == null, reason));
      if (reason != null) {
        break;
      }
    }
    return new Evaluation(List.copyOf(results));
  }

  /** The reason this gate refuses the person, or null when the person passes it. */
  private static String refuse(Gate gate, Candidate candidate, PostRequirement post) {
    return switch (gate) {
      case ACTIVE -> candidate.active() ? null : "该人员已停用";
      case ROLE ->
          candidate.roleId() == post.roleId()
              ? null
              : "角色 " + candidate.role() + " 不是岗位要求的 " + post.role();
      case JOB_LEVEL ->
          Bands.meets(Bands.JOB_LEVELS, candidate.jobLevel(), post.minJobLevel())
              ? null
              : "职级 " + candidate.jobLevel() + " 低于要求的 " + post.minJobLevel();
      case REQUIRED_SKILL -> skillShortfall(candidate, post);
      case TRAVEL ->
          Bands.meets(Bands.TRAVEL, candidate.travel(), post.travel())
              ? null
              : "差旅意愿 " + candidate.travel() + " 低于要求的 " + post.travel();
      case DATES -> blockingAllocation(candidate, post);
    };
  }

  /** The first required row the person does not hold at its band, in the post's own order. */
  private static String skillShortfall(Candidate candidate, PostRequirement post) {
    for (PostRequirement.SkillNeed need : post.requiredSkills()) {
      String band = candidate.skillBands().get(need.skillId());
      if (band == null) {
        return "未持有 " + need.skill();
      }
      if (!Bands.meets(Bands.PROFICIENCIES, band, need.band())) {
        return need.skill() + " 档位 " + band + " 低于要求的 " + need.band();
      }
    }
    return null;
  }

  /** The first allocation that shares a day with the post, at either boundary. */
  private static String blockingAllocation(Candidate candidate, PostRequirement post) {
    for (Candidate.Allocation allocation : candidate.allocations()) {
      if (Ranges.intersects(allocation.start(), allocation.end(), post.start(), post.end())) {
        return "已有占用 "
            + allocation.start()
            + " 至 "
            + allocation.end()
            + "，与岗位的 "
            + post.start()
            + " 至 "
            + post.end()
            + " 相交";
      }
    }
    return null;
  }
}
