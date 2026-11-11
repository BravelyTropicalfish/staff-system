package com.example.staffing.domain;

/**
 * One bar on the role axis. Every role the catalog still offers is present, so a count of zero is a
 * bar at zero rather than a missing category; {@code share} is a percentage with one decimal.
 */
public record RoleBar(long roleId, String role, int count, double share) {}
