package com.example.staffing.api;

import com.example.staffing.domain.ProjectService;
import com.example.staffing.domain.ProjectView;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Projects. The list answers both roles with what each may see (the coordinator everything, a lead
 * only its own); a project is opened by its own lead, never by the coordinator; the end is the
 * owning lead's. Each refusal is the server's answer and not only the screen's.
 */
@RestController
@RequestMapping("/api/projects")
public class ProjectController {

  private final ProjectService projects;

  public ProjectController(ProjectService projects) {
    this.projects = projects;
  }

  @GetMapping
  List<ProjectView> list(Authentication authentication) {
    return projects.list(Auth.user(authentication));
  }

  @GetMapping("/{id}")
  ProjectView get(@PathVariable long id, Authentication authentication) {
    return projects.get(id, Auth.user(authentication));
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  ProjectView create(@Valid @RequestBody ProjectCreate body, Authentication authentication) {
    return projects.create(
        body.name(), body.lead(), body.startDate(), body.endDate(), Auth.user(authentication));
  }

  @PostMapping("/{id}/end")
  ProjectView end(@PathVariable long id, Authentication authentication) {
    return projects.end(id, Auth.user(authentication));
  }

  record ProjectCreate(
      @NotBlank @Size(max = 120) String name,
      @NotBlank String lead,
      @NotNull LocalDate startDate,
      @NotNull LocalDate endDate) {}
}
