import { expect, request, type APIRequestContext } from '@playwright/test';
import { API, COORDINATOR, LEAD_ONE } from './env';
import { workbook, type PersonRow, type SkillRow } from './workbook';

/**
 * T26, the fixtures. Every state the specs need is created through the same routes a user drives,
 * so a fixture can never build something the server would refuse: the coordinator imports the
 * workbook and proposes people; the lead opens its own project, creates its posts and confirms.
 */

export type Project = {
  id: number;
  name: string;
  lead: string;
  leadName: string;
  status: string;
  startDate: string;
  endDate: string;
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
};

export type Proposal = { id: number; personNo: string; state: string };

export type ImportBatch = { id: number; filename: string; status: string; errorCount: number };

export type PostDraft = {
  title?: string | null;
  role: string;
  minJobLevel: string;
  headcount: number;
  startDate: string;
  endDate: string;
  travel: string;
  skills?: { skill: string; kind: 'required' | 'bonus'; band: string }[];
};

/** An API context signed in as whoever `stateFile` holds. The caller disposes it. */
export function contextAs(stateFile: string): Promise<APIRequestContext> {
  return request.newContext({ baseURL: API, storageState: stateFile });
}

export function coordinator(): Promise<APIRequestContext> {
  return contextAs(COORDINATOR);
}

export function leadOne(): Promise<APIRequestContext> {
  return contextAs(LEAD_ONE);
}

/** A name no other run can collide with, since the database outlives the single test file. */
export function unique(prefix: string): string {
  return `${prefix}-${Date.now().toString(36)}`;
}

export async function importWorkbook(
  context: APIRequestContext,
  people: PersonRow[],
  skills: SkillRow[],
): Promise<ImportBatch> {
  const response = await context.post('/api/imports', {
    multipart: {
      file: {
        name: 'people.xlsx',
        mimeType: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
        buffer: await workbook(people, skills),
      },
    },
  });
  expect(response.status(), 'the workbook is applied').toBe(201);
  return (await response.json()) as ImportBatch;
}

/**
 * A project is opened by its own lead, so this signs in as lead.one whatever context is handed in:
 * the caller's context may be the coordinator, who reads projects and opens none.
 */
export async function createProject(
  _context: APIRequestContext,
  body: { name: string; lead?: string; startDate?: string; endDate?: string },
): Promise<Project> {
  const lead = await leadOne();
  try {
    const response = await lead.post('/api/projects', {
      data: {
        name: body.name,
        lead: body.lead ?? 'lead.one',
        startDate: body.startDate ?? '2027-01-01',
        endDate: body.endDate ?? '2027-12-31',
      },
    });
    expect(response.status(), 'the project is created').toBe(201);
    return (await response.json()) as Project;
  } finally {
    await lead.dispose();
  }
}

export async function createPost(
  context: APIRequestContext,
  projectId: number,
  draft: PostDraft,
): Promise<Post> {
  const response = await context.post(`/api/projects/${projectId}/posts`, { data: draft });
  expect(response.status(), 'the post is created').toBe(201);
  return (await response.json()) as Post;
}

export async function propose(
  context: APIRequestContext,
  postId: number,
  personNo: string,
): Promise<Proposal> {
  const response = await context.post(`/api/posts/${postId}/recommendations`, {
    data: { personNo },
  });
  expect(response.status(), 'the proposal is stored').toBe(201);
  return (await response.json()) as Proposal;
}

export async function confirm(
  context: APIRequestContext,
  recommendationId: number,
): Promise<void> {
  const response = await context.post(`/api/recommendations/${recommendationId}/confirm`);
  expect(response.status(), 'the confirmation is accepted').toBe(200);
}

/** A DEV post with one required band, the shape both journeys start from. */
export function devPost(overrides: Partial<PostDraft> = {}): PostDraft {
  return {
    title: 'Backend developer',
    role: 'DEV',
    minJobLevel: 'Senior',
    headcount: 1,
    startDate: '2027-02-01',
    endDate: '2027-06-30',
    travel: 'none',
    skills: [{ skill: '后端开发', kind: 'required', band: 'Practitioner' }],
    ...overrides,
  };
}

export type Person = {
  employeeNo: string;
  name: string;
  role: string;
  jobLevel: string;
  travel: string;
  skill: string;
  band: string;
};

/**
 * One person with one band, written through the import the profile reads from, so a screen test
 * never sees a row the product could not have produced. The defaults pass every gate of `devPost`.
 */
export async function seedPerson(
  context: APIRequestContext,
  overrides: Partial<Person> = {},
): Promise<Person> {
  const tag = `${Date.now().toString(36)}${Math.random().toString(36).slice(2, 6)}`;
  const person: Person = {
    employeeNo: `E2E-${tag}`,
    name: `E2E Person ${tag}`,
    role: 'DEV',
    jobLevel: 'Senior',
    travel: 'full',
    skill: '后端开发',
    band: 'Practitioner',
    ...overrides,
  };
  await importWorkbook(
    context,
    [
      {
        employeeNo: person.employeeNo,
        name: person.name,
        role: person.role,
        jobLevel: person.jobLevel,
        travel: person.travel,
      },
    ],
    [{ employeeNo: person.employeeNo, skill: person.skill, proficiency: person.band }],
  );
  return person;
}

/** A hand-entered range, the only allocation a person has without a confirmation. */
export async function allocate(
  context: APIRequestContext,
  body: { personNo: string; projectId: number; startDate: string; endDate: string },
): Promise<{ id: number }> {
  const response = await context.post('/api/allocations', { data: body });
  expect(response.status(), 'the hand record is stored').toBe(201);
  return (await response.json()) as { id: number };
}
