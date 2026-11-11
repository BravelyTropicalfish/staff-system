package com.example.staffing.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

/** T14, the G6 predicate: a range occupies its dates inclusively, so a boundary day still blocks. */
class RangesTest {

  private static final LocalDate ALLOC_START = LocalDate.parse("2026-12-01");
  private static final LocalDate ALLOC_END = LocalDate.parse("2027-01-01");

  @Test
  void aBoundaryDayStillBlocks() {
    assertThat(
            Ranges.intersects(
                ALLOC_START, ALLOC_END, LocalDate.parse("2027-01-01"), LocalDate.parse("2027-03-31")))
        .isTrue();
  }

  @Test
  void aLaterPostStaysOpen() {
    assertThat(
            Ranges.intersects(
                ALLOC_START, ALLOC_END, LocalDate.parse("2027-02-01"), LocalDate.parse("2027-04-30")))
        .isFalse();
  }

  @Test
  void thePostEndTouchingTheAllocationStartBlocks() {
    assertThat(
            Ranges.intersects(
                ALLOC_START, ALLOC_END, LocalDate.parse("2026-11-01"), LocalDate.parse("2026-12-01")))
        .isTrue();
  }

  @Test
  void theDayAfterTheAllocationEndsStaysOpen() {
    assertThat(
            Ranges.intersects(
                ALLOC_START, ALLOC_END, LocalDate.parse("2027-01-02"), LocalDate.parse("2027-01-31")))
        .isFalse();
  }

  @Test
  void aPostInsideTheAllocationBlocks() {
    assertThat(
            Ranges.intersects(
                ALLOC_START, ALLOC_END, LocalDate.parse("2026-12-10"), LocalDate.parse("2026-12-20")))
        .isTrue();
  }
}
