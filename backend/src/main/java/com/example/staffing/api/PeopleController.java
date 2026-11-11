package com.example.staffing.api;

import com.example.staffing.domain.PersonProfileView;
import com.example.staffing.domain.PersonService;
import com.example.staffing.domain.PersonSummaryView;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The pool's read surface: the list and one profile. Both are the coordinator's, as the spec's
 * "readable by the coordinator" requires, and neither answers a write method: person data only
 * arrives by import, so a PUT, POST or DELETE here is simply not a route (405).
 */
@RestController
@RequestMapping("/api/people")
public class PeopleController {

  private final PersonService people;

  public PeopleController(PersonService people) {
    this.people = people;
  }

  @GetMapping
  public List<PersonSummaryView> list(Authentication authentication) {
    Auth.coordinator(authentication);
    return people.list();
  }

  @GetMapping("/{employeeNo}")
  public PersonProfileView profile(
      @PathVariable String employeeNo, Authentication authentication) {
    Auth.coordinator(authentication);
    return people.profile(employeeNo);
  }
}
