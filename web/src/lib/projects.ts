/**
 * `GET /api/projects` 与 `GET /api/projects/{id}` 的载荷，以及项目屏幕对它们的读数。结束是不可
 * 逆的终态，所以屏幕先问一次、把后果说全 —— 取消几个岗位、释放几个已确认人员 —— 再把动作交给
 * 项目自己的项目经理、且仅在项目仍进行中时。
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
  return project.status === 'CLOSED' ? '已结束' : '进行中';
}

export function projectLine(project: Project): string {
  return `${project.leadName} · ${project.startDate} → ${project.endDate} · ${projectState(project)}`;
}

/** 项目自己那一屏对当前状态的说明。 */
export function projectNote(project: Pick<Project, 'status'>): string {
  return project.status === 'CLOSED'
    ? '已结束 · 岗位全部取消，本项目的占用全部删除'
    : '进行中 · 下列岗位是当前需求';
}

export type ProjectGap = { posts: number; seats: number; chosen: number; remaining: number };

/** 整个项目的缺口数字，由 `GET /api/projects/{id}/gap` 的行加总而来。 */
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

/** 一个项目一句话：几个岗位、几个名额已到位、还缺几个。 */
export function gapSummary(rows: Pick<Post, 'headcount' | 'confirmed' | 'remaining'>[]): string {
  const gap = projectGap(rows);
  const chosen = `${gap.chosen} / ${gap.seats} 已到位`;
  const short = gap.remaining === 0 ? '已满' : `还缺 ${gap.remaining} 人`;
  return `${gap.posts} 个岗位 · ${chosen} · ${short}`;
}

/** 只有项目自己的项目经理能结束它，且只在项目仍进行中时；服务端会拒绝其他人。 */
export function canEnd(
  project: Pick<Project, 'lead' | 'status'>,
  viewer: { username: string; role: string },
): boolean {
  return viewer.role === 'LEAD' && project.lead === viewer.username && project.status !== 'CLOSED';
}

/** 确认框上的那句话：在发出去之前，把全部后果说清楚。 */
export function endConfirmation(
  project: Pick<Project, 'name'>,
  rows: Pick<Post, 'confirmed'>[],
): string {
  const people = rows.reduce((sum, row) => sum + row.confirmed, 0);
  return `结束 ${project.name} 会取消它的 ${rows.length} 个岗位，并释放 ${people} 名已确认人员。不可撤销。`;
}
