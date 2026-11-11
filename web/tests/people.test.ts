import assert from 'node:assert/strict';
import test from 'node:test';
import {
  bandLabel,
  occupancyLabel,
  profileNote,
  stateLabel,
} from '../src/lib/people.ts';
import type { PersonAllocation, PersonSkill } from '../src/lib/people.ts';

/**
 * T12, the pool screens' own readings. The profile is read-only because person data only arrives by
 * import, a band is named and never numbered, and an occupancy keeps its inclusive range with the
 * ended mark when the end date has passed.
 */

const practitioner: PersonSkill = { skill: '测试', roleDomain: 'QA', band: 'Practitioner' };
const expert: PersonSkill = { skill: '测试', roleDomain: 'QA', band: 'Expert' };

const confirmed: PersonAllocation = {
  id: 7,
  project: 'Atlas',
  startDate: '2026-01-05',
  endDate: '2026-03-27',
  source: 'CONFIRMATION',
  ended: false,
};

const handRun: PersonAllocation = {
  id: 8,
  project: 'Borealis',
  startDate: '2025-09-01',
  endDate: '2025-12-31',
  source: 'MANUAL',
  ended: true,
};

test('the profile says it is read-only and names the import as the writer', () => {
  assert.equal(profileNote({ active: true }), '在职 · 主数据由报表导入，本页只读');
  assert.equal(profileNote({ active: false }), '已停用 · 由导入标记 · 主数据由报表导入，本页只读');
});

test('a band is displayed by name, never as a number', () => {
  assert.equal(bandLabel(practitioner), '测试 · Practitioner · QA 域');
  assert.equal(bandLabel(expert), '测试 · Expert · QA 域');
  assert.ok(!/\d/.test(bandLabel(practitioner)), 'a band carries no rank number');
});

test('an occupancy keeps its inclusive range and the way it came about', () => {
  assert.equal(occupancyLabel(confirmed), 'Atlas · 2026-01-05 → 2026-03-27 · 确认座位');
  assert.equal(occupancyLabel(handRun), 'Borealis · 2025-09-01 → 2025-12-31 · 手工登记 · 已结束');
});

test('a past range is listed and only marked, never dropped', () => {
  assert.ok(occupancyLabel(handRun).endsWith('· 已结束'));
  assert.ok(!occupancyLabel(confirmed).includes('已结束'));
});

test('the active flag reads as a word', () => {
  assert.equal(stateLabel({ active: true }), '在职');
  assert.equal(stateLabel({ active: false }), '已停用');
});
