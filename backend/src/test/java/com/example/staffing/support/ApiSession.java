package com.example.staffing.support;

import java.util.List;
import java.util.Map;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * A signed-in caller for the API tests: every request carries the session cookie a real browser
 * would carry. SessionApiTest keeps its own raw cookie handling on purpose, because the cookie is
 * what that class asserts on.
 */
public final class ApiSession {

  private static final String DEMO_PASSWORD = "staffing-demo";

  private final TestRestTemplate rest;
  private final String cookie;

  private ApiSession(TestRestTemplate rest, String cookie) {
    this.rest = rest;
    this.cookie = cookie;
  }

  public static ApiSession signIn(TestRestTemplate rest, String username) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);

    ResponseEntity<Map> response =
        rest.exchange(
            "/api/session",
            HttpMethod.POST,
            new HttpEntity<>(Map.of("username", username, "password", DEMO_PASSWORD), headers),
            Map.class);

    if (response.getStatusCode() != HttpStatus.OK) {
      throw new IllegalStateException("sign-in failed for " + username + ": " + response.getStatusCode());
    }
    return new ApiSession(rest, cookieOf(response));
  }

  public <T> ResponseEntity<T> get(String path, Class<T> type) {
    return rest.exchange(path, HttpMethod.GET, entity(null), type);
  }

  public <T> ResponseEntity<T> post(String path, Object body, Class<T> type) {
    return rest.exchange(path, HttpMethod.POST, entity(body), type);
  }

  public <T> ResponseEntity<T> patch(String path, Object body, Class<T> type) {
    return rest.exchange(path, HttpMethod.PATCH, entity(body), type);
  }

  public <T> ResponseEntity<T> delete(String path, Class<T> type) {
    return rest.exchange(path, HttpMethod.DELETE, entity(null), type);
  }

  /** Sends the file as the one part a browser form upload would send. */
  public <T> ResponseEntity<T> upload(String path, String filename, byte[] content, Class<T> type) {
    HttpHeaders headers = new HttpHeaders();
    headers.add(HttpHeaders.COOKIE, cookie);
    headers.setContentType(MediaType.MULTIPART_FORM_DATA);
    MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
    body.add(
        "file",
        new ByteArrayResource(content) {
          @Override
          public String getFilename() {
            return filename;
          }
        });
    return rest.exchange(path, HttpMethod.POST, new HttpEntity<>(body, headers), type);
  }

  private HttpEntity<Object> entity(Object body) {
    HttpHeaders headers = new HttpHeaders();
    headers.add(HttpHeaders.COOKIE, cookie);
    if (body != null) {
      headers.setContentType(MediaType.APPLICATION_JSON);
    }
    return new HttpEntity<>(body, headers);
  }

  private static String cookieOf(ResponseEntity<?> response) {
    List<String> cookies = response.getHeaders().get(HttpHeaders.SET_COOKIE);
    if (cookies == null) {
      throw new IllegalStateException("sign-in returned no cookie");
    }
    return cookies.stream()
        .filter(value -> value.startsWith("JSESSIONID="))
        .map(value -> value.split(";", 2)[0])
        .findFirst()
        .orElseThrow(() -> new IllegalStateException("sign-in returned no session cookie"));
  }
}
