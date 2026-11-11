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
    assertRefused(() -> service.createRole(null), "A role name must be 1 to 40 characters.");
    assertRefused(() -> service.createRole("x".repeat(41)), "A role name must be 1 to 40 characters.");
    assertRefused(() -> service.createRole("   "), "A role name must be 1 to 40 characters.");
  }

  @Test
  void aSkillNameIsOneToSixtyCharacters() {
    assertRefused(() -> service.createSkill(null, 1), "A skill name must be 1 to 60 characters.");
    assertRefused(
        () -> service.createSkill("x".repeat(61), 1), "A skill name must be 1 to 60 characters.");
  }

  private static void assertRefused(Runnable call, String message) {
    assertThatThrownBy(call::run)
        .isInstanceOf(CatalogException.class)
        .hasMessage(message);
  }
}
