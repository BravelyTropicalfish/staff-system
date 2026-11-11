package com.example.staffing.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** T15, the deferred unit item: the status is exactly what the confirmed count makes it. */
class PostStatusTest {

  @Test
  void nobodyConfirmedLeavesThePostOpen() {
    assertThat(PostStatus.of(2, 0)).isEqualTo("open");
  }

  @Test
  void fewerThanTheHeadcountIsPartiallyFilled() {
    assertThat(PostStatus.of(2, 1)).isEqualTo("partially filled");
  }

  @Test
  void theWholeHeadcountIsFilled() {
    assertThat(PostStatus.of(2, 2)).isEqualTo("filled");
  }

  @Test
  void aSingleSeatCanBeFilled() {
    assertThat(PostStatus.of(1, 1)).isEqualTo("filled");
  }
}
