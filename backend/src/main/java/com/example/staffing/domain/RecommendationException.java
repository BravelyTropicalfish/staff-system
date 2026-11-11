package com.example.staffing.domain;

/**
 * A recommendation the rules refused. Every code here is a state conflict, never a validation: the
 * post no longer takes a proposal (`post filled`, `post cancelled`), the person is not on either
 * list (`not a candidate`), the same person already waits on this post (`already proposed`), or the
 * person no longer passes a gate — then the code is the gate itself, `G1` to `G6`, `X1` or `X2`,
 * and the message is the reason the exclusion list shows.
 */
public class RecommendationException extends RuntimeException {

  private final String code;

  public RecommendationException(String code, String message) {
    super(message);
    this.code = code;
  }

  public String code() {
    return code;
  }
}
