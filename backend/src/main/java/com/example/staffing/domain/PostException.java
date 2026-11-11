package com.example.staffing.domain;

/**
 * A post rule that refused the call. The code is the contract the API maps to a status:
 * `invalid post` 422, `release first` 409 while a confirmed person blocks the change,
 * `post cancelled` 409 because a cancelled post is read-only.
 */
public class PostException extends RuntimeException {

  private final String code;

  public PostException(String code, String message) {
    super(message);
    this.code = code;
  }

  public String code() {
    return code;
  }
}
