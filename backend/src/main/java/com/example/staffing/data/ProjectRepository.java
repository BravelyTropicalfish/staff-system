package com.example.staffing.data;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

/**
 * Projects and the four statements the terminal end needs. Ending is one transaction in
 * {@code ProjectService}: this class only executes the parts, in the order the foreign keys allow
 * (allocations before assignments).
 */
@Repository
public class ProjectRepository {

  private static final String SELECT_PROJECT =
      "SELECT p.id, p.name, u.username AS lead, u.display_name AS lead_name, p.status,"
          + " p.start_date, p.end_date FROM project p JOIN app_user u ON u.id = p.lead_user_id";

  private final JdbcTemplate jdbc;

  public ProjectRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public long insert(String name, long leadUserId, LocalDate start, LocalDate end) {
    KeyHolder keys = new GeneratedKeyHolder();
    jdbc.update(
        connection -> {
          var statement =
              connection.prepareStatement(
                  "INSERT INTO project (name, lead_user_id, start_date, end_date)"
                      + " VALUES (?, ?, ?, ?)",
                  new String[] {"id"});
          statement.setString(1, name);
          statement.setLong(2, leadUserId);
          statement.setDate(3, java.sql.Date.valueOf(start));
          statement.setDate(4, java.sql.Date.valueOf(end));
          return statement;
        },
        keys);
    return keys.getKey().longValue();
  }

  public List<ProjectRow> findAll() {
    return jdbc.query(
        SELECT_PROJECT + " ORDER BY p.name, p.id", (rs, row) -> project(rs));
  }

  public List<ProjectRow> findByLead(long leadUserId) {
    return jdbc.query(
        SELECT_PROJECT + " WHERE p.lead_user_id = ? ORDER BY p.name, p.id",
        (rs, row) -> project(rs),
        leadUserId);
  }

  public Optional<ProjectRow> findById(long id) {
    return jdbc.query(
        SELECT_PROJECT + " WHERE p.id = ?",
        rs -> rs.next() ? Optional.of(project(rs)) : Optional.empty(),
        id);
  }

  /** Closes an open project; 0 rows means it was already closed. */
  public int close(long id) {
    return jdbc.update("UPDATE project SET status = 'CLOSED' WHERE id = ? AND status = 'OPEN'", id);
  }

  /** The pending recommendations of every post of the project, before they are voided. */
  public List<PendingRow> pendingRecommendations(long projectId) {
    return jdbc.query(
        "SELECT r.id, r.post_id, r.person_no, r.proposed_by FROM recommendation r"
            + " JOIN post p ON p.id = r.post_id"
            + " WHERE p.project_id = ? AND r.state = 'proposed' ORDER BY r.id",
        (rs, row) ->
            new PendingRow(
                rs.getLong("id"),
                rs.getLong("post_id"),
                rs.getString("person_no"),
                rs.getLong("proposed_by")),
        projectId);
  }

  /** Voids one pending recommendation with the reason the end carries. */
  public void voidRecommendation(long recommendationId, String reason) {
    jdbc.update(
        "UPDATE recommendation SET state = 'voided', void_reason = ? WHERE id = ? AND state = 'proposed'",
        reason,
        recommendationId);
  }

  public int cancelPosts(long projectId) {
    return jdbc.update("UPDATE post SET status = 'cancelled' WHERE project_id = ?", projectId);
  }

  public int deleteAllocations(long projectId) {
    return jdbc.update("DELETE FROM allocation WHERE project_id = ?", projectId);
  }

  /** An assignment is the confirmed seat; releasing deletes it (the table has no state). */
  public int deleteAssignments(long projectId) {
    return jdbc.update(
        "DELETE FROM assignment WHERE post_id IN (SELECT id FROM post WHERE project_id = ?)",
        projectId);
  }

  private ProjectRow project(java.sql.ResultSet rs) throws java.sql.SQLException {
    return new ProjectRow(
        rs.getLong("id"),
        rs.getString("name"),
        rs.getString("lead"),
        rs.getString("lead_name"),
        rs.getString("status"),
        rs.getDate("start_date").toLocalDate(),
        rs.getDate("end_date").toLocalDate());
  }

  public record ProjectRow(
      long id,
      String name,
      String lead,
      String leadName,
      String status,
      LocalDate startDate,
      LocalDate endDate) {}

  public record PendingRow(long id, long postId, String personNo, long proposedBy) {}
}
