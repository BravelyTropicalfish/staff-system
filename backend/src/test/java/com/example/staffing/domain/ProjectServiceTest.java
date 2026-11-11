package com.example.staffing.domain;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.staffing.data.AppUserRepository;
import com.example.staffing.data.NoticeRepository;
import com.example.staffing.data.ProjectRepository;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

/**
 * The project form's own guards. The route rejects a blank name or a missing date before the service
 * runs, so these branches answer only a caller inside the application — the service still refuses
 * rather than storing a nameless or dateless project. T33 adds the ownership guard: a lead opens a
 * project of their own, so naming another lead is the coordinator's alone.
 */
class ProjectServiceTest {

  private static final LocalDate START = LocalDate.parse("2027-01-01");
  private static final LocalDate END = LocalDate.parse("2027-12-31");

  private static final AuthenticatedUser COORDINATOR =
      new AuthenticatedUser(1L, "coordinator", AuthenticatedUser.COORDINATOR, "Resource coordinator");
  private static final AuthenticatedUser LEAD_ONE =
      new AuthenticatedUser(2L, "lead.one", AuthenticatedUser.LEAD, "Project lead one");

  private final ProjectRepository projects = mock(ProjectRepository.class);
  private final AppUserRepository users = mock(AppUserRepository.class);
  private final ProjectService service =
      new ProjectService(projects, users, mock(NoticeRepository.class));

  ProjectServiceTest() {
    when(users.findByUsername("lead.one"))
        .thenReturn(
            Optional.of(
                new AppUserRepository.UserRow(
                    2L, "lead.one", "hash", AuthenticatedUser.LEAD, "Project lead one")));
    when(users.findByUsername("lead.two"))
        .thenReturn(
            Optional.of(
                new AppUserRepository.UserRow(
                    3L, "lead.two", "hash", AuthenticatedUser.LEAD, "Project lead two")));
  }

  @Test
  void aProjectNeedsAName() {
    assertRefused(
        () -> service.create(null, "lead.one", START, END, COORDINATOR), "the project needs a name");
    assertRefused(
        () -> service.create("   ", "lead.one", START, END, COORDINATOR),
        "the project needs a name");
  }

  @Test
  void aProjectNeedsBothDates() {
    assertRefused(
        () -> service.create("Atlas", "lead.one", null, END, COORDINATOR),
        "the project needs a start and an end date");
    assertRefused(
        () -> service.create("Atlas", "lead.one", START, null, COORDINATOR),
        "the project needs a start and an end date");
  }

  @Test
  void anAbsentLeadNameIsRefusedAsTheEmptyAccount() {
    when(users.findByUsername("")).thenReturn(Optional.empty());

    assertRefused(
        () -> service.create("Atlas", null, START, END, COORDINATOR), "lead null is not an account");
  }

  @Test
  void aLeadOpensAProjectOfTheirOwnOnly() {
    assertThatThrownBy(() -> service.create("Atlas", "lead.two", START, END, LEAD_ONE))
        .isInstanceOf(AccessDeniedException.class)
        .hasMessage("a lead creates only a project of their own");
  }

  private static void assertRefused(Runnable call, String message) {
    assertThatThrownBy(call::run)
        .isInstanceOf(ProjectException.class)
        .as(call.toString())
        .hasMessage(message);
  }
}
