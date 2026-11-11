/**
 * The payloads of `GET /api/projects` and `GET /api/projects/{id}` plus the readings the project
 * screens make of them. Ending is terminal, so the screen asks once with the whole consequence
 * spelled out — how many posts it cancels and how many confirmed people it releases — and offers
 * the action only to the project's own lead while the project is still open.
 */

import type { Post } from './post.ts';

export type Project = {
  id: number;
  name: string;
  lead: string;
  leadName: string;
  status: string;
  startDate: string;
  endDate: string;
};

export function projectState(project: Pick<Project, 'status'>): string {
  return project.status === 'CLOSED' ? 'closed' : 'open';
}

export function projectLine(project: Project): string {
  return `${project.leadName} · ${project.startDate} → ${project.endDate} · ${projectState(project)}`;
}

/** What the project's own screen says about the terminal state it is in. */
export function projectNote(project: Pick<Project, 'status'>): string {
  return project.status === 'CLOSED'
    ? 'closed · every post is cancelled and every allocation of this project is gone'
    : 'open · the posts below are the live requirement';
}

export type ProjectGap = { posts: number; seats: number; chosen: number; remaining: number };

/** The gap numbers of the whole project, summed from the rows `GET /api/projects/{id}/gap` sent. */
export function projectGap(
  rows: Pick<Post, 'headcount' | 'confirmed' | 'remaining'>[],
): ProjectGap {
  return {
    posts: rows.length,
    seats: rows.reduce((sum, row) => sum + row.headcount, 0),
    chosen: rows.reduce((sum, row) => sum + row.confirmed, 0),
    remaining: rows.reduce((sum, row) => sum + row.remaining, 0),
  };
}

function plural(count: number, one: string, many: string): string {
  return `${count} ${count === 1 ? one : many}`;
}

/** One sentence for the project: how many posts, how many seats chosen, how many still needed. */
export function gapSummary(rows: Pick<Post, 'headcount' | 'confirmed' | 'remaining'>[]): string {
  const gap = projectGap(rows);
  const chosen = `${gap.chosen} of ${gap.seats} seats chosen`;
  const short =
    gap.remaining === 0 ? 'filled' : `${plural(gap.remaining, 'seat', 'seats')} still needed`;
  return `${plural(gap.posts, 'post', 'posts')} · ${chosen} · ${short}`;
}

/** Only the project's own lead ends it, and only while it is open; the server refuses the rest. */
export function canEnd(
  project: Pick<Project, 'lead' | 'status'>,
  viewer: { username: string; role: string },
): boolean {
  return viewer.role === 'LEAD' && project.lead === viewer.username && project.status !== 'CLOSED';
}

/** The sentence the confirmation shows: the whole consequence, before anything is sent. */
export function endConfirmation(
  project: Pick<Project, 'name'>,
  rows: Pick<Post, 'confirmed'>[],
): string {
  const people = rows.reduce((sum, row) => sum + row.confirmed, 0);
  return (
    `Ending ${project.name} cancels its ${plural(rows.length, 'post', 'posts')} `
    + `and releases ${plural(people, 'confirmed person', 'confirmed people')}. `
    + 'It cannot be undone.'
  );
}
