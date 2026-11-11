import { expect, test } from '@playwright/test';
import { APP, COORDINATOR, LEAD_ONE } from './support/env';
import {
  confirm,
  coordinator,
  createPost,
  createProject,
  devPost,
  leadOne,
  propose,
  seedPerson,
  unique,
} from './support/fixtures';
import { workbook } from './support/workbook';

/**
 * T27, the two journeys. They add no scenario: they stitch the eight into the two paths the
 * product is bought for. The steps with a screen are driven through it; a step whose screen does
 * not exist yet — creating a project, proposing a person — is seeded through the same route the
 * screen would call, and says so where it happens.
 */

const XLSX = 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet';

test.describe('J1 import to gap', () => {
  test.use({ storageState: COORDINATOR });

  test('J1 imports, posts, searches, proposes, confirms and reads the gap', async ({ page, browser }) => {
    const tag = `${Date.now().toString(36)}${Math.random().toString(36).slice(2, 6)}`;
    const employeeNo = `J1-${tag}`;
    const name = `J1 Person ${tag}`;

    // 1. The coordinator imports one person through the import screen.
    const file = await workbook(
      [{ employeeNo, name, role: 'DEV', jobLevel: 'Senior', travel: 'full' }],
      [{ employeeNo, skill: '后端开发', proficiency: 'Practitioner' }],
    );
    await page.goto('/imports');
    await page.locator('input[type=file]').setInputFiles({ name: `${employeeNo}.xlsx`, mimeType: XLSX, buffer: file });
    await page.getByRole('button', { name: 'Upload' }).click();
    await expect(
      page.getByText(`${employeeNo}.xlsx · applied · 1 person row · 1 skill row written`),
    ).toBeVisible();

    // 2. The project and its post: no screen owns creating either, so they are seeded.
    const api = await coordinator();
    const lead = await leadOne();
    const project = await createProject(api, { name: unique('J1 project') });
    const post = await createPost(lead, project.id, devPost());

    // 3. The search reads the imported person as suitable.
    await page.goto(`/posts/${post.id}/candidates`);
    const suitable = page
      .locator('section')
      .filter({ has: page.getByRole('heading', { name: 'Suitable people' }) });
    await expect(suitable.getByText(name)).toBeVisible();

    // 4. The proposal: also no screen, so the same route the coordinator's list would call.
    await propose(api, post.id, employeeNo);
    await api.dispose();
    await lead.dispose();

    // 5. The lead confirms on the post screen.
    const leadContext = await browser.newContext({ storageState: LEAD_ONE, baseURL: APP });
    const leadPage = await leadContext.newPage();
    await leadPage.goto(`/posts/${post.id}`);
    await expect(leadPage.getByText(`${name} (${employeeNo})`)).toBeVisible();
    await leadPage.getByRole('button', { name: 'Confirm' }).click();
    await expect(leadPage.getByText('1 of 1 chosen · filled')).toBeVisible();
    await expect(leadPage.getByRole('button', { name: 'Confirm' })).toHaveCount(0);

    // 6. The gap reads the same on the project screen, where the work is planned.
    await leadPage.goto(`/projects/${project.id}`);
    await expect(leadPage.getByText('1 post · 1 of 1 seats chosen · filled')).toBeVisible();
    await leadContext.close();
  });
});

test.describe('J2 the end and the later search', () => {
  test.use({ storageState: LEAD_ONE });

  test('J2 ends a project and the released people come back in a later search', async ({ page, browser }) => {
    const api = await coordinator();
    const person = await seedPerson(api);
    const lead = await leadOne();
    const first = await createProject(api, { name: unique('J2 first project') });
    const firstPost = await createPost(lead, first.id, devPost());
    const proposal = await propose(api, firstPost.id, person.employeeNo);
    await confirm(lead, proposal.id);
    const later = await createProject(api, { name: unique('J2 later project') });
    const laterPost = await createPost(
      lead,
      later.id,
      devPost({ startDate: '2027-07-01', endDate: '2027-12-31' }),
    );
    await lead.dispose();
    await api.dispose();

    // 1. The lead ends the first project on its own screen, twice.
    await page.goto(`/projects/${first.id}`);
    await expect(page.getByText('1 post · 1 of 1 seats chosen · filled')).toBeVisible();
    await page.getByRole('button', { name: 'End the project' }).click();
    await page.getByRole('button', { name: 'End it' }).click();
    await expect(
      page.getByText('closed · every post is cancelled and every allocation of this project is gone'),
    ).toBeVisible();

    // 2. The coordinator searches again: the same person now passes on a later post.
    const coordinatorContext = await browser.newContext({ storageState: COORDINATOR, baseURL: APP });
    const coordinatorPage = await coordinatorContext.newPage();
    await coordinatorPage.goto(`/posts/${laterPost.id}/candidates`);
    const suitable = coordinatorPage
      .locator('section')
      .filter({ has: coordinatorPage.getByRole('heading', { name: 'Suitable people' }) });
    await expect(suitable.getByText(person.name)).toBeVisible();
    await coordinatorContext.close();
  });
});
