package com.example.staffing.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.staffing.domain.AuthenticatedUser;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

/**
 * T30, the one guard every coordinator-only route shares. T06 gives the refusal its API-level proof;
 * this pins the branch that a session can only reach if the principal is something other than the
 * account this application put there, which no route can produce and the guard must still refuse.
 */
class AuthTest {

  private static final AuthenticatedUser COORDINATOR =
      new AuthenticatedUser(1, "coordinator", AuthenticatedUser.COORDINATOR, "Coordinator");
  private static final AuthenticatedUser LEAD =
      new AuthenticatedUser(2, "lead.one", AuthenticatedUser.LEAD, "Lead one");

  @Test
  void aCoordinatorPassesAndALeadDoesNot() {
    assertThat(Auth.coordinator(token(COORDINATOR))).isEqualTo(COORDINATOR);
    assertThatThrownBy(() -> Auth.coordinator(token(LEAD)))
        .isInstanceOf(AccessDeniedException.class)
        .hasMessage("这个动作只属于协调员。");
  }

  @Test
  void aPrincipalThatIsNotAnAccountIsRefusedRatherThanCast() {
    assertThatThrownBy(() -> Auth.coordinator(token("someone-else")))
        .isInstanceOf(AccessDeniedException.class)
        .hasMessage("这个动作只属于协调员。");
  }

  @Test
  void theReadRouteAcceptsEitherRoleAndCastsNothingAway() {
    assertThat(Auth.user(token(LEAD))).isEqualTo(LEAD);
    assertThat(Auth.user(token(COORDINATOR))).isEqualTo(COORDINATOR);
  }

  private static Authentication token(Object principal) {
    return new UsernamePasswordAuthenticationToken(principal, null, List.of());
  }
}
