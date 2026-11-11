/**
 * The readings of the one board that holds the posts and their gap. The numbers are the server's:
 * the board sums the rows it was sent, it never recounts a seat. The prototype keeps the projects
 * in the same view as the posts — the three tiles, the project table, the post table and the one
 * post's detail — so these helpers describe that one screen, not three.
 */

import type { PostGap } from './post.ts';

export type BoardNumbers = { short: number; confirmed: number; pending: number };

/** The three tiles: posts still short a person, seats filled, proposals waiting on this account. */
export function boardNumbers(
  gaps: Pick<PostGap, 'stillShort' | 'confirmed'>[],
  proposals: { state: string }[],
): BoardNumbers {
  return {
    short: gaps.filter((row) => row.stillShort).length,
    confirmed: gaps.reduce((sum, row) => sum + row.confirmed, 0),
    pending: proposals.filter((row) => row.state === 'proposed').length,
  };
}

/** 一行里岗位的写法：自己的标题，lead 没填标题时用岗位号。 */
export function postName(row: { id: number; title: string | null }): string {
  return row.title ?? `岗位 ${row.id}`;
}

/** The board opens the first post that still needs somebody, and falls back to the first one. */
export function initialPost(gaps: Pick<PostGap, 'id' | 'stillShort'>[]): number | null {
  const short = gaps.find((row) => row.stillShort);
  return short?.id ?? gaps[0]?.id ?? null;
}

/**
 * Whether the board may cancel this post: only its own lead, only while nothing is confirmed and
 * the post is not cancelled already. The server repeats every clause.
 */
export function canCancelPost(
  post: { status: string; confirmed: number },
  project: { lead: string } | null,
  viewer: { username: string; role: string } | null,
): boolean {
  if (!viewer || !project) return false;
  return (
    viewer.role === 'LEAD' &&
    viewer.username === project.lead &&
    post.status !== 'cancelled' &&
    post.confirmed === 0
  );
}

/** Whether this post may be edited on its own screen, by the same rule that allows the cancel. */
export function canEditPost(
  post: { status: string },
  project: { lead: string } | null,
  viewer: { username: string; role: string } | null,
): boolean {
  if (!viewer || !project) return false;
  return viewer.role === 'LEAD' && viewer.username === project.lead && post.status !== 'cancelled';
}
