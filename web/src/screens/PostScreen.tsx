import { useCallback, useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import PostForm from '../components/PostForm';
import type { FrozenField } from '../components/PostForm';
import { ApiError, getJson, sendJson } from '../lib/api';
import type { Bands, Post, PostFormValue, PostGap, RoleOption, SkillOption } from '../lib/post';
import type { Proposal, ProposalAction, SessionView } from '../lib/confirmation';
import {
  evidenceLines,
  gatesLabel,
  moment,
  proposalActions,
  proposalPerson,
  splitProposals,
  stateLabel,
} from '../lib/confirmation';
import {
  formFromPost,
  frozenFields,
  frozenReason,
  gapLabel,
  gapNumbers,
  headcountFloor,
  postDraft,
  seatLabel,
} from '../lib/post';

type Catalogs = { bands: Bands; roles: RoleOption[]; skills: SkillOption[] };
type Ready = {
  post: Post;
  gap: PostGap | null;
  catalogs: Catalogs;
  proposals: Proposal[];
  viewer: SessionView;
};

const ACTION_LABEL: Record<ProposalAction, string> = {
  confirm: 'Confirm',
  reject: 'Reject',
  withdraw: 'Withdraw the proposal',
};

function failure(error: unknown): string {
  return error instanceof ApiError ? `${error.code}: ${error.message}` : String(error);
}

function GapPanel({
  post,
  gap,
  busy,
  onRelease,
}: {
  post: Post;
  gap: PostGap | null;
  busy: boolean;
  onRelease: (assignmentId: number) => void;
}) {
  const numbers = gap ? gapNumbers(gap) : { chosen: post.confirmed, required: post.headcount, remaining: post.remaining };
  return (
    <section className="mt-6 rounded-lg border border-slate-200 bg-white p-4 shadow-sm">
      <h2 className="text-base font-semibold text-slate-900">Gap</h2>
      <p className="mt-1 text-sm text-slate-700">
        {gap ? gapLabel(gap) : `${numbers.chosen} of ${numbers.required} chosen`}
      </p>
      {gap && gap.people.length > 0 ? (
        <ul className="mt-2 space-y-1 text-sm text-slate-700">
          {gap.people.map((person) => (
            <li key={person.personNo} className="flex items-center justify-between gap-2">
              <span>{seatLabel(person)}</span>
              <button
                type="button"
                className="rounded border border-slate-300 px-2 py-1 text-sm text-slate-700 disabled:text-slate-400"
                disabled={busy}
                onClick={() => onRelease(person.assignmentId)}
              >
                Release
              </button>
            </li>
          ))}
        </ul>
      ) : (
        <p className="mt-2 text-sm text-slate-600">Nobody is confirmed on this post yet.</p>
      )}
    </section>
  );
}

/** The proposals on the post: what is waiting, with the verbs this caller may use, and its history. */
function ProposalPanel({
  proposals,
  viewer,
  busy,
  onDecide,
}: {
  proposals: Proposal[];
  viewer: string;
  busy: boolean;
  onDecide: (action: ProposalAction, id: number) => void;
}) {
  const { open, closed } = splitProposals(proposals);
  return (
    <section className="mt-6 rounded-lg border border-slate-200 bg-white p-4 shadow-sm">
      <h2 className="text-base font-semibold text-slate-900">Proposals</h2>
      {open.length === 0 ? (
        <p className="mt-1 text-sm text-slate-600">No proposal is waiting on this post.</p>
      ) : (
        <ul className="mt-2 space-y-3">
          {open.map((proposal) => (
            <li key={proposal.id} className="rounded border border-slate-200 p-3">
              <p className="text-sm font-medium text-slate-900">{proposalPerson(proposal)}</p>
              <p className="text-sm text-slate-600">
                proposed by {proposal.proposedBy} · {moment(proposal.proposedAt)}
              </p>
              {proposal.snapshot ? (
                <div className="mt-1 text-sm text-slate-700">
                  <p>{gatesLabel(proposal.snapshot)}</p>
                  {evidenceLines(proposal.snapshot).map((line) => (
                    <p key={line}>{line}</p>
                  ))}
                </div>
              ) : null}
              <div className="mt-2 flex gap-2">
                {proposalActions(viewer, proposal.state).map((action) => (
                  <button
                    key={action}
                    type="button"
                    className="rounded border border-slate-300 px-2 py-1 text-sm text-slate-700 disabled:text-slate-400"
                    disabled={busy}
                    onClick={() => onDecide(action, proposal.id)}
                  >
                    {ACTION_LABEL[action]}
                  </button>
                ))}
              </div>
            </li>
          ))}
        </ul>
      )}
      {closed.length > 0 ? (
        <div className="mt-4">
          <h3 className="text-sm font-semibold text-slate-900">History</h3>
          <ul className="mt-1 space-y-1 text-sm text-slate-600">
            {closed.map((proposal) => (
              <li key={proposal.id}>
                {proposalPerson(proposal)} · {stateLabel(proposal)}
              </li>
            ))}
          </ul>
        </div>
      ) : null}
    </section>
  );
}

/** One post: its form with the frozen fields disabled and its own gap underneath. */
export default function PostScreen() {
  const { postId = '' } = useParams();
  const [ready, setReady] = useState<Ready | null>(null);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [value, setValue] = useState<PostFormValue | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  const reload = useCallback(() => {
    let cancelled = false;
    getJson<Post>(`/api/posts/${encodeURIComponent(postId)}`)
      .then(async (post) => {
        const [catalogs, gapRows, proposals, viewer] = await Promise.all([
          Promise.all([
            getJson<Bands>('/api/bands'),
            getJson<RoleOption[]>('/api/roles'),
            getJson<SkillOption[]>('/api/skills?active=true'),
          ]).then(([bands, roles, skills]) => ({ bands, roles, skills })),
          getJson<PostGap[]>(`/api/projects/${post.projectId}/gap`),
          getJson<Proposal[]>(`/api/posts/${post.id}/recommendations`),
          getJson<SessionView>('/api/session'),
        ]);
        if (cancelled) {
          return;
        }
        const gap = gapRows.find((row) => row.id === post.id) ?? null;
        setReady({ post, gap, catalogs, proposals, viewer });
        setValue((current) => current ?? formFromPost(post));
      })
      .catch((caught: unknown) => {
        if (!cancelled) {
          setLoadError(failure(caught));
        }
      });
    return () => {
      cancelled = true;
    };
  }, [postId]);

  useEffect(() => reload(), [reload]);

  if (loadError) {
    return (
      <main className="mx-auto max-w-3xl p-8">
        <h1 className="text-2xl font-semibold tracking-tight text-slate-900">Post {postId}</h1>
        <p className="mt-4 rounded border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-800">
          {loadError}
        </p>
      </main>
    );
  }

  if (!ready || !value) {
    return (
      <main className="mx-auto max-w-3xl p-8">
        <h1 className="text-2xl font-semibold tracking-tight text-slate-900">Post {postId}</h1>
        <p className="mt-2 text-sm text-slate-600">Reading the post…</p>
      </main>
    );
  }

  const { post, gap, catalogs, proposals, viewer } = ready;
  const frozen = frozenFields(post) as FrozenField[];

  const save = () => {
    setBusy(true);
    setError(null);
    sendJson<Post>('PATCH', `/api/posts/${post.id}`, postDraft(value))
      .then((saved) => {
        setValue(formFromPost(saved));
        setReady((current) => (current ? { ...current, post: saved } : current));
        reload();
      })
      .catch((caught: unknown) => setError(failure(caught)))
      .finally(() => setBusy(false));
  };

  const cancel = () => {
    setBusy(true);
    setError(null);
    sendJson<Post>('POST', `/api/posts/${post.id}/cancel`)
      .then(() => reload())
      .catch((caught: unknown) => setError(failure(caught)))
      .finally(() => setBusy(false));
  };

  const decide = (action: ProposalAction, id: number) => {
    setBusy(true);
    setError(null);
    const path = `/api/recommendations/${id}`;
    const request =
      action === 'confirm'
        ? sendJson<unknown>('POST', `${path}/confirm`)
        : action === 'reject'
          ? sendJson<unknown>('POST', `${path}/reject`, { note: null })
          : sendJson<unknown>('DELETE', path);
    request
      .then(() => reload())
      .catch((caught: unknown) => setError(failure(caught)))
      .finally(() => setBusy(false));
  };

  const release = (assignmentId: number) => {
    setBusy(true);
    setError(null);
    sendJson<unknown>('POST', `/api/assignments/${assignmentId}/release`)
      .then(() => reload())
      .catch((caught: unknown) => setError(failure(caught)))
      .finally(() => setBusy(false));
  };

  return (
    <main className="mx-auto max-w-3xl p-8">
      <h1 className="text-2xl font-semibold tracking-tight text-slate-900">
        Post {post.id} · {post.project}
      </h1>
      <p className="mt-1 text-sm text-slate-600">
        {post.role} · {post.status} · {post.startDate} to {post.endDate}
      </p>

      <PostForm
        value={value}
        bands={catalogs.bands}
        roles={catalogs.roles}
        skills={catalogs.skills}
        frozen={frozen}
        frozenReason={frozenReason(post)}
        floor={headcountFloor(post)}
        busy={busy}
        error={error}
        submitLabel="Save the post"
        onChange={(patch) => setValue((current) => (current ? { ...current, ...patch } : current))}
        onSkills={(skills) => setValue((current) => (current ? { ...current, skills } : current))}
        onSubmit={save}
      />

      {post.status === 'cancelled' ? null : (
        <button
          type="button"
          className="mt-4 rounded border border-red-300 px-3 py-1 text-sm text-red-700 disabled:text-red-300"
          disabled={busy || post.confirmed > 0}
          title={post.confirmed > 0 ? 'release the confirmed people first' : undefined}
          onClick={cancel}
        >
          Cancel the post
        </button>
      )}

      <ProposalPanel
        proposals={proposals}
        viewer={viewer.role}
        busy={busy}
        onDecide={decide}
      />

      <GapPanel post={post} gap={gap} busy={busy} onRelease={release} />
    </main>
  );
}
