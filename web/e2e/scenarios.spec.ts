import { expect, test } from '@playwright/test';
import { APP, COORDINATOR, LEAD_ONE } from './support/env';
import {
  allocate,
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

/**
 * T27, the eight scenarios the ATDD design keeps for a browser. Each one proves something only a
 * rendered screen can: an absence (no field, no number), two accounts over one state, or the text
 * the user actually reads. The API tests remain the primary layer for every other scenario.
 */

test.describe('the project end', () => {
  test.use({ storageState: LEAD_ONE });

  test('The end releases everyone and cancels the posts', async ({ page, browser }) => {
    const api = await coordinator();
    const person = await seedPerson(api);
    const project = await createProject(api, { name: unique('Ended project') });
    const lead = await leadOne();
    const post = await createPost(lead, project.id, devPost({ title: unique('Ended post') }));
    const proposal = await propose(api, post.id, person.employeeNo);
    await confirm(lead, proposal.id);
    await lead.dispose();
    await api.dispose();

    await page.goto(`/projects/${project.id}`);
    await expect(page.getByText('1 个岗位 · 1 / 1 已到位 · 已满')).toBeVisible();

    await page.getByRole('button', { name: '结束项目' }).click();
    await expect(
      page.getByText(
        `结束 ${project.name} 会取消它的 1 个岗位，并释放 1 名已确认人员。不可撤销。`,
      ),
    ).toBeVisible();
    await page.getByRole('button', { name: '确认结束' }).click();

    await expect(
      page.getByText('已结束 · 岗位全部取消，本项目的占用全部删除'),
    ).toBeVisible();
    const row = page.getByRole('row').filter({ hasText: post.title ?? '' });
    await expect(row).toContainText('cancelled');
    await expect(page.getByRole('button', { name: '结束项目' })).toHaveCount(0);

    // The release is visible where the people are: the coordinator's profile of the same person.
    const coordinatorContext = await browser.newContext({ storageState: COORDINATOR, baseURL: APP });
    const coordinatorPage = await coordinatorContext.newPage();
    await coordinatorPage.goto(`/people/${person.employeeNo}`);
    await expect(coordinatorPage.getByText('这个人员没有记录任何占用。')).toBeVisible();
    await coordinatorContext.close();
  });
});

test.describe('the person profile', () => {
  test.use({ storageState: COORDINATOR });

  test('The profile is read-only', async ({ page }) => {
    const api = await coordinator();
    const person = await seedPerson(api);
    await api.dispose();

    await page.goto(`/people/${person.employeeNo}`);
    await expect(page.getByRole('heading', { name: person.name })).toBeVisible();
    await expect(
      page.getByText('主数据由报表导入，本页只读'),
    ).toBeVisible();
    await expect(page.locator('input, textarea, select, button')).toHaveCount(0);
  });

  test('A band is displayed, never a number', async ({ page }) => {
    const api = await coordinator();
    const person = await seedPerson(api);
    await api.dispose();

    await page.goto(`/people/${person.employeeNo}`);
    const skills = page.locator('section').filter({ has: page.getByRole('heading', { name: '技能' }) });
    await expect(skills).toContainText('后端开发 · Practitioner · DEV 域');
    expect(await skills.innerText(), 'a band is a name, never a digit').not.toMatch(/\d/);
  });

  test("Profile lists a person's occupancy", async ({ page }) => {
    const api = await coordinator();
    const person = await seedPerson(api);
    const project = await createProject(api, { name: unique('Occupied project') });
    await allocate(api, {
      personNo: person.employeeNo,
      projectId: project.id,
      startDate: '2026-01-05',
      endDate: '2026-03-27',
    });
    await allocate(api, {
      personNo: person.employeeNo,
      projectId: project.id,
      startDate: '2027-01-05',
      endDate: '2027-03-27',
    });
    await api.dispose();

    await page.goto(`/people/${person.employeeNo}`);
    await expect(
      page.getByText(`${project.name} · 2026-01-05 → 2026-03-27 · 手工登记 · 已结束`),
    ).toBeVisible();
    await expect(
      page.getByText(`${project.name} · 2027-01-05 → 2027-03-27 · 手工登记`, { exact: true }),
    ).toBeVisible();
  });
});

test.describe('the confirmation', () => {
  test.use({ storageState: LEAD_ONE });

  test('Confirmation is visible to both roles', async ({ page, browser }) => {
    const api = await coordinator();
    const person = await seedPerson(api);
    const project = await createProject(api, { name: unique('Both roles project') });
    const lead = await leadOne();
    const post = await createPost(lead, project.id, devPost());
    const proposal = await propose(api, post.id, person.employeeNo);
    await confirm(lead, proposal.id);
    await lead.dispose();
    await api.dispose();

    const seat = `${person.name}（${person.employeeNo}）· 确认于`;
    await page.goto(`/posts/${post.id}`);
    const gap = page.locator('section').filter({ has: page.getByRole('heading', { name: '缺口' }) });
    await expect(gap.getByText(seat, { exact: false })).toBeVisible();

    const coordinatorContext = await browser.newContext({ storageState: COORDINATOR, baseURL: APP });
    const coordinatorPage = await coordinatorContext.newPage();
    await coordinatorPage.goto(`/posts/${post.id}`);
    const coordinatorGap = coordinatorPage
      .locator('section')
      .filter({ has: coordinatorPage.getByRole('heading', { name: '缺口' }) });
    await expect(coordinatorGap.getByText(seat, { exact: false })).toBeVisible();
    await coordinatorContext.close();
  });

  test('Both roles see the same gap numbers', async ({ page, browser }) => {
    const api = await coordinator();
    const person = await seedPerson(api);
    const project = await createProject(api, { name: unique('Gap project') });
    const lead = await leadOne();
    const post = await createPost(lead, project.id, devPost({ headcount: 2 }));
    const proposal = await propose(api, post.id, person.employeeNo);
    await confirm(lead, proposal.id);
    await lead.dispose();
    await api.dispose();

    const gap = '1 / 2 已到位 · 还缺 1 人';
    await page.goto(`/posts/${post.id}`);
    await expect(page.getByText(gap)).toBeVisible();

    const coordinatorContext = await browser.newContext({ storageState: COORDINATOR, baseURL: APP });
    const coordinatorPage = await coordinatorContext.newPage();
    await coordinatorPage.goto(`/posts/${post.id}`);
    await expect(coordinatorPage.getByText(gap)).toBeVisible();
    await coordinatorContext.close();
  });
});

test.describe('the search', () => {
  test.use({ storageState: COORDINATOR });

  test('The confirmed person leaves other searches', async ({ page }) => {
    const api = await coordinator();
    const person = await seedPerson(api);
    const lead = await leadOne();
    const taken = await createProject(api, { name: unique('Taken project') });
    const takenPost = await createPost(lead, taken.id, devPost());
    const proposal = await propose(api, takenPost.id, person.employeeNo);
    await confirm(lead, proposal.id);
    const later = await createProject(api, { name: unique('Later project') });
    const laterPost = await createPost(lead, later.id, devPost({ startDate: '2027-03-01', endDate: '2027-09-30' }));
    await lead.dispose();
    await api.dispose();

    await page.goto(`/posts/${laterPost.id}/candidates`);

    const suitable = page
      .locator('section')
      .filter({ has: page.getByRole('heading', { name: '合适清单' }) });
    await expect(suitable.getByText(person.name)).toHaveCount(0);

    const excluded = page
      .locator('section')
      .filter({ has: page.getByRole('heading', { name: '被排除的人' }) });
    const row = excluded.getByRole('row').filter({ hasText: person.name });
    await expect(row).toBeVisible();
    await expect(row).toContainText('G6');
    await expect(row).toContainText('已有占用');
  });

  test('The notice list explains each loss', async ({ page }) => {
    const api = await coordinator();
    const person = await seedPerson(api);
    const lead = await leadOne();
    const first = await createProject(api, { name: unique('Notice project') });
    const kept = await createPost(lead, first.id, devPost({ title: unique('Keep this post') }));
    const second = await createProject(api, { name: unique('Lost project') });
    const lostTitle = unique('Lost this post');
    const lost = await createPost(lead, second.id, devPost({ title: lostTitle }));
    const keptProposal = await propose(api, kept.id, person.employeeNo);
    await propose(api, lost.id, person.employeeNo);
    await confirm(lead, keptProposal.id);
    await lead.dispose();
    await api.dispose();

    await page.goto('/notices');
    const entry = page.getByRole('listitem').filter({ hasText: lostTitle });
    await expect(entry).toContainText(`${person.name}（${person.employeeNo}）`);
    await expect(entry).toContainText('人员已被占用');
  });
});
