/**
 * The payloads of `GET /api/posts/{id}`, `GET /api/projects/{id}/gap` and `GET /api/bands`, plus the
 * readings the post screens make of them. The numbers are the server's: nothing here recomputes a
 * remaining count or reorders a list, so the two roles cannot read different ones.
 */

export type PostSkill = {
  skill: string;
  kind: 'required' | 'bonus';
  band: string | null;
  target: string | null;
};

export type Post = {
  id: number;
  projectId: number;
  project: string;
  title: string | null;
  role: string;
  minJobLevel: string;
  headcount: number;
  startDate: string;
  endDate: string;
  travel: string;
  status: string;
  confirmed: number;
  remaining: number;
  skills: PostSkill[];
};

export type GapPerson = {
  /** The assignment a release names; the seat is read from the gap but released by this id. */
  assignmentId: number;
  personNo: string;
  name: string;
  confirmedAt: string;
  active: boolean;
};

export type PostGap = {
  id: number;
  title: string | null;
  role: string;
  status: string;
  headcount: number;
  confirmed: number;
  remaining: number;
  stillShort: boolean;
  people: GapPerson[];
};

export type Bands = {
  jobLevels: string[];
  proficiencies: string[];
  travel: string[];
};

export type RoleOption = {
  id: number;
  name: string;
  active: boolean;
};

export type SkillOption = {
  id: number;
  name: string;
  active: boolean;
  roleDomainId: number | null;
};

export type GapNumbers = { chosen: number; required: number; remaining: number };

/** The three numbers a gap row carries; the screen shows them as they arrive. */
export function gapNumbers(row: Pick<PostGap, 'headcount' | 'confirmed' | 'remaining'>): GapNumbers {
  return { chosen: row.confirmed, required: row.headcount, remaining: row.remaining };
}

/** One sentence per row: how many are chosen, out of how many, and what is still needed. */
export function gapLabel(
  row: Pick<PostGap, 'headcount' | 'confirmed' | 'remaining' | 'status'>,
): string {
  const numbers = gapNumbers(row);
  const chosen = `${numbers.chosen} of ${numbers.required} chosen`;
  if (numbers.remaining === 0) {
    return `${chosen} · filled`;
  }
  return `${chosen} · ${numbers.remaining} still needed`;
}

/** The gate-bearing fields the server freezes while a person is confirmed. */
export function frozenFields(post: Pick<Post, 'confirmed'>): string[] {
  if (post.confirmed === 0) {
    return [];
  }
  return ['role', 'minJobLevel', 'requiredSkills', 'startDate', 'endDate', 'travel'];
}

/** Why those fields are disabled, in the server's own voice with the people named by count. */
export function frozenReason(post: Pick<Post, 'confirmed'>): string | null {
  if (post.confirmed === 0) {
    return null;
  }
  const who =
    post.confirmed === 1 ? 'the confirmed person' : `the ${post.confirmed} confirmed people`;
  return (
    `release ${who} first; the role, the job level, a required skill row, the dates and the ` +
    'travel cannot change while a person is confirmed'
  );
}

/** The headcount may rise under a confirmation but never fall below it. */
export function headcountFloor(post: Pick<Post, 'confirmed'>): number {
  return Math.max(1, post.confirmed);
}

/** The posts the gap view calls still short, in the server's order. */
export function stillShort(rows: PostGap[]): PostGap[] {
  return rows.filter((row) => row.stillShort);
}

/** A person's seat on the gap row is marked when the import has since deactivated them. */
export function seatLabel(person: GapPerson): string {
  const base = `${person.name} (${person.personNo}) · confirmed ${person.confirmedAt.slice(0, 10)}`;
  return person.active ? base : `${base} · inactive`;
}

export type SkillRowValue = { skill: string; kind: 'required' | 'bonus'; band: string };

export type PostFormValue = {
  title: string;
  role: string;
  minJobLevel: string;
  headcount: string;
  startDate: string;
  endDate: string;
  travel: string;
  skills: SkillRowValue[];
};

/** The body both the create route and the patch route take; a blank title is no title. */
export function postDraft(value: PostFormValue) {
  return {
    title: value.title.trim() === '' ? null : value.title.trim(),
    role: value.role,
    minJobLevel: value.minJobLevel,
    headcount: Number(value.headcount),
    startDate: value.startDate,
    endDate: value.endDate,
    travel: value.travel,
    skills: value.skills,
  };
}

/** The edit form starts from the stored post, its own skills included. */
export function formFromPost(post: Post): PostFormValue {
  return {
    title: post.title ?? '',
    role: post.role,
    minJobLevel: post.minJobLevel,
    headcount: String(post.headcount),
    startDate: post.startDate,
    endDate: post.endDate,
    travel: post.travel,
    skills: post.skills.map((row) => ({
      skill: row.skill,
      kind: row.kind,
      band: row.band ?? '',
    })),
  };
}
