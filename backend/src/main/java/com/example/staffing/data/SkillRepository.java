package com.example.staffing.data;

import java.sql.PreparedStatement;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

/** Reads and writes the skill catalog. The domain is one role reference, never a list. */
@Repository
public class SkillRepository {

  private static final String SELECT =
      "SELECT s.id, s.name, s.role_domain_id, r.name AS domain_name, s.active"
          + " FROM skill s JOIN role r ON r.id = s.role_domain_id";

  private final JdbcTemplate jdbc;

  public SkillRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public List<SkillRow> findAll() {
    return jdbc.query(SELECT + " ORDER BY s.name", (rs, row) -> map(rs));
  }

  public Optional<SkillRow> findById(long id) {
    return jdbc.query(
        SELECT + " WHERE s.id = ?",
        rs -> rs.next() ? Optional.of(map(rs)) : Optional.empty(),
        id);
  }

  public Optional<SkillRow> findByName(String name) {
    return jdbc.query(
        SELECT + " WHERE s.name = ?",
        rs -> rs.next() ? Optional.of(map(rs)) : Optional.empty(),
        name);
  }

  public boolean existsByName(String name) {
    Integer count =
        jdbc.queryForObject("SELECT COUNT(*) FROM skill WHERE name = ?", Integer.class, name);
    return count != null && count > 0;
  }

  /**
   * Every skill whose domain is this role, the set the cross-role fallback admits on. An empty list
   * means the role has no domain skills, and the fallback must admit nobody rather than widen.
   */
  public List<SkillRow> findByDomain(long roleDomainId) {
    return jdbc.query(
        SELECT + " WHERE s.role_domain_id = ? ORDER BY s.name",
        (rs, row) -> map(rs),
        roleDomainId);
  }

  public long insert(String name, long roleDomainId) {
    KeyHolder keys = new GeneratedKeyHolder();
    jdbc.update(
        connection -> {
          PreparedStatement statement =
              connection.prepareStatement(
                  "INSERT INTO skill (name, role_domain_id, active) VALUES (?, ?, TRUE)",
                  new String[] {"id"});
          statement.setString(1, name);
          statement.setLong(2, roleDomainId);
          return statement;
        },
        keys);
    return keys.getKey().longValue();
  }

  public void rename(long id, String name) {
    jdbc.update("UPDATE skill SET name = ? WHERE id = ?", name, id);
  }

  public void moveToDomain(long id, long roleDomainId) {
    jdbc.update("UPDATE skill SET role_domain_id = ? WHERE id = ?", roleDomainId, id);
  }

  public void setActive(long id, boolean active) {
    jdbc.update("UPDATE skill SET active = ? WHERE id = ?", active, id);
  }

  /** Only the service calls this, and only after it counted the references. */
  public void delete(long id) {
    jdbc.update("DELETE FROM skill WHERE id = ?", id);
  }

  /** People and posts that already carry the skill; a rename is refused while either exists. */
  public int countReferences(long id) {
    Integer people =
        jdbc.queryForObject(
            "SELECT COUNT(*) FROM person_skill WHERE skill_id = ?", Integer.class, id);
    Integer posts =
        jdbc.queryForObject("SELECT COUNT(*) FROM post_skill WHERE skill_id = ?", Integer.class, id);
    return (people == null ? 0 : people) + (posts == null ? 0 : posts);
  }

  private SkillRow map(java.sql.ResultSet rs) throws java.sql.SQLException {
    return new SkillRow(
        rs.getLong("id"),
        rs.getString("name"),
        rs.getLong("role_domain_id"),
        rs.getString("domain_name"),
        rs.getBoolean("active"));
  }

  public record SkillRow(
      long id, String name, long roleDomainId, String domainName, boolean active) {}
}
