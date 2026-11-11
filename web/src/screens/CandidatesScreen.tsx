import { useEffect, useState } from 'react';
import type { ReactNode } from 'react';
import { useParams } from 'react-router-dom';
import { ApiError, getJson } from '../lib/api';
import type { CandidateSearch, ExclusionRow, FallbackRow, SuitableRow } from '../lib/candidates';
import { bandsAboveLabel, crossRole, gateChips, skillLabel } from '../lib/candidates';

type State =
  | { kind: 'loading' }
  | { kind: 'failed'; status: number; code: string; message: string }
  | { kind: 'ready'; search: CandidateSearch };

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
    <section className="mt-6 rounded-lg border border-slate-200 bg-white p-4 shadow-sm">
      <div className="flex flex-wrap items-baseline justify-between gap-x-4 gap-y-1">
        <h2 className="text-base font-semibold text-slate-900">{title}</h2>
        {note ? <p className="text-xs text-slate-500">{note}</p> : null}
      </div>
      {children}
    </section>
  );
}

function SuitableTable({ rows }: { rows: SuitableRow[] }) {
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
          <th scope="col" className="py-1">Skills</th>
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
            <td className="py-2">
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
          </tr>
        ))}
      </tbody>
    </table>
  );
}

function FallbackTable({ rows }: { rows: FallbackRow[] }) {
  return (
    <table className="mt-3 w-full text-left text-sm">
      <thead className="text-xs uppercase tracking-wide text-slate-500">
        <tr>
          <th scope="col" className="py-1 pr-3">Name</th>
          <th scope="col" className="py-1 pr-3">Employee no</th>
          <th scope="col" className="py-1 pr-3">Role</th>
          <th scope="col" className="py-1 pr-3">Job level</th>
          <th scope="col" className="py-1 pr-3">Above requirement</th>
          <th scope="col" className="py-1">Qualifying skill</th>
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
            <td className="py-2">
              {row.skill} · {row.band}
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

function Ready({ postId, search }: { postId: string; search: CandidateSearch }) {
  const cross = crossRole(search);
  const chips = gateChips(search);

  return (
    <main className="mx-auto max-w-5xl p-8">
      <h1 className="text-2xl font-semibold tracking-tight text-slate-900">
        Candidates for post {postId}
      </h1>
      <p className="mt-1 text-sm text-slate-600">
        {search.suitable.length} suitable · {search.excluded.length} excluded
      </p>

      <Section
        title="Suitable people"
        note="Everyone who passes every gate, by name. No score, no ranking."
      >
        {search.suitable.length === 0 ? (
          <p className="mt-3 text-sm text-slate-600">Nobody of the post's role passes the gates.</p>
        ) : (
          <SuitableTable rows={search.suitable} />
        )}
      </Section>

      {cross.shown ? (
        <Section title="Cross-role fallback" note={cross.reason}>
          {cross.rows.length === 0 ? (
            <p className="mt-3 text-sm text-slate-600">
              Nobody of another role qualifies either — the exclusion list says why.
            </p>
          ) : (
            <FallbackTable rows={cross.rows} />
          )}
        </Section>
      ) : null}

      <Section title="Excluded people" note="Each person's first failing gate, and how many failed each gate.">
        <div className="mt-2 flex flex-wrap gap-1">
          {chips.map((chip) => (
            <Chip key={chip.code} muted={chip.count === 0}>
              {chip.code} {chip.name} × {chip.count}
            </Chip>
          ))}
        </div>
        {search.excluded.length === 0 ? (
          <p className="mt-3 text-sm text-slate-600">Nobody was excluded.</p>
        ) : (
          <ExclusionTable rows={search.excluded} />
        )}
      </Section>
    </main>
  );
}

export default function CandidatesScreen() {
  const { id } = useParams();
  const postId = id ?? '';
  const [state, setState] = useState<State>({ kind: 'loading' });

  useEffect(() => {
    let cancelled = false;
    setState({ kind: 'loading' });
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
      <main className="mx-auto max-w-5xl p-8">
        <h1 className="text-2xl font-semibold tracking-tight text-slate-900">
          Candidates for post {postId}
        </h1>
        <p className="mt-2 text-sm text-slate-600">Searching the pool…</p>
      </main>
    );
  }

  if (state.kind === 'failed') {
    return (
      <main className="mx-auto max-w-5xl p-8">
        <h1 className="text-2xl font-semibold tracking-tight text-slate-900">
          Candidates for post {postId}
        </h1>
        <div className="mt-4 rounded-lg border border-red-200 bg-red-50 p-4 text-sm text-red-800">
          <p className="font-medium">{failureText(state.status) ?? 'The search failed.'}</p>
          <p className="mt-1 text-red-700">
            {state.code}: {state.message}
          </p>
        </div>
      </main>
    );
  }

  return <Ready postId={postId} search={state.search} />;
}
