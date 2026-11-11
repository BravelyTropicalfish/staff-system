package com.example.staffing.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.staffing.data.AllocationRepository;
import com.example.staffing.data.AssignmentRepository;
import com.example.staffing.data.NoticeRepository;
import com.example.staffing.data.PersonRepository;
import com.example.staffing.data.PostRepository;
import com.example.staffing.data.RecommendationRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * The decisions a live request reaches only through a race: the state moved between the read and
 * the write, so the guarded update touched no row and the caller is told the proposal is no longer
 * proposed. The proposal another confirmation voided first is skipped rather than reported as
 * unexplained.
 */
class ConfirmationServiceTest {

  private static final LocalDate START = LocalDate.parse("2027-02-01");
  private static final LocalDate END = LocalDate.parse("2027-06-30");
  private static final AuthenticatedUser LEAD =
      new AuthenticatedUser(2, "lead.one", AuthenticatedUser.LEAD, "Lead one");
  private static final AuthenticatedUser COORDINATOR =
      new AuthenticatedUser(1, "coordinator", AuthenticatedUser.COORDINATOR, "Resource coordinator");

  private final RecommendationRepository recommendations = mock(RecommendationRepository.class);
  private final PostRepository posts = mock(PostRepository.class);
  private final PersonRepository people = mock(PersonRepository.class);
  private final AssignmentRepository assignments = mock(AssignmentRepository.class);
  private final AllocationRepository allocations = mock(AllocationRepository.class);
  private final NoticeRepository notices = mock(NoticeRepository.class);
  private final ConfirmationService service =
      new ConfirmationService(
          recommendations, posts, people, assignments, allocations, notices, new ObjectMapper());

  @Test
  void aConfirmationThatFindsTheStateMovedSaysItIsNotProposed() {
    givenConfirmableProposal();
    when(recommendations.markConfirmed(7)).thenReturn(0);

    assertThatThrownBy(() -> service.confirm(7, LEAD))
        .isInstanceOf(RecommendationException.class)
        .hasMessage("这条推荐已不是待决策状态");
  }

  @Test
  void aRejectionThatFindsTheStateMovedSaysItIsNotProposed() {
    when(recommendations.findById(7)).thenReturn(Optional.of(row("proposed", null)));
    when(posts.findById(3)).thenReturn(Optional.of(post()));
    when(recommendations.markRejected(7, "band too thin")).thenReturn(0);

    assertThatThrownBy(() -> service.reject(7, "band too thin", LEAD))
        .isInstanceOf(RecommendationException.class)
        .hasMessage("这条推荐已不是待决策状态");
  }

  @Test
  void aWithdrawalThatFindsTheStateMovedSaysItIsNotProposed() {
    when(recommendations.findById(7)).thenReturn(Optional.of(row("proposed", null)));
    when(recommendations.markWithdrawn(7)).thenReturn(0);

    assertThatThrownBy(() -> service.withdraw(7, COORDINATOR))
        .isInstanceOf(RecommendationException.class)
        .hasMessage("这条推荐已不是待决策状态");
  }

  @Test
  void aProposalAnotherConfirmationVoidedFirstIsSkipped() {
    givenConfirmableProposal();
    when(recommendations.findById(7))
        .thenReturn(
            Optional.of(row("proposed", null)),
            Optional.of(row("proposed", null)),
            Optional.of(row("confirmed", null)));
    when(recommendations.markConfirmed(7)).thenReturn(1);
    when(recommendations.pendingElsewhere("E100", 7))
        .thenReturn(
            List.of(new RecommendationRepository.PendingElsewhereRow(8, 4, 1, START, END)));
    when(recommendations.voidOne(8, "person taken")).thenReturn(0);

    ConfirmationView view = service.confirm(7, LEAD);

    assertThat(view.voided()).isEmpty();
    verify(notices, never()).insert(anyLong(), anyString(), anyLong(), anyString());
  }

  @Test
  void aProposalVoidedAsPersonTakenAnswersInThatVoice() {
    when(recommendations.findById(7)).thenReturn(Optional.of(row("voided", "person taken")));
    when(posts.findById(3)).thenReturn(Optional.of(post()));

    assertThatThrownBy(() -> service.confirm(7, LEAD))
        .isInstanceOf(RecommendationException.class)
        .hasMessage("人员已被占用：另一岗位已确认该人员，这条推荐已作废");
  }

  @Test
  void aProposalVoidedForAChangedPostIsOnlyNoLongerProposed() {
    when(recommendations.findById(7)).thenReturn(Optional.of(row("voided", "post changed")));
    when(posts.findById(3)).thenReturn(Optional.of(post()));

    assertThatThrownBy(() -> service.confirm(7, LEAD))
        .isInstanceOf(RecommendationException.class)
        .hasMessage("只有待决策的推荐可以确认：当前状态是 voided");
  }

  private void givenConfirmableProposal() {
    when(recommendations.findById(7)).thenReturn(Optional.of(row("proposed", null)));
    when(posts.findById(3)).thenReturn(Optional.of(post()));
    when(people.findByEmployeeNo("E100"))
        .thenReturn(
            Optional.of(
                new PersonRepository.PersonRow(
                    "E100", "E100", null, 5, "DEV", "Senior", "none", true)));
    when(people.allocationsOf("E100")).thenReturn(List.of());
    when(assignments.insert(3, "E100")).thenReturn(11L);
    when(allocations.insertConfirmation("E100", 10, START, END, 11L)).thenReturn(12L);
  }

  private static RecommendationRepository.Row row(String state, String voidReason) {
    return new RecommendationRepository.Row(
        7,
        3,
        "E100",
        "E100",
        true,
        state,
        voidReason,
        "{}",
        "coordinator",
        LocalDateTime.parse("2027-01-01T09:00"));
  }

  private static PostRepository.PostRow post() {
    return new PostRepository.PostRow(
        3, 10, "Atlas", "OPEN", "lead.one", null, 5, "DEV", "Senior", 1, START, END, "none", "open",
        0);
  }
}
