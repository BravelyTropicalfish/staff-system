import assert from 'node:assert/strict';
import test from 'node:test';
import {
  INBOX_FILTERS,
  evidenceLines,
  gatesLabel,
  inboxCounts,
  inboxPage,
  inboxPost,
  moment,
  noticeLine,
  noticePost,
  proposalActions,
  proposalPerson,
  splitProposals,
  stateLabel,
} from '../src/lib/confirmation.ts';
import type { InboxProposal, Notice, Proposal, ProposalSnapshot } from '../src/lib/confirmation.ts';

/**
 * T25, the confirmation screens' own readings of a post's proposals and of the notice list. The
 * server stores the state, the reason snapshot and the notice; these functions only decide what is
 * waiting, what is history and which verb a signed-in role may be offered, so pinning them pins
 * what the two roles see.
 */

function proposal(overrides: Partial<Proposal> = {}): Proposal {
  return {
    id: 1,
    personNo: 'E100',
    personName: 'Ada',
    personActive: true,
    state: 'proposed',
    voidReason: null,
    snapshot: null,
    proposedBy: 'coordinator',
    proposedAt: '2026-11-11T09:05:00',
    ...overrides,
  };
}

const sameRole: ProposalSnapshot = {
  personNo: 'E100',
  role: 'QA',
  jobLevel: 'Senior',
  travel: 'full',
  gates: ['G1', 'G2', 'G3', 'G4', 'G5', 'G6'],
  skills: [{ skill: '测试', kind: 'required', band: 'Practitioner', target: null }],
  crossRole: null,
};

const crossRole: ProposalSnapshot = {
  personNo: 'E900',
  role: 'DEV',
  jobLevel: 'Lead',
  travel: 'full',
  gates: ['G1', 'G5', 'G6', 'X1', 'X2'],
  skills: [],
  crossRole: { bandsAbove: 1, skill: '测试', band: 'Practitioner' },
};

test('a state is named, and a voided one names its reason', () => {
  assert.equal(stateLabel(proposal()), '待决策');
  assert.equal(stateLabel(proposal({ state: 'confirmed' })), '已确认');
  assert.equal(
    stateLabel(proposal({ state: 'voided', voidReason: 'person taken' })),
    '已作废 · 人员已被占用',
  );
  assert.equal(stateLabel(proposal({ state: 'voided' })), '已作废');
});

test('the open proposals come first and the closed history follows, each in the server order', () => {
  const rows = [
    proposal({ id: 3, personNo: 'E300', state: 'proposed' }),
    proposal({ id: 2, personNo: 'E200', state: 'voided', voidReason: 'post changed' }),
    proposal({ id: 1, personNo: 'E100', state: 'proposed' }),
    proposal({ id: 4, personNo: 'E400', state: 'rejected' }),
  ];

  const split = splitProposals(rows);

  assert.deepEqual(
    split.open.map((row) => row.id),
    [3, 1],
    'the open ones keep the order the server answered',
  );
  assert.deepEqual(
    split.closed.map((row) => row.id),
    [2, 4],
  );
  assert.deepEqual(splitProposals([]), { open: [], closed: [] });
});

test('the lead confirms and rejects, the coordinator withdraws, and a closed row offers nothing', () => {
  assert.deepEqual(proposalActions('LEAD', 'proposed'), ['confirm', 'reject']);
  assert.deepEqual(proposalActions('COORDINATOR', 'proposed'), ['withdraw']);
  assert.deepEqual(proposalActions('LEAD', 'confirmed'), []);
  assert.deepEqual(proposalActions('COORDINATOR', 'voided'), []);
});

test('a proposal names the person and marks a deactivation', () => {
  assert.equal(proposalPerson(proposal()), 'Ada（E100）');
  assert.equal(proposalPerson(proposal({ personActive: false })), 'Ada（E100） · 已停用');
});

test('the reason snapshot reads as the gates it passed and the evidence behind it', () => {
  assert.equal(gatesLabel(sameRole), '通过 G1、G2、G3、G4、G5、G6');
  assert.deepEqual(evidenceLines(sameRole), ['测试 · Practitioner']);
  assert.equal(gatesLabel(crossRole), '通过 G1、G5、G6、X1、X2');
  assert.deepEqual(evidenceLines(crossRole), ['职级 +1 档 · 测试 Practitioner']);
  assert.deepEqual(evidenceLines({ ...sameRole, skills: [] }), []);
});

test('a notice names its post, the person, the reason and the moment', () => {
  const notice: Notice = {
    id: 5,
    postId: 7,
    postTitle: 'QA engineer',
    personNo: 'E100',
    personName: 'Ada',
    reason: 'person taken',
    createdAt: '2026-11-11T09:05:12',
    dismissedAt: null,
  };

  assert.equal(moment(notice.createdAt), '2026-11-11 09:05');
  assert.equal(noticePost(notice), 'QA engineer');
  assert.equal(noticePost({ postTitle: null, postId: 7 }), '岗位 7');
  assert.equal(noticeLine(notice), 'QA engineer · Ada（E100）· 人员已被占用 · 2026-11-11 09:05');
});

function inboxRow(overrides: Partial<InboxProposal> = {}): InboxProposal {
  return {
    ...proposal(),
    postId: 7,
    postTitle: 'QA engineer',
    project: 'Atlas',
    ...overrides,
  };
}

test('the inbox counts each chip over the whole list and pages one state', () => {
  const rows = [
    inboxRow({ id: 1, state: 'proposed' }),
    inboxRow({ id: 2, state: 'proposed' }),
    inboxRow({ id: 3, state: 'rejected' }),
    inboxRow({ id: 4, state: 'voided', voidReason: 'post changed' }),
  ];

  assert.deepEqual(INBOX_FILTERS.map((chip) => chip.state), ['proposed', 'rejected', 'voided']);
  assert.deepEqual(
    INBOX_FILTERS.map((chip) => chip.label),
    ['待决策', '已驳回', '已作废'],
  );
  assert.deepEqual(inboxCounts(rows), { proposed: 2, rejected: 1, voided: 1 });
  assert.deepEqual(
    inboxPage(rows, 'proposed').map((row) => row.id),
    [1, 2],
    'the server order is kept inside the page',
  );
  assert.deepEqual(inboxPage(rows, 'voided').map((row) => row.id), [4]);
  assert.deepEqual(inboxPage(rows, 'rejected').map((row) => row.id), [3]);
});

test('an inbox row names its post, and a post without a title falls back to its number', () => {
  assert.equal(inboxPost(inboxRow()), 'QA engineer');
  assert.equal(inboxPost(inboxRow({ postTitle: null, postId: 7 })), '岗位 7');
});
