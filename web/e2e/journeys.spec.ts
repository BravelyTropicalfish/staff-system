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
 * product is bought for. Every step a screen owns is driven through it; the few rows a journey
 * only needs as setup — the imported person, the project a later step searches — are seeded
 * through the same route the screen calls, and say so where they happen. J3 then walks the
 * whole chain a project manager drives, screens only.
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
    await page.getByRole('button', { name: '上传并校验' }).click();
    await expect(
      page.getByText(`${employeeNo}.xlsx · 已应用 · 写入 人员 1 行 · 技能 1 行`),
    ).toBeVisible();

    // 2. The project and its post are this journey's setup, not its subject.
    const api = await coordinator();
    const lead = await leadOne();
    const project = await createProject(api, { name: unique('J1 project') });
    const post = await createPost(lead, project.id, devPost());

    // 3. The search reads the imported person as suitable.
    await page.goto(`/posts/${post.id}/candidates`);
    const suitable = page
      .locator('section')
      .filter({ has: page.getByRole('heading', { name: '合适清单' }) });
    await expect(suitable.getByText(name)).toBeVisible();

    // 4. The coordinator recommends her from that row, on the screen.
    const row = suitable.getByRole('row').filter({ hasText: name });
    await row.getByRole('button', { name: '推荐' }).click();
    await expect(
      page.getByText(`${name} 已推荐给项目经理`),
    ).toBeVisible();
    await expect(row.getByRole('button', { name: '已推荐' })).toBeDisabled();

    // 4b. The mark survives a reload: it is read from the server, not kept in this tab.
    await page.reload();
    await expect(row.getByRole('button', { name: '已推荐' })).toBeDisabled();
    await api.dispose();
    await lead.dispose();

    // 5. The lead confirms on the post screen.
    const leadContext = await browser.newContext({ storageState: LEAD_ONE, baseURL: APP });
    const leadPage = await leadContext.newPage();
    await leadPage.goto(`/posts/${post.id}`);
    await expect(leadPage.getByText(`${name}（${employeeNo}）`)).toBeVisible();
    await leadPage.getByRole('button', { name: '确认' }).click();
    await expect(leadPage.getByText('1 / 1 已到位 · 已满')).toBeVisible();
    await expect(leadPage.getByRole('button', { name: '确认' })).toHaveCount(0);

    // 6. The gap reads the same on the project screen, where the work is planned.
    await leadPage.goto(`/projects/${project.id}`);
    await expect(leadPage.getByText('1 个岗位 · 1 / 1 已到位 · 已满')).toBeVisible();
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
    await expect(page.getByText('1 个岗位 · 1 / 1 已到位 · 已满')).toBeVisible();
    await page.getByRole('button', { name: '结束项目' }).click();
    await page.getByRole('button', { name: '确认结束' }).click();
    await expect(
      page.getByText('已结束 · 岗位全部取消，本项目的占用全部删除'),
    ).toBeVisible();

    // 2. The coordinator searches again: the same person now passes on a later post.
    const coordinatorContext = await browser.newContext({ storageState: COORDINATOR, baseURL: APP });
    const coordinatorPage = await coordinatorContext.newPage();
    await coordinatorPage.goto(`/posts/${laterPost.id}/candidates`);
    const suitable = coordinatorPage
      .locator('section')
      .filter({ has: coordinatorPage.getByRole('heading', { name: '合适清单' }) });
    await expect(suitable.getByText(person.name)).toBeVisible();
    await coordinatorContext.close();
  });
});

test.describe('J3 the lead opens a project, the coordinator recommends', () => {
  test.use({ storageState: LEAD_ONE });

  test('J3 opens a project, states its post, is recommended a person and confirms her', async ({ page, browser }) => {
    const api = await coordinator();
    const person = await seedPerson(api);
    await api.dispose();

    // 1. The lead opens a project of their own: the lead field is their account and locked.
    await page.goto('/posts');
    await page.getByRole('button', { name: '新建项目' }).click();
    const projectName = unique('J3 project');
    await page.getByLabel('项目名称').fill(projectName);
    await expect(page.getByLabel('项目经理账号')).toHaveValue('lead.one');
    await expect(page.getByLabel('项目经理账号')).toBeDisabled();
    await page.getByLabel('开始日期').fill('2027-01-01');
    await page.getByLabel('结束日期').fill('2027-12-31');
    await page.getByRole('button', { name: '创建项目' }).click();

    // 2. It opens on its own screen, where the lead states the post the project is short of.
    await expect(page.getByRole('heading', { level: 1, name: projectName })).toBeVisible();
    await page.getByRole('link', { name: '新增岗位' }).click();
    await page.getByLabel('角色').selectOption('DEV');
    await page.getByLabel('最低职级').selectOption('Senior');
    await page.getByLabel('开始日期').fill('2027-02-01');
    await page.getByLabel('结束日期').fill('2027-06-30');
    await page.getByRole('button', { name: '创建岗位' }).click();
    await expect(page.getByRole('heading', { level: 1, name: /^岗位 \d+ ·/ })).toBeVisible();
    const postId = new URL(page.url()).pathname.split('/').pop() ?? '';

    // 3. The coordinator reads the pool and recommends the person who passes every gate.
    const coordinatorContext = await browser.newContext({ storageState: COORDINATOR, baseURL: APP });
    const coordinatorPage = await coordinatorContext.newPage();
    await coordinatorPage.goto(`/posts/${postId}/candidates`);
    const suitable = coordinatorPage
      .locator('section')
      .filter({ has: coordinatorPage.getByRole('heading', { name: '合适清单' }) });
    const row = suitable.getByRole('row').filter({ hasText: person.name });
    await expect(row).toBeVisible();
    await row.getByRole('button', { name: '推荐' }).click();
    await expect(
      coordinatorPage.getByText(`${person.name} 已推荐给项目经理`),
    ).toBeVisible();
    await coordinatorContext.close();

    // 4. The decision is the project lead's, on the post: she confirms and the seat is filled.
    await page.goto(`/posts/${postId}`);
    await expect(page.getByText(`${person.name}（${person.employeeNo}）`)).toBeVisible();
    await page.getByRole('button', { name: '确认' }).click();
    await expect(page.getByText('1 / 1 已到位 · 已满')).toBeVisible();

    // 5. The same numbers read on the board the lead opened the project on.
    await page.goto('/posts');
    const boardRow = page
      .locator('table')
      .getByRole('row')
      .filter({ hasText: projectName })
      .filter({ hasText: 'filled' })
      .first();
    await boardRow.click();
    await expect(page.getByText('1 / 1 已到位 · 已满')).toBeVisible();
  });
});
