package com.example.staffing.data;

import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * The headcount report's two reads. Nothing is stored and nothing is written: the report is derived
 * from rows the pool already holds, so a later change to the pool is visible on the next read.
 */
@Repository
public class HeadcountRepository {

  private final JdbcTemplate jdbc;

  public HeadcountRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  /**
   * The role axis: every role the catalog still offers, plus every role that at least one active
   * person still holds, in the catalog's own order. A deactivated role that keeps nobody is absent,
   * because the catalog no longer offers it.
   */
  public List<RoleCount> roleCounts() {
    return jdbc.query(
        "SELECT r.id, r.name, COUNT(p.employee_no) AS people"
            + " FROM role r LEFT JOIN person p ON p.role_id = r.id AND p.active = TRUE"
            + " WHERE r.active = TRUE OR p.employee_no IS NOT NULL"
            + " GROUP BY r.id, r.name"
            + " ORDER BY r.id",
        (rs, row) -> new RoleCount(rs.getLong("id"), rs.getString("name"), rs.getInt("people")));
  }

  /**
   * The grade counts as stored — only the bands that hold somebody. The service pads the rest of the
   * scale with zeros, because the band order belongs to {@code Bands}, not to the rows.
   */
  public List<GradeCount> gradeCounts() {
    return jdbc.query(
        "SELECT job_level, COUNT(*) AS people FROM person WHERE active = TRUE GROUP BY job_level",
        (rs, row) -> new GradeCount(rs.getString("job_level"), rs.getInt("people")));
  }

  public record RoleCount(long roleId, String role, int count) {}

  public record GradeCount(String grade, int count) {}
}
