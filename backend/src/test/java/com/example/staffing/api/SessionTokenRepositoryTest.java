package com.example.staffing.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.staffing.domain.AuthenticatedUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpRequestResponseHolder;

/**
 * The two identities one browser may hold: the cookie every tab shares and the token one tab keeps.
 * These cases pin the precedence rule — a named token always decides, an unknown one is signed out
 * instead of falling back, and only a request without a token touches the session.
 */
class SessionTokenRepositoryTest {

  private final SessionTokens tokens = new SessionTokens();
  private final SessionTokenRepository repository = new SessionTokenRepository(tokens);

  @Test
  void aKnownTokenIsTheWholeIdentity() {
    String token = tokens.issue(contextFor("lead.one"));

    SecurityContext loaded = repository.loadContext(holder(request(token), mock(HttpServletResponse.class)));

    assertThat(principal(loaded)).isEqualTo("lead.one");
  }

  @Test
  void anUnknownTokenIsSignedOutRatherThanFallingBackToTheCookie() {
    HttpServletRequest request = request("forgotten");

    SecurityContext loaded =
        repository.loadContext(holder(request, mock(HttpServletResponse.class)));

    assertThat(loaded.getAuthentication()).isNull();
    verify(request, never()).getSession(anyBoolean());
  }

  @Test
  void withoutATokenTheSessionCookieDecides() {
    HttpServletRequest request = request(null);
    when(request.getSession(false)).thenReturn(null);

    SecurityContext loaded =
        repository.loadContext(holder(request, mock(HttpServletResponse.class)));

    assertThat(loaded.getAuthentication()).isNull();
    verify(request).getSession(false);
  }

  @Test
  void containsContextFollowsTheNamedTokenAndOtherwiseTheSession() {
    String token = tokens.issue(contextFor("coordinator"));

    HttpServletRequest known = request(token);
    HttpServletRequest unknown = request("forgotten");
    HttpServletRequest none = request(null);
    when(none.getSession(false)).thenReturn(null);

    assertThat(repository.containsContext(known)).isTrue();
    assertThat(repository.containsContext(unknown)).isFalse();
    assertThat(repository.containsContext(none)).isFalse();
  }

  @Test
  void aTokenRequestNeverWritesTheSession() {
    HttpServletRequest request = request("tab-token");

    repository.saveContext(contextFor("lead.one"), request, mock(HttpServletResponse.class));

    verify(request, never()).getSession(anyBoolean());
  }

  @Test
  void aCookieRequestStillWritesTheSessionTheApiClientsUse() {
    HttpServletRequest request = request(null);
    HttpSession session = mock(HttpSession.class);
    when(request.getSession(true)).thenReturn(session);

    repository.saveContext(contextFor("coordinator"), request, mock(HttpServletResponse.class));

    verify(request).getSession(true);
    verify(session).setAttribute(anyString(), any());
  }

  @Test
  void revokingAnAbsentTokenChangesNothing() {
    tokens.revoke(null);

    assertThat(tokens.find(null)).isEmpty();
    assertThat(tokens.find("never-issued")).isEmpty();
  }

  private static HttpServletRequest request(String token) {
    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getHeader(SessionTokens.HEADER)).thenReturn(token);
    return request;
  }

  private static HttpRequestResponseHolder holder(
      HttpServletRequest request, HttpServletResponse response) {
    return new HttpRequestResponseHolder(request, response);
  }

  private static SecurityContext contextFor(String username) {
    AuthenticatedUser user =
        new AuthenticatedUser(2L, username, AuthenticatedUser.LEAD, "Project lead one");
    SecurityContext context = SecurityContextHolder.createEmptyContext();
    context.setAuthentication(
        new UsernamePasswordAuthenticationToken(
            user, null, List.of(new SimpleGrantedAuthority("ROLE_LEAD"))));
    return context;
  }

  private static String principal(SecurityContext context) {
    return ((AuthenticatedUser) context.getAuthentication().getPrincipal()).username();
  }
}
