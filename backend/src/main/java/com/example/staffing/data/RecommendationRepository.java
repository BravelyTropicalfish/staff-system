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
 * Recommendations. A proposal is written once as `proposed` with its snapshot; the lifecycle that
 * follows — confirmed, rejected, withdrawn and voided — is a guarded state change, so every write
 * carries `state = 'proposed'` in its WHERE clause and a zero count means somebody got there first.
 */
@Repository
public class RecommendationRepository {

  private static final String SELECT =
      "SELECT r.id, r.post_id, r.person_no, pe.name AS person_name, pe.active AS person_active,"
          + " r.state, r.void_reason, r.snapshot_json, u.username AS proposed_by, r.created_at"
          + " FROM recommendation r JOIN app_user u ON u.id = r.proposed_by"
          + " JOIN person pe ON pe.employee_no = r.person_no";

  private final JdbcTemplate jdbc;

  public RecommendationRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public long insert(long postId, String personNo, String snapshotJson, long proposedBy) {
    KeyHolder keys = new GeneratedKeyHolder();
    jdbc.update(
        connection -> {
          var statement =
              connection.prepareStatement(
                  "INSERT INTO recommendation (post_id, person_no, state, snapshot_json, proposed_by)"
                      + " VALUES (?, ?, 'proposed', ?, ?)",
                  new String[] {"id"});
          statement.setLong(1, postId);
          statement.setString(2, personNo);
          statement.setString(3, snapshotJson);
          statement.setLong(4, proposedBy);
          return statement;
        },
        keys);
    return keys.getKey().longValue();
  }

  public Optional<Row> findById(long id) {
    return jdbc.query(
        SELECT + " WHERE r.id = ?", rs -> rs.next() ? Optional.of(row(rs)) : Optional.empty(), id);
  }

  /**
   * A post's proposals for its screen: the open ones first and then the closed history, each group
   * newest first. Every state stays listed, because a refusal is history the lead is entitled to see.
   */
  public List<Row> findByPost(long postId) {
    return jdbc.query(
        SELECT
            + " WHERE r.post_id = ?"
            + " ORDER BY CASE r.state WHEN 'proposed' THEN 0 ELSE 1 END,"
            + " r.created_at DESC, r.id DESC",
        (rs, row) -> row(rs),
        postId);
  }

  /** Whether the person already holds a pending proposal on this post; a post is never proposed twice. */
  public boolean pendingFor(long postId, String personNo) {
    Integer pending =
        jdbc.queryForObject(
            "SELECT COUNT(*) FROM recommendation"
                + " WHERE post_id = ? AND person_no = ? AND state = 'proposed'",
            Integer.class,
            postId,
            personNo);
    return pending != null && pending > 0;
  }

  public int markConfirmed(long id) {
    return jdbc.update(
        "UPDATE recommendation SET state = 'confirmed' WHERE id = ? AND state = 'proposed'", id);
  }

  public int markRejected(long id, String note) {
    return jdbc.update(
        "UPDATE recommendation SET state = 'rejected', note = ? WHERE id = ? AND state = 'proposed'",
        note,
        id);
  }

  public int markWithdrawn(long id) {
    return jdbc.update(
        "UPDATE recommendation SET state = 'withdrawn' WHERE id = ? AND state = 'proposed'", id);
  }

  /**
   * Voids one pending recommendation; a void always names its reason, and 0 rows means it was no
   * longer pending when the transaction reached it.
   */
  public int voidOne(long id, String reason) {
    return jdbc.update(
        "UPDATE recommendation SET state = 'voided', void_reason = ?"
            + " WHERE id = ? AND state = 'proposed'",
        reason,
        id);
  }

  /**
   * The other pending proposals of one person with the dates of the post each one is for: a
   * confirmation voids the ones whose dates intersect the written allocation and leaves the rest
   * alone.
   */
  public List<PendingElsewhereRow> pendingElsewhere(String personNo, long exceptId) {
    return jdbc.query(
        "SELECT r.id, r.post_id, r.proposed_by, p.start_date, p.end_date"
            + " FROM recommendation r JOIN post p ON p.id = r.post_id"
            + " WHERE r.person_no = ? AND r.state = 'proposed' AND r.id <> ? ORDER BY r.id",
        (rs, row) ->
            new PendingElsewhereRow(
                rs.getLong("id"),
                rs.getLong("post_id"),
                rs.getLong("proposed_by"),
                rs.getDate("start_date").toLocalDate(),
                rs.getDate("end_date").toLocalDate()),
        personNo,
        exceptId);
  }

  private static Row row(java.sql.ResultSet rs) throws java.sql.SQLException {
    return new Row(
        rs.getLong("id"),
        rs.getLong("post_id"),
        rs.getString("person_no"),
        rs.getString("person_name"),
        rs.getBoolean("person_active"),
        rs.getString("state"),
        rs.getString("void_reason"),
        rs.getString("snapshot_json"),
        rs.getString("proposed_by"),
        rs.getTimestamp("created_at").toLocalDateTime());
  }

  public record Row(
      long id,
      long postId,
      String personNo,
      String personName,
      boolean personActive,
      String state,
      String voidReason,
      String snapshotJson,
      String proposedBy,
      LocalDateTime proposedAt) {}

  public record PendingElsewhereRow(
      long id, long postId, long proposedBy, LocalDate startDate, LocalDate endDate) {}
}
