package com.example.staffing.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.staffing.domain.AuthService;
import com.example.staffing.domain.AuthenticatedUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;

/**
 * T30, the sign-out branch no signed-in caller can reach: a request that carries an authentication
 * but no session to invalidate. The API-level sign-out is SessionApiTest's; this pins that the
 * handler skips the invalidation instead of failing, which is what keeps a repeated sign-out clean.
 */
class SessionSignOutTest {

  private final SessionTokens tokens = new SessionTokens();
  private final SessionController controller =
      new SessionController(
          mock(AuthService.class), mock(SecurityContextRepository.class), tokens);

  @AfterEach
  void clearTheContext() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void aSignOutWithoutASessionStillClearsTheContext() {
    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getSession(false)).thenReturn(null);

    controller.signOut(request);

    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
  }

  @Test
  void aSignOutWithASessionInvalidatesIt() {
    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpSession session = mock(HttpSession.class);
    when(request.getSession(false)).thenReturn(session);

    controller.signOut(request);

    verify(session).invalidate();
  }

  @Test
  void aSignOutThatCarriesATokenEndsThatTabOnly() {
    AuthenticatedUser user =
        new AuthenticatedUser(1, "coordinator", AuthenticatedUser.COORDINATOR, "Coordinator");
    SecurityContext context = SecurityContextHolder.createEmptyContext();
    context.setAuthentication(
        new UsernamePasswordAuthenticationToken(
            user, null, List.of(new SimpleGrantedAuthority("ROLE_COORDINATOR"))));
    String token = tokens.issue(context);

    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getHeader(SessionTokens.HEADER)).thenReturn(token);

    controller.signOut(request);

    assertThat(tokens.find(token)).isEmpty();
    verify(request, never()).getSession(false);
  }
}
