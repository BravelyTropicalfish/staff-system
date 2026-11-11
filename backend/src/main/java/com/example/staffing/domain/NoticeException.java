package com.example.staffing.domain;

/** A refusal about a notice: the entry was dismissed already and cannot be dismissed twice. */
public class NoticeException extends RuntimeException {

  private final String code;

  public NoticeException(String code, String message) {
    super(message);
    this.code = code;
  }

  public String code() {
    return code;
  }
}
