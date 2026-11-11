/**
 * The payloads of `GET /api/posts/{id}/recommendations`, `GET /api/notices` and the four decision
 * verbs, plus the readings the confirmation screens make of them. The state, the reason snapshot and
 * the notice come from the server as they were stored; the screen decides only what is open, what
 * is history, and which verb a signed-in role may be offered.
 */

import { bandsAboveLabel, skillLabel } from './candidates.ts';
import type { CandidateSkill } from './candidates.ts';

/** The signed-in account as `GET /api/session` answers it. */
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

/** The reason snapshot a proposal was stored with: the gates that ran and the evidence behind it. */
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
  /** The coordinator's alone; a lead reads the same row with `null` here. */
  snapshot: ProposalSnapshot | null;
  proposedBy: string;
  proposedAt: string;
};

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

/** A proposal's state, with the reason beside it when the state is `voided`. */
export function stateLabel(row: Pick<Proposal, 'state' | 'voidReason'>): string {
  return row.state === 'voided' && row.voidReason ? `voided · ${row.voidReason}` : row.state;
}

/** The open proposals and the closed history, each in the order the server answered them. */
export function splitProposals(rows: Proposal[]): { open: Proposal[]; closed: Proposal[] } {
  return {
    open: rows.filter((row) => row.state === 'proposed'),
    closed: rows.filter((row) => row.state !== 'proposed'),
  };
}

export type ProposalAction = 'confirm' | 'reject' | 'withdraw';

/**
 * The verbs a signed-in role may be offered for one proposal: the lead of the project confirms or
 * rejects, the coordinator withdraws the proposal they made, and a closed proposal offers none.
 */
export function proposalActions(role: string, state: ProposalState): ProposalAction[] {
  if (state !== 'proposed') {
    return [];
  }
  return role === 'COORDINATOR' ? ['withdraw'] : ['confirm', 'reject'];
}

/** The person as the list names them, with the import's deactivation when there is one. */
export function proposalPerson(
  row: Pick<Proposal, 'personName' | 'personNo' | 'personActive'>,
): string {
  const base = `${row.personName} (${row.personNo})`;
  return row.personActive ? base : `${base} · inactive`;
}

/** The gates the snapshot recorded as passed, in the order they ran. */
export function gatesLabel(snapshot: ProposalSnapshot): string {
  return `passed ${snapshot.gates.join(', ')}`;
}

/** The evidence behind a proposal: the skills held, or the cross-role gap and its qualifying skill. */
export function evidenceLines(snapshot: ProposalSnapshot): string[] {
  if (snapshot.crossRole) {
    const cross = snapshot.crossRole;
    return [
      `${bandsAboveLabel(cross.bandsAbove)} the minimum · ${cross.skill} at ${cross.band}`,
    ];
  }
  return snapshot.skills.map(skillLabel);
}

/** The moment a notice was written, to the minute. */
export function moment(value: string): string {
  return value.slice(0, 16).replace('T', ' ');
}

/** The post a notice is about; a post without a title is named by its number. */
export function noticePost(notice: Pick<Notice, 'postTitle' | 'postId'>): string {
  return notice.postTitle ?? `post ${notice.postId}`;
}

/** One notice as the coordinator reads it: the post, the person, the reason and the time. */
export function noticeLine(notice: Notice): string {
  return `${noticePost(notice)} · ${notice.personName} (${notice.personNo}) · ${notice.reason} · ${moment(notice.createdAt)}`;
}
