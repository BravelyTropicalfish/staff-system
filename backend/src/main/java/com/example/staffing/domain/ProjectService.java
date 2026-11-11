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
 * Projects. Only the coordinator creates one, a lead only ever sees its own, and ending is the one
 * terminal act: it closes the project, cancels every post, voids the pending recommendations with
 * `post cancelled` (each with its notice), deletes every allocation and releases every assignment —
 * all in one transaction, so a failure leaves the project untouched.
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
        projects.findById(id).orElseThrow(() -> new NoSuchElementException("No project " + id));
    if (!actor.isCoordinator() && !project.lead().equals(actor.username())) {
      throw new AccessDeniedException("this project is not yours");
    }
    return view(project);
  }

  @Transactional
  public ProjectView create(String name, String leadUsername, LocalDate start, LocalDate end) {
    if (name == null || name.isBlank()) {
      throw invalid("the project needs a name");
    }
    if (start == null || end == null) {
      throw invalid("the project needs a start and an end date");
    }
    if (end.isBefore(start)) {
      throw invalid("the end date cannot precede the start date");
    }
    AppUserRepository.UserRow lead = leadOf(leadUsername);
    long id = projects.insert(name.trim(), lead.id(), start, end);
    return view(projects.findById(id).orElseThrow());
  }

  /** The project's lead ends it exactly once; nothing about it can change afterwards. */
  @Transactional
  public ProjectView end(long id, AuthenticatedUser actor) {
    ProjectRepository.ProjectRow project =
        projects.findById(id).orElseThrow(() -> new NoSuchElementException("No project " + id));
    if (!project.lead().equals(actor.username())) {
      throw new AccessDeniedException("only the project's lead ends it");
    }
    if (projects.close(id) == 0) {
      throw new ProjectException("project closed", "The project is closed already.");
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
            .orElseThrow(() -> invalid("lead " + leadUsername + " is not an account"));
    if (!AuthenticatedUser.LEAD.equals(lead.role())) {
      throw invalid("lead " + leadUsername + " is not a lead account");
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
