package com.example.staffing.domain;

import com.example.staffing.data.AppUserRepository;
import com.example.staffing.data.NoticeRepository;
import com.example.staffing.data.ProjectRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Projects. A lead creates a project of their own and the coordinator creates one for any lead; a
 * lead only ever sees its own, and ending is the one terminal act: it closes the project, cancels
 * every post, voids the pending recommendations with `post cancelled` (each with its notice),
 * deletes every allocation and releases every assignment — all in one transaction, so a failure
 * leaves the project untouched.
 */
@Service
public class ProjectService {

  static final String POST_CANCELLED = "post cancelled";

  private final ProjectRepository projects;
  private final AppUserRepository users;
  private final NoticeRepository notices;

  public ProjectService(
      ProjectRepository projects, AppUserRepository users, NoticeRepository notices) {
    this.projects = projects;
    this.users = users;
    this.notices = notices;
  }

  /** The coordinator sees every project; a lead sees only the ones they own. */
  @Transactional(readOnly = true)
  public List<ProjectView> list(AuthenticatedUser actor) {
    List<ProjectRepository.ProjectRow> rows =
        actor.isCoordinator() ? projects.findAll() : projects.findByLead(actor.id());
    return rows.stream().map(ProjectService::view).toList();
  }

  /** One project for its own screen: the coordinator reads any, a lead only its own. */
  @Transactional(readOnly = true)
  public ProjectView get(long id, AuthenticatedUser actor) {
    ProjectRepository.ProjectRow project =
        projects.findById(id).orElseThrow(() -> new NoSuchElementException("没有项目 " + id));
    if (!actor.isCoordinator() && !project.lead().equals(actor.username())) {
      throw new AccessDeniedException("这个项目不属于你");
    }
    return view(project);
  }

  /**
   * A project is opened by its own lead and by nobody else: the coordinator reads every project and
   * opens none, and a lead may not name another lead. The refusal is the server's answer rather
   * than only the screen's.
   */
  @Transactional
  public ProjectView create(
      String name, String leadUsername, LocalDate start, LocalDate end, AuthenticatedUser actor) {
    if (actor.isCoordinator()) {
      throw new AccessDeniedException("只有项目经理可以新建项目");
    }
    AppUserRepository.UserRow lead = leadOf(leadUsername);
    if (!lead.username().equals(actor.username())) {
      throw new AccessDeniedException("项目经理只能为自己新建项目");
    }
    if (name == null || name.isBlank()) {
      throw invalid("项目必须填写名称");
    }
    if (start == null || end == null) {
      throw invalid("项目必须填写起止日期");
    }
    if (end.isBefore(start)) {
      throw invalid("结束日期不能早于开始日期");
    }
    long id = projects.insert(name.trim(), lead.id(), start, end);
    return view(projects.findById(id).orElseThrow());
  }

  /** The project's lead ends it exactly once; nothing about it can change afterwards. */
  @Transactional
  public ProjectView end(long id, AuthenticatedUser actor) {
    ProjectRepository.ProjectRow project =
        projects.findById(id).orElseThrow(() -> new NoSuchElementException("没有项目 " + id));
    if (!project.lead().equals(actor.username())) {
      throw new AccessDeniedException("只有本项目的项目经理可以结束项目");
    }
    if (projects.close(id) == 0) {
      throw new ProjectException("project closed", "项目已经结束。");
    }
    for (ProjectRepository.PendingRow pending : projects.pendingRecommendations(id)) {
      projects.voidRecommendation(pending.id(), POST_CANCELLED);
      notices.insert(pending.proposedBy(), pending.personNo(), pending.postId(), POST_CANCELLED);
    }
    projects.deleteAllocations(id);
    projects.deleteAssignments(id);
    projects.cancelPosts(id);
    return view(projects.findById(id).orElseThrow());
  }

  private AppUserRepository.UserRow leadOf(String leadUsername) {
    AppUserRepository.UserRow lead =
        users
            .findByUsername(leadUsername == null ? "" : leadUsername.trim())
            .orElseThrow(() -> invalid("账号 " + leadUsername + " 不存在"));
    if (!AuthenticatedUser.LEAD.equals(lead.role())) {
      throw invalid("账号 " + leadUsername + " 不是项目经理账号");
    }
    return lead;
  }

  private static ProjectException invalid(String message) {
    return new ProjectException("invalid project", message);
  }

  private static ProjectView view(ProjectRepository.ProjectRow row) {
    return new ProjectView(
        row.id(),
        row.name(),
        row.lead(),
        row.leadName(),
        row.status(),
        row.startDate().toString(),
        row.endDate().toString());
  }
}
