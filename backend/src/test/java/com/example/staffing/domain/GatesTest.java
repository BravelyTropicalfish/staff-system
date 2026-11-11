package com.example.staffing.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * T18, the gate engine: G1 to G6 in the fixed order, the first failure is the reported reason, and
 * G6 is closed at both ends. The predicate itself is pinned in {@link RangesTest}; here it is the
 * wiring of it to the gate that is under test.
 */
class GatesTest {

  private static final long DEV = 1L;
  private static final long QA = 2L;
  private static final long BACKEND = 10L;
  private static final long TESTING = 11L;

  private static final LocalDate POST_START = LocalDate.parse("2027-05-01");
  private static final LocalDate POST_END = LocalDate.parse("2027-07-31");

  @Test
  void theGatesAreWalkedInTheSpecifiedOrder() {
    assertThat(Gate.values()).extracting(Gate::code).containsExactly("G1", "G2", "G3", "G4", "G5", "G6");
  }

  @Test
  void aPersonPassingEveryGateIsAdmitted() {
    Gates.Evaluation evaluation = Gates.evaluate(candidate().build(), post().build());

    assertThat(evaluation.admitted()).isTrue();
    assertThat(evaluation.firstFailure()).isNull();
    assertThat(evaluation.results()).hasSize(6);
    assertThat(evaluation.results()).allMatch(Gates.Result::passed);
  }

  @Test
  void anInactivePersonIsRefusedByG1() {
    Gates.Evaluation evaluation = Gates.evaluate(candidate().active(false).build(), post().build());

    assertThat(evaluation.firstFailure().gate()).isEqualTo(Gate.ACTIVE);
    assertThat(evaluation.firstFailure().reason()).isEqualTo("the person is not active");
  }

  @Test
  void anotherRoleIsRefusedByG2() {
    Gates.Evaluation evaluation =
        Gates.evaluate(candidate().role(QA, "QA").band(TESTING, "Practitioner").build(), post().build());

    assertThat(evaluation.firstFailure().gate()).isEqualTo(Gate.ROLE);
    assertThat(evaluation.firstFailure().reason()).isEqualTo("the role QA is not the post's DEV");
  }

  @Test
  void roleEqualityIsTheRoleRowNotItsName() {
    // A role renamed while nothing referenced it keeps its id; the post's role is still that role.
    Gates.Evaluation evaluation =
        Gates.evaluate(candidate().role(DEV, "Development").build(), post().build());

    assertThat(evaluation.admitted()).isTrue();
  }

  @Test
  void aJobLevelBelowTheRequirementIsRefusedByG3() {
    Gates.Evaluation evaluation =
        Gates.evaluate(candidate().jobLevel("Junior").build(), post().build());

    assertThat(evaluation.firstFailure().gate()).isEqualTo(Gate.JOB_LEVEL);
    assertThat(evaluation.firstFailure().reason())
        .isEqualTo("the job level Junior is below the required Senior");
  }

  @Test
  void theRequiredBandItselfPassesG3() {
    Gates.Evaluation evaluation =
        Gates.evaluate(candidate().jobLevel("Senior").build(), post().minJobLevel("Senior").build());

    assertThat(evaluation.admitted()).isTrue();
  }

  @Test
  void theFirstFailingGateIsTheReportedReason() {
    // The role matches, the job level is below and 后端开发 is below: the job level is reported.
    Gates.Evaluation evaluation =
        Gates.evaluate(
            candidate().jobLevel("Junior").band(BACKEND, "Novice").build(), post().build());

    assertThat(evaluation.firstFailure().gate()).isEqualTo(Gate.JOB_LEVEL);
    assertThat(evaluation.results())
        .extracting(Gates.Result::gate)
        .containsExactly(Gate.ACTIVE, Gate.ROLE, Gate.JOB_LEVEL);
  }

  @Test
  void aRequiredSkillThePersonDoesNotHoldIsRefusedByG4() {
    Gates.Evaluation evaluation =
        Gates.evaluate(candidate().withoutBand(BACKEND).build(), post().build());

    assertThat(evaluation.firstFailure().gate()).isEqualTo(Gate.REQUIRED_SKILL);
    assertThat(evaluation.firstFailure().reason()).isEqualTo("the person does not hold 后端开发");
  }

  @Test
  void aRequiredSkillBelowItsBandIsRefusedByG4() {
    Gates.Evaluation evaluation =
        Gates.evaluate(candidate().band(BACKEND, "Advanced beginner").build(), post().build());

    assertThat(evaluation.firstFailure().gate()).isEqualTo(Gate.REQUIRED_SKILL);
    assertThat(evaluation.firstFailure().reason())
        .isEqualTo("后端开发 at Advanced beginner is below the required Practitioner");
  }

  @Test
  void everyRequiredRowMustBeHeld() {
    // 后端开发 is held; the second required row is not.
    Gates.Evaluation evaluation =
        Gates.evaluate(
            candidate().build(), post().requires(TESTING, "测试", "Practitioner").build());

    assertThat(evaluation.firstFailure().gate()).isEqualTo(Gate.REQUIRED_SKILL);
    assertThat(evaluation.firstFailure().reason()).isEqualTo("the person does not hold 测试");
  }

  @Test
  void aPostWithoutRequiredRowsAsksNothingOfTheSkills() {
    Gates.Evaluation evaluation = Gates.evaluate(candidate().build(), post().noSkills().build());

    assertThat(evaluation.admitted()).isTrue();
  }

  @Test
  void travelWillingnessBelowTheRequirementIsRefusedByG5() {
    Gates.Evaluation evaluation =
        Gates.evaluate(candidate().travel("limited").build(), post().travel("full").build());

    assertThat(evaluation.firstFailure().gate()).isEqualTo(Gate.TRAVEL);
    assertThat(evaluation.firstFailure().reason())
        .isEqualTo("the travel willingness limited is below the required full");
  }

  @Test
  void theRequiredTravelRankItselfPassesG5() {
    Gates.Evaluation evaluation =
        Gates.evaluate(candidate().travel("limited").build(), post().travel("limited").build());

    assertThat(evaluation.admitted()).isTrue();
  }

  @Test
  void anAllocationEndingOnThePostStartBlocks() {
    Gates.Evaluation evaluation =
        Gates.evaluate(candidate().allocated("2027-03-01", "2027-05-01").build(), post().build());

    assertThat(evaluation.firstFailure().gate()).isEqualTo(Gate.DATES);
    assertThat(evaluation.firstFailure().reason())
        .isEqualTo(
            "the allocation 2027-03-01 to 2027-05-01 intersects the post's 2027-05-01 to 2027-07-31");
  }

  @Test
  void anAllocationStartingOnThePostEndBlocks() {
    Gates.Evaluation evaluation =
        Gates.evaluate(candidate().allocated("2027-07-31", "2027-09-30").build(), post().build());

    assertThat(evaluation.firstFailure().gate()).isEqualTo(Gate.DATES);
  }

  @Test
  void aPostAfterTheAllocationStaysOpen() {
    Gates.Evaluation evaluation =
        Gates.evaluate(candidate().allocated("2027-01-01", "2027-03-31").build(), post().build());

    assertThat(evaluation.admitted()).isTrue();
  }

  @Test
  void aPostBeforeTheAllocationStaysOpen() {
    Gates.Evaluation evaluation =
        Gates.evaluate(candidate().allocated("2027-09-01", "2027-10-31").build(), post().build());

    assertThat(evaluation.admitted()).isTrue();
  }

  private static Builder candidate() {
    return new Builder();
  }

  private static PostBuilder post() {
    return new PostBuilder();
  }

  private static final class Builder {

    private final Map<Long, String> bands = new LinkedHashMap<>(Map.of(BACKEND, "Practitioner"));
    private final List<Candidate.Allocation> allocations = new ArrayList<>();
    private boolean active = true;
    private long roleId = DEV;
    private String role = "DEV";
    private String jobLevel = "Senior";
    private String travel = "full";

    Builder active(boolean value) {
      this.active = value;
      return this;
    }

    Builder role(long id, String name) {
      this.roleId = id;
      this.role = name;
      return this;
    }

    Builder jobLevel(String value) {
      this.jobLevel = value;
      return this;
    }

    Builder travel(String value) {
      this.travel = value;
      return this;
    }

    Builder band(long skillId, String value) {
      bands.put(skillId, value);
      return this;
    }

    Builder withoutBand(long skillId) {
      bands.remove(skillId);
      return this;
    }

    Builder allocated(String start, String end) {
      allocations.add(new Candidate.Allocation(LocalDate.parse(start), LocalDate.parse(end)));
      return this;
    }

    Candidate build() {
      return new Candidate(
          "E100",
          "Ada Lovelace",
          active,
          roleId,
          role,
          jobLevel,
          travel,
          Map.copyOf(bands),
          List.copyOf(allocations));
    }
  }

  private static final class PostBuilder {

    private final List<PostRequirement.SkillNeed> required =
        new ArrayList<>(List.of(new PostRequirement.SkillNeed(BACKEND, "后端开发", "Practitioner")));
    private long roleId = DEV;
    private String role = "DEV";
    private String minJobLevel = "Senior";
    private String travel = "full";

    PostBuilder minJobLevel(String value) {
      this.minJobLevel = value;
      return this;
    }

    PostBuilder travel(String value) {
      this.travel = value;
      return this;
    }

    PostBuilder requires(long skillId, String skill, String band) {
      required.add(new PostRequirement.SkillNeed(skillId, skill, band));
      return this;
    }

    PostBuilder noSkills() {
      required.clear();
      return this;
    }

    PostRequirement build() {
      return new PostRequirement(
          roleId,
          role,
          minJobLevel,
          travel,
          POST_START,
          POST_END,
          List.copyOf(required));
    }
  }
}
