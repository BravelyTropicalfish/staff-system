package com.example.staffing.domain;

import com.example.staffing.data.AllocationRepository;
import com.example.staffing.data.AssignmentRepository;
import com.example.staffing.data.NoticeRepository;
import com.example.staffing.data.PersonRepository;
import com.example.staffing.data.PostRepository;
import com.example.staffing.data.RecommendationRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The three decisions a proposal can reach. Only the lead of the post's project confirms or rejects
 * — the coordinator proposes and may withdraw their own proposal, but never confirms — and a
 * confirmation happens once per post and person.
 *
 * <p>A confirmation runs as one transaction that takes the post's row lock and then the person's,
 * always in that order, before it re-checks: two confirmations of one person queue instead of both
 * reading an unbooked person, and two on one post never over-fill it. Under those locks G1 and G6
 * are re-run: a person deactivated after the proposal, or an allocation recorded after it, refuses
 * the confirmation with that reason and leaves the recommendation `proposed`. Otherwise the
 * assignment and its allocation are written for the post's inclusive dates, the post's status is
 * re-derived from the confirmed count, and every other pending proposal of that person whose post
 * dates intersect the written allocation is voided with `person taken` and gets the coordinator's
 * notice; a proposal on disjoint dates stays `proposed`.
 */
@Service
public class ConfirmationService {

  static final String PERSON_TAKEN = "person taken";
  static final String NOT_PROPOSED = "not proposed";
  static final String POST_FILLED = "post filled";

  private final RecommendationRepository recommendations;
  private final PostRepository posts;
  private final PersonRepository people;
  private final AssignmentRepository assignments;
  private final AllocationRepository allocations;
  private final NoticeRepository notices;
  private final ObjectMapper json;

  public ConfirmationService(
      RecommendationRepository recommendations,
      PostRepository posts,
      PersonRepository people,
      AssignmentRepository assignments,
      AllocationRepository allocations,
      NoticeRepository notices,
      ObjectMapper json) {
    this.recommendations = recommendations;
    this.posts = posts;
    this.people = people;
    this.assignments = assignments;
    this.allocations = allocations;
    this.notices = notices;
    this.json = json;
  }

  @Transactional
  public ConfirmationView confirm(long id, AuthenticatedUser actor) {
    RecommendationRepository.Row row = find(id);
    PostRepository.PostRow post = post(row.postId());
    requireOwningLead(post, actor);

    // The post first, then the person: one lock order for every confirmation, so two never cycle.
    posts.lockById(post.id());
    people.lockByEmployeeNo(row.personNo());
    row = find(id);
    post = post(row.postId());
    requireConfirmable(row);
    PersonRepository.PersonRow person = person(row.personNo());
    if (!person.active()) {
      throw new RecommendationException("G1", "the person is not active");
    }
    if (post.confirmed() >= post.headcount()) {
      throw new RecommendationException(
          POST_FILLED, "the post is filled; there is no remaining slot to confirm");
    }
    for (PersonRepository.AllocationRow allocation : people.allocationsOf(row.personNo())) {
      if (Ranges.intersects(
          allocation.startDate(), allocation.endDate(), post.startDate(), post.endDate())) {
        throw new RecommendationException(
            PERSON_TAKEN, allocationReason(allocation, post));
      }
    }

    long assignmentId = assignments.insert(post.id(), row.personNo());
    long allocationId =
        allocations.insertConfirmation(
            row.personNo(), post.projectId(), post.startDate(), post.endDate(), assignmentId);
    String status = PostStatus.of(post.headcount(), post.confirmed() + 1);
    posts.setStatus(post.id(), status);
    if (recommendations.markConfirmed(id) == 0) {
      throw new RecommendationException(NOT_PROPOSED, "the recommendation is not proposed any more");
    }
    List<VoidedView> voided =
        voidOthers(row.personNo(), id, post.startDate(), post.endDate());
    return new ConfirmationView(
        view(find(id)),
        assignmentId,
        allocationId,
        status,
        Math.max(0, post.headcount() - post.confirmed() - 1),
        voided);
  }

  @Transactional
  public RecommendationView reject(long id, String note, AuthenticatedUser actor) {
    RecommendationRepository.Row row = find(id);
    requireOwningLead(post(row.postId()), actor);
    requireProposed(row, "rejected");
    if (recommendations.markRejected(id, note) == 0) {
      throw new RecommendationException(NOT_PROPOSED, "the recommendation is not proposed any more");
    }
    return view(find(id));
  }

  @Transactional
  public RecommendationView withdraw(long id, AuthenticatedUser actor) {
    RecommendationRepository.Row row = find(id);
    if (!actor.isCoordinator() || !row.proposedBy().equals(actor.username())) {
      throw new AccessDeniedException("only the coordinator who proposed it withdraws it");
    }
    requireProposed(row, "withdrawn");
    if (recommendations.markWithdrawn(id) == 0) {
      throw new RecommendationException(NOT_PROPOSED, "the recommendation is not proposed any more");
    }
    return view(find(id));
  }

  /** The other proposals of the person the written allocation takes, each with its notice. */
  private List<VoidedView> voidOthers(
      String personNo, long confirmedId, LocalDate start, LocalDate end) {
    List<VoidedView> voided = new ArrayList<>();
    for (RecommendationRepository.PendingElsewhereRow pending :
        recommendations.pendingElsewhere(personNo, confirmedId)) {
      if (!Ranges.intersects(pending.startDate(), pending.endDate(), start, end)) {
        continue;
      }
      if (recommendations.voidOne(pending.id(), PERSON_TAKEN) == 0) {
        continue;
      }
      notices.insert(pending.proposedBy(), personNo, pending.postId(), PERSON_TAKEN);
      voided.add(new VoidedView(pending.id(), pending.postId(), personNo, PERSON_TAKEN));
    }
    return List.copyOf(voided);
  }

  /** The G6 wording the exclusion list already prints, so both reads refuse in one voice. */
  private static String allocationReason(
      PersonRepository.AllocationRow allocation, PostRepository.PostRow post) {
    return "the allocation "
        + allocation.startDate()
        + " to "
        + allocation.endDate()
        + " intersects the post's "
        + post.startDate()
        + " to "
        + post.endDate();
  }

  private static void requireOwningLead(PostRepository.PostRow post, AuthenticatedUser actor) {
    if (actor.isCoordinator() || !post.projectLead().equals(actor.username())) {
      throw new AccessDeniedException(
          "only the lead of the post's project confirms or rejects a recommendation");
    }
  }

  /**
   * A proposal voided because the person was taken answers in that voice, not as a generic state
   * clash: the concurrency rule says the second confirmation is refused with the person-taken
   * reason, and the void the first one wrote is exactly that fact.
   */
  private static void requireConfirmable(RecommendationRepository.Row row) {
    if ("voided".equals(row.state()) && PERSON_TAKEN.equals(row.voidReason())) {
      throw new RecommendationException(
          PERSON_TAKEN,
          "the person is taken; another post confirmed them and this proposal was voided");
    }
    requireProposed(row, "confirmed");
  }

  private static void requireProposed(RecommendationRepository.Row row, String verb) {
    if (!"proposed".equals(row.state())) {
      throw new RecommendationException(
          NOT_PROPOSED,
          "the recommendation is " + row.state() + "; only a proposed one can be " + verb);
    }
  }

  private RecommendationRepository.Row find(long id) {
    return recommendations
        .findById(id)
        .orElseThrow(() -> new NoSuchElementException("No recommendation " + id));
  }

  private PostRepository.PostRow post(long id) {
    return posts.findById(id).orElseThrow(() -> new NoSuchElementException("No post " + id));
  }

  private PersonRepository.PersonRow person(String employeeNo) {
    return people
        .findByEmployeeNo(employeeNo)
        .orElseThrow(() -> new NoSuchElementException("No person " + employeeNo));
  }

  private RecommendationView view(RecommendationRepository.Row row) {
    return RecommendationView.of(row, json);
  }
}
