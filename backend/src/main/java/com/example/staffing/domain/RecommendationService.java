package com.example.staffing.domain;

import com.example.staffing.data.PersonRepository;
import com.example.staffing.data.PostRepository;
import com.example.staffing.data.RecommendationRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.NoSuchElementException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Proposing a named candidate for a post. The coordinator alone recommends, and only while the post
 * still has a remaining slot: a cancelled or filled post refuses with its own code because the slot
 * is gone, not because the person is wrong.
 *
 * <p>The person must be on the list the search answers at that moment — a suitable row, or a
 * fallback row while that section is open. Whoever is off both lists is refused with the gate that
 * keeps them off, so the reason reads exactly as the exclusion list reads it, and the list cannot go
 * stale behind the coordinator's back. The snapshot is then written once: it copies the row that
 * admitted the person, including the fallback's evidence.
 *
 * <p>A proposal never reserves anybody, so the same person may wait on several posts at once; only a
 * second proposal for the same person on the same post is refused.
 */
@Service
public class RecommendationService {

  static final String POST_FILLED = "post filled";
  static final String POST_CANCELLED = "post cancelled";
  static final String ALREADY_PROPOSED = "already proposed";
  static final String NOT_A_CANDIDATE = "not a candidate";

  /** The gates the fallback ran and passed: X1 stands in for G3, X2 for G2 and G4. */
  private static final List<String> CROSS_ROLE_GATES = List.of("G1", "G5", "G6", "X1", "X2");

  private final PostRepository posts;
  private final PersonRepository people;
  private final RecommendationRepository recommendations;
  private final MatchingService matching;
  private final ObjectMapper json;

  public RecommendationService(
      PostRepository posts,
      PersonRepository people,
      RecommendationRepository recommendations,
      MatchingService matching,
      ObjectMapper json) {
    this.posts = posts;
    this.people = people;
    this.recommendations = recommendations;
    this.matching = matching;
    this.json = json;
  }

  @Transactional
  public RecommendationView recommend(long postId, String employeeNo, AuthenticatedUser actor) {
    PostRepository.PostRow post =
        posts.findById(postId)
            .orElseThrow(() -> new NoSuchElementException("No post " + postId));
    if (PostStatus.CANCELLED.equals(post.status())) {
      throw new RecommendationException(
          POST_CANCELLED, "the post is cancelled; nothing can be proposed for it");
    }
    if (post.confirmed() >= post.headcount()) {
      throw new RecommendationException(
          POST_FILLED, "the post is filled; a recommendation needs a remaining slot");
    }

    RecommendationSnapshot snapshot = snapshot(matching.search(postId), post, employeeNo);
    if (recommendations.pendingFor(postId, employeeNo)) {
      throw new RecommendationException(
          ALREADY_PROPOSED, "the person is proposed for this post already");
    }
    long id = recommendations.insert(postId, employeeNo, write(snapshot), actor.id());
    return RecommendationView.of(
        recommendations
            .findById(id)
            .orElseThrow(() -> new NoSuchElementException("No recommendation " + id)),
        json);
  }

  /**
   * A post's proposals for its screen. The coordinator reads every post's and with each proposal's
   * reason snapshot; a lead reads only their own project's and never the snapshot.
   */
  @Transactional(readOnly = true)
  public List<PostRecommendationView> list(long postId, AuthenticatedUser actor) {
    PostRepository.PostRow post =
        posts.findById(postId)
            .orElseThrow(() -> new NoSuchElementException("No post " + postId));
    if (!actor.isCoordinator() && !post.projectLead().equals(actor.username())) {
      throw new AccessDeniedException("this post is not yours");
    }
    boolean withSnapshot = actor.isCoordinator();
    return recommendations.findByPost(postId).stream()
        .map(row -> view(row, withSnapshot))
        .toList();
  }

  /** The row as the post's screen reads it, with the reason snapshot only when the caller owns it. */
  private PostRecommendationView view(RecommendationRepository.Row row, boolean withSnapshot) {
    return new PostRecommendationView(
        row.id(),
        row.personNo(),
        row.personName(),
        row.personActive(),
        row.state(),
        row.voidReason(),
        withSnapshot ? RecommendationView.of(row, json).snapshot() : null,
        row.proposedBy(),
        row.proposedAt().toString());
  }

  /** The snapshot of the row that admits the person, or the refusal that keeps them off. */
  private RecommendationSnapshot snapshot(
      CandidateSearchView search, PostRepository.PostRow post, String employeeNo) {
    for (SuitabilityRow row : search.suitable()) {
      if (row.employeeNo().equals(employeeNo)) {
        return new RecommendationSnapshot(
            row.employeeNo(),
            row.role(),
            row.jobLevel(),
            row.travel(),
            row.gates(),
            row.skills().stream()
                .map(skill -> new RecommendationSnapshot.Skill(
                    skill.skill(), skill.kind(), skill.band(), skill.target()))
                .toList(),
            null);
      }
    }
    for (FallbackRow row : search.fallback()) {
      if (row.employeeNo().equals(employeeNo)) {
        return new RecommendationSnapshot(
            row.employeeNo(),
            row.role(),
            row.jobLevel(),
            row.travel(),
            CROSS_ROLE_GATES,
            List.of(),
            new RecommendationSnapshot.CrossRole(row.bandsAbove(), row.skill(), row.band()));
      }
    }
    for (ExclusionRow row : search.excluded()) {
      if (row.employeeNo().equals(employeeNo)) {
        throw new RecommendationException(row.gate(), row.reason());
      }
    }
    if (people.findByEmployeeNo(employeeNo).isEmpty()) {
      throw new NoSuchElementException("No person " + employeeNo);
    }
    throw new RecommendationException(
        NOT_A_CANDIDATE,
        "the person is not a candidate for "
            + post.role()
            + "; the cross-role section is closed while a "
            + post.role()
            + " person is suitable");
  }

  private String write(RecommendationSnapshot snapshot) {
    try {
      return json.writeValueAsString(snapshot);
    } catch (JsonProcessingException impossible) {
      throw new IllegalStateException("the reason snapshot could not be written", impossible);
    }
  }
}
