package com.example.staffing.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

/**
 * T05, session slice: sign in, read the current account, sign out, and refuse everything else.
 * The per-route role and scope checks (403) close T05 with each route group, because a guard with
 * no route to guard proves nothing.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SessionApiTest {

  private static final String DEMO_PASSWORD = "staffing-demo";

  @Autowired private TestRestTemplate rest;

  @Test
  void readingTheSessionWithoutSigningInIsRefused() {
    ResponseEntity<Map> response = rest.getForEntity("/api/session", Map.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    assertThat(response.getBody()).containsEntry("code", "unauthenticated");
  }

  @Test
  void everyOtherRouteIsRefusedWithoutASession() {
    ResponseEntity<Map> response = rest.getForEntity("/api/people", Map.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    assertThat(response.getBody()).containsEntry("code", "unauthenticated");
  }

  @Test
  void aWrongPasswordIsRefusedAndLeaksNoInternals() {
    ResponseEntity<Map> response = signIn("coordinator", "not-the-password");

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    assertThat(response.getBody()).containsEntry("code", "invalid credentials");
    assertThat(response.getBody().toString())
        .doesNotContain("com.example")
        .doesNotContain("Exception");
    assertThat(cookieOf(response)).isNull();
  }

  @Test
  void anUnknownUsernameGetsTheSameAnswerAsAWrongPassword() {
    ResponseEntity<Map> response = signIn("nobody", DEMO_PASSWORD);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    assertThat(response.getBody()).containsEntry("code", "invalid credentials");
  }

  @Test
  void aBodyWithoutCredentialsIsRefusedLikeAWrongOne() {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);

    ResponseEntity<Map> response =
        rest.exchange("/api/session", HttpMethod.POST, new HttpEntity<>(Map.of(), headers), Map.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    assertThat(response.getBody()).containsEntry("code", "invalid credentials");
    assertThat(cookieOf(response)).isNull();
  }

  @Test
  void theCoordinatorSignsInAndReadsHerOwnSession() {
    ResponseEntity<Map> signIn = signIn("coordinator", DEMO_PASSWORD);

    assertThat(signIn.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(signIn.getBody())
        .containsEntry("username", "coordinator")
        .containsEntry("role", "COORDINATOR")
        .containsEntry("displayName", "Resource coordinator");
    assertThat(signIn.getHeaders().get(HttpHeaders.SET_COOKIE))
        .anySatisfy(cookie -> assertThat(cookie).startsWith("JSESSIONID=").contains("HttpOnly"));

    ResponseEntity<Map> current = readSession(cookieOf(signIn));

    assertThat(current.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(current.getBody()).containsEntry("username", "coordinator");
  }

  @Test
  void aLeadSignsInAsALead() {
    ResponseEntity<Map> signIn = signIn("lead.one", DEMO_PASSWORD);

    assertThat(signIn.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(signIn.getBody())
        .containsEntry("username", "lead.one")
        .containsEntry("role", "LEAD");
  }

  @Test
  void signingOutEndsTheSessionForTheOldCookie() {
    String cookie = cookieOf(signIn("coordinator", DEMO_PASSWORD));
    assertThat(cookie).isNotNull();

    ResponseEntity<Void> signOut = rest.exchange(
        "/api/session", HttpMethod.DELETE, entity(cookie), Void.class);

    assertThat(signOut.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    assertThat(readSession(cookie).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
  }

  private ResponseEntity<Map> signIn(String username, String password) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    return rest.exchange(
        "/api/session",
        HttpMethod.POST,
        new HttpEntity<>(Map.of("username", username, "password", password), headers),
        Map.class);
  }

  private ResponseEntity<Map> readSession(String cookie) {
    return rest.exchange("/api/session", HttpMethod.GET, entity(cookie), Map.class);
  }

  private HttpEntity<Void> entity(String cookie) {
    HttpHeaders headers = new HttpHeaders();
    headers.add(HttpHeaders.COOKIE, cookie);
    return new HttpEntity<>(headers);
  }

  private String cookieOf(ResponseEntity<?> response) {
    List<String> cookies = response.getHeaders().get(HttpHeaders.SET_COOKIE);
    if (cookies == null) {
      return null;
    }
    return cookies.stream()
        .filter(cookie -> cookie.startsWith("JSESSIONID="))
        .map(cookie -> cookie.split(";", 2)[0])
        .findFirst()
        .orElse(null);
  }
}
