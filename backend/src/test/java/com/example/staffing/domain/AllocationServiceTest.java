package com.example.staffing.domain;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.staffing.data.AllocationRepository;
import com.example.staffing.data.PersonRepository;
import com.example.staffing.data.ProjectRepository;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * The hand-entered allocation's own guards: the route requires a person number and both dates, so
 * these two refusals answer a caller inside the application.
 */
class AllocationServiceTest {

  private static final LocalDate START = LocalDate.parse("2027-01-01");
  private static final LocalDate END = LocalDate.parse("2027-03-31");

  private final AllocationRepository allocations = mock(AllocationRepository.class);
  private final PersonRepository people = mock(PersonRepository.class);
  private final AllocationService service =
      new AllocationService(allocations, people, mock(ProjectRepository.class));

  @Test
  void anAllocationNeedsAPersonInThePool() {
    when(people.findByEmployeeNo("")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.create(null, 1, START, END))
        .isInstanceOf(AllocationException.class)
        .hasMessage("person null is not in the pool");
  }

  @Test
  void anAllocationNeedsBothDates() {
    assertThatThrownBy(() -> service.update(1, null, END))
        .isInstanceOf(AllocationException.class)
        .hasMessage("the allocation needs a start and an end date");
    assertThatThrownBy(() -> service.update(1, START, null))
        .isInstanceOf(AllocationException.class)
        .hasMessage("the allocation needs a start and an end date");
  }
}
