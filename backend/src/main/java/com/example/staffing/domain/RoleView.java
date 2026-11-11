package com.example.staffing.domain;

import java.util.List;

/** A role with its skill domain: every skill whose single domain is this role. */
public record RoleView(long id, String name, boolean active, List<SkillView> skills) {}
