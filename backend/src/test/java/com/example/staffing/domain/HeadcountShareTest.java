package com.example.staffing.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * T03's unit half: the one rounding rule. The API checks assert the rounded values the pool produces;
 * this pins the branches a fixture cannot reach cheaply — an exact half and a pool with no divisor.
 */
class HeadcountShareTest {

  @Test
  void aShareIsOneDecimalRoundedHalfUp() {
    assertThat(HeadcountService.share(40, 113)).isEqualTo(35.4);
    assertThat(HeadcountService.share(38, 113)).isEqualTo(33.6);
    assertThat(HeadcountService.share(1, 3)).isEqualTo(33.3);
    assertThat(HeadcountService.share(2, 3)).isEqualTo(66.7);
  }

  @Test
  void anExactHalfRoundsUp() {
    assertThat(HeadcountService.share(1, 8)).isEqualTo(12.5);
    assertThat(HeadcountService.share(1, 16)).isEqualTo(6.3);
  }

  @Test
  void aCategoryThatHoldsEverybodyReadsOneHundred() {
    assertThat(HeadcountService.share(113, 113)).isEqualTo(100.0);
  }

  @Test
  void aPoolWithNobodyHasNoDivisor() {
    assertThat(HeadcountService.share(0, 0)).isEqualTo(0.0);
  }
}
