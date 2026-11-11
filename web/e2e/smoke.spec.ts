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

  test('a signed-in lead opens it with the links the role can use', async ({ page }) => {
    await page.goto('/');

    // The board is where the app lands, so the shell is read together with its first heading.
    await expect(page.getByRole('heading', { name: '我的岗位' })).toBeVisible();
    // The sidebar is the prototype's: three groups, filtered by the role the server named, so a
    // lead is offered the two delivery links and nothing a lead would be refused on.
    await expect(page.getByRole('link', { name: '岗位与缺口' })).toBeVisible();
    await expect(page.getByRole('link', { name: /推荐与确认/ })).toBeVisible();
    await expect(page.getByRole('link', { name: '人员池' })).toHaveCount(0);
    await expect(page.getByRole('link', { name: '候选人搜索' })).toHaveCount(0);
    await expect(page.getByRole('navigation').getByText('项目交付', { exact: true })).toBeVisible();
  });
});
