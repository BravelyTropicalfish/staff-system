package com.example.staffing.domain;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Everything the gates ask about one person, gathered once: whether the person is active, the one
 * role they hold, their job level and travel willingness, the band held per skill, and every
 * allocation they carry. The caller assembles it from the pool, so no gate reads the database.
 *
 * <p>Roles and skills are compared by row id, not by name: the spec calls role equality "by
 * identity", and a catalog row a person or a post references can be neither renamed nor removed, so
 * the id is the honest key. The names travel along for the reason text.
 *
 * <p>The name and the employee number are carried because the suitable list is ordered by them; no
 * gate reads either.
 */
public record Candidate(
    String employeeNo,
    String name,
    boolean active,
    long roleId,
    String role,
    String jobLevel,
    String travel,
    Map<Long, String> skillBands,
    List<Allocation> allocations) {

  /** One allocation, closed at both ends: the person is occupied on every day it names. */
  public record Allocation(LocalDate start, LocalDate end) {}
}
