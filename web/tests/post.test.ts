import assert from 'node:assert/strict';
import test from 'node:test';
import {
  formFromPost,
  frozenFields,
  frozenReason,
  gapLabel,
  gapNumbers,
  headcountFloor,
  postDraft,
  seatLabel,
  stillShort,
} from '../src/lib/post.ts';
import type { GapPerson, Post, PostGap } from '../src/lib/post.ts';

/**
 * T16, the post screens' own readings of a post and of the gap view. The create and edit forms
 * render from these functions and show the numbers the server sent, so pinning them pins what the
 * page shows: one sentence per gap row, the six fields a confirmation freezes, and the body the
 * form posts back.
 */

const post: Post = {
  id: 7,
  projectId: 3,
  project: 'Atlas',
  title: 'QA engineer',
  role: 'QA',
  minJobLevel: 'Senior',
  headcount: 2,
  startDate: '2027-05-01',
  endDate: '2027-08-31',
  travel: 'full',
  status: 'partially filled',
  confirmed: 1,
  remaining: 1,
  skills: [
    { skill: '测试', kind: 'required', band: 'Practitioner', target: null },
    { skill: '后端开发', kind: 'bonus', band: null, target: 'Practitioner' },
  ],
};

function gap(overrides: Partial<PostGap> = {}): PostGap {
  return {
    id: 7,
    title: 'QA engineer',
    role: 'QA',
    status: 'partially filled',
    headcount: 2,
    confirmed: 1,
    remaining: 1,
    stillShort: true,
    people: [],
    ...overrides,
  };
}

const ada: GapPerson = {
  assignmentId: 41,
  personNo: 'E100',
  name: 'Ada',
  confirmedAt: '2027-05-01T09:00:00',
  active: true,
};

test('the gap numbers are the three the server sent, never recomputed', () => {
  assert.deepEqual(gapNumbers(gap()), { chosen: 1, required: 2, remaining: 1 });
  assert.deepEqual(gapNumbers(gap({ confirmed: 0, remaining: 2 })), {
    chosen: 0,
    required: 2,
    remaining: 2,
  });
});

test('one sentence per gap row says how many are chosen and what is still needed', () => {
  assert.equal(gapLabel(gap()), '1 / 2 已到位 · 还缺 1 人');
  assert.equal(gapLabel(gap({ confirmed: 2, remaining: 0 })), '2 / 2 已到位 · 已满');
  assert.equal(gapLabel(gap({ confirmed: 0, remaining: 2 })), '0 / 2 已到位 · 还缺 2 人');
  assert.equal(
    gapLabel(gap({ headcount: 1, confirmed: 0, remaining: 1 })),
    '0 / 1 已到位 · 还缺 1 人',
    'the singular and the plural read the same way',
  );
});

test('a confirmation freezes the six gate-bearing fields and says why', () => {
  assert.deepEqual(frozenFields({ confirmed: 0 }), [], 'a post with no seat taken is fully editable');
  assert.deepEqual(frozenFields({ confirmed: 1 }), [
    'role',
    'minJobLevel',
    'requiredSkills',
    'startDate',
    'endDate',
    'travel',
  ]);
  assert.equal(frozenReason({ confirmed: 0 }), null);
  assert.equal(
    frozenReason({ confirmed: 1 }),
    '先解除已确认的 1 人；在有人确认期间，角色、职级、required 技能行、日期与差旅不可修改',
  );
  assert.match(frozenReason({ confirmed: 3 }) ?? '', /^先解除已确认的 3 人；/);
});

test('the headcount may rise under a confirmation but never fall below it', () => {
  assert.equal(headcountFloor({ confirmed: 0 }), 1);
  assert.equal(headcountFloor({ confirmed: 1 }), 1);
  assert.equal(headcountFloor({ confirmed: 3 }), 3);
});

test('the still-short posts are the ones the server marked, in its order', () => {
  const rows = [
    gap({ id: 9, stillShort: false }),
    gap({ id: 7, stillShort: true }),
    gap({ id: 8, stillShort: true }),
  ];

  assert.deepEqual(
    stillShort(rows).map((row) => row.id),
    [7, 8],
    'the server order is kept, never re-sorted',
  );
});

test('a seat names the person, the day and the deactivation when the import has one', () => {
  assert.equal(seatLabel(ada), 'Ada（E100）· 确认于 2027-05-01');
  assert.equal(
    seatLabel({ ...ada, active: false }),
    'Ada（E100）· 确认于 2027-05-01 · 已停用',
  );
});

test('the form starts from the stored post and posts back the same body', () => {
  const value = formFromPost(post);

  assert.equal(value.title, 'QA engineer');
  assert.equal(value.headcount, '2', 'the number becomes an editable string');
  assert.deepEqual(value.skills, [
    { skill: '测试', kind: 'required', band: 'Practitioner' },
    { skill: '后端开发', kind: 'bonus', band: '' },
  ]);

  assert.deepEqual(postDraft(value), {
    title: 'QA engineer',
    role: 'QA',
    minJobLevel: 'Senior',
    headcount: 2,
    startDate: '2027-05-01',
    endDate: '2027-08-31',
    travel: 'full',
    skills: value.skills,
  });

  assert.equal(postDraft({ ...value, title: '  ' }).title, null, 'a blank title is no title');
  assert.equal(
    postDraft({ ...value, title: '  QA engineer  ' }).title,
    'QA engineer',
    'the title is trimmed',
  );
  assert.equal(postDraft({ ...value, title: '' }).title, null);
  assert.equal(postDraft({ ...value, headcount: '4' }).headcount, 4);
});
