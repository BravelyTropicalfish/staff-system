import { useCallback, useEffect, useState } from 'react';
import { Link, useOutletContext } from 'react-router-dom';
import { ApiError, getJson, sendJson } from '../lib/api';
import type {
  InboxFilter,
  InboxProposal,
  Notice,
  ProposalAction,
  SessionView,
} from '../lib/confirmation';
import {
  INBOX_FILTERS,
  evidenceLines,
  gatesLabel,
  inboxCounts,
  inboxPage,
  inboxPost,
  moment,
  noticeLine,
  proposalActions,
  proposalPerson,
  reasonLabel,
  stateLabel,
} from '../lib/confirmation';

function failure(error: unknown): string {
  return error instanceof ApiError ? `${error.code}: ${error.message}` : String(error);
}

const ACTION_LABEL: Record<ProposalAction, string> = {
  confirm: '确认',
  reject: '驳回',
  withdraw: '撤回推荐',
};

const STATE_CLASS: Record<string, string> = {
  proposed: 'badge badge-warning',
  rejected: 'badge badge-muted',
  voided: 'badge badge-danger',
};

/**
 * 原型画的决策收件箱：按状态分页的推荐，每条带着写入时的理由快照，以及当前角色可用的动作。
 * 项目经理在自己项目的推荐上确认或驳回；协调员只撤回自己发起的推荐，从不确认。协调员的作废
 * 通知也在这一屏 —— 它们说的就是上面这些推荐。
 */
export default function RecommendationsScreen() {
  const { session } = useOutletContext<{ session: SessionView | null }>();
  const [rows, setRows] = useState<InboxProposal[] | null>(null);
  const [notices, setNotices] = useState<Notice[] | null>(null);
  const [filter, setFilter] = useState<InboxFilter>('proposed');
  const [error, setError] = useState<string | null>(null);
  const [note, setNote] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  const load = useCallback(() => {
    let cancelled = false;
    getJson<InboxProposal[]>('/api/recommendations')
      .then(async (inbox) => {
        if (cancelled) return;
        setRows(inbox);
        // 作废通知属于协调员；项目经理会被拒，所以只在协调员时请求。
        const mine =
          session?.role === 'COORDINATOR'
            ? await getJson<Notice[]>('/api/notices').catch(() => [])
            : [];
        if (!cancelled) setNotices(mine);
      })
      .catch((caught: unknown) => {
        if (!cancelled) setError(failure(caught));
      });
    return () => {
      cancelled = true;
    };
  }, [session?.role]);

  useEffect(() => load(), [load]);

  const decide = (action: ProposalAction, id: number) => {
    setBusy(true);
    setError(null);
    setNote(null);
    const path = `/api/recommendations/${id}`;
    const request =
      action === 'confirm'
        ? sendJson<unknown>('POST', `${path}/confirm`)
        : action === 'reject'
          ? sendJson<unknown>('POST', `${path}/reject`, { note: null })
          : sendJson<unknown>('DELETE', path);
    request
      .then(() => {
        setNote(
          action === 'confirm'
            ? '已确认：写入指派与占用，剩余名额 −1。'
            : action === 'reject'
              ? '已驳回：推荐关闭，协调员在历史里读到它。'
              : '已撤回：推荐关闭，项目经理不再需要决策。',
        );
        load();
      })
      .catch((caught: unknown) => setError(failure(caught)))
      .finally(() => setBusy(false));
  };

  const dismiss = (id: number) => {
    setBusy(true);
    setError(null);
    sendJson<Notice>('POST', `/api/notices/${id}/dismiss`)
      .then(() => load())
      .catch((caught: unknown) => setError(failure(caught)))
      .finally(() => setBusy(false));
  };

  if (rows === null) {
    return (
      <div>
        <h1>推荐与确认</h1>
        <p className="note" style={{ marginTop: 8 }}>
          {error ?? '正在读取推荐…'}
        </p>
      </div>
    );
  }

  const coordinator = session?.role === 'COORDINATOR';
  const counts = inboxCounts(rows);
  const page = inboxPage(rows, filter);

  return (
    <div>
      <div className="page-head">
        <div>
          <h1>{session?.role === 'LEAD' ? '推荐给我的人' : '推荐与确认'}</h1>
          <div className="lede">
            {session?.role === 'LEAD'
              ? '只有推荐到本项目的候选人才可见，带推荐时的门槛结果与达标技能；确认后写入指派与占用。'
              : '推荐由协调员发起，确认或驳回由项目经理执行；确认后写入指派与占用。'}
          </div>
        </div>
        <div className="btn-row">
          <Link className="btn" to="/posts">
            岗位与缺口
          </Link>
        </div>
      </div>

      {coordinator ? (
        <div className="banner" style={{ marginBottom: 12 }}>
          <div>协调员不能确认推荐，只能撤回；确认动作只属于岗位所属项目的项目经理。</div>
        </div>
      ) : null}
      {note ? <div className="banner" style={{ marginBottom: 12 }}>{note}</div> : null}
      {error ? <div className="banner danger" style={{ marginBottom: 12 }}>{error}</div> : null}

      <div className="stack">
        <div className="chips">
          {INBOX_FILTERS.map((chip) => (
            <button
              key={chip.state}
              type="button"
              className={`chip${filter === chip.state ? ' active' : ''}`}
              onClick={() => setFilter(chip.state)}
            >
              {chip.label} ({counts[chip.state]})
            </button>
          ))}
        </div>

        {page.length === 0 ? (
          <section className="card">
            <p className="empty">没有该状态的推荐。</p>
          </section>
        ) : (
          <div className="rec-grid">
            {page.map((row) => (
              <article className="card" key={row.id}>
                <div className="card-body stack">
                  <div className="row" style={{ justifyContent: 'space-between' }}>
                    <div>
                      <strong>{proposalPerson(row)}</strong>{' '}
                      {row.snapshot?.crossRole ? (
                        <span className="badge badge-warning">
                          跨角色 · 高 {row.snapshot.crossRole.bandsAbove} 档
                        </span>
                      ) : null}
                      <div className="note">
                        {inboxPost(row)} · {row.postId} · {row.project} · 推荐人{' '}
                        {row.proposedBy} · {moment(row.proposedAt)}
                      </div>
                    </div>
                    <span className={STATE_CLASS[row.state] ?? 'badge'}>{stateLabel(row)}</span>
                  </div>

                  <div className="row" style={{ gap: 16, alignItems: 'flex-start' }}>
                    <div style={{ flex: '1 1 320px' }}>
                      <div className="note">推荐时快照 · 门槛结果与达标技能</div>
                      {row.snapshot ? (
                        <>
                          <div>{gatesLabel(row.snapshot)}</div>
                          {evidenceLines(row.snapshot).map((line) => (
                            <div key={line} className="note">
                              {line}
                            </div>
                          ))}
                        </>
                      ) : (
                        <div className="note">
                          快照只随协调员可见；这里读到的是人员与岗位本身。
                        </div>
                      )}
                    </div>
                    <div className="note" style={{ flex: '1 1 240px' }}>
                      快照是推荐那一刻的门槛结果与技能档位，之后数据变化不会改写它，也不含任何分数。
                    </div>
                  </div>

                  <div className="btn-row">
                    {proposalActions(session?.role ?? '', row.state).map((action) => (
                      <button
                        key={action}
                        type="button"
                        className={
                          action === 'confirm' ? 'btn btn-primary btn-sm' : 'btn btn-sm'
                        }
                        disabled={busy}
                        onClick={() => decide(action, row.id)}
                      >
                        {ACTION_LABEL[action]}
                      </button>
                    ))}
                    {row.state === 'voided' ? (
                      <span className="note">作废原因：{row.voidReason ? reasonLabel(row.voidReason) : '—'}</span>
                    ) : null}
                  </div>
                </div>
              </article>
            ))}
          </div>
        )}

        {coordinator ? (
          <section className="card">
            <div className="card-head">
              <h2>作废通知</h2>
              <span className="note">保留到手动清除，不自动过期</span>
            </div>
            <div className="card-body tight">
              {notices === null || notices.length === 0 ? (
                <p className="empty">没有被作废的推荐。</p>
              ) : (
                <div className="list">
                  {notices.map((notice) => (
                    <div className="list-item" key={notice.id}>
                      <span className="badge badge-danger">{reasonLabel(notice.reason)}</span>
                      <div style={{ flex: 1 }}>
                        <span>{noticeLine(notice)}</span>
                      </div>
                      <button
                        type="button"
                        className="btn btn-sm"
                        disabled={busy}
                        onClick={() => dismiss(notice.id)}
                      >
                        清除
                      </button>
                    </div>
                  ))}
                </div>
              )}
            </div>
          </section>
        ) : null}
      </div>
    </div>
  );
}
