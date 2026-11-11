package com.example.staffing.api;

import com.example.staffing.domain.Bands;
import com.example.staffing.domain.BandsView;
import com.example.staffing.domain.CatalogService;
import com.example.staffing.domain.RoleView;
import com.example.staffing.domain.SkillView;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * The catalogs. Reads are open to both roles because a lead picks a role and a skill on a post;
 * every write is the coordinator's, and the server says so even when the screen hides the control.
 */
@RestController
@RequestMapping("/api")
public class CatalogController {

  private final CatalogService catalog;

  public CatalogController(CatalogService catalog) {
    this.catalog = catalog;
  }

  @GetMapping("/roles")
  List<RoleView> roles() {
    return catalog.roles();
  }

  /** The three ordered scales, read by both roles: a post form offers them and copies nothing. */
  @GetMapping("/bands")
  BandsView bands() {
    return new BandsView(Bands.JOB_LEVELS, Bands.PROFICIENCIES, Bands.TRAVEL);
  }

  @PostMapping("/roles")
  @ResponseStatus(HttpStatus.CREATED)
  RoleView createRole(@Valid @RequestBody RoleName body, Authentication authentication) {
    Auth.coordinator(authentication);
    return catalog.createRole(body.name());
  }

  @PatchMapping("/roles/{id}")
  RoleView patchRole(
      @PathVariable long id, @Valid @RequestBody RolePatch body, Authentication authentication) {
    Auth.coordinator(authentication);
    return catalog.updateRole(id, body.name(), body.active());
  }

  @DeleteMapping("/roles/{id}")
  void deleteRole(@PathVariable long id, Authentication authentication) {
    Auth.coordinator(authentication);
    throw catalog.refuseRoleDelete(id);
  }

  @GetMapping("/skills")
  List<SkillView> skills(@RequestParam(defaultValue = "false") boolean active) {
    return catalog.skills(active);
  }

  @PostMapping("/skills")
  @ResponseStatus(HttpStatus.CREATED)
  SkillView createSkill(@Valid @RequestBody SkillCreate body, Authentication authentication) {
    Auth.coordinator(authentication);
    return catalog.createSkill(body.name(), body.roleDomainId());
  }

  @PatchMapping("/skills/{id}")
  SkillView patchSkill(
      @PathVariable long id,
      @Valid @RequestBody SkillPatch body,
      Authentication authentication) {
    Auth.coordinator(authentication);
    return catalog.updateSkill(id, body.name(), body.roleDomainId(), body.active());
  }

  @DeleteMapping("/skills/{id}")
  void deleteSkill(@PathVariable long id, Authentication authentication) {
    Auth.coordinator(authentication);
    throw catalog.refuseSkillDelete(id);
  }

  record RoleName(@NotBlank @Size(max = 40) String name) {}

  record RolePatch(@Size(max = 40) String name, Boolean active) {}

  record SkillCreate(@NotBlank @Size(max = 60) String name, @NotNull Long roleDomainId) {}

  record SkillPatch(@Size(max = 60) String name, Long roleDomainId, Boolean active) {}
}
