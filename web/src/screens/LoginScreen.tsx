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
    <div className="login-wrap">
      <div className="login-card">
        <div className="login-head">
          <span className="brand-mark">S</span>
          <div>
            <div className="login-title">Sign in to Staffing</div>
            <div className="note">One account, one person, one role</div>
          </div>
        </div>
        <p className="login-sub">
          The seeded accounts are the sign-in mechanism: the workbook holds people, the catalog holds
          roles, and the role of the account decides which screens the server lets you use.
        </p>

        {problem === null ? null : (
          <div className="banner danger">
            <span className="ico">!</span>
            <span>{problem}</span>
          </div>
        )}

        <form className="stack" onSubmit={submit}>
          <div className="field">
            <label htmlFor="username">Account</label>
            <input
              className="input"
              id="username"
              name="username"
              autoComplete="username"
              value={username}
              onChange={(event) => setUsername(event.target.value)}
            />
          </div>
          <div className="field">
            <label htmlFor="password">Password</label>
            <input
              className="input"
              id="password"
              name="password"
              type="password"
              autoComplete="current-password"
              value={password}
              onChange={(event) => setPassword(event.target.value)}
            />
          </div>
          <button className="btn btn-primary" type="submit" disabled={busy}>
            {busy ? 'Signing in…' : 'Sign in'}
          </button>
        </form>

        <div className="login-foot">
          Seeded accounts: <code>coordinator</code>, <code>lead.one</code>, <code>lead.two</code> —
          password <code>staffing-demo</code>. Signing in again switches the account.
        </div>
      </div>
    </div>
  );
}
