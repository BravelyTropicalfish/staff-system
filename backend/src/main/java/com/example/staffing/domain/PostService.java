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

  /**
   * Every post the caller may read, for the one gap board: the coordinator reads every project's, a
   * lead only the posts of the projects they own. The board shows the project of each row, so the
   * screen does not have to walk the projects one request at a time.
   */
  @Transactional(readOnly = true)
  public List<PostView> listAll(AuthenticatedUser actor) {
    List<PostRepository.PostRow> rows =
        actor.isCoordinator() ? posts.findAll() : posts.findByLead(actor.username());
    return rows.stream().map(this::view).toList();
  }

  /** The gap numbers of every post the caller may read, cancelled posts left out. */
  @Transactional(readOnly = true)
  public List<PostGapView> gapAll(AuthenticatedUser actor) {
    List<PostRepository.PostRow> rows =
        actor.isCoordinator() ? posts.findAll() : posts.findByLead(actor.username());
    return rows.stream()
        .filter(row -> !PostStatus.CANCELLED.equals(row.status()))
        .map(this::gapView)
        .toList();
  }

  /** One post for the screen that edits it: the coordinator reads any, a lead only its own. */
  @Transactional(readOnly = true)
  public PostView get(long postId, AuthenticatedUser actor) {
    PostRepository.PostRow row = find(postId);
    if (!actor.isCoordinator() && !row.projectLead().equals(actor.username())) {
      throw new AccessDeniedException("这个岗位不属于你");
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
      throw new AccessDeniedException("只有项目的项目经理可以新增岗位");
    }
    if (!"OPEN".equals(project.status())) {
      throw new ProjectException("project closed", "项目 " + project.name() + " 已结束");
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
    requireOwningLead(current, actor, "修改");
    if (PostStatus.CANCELLED.equals(current.status())) {
      throw new PostException(POST_CANCELLED, "岗位已取消，不能再修改");
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
          "请先解除 "
              + names(current)
              + "；已有确认人员时，"
              + locked
              + "不能修改");
    }
    if (headcount < current.confirmed()) {
      throw new PostException(
          RELEASE_FIRST,
          "请先解除 "
              + names(current)
              + "；名额不能低于已确认的 "
              + current.confirmed()
              + " 人");
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
    requireOwningLead(current, actor, "取消");
    if (current.confirmed() > 0) {
      throw new PostException(
          RELEASE_FIRST,
          "请先解除 "
              + names(current)
              + "；已有确认人员的岗位不能取消");
    }
    if (posts.cancel(postId) == 0) {
      throw new PostException(POST_CANCELLED, "岗位已经取消");
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
      locked.add("角色");
    }
    if (!level.equals(current.minJobLevel())) {
      locked.add("最低职级");
    }
    if (!required(before).equals(required(after))) {
      locked.add("必备技能行");
    }
    if (!start.equals(current.startDate())) {
      locked.add("开始日期");
    }
    if (!end.equals(current.endDate())) {
      locked.add("结束日期");
    }
    if (!travel.equals(current.travel())) {
      locked.add("差旅要求");
    }
    return String.join("、", locked);
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
    return posts.findById(postId).orElseThrow(() -> new NoSuchElementException("没有岗位 " + postId));
  }

  private ProjectRepository.ProjectRow visibleProject(long projectId, AuthenticatedUser actor) {
    ProjectRepository.ProjectRow project =
        projects
            .findById(projectId)
            .orElseThrow(() -> new NoSuchElementException("没有项目 " + projectId));
    if (!actor.isCoordinator() && !project.lead().equals(actor.username())) {
      throw new AccessDeniedException("这个项目不属于你");
    }
    return project;
  }

  private static void requireOwningLead(
      PostRepository.PostRow post, AuthenticatedUser actor, String action) {
    if (actor.isCoordinator() || !post.projectLead().equals(actor.username())) {
      throw new AccessDeniedException("只有项目的项目经理可以" + action + "本项目岗位");
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
        roles.findByName(clean).orElseThrow(() -> invalid("角色 " + clean + " 不在目录中"));
    if (!role.active()) {
      throw invalid("角色 " + clean + " 已停用");
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
              .orElseThrow(() -> invalid("技能 " + name + " 不在目录中"));
      PostRepository.SkillRow row =
          new PostRepository.SkillRow(
              skill.id(), skill.name(), kind(draft.kind()), band(draft.band()));
      if (rows.put(skill.id(), row) != null) {
        throw invalid("技能 " + name + " 重复出现");
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
    throw invalid("技能类型只能是 required 或 bonus，不是 " + value);
  }

  private static String band(String value) {
    String clean = value == null ? "" : value.trim();
    if (!Bands.PROFICIENCIES.contains(clean)) {
      throw invalid(clean + " 不是五个熟练度档位之一");
    }
    return clean;
  }

  private static String jobLevel(String value) {
    String clean = value == null ? "" : value.trim();
    if (!Bands.JOB_LEVELS.contains(clean)) {
      throw invalid(clean + " 不是四个职级之一");
    }
    return clean;
  }

  private static String travel(String value) {
    String clean = value == null ? "" : value.trim();
    if (!Bands.TRAVEL.contains(clean)) {
      throw invalid(clean + " 不是三个差旅档位之一");
    }
    return clean;
  }

  private static int headcount(BigDecimal value) {
    if (value == null) {
      throw invalid("岗位必须填写名额");
    }
    if (value.stripTrailingZeros().scale() > 0 || value.compareTo(BigDecimal.ONE) < 0) {
      throw invalid("名额必须是至少为 1 的整数");
    }
    try {
      return value.intValueExact();
    } catch (ArithmeticException tooLarge) {
      throw invalid("名额过大");
    }
  }

  private static void requireRange(LocalDate start, LocalDate end) {
    if (start == null || end == null) {
      throw invalid("岗位必须填写起止日期");
    }
    if (end.isBefore(start)) {
      throw invalid("结束日期不能早于开始日期");
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
