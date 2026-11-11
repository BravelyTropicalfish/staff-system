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

/** 一行缺口一句话：几个已到位、共几个，还缺什么。 */
export function gapLabel(
  row: Pick<PostGap, 'headcount' | 'confirmed' | 'remaining' | 'status'>,
): string {
  const numbers = gapNumbers(row);
  const chosen = `${numbers.chosen} / ${numbers.required} 已到位`;
  if (numbers.remaining === 0) {
    return `${chosen} · 已满`;
  }
  return `${chosen} · 还缺 ${numbers.remaining} 人`;
}

/** The gate-bearing fields the server freezes while a person is confirmed. */
export function frozenFields(post: Pick<Post, 'confirmed'>): string[] {
  if (post.confirmed === 0) {
    return [];
  }
  return ['role', 'minJobLevel', 'requiredSkills', 'startDate', 'endDate', 'travel'];
}

/** 为什么这些字段被禁用，用服务端的口径、按人数点名。 */
export function frozenReason(post: Pick<Post, 'confirmed'>): string | null {
  if (post.confirmed === 0) {
    return null;
  }
  const who =
    post.confirmed === 1 ? '已确认的 1 人' : `已确认的 ${post.confirmed} 人`;
  return `先解除${who}；在有人确认期间，角色、职级、required 技能行、日期与差旅不可修改`;
}

/** The headcount may rise under a confirmation but never fall below it. */
export function headcountFloor(post: Pick<Post, 'confirmed'>): number {
  return Math.max(1, post.confirmed);
}

/** The posts the gap view calls still short, in the server's order. */
export function stillShort(rows: PostGap[]): PostGap[] {
  return rows.filter((row) => row.stillShort);
}

/** 一个人的座位：姓名、工号与确认日；导入后来停用的会被标记。 */
export function seatLabel(person: GapPerson): string {
  const base = `${person.name}（${person.personNo}）· 确认于 ${person.confirmedAt.slice(0, 10)}`;
  return person.active ? base : `${base} · 已停用`;
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
