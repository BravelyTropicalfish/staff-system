import { useCallback, useEffect, useState } from 'react';
import { Link, useNavigate, useOutletContext } from 'react-router-dom';
import { ApiError, getJson, sendJson } from '../lib/api';
import type { Project } from '../lib/projects';
import { projectState } from '../lib/projects';
import { emptyProject, leadLocked, projectDraft, projectProblem } from '../lib/projectForm';
import type { ProjectFormValue, Viewer } from '../lib/projectForm';

function failure(error: unknown): string {
  return error instanceof ApiError ? `${error.code}: ${error.message}` : String(error);
}

/**
 * The create form. A lead opens a project of their own, so the lead field is their account and
 * locked; the coordinator opens one for any lead. The same rule sits in the server, and the form
 * only spares the round trip that would repeat it.
 */
function NewProject({ viewer, onCreated }: { viewer: Viewer; onCreated: (project: Project) => void }) {
  const locked = leadLocked(viewer);
  const [value, setValue] = useState<ProjectFormValue>(() => emptyProject(viewer));
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  const change = (patch: Partial<ProjectFormValue>) =>
    setValue((current) => ({ ...current, ...patch }));

  const submit = () => {
    const problem = projectProblem(value);
    if (problem !== null) {
      setError(problem);
      return;
    }
    setBusy(true);
    setError(null);
    sendJson<Project>('POST', '/api/projects', projectDraft(value))
      .then(onCreated)
      .catch((caught: unknown) => setError(failure(caught)))
      .finally(() => setBusy(false));
  };

  return (
    <section className="card" style={{ marginBottom: 12 }}>
      <div className="card-head">
        <h2>New project</h2>
        <p className="note">
          {locked
            ? 'You lead the project you open; the coordinator opens projects for any lead.'
            : 'The coordinator opens a project for any lead.'}
        </p>
      </div>
      <div className="card-body stack">
        <div style={{ display: 'grid', gridTemplateColumns: '1.4fr 1fr 1fr 1fr', gap: 12 }}>
          <div className="field">
            <label htmlFor="project-name">Name</label>
            <input
              id="project-name"
              className="input"
              value={value.name}
              onChange={(event) => change({ name: event.target.value })}
            />
          </div>
          <div className="field">
            <label htmlFor="project-lead">Lead account</label>
            <input
              id="project-lead"
              className="input"
              value={value.lead}
              disabled={locked}
              onChange={(event) => change({ lead: event.target.value })}
            />
          </div>
          <div className="field">
            <label htmlFor="project-start">Start</label>
            <input
              id="project-start"
              className="input"
              type="date"
              value={value.startDate}
              onChange={(event) => change({ startDate: event.target.value })}
            />
          </div>
          <div className="field">
            <label htmlFor="project-end">End</label>
            <input
              id="project-end"
              className="input"
              type="date"
              value={value.endDate}
              onChange={(event) => change({ endDate: event.target.value })}
            />
          </div>
        </div>
        {error ? <div className="banner danger">{error}</div> : null}
        <div className="btn-row">
          <button type="button" className="btn btn-primary" disabled={busy} onClick={submit}>
            Create the project
          </button>
        </div>
      </div>
    </section>
  );
}

/**
 * The projects as `GET /api/projects` scopes them: the coordinator reads every project, a lead
 * only its own. Opening a project is the way in — a lead opens their own, the coordinator opens it
 * for any lead — and each name leads to its own screen, where the posts, the gap numbers and the
 * end live.
 */
export default function ProjectsScreen() {
  const navigate = useNavigate();
  const { session } = useOutletContext<{ session: Viewer | null }>();
  const [projects, setProjects] = useState<Project[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [creating, setCreating] = useState(false);

  const load = useCallback(() => {
    let cancelled = false;
    getJson<Project[]>('/api/projects')
      .then((rows) => {
        if (!cancelled) {
          setProjects(rows);
        }
      })
      .catch((caught: unknown) => {
        if (!cancelled) {
          setError(failure(caught));
        }
      });
    return () => {
      cancelled = true;
    };
  }, []);

  useEffect(() => load(), [load]);

  return (
    <div>
      <div className="page-head">
        <div>
          <h1>Projects</h1>
          <div className="lede">
            A lead sees its own projects; the coordinator sees all of them. Nothing here ends a
            project — that is its own screen&apos;s one terminal action.
          </div>
        </div>
        <div className="btn-row">
          {session && !creating ? (
            <button type="button" className="btn btn-primary" onClick={() => setCreating(true)}>
              New project
            </button>
          ) : null}
        </div>
      </div>

      {creating && session ? (
        <NewProject
          key={session.username}
          viewer={session}
          onCreated={(project) => navigate(`/projects/${project.id}`)}
        />
      ) : null}

      {error ? <div className="banner danger">{error}</div> : null}

      <section className="card">
        {projects === null ? (
          error === null ? <p className="note">Reading the projects…</p> : null
        ) : projects.length === 0 ? (
          <p className="note">
            No project has been created yet. A lead opens their own; the coordinator opens it for
            any lead.
          </p>
        ) : (
          <table className="w-full text-left text-sm">
            <thead className="text-slate-500">
              <tr>
                <th className="py-1 font-medium">Project</th>
                <th className="py-1 font-medium">Lead</th>
                <th className="py-1 font-medium">From</th>
                <th className="py-1 font-medium">To</th>
                <th className="py-1 font-medium">State</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {projects.map((project) => (
                <tr key={project.id}>
                  <td className="py-2 pr-2">
                    <Link className="text-slate-800 underline" to={`/projects/${project.id}`}>
                      {project.name}
                    </Link>
                  </td>
                  <td className="py-2 pr-2 text-slate-700">{project.leadName}</td>
                  <td className="py-2 pr-2 text-slate-700">{project.startDate}</td>
                  <td className="py-2 pr-2 text-slate-700">{project.endDate}</td>
                  <td className="py-2 text-slate-700">{projectState(project)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </section>
    </div>
  );
}
