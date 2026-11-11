package com.example.staffing.domain;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.staffing.data.NoticeRepository;
import com.example.staffing.data.PostRepository;
import com.example.staffing.data.ProjectRepository;
import com.example.staffing.data.RoleRepository;
import com.example.staffing.data.SkillRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * The post form's own guards and the derivation that decides whether a save was a change. The route
 * requires the role, the level, the headcount, the dates and the travel, so the null branches answer
 * a caller inside the application; the change derivation decides whether pending proposals are
 * voided, and each gate-bearing field on its own must count as a change.
 */
class PostServiceTest {

  private static final LocalDate START = LocalDate.parse("2027-02-01");
  private static final LocalDate END = LocalDate.parse("2027-06-30");
  private static final AuthenticatedUser LEAD =
      new AuthenticatedUser(2, "lead.one", AuthenticatedUser.LEAD, "Lead one");

  private final PostRepository posts = mock(PostRepository.class);
  private final ProjectRepository projects = mock(ProjectRepository.class);
  private final RoleRepository roles = mock(RoleRepository.class);
  private final SkillRepository skills = mock(SkillRepository.class);
  private final PostService service =
      new PostService(posts, projects, roles, skills, mock(NoticeRepository.class));

  @Test
  void aNewPostNeedsAKnownRole() {
    givenOwningProject();
    when(roles.findByName("")).thenReturn(Optional.empty());

    assertInvalid(() -> service.create(10, draft(null, "Senior", 1, START, END, "none"), LEAD),
        "role  is not in the catalog");
  }

  @Test
  void aNewPostNeedsAJobLevelFromTheFour() {
    givenOwningProject();
    givenRole("DEV", 5);

    assertInvalid(() -> service.create(10, draft("DEV", null, 1, START, END, "none"), LEAD),
        " is not one of the four job level bands");
  }

  @Test
  void aNewPostNeedsATravelWillingnessFromTheThree() {
    givenOwningProject();
    givenRole("DEV", 5);

    assertInvalid(() -> service.create(10, draft("DEV", "Senior", 1, START, END, null), LEAD),
        " is not one of the three travel bands");
  }

  @Test
  void aNewPostNeedsAHeadcount() {
    givenOwningProject();
    givenRole("DEV", 5);

    assertInvalid(
        () -> service.create(10, new PostDraft(null, "DEV", "Senior", null, START, END, "none", null), LEAD),
        "the post needs a headcount");
  }

  @Test
  void aNewPostNeedsBothDates() {
    givenOwningProject();
    givenRole("DEV", 5);

    assertInvalid(() -> service.create(10, draft("DEV", "Senior", 1, null, END, "none"), LEAD),
        "the post needs a start and an end date");
    assertInvalid(() -> service.create(10, draft("DEV", "Senior", 1, START, null, "none"), LEAD),
        "the post needs a start and an end date");
  }

  @Test
  void aTitleChangeVoidsThePendingProposals() {
    givenStoredPost();

    service.update(1, new PostDraft("Backend 2", null, null, null, null, null, null, null), LEAD);

    verify(posts).voidRecommendations(1, "post changed");
  }

  @Test
  void aRoleChangeVoidsThePendingProposals() {
    givenStoredPost();
    givenRole("QA", 6);

    service.update(1, draft("QA", null, null, null, null, null), LEAD);

    verify(posts).voidRecommendations(1, "post changed");
  }

  @Test
  void aLevelChangeVoidsThePendingProposals() {
    givenStoredPost();

    service.update(1, draft(null, "Lead", null, null, null, null), LEAD);

    verify(posts).voidRecommendations(1, "post changed");
  }

  @Test
  void aHeadcountChangeVoidsThePendingProposals() {
    givenStoredPost();

    service.update(1, new PostDraft(null, null, null, BigDecimal.valueOf(3), null, null, null, null), LEAD);

    verify(posts).voidRecommendations(1, "post changed");
  }

  @Test
  void aStartDateChangeVoidsThePendingProposals() {
    givenStoredPost();

    service.update(1, draft(null, null, null, START.plusDays(1), null, null), LEAD);

    verify(posts).voidRecommendations(1, "post changed");
  }

  @Test
  void anEndDateChangeVoidsThePendingProposals() {
    givenStoredPost();

    service.update(1, draft(null, null, null, null, END.plusDays(1), null), LEAD);

    verify(posts).voidRecommendations(1, "post changed");
  }

  @Test
  void aTravelChangeVoidsThePendingProposals() {
    givenStoredPost();

    service.update(1, draft(null, null, null, null, null, "full"), LEAD);

    verify(posts).voidRecommendations(1, "post changed");
  }

  @Test
  void aSkillChangeVoidsThePendingProposals() {
    givenStoredPost();
    when(skills.findByName("测试"))
        .thenReturn(Optional.of(new SkillRepository.SkillRow(7, "测试", 6, "QA", true)));

    service.update(
        1,
        new PostDraft(
            null, null, null, null, null, null, null,
            List.of(new SkillDraft("测试", "required", "Practitioner"))),
        LEAD);

    verify(posts).voidRecommendations(1, "post changed");
  }

  @Test
  void aSaveThatChangesNothingLeavesTheProposalsAlone() {
    givenStoredPost();
    when(skills.findByName("测试"))
        .thenReturn(Optional.of(new SkillRepository.SkillRow(7, "测试", 6, "QA", true)));

    service.update(
        1,
        new PostDraft(
            "Backend", "DEV", "Senior", BigDecimal.valueOf(2), START, END, "none", null),
        LEAD);

    verify(posts, never()).voidRecommendations(1, "post changed");
  }

  private void givenOwningProject() {
    when(projects.findById(10L))
        .thenReturn(
            Optional.of(
                new ProjectRepository.ProjectRow(
                    10, "Atlas", "lead.one", "Lead one", "OPEN", START, END)));
  }

  private void givenRole(String name, long id) {
    when(roles.findByName(name)).thenReturn(Optional.of(new RoleRepository.RoleRow(id, name, true)));
  }

  private void givenStoredPost() {
    when(posts.findById(1L))
        .thenReturn(
            Optional.of(
                new PostRepository.PostRow(
                    1, 10, "Atlas", "OPEN", "lead.one", "Backend", 5, "DEV", "Senior", 2, START,
                    END, "none", "open", 0)));
    when(posts.skillsOf(1L)).thenReturn(List.of());
    when(posts.pendingOf(1L)).thenReturn(List.of());
  }

  private static PostDraft draft(
      String role, String level, Object headcount, LocalDate start, LocalDate end, String travel) {
    BigDecimal count =
        headcount == null
            ? null
            : headcount instanceof BigDecimal number
                ? number
                : BigDecimal.valueOf(((Number) headcount).longValue());
    return new PostDraft(null, role, level, count, start, end, travel, null);
  }

  private static void assertInvalid(Runnable call, String message) {
    assertThatThrownBy(call::run)
        .isInstanceOf(PostException.class)
        .hasMessage(message);
  }
}
