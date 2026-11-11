package com.example.staffing.api;

import com.example.staffing.domain.AllocationService;
import com.example.staffing.domain.AllocationView;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * The hand-entered allocations. All three verbs are the coordinator's: a lead never records, edits
 * or removes occupancy by hand. An allocation a confirmation wrote answers 409 with the
 * release-first reason instead of being edited, and overlapping ranges are accepted as written.
 */
@RestController
@RequestMapping("/api/allocations")
public class AllocationController {

  private final AllocationService allocations;

  public AllocationController(AllocationService allocations) {
    this.allocations = allocations;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  AllocationView create(@Valid @RequestBody AllocationCreate body, Authentication authentication) {
    Auth.coordinator(authentication);
    return allocations.create(body.personNo(), body.projectId(), body.startDate(), body.endDate());
  }

  @PatchMapping("/{id}")
  AllocationView update(
      @PathVariable long id, @Valid @RequestBody RangePatch body, Authentication authentication) {
    Auth.coordinator(authentication);
    return allocations.update(id, body.startDate(), body.endDate());
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  void delete(@PathVariable long id, Authentication authentication) {
    Auth.coordinator(authentication);
    allocations.delete(id);
  }

  record AllocationCreate(
      @NotBlank String personNo,
      @NotNull Long projectId,
      @NotNull LocalDate startDate,
      @NotNull LocalDate endDate) {}

  record RangePatch(@NotNull LocalDate startDate, @NotNull LocalDate endDate) {}
}
