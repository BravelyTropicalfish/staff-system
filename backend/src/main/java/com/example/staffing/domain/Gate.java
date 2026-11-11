package com.example.staffing.domain;

/**
 * The six eligibility gates in the order the spec fixes them. The declaration order is the
 * evaluation order, so walking the enum walks the contract.
 */
public enum Gate {
  /** G1 — the person is active. */
  ACTIVE("G1"),
  /** G2 — the person's role is the post's role, by identity. */
  ROLE("G2"),
  /** G3 — the job level is at or above the required band. */
  JOB_LEVEL("G3"),
  /** G4 — every required skill is held at or above its minimum band. */
  REQUIRED_SKILL("G4"),
  /** G5 — the travel willingness is at or above the required rank. */
  TRAVEL("G5"),
  /** G6 — no allocation intersects the post's inclusive dates. */
  DATES("G6");

  private final String code;

  Gate(String code) {
    this.code = code;
  }

  /** The name the spec uses for this gate, e.g. {@code G3}. */
  public String code() {
    return code;
  }
}
