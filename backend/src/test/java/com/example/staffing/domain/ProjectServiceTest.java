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

/**
 * The project form's own guards. The route rejects a blank name or a missing date before the service
 * runs, so these branches answer only a caller inside the application — the service still refuses
 * rather than storing a nameless or dateless project.
 */
class ProjectServiceTest {

  private static final LocalDate START = LocalDate.parse("2027-01-01");
  private static final LocalDate END = LocalDate.parse("2027-12-31");

  private final ProjectRepository projects = mock(ProjectRepository.class);
  private final AppUserRepository users = mock(AppUserRepository.class);
  private final ProjectService service =
      new ProjectService(projects, users, mock(NoticeRepository.class));

  @Test
  void aProjectNeedsAName() {
    assertRefused(() -> service.create(null, "lead.one", START, END), "the project needs a name");
    assertRefused(() -> service.create("   ", "lead.one", START, END), "the project needs a name");
  }

  @Test
  void aProjectNeedsBothDates() {
    assertRefused(
        () -> service.create("Atlas", "lead.one", null, END),
        "the project needs a start and an end date");
    assertRefused(
        () -> service.create("Atlas", "lead.one", START, null),
        "the project needs a start and an end date");
  }

  @Test
  void anAbsentLeadNameIsRefusedAsTheEmptyAccount() {
    when(users.findByUsername("")).thenReturn(Optional.empty());

    assertRefused(
        () -> service.create("Atlas", null, START, END), "lead null is not an account");
  }

  private static void assertRefused(Runnable call, String message) {
    assertThatThrownBy(call::run)
        .isInstanceOf(ProjectException.class)
        .as(call.toString())
        .hasMessage(message);
  }
}
