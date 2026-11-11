package com.example.staffing.data;

import java.sql.PreparedStatement;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

/** Reads and writes the role catalog. No rule lives here beyond the constraints of the schema. */
@Repository
public class RoleRepository {

  private final JdbcTemplate jdbc;

  public RoleRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public List<RoleRow> findAll() {
    return jdbc.query(
        "SELECT id, name, active FROM role ORDER BY name",
        (rs, row) -> new RoleRow(rs.getLong("id"), rs.getString("name"), rs.getBoolean("active")));
  }

  public Optional<RoleRow> findById(long id) {
    return jdbc.query(
        "SELECT id, name, active FROM role WHERE id = ?",
        rs ->
            rs.next()
                ? Optional.of(
                    new RoleRow(rs.getLong("id"), rs.getString("name"), rs.getBoolean("active")))
                : Optional.empty(),
        id);
  }

  public Optional<RoleRow> findByName(String name) {
    return jdbc.query(
        "SELECT id, name, active FROM role WHERE name = ?",
        rs ->
            rs.next()
                ? Optional.of(
                    new RoleRow(rs.getLong("id"), rs.getString("name"), rs.getBoolean("active")))
                : Optional.empty(),
        name);
  }

  public boolean existsByName(String name) {
    Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM role WHERE name = ?", Integer.class, name);
    return count != null && count > 0;
  }

  public long insert(String name) {
    KeyHolder keys = new GeneratedKeyHolder();
    jdbc.update(
        connection -> {
          PreparedStatement statement =
              connection.prepareStatement(
                  "INSERT INTO role (name, active) VALUES (?, TRUE)", new String[] {"id"});
          statement.setString(1, name);
          return statement;
        },
        keys);
    return keys.getKey().longValue();
  }

  public void rename(long id, String name) {
    jdbc.update("UPDATE role SET name = ? WHERE id = ?", name, id);
  }

  public void setActive(long id, boolean active) {
    jdbc.update("UPDATE role SET active = ? WHERE id = ?", active, id);
  }

  /** People and posts that already point at the role; a rename is refused while either exists. */
  public int countReferences(long id) {
    Integer people =
        jdbc.queryForObject("SELECT COUNT(*) FROM person WHERE role_id = ?", Integer.class, id);
    Integer posts =
        jdbc.queryForObject("SELECT COUNT(*) FROM post WHERE role_id = ?", Integer.class, id);
    return (people == null ? 0 : people) + (posts == null ? 0 : posts);
  }

  public record RoleRow(long id, String name, boolean active) {}
}
