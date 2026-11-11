import { expect, test } from '@playwright/test';
import { COORDINATOR, LEAD_ONE } from './support/env';
import { contextAs } from './support/fixtures';

/**
 * T26, the smoke test: the two accounts sign in through the setup project and read back their own
 * role, and a signed-in lead opens the shell. If this fails, nothing else in the suite is worth
 * reading, because the identity or the two servers is what broke.
 */
test.describe('the harness', () => {
  test('each signed-in account reads back its own role', async () => {
    for (const [file, role] of [
      [COORDINATOR, 'COORDINATOR'],
      [LEAD_ONE, 'LEAD'],
    ] as const) {
      const context = await contextAs(file);
      const response = await context.get('/api/session');
      expect(response.status(), `session for ${role}`).toBe(200);
      expect((await response.json()).role).toBe(role);
      await context.dispose();
    }
  });
});

test.describe('the shell', () => {
  test.use({ storageState: LEAD_ONE });

  test('a signed-in lead opens it with the links to the screens', async ({ page }) => {
    await page.goto('/');

    await expect(page.getByRole('heading', { name: 'Staffing' })).toBeVisible();
    await expect(page.getByRole('link', { name: 'Projects and their posts' })).toBeVisible();
    await expect(page.getByRole('link', { name: 'The people pool' })).toBeVisible();
  });
});
