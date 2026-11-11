package com.example.staffing.domain;

import com.example.staffing.data.PersonRepository;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The read side of the pool: the list and the profile. Both are read-only by design — the only
 * writer of person data is {@link ImportService}, so no method here changes a row.
 */
@Service
public class PersonService {

  private final PersonRepository people;

  public PersonService(PersonRepository people) {
    this.people = people;
  }

  /** Everyone in the pool, by employee number, each with the one role they hold. */
  @Transactional(readOnly = true)
  public List<PersonSummaryView> list() {
    return people.findAll().stream()
        .map(
            row ->
                new PersonSummaryView(
                    row.employeeNo(),
                    row.name(),
                    row.role(),
                    row.jobLevel(),
                    row.travel(),
                    row.active()))
        .toList();
  }

  /** One person: master data, every band strongest first, every allocation newest first. */
  @Transactional(readOnly = true)
  public PersonProfileView profile(String employeeNo) {
    PersonRepository.PersonRow person =
        people.findByEmployeeNo(employeeNo)
            .orElseThrow(() -> new NoSuchElementException("No person " + employeeNo));

    List<PersonSkillView> skills =
        people.skillBandsOf(person.employeeNo()).stream()
            .sorted(
                Comparator.comparingInt(
                        (PersonRepository.SkillBandRow row) ->
                            -Bands.rank(Bands.PROFICIENCIES, row.band()))
                    .thenComparing(PersonRepository.SkillBandRow::skill))
            .map(row -> new PersonSkillView(row.skill(), row.domain(), row.band()))
            .toList();

    LocalDate today = LocalDate.now();
    List<PersonAllocationView> allocations =
        people.allocationsOf(person.employeeNo()).stream()
            .map(
                row ->
                    new PersonAllocationView(
                        row.id(),
                        row.project(),
                        row.startDate().toString(),
                        row.endDate().toString(),
                        row.source(),
                        row.endDate().isBefore(today)))
            .toList();

    return new PersonProfileView(
        person.employeeNo(),
        person.name(),
        person.email(),
        person.role(),
        person.jobLevel(),
        person.travel(),
        person.active(),
        skills,
        allocations);
  }
}
