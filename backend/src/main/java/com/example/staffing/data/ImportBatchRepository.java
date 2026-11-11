package com.example.staffing.data;

import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

/**
 * The batch history: one row per upload, one row per refused spreadsheet row. Batches are only ever
 * inserted, never updated, because the history the coordinator reads must stay as it was written.
 */
@Repository
public class ImportBatchRepository {

  private static final String SELECT =
      "SELECT b.id, b.filename, b.status, b.uploaded_at, b.person_rows, b.skill_rows, b.error_count,"
          + " u.display_name AS uploader"
          + " FROM import_batch b JOIN app_user u ON u.id = b.uploaded_by";

  private final JdbcTemplate jdbc;

  public ImportBatchRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public long insert(
      String filename,
      String status,
      long uploadedBy,
      int personRows,
      int skillRows,
      int errorCount) {
    KeyHolder keys = new GeneratedKeyHolder();
    jdbc.update(
        connection -> {
          PreparedStatement statement =
              connection.prepareStatement(
                  "INSERT INTO import_batch"
                      + " (filename, status, uploaded_by, person_rows, skill_rows, error_count)"
                      + " VALUES (?, ?, ?, ?, ?, ?)",
                  new String[] {"id"});
          statement.setString(1, filename);
          statement.setString(2, status);
          statement.setLong(3, uploadedBy);
          statement.setInt(4, personRows);
          statement.setInt(5, skillRows);
          statement.setInt(6, errorCount);
          return statement;
        },
        keys);
    return keys.getKey().longValue();
  }

  public void insertErrors(long batchId, List<ErrorRow> errors) {
    for (ErrorRow error : errors) {
      jdbc.update(
          "INSERT INTO import_error (batch_id, sheet, row_no, key_value, message) VALUES (?, ?, ?, ?, ?)",
          batchId,
          error.sheet(),
          error.rowNo(),
          error.key(),
          error.message());
    }
  }

  public List<BatchRow> findAll() {
    return jdbc.query(
        SELECT + " ORDER BY b.uploaded_at DESC, b.id DESC", (rs, row) -> batch(rs));
  }

  public Optional<BatchRow> findById(long id) {
    return jdbc.query(
        SELECT + " WHERE b.id = ?",
        rs -> rs.next() ? Optional.of(batch(rs)) : Optional.empty(),
        id);
  }

  public List<ErrorRow> findErrors(long batchId) {
    return jdbc.query(
        "SELECT sheet, row_no, key_value, message FROM import_error WHERE batch_id = ? ORDER BY id",
        (rs, row) ->
            new ErrorRow(
                rs.getString("sheet"),
                rs.getInt("row_no"),
                rs.getString("key_value"),
                rs.getString("message")),
        batchId);
  }

  private BatchRow batch(java.sql.ResultSet rs) throws java.sql.SQLException {
    Timestamp uploadedAt = rs.getTimestamp("uploaded_at");
    Instant instant = uploadedAt == null ? null : uploadedAt.toInstant();
    return new BatchRow(
        rs.getLong("id"),
        rs.getString("filename"),
        rs.getString("status"),
        rs.getString("uploader"),
        instant,
        rs.getInt("person_rows"),
        rs.getInt("skill_rows"),
        rs.getInt("error_count"));
  }

  public record BatchRow(
      long id,
      String filename,
      String status,
      String uploader,
      Instant uploadedAt,
      int personRows,
      int skillRows,
      int errorCount) {}

  public record ErrorRow(String sheet, int rowNo, String key, String message) {}
}
