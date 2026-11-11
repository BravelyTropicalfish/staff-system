import { useCallback, useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { ApiError, getJson } from '../lib/api';
import type { PersonProfile } from '../lib/people';
import { bandLabel, occupancyLabel, profileNote } from '../lib/people';

function failure(error: unknown): string {
  return error instanceof ApiError ? `${error.code}: ${error.message}` : String(error);
}

/**
 * One person as `GET /api/people/{employeeNo}` answers them: the master data with the single role,
 * every band by name, and every occupancy — the ended ranges too, marked rather than hidden. The
 * screen renders only; a field here would be a write the server does not have (person data arrives
 * by import), so no control is drawn at all.
 */
export default function PersonScreen() {
  const { employeeNo = '' } = useParams();
  const [person, setPerson] = useState<PersonProfile | null>(null);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(() => {
    let cancelled = false;
    getJson<PersonProfile>(`/api/people/${employeeNo}`)
      .then((row) => {
        if (!cancelled) {
          setPerson(row);
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
  }, [employeeNo]);

  useEffect(() => load(), [load]);

  return (
    <main className="mx-auto max-w-4xl p-8">
      <p className="text-sm">
        <Link className="text-slate-700 underline" to="/people">
          People
        </Link>
      </p>
      {error ? (
        <p className="mt-4 rounded border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-800">
          {error}
        </p>
      ) : null}
      {person === null ? (
        error === null ? <p className="mt-4 text-sm text-slate-600">Reading the profile…</p> : null
      ) : (
        <>
          <h1 className="mt-2 text-2xl font-semibold tracking-tight text-slate-900">
            {person.name}
          </h1>
          <p className="mt-1 text-sm text-slate-600">{profileNote(person)}</p>

          <section className="mt-6 rounded-lg border border-slate-200 bg-white p-4 shadow-sm">
            <h2 className="text-base font-semibold text-slate-900">Master data</h2>
            <dl className="mt-2 grid grid-cols-2 gap-x-4 gap-y-1 text-sm">
              <dt className="text-slate-500">Employee</dt>
              <dd className="text-slate-800">{person.employeeNo}</dd>
              <dt className="text-slate-500">Role</dt>
              <dd className="text-slate-800">{person.role}</dd>
              <dt className="text-slate-500">Job level</dt>
              <dd className="text-slate-800">{person.jobLevel}</dd>
              <dt className="text-slate-500">Travel willingness</dt>
              <dd className="text-slate-800">{person.travel}</dd>
              <dt className="text-slate-500">Email</dt>
              <dd className="text-slate-800">{person.email ?? '—'}</dd>
            </dl>
          </section>

          <section className="mt-6 rounded-lg border border-slate-200 bg-white p-4 shadow-sm">
            <h2 className="text-base font-semibold text-slate-900">Skills</h2>
            {person.skills.length === 0 ? (
              <p className="mt-2 text-sm text-slate-600">No skill is recorded for this person.</p>
            ) : (
              <ul className="mt-2 space-y-1 text-sm text-slate-800">
                {person.skills.map((skill) => (
                  <li key={skill.skill}>{bandLabel(skill)}</li>
                ))}
              </ul>
            )}
          </section>

          <section className="mt-6 rounded-lg border border-slate-200 bg-white p-4 shadow-sm">
            <h2 className="text-base font-semibold text-slate-900">Occupancy</h2>
            {person.allocations.length === 0 ? (
              <p className="mt-2 text-sm text-slate-600">
                No allocation is recorded for this person.
              </p>
            ) : (
              <ul className="mt-2 space-y-1 text-sm text-slate-800">
                {person.allocations.map((allocation) => (
                  <li key={allocation.id}>{occupancyLabel(allocation)}</li>
                ))}
              </ul>
            )}
          </section>
        </>
      )}
    </main>
  );
}
