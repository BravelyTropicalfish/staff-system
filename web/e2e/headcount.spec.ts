import { expect, test } from '@playwright/test';
import { COORDINATOR, LEAD_ONE } from './support/env';
import { contextAs, importWorkbook, unique } from './support/fixtures';

/**
 * T06–T08, the chart section on the people pool screen.
 *
 * <p>The suite runs with one worker and this file sorts first, so the pool still holds the seed's
 * nobody when the empty check runs; that check asserts its own precondition rather than trusting the
 * order silently. Every later check reads the page against the table beside it, or against a delta
 * the same test caused, so a pool another spec has grown cannot make an assertion meaningless.
 */

async function report() {
  const context = await contextAs(COORDINATOR);
  try {
    const response = await context.get('/api/headcount');
    expect(response.status()).toBe(200);
    return (await response.json()) as { total: number };
  } finally {
    await context.dispose();
  }
}

test.describe('the coordinator', () => {
  test.use({ storageState: COORDINATOR });

  test('an empty pool replaces both charts instead of drawing bars', async ({ page }) => {
    const before = await report();
    expect(
      before.total,
      'this check needs the seed pool: it must run before any spec imports people',
    ).toBe(0);

    await page.goto('/people');

    await expect(page.getByRole('heading', { name: 'Headcount by Role' })).toBeVisible();
    await expect(page.getByRole('heading', { name: 'Headcount by Grade' })).toBeVisible();
    await expect(page.locator('.chart-empty')).toHaveCount(2);
    await expect(page.locator('.chart-grid .card-head .total')).toHaveText(['Total 0', 'Total 0']);
    await expect(page.locator('.bar')).toHaveCount(0);
  });

  test('both charts sit above the table and count only the active people', async ({ page }) => {
    const tag = unique('HC');
    const context = await contextAs(COORDINATOR);
    try {
      await importWorkbook(
        context,
        [
          { employeeNo: `${tag}-A`, name: 'Chart Active One', role: 'PM', jobLevel: 'Senior', travel: 'none' },
          { employeeNo: `${tag}-B`, name: 'Chart Active Two', role: 'PM', jobLevel: 'Junior', travel: 'none' },
          {
            employeeNo: `${tag}-C`,
            name: 'Chart Inactive',
            role: 'DEV',
            jobLevel: 'Lead',
            travel: 'none',
            active: 'inactive',
          },
        ],
        [],
      );
    } finally {
      await context.dispose();
    }

    await page.goto('/people');

    // S8: both charts are on this one screen, above the table, and no other screen carries them.
    await expect(page.getByRole('heading', { name: 'Headcount by Role' })).toBeVisible();
    await expect(page.getByRole('heading', { name: 'Headcount by Grade' })).toBeVisible();
    await expect(page.locator('.chart-grid')).toHaveCount(1);
    await expect(page.locator('.chart-grid table')).toHaveCount(0);

    // S12: the table lists every row, the charts count only the active ones — and both numbers are
    // read from the same page, so they cannot drift apart.
    const rows = page.locator('section.card table tbody tr');
    const statuses = await rows.locator('td:last-child').allTextContents();
    const active = statuses.filter((text) => text.includes('在职')).length;
    expect(
      statuses.some((text) => text.includes('已停用')),
      'the pool holds an inactive person',
    ).toBe(true);

    await expect(page.locator('.chart-grid .card-head .total')).toHaveText([
      `Total ${active}`,
      `Total ${active}`,
    ]);

    // The zero bar keeps its place: nobody holds UX, and the category is still on the axis.
    const ux = page.locator('.label-cell', { hasText: 'UX' });
    await expect(ux).toHaveClass(/is-zero/);
    await expect(ux).toContainText('0.0%');
  });

  test('an import shows up in the charts on the next load', async ({ page }) => {
    const before = await report();
    const tag = unique('HCJ');
    const context = await contextAs(COORDINATOR);
    try {
      await importWorkbook(
        context,
        [
          {
            employeeNo: `${tag}-D`,
            name: 'Chart Fresh',
            role: 'QA',
            jobLevel: 'Principal',
            travel: 'none',
          },
        ],
        [],
      );
    } finally {
      await context.dispose();
    }

    await page.goto('/people');

    // Freshness: the report is derived per read, so one more active person moves both totals.
    await expect(page.locator('.chart-grid .card-head .total')).toHaveText([
      `Total ${before.total + 1}`,
      `Total ${before.total + 1}`,
    ]);
    await expect(page.locator('.label-cell', { hasText: 'QA' })).toContainText('%');
  });
});

test.describe('a lead', () => {
  test.use({ storageState: LEAD_ONE });

  test('sees no chart section and puts no report request on the wire', async ({ page }) => {
    const requested: string[] = [];
    page.on('request', (request) => {
      if (request.url().includes('/api/headcount')) {
        requested.push(request.url());
      }
    });

    await page.goto('/people');

    await expect(page.locator('.banner.danger')).toBeVisible();
    await expect(page.getByRole('heading', { name: 'Headcount by Role' })).toHaveCount(0);
    await expect(page.locator('.chart-grid')).toHaveCount(0);
    expect(requested, 'a lead’s page never asks for the report').toEqual([]);
  });
});
