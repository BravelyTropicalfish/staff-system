import { useCallback, useEffect, useState } from 'react';
import { Link, NavLink, Outlet, useLocation } from 'react-router-dom';
import { getJson } from '../lib/api';
import { activeLink, breadcrumb, initial, roleWord, NAV, visibleNav, type Role } from '../lib/nav';

type SessionView = { username: string; role: Role; displayName: string };

/**
 * The chrome the prototype drew: a sidebar with the grouped links, a top bar with the breadcrumb and
 * the account, and a full-width view. It sits above every screen, so a screen only renders its own
 * content and no longer sets its own width or margins.
 */
export default function AppShell() {
  const location = useLocation();
  const [session, setSession] = useState<SessionView | null | undefined>(undefined);

  const load = useCallback(() => {
    let cancelled = false;
    getJson<SessionView>('/api/session')
      .then((view) => {
        if (!cancelled) setSession(view);
      })
      .catch(() => {
        // Signed out is a state here, not an error: the shell still draws, the screens say why not.
        if (!cancelled) setSession(null);
      });
    return () => {
      cancelled = true;
    };
  }, []);

  useEffect(() => load(), [load, location.pathname]);

  const role = session?.role ?? null;
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
            <div className="brand-sub">Match people to project posts</div>
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
                </NavLink>
              ))}
            </div>
          ))}
        </nav>

        <div className="sidebar-foot">
          <div className="note">
            Prototype-driven UI. Person data arrives by import; a confirmed seat is released by the
            lead or by ending the project.
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
            {session ? (
              <>
                <span className="avatar">{initial(session.username)}</span>
                <span className="who">
                  <strong>{session.username}</strong>
                  <span>{roleWord(session.role)}</span>
                </span>
              </>
            ) : (
              <span className="note">Not signed in</span>
            )}
            <Link className="btn btn-sm" to="/login">
              {session ? 'Switch account' : 'Sign in'}
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
