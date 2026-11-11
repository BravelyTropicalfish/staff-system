package com.example.staffing.domain;

import com.example.staffing.data.AllocationRepository;
import com.example.staffing.data.AssignmentRepository;
import com.example.staffing.data.PostRepository;
import java.util.NoSuchElementException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The seat taken back. A release deletes the assignment and the allocation it wrote and recomputes
 * the post status from the remaining count, which is all the person's calendar needs: G6 is
 * answered from the allocations that remain. The coordinator may release anywhere; a lead only on
 * their own project's posts. A second release finds no seat, so it changes nothing.
 */
@Service
public class ReleaseService {

  private final AssignmentRepository assignments;
  private final AllocationRepository allocations;
  private final PostRepository posts;

  public ReleaseService(
      AssignmentRepository assignments, AllocationRepository allocations, PostRepository posts) {
    this.assignments = assignments;
    this.allocations = allocations;
    this.posts = posts;
  }

  @Transactional
  public ReleaseView release(long assignmentId, AuthenticatedUser actor) {
    AssignmentRepository.Row seat = find(assignmentId);
    PostRepository.PostRow post = findPost(seat.postId());
    if (!actor.isCoordinator() && !post.projectLead().equals(actor.username())) {
      throw new AccessDeniedException("the coordinator or the post's own lead releases a person");
    }

    allocations.deleteByAssignment(assignmentId);
    if (assignments.delete(assignmentId) == 0) {
      throw new NoSuchElementException("No assignment " + assignmentId);
    }

    PostRepository.PostRow after = findPost(post.id());
    String status =
        PostStatus.CANCELLED.equals(after.status())
            ? PostStatus.CANCELLED
            : PostStatus.of(after.headcount(), after.confirmed());
    posts.setStatus(after.id(), status);
    return new ReleaseView(
        after.id(), seat.personNo(), status, Math.max(0, after.headcount() - after.confirmed()));
  }

  private AssignmentRepository.Row find(long assignmentId) {
    return assignments
        .findById(assignmentId)
        .orElseThrow(() -> new NoSuchElementException("No assignment " + assignmentId));
  }

  private PostRepository.PostRow findPost(long postId) {
    return posts.findById(postId).orElseThrow(() -> new NoSuchElementException("No post " + postId));
  }
}
