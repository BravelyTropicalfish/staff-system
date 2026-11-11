package com.example.staffing.domain;

/**
 * One person kept off the suitable list, with the gate that refused them and its reason. Only the
 * first failing gate is reported, so an exclusion never lists a pile of causes.
 */
public record ExclusionRow(String employeeNo, String name, String role, String gate, String reason) {}
