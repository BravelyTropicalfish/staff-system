package com.example.staffing.domain;

import java.util.List;

/**
 * One person in full: the master data with the single role, every skill with its band, and every
 * allocation current or past. Nothing here is writable, because person data only arrives by import.
 */
public record PersonProfileView(
    String employeeNo,
    String name,
    String email,
    String role,
    String jobLevel,
    String travel,
    boolean active,
    List<PersonSkillView> skills,
    List<PersonAllocationView> allocations) {}
