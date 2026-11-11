package com.example.staffing.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.Test;

/** T11, the ordered scales: a band is compared by rank, and the rank is the scale's position. */
class BandsTest {

  @Test
  void aHigherJobLevelOutranksALowerOne() {
    assertThat(Bands.rank(Bands.JOB_LEVELS, "Junior")).isLessThan(Bands.rank(Bands.JOB_LEVELS, "Lead"));
    assertThat(Bands.meets(Bands.JOB_LEVELS, "Lead", "Senior")).isTrue();
    assertThat(Bands.meets(Bands.JOB_LEVELS, "Junior", "Senior")).isFalse();
  }

  @Test
  void theProficiencyScaleRunsFromNoviceToExpert() {
    assertThat(Bands.rank(Bands.PROFICIENCIES, "Expert")).isEqualTo(4);
    assertThat(Bands.meets(Bands.PROFICIENCIES, "Practitioner", "Practitioner")).isTrue();
    assertThat(Bands.meets(Bands.PROFICIENCIES, "Advanced beginner", "Practitioner")).isFalse();
  }

  @Test
  void travelWillingnessIsOrderedToo() {
    assertThat(Bands.rank(Bands.TRAVEL, "none")).isZero();
    assertThat(Bands.rank(Bands.TRAVEL, "full")).isEqualTo(2);
  }

  @Test
  void aBandOutsideItsScaleIsRefusedRatherThanGuessed() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> Bands.rank(Bands.JOB_LEVELS, "Mid"))
        .withMessage("unknown band Mid");
  }
}
