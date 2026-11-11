import { useEffect, useState } from 'react';
import type { ReactNode } from 'react';
import { Link, useParams } from 'react-router-dom';
import { ApiError, getJson, sendJson } from '../lib/api';
import type { CandidateSearch, ExclusionRow, FallbackRow, SuitableRow } from '../lib/candidates';
import { bandsAboveLabel, crossRole, gateChips } from '../lib/candidates';
import type { Proposal } from '../lib/confirmation';
import type { Post, PostGap } from '../lib/post';
import { gapLabel } from '../lib/post';
import { postName } from '../lib/board';

type State =
  | { kind: 'loading' }
  | { kind: 'failed'; status: number; code: string; message: string }
  | { kind: 'ready'; search: CandidateSearch; post: Post };

/** 一行要推荐谁：工号是键，姓名是读得懂的那一面。 */
type RecommendTarget = { employeeNo: string; name: string };

function failure(error: unknown): string {
  return error instanceof ApiError ? `${error.code}: ${error.message}` : String(error);
}

function failureText(status: number): string | null {
  if (status === 401) {
    return '请以协调员身份登录后再搜索人员池。';
  }
  if (status === 403) {
    return '只有协调员可以打开合适清单。';
  }
  if (status === 404) {
    return '该岗位不存在。';
  }
  return null;
}

function Badge({ children, muted }: { children: ReactNode; muted?: boolean }) {
  return <span className={`badge${muted ? ' badge-muted' : ''}`}>{children}</span>;
}

/** 仍然缺人的岗位，就是原型打开搜索时那排 chips。 */
function PostChips({
  posts,
  active,
  onPick,
}: {
  posts: { id: number; title: string | null }[];
  active: string;
  onPick: (id: string) => void;
}) {
  return (
    <div className="chips" style={{ marginBottom: 12 }}>
      {posts.map((post) => (
        <button
          key={post.id}
          type="button"
          className={`chip${String(post.id) === active ? ' active' : ''}`}
          onClick={() => onPick(String(post.id))}
        >
          {post.id} · {postName(post)}
        </button>
      ))}
    </div>
  );
}

function Section({
  title,
  note,
  children,
}: {
  title: string;
  note?: string | null;
  children: ReactNode;
}) {
  return (
    <section className="card">
      <div className="card-head">
        <h2>{title}</h2>
        {note ? <p className="note">{note}</p> : null}
      </div>
      <div className="card-body tight">{children}</div>
    </section>
  );
}

function RecommendButton({
  person,
  proposed,
  busy,
  onRecommend,
}: {
  person: RecommendTarget;
  proposed: string[];
  busy: boolean;
  onRecommend: (person: RecommendTarget) => void;
}) {
  const done = proposed.includes(person.employeeNo);
  return (
    <button
      type="button"
      className="btn btn-sm btn-primary"
      disabled={busy || done}
      onClick={() => onRecommend(person)}
    >
      {done ? '已推荐' : '推荐'}
    </button>
  );
}

function SuitableTable({
  rows,
  proposed,
  busy,
  onRecommend,
}: {
  rows: SuitableRow[];
  proposed: string[];
  busy: boolean;
  onRecommend: (person: RecommendTarget) => void;
}) {
  return (
    <table>
      <thead>
        <tr>
          <th>候选人</th>
          <th>工号</th>
          <th>角色 / 职级</th>
          <th>差旅</th>
          <th>通过门槛</th>
          <th>达标技能</th>
          <th>操作</th>
        </tr>
      </thead>
      <tbody>
        {rows.map((row) => (
          <tr key={row.employeeNo}>
            <td>
              <strong>{row.name}</strong>
            </td>
            <td className="note mono">{row.employeeNo}</td>
            <td>
              <Badge>{row.role}</Badge> {row.jobLevel}
            </td>
            <td>{row.travel}</td>
            <td>
              <span className="row" style={{ gap: 4 }}>
                {row.gates.map((gate) => (
                  <Badge key={gate}>{gate}</Badge>
                ))}
              </span>
            </td>
            <td>
              {row.skills.map((skill) => (
                <div key={skill.skill} className="note">
                  <strong>{skill.skill}</strong> {skill.band ?? '未持有'}
                  {skill.kind === 'bonus' ? `（目标 ${skill.target} · 仅供参考）` : ''}
                </div>
              ))}
            </td>
            <td>
              <RecommendButton
                person={{ employeeNo: row.employeeNo, name: row.name }}
                proposed={proposed}
                busy={busy}
                onRecommend={onRecommend}
              />
            </td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}

function FallbackTable({
  rows,
  proposed,
  busy,
  onRecommend,
}: {
  rows: FallbackRow[];
  proposed: string[];
  busy: boolean;
  onRecommend: (person: RecommendTarget) => void;
}) {
  return (
    <table>
      <thead>
        <tr>
          <th>候选人</th>
          <th>工号</th>
          <th>角色 / 职级差</th>
          <th>通过门槛</th>
          <th>达标技能</th>
          <th>操作</th>
        </tr>
      </thead>
      <tbody>
        {rows.map((row) => (
          <tr key={row.employeeNo}>
            <td>
              <strong>{row.name}</strong>
            </td>
            <td className="note mono">{row.employeeNo}</td>
            <td>
              <Badge>{row.role}</Badge>
              <div className="note mono">
                {bandsAboveLabel(row.bandsAbove)} · {row.jobLevel}
              </div>
            </td>
            <td>
              <span className="row" style={{ gap: 4 }}>
                {row.gates.map((gate) => (
                  <Badge key={gate}>{gate}</Badge>
                ))}
              </span>
            </td>
            <td>
              <strong>
                {row.skill} · {row.band}
              </strong>
            </td>
            <td>
              <RecommendButton
                person={{ employeeNo: row.employeeNo, name: row.name }}
                proposed={proposed}
                busy={busy}
                onRecommend={onRecommend}
              />
            </td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}

function ExclusionTable({ rows }: { rows: ExclusionRow[] }) {
  return (
    <table>
      <thead>
        <tr>
          <th>候选人</th>
          <th>工号</th>
          <th>角色</th>
          <th>首个不通过门槛</th>
          <th>原因</th>
        </tr>
      </thead>
      <tbody>
        {rows.map((row) => (
          <tr key={row.employeeNo}>
            <td>
              <strong>{row.name}</strong>
            </td>
            <td className="note mono">{row.employeeNo}</td>
            <td>{row.role}</td>
            <td>
              <Badge>{row.gate}</Badge>
            </td>
            <td className="note">{row.reason}</td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}

function Ready({
  postId,
  search,
  post,
  chips,
  proposed,
  busy,
  note,
  error,
  onRecommend,
}: {
  postId: string;
  search: CandidateSearch;
  post: Post;
  chips: ReactNode;
  proposed: string[];
  busy: boolean;
  note: string | null;
  error: string | null;
  onRecommend: (person: RecommendTarget) => void;
}) {
  const cross = crossRole(search);
  const gateCells = gateChips(search);

  return (
    <div>
      <div className="page-head">
        <div>
          <h1>候选人搜索</h1>
          <div className="lede">
            六道硬门槛按顺序判定，第一个不满足的门槛就是排除理由；通过者全部列出，按姓名升序，
            不评分、不排序。本角色无人时，另给一份跨角色兜底名单。当前岗位 {postId}：
            {search.suitable.length} 人合适 · {search.excluded.length} 人被排除。
          </div>
        </div>
        <div className="btn-row">
          <Link className="btn" to={`/posts/${postId}`}>
            打开岗位
          </Link>
        </div>
      </div>

      {chips}

      <section className="card" style={{ marginBottom: 12 }}>
        <div className="card-body stack">
          <div className="row" style={{ justifyContent: 'space-between' }}>
            <div>
              <strong>{postName(post)}</strong>
              <div className="note mono">
                {post.id} · {post.role} ≥ {post.minJobLevel} · 差旅 {post.travel} ·{' '}
                {gapLabel(post)}
              </div>
            </div>
            <div className="row">
              {post.skills.map((skill) => (
                <span
                  key={`${skill.skill}-${skill.kind}`}
                  className={`badge${skill.kind === 'bonus' ? ' badge-muted' : ''}`}
                >
                  {skill.skill} ≥ {skill.band ?? skill.target}
                  {skill.kind === 'bonus' ? ' · bonus' : ''}
                </span>
              ))}
            </div>
          </div>
        </div>
      </section>

      {note ? <div className="banner">{note}</div> : null}
      {error ? <div className="banner danger">{error}</div> : null}

      <div className="stack">
        <Section title="合适清单" note="通过全部门槛的人，按姓名排列；不评分、不排名。">
          {search.suitable.length === 0 ? (
            <p className="empty">本角色没有候选人通过全部门槛。</p>
          ) : (
            <SuitableTable
              rows={search.suitable}
              proposed={proposed}
              busy={busy}
              onRecommend={onRecommend}
            />
          )}
        </Section>

        {cross.shown ? (
          <Section title="跨角色兜底" note={cross.reason}>
            {cross.rows.length === 0 ? (
              <p className="empty">其他角色也没有人合格 —— 排除清单说明了原因。</p>
            ) : (
              <FallbackTable
                rows={cross.rows}
                proposed={proposed}
                busy={busy}
                onRecommend={onRecommend}
              />
            )}
          </Section>
        ) : null}

        <Section title="被排除的人" note="每个人第一个不通过的门槛，以及每道门槛排除了多少人。">
          <div className="gate-grid" style={{ padding: 12 }}>
            {gateCells.map((chip) => (
              <div className="gate-cell" key={chip.code}>
                <div className="n mono">{chip.count}</div>
                <div className="l">
                  {chip.code} {chip.name}
                </div>
              </div>
            ))}
          </div>
          {search.excluded.length === 0 ? (
            <p className="empty">没有人被排除。</p>
          ) : (
            <ExclusionTable rows={search.excluded} />
          )}
        </Section>
      </div>
    </div>
  );
}

export default function CandidatesScreen() {
  const { id } = useParams();
  const [picked, setPicked] = useState<string | null>(id ?? null);
  // 路由可以点名岗位，也可以由 chips 选；两种方式都只读一个岗位。
  const postId = id ?? picked ?? '';
  const [open, setOpen] = useState<{ id: number; title: string | null }[]>([]);
  const [state, setState] = useState<State>({ kind: 'loading' });
  const [proposed, setProposed] = useState<string[]>([]);
  const [note, setNote] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  // chips 是缺口仍算缺人的岗位；第一个自动打开。
  useEffect(() => {
    let cancelled = false;
    getJson<PostGap[]>('/api/gap')
      .then((rows) => {
        if (cancelled) return;
        const short = rows
          .filter((row) => row.stillShort)
          .map((row) => ({ id: row.id, title: row.title }));
        setOpen(short);
        if (!id && short.length > 0) {
          setPicked((current) => current ?? String(short[0].id));
        }
      })
      .catch(() => {
        // chips 被拒时把搜索交给路由；搜索本身会说明原因。
      });
    return () => {
      cancelled = true;
    };
  }, [id]);

  const recommend = (person: RecommendTarget) => {
    setBusy(true);
    setError(null);
    setNote(null);
    sendJson<{ id: number }>('POST', `/api/posts/${encodeURIComponent(postId)}/recommendations`, {
      personNo: person.employeeNo,
    })
      .then(() => {
        setProposed((current) => [...current, person.employeeNo]);
        setNote(`${person.name} 已推荐给项目经理，由项目经理在岗位上确认或驳回。`);
      })
      .catch((caught: unknown) => setError(failure(caught)))
      .finally(() => setBusy(false));
  };

  useEffect(() => {
    if (postId === '') {
      return;
    }
    let cancelled = false;
    setState({ kind: 'loading' });
    setProposed([]);
    setNote(null);
    setError(null);
    getJson<CandidateSearch>(`/api/posts/${encodeURIComponent(postId)}/candidates`)
      .then(async (search) => {
        // The buttons start from what the server already holds, not from this visit's clicks: a
        // person proposed earlier reads as 已推荐 the moment the screen opens.
        const [post, proposals] = await Promise.all([
          getJson<Post>(`/api/posts/${encodeURIComponent(postId)}`),
          getJson<Proposal[]>(`/api/posts/${encodeURIComponent(postId)}/recommendations`).catch(
            () => [] as Proposal[],
          ),
        ]);
        if (!cancelled) {
          setState({ kind: 'ready', search, post });
          setProposed(
            proposals.filter((row) => row.state === 'proposed').map((row) => row.personNo),
          );
        }
      })
      .catch((error: unknown) => {
        if (cancelled) {
          return;
        }
        if (error instanceof ApiError) {
          setState({ kind: 'failed', status: error.status, code: error.code, message: error.message });
          return;
        }
        setState({
          kind: 'failed',
          status: 0,
          code: 'network',
          message: error instanceof Error ? error.message : String(error),
        });
      });
    return () => {
      cancelled = true;
    };
  }, [postId]);

  if (postId === '') {
    return (
      <div>
        <h1>候选人搜索</h1>
        <p className="note" style={{ marginTop: 8 }}>
          {open.length === 0
            ? '你的项目里没有仍缺人的岗位，暂时无处可搜。'
            : '选择一个岗位开始搜索。'}
        </p>
      </div>
    );
  }

  if (state.kind === 'loading') {
    return (
      <div>
        <h1>候选人搜索</h1>
        <p className="note" style={{ marginTop: 8 }}>
          正在搜索人员池…
        </p>
      </div>
    );
  }

  if (state.kind === 'failed') {
    return (
      <div>
        <h1>候选人搜索</h1>
        <div className="banner danger" style={{ marginTop: 12 }}>
          <p>
            <strong>{failureText(state.status) ?? '搜索失败。'}</strong>
          </p>
          <p className="note">
            {state.code}: {state.message}
          </p>
        </div>
      </div>
    );
  }

  return (
    <Ready
      postId={postId}
      search={state.search}
      post={state.post}
      chips={<PostChips posts={open} active={postId} onPick={setPicked} />}
      proposed={proposed}
      busy={busy}
      note={note}
      error={error}
      onRecommend={recommend}
    />
  );
}
