package com.example.staffing.domain;

import com.example.staffing.data.PersonRepository;
import com.example.staffing.data.PostRepository;
import com.example.staffing.data.SkillRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The search of the pool for one post. The pool is the people who hold the post's role, since the
 * gate says a person of another role is never a same-role candidate; everyone in it is evaluated, so
 * an inactive person is reported under G1 rather than dropped silently. Passing every gate is the
 * whole definition of suitable — nothing is scored, graded or ranked, and the order is the name and
 * then the employee number, so the same post and pool always answer the same list.
 *
 * <p>Only when that list is empty does the cross-role tier open: people of other roles answering G1,
 * G5 and G6 plus X1 (a band above the requirement) and X2 (a skill of the post role's domain at
 * `Practitioner` or above). X1 stands in for G3 and X2 for G2 and G4, and the post's required rows
 * never gate a cross-role person. A role with no domain skills admits nobody rather than widening.
 */
@Service
public class MatchingService {

  private static final String X1 = "X1";
  private static final String X2 = "X2";
  private static final int PRACTITIONER = Bands.rank(Bands.PROFICIENCIES, "Practitioner");

  /**
   * Name ascending, then the employee number. Plain code-unit order, so the answer never depends on
   * a database collation.
   */
  private static final Comparator<Candidate> BY_NAME =
      Comparator.comparing(Candidate::name).thenComparing(Candidate::employeeNo);

  private final PostRepository posts;
  private final PersonRepository people;
  private final SkillRepository skills;

  public MatchingService(
      PostRepository posts, PersonRepository people, SkillRepository skills) {
    this.posts = posts;
    this.people = people;
    this.skills = skills;
  }

  @Transactional(readOnly = true)
  public CandidateSearchView search(long postId) {
    PostRepository.PostRow post =
        posts.findById(postId)
            .orElseThrow(() -> new NoSuchElementException("No post " + postId));
    List<PostRepository.SkillRow> rows = posts.skillsOf(postId);
    PostRequirement requirement = requirement(post, rows);
    Map<String, Map<Long, String>> bands = bandsByPerson();
    Map<String, List<Candidate.Allocation>> occupancy = occupancyByPerson();
    List<PersonRepository.PersonRow> everyone = people.findAll();

    Map<String, Integer> counts = new LinkedHashMap<>();
    for (Gate gate : Gate.values()) {
      counts.put(gate.code(), 0);
    }
    counts.put(X1, 0);
    counts.put(X2, 0);

    List<SuitabilityRow> suitable = new ArrayList<>();
    List<ExclusionRow> excluded = new ArrayList<>();
    for (Candidate candidate : pool(post, everyone, bands, occupancy)) {
      Gates.Evaluation evaluation = Gates.evaluate(candidate, requirement);
      if (evaluation.admitted()) {
        suitable.add(row(candidate, rows, evaluation));
        continue;
      }
      Gates.Result failure = evaluation.firstFailure();
      excluded.add(
          new ExclusionRow(
              candidate.employeeNo(), candidate.name(), candidate.role(), failure.gate().code(),
              failure.reason()));
      counts.merge(failure.gate().code(), 1, Integer::sum);
    }

    List<FallbackRow> fallback = new ArrayList<>();
    String fallbackReason = null;
    if (suitable.isEmpty()) {
      fallbackReason = "no " + post.role() + " person passes the gates";
      List<SkillRepository.SkillRow> domain = skills.findByDomain(post.roleId());
      for (Candidate candidate : otherRoles(post, everyone, bands, occupancy)) {
        Qualifying qualifying = qualifying(candidate, domain);
        Refusal refusal = refusal(candidate, post, domain.isEmpty(), qualifying);
        if (refusal != null) {
          excluded.add(
              new ExclusionRow(
                  candidate.employeeNo(), candidate.name(), candidate.role(), refusal.gate(),
                  refusal.reason()));
          counts.merge(refusal.gate(), 1, Integer::sum);
          continue;
        }
        fallback.add(
            new FallbackRow(
                candidate.employeeNo(),
                candidate.name(),
                candidate.role(),
                candidate.jobLevel(),
                candidate.travel(),
                bandsAbove(candidate, post),
                qualifying.skill(),
                qualifying.band()));
      }
    }
    return new CandidateSearchView(
        List.copyOf(suitable), List.copyOf(excluded), counts, List.copyOf(fallback), fallbackReason);
  }

  /** The people who hold the post's role, in name order. */
  private static List<Candidate> pool(
      PostRepository.PostRow post,
      List<PersonRepository.PersonRow> everyone,
      Map<String, Map<Long, String>> bands,
      Map<String, List<Candidate.Allocation>> occupancy) {
    return everyone.stream()
        .filter(row -> row.roleId() == post.roleId())
        .map(row -> candidate(row, bands, occupancy))
        .sorted(BY_NAME)
        .toList();
  }

  /** The fallback's people: every other role, judged by X1 and X2 instead of G2, G3 and G4. */
  private static List<Candidate> otherRoles(
      PostRepository.PostRow post,
      List<PersonRepository.PersonRow> everyone,
      Map<String, Map<Long, String>> bands,
      Map<String, List<Candidate.Allocation>> occupancy) {
    return everyone.stream()
        .filter(row -> row.roleId() != post.roleId())
        .map(row -> candidate(row, bands, occupancy))
        .sorted(BY_NAME)
        .toList();
  }

  /**
   * Why a person of another role does not qualify, or null when they do. G1, G5 and G6 still apply;
   * X1 stands in for G3 and X2 for G2 and G4, so the post's required rows never gate this tier.
   */
  private static Refusal refusal(
      Candidate candidate,
      PostRepository.PostRow post,
      boolean domainEmpty,
      Qualifying qualifying) {
    if (!candidate.active()) {
      return new Refusal("G1", "the person is not active");
    }
    if (!Bands.meets(Bands.TRAVEL, candidate.travel(), post.travel())) {
      return new Refusal(
          "G5",
          "the travel willingness "
              + candidate.travel()
              + " is below the required "
              + post.travel());
    }
    for (Candidate.Allocation allocation : candidate.allocations()) {
      if (Ranges.intersects(allocation.start(), allocation.end(), post.startDate(), post.endDate())) {
        return new Refusal(
            "G6",
            "the allocation "
                + allocation.start()
                + " to "
                + allocation.end()
                + " intersects the post's "
                + post.startDate()
                + " to "
                + post.endDate());
      }
    }
    if (bandsAbove(candidate, post) < 1) {
      return new Refusal(
          X1,
          Bands.rank(Bands.JOB_LEVELS, post.minJobLevel()) == Bands.JOB_LEVELS.size() - 1
              ? "cross-role: no band sits above " + post.minJobLevel()
              : "cross-role: level not above the requirement");
    }
    if (qualifying == null) {
      return new Refusal(
          X2,
          domainEmpty
              ? "cross-role: " + post.role() + " has no skill to qualify on"
              : "cross-role: no domain skill at Practitioner");
    }
    return null;
  }

  /** How many bands the person sits above the post's requirement on the job level scale. */
  private static int bandsAbove(Candidate candidate, PostRepository.PostRow post) {
    return Bands.rank(Bands.JOB_LEVELS, candidate.jobLevel())
        - Bands.rank(Bands.JOB_LEVELS, post.minJobLevel());
  }

  /** The domain skill the person holds highest at `Practitioner` or above, or null. */
  private static Qualifying qualifying(
      Candidate candidate, List<SkillRepository.SkillRow> domain) {
    Qualifying best = null;
    for (SkillRepository.SkillRow skill : domain) {
      String band = candidate.skillBands().get(skill.id());
      if (band == null || Bands.rank(Bands.PROFICIENCIES, band) < PRACTITIONER) {
        continue;
      }
      if (best == null) {
        best = new Qualifying(skill.name(), band);
        continue;
      }
      int rank = Bands.rank(Bands.PROFICIENCIES, band);
      int bestRank = Bands.rank(Bands.PROFICIENCIES, best.band());
      if (rank > bestRank || (rank == bestRank && skill.name().compareTo(best.skill()) < 0)) {
        best = new Qualifying(skill.name(), band);
      }
    }
    return best;
  }

  /** The gate a cross-role person failed, one of G1, G5, G6, X1 or X2, and its reason. */
  private record Refusal(String gate, String reason) {}

  /** The domain skill that admits a cross-role person, with the band held. */
  private record Qualifying(String skill, String band) {}

  /** The post as the gates read it; only the {@code required} rows gate anybody. */
  private static PostRequirement requirement(
      PostRepository.PostRow post, List<PostRepository.SkillRow> rows) {
    List<PostRequirement.SkillNeed> required =
        rows.stream()
            .filter(row -> "REQUIRED".equals(row.kind()))
            .map(row -> new PostRequirement.SkillNeed(row.skillId(), row.name(), row.band()))
            .toList();
    return new PostRequirement(
        post.roleId(),
        post.role(),
        post.minJobLevel(),
        post.travel(),
        post.startDate(),
        post.endDate(),
        required);
  }

  private static Candidate candidate(
      PersonRepository.PersonRow row,
      Map<String, Map<Long, String>> bands,
      Map<String, List<Candidate.Allocation>> occupancy) {
    return new Candidate(
        row.employeeNo(),
        row.name(),
        row.active(),
        row.roleId(),
        row.role(),
        row.jobLevel(),
        row.travel(),
        bands.getOrDefault(row.employeeNo(), Map.of()),
        occupancy.getOrDefault(row.employeeNo(), List.of()));
  }

  /**
   * The row the coordinator reads: every gate that ran passed, so all six are listed as passed, and
   * each of the post's skill rows carries the band this person holds.
   */
  private static SuitabilityRow row(
      Candidate candidate, List<PostRepository.SkillRow> rows, Gates.Evaluation evaluation) {
    List<String> gates =
        evaluation.results().stream()
            .filter(Gates.Result::passed)
            .map(result -> result.gate().code())
            .toList();
    List<CandidateSkillView> skills =
        rows.stream()
            .map(
                row ->
                    new CandidateSkillView(
                        row.name(),
                        "REQUIRED".equals(row.kind()) ? "required" : "bonus",
                        candidate.skillBands().get(row.skillId()),
                        "REQUIRED".equals(row.kind()) ? null : row.band()))
            .toList();
    return new SuitabilityRow(
        candidate.employeeNo(),
        candidate.name(),
        candidate.role(),
        candidate.jobLevel(),
        candidate.travel(),
        gates,
        skills);
  }

  private Map<String, Map<Long, String>> bandsByPerson() {
    Map<String, Map<Long, String>> bands = new HashMap<>();
    for (PersonRepository.PersonBandRow row : people.allBands()) {
      bands.computeIfAbsent(row.employeeNo(), key -> new HashMap<>()).put(row.skillId(), row.band());
    }
    return bands;
  }

  private Map<String, List<Candidate.Allocation>> occupancyByPerson() {
    Map<String, List<Candidate.Allocation>> occupancy = new HashMap<>();
    for (PersonRepository.PersonRangeRow row : people.allAllocations()) {
      occupancy
          .computeIfAbsent(row.employeeNo(), key -> new ArrayList<>())
          .add(new Candidate.Allocation(row.startDate(), row.endDate()));
    }
    return occupancy;
  }
}
