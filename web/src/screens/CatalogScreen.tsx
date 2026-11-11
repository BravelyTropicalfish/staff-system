import { useCallback, useEffect, useState } from 'react';
import { ApiError, getJson, sendJson } from '../lib/api';
import type { Role, Skill } from '../lib/catalog';
import { canEditCatalogs, domainLabel, renameBody, toggleBody, toggleLabel } from '../lib/catalog';
import type { SessionView } from '../lib/confirmation';

function failure(error: unknown): string {
  return error instanceof ApiError ? `${error.code}: ${error.message}` : String(error);
}

/**
 * The coordinator's catalog: the roles and the skills in two tables, each with its domain, its
 * state and the one toggle that state allows. A rename is sent as the name alone, so a refusal —
 * `referenced` on anything a person or a post already points at — leaves the row exactly as it
 * was and only prints why.
 */
export default function CatalogScreen() {
  const [roles, setRoles] = useState<Role[] | null>(null);
  const [skills, setSkills] = useState<Skill[] | null>(null);
  const [viewer, setViewer] = useState<SessionView | null>(null);
  const [names, setNames] = useState<Record<string, string>>({});
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  const load = useCallback(() => {
    let cancelled = false;
    Promise.all([
      getJson<Role[]>('/api/roles'),
      getJson<Skill[]>('/api/skills'),
      getJson<SessionView>('/api/session'),
    ])
      .then(([roleRows, skillRows, session]) => {
        if (!cancelled) {
          setRoles(roleRows);
          setSkills(skillRows);
          setViewer(session);
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

  const send = (request: Promise<unknown>) => {
    setBusy(true);
    setError(null);
    request
      .then(() => load())
      .catch((caught: unknown) => setError(failure(caught)))
      .finally(() => setBusy(false));
  };

  const rename = (path: string, key: string, current: string) => {
    const name = names[key] ?? current;
    send(sendJson<unknown>('PATCH', path, renameBody(name)));
  };

  const toggle = (path: string, active: boolean) => {
    send(sendJson<unknown>('PATCH', path, toggleBody({ active })));
  };

  const editable = viewer !== null && canEditCatalogs(viewer.role);

  return (
    <div>
      <h1>Catalogs</h1>
      <p className="lede">
        A role or a skill that a person or a post already points at can neither be renamed nor
        removed; deactivation is the only way back, and it hides the item from new work only.
      </p>
      {error ? (
        <p className="mt-4 rounded border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-800">
          {error}
        </p>
      ) : null}

      <section className="card">
        <h2>Roles</h2>
        {roles === null ? (
          error === null ? <p className="mt-2 text-sm text-slate-600">Reading the roles…</p> : null
        ) : (
          <table className="mt-2 w-full text-left text-sm">
            <thead className="text-slate-500">
              <tr>
                <th className="py-1 font-medium">Role</th>
                <th className="py-1 font-medium">Domain skills</th>
                <th className="py-1 font-medium">State</th>
                <th className="py-1" />
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {roles.map((role) => {
                const key = `role-${role.id}`;
                return (
                  <tr key={role.id}>
                    <td className="py-2 pr-2">
                      <input
                        className="w-40 rounded border border-slate-300 px-2 py-1 text-slate-900"
                        value={names[key] ?? role.name}
                        disabled={!editable || busy}
                        onChange={(event) =>
                          setNames((current) => ({ ...current, [key]: event.target.value }))
                        }
                      />
                      <button
                        type="button"
                        className="ml-2 rounded border border-slate-300 px-2 py-1 text-slate-700 disabled:text-slate-400"
                        disabled={!editable || busy}
                        onClick={() => rename(`/api/roles/${role.id}`, key, role.name)}
                      >
                        Rename
                      </button>
                    </td>
                    <td className="py-2 pr-2 text-slate-700">{domainLabel(role)}</td>
                    <td className="py-2 pr-2 text-slate-700">{role.active ? 'active' : 'deactivated'}</td>
                    <td className="py-2 text-right">
                      <button
                        type="button"
                        className="rounded border border-slate-300 px-2 py-1 text-slate-700 disabled:text-slate-400"
                        disabled={!editable || busy}
                        onClick={() => toggle(`/api/roles/${role.id}`, role.active)}
                      >
                        {toggleLabel(role)}
                      </button>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        )}
      </section>

      <section className="card">
        <h2>Skills</h2>
        {skills === null ? (
          error === null ? <p className="mt-2 text-sm text-slate-600">Reading the skills…</p> : null
        ) : (
          <table className="mt-2 w-full text-left text-sm">
            <thead className="text-slate-500">
              <tr>
                <th className="py-1 font-medium">Skill</th>
                <th className="py-1 font-medium">Domain</th>
                <th className="py-1 font-medium">State</th>
                <th className="py-1" />
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {skills.map((skill) => {
                const key = `skill-${skill.id}`;
                return (
                  <tr key={skill.id}>
                    <td className="py-2 pr-2">
                      <input
                        className="w-40 rounded border border-slate-300 px-2 py-1 text-slate-900"
                        value={names[key] ?? skill.name}
                        disabled={!editable || busy}
                        onChange={(event) =>
                          setNames((current) => ({ ...current, [key]: event.target.value }))
                        }
                      />
                      <button
                        type="button"
                        className="ml-2 rounded border border-slate-300 px-2 py-1 text-slate-700 disabled:text-slate-400"
                        disabled={!editable || busy}
                        onClick={() => rename(`/api/skills/${skill.id}`, key, skill.name)}
                      >
                        Rename
                      </button>
                    </td>
                    <td className="py-2 pr-2 text-slate-700">{skill.roleDomainName}</td>
                    <td className="py-2 pr-2 text-slate-700">
                      {skill.active ? 'active' : 'deactivated'}
                    </td>
                    <td className="py-2 text-right">
                      <button
                        type="button"
                        className="rounded border border-slate-300 px-2 py-1 text-slate-700 disabled:text-slate-400"
                        disabled={!editable || busy}
                        onClick={() => toggle(`/api/skills/${skill.id}`, skill.active)}
                      >
                        {toggleLabel(skill)}
                      </button>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        )}
      </section>

      {viewer !== null && !editable ? (
        <p className="mt-4 text-sm text-slate-600">
          Signed in as {viewer.displayName}; only the coordinator writes a catalog.
        </p>
      ) : null}
    </div>
  );
}
