package com.example.staffing.domain;

import static org.assertj.core.api.Assertions.assertThat;
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
 * rather than storing a nameless or dateless project. T37 tightens the ownership guard: only a lead
 * opens a project, and only their own.
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
        () -> service.create(null, "lead.one", START, END, LEAD_ONE), "项目必须填写名称");
    assertRefused(
        () -> service.create("   ", "lead.one", START, END, LEAD_ONE),
        "项目必须填写名称");
  }

  @Test
  void aProjectNeedsBothDates() {
    assertRefused(
        () -> service.create("Atlas", "lead.one", null, END, LEAD_ONE),
        "项目必须填写起止日期");
    assertRefused(
        () -> service.create("Atlas", "lead.one", START, null, LEAD_ONE),
        "项目必须填写起止日期");
  }

  @Test
  void anAbsentLeadNameIsRefusedAsTheEmptyAccount() {
    when(users.findByUsername("")).thenReturn(Optional.empty());

    assertRefused(
        () -> service.create("Atlas", null, START, END, LEAD_ONE), "账号 null 不存在");
  }

  @Test
  void aLeadOpensAProjectOfTheirOwnOnly() {
    assertThatThrownBy(() -> service.create("Atlas", "lead.two", START, END, LEAD_ONE))
        .isInstanceOf(AccessDeniedException.class)
        .hasMessage("项目经理只能为自己新建项目");
  }

  @Test
  void theCoordinatorDoesNotOpenAProject() {
    assertThatThrownBy(() -> service.create("Atlas", "lead.one", START, END, COORDINATOR))
        .isInstanceOf(AccessDeniedException.class)
        .hasMessage("只有项目经理可以新建项目");
  }

  @Test
  void aLeadOpensAProjectOfTheirOwn() {
    when(projects.insert("Atlas", 2L, START, END)).thenReturn(5L);
    when(projects.findById(5L))
        .thenReturn(
            Optional.of(
                new ProjectRepository.ProjectRow(
                    5L, "Atlas", "lead.one", "Project lead one", "OPEN", START, END)));

    ProjectView view = service.create("Atlas", "lead.one", START, END, LEAD_ONE);

    assertThat(view.id()).isEqualTo(5L);
    assertThat(view.name()).isEqualTo("Atlas");
    assertThat(view.lead()).isEqualTo("lead.one");
    assertThat(view.status()).isEqualTo("OPEN");
  }

  @Test
  void aLeadNameThatIsNotALeadAccountIsRefused() {
    when(users.findByUsername("coordinator"))
        .thenReturn(
            Optional.of(
                new AppUserRepository.UserRow(
                    1L, "coordinator", "hash", AuthenticatedUser.COORDINATOR, "Resource coordinator")));

    assertRefused(
        () -> service.create("Atlas", "coordinator", START, END, LEAD_ONE),
        "账号 coordinator 不是项目经理账号");
  }

  private static void assertRefused(Runnable call, String message) {
    assertThatThrownBy(call::run)
        .isInstanceOf(ProjectException.class)
        .as(call.toString())
        .hasMessage(message);
  }
}
