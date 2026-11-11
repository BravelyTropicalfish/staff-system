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

/** 导入的在职标记，说成一个词。 */
export function stateLabel(person: Pick<PersonSummary, 'active'>): string {
  return person.active ? '在职' : '已停用';
}

/**
 * 档案上方那句话：数据从哪来，以及导入是否把它标为停用。本页没有任何写入口，因为服务端也没有。
 */
export function profileNote(person: Pick<PersonProfile, 'active'>): string {
  const state = person.active ? '在职' : '已停用 · 由导入标记';
  return `${state} · 主数据由报表导入，本页只读`;
}

/** 一项技能在档案里的写法：技能、档位名，以及它所属的技能域。 */
export function bandLabel(skill: PersonSkill): string {
  return `${skill.skill} · ${skill.band} · ${skill.roleDomain} 域`;
}

/** 一段占用：项目、闭区间日期、来历，以及已结束的标记。 */
export function occupancyLabel(row: PersonAllocation): string {
  const source = row.source === 'CONFIRMATION' ? '确认座位' : '手工登记';
  return `${row.project} · ${row.startDate} → ${row.endDate} · ${source}${row.ended ? ' · 已结束' : ''}`;
}
