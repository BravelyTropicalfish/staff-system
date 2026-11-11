package com.example.staffing.data;

import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Reads the seeded accounts. Accounts are never created in the app: only the seed writes here. */
@Repository
public class AppUserRepository {

  private final JdbcTemplate jdbc;

  public AppUserRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public Optional<UserRow> findByUsername(String username) {
    return jdbc.query(
        "SELECT id, username, password_hash, role, display_name FROM app_user WHERE username = ?",
        rs ->
            rs.next()
                ? Optional.of(
                    new UserRow(
                        rs.getLong("id"),
                        rs.getString("username"),
                        rs.getString("password_hash"),
                        rs.getString("role"),
                        rs.getString("display_name")))
                : Optional.empty(),
        username);
  }

  public record UserRow(
      long id, String username, String passwordHash, String role, String displayName) {}
}
