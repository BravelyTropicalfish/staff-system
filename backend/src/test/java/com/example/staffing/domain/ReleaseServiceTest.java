package com.example.staffing.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.staffing.data.AllocationRepository;
import com.example.staffing.data.AssignmentRepository;
import com.example.staffing.data.PostRepository;
import java.time.LocalDate;
import java.util.NoSuchElementException;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * The two release branches a live request rarely reaches: a seat another transaction removed
 * between the read and the delete, and a post a person cancelled by hand while the seat was still on
 * it. Both must answer without leaving a fresh status behind.
 */
class ReleaseServiceTest {

  private static final AuthenticatedUser COORDINATOR =
      new AuthenticatedUser(1, "coordinator", AuthenticatedUser.COORDINATOR, "Resource coordinator");

  private final AssignmentRepository assignments = mock(AssignmentRepository.class);
  private final AllocationRepository allocations = mock(AllocationRepository.class);
  private final PostRepository posts = mock(PostRepository.class);
  private final ReleaseService service = new ReleaseService(assignments, allocations, posts);

  @Test
  void aReleaseThatFindsNoSeatAnyMoreSaysSo() {
    when(assignments.findById(9)).thenReturn(Optional.of(new AssignmentRepository.Row(9, 3, "E100")));
    when(posts.findById(3)).thenReturn(Optional.of(post("open", 2, 1)));
    when(assignments.delete(9)).thenReturn(0);

    assertThatThrownBy(() -> service.release(9, COORDINATOR))
        .isInstanceOf(NoSuchElementException.class)
        .hasMessage("No assignment 9");
  }

  @Test
  void aCancelledPostStaysCancelledWhenItsSeatLeaves() {
    when(assignments.findById(9)).thenReturn(Optional.of(new AssignmentRepository.Row(9, 3, "E100")));
    when(posts.findById(3))
        .thenReturn(Optional.of(post("open", 2, 1)), Optional.of(post(PostStatus.CANCELLED, 2, 0)));
    when(assignments.delete(9)).thenReturn(1);

    ReleaseView view = service.release(9, COORDINATOR);

    assertThat(view)
        .extracting(ReleaseView::postStatus, ReleaseView::remaining)
        .containsExactly(PostStatus.CANCELLED, 2);
    verify(posts).setStatus(3, PostStatus.CANCELLED);
  }

  private static PostRepository.PostRow post(String status, int headcount, int confirmed) {
    return new PostRepository.PostRow(
        3,
        10,
        "Atlas",
        "OPEN",
        "lead.one",
        "Backend",
        5,
        "DEV",
        "Senior",
        headcount,
        LocalDate.parse("2027-02-01"),
        LocalDate.parse("2027-06-30"),
        "none",
        status,
        confirmed);
  }
}
