package com.example.staffing.data;

import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Person master data. The import is the only writer in this change, so the write side is two
 * methods: overwrite the cells a file carries, and insert a band or overwrite the one there. The
 * read side serves the profile: the person with their one role, their bands and their occupancy.
 */
@Repository
public class PersonRepository {

  private static final String SELECT_PERSON =
      "SELECT p.employee_no, p.name, p.email, p.role_id, p.job_level, p.travel, p.active,"
          + " r.name AS role"
          + " FROM person p JOIN role r ON r.id = p.role_id";

  private final JdbcTemplate jdbc;

  public PersonRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public Set<String> employeeNumbers() {
    return new LinkedHashSet<>(
        jdbc.queryForList("SELECT employee_no FROM person", String.class));
  }

  public void upsert(
      String employeeNo,
      String name,
      String email,
      long roleId,
      String jobLevel,
      String travel,
      boolean active) {
    int updated =
        jdbc.update(
            "UPDATE person SET name = ?, email = ?, role_id = ?, job_level = ?, travel = ?, active = ?"
                + " WHERE employee_no = ?",
            name,
            email,
            roleId,
            jobLevel,
            travel,
            active,
            employeeNo);
    if (updated == 0) {
      jdbc.update(
          "INSERT INTO person (employee_no, name, email, role_id, job_level, travel, active)"
              + " VALUES (?, ?, ?, ?, ?, ?, ?)",
          employeeNo,
          name,
          email,
          roleId,
          jobLevel,
          travel,
          active);
    }
  }

  public void upsertBand(String employeeNo, long skillId, String band) {
    int updated =
        jdbc.update(
            "UPDATE person_skill SET band = ? WHERE employee_no = ? AND skill_id = ?",
            band,
            employeeNo,
            skillId);
    if (updated == 0) {
      jdbc.update(
          "INSERT INTO person_skill (employee_no, skill_id, band) VALUES (?, ?, ?)",
          employeeNo,
          skillId,
          band);
    }
  }

  public List<PersonRow> findAll() {
    return jdbc.query(SELECT_PERSON + " ORDER BY p.employee_no", (rs, row) -> person(rs));
  }

  public Optional<PersonRow> findByEmployeeNo(String employeeNo) {
    return jdbc.query(
        SELECT_PERSON + " WHERE p.employee_no = ?",
        rs -> rs.next() ? Optional.of(person(rs)) : Optional.empty(),
        employeeNo);
  }

  /**
   * Takes the person's row lock for the rest of the transaction, so two confirmations of one person
   * queue instead of both reading "not booked yet". False means no such person.
   */
  public boolean lockByEmployeeNo(String employeeNo) {
    return !jdbc
        .queryForList(
            "SELECT employee_no FROM person WHERE employee_no = ? FOR UPDATE",
            String.class,
            employeeNo)
        .isEmpty();
  }

  /** Every band the person holds, with the one role whose domain the skill belongs to. */
  public List<SkillBandRow> skillBandsOf(String employeeNo) {
    return jdbc.query(
        "SELECT s.name AS skill, d.name AS domain, ps.band FROM person_skill ps"
            + " JOIN skill s ON s.id = ps.skill_id"
            + " JOIN role d ON d.id = s.role_domain_id"
            + " WHERE ps.employee_no = ?",
        (rs, row) ->
            new SkillBandRow(rs.getString("skill"), rs.getString("domain"), rs.getString("band")),
        employeeNo);
  }

  /** Every allocation, current or past, newest first; ended is judged by the caller. */
  public List<AllocationRow> allocationsOf(String employeeNo) {
    return jdbc.query(
        "SELECT a.id, pr.name AS project, a.start_date, a.end_date, a.source FROM allocation a"
            + " JOIN project pr ON pr.id = a.project_id"
            + " WHERE a.person_no = ? ORDER BY a.start_date DESC, a.id DESC",
        (rs, row) ->
            new AllocationRow(
                rs.getLong("id"),
                rs.getString("project"),
                rs.getDate("start_date").toLocalDate(),
                rs.getDate("end_date").toLocalDate(),
                rs.getString("source")),
        employeeNo);
  }

  /** Every band every person holds, one row per person and skill: the pool a search reads. */
  public List<PersonBandRow> allBands() {
    return jdbc.query(
        "SELECT employee_no, skill_id, band FROM person_skill",
        (rs, row) ->
            new PersonBandRow(rs.getString("employee_no"), rs.getLong("skill_id"), rs.getString("band")));
  }

  /** Every allocation anyone carries, current or past; G6 compares it with the post's dates. */
  public List<PersonRangeRow> allAllocations() {
    return jdbc.query(
        "SELECT person_no, start_date, end_date FROM allocation",
        (rs, row) ->
            new PersonRangeRow(
                rs.getString("person_no"),
                rs.getDate("start_date").toLocalDate(),
                rs.getDate("end_date").toLocalDate()));
  }

  private PersonRow person(java.sql.ResultSet rs) throws java.sql.SQLException {
    return new PersonRow(
        rs.getString("employee_no"),
        rs.getString("name"),
        rs.getString("email"),
        rs.getLong("role_id"),
        rs.getString("role"),
        rs.getString("job_level"),
        rs.getString("travel"),
        rs.getBoolean("active"));
  }

  public record PersonRow(
      String employeeNo,
      String name,
      String email,
      long roleId,
      String role,
      String jobLevel,
      String travel,
      boolean active) {}

  /** One band a person holds, keyed by skill id so a rename never moves it. */
  public record PersonBandRow(String employeeNo, long skillId, String band) {}

  /** One allocation anyone carries; the gates read the dates, not the project. */
  public record PersonRangeRow(String employeeNo, LocalDate startDate, LocalDate endDate) {}

  public record SkillBandRow(String skill, String domain, String band) {}

  public record AllocationRow(
      long id, String project, LocalDate startDate, LocalDate endDate, String source) {}
}