/**
 * The payloads of `GET /api/people` and `GET /api/people/{employeeNo}` plus the readings the pool
 * screens make of them. Person data only arrives by import, so both screens are read-only: they
 * name what the server holds — the one role, the bands by name, the occupancy including the ranges
 * that have already ended — and offer no verb at all.
 */

export type PersonSummary = {
  employeeNo: string;
  name: string;
  role: string;
  jobLevel: string;
  travel: string;
  active: boolean;
};

export type PersonSkill = {
  skill: string;
  /** The one role whose domain holds the skill; displayed as context, never as a second role. */
  roleDomain: string;
  /** The band's name, never its position in the scale. */
  band: string;
};

export type PersonAllocation = {
  id: number;
  project: string;
  startDate: string;
  endDate: string;
  /** `MANUAL` for a hand record, `CONFIRMATION` for the seat a confirmation wrote. */
  source: string;
  ended: boolean;
};

export type PersonProfile = {
  employeeNo: string;
  name: string;
  email: string | null;
  role: string;
  jobLevel: string;
  travel: string;
  active: boolean;
  skills: PersonSkill[];
  allocations: PersonAllocation[];
};

/** The import's active flag, said in a word. */
export function stateLabel(person: Pick<PersonSummary, 'active'>): string {
  return person.active ? 'active' : 'inactive';
}

/**
 * The line above a profile: where the data comes from, with the deactivation the import set when
 * there is one. Nothing on this screen writes, because nothing here has a route to write to.
 */
export function profileNote(person: Pick<PersonProfile, 'active'>): string {
  const state = person.active ? 'active' : 'inactive — the import marked it';
  return `${state} · master data arrives by import, so this profile is read-only`;
}

/** One skill as the profile names it: the skill, the band by name, and the domain it sits in. */
export function bandLabel(skill: PersonSkill): string {
  return `${skill.skill} · ${skill.band} · ${skill.roleDomain} domain`;
}

/** One occupancy: the project, the inclusive range, how it came about, and the ended mark. */
export function occupancyLabel(row: PersonAllocation): string {
  const source = row.source === 'CONFIRMATION' ? 'confirmed seat' : 'hand record';
  return `${row.project} · ${row.startDate} → ${row.endDate} · ${source}${row.ended ? ' · ended' : ''}`;
}
