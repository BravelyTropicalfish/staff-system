package com.example.staffing.domain;

/**
 * One proposal on a post's own screen: who was proposed, the decision state, the reason it was
 * voided when it was, by whom and when.
 *
 * <p>{@code snapshot} is the reason snapshot — the gates and the qualifying skills taken when the
 * proposal was made — and it is answered to the coordinator only: the spec lets the lead read who
 * is proposed and in which state, while the evidence behind the proposal belongs to the coordinator
 * who made it.
 */
public record PostRecommendationView(
    long id,
    String personNo,
    String personName,
    boolean personActive,
    String state,
    String voidReason,
    RecommendationSnapshot snapshot,
    String proposedBy,
    String proposedAt) {}
