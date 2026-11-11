package com.example.staffing.domain;

import com.example.staffing.data.RoleRepository;
import com.example.staffing.data.RoleRepository.RoleRow;
import com.example.staffing.data.SkillRepository;
import com.example.staffing.data.SkillRepository.SkillRow;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/**
 * The catalogs. A name is free to change until a person or a post points at the item; after that
 * only the `active` flag may move, because deactivating hides an item from new work without
 * rewriting what already refers to it. Nothing here deletes: the catalog is history.
 */
@Service
public class CatalogService {

  private static final int ROLE_NAME_MAX = 40;
  private static final int SKILL_NAME_MAX = 60;

  private final RoleRepository roles;
  private final SkillRepository skills;

  public CatalogService(RoleRepository roles, SkillRepository skills) {
    this.roles = roles;
    this.skills = skills;
  }

  public List<RoleView> roles() {
    Map<Long, List<SkillRow>> byDomain =
        skills.findAll().stream().collect(Collectors.groupingBy(SkillRow::roleDomainId));

    return roles.findAll().stream()
        .map(
            role ->
                new RoleView(
                    role.id(),
                    role.name(),
                    role.active(),
                    byDomain.getOrDefault(role.id(), List.of()).stream()
                        .map(CatalogService::toView)
                        .toList()))
        .toList();
  }

  /** `onlyActive` is what a picker asks for; the full list is what a catalog screen shows. */
  public List<SkillView> skills(boolean onlyActive) {
    return skills.findAll().stream()
        .filter(skill -> !onlyActive || skill.active())
        .map(CatalogService::toView)
        .toList();
  }

  public RoleView createRole(String name) {
    String clean = requireName(name, ROLE_NAME_MAX, "role");
    if (roles.existsByName(clean)) {
      throw new CatalogException("duplicate name", "A role named " + clean + " already exists.");
    }
    return role(roles.insert(clean));
  }

  public RoleView renameRole(long id, String name) {
    RoleRow role = roles.findById(id).orElseThrow(() -> missing("role"));
    String clean = requireName(name, ROLE_NAME_MAX, "role");

    if (roles.countReferences(id) > 0) {
      throw new CatalogException(
          "referenced",
          "Role " + role.name() + " is used by people or posts; deactivate it instead of renaming"
              + " it.");
    }
    if (!role.name().equals(clean)) {
      if (roles.existsByName(clean)) {
        throw new CatalogException("duplicate name", "A role named " + clean + " already exists.");
      }
      roles.rename(id, clean);
    }
    return role(id);
  }

  public RoleView setRoleActive(long id, boolean active) {
    roles.findById(id).orElseThrow(() -> missing("role"));
    roles.setActive(id, active);
    return role(id);
  }

  /** Never deletes: an unknown id is a 404, a known one the refusal the route throws. */
  public CatalogException refuseRoleDelete(long id) {
    roles.findById(id).orElseThrow(() -> missing("role"));
    return deleteRefused();
  }

  /** A partial edit: a null field is left as it is. A refused rename leaves the flag untouched. */
  public RoleView updateRole(long id, String name, Boolean active) {
    RoleView view = role(id);
    if (name != null) {
      view = renameRole(id, name);
    }
    if (active != null) {
      view = setRoleActive(id, active);
    }
    return view;
  }

  public SkillView createSkill(String name, long roleDomainId) {
    String clean = requireName(name, SKILL_NAME_MAX, "skill");
    roles.findById(roleDomainId).orElseThrow(() -> unknownDomain(roleDomainId));
    if (skills.existsByName(clean)) {
      throw new CatalogException("duplicate name", "A skill named " + clean + " already exists.");
    }
    return skill(skills.insert(clean, roleDomainId));
  }

  public SkillView renameSkill(long id, String name) {
    SkillRow skill = skills.findById(id).orElseThrow(() -> missing("skill"));
    String clean = requireName(name, SKILL_NAME_MAX, "skill");

    if (skills.countReferences(id) > 0) {
      throw new CatalogException(
          "referenced",
          "Skill " + skill.name() + " is used by people or posts; deactivate it instead of renaming"
              + " it.");
    }
    if (!skill.name().equals(clean)) {
      if (skills.existsByName(clean)) {
        throw new CatalogException("duplicate name", "A skill named " + clean + " already exists.");
      }
      skills.rename(id, clean);
    }
    return skill(id);
  }

  /** A domain change is allowed while the skill is referenced: it only moves future searches. */
  public SkillView moveSkillToDomain(long id, long roleDomainId) {
    skills.findById(id).orElseThrow(() -> missing("skill"));
    roles.findById(roleDomainId).orElseThrow(() -> unknownDomain(roleDomainId));
    skills.moveToDomain(id, roleDomainId);
    return skill(id);
  }

  public SkillView setSkillActive(long id, boolean active) {
    skills.findById(id).orElseThrow(() -> missing("skill"));
    skills.setActive(id, active);
    return skill(id);
  }

  /** Never deletes: an unknown id is a 404, a known one the refusal the route throws. */
  public CatalogException refuseSkillDelete(long id) {
    skills.findById(id).orElseThrow(() -> missing("skill"));
    return deleteRefused();
  }

  /** A partial edit: a null field is left as it is, including the domain. */
  public SkillView updateSkill(long id, String name, Long roleDomainId, Boolean active) {
    SkillView view = skill(id);
    if (name != null) {
      view = renameSkill(id, name);
    }
    if (roleDomainId != null) {
      view = moveSkillToDomain(id, roleDomainId);
    }
    if (active != null) {
      view = setSkillActive(id, active);
    }
    return view;
  }

  private RoleView role(long id) {
    RoleRow role = roles.findById(id).orElseThrow(() -> missing("role"));
    List<SkillView> domain =
        skills.findAll().stream()
            .filter(skill -> skill.roleDomainId() == id)
            .map(CatalogService::toView)
            .toList();
    return new RoleView(role.id(), role.name(), role.active(), domain);
  }

  private SkillView skill(long id) {
    return toView(skills.findById(id).orElseThrow(() -> missing("skill")));
  }

  private static SkillView toView(SkillRow skill) {
    return new SkillView(
        skill.id(), skill.name(), skill.roleDomainId(), skill.domainName(), skill.active());
  }

  private static String requireName(String raw, int max, String kind) {
    String clean = raw == null ? "" : raw.trim();
    if (clean.isEmpty() || clean.length() > max) {
      throw new CatalogException(
          "invalid name", "A " + kind + " name must be 1 to " + max + " characters.");
    }
    return clean;
  }

  private static CatalogException missing(String kind) {
    return new CatalogException("not found", "No such " + kind + ".");
  }

  private static CatalogException unknownDomain(long roleDomainId) {
    return new CatalogException(
        "invalid role domain", "No role " + roleDomainId + " can hold this skill.");
  }

  private static CatalogException deleteRefused() {
    return new CatalogException(
        "delete refused", "Catalog items are deactivated, never deleted; use the active flag.");
  }
}
