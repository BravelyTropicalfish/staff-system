package com.example.staffing.domain;

/**
 * One entry of the coordinator's notice list: which post, which person, which reason and when —
 * enough to explain a loss without opening the recommendation it came from. The time is the void's
 * time and never moves; `dismissedAt` is null while the entry is still listed.
 */
public record NoticeView(
    long id,
    long postId,
    String postTitle,
    String personNo,
    String personName,
    String reason,
    String createdAt,
    String dismissedAt) {}
