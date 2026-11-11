import { expect, test } from '@playwright/test';
import { LEAD_ONE } from './support/env';

/**
 * The sign-in screen. It was added after G2 because the application had no door: REQ-001 §2 calls
 * sign-in "a lightweight seeded account with a role", and until this screen existed the only way in
 * was posting `POST /api/session` by hand. The second spec is the loading-state fix — a refused read
 * used to print its reason and keep saying it was still reading.
 */

test.describe('signed out', () => {
  test.use({ storageState: { cookies: [], origins: [] } });

  test('a screen with no session says so and the form opens it', async ({ page }) => {
    await page.goto('/people');
    await expect(page.getByText('unauthenticated: Sign in to continue.')).toBeVisible();

    await page.goto('/login');
    await page.getByLabel('Account').fill('coordinator');
    await page.getByLabel('Password').fill('staffing-demo');
    await page.getByRole('button', { name: 'Sign in' }).click();

    await expect(page.getByRole('heading', { name: 'Staffing' })).toBeVisible();

    await page.goto('/people');
    await expect(page.getByRole('heading', { name: 'People' })).toBeVisible();
    await expect(page.getByText('unauthenticated: Sign in to continue.')).toHaveCount(0);
  });

  test('an empty form is refused without a request', async ({ page }) => {
    await page.goto('/login');
    await page.getByRole('button', { name: 'Sign in' }).click();

    await expect(page.getByText('Enter the account name.')).toBeVisible();
  });
});

test.describe('a signed-in lead', () => {
  test.use({ storageState: LEAD_ONE });

  test('a forbidden screen keeps its reason and stops claiming to read', async ({ page }) => {
    await page.goto('/people');

    await expect(page.getByText('forbidden: This action is not yours to take.')).toBeVisible();
    await expect(page.getByText('Reading the pool…')).toHaveCount(0);
  });
});
