package com.example.staffing.domain;

import com.example.staffing.data.AllocationRepository;
import com.example.staffing.data.PersonRepository;
import com.example.staffing.data.ProjectRepository;
import java.time.LocalDate;
import java.util.NoSuchElementException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The hand-entered allocations. The coordinator records a person staffed outside the app, may
 * change or remove that record at any time, and overlapping ranges are stored as a fact — the person
 * is occupied on those days, which is what G6 reads. An allocation a confirmation wrote is not a
 * hand record: it goes away only with a release or a project end, so both edits are refused with the
 * release-first reason.
 */
@Service
public class AllocationService {

  static final String MANUAL = "MANUAL";

  private final AllocationRepository allocations;
  private final PersonRepository people;
  private final ProjectRepository projects;

  public AllocationService(
      AllocationRepository allocations, PersonRepository people, ProjectRepository projects) {
    this.allocations = allocations;
    this.people = people;
    this.projects = projects;
  }

  @Transactional
  public AllocationView create(String personNo, long projectId, LocalDate start, LocalDate end) {
    requireDates(start, end);
    String employeeNo = personNo == null ? "" : personNo.trim();
    if (people.findByEmployeeNo(employeeNo).isEmpty()) {
      throw invalid("person " + personNo + " is not in the pool");
    }
    ProjectRepository.ProjectRow project =
        projects
            .findById(projectId)
            .orElseThrow(() -> invalid("project " + projectId + " is not a project"));
    if (!"OPEN".equals(project.status())) {
      throw new ProjectException(
          "project closed", "the project " + project.name() + " is closed");
    }
    long id = allocations.insertManual(employeeNo, projectId, start, end);
    return view(find(id));
  }

  @Transactional
  public AllocationView update(long id, LocalDate start, LocalDate end) {
    requireDates(start, end);
    AllocationRepository.AllocationRow allocation = find(id);
    requireHandEntered(allocation);
    allocations.updateDates(id, start, end);
    return view(find(id));
  }

  @Transactional
  public void delete(long id) {
    requireHandEntered(find(id));
    allocations.delete(id);
  }

  private void requireHandEntered(AllocationRepository.AllocationRow allocation) {
    if (!MANUAL.equals(allocation.source())) {
      throw new AllocationException(
          "release first",
          "the allocation of "
              + allocation.personNo()
              + " on "
              + allocation.project()
              + " came from a confirmation; release the person instead");
    }
  }

  private static void requireDates(LocalDate start, LocalDate end) {
    if (start == null || end == null) {
      throw invalid("the allocation needs a start and an end date");
    }
    if (end.isBefore(start)) {
      throw invalid("the end date cannot precede the start date");
    }
  }

  private AllocationRepository.AllocationRow find(long id) {
    return allocations
        .findById(id)
        .orElseThrow(() -> new NoSuchElementException("No allocation " + id));
  }

  private static AllocationException invalid(String message) {
    return new AllocationException("invalid allocation", message);
  }

  private static AllocationView view(AllocationRepository.AllocationRow row) {
    return new AllocationView(
        row.id(),
        row.personNo(),
        row.project(),
        row.startDate().toString(),
        row.endDate().toString(),
        row.source());
  }
}
