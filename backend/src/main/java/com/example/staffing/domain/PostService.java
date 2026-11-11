package com.example.staffing.domain;

import com.example.staffing.data.NoticeRepository;
import com.example.staffing.data.PostRepository;
import com.example.staffing.data.ProjectRepository;
import com.example.staffing.data.RoleRepository;
import com.example.staffing.data.SkillRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Posts. The project's lead creates, edits and cancels them; the coordinator reads every post and
 * every gap and writes none. Two locks live here. While a post carries a confirmed person the
 * gate-bearing fields — role, minimum job level, required rows, dates and travel — freeze and the
 * headcount may rise but not fall below the confirmed count; both refusals name the people who
 * block them. Any accepted change voids the pending recommendations with `post changed`, each with
 * its notice, because a proposal computed against the old post is no longer a proposal for this one.
 */
@Service
public class PostService {

  static final String POST_CHANGED = "post changed";
  static final String POST_CANCELLED = "post cancelled";
  static final String RELEASE_FIRST = "release first";

  private final PostRepository posts;
  private final ProjectRepository projects;
  private final RoleRepository roles;
  private final SkillRepository skills;
  private final NoticeRepository notices;

  public PostService(
      PostRepository posts,
      ProjectRepository projects,
      RoleRepository roles,
      SkillRepository skills,
      NoticeRepository notices) {
    this.posts = posts;
    this.projects = projects;
    this.roles = roles;
    this.skills = skills;
    this.notices = notices;
  }

  @Transactional(readOnly = true)
  public List<PostView> list(long projectId, AuthenticatedUser actor) {
    visibleProject(projectId, actor);
    return posts.findByProject(projectId).stream().map(this::view).toList();
  }

  /** One post for the screen that edits it: the coordinator reads any, a lead only its own. */
  @Transactional(readOnly = true)
  public PostView get(long postId, AuthenticatedUser actor) {
    PostRepository.PostRow row = find(postId);
    if (!actor.isCoordinator() && !row.projectLead().equals(actor.username())) {
      throw new AccessDeniedException("this post is not yours");
    }
    return view(row);
  }

  @Transactional(readOnly = true)
  public List<PostGapView> gap(long projectId, AuthenticatedUser actor) {
    visibleProject(projectId, actor);
    return posts.findByProject(projectId).stream()
        .filter(row -> !PostStatus.CANCELLED.equals(row.status()))
        .map(this::gapView)
        .toList();
  }

  @Transactional
  public PostView create(long projectId, PostDraft draft, AuthenticatedUser actor) {
    ProjectRepository.ProjectRow project = visibleProject(projectId, actor);
    // visibleProject already answered a lead that does not own the project; only the role is left.
    if (actor.isCoordinator()) {
      throw new AccessDeniedException("only the project's lead creates its posts");
    }
    if (!"OPEN".equals(project.status())) {
      throw new ProjectException("project closed", "the project " + project.name() + " is closed");
    }
    RoleRepository.RoleRow role = activeRole(draft.role());
    String level = jobLevel(draft.minJobLevel());
    int headcount = headcount(draft.headcount());
    requireRange(draft.startDate(), draft.endDate());
    String travel = travel(draft.travel());
    List<PostRepository.SkillRow> skillRows = skillRows(draft.skills());

    long id =
        posts.insert(
            projectId,
            title(draft.title()),
            role.id(),
            level,
            headcount,
            draft.startDate(),
            draft.endDate(),
            travel,
            PostStatus.OPEN);
    posts.replaceSkills(id, skillRows);
    return view(find(id));
  }

  @Transactional
  public PostView update(long postId, PostDraft change, AuthenticatedUser actor) {
    PostRepository.PostRow current = find(postId);
    requireOwningLead(current, actor, "changes");
    if (PostStatus.CANCELLED.equals(current.status())) {
      throw new PostException(POST_CANCELLED, "the post is cancelled; nothing about it can change");
    }

    String title = change.title() == null ? current.title() : title(change.title());
    long roleId = current.roleId();
    boolean roleChanged = false;
    if (change.role() != null && !change.role().trim().equals(current.role())) {
      RoleRepository.RoleRow moved = activeRole(change.role());
      roleId = moved.id();
      roleChanged = true;
    }
    String level =
        change.minJobLevel() == null ? current.minJobLevel() : jobLevel(change.minJobLevel());
    LocalDate start = change.startDate() == null ? current.startDate() : change.startDate();
    LocalDate end = change.endDate() == null ? current.endDate() : change.endDate();
    requireRange(start, end);
    String travel = change.travel() == null ? current.travel() : travel(change.travel());
    int headcount = change.headcount() == null ? current.headcount() : headcount(change.headcount());

    List<PostRepository.SkillRow> before = posts.skillsOf(postId);
    List<PostRepository.SkillRow> after =
        change.skills() == null ? before : skillRows(change.skills());

    String locked = lockedFields(current, roleChanged, level, start, end, travel, before, after);
    if (current.confirmed() > 0 && !locked.isEmpty()) {
      throw new PostException(
          RELEASE_FIRST,
          "release "
              + names(current)
              + " first; "
              + locked
              + " cannot change while a person is confirmed");
    }
    if (headcount < current.confirmed()) {
      throw new PostException(
          RELEASE_FIRST,
          "release "
              + names(current)
              + " first; the headcount cannot fall below the "
              + current.confirmed()
              + " confirmed");
    }

    boolean changed =
        !Objects.equals(title, current.title())
            || roleChanged
            || !level.equals(current.minJobLevel())
            || headcount != current.headcount()
            || !start.equals(current.startDate())
            || !end.equals(current.endDate())
            || !travel.equals(current.travel())
            || !new HashSet<>(before).equals(new HashSet<>(after));

    posts.update(
        postId,
        title,
        roleId,
        level,
        headcount,
        start,
        end,
        travel,
        PostStatus.of(headcount, current.confirmed()));
    if (change.skills() != null) {
      posts.replaceSkills(postId, after);
    }
    if (changed) {
      voidPending(postId, POST_CHANGED);
    }
    return view(find(postId));
  }

  @Transactional
  public PostView cancel(long postId, AuthenticatedUser actor) {
    PostRepository.PostRow current = find(postId);
    requireOwningLead(current, actor, "cancels");
    if (current.confirmed() > 0) {
      throw new PostException(
          RELEASE_FIRST,
          "release "
              + names(current)
              + " first; a post with a confirmed person cannot be cancelled");
    }
    if (posts.cancel(postId) == 0) {
      throw new PostException(POST_CANCELLED, "the post is cancelled already");
    }
    voidPending(postId, POST_CANCELLED);
    return view(find(postId));
  }

  /** The gate-bearing fields that differ from what is stored, or an empty string. */
  private static String lockedFields(
      PostRepository.PostRow current,
      boolean roleChanged,
      String level,
      LocalDate start,
      LocalDate end,
      String travel,
      List<PostRepository.SkillRow> before,
      List<PostRepository.SkillRow> after) {
    List<String> locked = new java.util.ArrayList<>();
    if (roleChanged) {
      locked.add("the role");
    }
    if (!level.equals(current.minJobLevel())) {
      locked.add("the required job level");
    }
    if (!required(before).equals(required(after))) {
      locked.add("a required skill row");
    }
    if (!start.equals(current.startDate())) {
      locked.add("the start date");
    }
    if (!end.equals(current.endDate())) {
      locked.add("the end date");
    }
    if (!travel.equals(current.travel())) {
      locked.add("the travel requirement");
    }
    return String.join(", ", locked);
  }

  /** The rows that gate eligibility; a bonus row is informational and never part of the lock. */
  private static Set<PostRepository.SkillRow> required(List<PostRepository.SkillRow> rows) {
    return rows.stream()
        .filter(row -> "REQUIRED".equals(row.kind()))
        .collect(Collectors.toSet());
  }

  private void voidPending(long postId, String reason) {
    for (PostRepository.PendingRow pending : posts.pendingOf(postId)) {
      notices.insert(pending.proposedBy(), pending.personNo(), postId, reason);
    }
    posts.voidRecommendations(postId, reason);
  }

  private PostRepository.PostRow find(long postId) {
    return posts.findById(postId).orElseThrow(() -> new NoSuchElementException("No post " + postId));
  }

  private ProjectRepository.ProjectRow visibleProject(long projectId, AuthenticatedUser actor) {
    ProjectRepository.ProjectRow project =
        projects
            .findById(projectId)
            .orElseThrow(() -> new NoSuchElementException("No project " + projectId));
    if (!actor.isCoordinator() && !project.lead().equals(actor.username())) {
      throw new AccessDeniedException("this project is not yours");
    }
    return project;
  }

  private static void requireOwningLead(
      PostRepository.PostRow post, AuthenticatedUser actor, String verb) {
    if (actor.isCoordinator() || !post.projectLead().equals(actor.username())) {
      throw new AccessDeniedException("only the project's lead " + verb + " its posts");
    }
  }

  private String names(PostRepository.PostRow post) {
    return posts.confirmedPeople(post.id()).stream()
        .map(PostRepository.ConfirmedRow::personNo)
        .collect(Collectors.joining(", "));
  }

  private RoleRepository.RoleRow activeRole(String roleName) {
    String clean = roleName == null ? "" : roleName.trim();
    RoleRepository.RoleRow role =
        roles.findByName(clean).orElseThrow(() -> invalid("role " + clean + " is not in the catalog"));
    if (!role.active()) {
      throw invalid("role " + clean + " is deactivated");
    }
    return role;
  }

  private List<PostRepository.SkillRow> skillRows(List<SkillDraft> drafts) {
    if (drafts == null) {
      return List.of();
    }
    Map<Long, PostRepository.SkillRow> rows = new LinkedHashMap<>();
    for (SkillDraft draft : drafts) {
      String name = draft.skill() == null ? "" : draft.skill().trim();
      SkillRepository.SkillRow skill =
          skills
              .findByName(name)
              .orElseThrow(() -> invalid("skill " + name + " is not in the catalog"));
      PostRepository.SkillRow row =
          new PostRepository.SkillRow(
              skill.id(), skill.name(), kind(draft.kind()), band(draft.band()));
      if (rows.put(skill.id(), row) != null) {
        throw invalid("skill " + name + " appears twice");
      }
    }
    return List.copyOf(rows.values());
  }

  private static String kind(String value) {
    String clean = value == null ? "" : value.trim();
    if ("required".equalsIgnoreCase(clean)) {
      return "REQUIRED";
    }
    if ("bonus".equalsIgnoreCase(clean)) {
      return "BONUS";
    }
    throw invalid("a skill row is required or bonus, not " + value);
  }

  private static String band(String value) {
    String clean = value == null ? "" : value.trim();
    if (!Bands.PROFICIENCIES.contains(clean)) {
      throw invalid(clean + " is not one of the five proficiency bands");
    }
    return clean;
  }

  private static String jobLevel(String value) {
    String clean = value == null ? "" : value.trim();
    if (!Bands.JOB_LEVELS.contains(clean)) {
      throw invalid(clean + " is not one of the four job level bands");
    }
    return clean;
  }

  private static String travel(String value) {
    String clean = value == null ? "" : value.trim();
    if (!Bands.TRAVEL.contains(clean)) {
      throw invalid(clean + " is not one of the three travel bands");
    }
    return clean;
  }

  private static int headcount(BigDecimal value) {
    if (value == null) {
      throw invalid("the post needs a headcount");
    }
    if (value.stripTrailingZeros().scale() > 0 || value.compareTo(BigDecimal.ONE) < 0) {
      throw invalid("headcount must be a whole number of at least 1");
    }
    try {
      return value.intValueExact();
    } catch (ArithmeticException tooLarge) {
      throw invalid("headcount is too large");
    }
  }

  private static void requireRange(LocalDate start, LocalDate end) {
    if (start == null || end == null) {
      throw invalid("the post needs a start and an end date");
    }
    if (end.isBefore(start)) {
      throw invalid("the end date cannot precede the start date");
    }
  }

  private static String title(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }

  private static PostException invalid(String message) {
    return new PostException("invalid post", message);
  }

  private PostView view(PostRepository.PostRow row) {
    String status =
        PostStatus.CANCELLED.equals(row.status())
            ? PostStatus.CANCELLED
            : PostStatus.of(row.headcount(), row.confirmed());
    int remaining =
        PostStatus.CANCELLED.equals(status) ? 0 : Math.max(0, row.headcount() - row.confirmed());
    List<PostSkillView> skills =
        posts.skillsOf(row.id()).stream().map(PostService::skill).toList();
    return new PostView(
        row.id(),
        row.projectId(),
        row.project(),
        row.title(),
        row.role(),
        row.minJobLevel(),
        row.headcount(),
        row.startDate().toString(),
        row.endDate().toString(),
        row.travel(),
        status,
        row.confirmed(),
        remaining,
        skills);
  }

  private PostGapView gapView(PostRepository.PostRow row) {
    int remaining = Math.max(0, row.headcount() - row.confirmed());
    List<GapPersonView> people =
        posts.confirmedPeople(row.id()).stream()
            .map(
                person ->
                    new GapPersonView(
                        person.assignmentId(),
                        person.personNo(),
                        person.name(),
                        person.confirmedAt().toString(),
                        person.active()))
            .toList();
    return new PostGapView(
        row.id(),
        row.title(),
        row.role(),
        PostStatus.of(row.headcount(), row.confirmed()),
        row.headcount(),
        row.confirmed(),
        remaining,
        remaining > 0,
        people);
  }

  private static PostSkillView skill(PostRepository.SkillRow row) {
    boolean informational = !"REQUIRED".equals(row.kind());
    return new PostSkillView(
        row.name(), informational ? "bonus" : "required", row.band(), informational);
  }
}
