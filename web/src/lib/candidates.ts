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

/** A cross-role person: the role, the bands above the requirement and the qualifying skill. */
export type FallbackRow = {
  employeeNo: string;
  name: string;
  role: string;
  jobLevel: string;
  travel: string;
  bandsAbove: number;
  skill: string;
  band: string;
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
  G1: 'active',
  G2: 'role',
  G3: 'job level',
  G4: 'required skills',
  G5: 'travel',
  G6: 'dates',
  X1: 'one band above',
  X2: 'domain skill',
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

/** A band is named, never numbered; a bonus row also names the target it misses. */
export function skillLabel(skill: CandidateSkill): string {
  const held = skill.band ?? 'not held';
  if (skill.kind === 'bonus' && skill.target) {
    return `${skill.skill} · ${held} (needs ${skill.target})`;
  }
  return `${skill.skill} · ${held}`;
}

/** How far above the post's minimum a cross-role person sits. */
export function bandsAboveLabel(bandsAbove: number): string {
  return bandsAbove === 1 ? '+1 band' : `+${bandsAbove} bands`;
}
