package com.example.staffing.domain;

import java.util.List;

/**
 * What one confirmation wrote, as the lead and the coordinator read it back: the recommendation now
 * `confirmed` with its untouched snapshot, the assignment and its allocation, the post's status and
 * remaining headcount after the seat was filled, and every proposal of that person the confirmation
 * voided because its post's dates intersect the written allocation.
 */
public record ConfirmationView(
    RecommendationView recommendation,
    long assignmentId,
    long allocationId,
    String postStatus,
    int remaining,
    List<VoidedView> voided) {}
