import { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { ApiError, getJson } from '../lib/api';
import type { PersonSummary } from '../lib/people';
import { stateLabel } from '../lib/people';

function failure(error: unknown): string {
  return error instanceof ApiError ? `${error.code}: ${error.message}` : String(error);
}

/**
 * The pool as `GET /api/people` answers it: everyone by employee number, each with the one role
 * they hold and the two bands the gates read. The list only names and links — the profile is where
 * the skills and the occupancy live, and nothing here writes.
 */
export default function PeopleScreen() {
  const [people, setPeople] = useState<PersonSummary[] | null>(null);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(() => {
    let cancelled = false;
    getJson<PersonSummary[]>('/api/people')
      .then((rows) => {
        if (!cancelled) {
          setPeople(rows);
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

  return (
    <main className="mx-auto max-w-4xl p-8">
      <h1 className="text-2xl font-semibold tracking-tight text-slate-900">People</h1>
      <p className="mt-1 text-sm text-slate-600">
        Everyone the last applied workbook left in the pool. The list is read-only: person data
        arrives by import.
      </p>
      {error ? (
        <p className="mt-4 rounded border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-800">
          {error}
        </p>
      ) : null}

      <section className="mt-6 rounded-lg border border-slate-200 bg-white p-4 shadow-sm">
        {people === null ? (
          error === null ? <p className="text-sm text-slate-600">Reading the pool…</p> : null
        ) : people.length === 0 ? (
          <p className="text-sm text-slate-600">No person has been imported yet.</p>
        ) : (
          <table className="w-full text-left text-sm">
            <thead className="text-slate-500">
              <tr>
                <th className="py-1 font-medium">Employee</th>
                <th className="py-1 font-medium">Name</th>
                <th className="py-1 font-medium">Role</th>
                <th className="py-1 font-medium">Job level</th>
                <th className="py-1 font-medium">Travel</th>
                <th className="py-1 font-medium">State</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {people.map((person) => (
                <tr key={person.employeeNo}>
                  <td className="py-2 pr-2 text-slate-700">{person.employeeNo}</td>
                  <td className="py-2 pr-2">
                    <Link
                      className="text-slate-800 underline"
                      to={`/people/${person.employeeNo}`}
                    >
                      {person.name}
                    </Link>
                  </td>
                  <td className="py-2 pr-2 text-slate-700">{person.role}</td>
                  <td className="py-2 pr-2 text-slate-700">{person.jobLevel}</td>
                  <td className="py-2 pr-2 text-slate-700">{person.travel}</td>
                  <td className="py-2 text-slate-700">{stateLabel(person)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </section>
    </main>
  );
}
