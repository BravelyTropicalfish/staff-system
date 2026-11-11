import assert from 'node:assert/strict';
import test from 'node:test';
import {
  canEditCatalogs,
  domainLabel,
  renameBody,
  toggleBody,
  toggleLabel,
} from '../src/lib/catalog.ts';
import type { Role, Skill } from '../src/lib/catalog.ts';

/**
 * T07, the catalog screen's own readings. The server decides what a rename or a deactivation may
 * do; the screen only names an item, offers the one toggle its state allows, and sends the trimmed
 * name alone so a refusal leaves the row as it was.
 */

const skill = (overrides: Partial<Skill> = {}): Skill => ({
  id: 3,
  name: '测试',
  roleDomainId: 1,
  roleDomainName: 'QA',
  active: true,
  ...overrides,
});

const qa: Role = {
  id: 1,
  name: 'QA',
  active: true,
  skills: [skill(), skill({ id: 4, name: '后端开发' })],
};

test('a role reads its domain as the skill names, and an empty one says so', () => {
  assert.equal(domainLabel(qa), '测试, 后端开发');
  assert.equal(domainLabel({ skills: [] }), 'no domain skills');
});

test('the toggle offers the state the item is not in', () => {
  assert.equal(toggleLabel({ active: true }), 'Deactivate');
  assert.equal(toggleLabel({ active: false }), 'Activate');
  assert.deepEqual(toggleBody({ active: true }), { active: false });
  assert.deepEqual(toggleBody({ active: false }), { active: true });
});

test('a rename sends the trimmed name and nothing else', () => {
  const body = renameBody('  QA lead  ');
  assert.deepEqual(body, { name: 'QA lead' });
  assert.deepEqual(Object.keys(body), ['name'], 'the state is never sent with a rename');
});

test('only the coordinator may edit a catalog', () => {
  assert.equal(canEditCatalogs('COORDINATOR'), true);
  assert.equal(canEditCatalogs('LEAD'), false);
});
