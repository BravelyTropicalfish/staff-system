import { useState, type FormEvent } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { ApiError, sendJson } from '../lib/api';
import { credentials, credentialsProblem, rememberToken, type SignedIn } from '../lib/session';

/**
 * The sign-in screen. The requirement calls sign-in "a lightweight seeded account with a role"
 * (REQ-001 §2) — the accounts exist in the seed, and this is the door to them: without it a screen
 * could only be reached by posting `POST /api/session` by hand.
 */
export default function LoginScreen() {
  const navigate = useNavigate();
  const location = useLocation();
  // 被壳层赶到登录页时，登录后回到原来那一屏；直接打开登录页时去板上。
  const from = (location.state as { from?: string } | null)?.from ?? '/';
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
      .then((signed) => {
        // 这个令牌只存在本标签页：另一个页面可以同时登录另一个账号。
        rememberToken(signed.token);
        navigate(from, { replace: true });
      })
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
            <div className="login-title">登录 Staffing</div>
            <div className="note">MVP · 一个账号一个人一个角色</div>
          </div>
        </div>
        <p className="login-sub">
          种子账号就是登录方式：工作簿存人员，目录存角色，账号的角色决定服务端允许你打开哪些屏。
        </p>

        {problem === null ? null : (
          <div className="banner danger">
            <span className="ico">!</span>
            <span>{problem}</span>
          </div>
        )}

        <form className="stack" onSubmit={submit}>
          <div className="field">
            <label htmlFor="username">账号</label>
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
            <label htmlFor="password">密码</label>
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
            {busy ? '正在登录…' : '登录'}
          </button>
        </form>

        <div className="login-foot">
          种子账号：<code>coordinator</code>、<code>lead.one</code>、<code>lead.two</code> ——
          密码 <code>staffing-demo</code>。再次登录即切换账号。
        </div>
      </div>
    </div>
  );
}
