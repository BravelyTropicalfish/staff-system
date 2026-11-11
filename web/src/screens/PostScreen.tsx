import { useCallback, useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
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
  confirm: '确认',
  reject: '驳回',
  withdraw: '撤回推荐',
};

function failure(error: unknown): string {
  return error instanceof ApiError ? `${error.code}: ${error.message}` : String(error);
}

function GapPanel({
  post,
  gap,
  busy,
  canRelease,
  onRelease,
}: {
  post: Post;
  gap: PostGap | null;
  busy: boolean;
  canRelease: boolean;
  onRelease: (assignmentId: number) => void;
}) {
  const numbers = gap ? gapNumbers(gap) : { chosen: post.confirmed, required: post.headcount, remaining: post.remaining };
  return (
    <section className="card">
      <h2>缺口</h2>
      <p className="mt-1 text-sm text-slate-700">
        {gap ? gapLabel(gap) : `${numbers.chosen} / ${numbers.required} 已到位`}
      </p>
      {gap && gap.people.length > 0 ? (
        <ul className="mt-2 space-y-1 text-sm text-slate-700">
          {gap.people.map((person) => (
            <li key={person.personNo} className="flex items-center justify-between gap-2">
              <span>{seatLabel(person)}</span>
              {canRelease ? (
                <button
                  type="button"
                  className="rounded border border-slate-300 px-2 py-1 text-sm text-slate-700 disabled:text-slate-400"
                  disabled={busy}
                  onClick={() => onRelease(person.assignmentId)}
                >
                  解除
                </button>
              ) : null}
            </li>
          ))}
        </ul>
      ) : (
        <p className="mt-2 text-sm text-slate-600">尚无确认人员。</p>
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
    <section className="card">
      <h2>推荐</h2>
      {open.length === 0 ? (
        <p className="lede">这个岗位还没有待决策的推荐。</p>
      ) : (
        <ul className="mt-2 space-y-3">
          {open.map((proposal) => (
            <li key={proposal.id} className="rounded border border-slate-200 p-3">
              <p className="text-sm font-medium text-slate-900">{proposalPerson(proposal)}</p>
              <p className="note">
                推荐人 {proposal.proposedBy} · {moment(proposal.proposedAt)}
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
          <h3 className="text-sm font-semibold text-slate-900">历史</h3>
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
      <div>
        <div className="btn-row" style={{ marginBottom: 8 }}>
          <Link className="btn btn-sm" to="/posts">
            ← 返回岗位与缺口
          </Link>
        </div>
        <h1>岗位 {postId}</h1>
        <p className="mt-4 rounded border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-800">
          {loadError}
        </p>
      </div>
    );
  }

  if (!ready || !value) {
    return (
      <div>
        <div className="btn-row" style={{ marginBottom: 8 }}>
          <Link className="btn btn-sm" to="/posts">
            ← 返回岗位与缺口
          </Link>
        </div>
        <h1>岗位 {postId}</h1>
        <p className="mt-2 text-sm text-slate-600">正在读取岗位…</p>
      </div>
    );
  }

  const { post, gap, catalogs, proposals, viewer } = ready;
  const frozen = frozenFields(post) as FrozenField[];
  // 只有岗位所属项目的项目经理能保存：协调员能读这一屏，但看到的是一张只读的岗位。
  const canWrite = viewer.role === 'LEAD';

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
    <div>
      <div className="btn-row" style={{ marginBottom: 8 }}>
        <Link className="btn btn-sm" to="/posts">
          ← 返回岗位与缺口
        </Link>
      </div>
      <h1>
        岗位 {post.id} · {post.project}
      </h1>
      <p className="lede">
        {post.role} · {post.status} · {post.startDate} 至 {post.endDate}
      </p>

      {canWrite ? null : (
        <div className="banner" style={{ marginBottom: 12 }}>
          <div>
            岗位由所属项目的项目经理维护，协调员只读：这里不能保存岗位要求，也不能取消岗位。要改，请用该项目的项目经理账号登录。
          </div>
        </div>
      )}

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
        submitLabel="保存岗位"
        readOnly={!canWrite}
        onChange={(patch) => setValue((current) => (current ? { ...current, ...patch } : current))}
        onSkills={(skills) => setValue((current) => (current ? { ...current, skills } : current))}
        onSubmit={save}
      />

      {canWrite && post.status !== 'cancelled' ? (
        <button
          type="button"
          className="mt-4 rounded border border-red-300 px-3 py-1 text-sm text-red-700 disabled:text-red-300"
          disabled={busy || post.confirmed > 0}
          title={post.confirmed > 0 ? '请先解除已确认人员' : undefined}
          onClick={cancel}
        >
          取消岗位
        </button>
      ) : null}

      <ProposalPanel
        proposals={proposals}
        viewer={viewer.role}
        busy={busy}
        onDecide={decide}
      />

      <GapPanel
        post={post}
        gap={gap}
        busy={busy}
        canRelease={canWrite}
        onRelease={release}
      />
    </div>
  );
}
