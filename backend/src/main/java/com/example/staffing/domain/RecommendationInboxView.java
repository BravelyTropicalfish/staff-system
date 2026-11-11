package com.example.staffing.domain;

/**
 * One proposal in the decision inbox: the person, the post and the project it belongs to, the state
 * it is in, the reason it was voided when it was, by whom and when.
 *
 * <p>The same visibility rule as the post's own list applies: the coordinator reads the reason
 * snapshot with every row, the lead reads who is proposed and in which state and never the evidence
 * behind the proposal.
 */
public record RecommendationInboxView(
    long id,
    long postId,
    String postTitle,
    String project,
    String personNo,
    String personName,
    boolean personActive,
    String state,
    String voidReason,
    RecommendationSnapshot snapshot,
    String proposedBy,
    String proposedAt) {}
