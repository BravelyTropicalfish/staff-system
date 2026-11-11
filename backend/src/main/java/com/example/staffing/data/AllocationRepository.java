package com.example.staffing.data;

import java.time.LocalDate;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

/**
 * The allocation records. The hand-entered ones are written here directly; a confirmation writes its
 * own through {@link #insertConfirmation}, and a release or a project end deletes them from
 * {@link ProjectRepository}.
 */
@Repository
public class AllocationRepository {

  private static final String SELECT_ALLOCATION =
      "SELECT a.id, a.person_no, pr.name AS project, a.start_date, a.end_date, a.source"
          + " FROM allocation a JOIN project pr ON pr.id = a.project_id";

  private final JdbcTemplate jdbc;

  public AllocationRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public long insertManual(String personNo, long projectId, LocalDate start, LocalDate end) {
    KeyHolder keys = new GeneratedKeyHolder();
    jdbc.update(
        connection -> {
          var statement =
              connection.prepareStatement(
                  "INSERT INTO allocation (person_no, project_id, start_date, end_date, source)"
                      + " VALUES (?, ?, ?, ?, 'MANUAL')",
                  new String[] {"id"});
          statement.setString(1, personNo);
          statement.setLong(2, projectId);
          statement.setDate(3, java.sql.Date.valueOf(start));
          statement.setDate(4, java.sql.Date.valueOf(end));
          return statement;
        },
        keys);
    return keys.getKey().longValue();
  }

  /** The range a confirmation writes: the post's inclusive dates, backed by its assignment. */
  public long insertConfirmation(
      String personNo, long projectId, LocalDate start, LocalDate end, long assignmentId) {
    KeyHolder keys = new GeneratedKeyHolder();
    jdbc.update(
        connection -> {
          var statement =
              connection.prepareStatement(
                  "INSERT INTO allocation (person_no, project_id, start_date, end_date, source,"
                      + " assignment_id) VALUES (?, ?, ?, ?, 'CONFIRMATION', ?)",
                  new String[] {"id"});
          statement.setString(1, personNo);
          statement.setLong(2, projectId);
          statement.setDate(3, java.sql.Date.valueOf(start));
          statement.setDate(4, java.sql.Date.valueOf(end));
          statement.setLong(5, assignmentId);
          return statement;
        },
        keys);
    return keys.getKey().longValue();
  }

  public Optional<AllocationRow> findById(long id) {
    return jdbc.query(
        SELECT_ALLOCATION + " WHERE a.id = ?",
        rs -> rs.next() ? Optional.of(row(rs)) : Optional.empty(),
        id);
  }

  public void updateDates(long id, LocalDate start, LocalDate end) {
    jdbc.update(
        "UPDATE allocation SET start_date = ?, end_date = ? WHERE id = ?",
        java.sql.Date.valueOf(start),
        java.sql.Date.valueOf(end),
        id);
  }

  public void delete(long id) {
    jdbc.update("DELETE FROM allocation WHERE id = ?", id);
  }

  /** Removes the allocation a release takes with its seat; 0 rows means a hand record or none. */
  public int deleteByAssignment(long assignmentId) {
    return jdbc.update("DELETE FROM allocation WHERE assignment_id = ?", assignmentId);
  }

  private AllocationRow row(java.sql.ResultSet rs) throws java.sql.SQLException {
    return new AllocationRow(
        rs.getLong("id"),
        rs.getString("person_no"),
        rs.getString("project"),
        rs.getDate("start_date").toLocalDate(),
        rs.getDate("end_date").toLocalDate(),
        rs.getString("source"));
  }

  public record AllocationRow(
      long id,
      String personNo,
      String project,
      LocalDate startDate,
      LocalDate endDate,
      String source) {}
}
