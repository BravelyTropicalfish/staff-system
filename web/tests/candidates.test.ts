import assert from 'node:assert/strict';
import test from 'node:test';
import { bandsAboveLabel, crossRole, gateChips, skillLabel } from '../src/lib/candidates.ts';
import type { CandidateSearch, FallbackRow, SuitableRow } from '../src/lib/candidates.ts';

/**
 * T21, the candidate screen's own readings of the search answer. The screen renders from these
 * functions, so pinning them pins what the page shows: the two sections, the gate results and the
 * qualifying skills per row, and the cross-role section appearing only on an empty main list.
 */

const ada: SuitableRow = {
  employeeNo: 'E100',
  name: 'Ada',
  role: 'QA',
  jobLevel: 'Senior',
  travel: 'full',
  gates: ['G1', 'G2', 'G3', 'G4', 'G5', 'G6'],
  skills: [
    { skill: '后端开发', kind: 'required', band: 'Practitioner', target: null },
    { skill: '测试', kind: 'bonus', band: 'Novice', target: 'Practitioner' },
  ],
};

const grace: FallbackRow = {
  employeeNo: 'E900',
  name: 'Grace',
  role: 'DEV',
  jobLevel: 'Principal',
  travel: 'full',
  bandsAbove: 2,
  skill: '测试',
  band: 'Practitioner',
  gates: ['G1', 'G5', 'G6', 'X1', 'X2'],
};

const linh: FallbackRow = {
  employeeNo: 'E800',
  name: 'Linh',
  role: 'BA',
  jobLevel: 'Lead',
  travel: 'full',
  bandsAbove: 1,
  skill: '测试',
  band: 'Expert',
  gates: ['G1', 'G5', 'G6', 'X1', 'X2'],
};

function search(overrides: Partial<CandidateSearch> = {}): CandidateSearch {
  return {
    suitable: [],
    excluded: [],
    exclusionCounts: {},
    fallback: [],
    fallbackReason: null,
    ...overrides,
  };
}

test('the cross-role section is shown only when the main list is empty', () => {
  const empty = crossRole(
    search({ fallback: [linh, grace], fallbackReason: 'no QA person passes the gates' }),
  );
  assert.equal(empty.shown, true);
  assert.deepEqual(
    empty.rows.map((row) => row.employeeNo),
    ['E800', 'E900'],
    'the server order is kept, never re-sorted',
  );
  assert.equal(empty.reason, 'no QA person passes the gates');

  const closed = crossRole(
    search({ suitable: [ada], fallback: [linh, grace], fallbackReason: 'should not be shown' }),
  );
  assert.equal(closed.shown, false, 'one same-role candidate closes the section');
  assert.deepEqual(closed.rows, []);
  assert.equal(closed.reason, null);
});

test('an empty cross-role section still explains itself', () => {
  const cross = crossRole(search({ fallbackReason: 'no QA person passes the gates' }));

  assert.equal(cross.shown, true);
  assert.deepEqual(cross.rows, []);
  assert.equal(cross.reason, 'no QA person passes the gates');
});

test('every gate is counted once, X1 and X2 included', () => {
  const chips = gateChips(search({ exclusionCounts: { G3: 1, X1: 2 } }));

  assert.deepEqual(
    chips.map((chip) => chip.code),
    ['G1', 'G2', 'G3', 'G4', 'G5', 'G6', 'X1', 'X2'],
  );
  assert.deepEqual(
    chips.map((chip) => chip.count),
    [0, 0, 1, 0, 0, 0, 2, 0],
    'a gate that did not run counts zero instead of disappearing',
  );
  assert.deepEqual(
    chips.map((chip) => chip.name),
    ['在职', '角色', '职级', '必备技能', '差旅', '占用', '高一档', '技能域'],
  );
});

test('a band is named, and a bonus row names the target it misses', () => {
  assert.equal(skillLabel(ada.skills[0]), '后端开发 · Practitioner');
  assert.equal(skillLabel(ada.skills[1]), '测试 · Novice（要求 Practitioner）');
  assert.equal(
    skillLabel({ skill: '测试', kind: 'required', band: null, target: null }),
    '测试 · 未持有',
  );
});

test('the gap is a count of bands', () => {
  assert.equal(bandsAboveLabel(1), '+1 档');
  assert.equal(bandsAboveLabel(2), '+2 档');
});
