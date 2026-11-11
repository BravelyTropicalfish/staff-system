package com.example.staffing.domain;

/**
 * A project rule that refused the call. The code is the contract the API maps to a status:
 * `invalid project` 422, `project closed` 409.
 */
public class ProjectException extends RuntimeException {

  private final String code;

  public ProjectException(String code, String message) {
    super(message);
    this.code = code;
  }

  public String code() {
    return code;
  }
}
