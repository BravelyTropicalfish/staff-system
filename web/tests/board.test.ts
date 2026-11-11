import assert from 'node:assert/strict';
import test from 'node:test';
import {
  boardNumbers,
  canCancelPost,
  canEditPost,
  initialPost,
  postName,
} from '../src/lib/board.ts';

/**
 * The board's own readings: the three tiles, the row a post is named by, the post that opens by
 * default and the two verbs its own lead may use. Every clause here repeats a server rule, so
 * pinning them pins exactly when a button is offered.
 */

const LEAD_ONE = { username: 'lead.one', role: 'LEAD' };
const COORDINATOR = { username: 'coordinator', role: 'COORDINATOR' };
const ATLAS = { lead: 'lead.one' };

test('the tiles count short posts, filled seats and proposals waiting on this account', () => {
  const gaps = [
    { stillShort: true, confirmed: 1 },
    { stillShort: true, confirmed: 0 },
    { stillShort: false, confirmed: 2 },
  ];
  const proposals = [{ state: 'proposed' }, { state: 'rejected' }, { state: 'voided' }];

  assert.deepEqual(boardNumbers(gaps, proposals), { short: 2, confirmed: 3, pending: 1 });
  assert.deepEqual(boardNumbers([], []), { short: 0, confirmed: 0, pending: 0 });
});

test('a post is named by its title, or by its number when the title is empty', () => {
  assert.equal(postName({ id: 12, title: 'Backend' }), 'Backend');
  assert.equal(postName({ id: 12, title: null }), '岗位 12');
});

test('the board opens the first post that still needs somebody', () => {
  const gaps = [
    { id: 3, stillShort: false },
    { id: 4, stillShort: true },
    { id: 5, stillShort: true },
  ];

  assert.equal(initialPost(gaps), 4);
  assert.equal(initialPost([{ id: 3, stillShort: false }]), 3, 'a filled board opens its first row');
  assert.equal(initialPost([]), null);
});

test('only the post\'s own lead edits or cancels it, and never a filled or cancelled one', () => {
  const open = { status: 'open', confirmed: 0 };

  assert.equal(canEditPost(open, ATLAS, LEAD_ONE), true);
  assert.equal(canCancelPost(open, ATLAS, LEAD_ONE), true);

  assert.equal(
    canCancelPost({ status: 'open', confirmed: 1 }, ATLAS, LEAD_ONE),
    false,
    'a confirmed person blocks the cancel',
  );
  assert.equal(canCancelPost({ status: 'cancelled', confirmed: 0 }, ATLAS, LEAD_ONE), false);
  assert.equal(canEditPost({ status: 'cancelled' }, ATLAS, LEAD_ONE), false);
  assert.equal(canCancelPost(open, ATLAS, COORDINATOR), false, 'the coordinator only reads');
  assert.equal(
    canCancelPost(open, ATLAS, { username: 'lead.two', role: 'LEAD' }),
    false,
    'another lead is refused on a project they do not own',
  );
  assert.equal(canCancelPost(open, null, LEAD_ONE), false);
  assert.equal(canCancelPost(open, ATLAS, null), false);
});
