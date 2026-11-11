package com.example.staffing.domain;

/**
 * One skill a person holds, with the band by name and the one role whose domain holds the skill.
 * The domain is displayed as context; it never changes the person's role.
 */
public record PersonSkillView(String skill, String roleDomain, String band) {}
