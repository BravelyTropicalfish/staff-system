import assert from 'node:assert/strict';
import test from 'node:test';
import { activeLink, breadcrumb, initial, NAV, roleWord, visibleNav } from '../src/lib/nav.ts';

/**
 * The chrome's rules: a role sees the links it can actually use, a detail screen keeps its parent
 * lit, and the breadcrumb names the area the prototype put the screen in.
 */

test('a lead sees the delivery links only, the coordinator sees everything', () => {
  const lead = visibleNav(NAV, 'LEAD').flatMap((group) => group.items.map((item) => item.to));
  assert.deepEqual(lead, ['/projects']);

  const coordinator = visibleNav(NAV, 'COORDINATOR').flatMap((group) =>
    group.items.map((item) => item.to),
  );
  assert.deepEqual(coordinator, ['/projects', '/people', '/imports', '/catalogs', '/notices']);
});

test('a signed-out visitor sees the whole map instead of an empty sidebar', () => {
  const signedOut = visibleNav(NAV, null).flatMap((group) => group.items.map((item) => item.to));
  assert.equal(signedOut.length, 5);
});

test('a detail screen keeps its parent link lit', () => {
  assert.equal(activeLink('/posts/1/candidates'), '/projects');
  assert.equal(activeLink('/projects/1/posts/new'), '/projects');
  assert.equal(activeLink('/people/E1001'), '/people');
  assert.equal(activeLink('/login'), null);
});

test('the breadcrumb names the area of each route', () => {
  assert.deepEqual(breadcrumb('/'), { section: 'Workspace', page: 'Overview' });
  assert.deepEqual(breadcrumb('/people'), { section: 'People pool', page: 'Everyone' });
  assert.deepEqual(breadcrumb('/people/E1001'), { section: 'People pool', page: 'Profile' });
  assert.deepEqual(breadcrumb('/projects'), {
    section: 'Delivery',
    page: 'Projects and their posts',
  });
  assert.deepEqual(breadcrumb('/projects/1'), { section: 'Delivery', page: 'Project' });
  assert.deepEqual(breadcrumb('/posts/2/candidates'), {
    section: 'Delivery',
    page: 'Candidate search',
  });
  assert.deepEqual(breadcrumb('/imports'), { section: 'Data', page: 'Import the workbook' });
});

test('the account chip names the role and draws one letter', () => {
  assert.equal(roleWord('COORDINATOR'), 'Resource coordinator');
  assert.equal(roleWord('LEAD'), 'Project lead');
  assert.equal(initial('Resource coordinator'), 'R');
  assert.equal(initial('  resource coordinator'), 'R');
});
