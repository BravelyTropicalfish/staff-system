package com.example.staffing.domain;

import com.example.staffing.data.AppUserRepository;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/** Checks a username and a password against the seeded hashes; it issues no token, the session does. */
@Service
public class AuthService {

  /**
   * A real hash that matches nothing, so an unknown username costs the same BCrypt round as a wrong
   * password and the response time does not reveal which accounts exist.
   */
  private static final String NEVER_MATCHING_HASH =
      "$2a$10$4RT8SigSVGBz4QwxE/O.Ne7I8e6HoWxoR0uxsdKNIV/qD.roIMO3K";

  private final AppUserRepository users;
  private final PasswordEncoder encoder;

  public AuthService(AppUserRepository users, PasswordEncoder encoder) {
    this.users = users;
    this.encoder = encoder;
  }

  public AuthenticatedUser authenticate(String username, String password) {
    var row = users.findByUsername(username == null ? "" : username);
    String hash = row.map(AppUserRepository.UserRow::passwordHash).orElse(NEVER_MATCHING_HASH);
    boolean matches = encoder.matches(password == null ? "" : password, hash);

    if (row.isEmpty() || !matches) {
      throw new BadCredentialsException("the username or the password is wrong");
    }

    var user = row.get();
    return new AuthenticatedUser(user.id(), user.username(), user.role(), user.displayName());
  }
}
