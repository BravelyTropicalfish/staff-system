import assert from 'node:assert/strict';
import test from 'node:test';
import { activeLink, breadcrumb, initial, NAV, roleWord, visibleNav } from '../src/lib/nav.ts';

/**
 * The chrome's rules: a role sees the links it can actually use, a detail screen keeps its parent
 * lit, and the breadcrumb names the area the prototype put the screen in.
 */

test('a lead sees the two delivery links, the coordinator sees the six', () => {
  const lead = visibleNav(NAV, 'LEAD').flatMap((group) => group.items.map((item) => item.to));
  assert.deepEqual(lead, ['/posts', '/recommendations']);

  const coordinator = visibleNav(NAV, 'COORDINATOR').flatMap((group) =>
    group.items.map((item) => item.to),
  );
  assert.deepEqual(coordinator, [
    '/posts',
    '/recommendations',
    '/candidates',
    '/people',
    '/imports',
    '/catalogs',
  ]);
});

test('the prototype keeps its three groups and drops the notices destination', () => {
  assert.deepEqual(
    NAV.map((group) => group.group),
    ['项目交付', '人员', '数据'],
  );
  const labels = NAV.flatMap((group) => group.items.map((item) => item.label));
  assert.ok(labels.includes('岗位与缺口'));
  assert.ok(labels.includes('推荐与确认'));
  assert.ok(labels.includes('候选人搜索'));
  assert.ok(!labels.includes('作废通知'));
});

test('only the decisions link carries the count badge', () => {
  const badges = NAV.flatMap((group) =>
    group.items.filter((item) => item.badge).map((item) => item.to),
  );
  assert.deepEqual(badges, ['/recommendations']);
});

test('a signed-out visitor sees the whole map instead of an empty sidebar', () => {
  const signedOut = visibleNav(NAV, null).flatMap((group) => group.items.map((item) => item.to));
  assert.equal(signedOut.length, 6);
});

test('a detail screen keeps its parent link lit', () => {
  assert.equal(activeLink('/posts'), '/posts');
  assert.equal(activeLink('/posts/1'), '/posts');
  assert.equal(activeLink('/projects/1/posts/new'), '/posts');
  assert.equal(activeLink('/posts/1/candidates'), '/candidates');
  assert.equal(activeLink('/candidates'), '/candidates');
  assert.equal(activeLink('/recommendations'), '/recommendations');
  assert.equal(activeLink('/people/E1001'), '/people');
  assert.equal(activeLink('/login'), null);
});

test('the breadcrumb names the area of each route', () => {
  assert.deepEqual(breadcrumb('/posts'), { section: '项目交付', page: '岗位与缺口' });
  assert.deepEqual(breadcrumb('/recommendations'), {
    section: '项目交付',
    page: '推荐与确认',
  });
  assert.deepEqual(breadcrumb('/candidates'), {
    section: '项目交付',
    page: '候选人搜索',
  });
  assert.deepEqual(breadcrumb('/people'), { section: '人员池', page: '全部人员' });
  assert.deepEqual(breadcrumb('/people/E1001'), { section: '人员池', page: '人员档案' });
  assert.deepEqual(breadcrumb('/projects/1'), { section: '项目交付', page: '项目' });
  assert.deepEqual(breadcrumb('/posts/2'), { section: '项目交付', page: '岗位' });
  assert.deepEqual(breadcrumb('/posts/2/candidates'), {
    section: '项目交付',
    page: '候选人搜索',
  });
  assert.deepEqual(breadcrumb('/imports'), { section: '数据', page: '报表导入' });
});

test('the account chip names the role and draws one letter', () => {
  assert.equal(roleWord('COORDINATOR'), '资源协调员');
  assert.equal(roleWord('LEAD'), '项目经理');
  assert.equal(initial('资源协调员'), '资');
  assert.equal(initial('  resource coordinator'), 'R');
});
