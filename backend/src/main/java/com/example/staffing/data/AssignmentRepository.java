package com.example.staffing.data;

import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

/**
 * The confirmed seat. A row exists only while the person is staffed, because a release deletes it
 * together with its allocation, so the table carries no state column and "active" needs no filter.
 */
@Repository
public class AssignmentRepository {

  private final JdbcTemplate jdbc;

  public AssignmentRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public long insert(long postId, String personNo) {
    KeyHolder keys = new GeneratedKeyHolder();
    jdbc.update(
        connection -> {
          var statement =
              connection.prepareStatement(
                  "INSERT INTO assignment (post_id, person_no) VALUES (?, ?)",
                  new String[] {"id"});
          statement.setLong(1, postId);
          statement.setString(2, personNo);
          return statement;
        },
        keys);
    return keys.getKey().longValue();
  }

  /** One seat by id; the caller reads the post for its owner and its dates. */
  public Optional<Row> findById(long id) {
    return jdbc.query(
        "SELECT id, post_id, person_no FROM assignment WHERE id = ?",
        rs ->
            rs.next()
                ? Optional.of(
                    new Row(rs.getLong("id"), rs.getLong("post_id"), rs.getString("person_no")))
                : Optional.empty(),
        id);
  }

  /** Removes the seat; 0 rows means a release got there first. */
  public int delete(long id) {
    return jdbc.update("DELETE FROM assignment WHERE id = ?", id);
  }

  public record Row(long id, long postId, String personNo) {}
}
