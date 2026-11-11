import { useCallback, useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { ApiError, getJson, sendJson } from '../lib/api';
import type { SessionView } from '../lib/confirmation';
import type { Post, PostGap } from '../lib/post';
import { gapLabel } from '../lib/post';
import type { Project } from '../lib/projects';
import { canEnd, endConfirmation, gapSummary, projectGap, projectLine, projectNote } from '../lib/projects';

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
      <div>
        <h1>Project {projectId}</h1>
        <p className="mt-4 rounded border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-800">
          {loadError}
        </p>
      </div>
    );
  }

  if (!ready) {
    return (
      <div>
        <h1>Project {projectId}</h1>
        <p className="mt-2 text-sm text-slate-600">Reading the project…</p>
      </div>
    );
  }

  const { project, posts, gaps, viewer } = ready;
  const ended = project.status === 'CLOSED';
  const tally = projectGap(gaps);

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
    <div>
      <div className="page-head">
        <div>
          <h1>{project.name}</h1>
          <div className="lede">{projectLine(project)}</div>
          <div className="lede">{projectNote(project)}</div>
        </div>
        <div className="btn-row">
          {/* Adding a post is the same guard as ending: the project's own lead, while it is open. */}
          {canEnd(project, viewer) ? (
            <Link className="btn btn-sm" to={`/projects/${project.id}/posts/new`}>
              Add a post
            </Link>
          ) : null}
        </div>
      </div>
      {error ? <div className="banner danger">{error}</div> : null}

      <div className="grid-3">
        <section className="card kpi">
          <div className="card-body">
            <div className="muted small">Posts</div>
            <div className="stat mono">{tally.posts}</div>
          </div>
        </section>
        <section className="card kpi">
          <div className="card-body">
            <div className="muted small">Seats chosen</div>
            <div className="stat mono">
              {tally.chosen}
              <small> of {tally.seats}</small>
            </div>
          </div>
        </section>
        <section className="card kpi">
          <div className="card-body">
            <div className="muted small">Seats still needed</div>
            <div className="stat mono">{tally.remaining}</div>
          </div>
        </section>
      </div>

      <div className="grid-2" style={{ marginTop: 12 }}>
        <section className="card">
          <div className="card-head">
            <h2>Posts</h2>
          </div>
          <div className="card-body tight">
            {posts.length === 0 ? (
              <p className="empty">The project carries no post yet.</p>
            ) : (
              <table>
                <thead>
                  <tr>
                    <th>Post</th>
                    <th>Role</th>
                    <th>From</th>
                    <th>To</th>
                    <th>Status</th>
                    <th>Seats</th>
                  </tr>
                </thead>
                <tbody>
                  {posts.map((post) => {
                    const gap = gaps.find((row) => row.id === post.id) ?? null;
                    return (
                      <tr key={post.id}>
                        <td>
                          <Link className="underline" to={`/posts/${post.id}`}>
                            {post.title ?? `Post ${post.id}`}
                          </Link>
                        </td>
                        <td>{post.role}</td>
                        <td className="mono">{post.startDate}</td>
                        <td className="mono">{post.endDate}</td>
                        <td>
                          <span className={`badge ${post.status === 'open' ? 'badge-muted' : 'badge-success'}`}>
                            {post.status}
                          </span>
                        </td>
                        <td className="note mono">{gap ? gapLabel(gap) : '—'}</td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            )}
          </div>
        </section>

        <section className="card">
          <div className="card-head">
            <h2>Gap</h2>
          </div>
          <div className="card-body stack">
            <p className="stat mono">{gapSummary(gaps)}</p>
            {canEnd(project, viewer) ? (
              <div className="banner danger stack">
                {confirming ? (
                  <>
                    <p>{endConfirmation(project, posts)}</p>
                    <div className="btn-row">
                      <button type="button" className="btn btn-danger" disabled={busy} onClick={end}>
                        End it
                      </button>
                      <button
                        type="button"
                        className="btn"
                        disabled={busy}
                        onClick={() => setConfirming(false)}
                      >
                        Keep it open
                      </button>
                    </div>
                  </>
                ) : (
                  <>
                    <p>
                      Ending is terminal: it cancels every post, releases everyone and cannot be undone.
                    </p>
                    <div className="btn-row">
                      <button type="button" className="btn btn-danger" onClick={() => setConfirming(true)}>
                        End the project
                      </button>
                    </div>
                  </>
                )}
              </div>
            ) : ended ? null : (
              <p className="note">
                Only the project's lead ends it; this screen offers the action to nobody else.
              </p>
            )}
          </div>
        </section>
      </div>
    </div>
  );
}
