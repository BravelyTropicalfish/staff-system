import { useCallback, useEffect, useMemo, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import ProjectForm from '../components/ProjectForm';
import Modal from '../components/Modal';
import { ApiError, getJson, sendJson } from '../lib/api';
import { boardNumbers, canCancelPost, canEditPost, initialPost, postName } from '../lib/board';
import type { InboxProposal, SessionView } from '../lib/confirmation';
import type { Post, PostGap } from '../lib/post';
import { gapLabel, seatLabel } from '../lib/post';
import type { Project } from '../lib/projects';
import { canEnd, endConfirmation, projectState } from '../lib/projects';

type Ready = {
  projects: Project[];
  posts: Post[];
  gaps: PostGap[];
  proposals: InboxProposal[];
  viewer: SessionView;
};

function failure(error: unknown): string {
  return error instanceof ApiError ? `${error.code}: ${error.message}` : String(error);
}

/** 岗位状态带上接口词，和原型一样：`open 空缺`、`filled 已满`。 */
const STATUS_WORD: Record<string, string> = {
  open: 'open 空缺',
  'partially filled': 'partially filled 部分到位',
  filled: 'filled 已满',
  cancelled: 'cancelled 已取消',
};

const STATUS_CLASS: Record<string, string> = {
  open: 'badge',
  'partially filled': 'badge badge-warning',
  filled: 'badge badge-success',
  cancelled: 'badge badge-muted',
};

function StatusBadge({ status }: { status: string }) {
  return <span className={STATUS_CLASS[status] ?? 'badge'}>{STATUS_WORD[status] ?? status}</span>;
}

/**
 * 原型的那一屏：三块 KPI、带结束动作的项目表、岗位表，以及选中岗位的详情。岗位表用来选，
 * 详情读服务端给的数据，并只提供该岗位自己的项目经理可用的动作。
 */
export default function PostsScreen() {
  const navigate = useNavigate();
  const [ready, setReady] = useState<Ready | null>(null);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const [creating, setCreating] = useState(false);
  const [picked, setPicked] = useState<number | null>(null);
  const [ending, setEnding] = useState<number | null>(null);

  const reload = useCallback(() => {
    let cancelled = false;
    Promise.all([
      getJson<Project[]>('/api/projects'),
      getJson<Post[]>('/api/posts'),
      getJson<PostGap[]>('/api/gap'),
      getJson<InboxProposal[]>('/api/recommendations'),
      getJson<SessionView>('/api/session'),
    ])
      .then(([projects, posts, gaps, proposals, viewer]) => {
        if (!cancelled) {
          setReady({ projects, posts, gaps, proposals, viewer });
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
  }, []);

  useEffect(() => reload(), [reload]);

  const numbers = useMemo(
    () => boardNumbers(ready?.gaps ?? [], ready?.proposals ?? []),
    [ready?.gaps, ready?.proposals],
  );

  if (loadError) {
    return (
      <div>
        <h1>岗位与缺口</h1>
        <div className="banner danger" style={{ marginTop: 12 }}>
          {loadError}
        </div>
      </div>
    );
  }

  if (!ready) {
    return (
      <div>
        <h1>岗位与缺口</h1>
        <p className="note" style={{ marginTop: 8 }}>
          正在读取岗位…
        </p>
      </div>
    );
  }

  const { projects, posts, gaps, viewer } = ready;
  const selectedId = posts.some((post) => post.id === picked) ? picked : initialPost(gaps);
  const post = posts.find((row) => row.id === selectedId) ?? null;
  const gap = gaps.find((row) => row.id === selectedId) ?? null;
  const project = post ? projects.find((row) => row.id === post.projectId) ?? null : null;

  const act = (request: Promise<unknown>) => {
    setBusy(true);
    setError(null);
    request
      .then(() => reload())
      .catch((caught: unknown) => setError(failure(caught)))
      .finally(() => {
        setBusy(false);
        setEnding(null);
      });
  };

  return (
    <div>
      <div className="page-head">
        <div>
          <h1>{viewer.role === 'LEAD' ? '我的岗位' : '岗位与缺口'}</h1>
          <div className="lede">
            按岗位显示已选定人员、已到位数量与剩余名额；剩余为 0 的岗位离开缺口列表。
          </div>
        </div>
        <div className="btn-row">
          {/* A project is opened by its own lead; the coordinator only reads projects. */}
          {viewer.role === 'LEAD' ? (
            <button
              type="button"
              className="btn btn-primary"
              disabled={busy || creating}
              onClick={() => setCreating(true)}
            >
              新建项目
            </button>
          ) : null}
        </div>
      </div>

      {creating ? (
        <Modal
          title="新建项目"
          note="项目开好后进入项目页，才能为它新增岗位。"
          onClose={() => setCreating(false)}
        >
          <ProjectForm
            key={viewer.username}
            viewer={viewer}
            bare
            onCreated={(created) => navigate(`/projects/${created.id}`)}
            onCancel={() => setCreating(false)}
          />
        </Modal>
      ) : null}

      {error ? <div className="banner danger">{error}</div> : null}

      <div className="stack">
        <div className="grid-3">
          <section className="card kpi">
            <div className="card-body">
              <div className="muted small">仍缺人岗位</div>
              <div className="stat mono">{numbers.short}</div>
            </div>
          </section>
          <section className="card kpi">
            <div className="card-body">
              <div className="muted small">已确认人员</div>
              <div className="stat mono">{numbers.confirmed}</div>
            </div>
          </section>
          <section className="card kpi">
            <div className="card-body">
              <div className="muted small">待决策推荐</div>
              <div className="stat mono">{numbers.pending}</div>
            </div>
          </section>
        </div>

        <section className="card">
          <div className="card-head">
            <div>
              <h2>项目</h2>
              <div className="note">
                结束项目是终态动作：项目变为 closed，全部岗位变为 cancelled，未决推荐以 post
                cancelled 作废，该项目的占用全部删除，且不可重开。
              </div>
            </div>
            <span className="badge">{projects.length} 个</span>
          </div>
          <div className="card-body tight">
            {ending !== null ? (
              <div className="banner danger stack" style={{ margin: 12 }}>
                <p>
                  {endConfirmation(
                    projects.find((row) => row.id === ending) ?? { name: '' },
                    posts.filter((row) => row.projectId === ending),
                  )}
                </p>
                <div className="btn-row">
                  <button
                    type="button"
                    className="btn btn-danger"
                    disabled={busy}
                    onClick={() => {
                      const row = projects.find((each) => each.id === ending);
                      if (row) {
                        act(sendJson<Project>('POST', `/api/projects/${row.id}/end`));
                      }
                    }}
                  >
                    确认结束
                  </button>
                  <button type="button" className="btn" disabled={busy} onClick={() => setEnding(null)}>
                    保持进行中
                  </button>
                </div>
              </div>
            ) : null}
            {projects.length === 0 ? (
              <p className="empty">
                还没有项目。项目经理可以自己开一个，协调员可以替任意项目经理开。
              </p>
            ) : (
              <table>
                <thead>
                  <tr>
                    <th>项目</th>
                    <th>项目经理</th>
                    <th>起止日期</th>
                    <th>状态</th>
                    <th>操作</th>
                  </tr>
                </thead>
                <tbody>
                  {projects.map((row) => (
                    <tr key={row.id}>
                      <td>
                        <Link className="underline" to={`/projects/${row.id}`}>
                          <strong>{row.name}</strong>
                        </Link>
                        <div className="note mono">{row.id}</div>
                      </td>
                      <td>{row.leadName}</td>
                      <td className="note mono">
                        {row.startDate} ~ {row.endDate}
                      </td>
                      <td>
                        {projectState(row) === '已结束' ? (
                          <span className="badge badge-muted">closed 已结束</span>
                        ) : (
                          <span className="badge badge-success">running 进行中</span>
                        )}
                      </td>
                      <td>
                        {projectState(row) === '已结束' ? (
                          <span className="note">已结束 · 不可重开</span>
                        ) : (
                          <button
                            type="button"
                            className="btn btn-sm btn-danger"
                            disabled={busy || !canEnd(row, viewer)}
                            title={
                              canEnd(row, viewer) ? undefined : '只有本项目的项目经理可以结束'
                            }
                            onClick={() => setEnding(row.id)}
                          >
                            结束项目
                          </button>
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </div>
        </section>

        <div className="grid-kpi">
          <section className="card">
            <div className="card-body tight">
              {posts.length === 0 ? (
                <p className="empty">还没有提出任何岗位。</p>
              ) : (
                <table>
                  <thead>
                    <tr>
                      <th>项目</th>
                      <th>岗位</th>
                      <th>角色 / 最低职级</th>
                      <th className="num">名额</th>
                      <th className="num">已到位</th>
                      <th className="num">剩余</th>
                      <th>状态</th>
                    </tr>
                  </thead>
                  <tbody>
                    {posts.map((row) => {
                      const rowProject = projects.find((each) => each.id === row.projectId) ?? null;
                      return (
                        <tr
                          key={row.id}
                          className={`clickable${row.id === selectedId ? ' selected' : ''}`}
                          onClick={() => setPicked(row.id)}
                        >
                          <td>{rowProject?.name ?? row.project}</td>
                          <td>
                            <strong>{postName(row)}</strong>
                            <div className="note mono">
                              {row.id} · {row.startDate} ~ {row.endDate}
                            </div>
                          </td>
                          <td>
                            <span className="badge">{row.role}</span> {row.minJobLevel}
                          </td>
                          <td className="num">{row.headcount}</td>
                          <td className="num">{row.confirmed}</td>
                          <td className="num">{row.status === 'cancelled' ? '—' : row.remaining}</td>
                          <td>
                            <StatusBadge status={row.status} />
                          </td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              )}
            </div>
          </section>

          {post ? (
            <section className="card">
              <div className="card-head">
                <div>
                  <h2>{postName(post)}</h2>
                  <div className="note mono">
                    {post.id} · {project?.name ?? post.project}
                  </div>
                </div>
                <StatusBadge status={post.status} />
              </div>
              <div className="card-body stack">
                {post.confirmed > 0 ? (
                  <div className="banner warn">
                    <div>
                      <strong>门槛字段已锁定。</strong>已有 {post.confirmed} 名确认人员，角色、职级、
                      <code>required</code> 技能、日期与差旅不可改；先<strong>解除</strong>才能改期
                      改要求，名额只能上调。
                      {viewer.role === 'COORDINATOR' ? '岗位由项目经理维护，协调员只读。' : ''}
                    </div>
                  </div>
                ) : viewer.role === 'COORDINATOR' ? (
                  <div className="banner">
                    <div>岗位由项目经理维护，协调员只读。</div>
                  </div>
                ) : null}

                <dl className="kv">
                  <dt>角色</dt>
                  <dd>
                    <span className="badge">{post.role}</span>
                  </dd>
                  <dt>最低职级</dt>
                  <dd>{post.minJobLevel}</dd>
                  <dt>起止日期</dt>
                  <dd className="mono">
                    {post.startDate} ~ {post.endDate}
                  </dd>
                  <dt>招聘名额</dt>
                  <dd>
                    {post.headcount} · 已到位 {post.confirmed} · 剩余 {post.remaining}
                  </dd>
                  <dt>差旅要求</dt>
                  <dd>{post.travel}</dd>
                </dl>

                <div>
                  <h3 style={{ marginBottom: 6 }}>技能要求</h3>
                  {post.skills.length === 0 ? (
                    <p className="note">这个岗位没有技能要求。</p>
                  ) : (
                    <table>
                      <thead>
                        <tr>
                          <th>技能</th>
                          <th>类型</th>
                          <th>最低 / 目标档位</th>
                        </tr>
                      </thead>
                      <tbody>
                        {post.skills.map((skill) => (
                          <tr key={`${skill.skill}-${skill.kind}`}>
                            <td>{skill.skill}</td>
                            <td>
                              {skill.kind === 'required' ? (
                                <span className="badge badge-solid">required 必备</span>
                              ) : (
                                <span className="badge">bonus 加分 · 仅供参考</span>
                              )}
                            </td>
                            <td>{skill.band ?? skill.target ?? '—'}</td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  )}
                </div>

                <div>
                  <h3 style={{ marginBottom: 6 }}>已确认人员</h3>
                  {gap && gap.people.length > 0 ? (
                    <table>
                      <tbody>
                        {gap.people.map((person) => (
                          <tr key={person.personNo}>
                            <td>{seatLabel(person)}</td>
                            <td style={{ width: 1, whiteSpace: 'nowrap' }}>
                              {canEditPost(post, project, viewer) ? (
                                <button
                                  type="button"
                                  className="btn btn-sm btn-danger"
                                  disabled={busy}
                                  onClick={() =>
                                    act(
                                      sendJson<unknown>(
                                        'POST',
                                        `/api/assignments/${person.assignmentId}/release`,
                                      ),
                                    )
                                  }
                                >
                                  解除
                                </button>
                              ) : null}
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  ) : (
                    <p className="empty">尚无确认人员</p>
                  )}
                </div>

                <div className="btn-row">
                  {viewer.role === 'COORDINATOR' ? (
                    <Link className="btn btn-primary" to={`/posts/${post.id}/candidates`}>
                      搜索候选人
                    </Link>
                  ) : (
                    <button
                      type="button"
                      className="btn btn-primary"
                      disabled
                      title="合适清单只对协调员开放"
                    >
                      搜索候选人
                    </button>
                  )}
                  {canEditPost(post, project, viewer) ? (
                    <Link className="btn" to={`/posts/${post.id}`}>
                      编辑岗位
                    </Link>
                  ) : (
                    <button type="button" className="btn" disabled>
                      编辑岗位
                    </button>
                  )}
                  {canCancelPost(post, project, viewer) ? (
                    <button
                      type="button"
                      className="btn btn-danger"
                      disabled={busy}
                      onClick={() => act(sendJson<Post>('POST', `/api/posts/${post.id}/cancel`))}
                    >
                      取消岗位
                    </button>
                  ) : (
                    <button
                      type="button"
                      className="btn btn-danger"
                      disabled
                      title={
                        post.confirmed > 0 ? '请先解除已确认人员' : '岗位由项目经理取消'
                      }
                    >
                      取消岗位
                    </button>
                  )}
                </div>

                <p className="note">{gapLabel(post)}</p>
              </div>
            </section>
          ) : (
            <section className="card">
              <p className="empty">选择一个岗位查看详情。</p>
            </section>
          )}
        </div>
      </div>
    </div>
  );
}
