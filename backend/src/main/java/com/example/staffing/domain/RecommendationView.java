package com.example.staffing.domain;

import com.example.staffing.data.RecommendationRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * A recommendation as the API answers it: the stored row's state, the snapshot read back from the
 * database with it, and who proposed it and when. Reading the snapshot back rather than echoing the
 * one the request built is the proof that what the coordinator is told is what was stored.
 */
public record RecommendationView(
    long id,
    long postId,
    String personNo,
    String state,
    RecommendationSnapshot snapshot,
    String proposedBy,
    String proposedAt) {

  /** The stored row and its snapshot, read back together; one mapping for every route that answers one. */
  public static RecommendationView of(RecommendationRepository.Row row, ObjectMapper json) {
    return new RecommendationView(
        row.id(),
        row.postId(),
        row.personNo(),
        row.state(),
        snapshotOf(row.snapshotJson(), json),
        row.proposedBy(),
        row.proposedAt().toString());
  }

  /** The reason snapshot exactly as it was stored, for the routes that answer it on its own. */
  public static RecommendationSnapshot snapshotOf(String snapshotJson, ObjectMapper json) {
    try {
      return json.readValue(snapshotJson, RecommendationSnapshot.class);
    } catch (JsonProcessingException broken) {
      throw new IllegalStateException("the stored reason snapshot is unreadable", broken);
    }
  }
}
