package com.example.staffing.domain;

/**
 * The status the confirmed count dictates: none confirmed is `open`, some but not the whole
 * headcount `partially filled`, as many as the headcount `filled`. `cancelled` is the one status a
 * person sets, so it never comes out of this derivation.
 */
public final class PostStatus {

  public static final String OPEN = "open";
  public static final String PARTIALLY_FILLED = "partially filled";
  public static final String FILLED = "filled";
  public static final String CANCELLED = "cancelled";

  private PostStatus() {}

  public static String of(int headcount, int confirmed) {
    if (confirmed <= 0) {
      return OPEN;
    }
    return confirmed >= headcount ? FILLED : PARTIALLY_FILLED;
  }
}
