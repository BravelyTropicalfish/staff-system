package com.example.staffing.domain;

/**
 * One project as the list and the create answer show it: the name, the lead who owns it, its
 * inclusive range and the terminal status.
 */
public record ProjectView(
    long id,
    String name,
    String lead,
    String leadName,
    String status,
    String startDate,
    String endDate) {}
