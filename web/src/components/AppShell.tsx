import { useCallback, useEffect, useState } from 'react';
import { Link, NavLink, Navigate, Outlet, useLocation } from 'react-router-dom';
import { getJson } from '../lib/api';
import { activeLink, breadcrumb, initial, roleWord, NAV, visibleNav, type Role } from '../lib/nav';
import { UNAUTHORIZED_EVENT } from '../lib/session';

type SessionView = { username: string; role: Role; displayName: string };

/**
 * The chrome the prototype drew: a sidebar with the grouped links, a top bar with the breadcrumb and
 * the account, and a full-width view. It sits above every screen, so a screen only renders its own
 * content and no longer sets its own width or margins.
 */
export default function AppShell() {
  const location = useLocation();
  const [session, setSession] = useState<SessionView | null | undefined>(undefined);
  const [pending, setPending] = useState(0);

  const load = useCallback(() => {
    let cancelled = false;
    getJson<SessionView>('/api/session')
      .then(async (view) => {
        if (cancelled) return;
        setSession(view);
        // The sidebar badge counts the proposals still waiting on this account, across projects.
        const inbox = await getJson<{ state: string }[]>('/api/recommendations').catch(() => []);
        if (!cancelled) setPending(inbox.filter((row) => row.state === 'proposed').length);
      })
      .catch(() => {
        // Signed out is a state here, not an error: the shell still draws, the screens say why not.
        if (!cancelled) {
          setSession(null);
          setPending(0);
        }
      });
    return () => {
      cancelled = true;
    };
  }, []);

  useEffect(() => load(), [load, location.pathname]);

  // 一次 401 就够了：清掉本标签页的令牌，壳层回到登录页。
  useEffect(() => {
    const signedOut = () => setSession(null);
    window.addEventListener(UNAUTHORIZED_EVENT, signedOut);
    return () => window.removeEventListener(UNAUTHORIZED_EVENT, signedOut);
  }, []);

  // 没登录时不画壳层，直接去登录页；回来时回到原来那一屏。
  if (session === null) {
    return <Navigate to="/login" replace state={{ from: location.pathname }} />;
  }

  if (session === undefined) {
    return (
      <p className="note" style={{ padding: 24 }}>
        正在读取会话…
      </p>
    );
  }

  const role = session.role;
  const groups = visibleNav(NAV, role);
  const here = breadcrumb(location.pathname);
  const active = activeLink(location.pathname);

  return (
    <div className="app">
      <aside className="sidebar">
        <div className="brand">
          <span className="brand-mark">S</span>
          <div>
            <div className="brand-name">Staffing</div>
            <div className="brand-sub">把人员匹配到项目岗位</div>
          </div>
        </div>

        <nav className="nav">
          {groups.map((group) => (
            <div key={group.group}>
              <div className="nav-group">{group.group}</div>
              {group.items.map((item) => (
                <NavLink
                  key={item.to}
                  to={item.to}
                  className={`nav-item${active === item.to ? ' active' : ''}`}
                >
                  <span className="dot" />
                  {item.label}
                  {item.badge === 'pending' && pending > 0 ? (
                    <span className="count">{pending}</span>
                  ) : null}
                </NavLink>
              ))}
            </div>
          ))}
        </nav>

        <div className="sidebar-foot">
          <div className="note">
            原型驱动的界面。人员数据由报表导入；已确认的座位由项目经理解除，或随项目结束释放。
          </div>
        </div>
      </aside>

      <div className="main-col">
        <header className="topbar">
          <div className="crumbs">
            <span>Staffing</span>
            <span>/</span>
            <span>{here.section}</span>
            <span>/</span>
            <strong>{here.page}</strong>
          </div>
          <div className="account">
            <span className="avatar">{initial(session.username)}</span>
            <span className="who">
              <strong>{session.username}</strong>
              <span>{roleWord(session.role)}</span>
            </span>
            <Link className="btn btn-sm" to="/login">
              切换账号
            </Link>
          </div>
        </header>

        <main className="view">
          <Outlet context={{ session }} />
        </main>
      </div>
    </div>
  );
}
