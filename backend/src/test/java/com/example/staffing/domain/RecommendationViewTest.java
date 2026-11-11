package com.example.staffing.domain;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.staffing.data.RecommendationRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

/**
 * A stored snapshot is read back on every route that answers a recommendation, so a row whose JSON
 * no longer parses must fail loudly rather than read as a reason with nothing in it.
 */
class RecommendationViewTest {

  @Test
  void anUnreadableSnapshotFailsRatherThanReadingAsAnEmptyReason() {
    RecommendationRepository.Row row =
        new RecommendationRepository.Row(
            1,
            2,
            "E100",
            "E100",
            true,
            "proposed",
            null,
            "not json",
            "coordinator",
            LocalDateTime.parse("2027-01-01T09:00"));

    assertThatThrownBy(() -> RecommendationView.of(row, new ObjectMapper()))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("the stored reason snapshot is unreadable");
  }
}
