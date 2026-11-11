package com.example.staffing.domain;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * The catalog and the employee list as they stand before an upload. Keys are {@link #key(String)},
 * so a sheet may spell {@code dev} while the catalog says {@code DEV}: the stored spelling wins.
 */
record ImportReference(
    Map<String, RoleRef> roles, Map<String, SkillRef> skills, Set<String> knownEmployees) {

  record RoleRef(long id, String name) {}

  record SkillRef(long id, String name) {}

  static String key(String name) {
    return name.trim().toLowerCase(Locale.ROOT);
  }

  /** The accepted role names, sorted so the reason reads the same on every run. */
  List<String> roleNames() {
    return roles.values().stream().map(RoleRef::name).sorted().toList();
  }
}
