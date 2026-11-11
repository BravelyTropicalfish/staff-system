package com.example.staffing.domain;

/**
 * A catalog rule that refused the call. The code is the contract the API maps to a status: `not
 * found` 404, `invalid name` and `invalid role domain` 422, everything else 409.
 */
public class CatalogException extends RuntimeException {

  private final String code;

  public CatalogException(String code, String message) {
    super(message);
    this.code = code;
  }

  public String code() {
    return code;
  }
}
