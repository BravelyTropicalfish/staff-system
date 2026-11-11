package com.example.staffing.api;

import com.example.staffing.domain.AuthenticatedUser;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;

/** The one place a controller asks "is this the coordinator?", so no route forgets to ask. */
final class Auth {

  private Auth() {}

  static AuthenticatedUser coordinator(Authentication authentication) {
    if (!(authentication.getPrincipal() instanceof AuthenticatedUser user)
        || !user.isCoordinator()) {
      throw new AccessDeniedException("这个动作只属于协调员。");
    }
    return user;
  }

  static AuthenticatedUser user(Authentication authentication) {
    return (AuthenticatedUser) authentication.getPrincipal();
  }
}
