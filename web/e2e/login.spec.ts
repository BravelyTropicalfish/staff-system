import { expect, test } from '@playwright/test';
import { LEAD_ONE } from './support/env';

/**
 * The sign-in screen. It was added after G2 because the application had no door: REQ-001 §2 calls
 * sign-in "a lightweight seeded account with a role", and until this screen existed the only way in
 * was posting `POST /api/session` by hand. A screen reached without a session now goes straight to
 * the form, and every sign-in also carries a per-tab token, so two tabs can hold two accounts.
 */

test.describe('signed out', () => {
  test.use({ storageState: { cookies: [], origins: [] } });

  test('a screen with no session lands on the form, and the form opens it', async ({ page }) => {
    await page.goto('/people');
    await expect(page).toHaveURL(/\/login$/);
    await expect(page.getByRole('button', { name: '登录' })).toBeVisible();

    await page.getByLabel('账号').fill('coordinator');
    await page.getByLabel('密码').fill('staffing-demo');
    await page.getByRole('button', { name: '登录' }).click();

    // Signing in returns to the screen that sent the caller to the form.
    await expect(page.getByRole('heading', { name: '人员池' })).toBeVisible();
  });

  test('an empty form is refused without a request', async ({ page }) => {
    await page.goto('/login');
    await page.getByRole('button', { name: '登录' }).click();

    await expect(page.getByText('请输入账号。')).toBeVisible();
  });

  test('two tabs of one browser keep two accounts', async ({ browser }) => {
    const context = await browser.newContext();
    try {
      const first = await context.newPage();
      await first.goto('/login');
      await first.getByLabel('账号').fill('coordinator');
      await first.getByLabel('密码').fill('staffing-demo');
      await first.getByRole('button', { name: '登录' }).click();
      await expect(first.getByRole('heading', { name: '岗位与缺口' })).toBeVisible();

      // The second tab signs in as somebody else; the cookie every tab shares moves with it.
      const second = await context.newPage();
      await second.goto('/login');
      await second.getByLabel('账号').fill('lead.one');
      await second.getByLabel('密码').fill('staffing-demo');
      await second.getByRole('button', { name: '登录' }).click();
      await expect(second.getByRole('heading', { name: '我的岗位' })).toBeVisible();

      // The first tab still reads its own account, because its token outranks the shared cookie.
      await first.reload();
      await expect(first.getByRole('heading', { name: '岗位与缺口' })).toBeVisible();
      await expect(first.getByText('coordinator')).toBeVisible();
      await expect(second.getByText('lead.one')).toBeVisible();
    } finally {
      await context.close();
    }
  });
});

test.describe('a signed-in lead', () => {
  test.use({ storageState: LEAD_ONE });

  test('a forbidden screen keeps its reason and stops claiming to read', async ({ page }) => {
    await page.goto('/people');

    await expect(page.getByText('forbidden: 这个动作不属于你。')).toBeVisible();
    await expect(page.getByText('正在读取人员池…')).toHaveCount(0);
  });
});
