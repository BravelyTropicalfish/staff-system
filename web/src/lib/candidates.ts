/**
 * The payload of `GET /api/posts/{id}/candidates` and the readings the screen makes of it. Nothing
 * here ranks, scores or reorders: the server already ordered both lists by name and by the employee
 * number, and the screen may not add a fit of its own.
 */

export type CandidateSkill = {
  skill: string;
  kind: 'required' | 'bonus';
  band: string | null;
  target: string | null;
};

export type SuitableRow = {
  employeeNo: string;
  name: string;
  role: string;
  jobLevel: string;
  travel: string;
  gates: string[];
  skills: CandidateSkill[];
};

export type ExclusionRow = {
  employeeNo: string;
  name: string;
  role: string;
  gate: string;
  reason: string;
};

/** A cross-role person: the role, the bands above, the qualifying skill, and the gates that ran. */
export type FallbackRow = {
  employeeNo: string;
  name: string;
  role: string;
  jobLevel: string;
  travel: string;
  bandsAbove: number;
  skill: string;
  band: string;
  gates: string[];
};

export type CandidateSearch = {
  suitable: SuitableRow[];
  excluded: ExclusionRow[];
  exclusionCounts: Record<string, number>;
  fallback: FallbackRow[];
  fallbackReason: string | null;
};

export const GATE_ORDER = ['G1', 'G2', 'G3', 'G4', 'G5', 'G6', 'X1', 'X2'] as const;

export type GateCode = (typeof GATE_ORDER)[number];

const GATE_NAMES: Record<GateCode, string> = {
  G1: '在职',
  G2: '角色',
  G3: '职级',
  G4: '必备技能',
  G5: '差旅',
  G6: '占用',
  X1: '高一档',
  X2: '技能域',
};

export type GateChip = { code: GateCode; name: string; count: number };

/**
 * Every gate the server counts, in its own order, `X1`/`X2` included and zero when the fallback did
 * not run — a fixed six-column walk would silently drop the fallback reasons.
 */
export function gateChips(search: CandidateSearch): GateChip[] {
  return GATE_ORDER.map((code) => ({
    code,
    name: GATE_NAMES[code],
    count: search.exclusionCounts[code] ?? 0,
  }));
}

export type CrossRole = { shown: boolean; rows: FallbackRow[]; reason: string | null };

/** The cross-role section is shown only when the same-role list is empty. */
export function crossRole(search: CandidateSearch): CrossRole {
  if (search.suitable.length > 0) {
    return { shown: false, rows: [], reason: null };
  }
  return { shown: true, rows: search.fallback, reason: search.fallbackReason };
}

/** 档位只以名字出现，从不用序号；bonus 行还会点名它差的目标档。 */
export function skillLabel(skill: CandidateSkill): string {
  const held = skill.band ?? '未持有';
  if (skill.kind === 'bonus' && skill.target) {
    return `${skill.skill} · ${held}（要求 ${skill.target}）`;
  }
  return `${skill.skill} · ${held}`;
}

/** 跟角色人选比岗位最低要求高出几档。 */
export function bandsAboveLabel(bandsAbove: number): string {
  return `+${bandsAbove} 档`;
}
