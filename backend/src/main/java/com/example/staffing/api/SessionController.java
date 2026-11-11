package com.example.staffing.api;

import com.example.staffing.domain.AuthService;
import com.example.staffing.domain.AuthenticatedUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Sign in, read the current account, sign out. The session cookie carries the identity. */
@RestController
@RequestMapping("/api/session")
public class SessionController {

  private final AuthService auth;
  private final SecurityContextRepository repository;

  public SessionController(AuthService auth, SecurityContextRepository repository) {
    this.auth = auth;
    this.repository = repository;
  }

  @PostMapping
  SessionResponse signIn(
      @RequestBody SignIn body, HttpServletRequest request, HttpServletResponse response) {
    AuthenticatedUser user = auth.authenticate(body.username(), body.password());

    var authentication =
        new UsernamePasswordAuthenticationToken(
            user, null, List.of(new SimpleGrantedAuthority("ROLE_" + user.role())));
    SecurityContext context = SecurityContextHolder.createEmptyContext();
    context.setAuthentication(authentication);
    SecurityContextHolder.setContext(context);
    repository.saveContext(context, request, response);

    return SessionResponse.of(user);
  }

  @GetMapping
  SessionResponse current(Authentication authentication) {
    return SessionResponse.of((AuthenticatedUser) authentication.getPrincipal());
  }

  @DeleteMapping
  @ResponseStatus(HttpStatus.NO_CONTENT)
  void signOut(HttpServletRequest request) {
    HttpSession session = request.getSession(false);
    if (session != null) {
      session.invalidate();
    }
    SecurityContextHolder.clearContext();
  }

  record SignIn(String username, String password) {}

  record SessionResponse(String username, String role, String displayName) {

    static SessionResponse of(AuthenticatedUser user) {
      return new SessionResponse(user.username(), user.role(), user.displayName());
    }
  }
}
