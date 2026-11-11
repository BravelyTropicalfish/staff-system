package com.example.staffing.data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

/**
 * Posts and their skill rows. The status column is written from the same derivation the read side
 * uses, so a stored row never says something the confirmed people contradict; the confirmed count
 * comes from the assignments, which is the truth that derivation reads.
 */
@Repository
public class PostRepository {

  private static final String SELECT_POST =
      "SELECT p.id, p.project_id, pr.name AS project, pr.status AS project_status,"
          + " u.username AS lead, p.title, p.role_id, r.name AS role, p.min_job_level, p.headcount,"
          + " p.start_date, p.end_date, p.travel_required, p.status,"
          + " (SELECT COUNT(*) FROM assignment a WHERE a.post_id = p.id) AS confirmed"
          + " FROM post p"
          + " JOIN project pr ON pr.id = p.project_id"
          + " JOIN app_user u ON u.id = pr.lead_user_id"
          + " JOIN role r ON r.id = p.role_id";

  private final JdbcTemplate jdbc;

  public PostRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public long insert(
      long projectId,
      String title,
      long roleId,
      String minJobLevel,
      int headcount,
      LocalDate start,
      LocalDate end,
      String travel,
      String status) {
    KeyHolder keys = new GeneratedKeyHolder();
    jdbc.update(
        connection -> {
          var statement =
              connection.prepareStatement(
                  "INSERT INTO post (project_id, title, role_id, min_job_level, headcount,"
                      + " start_date, end_date, travel_required, status)"
                      + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                  new String[] {"id"});
          statement.setLong(1, projectId);
          statement.setString(2, title);
          statement.setLong(3, roleId);
          statement.setString(4, minJobLevel);
          statement.setInt(5, headcount);
          statement.setDate(6, java.sql.Date.valueOf(start));
          statement.setDate(7, java.sql.Date.valueOf(end));
          statement.setString(8, travel);
          statement.setString(9, status);
          return statement;
        },
        keys);
    return keys.getKey().longValue();
  }

  public void update(
      long id,
      String title,
      long roleId,
      String minJobLevel,
      int headcount,
      LocalDate start,
      LocalDate end,
      String travel,
      String status) {
    jdbc.update(
        "UPDATE post SET title = ?, role_id = ?, min_job_level = ?, headcount = ?, start_date = ?,"
            + " end_date = ?, travel_required = ?, status = ? WHERE id = ?",
        title,
        roleId,
        minJobLevel,
        headcount,
        java.sql.Date.valueOf(start),
        java.sql.Date.valueOf(end),
        travel,
        status,
        id);
  }

  public Optional<PostRow> findById(long id) {
    return jdbc.query(
        SELECT_POST + " WHERE p.id = ?", rs -> rs.next() ? Optional.of(row(rs)) : Optional.empty(), id);
  }

  public List<PostRow> findByProject(long projectId) {
    return jdbc.query(
        SELECT_POST + " WHERE p.project_id = ? ORDER BY p.id", (rs, row) -> row(rs), projectId);
  }

  /** Every post of every project, for the gap board the coordinator reads. */
  public List<PostRow> findAll() {
    return jdbc.query(
        SELECT_POST + " ORDER BY p.project_id, p.id", (rs, row) -> row(rs));
  }

  /** Every post of the projects one lead owns, for that lead's own gap board. */
  public List<PostRow> findByLead(String leadUsername) {
    return jdbc.query(
        SELECT_POST + " WHERE u.username = ? ORDER BY p.project_id, p.id",
        (rs, row) -> row(rs),
        leadUsername);
  }

  /**
   * Takes the post's row lock for the rest of the transaction, so two confirmations on one post
   * queue before either counts the seats. The caller has already resolved the post.
   */
  public void lockById(long id) {
    jdbc.queryForList("SELECT id FROM post WHERE id = ? FOR UPDATE", Long.class, id);
  }

  /** Writes the status the confirmed count derives; a confirmation is the caller that moves it. */
  public void setStatus(long id, String status) {
    jdbc.update("UPDATE post SET status = ? WHERE id = ?", status, id);
  }

  /** Cancels a post that is not cancelled yet; 0 rows means it was cancelled already. */
  public int cancel(long id) {
    return jdbc.update(
        "UPDATE post SET status = 'cancelled' WHERE id = ? AND status <> 'cancelled'", id);
  }

  /** The rows are replaced wholesale: the request is the whole skill requirement. */
  public void replaceSkills(long postId, List<SkillRow> skills) {
    jdbc.update("DELETE FROM post_skill WHERE post_id = ?", postId);
    for (SkillRow skill : skills) {
      jdbc.update(
          "INSERT INTO post_skill (post_id, skill_id, kind, min_band, target_band)"
              + " VALUES (?, ?, ?, ?, ?)",
          postId,
          skill.skillId(),
          skill.kind(),
          "REQUIRED".equals(skill.kind()) ? skill.band() : null,
          "BONUS".equals(skill.kind()) ? skill.band() : null);
    }
  }

  /** Required rows first, each group in name order; the band is the one the kind carries. */
  public List<SkillRow> skillsOf(long postId) {
    return jdbc.query(
        "SELECT ps.skill_id, s.name, ps.kind, COALESCE(ps.min_band, ps.target_band) AS band"
            + " FROM post_skill ps JOIN skill s ON s.id = ps.skill_id"
            + " WHERE ps.post_id = ?"
            + " ORDER BY CASE ps.kind WHEN 'REQUIRED' THEN 0 ELSE 1 END, s.name",
        (rs, row) ->
            new SkillRow(
                rs.getLong("skill_id"),
                rs.getString("name"),
                rs.getString("kind"),
                rs.getString("band")),
        postId);
  }

  /** The seats a post carries; an inactive person still holds one and is flagged, not dropped. */
  public List<ConfirmedRow> confirmedPeople(long postId) {
    return jdbc.query(
        "SELECT a.id, a.person_no, p.name, p.active, a.created_at FROM assignment a"
            + " JOIN person p ON p.employee_no = a.person_no"
            + " WHERE a.post_id = ? ORDER BY a.created_at, a.id",
        (rs, row) ->
            new ConfirmedRow(
                rs.getLong("id"),
                rs.getString("person_no"),
                rs.getString("name"),
                rs.getBoolean("active"),
                rs.getTimestamp("created_at").toLocalDateTime()),
        postId);
  }

  /** The pending recommendations a change has to void, with the coordinator each notice goes to. */
  public List<PendingRow> pendingOf(long postId) {
    return jdbc.query(
        "SELECT id, person_no, proposed_by FROM recommendation"
            + " WHERE post_id = ? AND state = 'proposed' ORDER BY id",
        (rs, row) ->
            new PendingRow(
                rs.getLong("id"), rs.getString("person_no"), rs.getLong("proposed_by")),
        postId);
  }

  public void voidRecommendations(long postId, String reason) {
    jdbc.update(
        "UPDATE recommendation SET state = 'voided', void_reason = ?"
            + " WHERE post_id = ? AND state = 'proposed'",
        reason,
        postId);
  }

  private PostRow row(java.sql.ResultSet rs) throws java.sql.SQLException {
    return new PostRow(
        rs.getLong("id"),
        rs.getLong("project_id"),
        rs.getString("project"),
        rs.getString("project_status"),
        rs.getString("lead"),
        rs.getString("title"),
        rs.getLong("role_id"),
        rs.getString("role"),
        rs.getString("min_job_level"),
        rs.getInt("headcount"),
        rs.getDate("start_date").toLocalDate(),
        rs.getDate("end_date").toLocalDate(),
        rs.getString("travel_required"),
        rs.getString("status"),
        rs.getInt("confirmed"));
  }

  public record PostRow(
      long id,
      long projectId,
      String project,
      String projectStatus,
      String projectLead,
      String title,
      long roleId,
      String role,
      String minJobLevel,
      int headcount,
      LocalDate startDate,
      LocalDate endDate,
      String travel,
      String status,
      int confirmed) {}

  public record SkillRow(long skillId, String name, String kind, String band) {}

  public record ConfirmedRow(
      long assignmentId,
      String personNo,
      String name,
      boolean active,
      LocalDateTime confirmedAt) {}

  public record PendingRow(long id, String personNo, long proposedBy) {}
}
