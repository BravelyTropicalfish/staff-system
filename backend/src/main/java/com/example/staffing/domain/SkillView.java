package com.example.staffing.domain;

/** A skill with the single role whose domain holds it. */
public record SkillView(
    long id, String name, long roleDomainId, String roleDomainName, boolean active) {}
