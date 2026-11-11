package com.example.staffing.domain;

/** The signed-in account, as every service sees it: identity, the one role and the display name. */
public record AuthenticatedUser(long id, String username, String role, String displayName) {

  public static final String COORDINATOR = "COORDINATOR";
  public static final String LEAD = "LEAD";

  public boolean isCoordinator() {
    return COORDINATOR.equals(role);
  }
}
