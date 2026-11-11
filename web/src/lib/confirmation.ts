/**
 * `GET /api/posts/{id}/recommendations`、`GET /api/notices` 与四个决策动作的载荷，以及确认屏幕
 * 对它们的读数。状态、拒绝理由快照与通知都按服务端存的样子来；屏幕只决定什么还在等、什么是
 * 历史、以及当前角色可以被提供哪个动作。
 */

import { bandsAboveLabel, skillLabel } from './candidates.ts';
import type { CandidateSkill } from './candidates.ts';

/** 登录账号，`GET /api/session` 这样回答。 */
export type SessionView = {
  username: string;
  role: string;
  displayName: string;
};

export type CrossRoleEvidence = {
  bandsAbove: number;
  skill: string;
  band: string;
};

/** 推荐写入时的理由快照：跑过的门槛与背后的证据。 */
export type ProposalSnapshot = {
  personNo: string;
  role: string;
  jobLevel: string;
  travel: string;
  gates: string[];
  skills: CandidateSkill[];
  crossRole: CrossRoleEvidence | null;
};

export type ProposalState = 'proposed' | 'confirmed' | 'rejected' | 'withdrawn' | 'voided';

export type Proposal = {
  id: number;
  personNo: string;
  personName: string;
  personActive: boolean;
  state: ProposalState;
  voidReason: string | null;
  /** 协调员独有；项目经理读到的是同一行，但这里为 `null`。 */
  snapshot: ProposalSnapshot | null;
  proposedBy: string;
  proposedAt: string;
};

/**
 * 决策收件箱的一行：推荐本身，加上它所属的岗位与项目，这样收件箱跨项目读，不必一岗一请求。
 */
export type InboxProposal = Proposal & {
  postId: number;
  postTitle: string | null;
  project: string;
};

export type InboxFilter = 'proposed' | 'rejected' | 'voided';

/** 收件箱的三个 chips，按阅读顺序。 */
export const INBOX_FILTERS: { state: InboxFilter; label: string }[] = [
  { state: 'proposed', label: '待决策' },
  { state: 'rejected', label: '已驳回' },
  { state: 'voided', label: '已作废' },
];

/** 每个 chip 会显示多少行；计数始终描述整个收件箱。 */
export function inboxCounts(
  rows: Pick<InboxProposal, 'state'>[],
): Record<InboxFilter, number> {
  return {
    proposed: rows.filter((row) => row.state === 'proposed').length,
    rejected: rows.filter((row) => row.state === 'rejected').length,
    voided: rows.filter((row) => row.state === 'voided').length,
  };
}

/** 收件箱的一页；保持服务端顺序，最新的待决策读在最前。 */
export function inboxPage(rows: InboxProposal[], state: InboxFilter): InboxProposal[] {
  return rows.filter((row) => row.state === state);
}

/** 收件箱一行说的是哪个岗位；没有标题的岗位用工号称呼。 */
export function inboxPost(row: Pick<InboxProposal, 'postTitle' | 'postId'>): string {
  return row.postTitle ?? `岗位 ${row.postId}`;
}

export type Notice = {
  id: number;
  postId: number;
  postTitle: string | null;
  personNo: string;
  personName: string;
  reason: string;
  createdAt: string;
  dismissedAt: string | null;
};

const STATE_WORDS: Record<ProposalState, string> = {
  proposed: '待决策',
  confirmed: '已确认',
  rejected: '已驳回',
  withdrawn: '已撤回',
  voided: '已作废',
};

const REASON_WORDS: Record<string, string> = {
  'post changed': '岗位要求已变更',
  'post cancelled': '岗位已取消',
  'post filled': '岗位已满',
  'person taken': '人员已被占用',
};

/** 服务端记的作废/通知原因为码值，屏幕上换成人能读懂的说法。 */
export function reasonLabel(reason: string): string {
  return REASON_WORDS[reason] ?? reason;
}

/** 一条推荐的状态；状态为 `voided` 时把原因写在旁边。 */
export function stateLabel(row: Pick<Proposal, 'state' | 'voidReason'>): string {
  const word = STATE_WORDS[row.state] ?? row.state;
  return row.state === 'voided' && row.voidReason
    ? `${word} · ${reasonLabel(row.voidReason)}`
    : word;
}

/** 待决策的与已关闭的历史，各自保持服务端给的顺序。 */
export function splitProposals(rows: Proposal[]): { open: Proposal[]; closed: Proposal[] } {
  return {
    open: rows.filter((row) => row.state === 'proposed'),
    closed: rows.filter((row) => row.state !== 'proposed'),
  };
}

export type ProposalAction = 'confirm' | 'reject' | 'withdraw';

/**
 * 一个角色对一条推荐可以被提供的动作：项目所属的项目经理确认或驳回，协调员撤回自己发起的
 * 推荐，已关闭的推荐没有任何动作。
 */
export function proposalActions(role: string, state: ProposalState): ProposalAction[] {
  if (state !== 'proposed') {
    return [];
  }
  return role === 'COORDINATOR' ? ['withdraw'] : ['confirm', 'reject'];
}

/** 列表里对人的称呼，导入后来停用的会被标记。 */
export function proposalPerson(
  row: Pick<Proposal, 'personName' | 'personNo' | 'personActive'>,
): string {
  const base = `${row.personName}（${row.personNo}）`;
  return row.personActive ? base : `${base} · 已停用`;
}

/** 快照里记录为已通过的门槛，按跑过的顺序。 */
export function gatesLabel(snapshot: ProposalSnapshot): string {
  return `通过 ${snapshot.gates.join('、')}`;
}

/** 推荐背后的证据：持有的技能，或跨角色的档位差与达标技能。 */
export function evidenceLines(snapshot: ProposalSnapshot): string[] {
  if (snapshot.crossRole) {
    const cross = snapshot.crossRole;
    return [`职级 ${bandsAboveLabel(cross.bandsAbove)} · ${cross.skill} ${cross.band}`];
  }
  return snapshot.skills.map(skillLabel);
}

/** 通知写入的时刻，精确到分钟。 */
export function moment(value: string): string {
  return value.slice(0, 16).replace('T', ' ');
}

/** 通知说的是哪个岗位；没有标题的岗位用工号称呼。 */
export function noticePost(notice: Pick<Notice, 'postTitle' | 'postId'>): string {
  return notice.postTitle ?? `岗位 ${notice.postId}`;
}

/** 协调员读到的一条通知：岗位、人员、原因与时间。 */
export function noticeLine(notice: Notice): string {
  return `${noticePost(notice)} · ${notice.personName}（${notice.personNo}）· ${reasonLabel(notice.reason)} · ${moment(notice.createdAt)}`;
}
