import { useCallback, useEffect, useState } from 'react';
import { ApiError, getJson, sendJson } from '../lib/api';
import type { Notice } from '../lib/confirmation';
import { noticeLine } from '../lib/confirmation';

function failure(error: unknown): string {
  return error instanceof ApiError ? `${error.code}: ${error.message}` : String(error);
}

/** The coordinator's notice list: every lost proposal with its post, person, reason and time. */
export default function NoticesScreen() {
  const [notices, setNotices] = useState<Notice[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  const load = useCallback(() => {
    let cancelled = false;
    getJson<Notice[]>('/api/notices')
      .then((rows) => {
        if (!cancelled) {
          setNotices(rows);
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

  const dismiss = (id: number) => {
    setBusy(true);
    setError(null);
    sendJson<Notice>('POST', `/api/notices/${id}/dismiss`)
      .then(() => load())
      .catch((caught: unknown) => setError(failure(caught)))
      .finally(() => setBusy(false));
  };

  return (
    <div>
      <h1>作废通知</h1>
      <p className="lede">
        每一条被作废的推荐，以及原因。条目一直保留到手动清除。
      </p>
      {error ? (
        <p className="mt-4 rounded border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-800">
          {error}
        </p>
      ) : null}
      {notices === null ? (
        error === null ? <p className="mt-2 text-sm text-slate-600">正在读取通知…</p> : null
      ) : notices.length === 0 ? (
        <p className="mt-4 text-sm text-slate-600">没有被作废的推荐。</p>
      ) : (
        <ul className="mt-4 space-y-2">
          {notices.map((notice) => (
            <li
              key={notice.id}
              className="flex items-center justify-between gap-2 rounded border border-slate-200 bg-white p-3"
            >
              <span className="text-sm text-slate-800">{noticeLine(notice)}</span>
              <button
                type="button"
                className="rounded border border-slate-300 px-2 py-1 text-sm text-slate-700 disabled:text-slate-400"
                disabled={busy}
                onClick={() => dismiss(notice.id)}
              >
                清除
              </button>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
