package com.example.staffing.data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * The coordinator's notice list. A note is written in the same transaction as the void that
 * caused it, so a lost recommendation always has its entry. Reading the list skips what was
 * dismissed and nothing else: an entry never expires on its own, so a list with a stale one is the
 * point rather than a bug.
 */
@Repository
public class NoticeRepository {

  private static final String SELECT_NOTICE =
      "SELECT n.id, n.coordinator_id, n.post_id, p.title AS post_title, n.person_no,"
          + " pe.name AS person_name, n.reason, n.created_at, n.dismissed_at"
          + " FROM notice n JOIN post p ON p.id = n.post_id"
          + " JOIN person pe ON pe.employee_no = n.person_no";

  private final JdbcTemplate jdbc;

  public NoticeRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public void insert(long coordinatorId, String personNo, long postId, String reason) {
    jdbc.update(
        "INSERT INTO notice (coordinator_id, person_no, post_id, reason) VALUES (?, ?, ?, ?)",
        coordinatorId,
        personNo,
        postId,
        reason);
  }

  /** Every entry the coordinator has not dismissed, newest first. */
  public List<Row> listOpen(long coordinatorId) {
    return jdbc.query(
        SELECT_NOTICE
            + " WHERE n.coordinator_id = ? AND n.dismissed_at IS NULL"
            + " ORDER BY n.created_at DESC, n.id DESC",
        (rs, row) -> row(rs),
        coordinatorId);
  }

  public Optional<Row> findById(long id) {
    return jdbc.query(
        SELECT_NOTICE + " WHERE n.id = ?", rs -> rs.next() ? Optional.of(row(rs)) : Optional.empty(), id);
  }

  /** Marks one entry dismissed; 0 rows means it was dismissed already or is not this caller's. */
  public int dismiss(long id, long coordinatorId) {
    return jdbc.update(
        "UPDATE notice SET dismissed_at = CURRENT_TIMESTAMP"
            + " WHERE id = ? AND coordinator_id = ? AND dismissed_at IS NULL",
        id,
        coordinatorId);
  }

  private static Row row(java.sql.ResultSet rs) throws java.sql.SQLException {
    java.sql.Timestamp dismissed = rs.getTimestamp("dismissed_at");
    return new Row(
        rs.getLong("id"),
        rs.getLong("coordinator_id"),
        rs.getLong("post_id"),
        rs.getString("post_title"),
        rs.getString("person_no"),
        rs.getString("person_name"),
        rs.getString("reason"),
        rs.getTimestamp("created_at").toLocalDateTime(),
        dismissed == null ? null : dismissed.toLocalDateTime());
  }

  public record Row(
      long id,
      long coordinatorId,
      long postId,
      String postTitle,
      String personNo,
      String personName,
      String reason,
      LocalDateTime createdAt,
      LocalDateTime dismissedAt) {}
}
