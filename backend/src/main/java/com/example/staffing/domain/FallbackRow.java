package com.example.staffing.domain;

import java.util.List;

/**
 * One person offered by the cross-role fallback: someone of another role, at least one band above
 * the post's requirement, holding a skill of the post role's domain at `Practitioner` or above. The
 * row carries that evidence — the role, the bands above, the qualifying skill and its band, and the
 * gates that were evaluated — because the coordinator recommends on it and the snapshot copies it.
 *
 * <p>Nothing here makes the person a member of the post's role: the fallback is staffing advice for
 * one post, not a second role.
 */
public record FallbackRow(
    String employeeNo,
    String name,
    String role,
    String jobLevel,
    String travel,
    int bandsAbove,
    String skill,
    String band,
    List<String> gates) {}
