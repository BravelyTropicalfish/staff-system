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

/**
 * T17, the project screens' own readings. The gap numbers are sums of the rows the server sent, the
 * end is offered to the project's own lead alone while the project is open, and the confirmation
 * says the whole consequence — how many posts and how many confirmed people — before it is sent.
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
  assert.equal(projectLine(atlas), 'Project lead one · 2027-01-01 → 2027-12-31 · open');
  assert.equal(projectLine({ ...atlas, status: 'CLOSED' }), 'Project lead one · 2027-01-01 → 2027-12-31 · closed');
  assert.equal(projectState({ status: 'CLOSED' }), 'closed');
});

test('the note names what the state means for the posts and the allocations', () => {
  assert.equal(projectNote(atlas), 'open · the posts below are the live requirement');
  assert.equal(
    projectNote({ status: 'CLOSED' }),
    'closed · every post is cancelled and every allocation of this project is gone',
  );
});

test('the gap numbers of a project are sums of its rows', () => {
  assert.deepEqual(projectGap(seats), { posts: 2, seats: 5, chosen: 3, remaining: 2 });
  assert.deepEqual(projectGap([]), { posts: 0, seats: 0, chosen: 0, remaining: 0 });
});

test('the project sentence carries counts, a singular post and the filled case', () => {
  assert.equal(gapSummary(seats), '2 posts · 3 of 5 seats chosen · 2 seats still needed');
  assert.equal(gapSummary([seats[1]]), '1 post · 1 of 3 seats chosen · 2 seats still needed');
  assert.equal(gapSummary([seats[0]]), '1 post · 2 of 2 seats chosen · filled');
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
    'Ending Atlas cancels its 2 posts and releases 3 confirmed people. It cannot be undone.',
  );
  assert.equal(
    endConfirmation(atlas, [seats[0]]),
    'Ending Atlas cancels its 1 post and releases 2 confirmed people. It cannot be undone.',
  );
  assert.ok(endConfirmation(atlas, seats).includes('cannot be undone'));
});
