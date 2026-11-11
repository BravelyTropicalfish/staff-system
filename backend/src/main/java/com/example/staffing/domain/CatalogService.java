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
 * rewriting what already refers to it. An item nobody points at may be deleted; a referenced one
 * is refused with `referenced`, since deleting it would strand the work that names it.
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
      throw new CatalogException("duplicate name", "角色 " + clean + " 已存在。");
    }
    return role(roles.insert(clean));
  }

  public RoleView renameRole(long id, String name) {
    RoleRow role = roles.findById(id).orElseThrow(() -> missing("role"));
    String clean = requireName(name, ROLE_NAME_MAX, "role");

    if (roles.countReferences(id) > 0) {
      throw new CatalogException(
          "referenced",
          "角色 " + role.name() + " 已被人员或岗位使用；请改为停用，而不是重命名。");
    }
    if (!role.name().equals(clean)) {
      if (roles.existsByName(clean)) {
        throw new CatalogException("duplicate name", "角色 " + clean + " 已存在。");
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

  /** An unreferenced role is deleted; a referenced one, or one that holds skills, is refused. */
  public RoleView deleteRole(long id) {
    RoleView view = role(id);
    if (roles.countReferences(id) > 0) {
      throw new CatalogException(
          "referenced",
          "角色 " + view.name() + " 已被人员或岗位使用；请改为停用，而不是删除。");
    }
    long domainSkills = skills.findAll().stream().filter(skill -> skill.roleDomainId() == id).count();
    if (domainSkills > 0) {
      throw new CatalogException(
          "referenced",
          "角色 " + view.name() + " 是 " + domainSkills + " 个技能的技能域；请先移走或删除这些技能。");
    }
    roles.delete(id);
    return view;
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
      throw new CatalogException("duplicate name", "技能 " + clean + " 已存在。");
    }
    return skill(skills.insert(clean, roleDomainId));
  }

  public SkillView renameSkill(long id, String name) {
    SkillRow skill = skills.findById(id).orElseThrow(() -> missing("skill"));
    String clean = requireName(name, SKILL_NAME_MAX, "skill");

    if (skills.countReferences(id) > 0) {
      throw new CatalogException(
          "referenced",
          "技能 " + skill.name() + " 已被人员或岗位使用；请改为停用，而不是重命名。");
    }
    if (!skill.name().equals(clean)) {
      if (skills.existsByName(clean)) {
        throw new CatalogException("duplicate name", "技能 " + clean + " 已存在。");
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

  /** An unreferenced skill is deleted; a referenced one is refused with `referenced`. */
  public SkillView deleteSkill(long id) {
    SkillView view = skill(id);
    if (skills.countReferences(id) > 0) {
      throw new CatalogException(
          "referenced",
          "技能 " + view.name() + " 已被人员或岗位使用；请改为停用，而不是删除。");
    }
    skills.delete(id);
    return view;
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
          "invalid name", kindWord(kind) + "名称必须为 1 到 " + max + " 个字符。");
    }
    return clean;
  }

  private static CatalogException missing(String kind) {
    return new CatalogException("not found", "找不到该" + kindWord(kind) + "。");
  }

  private static CatalogException unknownDomain(long roleDomainId) {
    return new CatalogException(
        "invalid role domain", "没有角色 " + roleDomainId + " 可以承载这个技能。");
  }

  private static String kindWord(String kind) {
    return "role".equals(kind) ? "角色" : "技能";
  }
}
