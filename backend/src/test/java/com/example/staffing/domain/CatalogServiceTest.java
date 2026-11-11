package com.example.staffing.domain;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import com.example.staffing.data.RoleRepository;
import com.example.staffing.data.SkillRepository;
import org.junit.jupiter.api.Test;

/**
 * The name rule every catalog write shares. The route caps the length and refuses a blank name, so
 * these branches answer a caller inside the application; the service still refuses rather than
 * storing a name that no screen could show.
 */
class CatalogServiceTest {

  private final CatalogService service =
      new CatalogService(mock(RoleRepository.class), mock(SkillRepository.class));

  @Test
  void aRoleNameIsOneToFortyCharacters() {
    assertRefused(() -> service.createRole(null), "角色名称必须为 1 到 40 个字符。");
    assertRefused(() -> service.createRole("x".repeat(41)), "角色名称必须为 1 到 40 个字符。");
    assertRefused(() -> service.createRole("   "), "角色名称必须为 1 到 40 个字符。");
  }

  @Test
  void aSkillNameIsOneToSixtyCharacters() {
    assertRefused(() -> service.createSkill(null, 1), "技能名称必须为 1 到 60 个字符。");
    assertRefused(
        () -> service.createSkill("x".repeat(61), 1), "技能名称必须为 1 到 60 个字符。");
  }

  private static void assertRefused(Runnable call, String message) {
    assertThatThrownBy(call::run)
        .isInstanceOf(CatalogException.class)
        .hasMessage(message);
  }
}
