package com.example.staffing.domain;

/**
 * An allocation rule that refused the call. The code is the contract the API maps to a status:
 * `invalid allocation` 422, `release first` 409.
 */
public class AllocationException extends RuntimeException {

  private final String code;

  public AllocationException(String code, String message) {
    super(message);
    this.code = code;
  }

  public String code() {
    return code;
  }
}
