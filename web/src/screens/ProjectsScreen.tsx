import { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { ApiError, getJson } from '../lib/api';
import type { Project } from '../lib/projects';
import { projectState } from '../lib/projects';

function failure(error: unknown): string {
  return error instanceof ApiError ? `${error.code}: ${error.message}` : String(error);
}

/**
 * The projects as `GET /api/projects` scopes them: the coordinator reads every project, a lead
 * only its own. The list names each project and links to its own screen, where the posts, the gap
 * numbers and the end live.
 */
export default function ProjectsScreen() {
  const [projects, setProjects] = useState<Project[] | null>(null);
  const [error, setError] = useState<string | null>(null);

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
    <main className="mx-auto max-w-4xl p-8">
      <h1 className="text-2xl font-semibold tracking-tight text-slate-900">Projects</h1>
      <p className="mt-1 text-sm text-slate-600">
        A lead sees its own projects; the coordinator sees all of them. Nothing here ends a project
        — that is its own screen's one terminal action.
      </p>
      {error ? (
        <p className="mt-4 rounded border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-800">
          {error}
        </p>
      ) : null}

      <section className="mt-6 rounded-lg border border-slate-200 bg-white p-4 shadow-sm">
        {projects === null ? (
          error === null ? <p className="text-sm text-slate-600">Reading the projects…</p> : null
        ) : projects.length === 0 ? (
          <p className="text-sm text-slate-600">No project has been created yet.</p>
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
    </main>
  );
}
