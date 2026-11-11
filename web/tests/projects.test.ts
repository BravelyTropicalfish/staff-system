import assert from 'node:assert/strict';
import test from 'node:test';
import {
  canEnd,
  endConfirmation,
  gapSummary,
  projectGap,
  projectLine,
  projectNote,
  projectState,
} from '../src/lib/projects.ts';
import type { Project } from '../src/lib/projects.ts';
import {
  emptyProject,
  leadLocked,
  projectDraft,
  projectProblem,
} from '../src/lib/projectForm.ts';

/**
 * T17, the project screens' own readings. The gap numbers are sums of the rows the server sent, the
 * end is offered to the project's own lead alone while the project is open, and the confirmation
 * says the whole consequence — how many posts and how many confirmed people — before it is sent.
 * T33 adds the create form: a lead opens a project of their own, the coordinator names any lead.
 */

const atlas: Project = {
  id: 3,
  name: 'Atlas',
  lead: 'lead.one',
  leadName: 'Project lead one',
  status: 'OPEN',
  startDate: '2027-01-01',
  endDate: '2027-12-31',
};

const seats = [
  { headcount: 2, confirmed: 2, remaining: 0 },
  { headcount: 3, confirmed: 1, remaining: 2 },
];

test('a project reads as its lead, its inclusive range and its state', () => {
  assert.equal(projectLine(atlas), 'Project lead one · 2027-01-01 → 2027-12-31 · 进行中');
  assert.equal(
    projectLine({ ...atlas, status: 'CLOSED' }),
    'Project lead one · 2027-01-01 → 2027-12-31 · 已结束',
  );
  assert.equal(projectState({ status: 'CLOSED' }), '已结束');
});

test('the note names what the state means for the posts and the allocations', () => {
  assert.equal(projectNote(atlas), '进行中 · 下列岗位是当前需求');
  assert.equal(projectNote({ status: 'CLOSED' }), '已结束 · 岗位全部取消，本项目的占用全部删除');
});

test('the gap numbers of a project are sums of its rows', () => {
  assert.deepEqual(projectGap(seats), { posts: 2, seats: 5, chosen: 3, remaining: 2 });
  assert.deepEqual(projectGap([]), { posts: 0, seats: 0, chosen: 0, remaining: 0 });
});

test('the project sentence carries counts, a singular post and the filled case', () => {
  assert.equal(gapSummary(seats), '2 个岗位 · 3 / 5 已到位 · 还缺 2 人');
  assert.equal(gapSummary([seats[1]]), '1 个岗位 · 1 / 3 已到位 · 还缺 2 人');
  assert.equal(gapSummary([seats[0]]), '1 个岗位 · 2 / 2 已到位 · 已满');
});

test('only the project lead, and only while it is open, is offered the end', () => {
  assert.equal(canEnd(atlas, { username: 'lead.one', role: 'LEAD' }), true);
  assert.equal(canEnd(atlas, { username: 'lead.two', role: 'LEAD' }), false);
  assert.equal(canEnd(atlas, { username: 'coordinator', role: 'COORDINATOR' }), false);
  assert.equal(canEnd({ ...atlas, status: 'CLOSED' }, { username: 'lead.one', role: 'LEAD' }), false);
});

test('the confirmation names the project, its posts and the people it releases', () => {
  assert.equal(
    endConfirmation(atlas, seats),
    '结束 Atlas 会取消它的 2 个岗位，并释放 3 名已确认人员。不可撤销。',
  );
  assert.equal(
    endConfirmation(atlas, [seats[0]]),
    '结束 Atlas 会取消它的 1 个岗位，并释放 2 名已确认人员。不可撤销。',
  );
  assert.ok(endConfirmation(atlas, seats).includes('不可撤销'));
});

test('a lead opens a project of their own; the coordinator names the lead', () => {
  const lead = { username: 'lead.one', role: 'LEAD' };
  const coordinator = { username: 'coordinator', role: 'COORDINATOR' };

  assert.deepEqual(emptyProject(lead), {
    name: '',
    lead: 'lead.one',
    startDate: '',
    endDate: '',
  });
  assert.deepEqual(emptyProject(coordinator), {
    name: '',
    lead: '',
    startDate: '',
    endDate: '',
  });
  assert.equal(leadLocked(lead), true);
  assert.equal(leadLocked(coordinator), false);
});

test('the draft trims the two names and keeps the dates it was given', () => {
  assert.deepEqual(
    projectDraft({
      name: '  Atlas  ',
      lead: '  lead.one ',
      startDate: '2027-01-01',
      endDate: '2027-12-31',
    }),
    { name: 'Atlas', lead: 'lead.one', startDate: '2027-01-01', endDate: '2027-12-31' },
  );
});

test('the form names its own refusals before anything is sent', () => {
  const filled = {
    name: 'Atlas',
    lead: 'lead.one',
    startDate: '2027-01-01',
    endDate: '2027-12-31',
  };

  assert.equal(projectProblem(filled), null);
  assert.equal(projectProblem({ ...filled, name: '   ' }), '请填写项目名称。');
  assert.equal(projectProblem({ ...filled, lead: ' ' }), '请指定项目经理。');
  assert.equal(projectProblem({ ...filled, startDate: '' }), '请填写起止日期。');
  assert.equal(
    projectProblem({ ...filled, endDate: '2026-12-31' }),
    '结束日期不能早于开始日期。',
  );
});
