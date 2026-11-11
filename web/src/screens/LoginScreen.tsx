import { useState, type FormEvent } from 'react';
import { useNavigate } from 'react-router-dom';
import { ApiError, sendJson } from '../lib/api';
import { credentials, credentialsProblem, type SignedIn } from '../lib/session';

/**
 * The sign-in screen. The requirement calls sign-in "a lightweight seeded account with a role"
 * (REQ-001 §2) — the accounts exist in the seed, and this is the door to them: without it a screen
 * could only be reached by posting `POST /api/session` by hand.
 */
export default function LoginScreen() {
  const navigate = useNavigate();
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [problem, setProblem] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  const submit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const local = credentialsProblem(username, password);
    if (local !== null) {
      setProblem(local);
      return;
    }
    setProblem(null);
    setBusy(true);
    sendJson<SignedIn>('POST', '/api/session', credentials(username, password))
      .then(() => navigate('/'))
      .catch((error: unknown) => {
        setProblem(error instanceof ApiError ? `${error.code}: ${error.message}` : String(error));
      })
      .finally(() => setBusy(false));
  };

  return (
    <main className="mx-auto max-w-md p-8">
      <h1 className="text-2xl font-semibold tracking-tight text-slate-900">Sign in</h1>
      <p className="mt-1 text-sm text-slate-600">
        The three seeded accounts share the demo password; the role decides what the screens offer.
      </p>

      {problem === null ? null : (
        <p className="mt-4 rounded border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-800">
          {problem}
        </p>
      )}

      <form className="mt-6 space-y-4 rounded-lg border border-slate-200 bg-white p-4 shadow-sm" onSubmit={submit}>
        <label className="block text-sm">
          <span className="text-slate-700">Account</span>
          <input
            className="mt-1 w-full rounded border border-slate-300 px-3 py-2"
            name="username"
            autoComplete="username"
            value={username}
            onChange={(event) => setUsername(event.target.value)}
          />
        </label>
        <label className="block text-sm">
          <span className="text-slate-700">Password</span>
          <input
            className="mt-1 w-full rounded border border-slate-300 px-3 py-2"
            name="password"
            type="password"
            autoComplete="current-password"
            value={password}
            onChange={(event) => setPassword(event.target.value)}
          />
        </label>
        <button
          className="rounded bg-slate-900 px-3 py-2 text-sm font-medium text-white disabled:opacity-50"
          type="submit"
          disabled={busy}
        >
          {busy ? 'Signing in…' : 'Sign in'}
        </button>
      </form>

      <p className="mt-4 text-sm text-slate-600">
        Seeded accounts: <code className="text-slate-800">coordinator</code>,{' '}
        <code className="text-slate-800">lead.one</code>, <code className="text-slate-800">lead.two</code>{' '}
        — password <code className="text-slate-800">staffing-demo</code>.
      </p>
    </main>
  );
}
