import { useCallback, useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { ApiError, getJson, sendJson } from '../lib/api';
import type { SessionView } from '../lib/confirmation';
import type { Post, PostGap } from '../lib/post';
import { gapLabel } from '../lib/post';
import type { Project } from '../lib/projects';
import { canEnd, endConfirmation, gapSummary, projectLine, projectNote } from '../lib/projects';

type Ready = { project: Project; posts: Post[]; gaps: PostGap[]; viewer: SessionView };

function failure(error: unknown): string {
  return error instanceof ApiError ? `${error.code}: ${error.message}` : String(error);
}

/**
 * One project: its own data, its posts with the gap numbers the server computed, and the one
 * terminal action. The end is asked for twice — the button opens the consequence, the confirmation
 * sends it — because it cancels every post and releases everyone and cannot be undone.
 */
export default function ProjectScreen() {
  const { projectId = '' } = useParams();
  const [ready, setReady] = useState<Ready | null>(null);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [confirming, setConfirming] = useState(false);
  const [busy, setBusy] = useState(false);

  const reload = useCallback(() => {
    let cancelled = false;
    getJson<Project>(`/api/projects/${encodeURIComponent(projectId)}`)
      .then(async (project) => {
        const [posts, gaps, viewer] = await Promise.all([
          getJson<Post[]>(`/api/projects/${project.id}/posts`),
          getJson<PostGap[]>(`/api/projects/${project.id}/gap`),
          getJson<SessionView>('/api/session'),
        ]);
        if (!cancelled) {
          setReady({ project, posts, gaps, viewer });
        }
      })
      .catch((caught: unknown) => {
        if (!cancelled) {
          setLoadError(failure(caught));
        }
      });
    return () => {
      cancelled = true;
    };
  }, [projectId]);

  useEffect(() => reload(), [reload]);

  if (loadError) {
    return (
      <main className="mx-auto max-w-4xl p-8">
        <h1 className="text-2xl font-semibold tracking-tight text-slate-900">Project {projectId}</h1>
        <p className="mt-4 rounded border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-800">
          {loadError}
        </p>
      </main>
    );
  }

  if (!ready) {
    return (
      <main className="mx-auto max-w-4xl p-8">
        <h1 className="text-2xl font-semibold tracking-tight text-slate-900">Project {projectId}</h1>
        <p className="mt-2 text-sm text-slate-600">Reading the project…</p>
      </main>
    );
  }

  const { project, posts, gaps, viewer } = ready;
  const ended = project.status === 'CLOSED';

  const end = () => {
    setBusy(true);
    setError(null);
    sendJson<Project>('POST', `/api/projects/${project.id}/end`)
      .then((closed) => {
        setConfirming(false);
        setReady((current) => (current ? { ...current, project: closed } : current));
        reload();
      })
      .catch((caught: unknown) => setError(failure(caught)))
      .finally(() => setBusy(false));
  };

  return (
    <main className="mx-auto max-w-4xl p-8">
      <p className="text-sm">
        <Link className="text-slate-700 underline" to="/projects">
          Projects
        </Link>
      </p>
      <h1 className="mt-2 text-2xl font-semibold tracking-tight text-slate-900">{project.name}</h1>
      <p className="mt-1 text-sm text-slate-600">{projectLine(project)}</p>
      <p className="mt-1 text-sm text-slate-600">{projectNote(project)}</p>
      {error ? (
        <p className="mt-4 rounded border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-800">
          {error}
        </p>
      ) : null}

      <section className="mt-6 rounded-lg border border-slate-200 bg-white p-4 shadow-sm">
        <h2 className="text-base font-semibold text-slate-900">Gap</h2>
        <p className="mt-1 text-sm text-slate-700">{gapSummary(gaps)}</p>
      </section>

      <section className="mt-6 rounded-lg border border-slate-200 bg-white p-4 shadow-sm">
        <div className="flex items-center justify-between gap-2">
          <h2 className="text-base font-semibold text-slate-900">Posts</h2>
          {/* Adding a post is the same guard as ending: the project's own lead, while it is open. */}
          {canEnd(project, viewer) ? (
            <Link
              className="text-sm text-slate-700 underline"
              to={`/projects/${project.id}/posts/new`}
            >
              Add a post
            </Link>
          ) : null}
        </div>
        {posts.length === 0 ? (
          <p className="mt-2 text-sm text-slate-600">The project carries no post yet.</p>
        ) : (
          <table className="mt-2 w-full text-left text-sm">
            <thead className="text-slate-500">
              <tr>
                <th className="py-1 font-medium">Post</th>
                <th className="py-1 font-medium">Role</th>
                <th className="py-1 font-medium">From</th>
                <th className="py-1 font-medium">To</th>
                <th className="py-1 font-medium">Status</th>
                <th className="py-1 font-medium">Seats</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {posts.map((post) => {
                const gap = gaps.find((row) => row.id === post.id) ?? null;
                return (
                  <tr key={post.id}>
                    <td className="py-2 pr-2">
                      <Link className="text-slate-800 underline" to={`/posts/${post.id}`}>
                        {post.title ?? `Post ${post.id}`}
                      </Link>
                    </td>
                    <td className="py-2 pr-2 text-slate-700">{post.role}</td>
                    <td className="py-2 pr-2 text-slate-700">{post.startDate}</td>
                    <td className="py-2 pr-2 text-slate-700">{post.endDate}</td>
                    <td className="py-2 pr-2 text-slate-700">{post.status}</td>
                    <td className="py-2 text-slate-700">{gap ? gapLabel(gap) : '—'}</td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        )}
      </section>

      {canEnd(project, viewer) ? (
        <section className="mt-6 rounded-lg border border-red-200 bg-red-50 p-4">
          {confirming ? (
            <>
              <p className="text-sm text-red-900">{endConfirmation(project, posts)}</p>
              <div className="mt-2 flex gap-2">
                <button
                  type="button"
                  className="rounded border border-red-400 bg-white px-3 py-1 text-sm text-red-700 disabled:text-red-300"
                  disabled={busy}
                  onClick={end}
                >
                  End it
                </button>
                <button
                  type="button"
                  className="rounded border border-slate-300 bg-white px-3 py-1 text-sm text-slate-700 disabled:text-slate-400"
                  disabled={busy}
                  onClick={() => setConfirming(false)}
                >
                  Keep it open
                </button>
              </div>
            </>
          ) : (
            <>
              <p className="text-sm text-red-900">
                Ending is terminal: it cancels every post, releases everyone and cannot be undone.
              </p>
              <button
                type="button"
                className="mt-2 rounded border border-red-400 bg-white px-3 py-1 text-sm text-red-700"
                onClick={() => setConfirming(true)}
              >
                End the project
              </button>
            </>
          )}
        </section>
      ) : ended ? null : (
        <p className="mt-6 text-sm text-slate-600">
          Only the project's lead ends it; this screen offers the action to nobody else.
        </p>
      )}
    </main>
  );
}
