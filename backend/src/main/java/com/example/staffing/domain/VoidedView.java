package com.example.staffing.domain;

/**
 * A proposal a confirmation voided, with the reason the coordinator's notice list will show. It is
 * one row here so the answer says which proposals were lost and why, not merely how many.
 */
public record VoidedView(long recommendationId, long postId, String personNo, String reason) {}
