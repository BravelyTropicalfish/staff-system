import { useEffect, useState } from 'react';
import type { ReactNode } from 'react';
import { Link, useParams } from 'react-router-dom';
import { ApiError, getJson, sendJson } from '../lib/api';
import type { CandidateSearch, ExclusionRow, FallbackRow, SuitableRow } from '../lib/candidates';
import { bandsAboveLabel, crossRole, gateChips, skillLabel } from '../lib/candidates';

type State =
  | { kind: 'loading' }
  | { kind: 'failed'; status: number; code: string; message: string }
  | { kind: 'ready'; search: CandidateSearch };

/** Who a row proposes: the employee number is the key, the name is what the reader recognises. */
type RecommendTarget = { employeeNo: string; name: string };

function failure(error: unknown): string {
  return error instanceof ApiError ? `${error.code}: ${error.message}` : String(error);
}

function failureText(status: number): string | null {
  if (status === 401) {
    return 'Sign in as the coordinator to search the pool.';
  }
  if (status === 403) {
    return 'Only the coordinator can open the suitable list.';
  }
  if (status === 404) {
    return 'That post does not exist.';
  }
  return null;
}

function Chip({ children, muted }: { children: ReactNode; muted?: boolean }) {
  return (
    <span
      className={
        'inline-flex items-center rounded border px-1.5 py-0.5 text-xs ' +
        (muted
          ? 'border-slate-200 bg-slate-50 text-slate-400'
          : 'border-slate-300 bg-white text-slate-700')
      }
    >
      {children}
    </span>
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
      {done ? 'Proposed' : 'Recommend'}
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
    <table className="mt-3 w-full text-left text-sm">
      <thead className="text-xs uppercase tracking-wide text-slate-500">
        <tr>
          <th scope="col" className="py-1 pr-3">Name</th>
          <th scope="col" className="py-1 pr-3">Employee no</th>
          <th scope="col" className="py-1 pr-3">Role</th>
          <th scope="col" className="py-1 pr-3">Job level</th>
          <th scope="col" className="py-1 pr-3">Travel</th>
          <th scope="col" className="py-1 pr-3">Gates passed</th>
          <th scope="col" className="py-1 pr-3">Skills</th>
          <th scope="col" className="py-1">Decision</th>
        </tr>
      </thead>
      <tbody>
        {rows.map((row) => (
          <tr key={row.employeeNo} className="border-t border-slate-100 align-top">
            <td className="py-2 pr-3 font-medium text-slate-900">{row.name}</td>
            <td className="py-2 pr-3 text-slate-600">{row.employeeNo}</td>
            <td className="py-2 pr-3">{row.role}</td>
            <td className="py-2 pr-3">{row.jobLevel}</td>
            <td className="py-2 pr-3">{row.travel}</td>
            <td className="py-2 pr-3">
              <span className="flex flex-wrap gap-1">
                {row.gates.map((gate) => (
                  <Chip key={gate}>{gate}</Chip>
                ))}
              </span>
            </td>
            <td className="py-2 pr-3">
              <ul className="space-y-0.5">
                {row.skills.map((skill) => (
                  <li
                    key={skill.skill}
                    className={skill.kind === 'bonus' ? 'text-slate-500' : 'text-slate-800'}
                  >
                    {skillLabel(skill)}
                    {skill.kind === 'bonus' ? ' · bonus' : ''}
                  </li>
                ))}
              </ul>
            </td>
            <td className="py-2">
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
    <table className="mt-3 w-full text-left text-sm">
      <thead className="text-xs uppercase tracking-wide text-slate-500">
        <tr>
          <th scope="col" className="py-1 pr-3">Name</th>
          <th scope="col" className="py-1 pr-3">Employee no</th>
          <th scope="col" className="py-1 pr-3">Role</th>
          <th scope="col" className="py-1 pr-3">Job level</th>
          <th scope="col" className="py-1 pr-3">Above requirement</th>
          <th scope="col" className="py-1 pr-3">Qualifying skill</th>
          <th scope="col" className="py-1">Decision</th>
        </tr>
      </thead>
      <tbody>
        {rows.map((row) => (
          <tr key={row.employeeNo} className="border-t border-slate-100 align-top">
            <td className="py-2 pr-3 font-medium text-slate-900">{row.name}</td>
            <td className="py-2 pr-3 text-slate-600">{row.employeeNo}</td>
            <td className="py-2 pr-3">
              <Chip>{row.role}</Chip>
            </td>
            <td className="py-2 pr-3">{row.jobLevel}</td>
            <td className="py-2 pr-3">{bandsAboveLabel(row.bandsAbove)}</td>
            <td className="py-2 pr-3">
              {row.skill} · {row.band}
            </td>
            <td className="py-2">
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
    <table className="mt-3 w-full text-left text-sm">
      <thead className="text-xs uppercase tracking-wide text-slate-500">
        <tr>
          <th scope="col" className="py-1 pr-3">Name</th>
          <th scope="col" className="py-1 pr-3">Employee no</th>
          <th scope="col" className="py-1 pr-3">Role</th>
          <th scope="col" className="py-1 pr-3">First failing gate</th>
          <th scope="col" className="py-1">Reason</th>
        </tr>
      </thead>
      <tbody>
        {rows.map((row) => (
          <tr key={row.employeeNo} className="border-t border-slate-100 align-top">
            <td className="py-2 pr-3 font-medium text-slate-900">{row.name}</td>
            <td className="py-2 pr-3 text-slate-600">{row.employeeNo}</td>
            <td className="py-2 pr-3">{row.role}</td>
            <td className="py-2 pr-3">
              <Chip>{row.gate}</Chip>
            </td>
            <td className="py-2 text-slate-700">{row.reason}</td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}

function Ready({
  postId,
  search,
  proposed,
  busy,
  note,
  error,
  onRecommend,
}: {
  postId: string;
  search: CandidateSearch;
  proposed: string[];
  busy: boolean;
  note: string | null;
  error: string | null;
  onRecommend: (person: RecommendTarget) => void;
}) {
  const cross = crossRole(search);
  const chips = gateChips(search);

  return (
    <div>
      <div className="page-head">
        <div>
          <h1>Candidates for post {postId}</h1>
          <div className="lede">
            {search.suitable.length} suitable · {search.excluded.length} excluded
          </div>
        </div>
        <div className="btn-row">
          <Link className="btn" to={`/posts/${postId}`}>
            Open the post
          </Link>
        </div>
      </div>

      {note ? <div className="banner">{note}</div> : null}
      {error ? <div className="banner danger">{error}</div> : null}

      <section className="card" style={{ marginBottom: 12 }}>
        <div className="card-head">
          <h2>Gate counts</h2>
          <p className="note">How many people each gate turned away, in the spec's order.</p>
        </div>
        <div className="card-body">
          <div className="gate-grid">
            {chips.map((chip) => (
              <div className="gate-cell" key={chip.code}>
                <div className="n mono">{chip.count}</div>
                <div className="l">
                  {chip.code} {chip.name}
                </div>
              </div>
            ))}
          </div>
        </div>
      </section>

      <div className="stack">
        <Section
          title="Suitable people"
          note="Everyone who passes every gate, by name. No score, no ranking."
        >
          {search.suitable.length === 0 ? (
            <p className="empty">Nobody of the post's role passes the gates.</p>
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
          <Section title="Cross-role fallback" note={cross.reason}>
            {cross.rows.length === 0 ? (
              <p className="empty">
                Nobody of another role qualifies either — the exclusion list says why.
              </p>
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

        <Section
          title="Excluded people"
          note="Each person's first failing gate, and how many failed each gate."
        >
          {search.excluded.length === 0 ? (
            <p className="empty">Nobody was excluded.</p>
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
  const postId = id ?? '';
  const [state, setState] = useState<State>({ kind: 'loading' });
  const [proposed, setProposed] = useState<string[]>([]);
  const [note, setNote] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  const recommend = (person: RecommendTarget) => {
    setBusy(true);
    setError(null);
    setNote(null);
    sendJson<{ id: number }>('POST', `/api/posts/${encodeURIComponent(postId)}/recommendations`, {
      personNo: person.employeeNo,
    })
      .then(() => {
        setProposed((current) => [...current, person.employeeNo]);
        setNote(
          `${person.name} is recommended to the project lead, who confirms or rejects it on the post.`,
        );
      })
      .catch((caught: unknown) => setError(failure(caught)))
      .finally(() => setBusy(false));
  };

  useEffect(() => {
    let cancelled = false;
    setState({ kind: 'loading' });
    setProposed([]);
    setNote(null);
    setError(null);
    getJson<CandidateSearch>(`/api/posts/${encodeURIComponent(postId)}/candidates`)
      .then((search) => {
        if (!cancelled) {
          setState({ kind: 'ready', search });
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

  if (state.kind === 'loading') {
    return (
      <div>
        <h1>
          Candidates for post {postId}
        </h1>
        <p className="mt-2 text-sm text-slate-600">Searching the pool…</p>
      </div>
    );
  }

  if (state.kind === 'failed') {
    return (
      <div>
        <h1>
          Candidates for post {postId}
        </h1>
        <div className="mt-4 rounded-lg border border-red-200 bg-red-50 p-4 text-sm text-red-800">
          <p className="font-medium">{failureText(state.status) ?? 'The search failed.'}</p>
          <p className="mt-1 text-red-700">
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
      proposed={proposed}
      busy={busy}
      note={note}
      error={error}
      onRecommend={recommend}
    />
  );
}
