package com.example.staffing.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.staffing.domain.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;

/**
 * T30, the sign-out branch no signed-in caller can reach: a request that carries an authentication
 * but no session to invalidate. The API-level sign-out is SessionApiTest's; this pins that the
 * handler skips the invalidation instead of failing, which is what keeps a repeated sign-out clean.
 */
class SessionSignOutTest {

  private final SessionController controller =
      new SessionController(mock(AuthService.class), mock(SecurityContextRepository.class));

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
}
