import { useCallback, useEffect, useState } from 'react';
import { ApiError, getJson, sendJson } from '../lib/api';
import type { Role, Skill } from '../lib/catalog';
import { canEditCatalogs, domainLabel, renameBody, toggleBody, toggleLabel } from '../lib/catalog';
import { roleWord } from '../lib/nav';
import type { SessionView } from '../lib/confirmation';
import type { PersonSummary } from '../lib/people';

function failure(error: unknown): string {
  return error instanceof ApiError ? `${error.code}: ${error.message}` : String(error);
}

type Tab = 'roles' | 'skills';

/**
 * 协调员的目录：角色与技能两张表，各自带技能域、状态与当前状态允许的那一个开关；右侧是原型的
 * 角色快照 —— 每个角色有几个人、它的技能域是什么。改名只发名字，所以被引用时的 `referenced`
 * 拒绝会让这一行原样不动，只把原因打出来。
 */
export default function CatalogScreen() {
  const [roles, setRoles] = useState<Role[] | null>(null);
  const [skills, setSkills] = useState<Skill[] | null>(null);
  const [people, setPeople] = useState<PersonSummary[]>([]);
  const [viewer, setViewer] = useState<SessionView | null>(null);
  const [names, setNames] = useState<Record<string, string>>({});
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const [tab, setTab] = useState<Tab>('roles');
  const [newName, setNewName] = useState('');
  const [newDomain, setNewDomain] = useState('');

  const load = useCallback(() => {
    let cancelled = false;
    Promise.all([
      getJson<Role[]>('/api/roles'),
      getJson<Skill[]>('/api/skills'),
      getJson<PersonSummary[]>('/api/people').catch(() => []),
      getJson<SessionView>('/api/session'),
    ])
      .then(([roleRows, skillRows, personRows, session]) => {
        if (!cancelled) {
          setRoles(roleRows);
          setSkills(skillRows);
          setPeople(personRows);
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
  const holders = (roleName: string) => people.filter((person) => person.role === roleName);

  const remove = (path: string) => {
    send(sendJson<unknown>('DELETE', path));
  };

  const create = () => {
    const name = newName.trim();
    if (name === '') {
      return;
    }
    const domain = Number(newDomain || roles?.[0]?.id || 0);
    const request =
      tab === 'roles'
        ? sendJson<unknown>('POST', '/api/roles', { name })
        : sendJson<unknown>('POST', '/api/skills', { name, roleDomainId: domain });
    setNewName('');
    send(request);
  };

  return (
    <div>
      <div className="page-head">
        <div>
          <h1>目录管理</h1>
          <div className="lede">
            被引用的目录项只可停用，不可改名或删除；停用不影响任何已存数据。
          </div>
        </div>
      </div>

      {error ? <div className="banner danger">{error}</div> : null}

      <div className="stack">
        <div className="banner">
          <div>
            角色目录预置 <code>PM</code> <code>BA</code> <code>DEV</code> <code>QA</code>{' '}
            <code>UX</code>；技能目录预置六项。两者都可新增，新增后同样可用于岗位、人员与导入。
            角色是人员的唯一属性，一个人只能有一个角色。技能声明一个角色技能域，改域只影响之后的
            搜索，不改写已存人员、岗位或推荐快照。
          </div>
        </div>

        <div className="grid-2">
          <section className="card">
            <div className="card-head">
              <div className="chips">
                <button
                  type="button"
                  className={`chip${tab === 'roles' ? ' active' : ''}`}
                  onClick={() => setTab('roles')}
                >
                  角色
                </button>
                <button
                  type="button"
                  className={`chip${tab === 'skills' ? ' active' : ''}`}
                  onClick={() => setTab('skills')}
                >
                  技能
                </button>
              </div>
              <span className="note">
                {viewer === null
                  ? ''
                  : editable
                    ? `以${roleWord(viewer.role)}身份登录 · 可维护`
                    : `以${roleWord(viewer.role)}身份登录 · 只读`}
              </span>
            </div>
            <div className="card-body tight">
              {editable ? (
                <div className="btn-row" style={{ padding: 12, borderBottom: '1px solid var(--color-line)' }}>
                  <input
                    className="input"
                    style={{ width: 160 }}
                    placeholder={tab === 'roles' ? '新角色名称' : '新技能名称'}
                    value={newName}
                    onChange={(event) => setNewName(event.target.value)}
                  />
                  {tab === 'skills' ? (
                    <select
                      className="input"
                      value={newDomain}
                      onChange={(event) => setNewDomain(event.target.value)}
                    >
                      {(roles ?? []).map((role) => (
                        <option key={role.id} value={String(role.id)}>
                          {role.name}
                        </option>
                      ))}
                    </select>
                  ) : null}
                  <button type="button" className="btn btn-primary btn-sm" disabled={busy} onClick={create}>
                    {tab === 'roles' ? '新增角色' : '新增技能'}
                  </button>
                </div>
              ) : null}
              {tab === 'roles' ? (
                roles === null ? (
                  error === null ? <p className="note">正在读取角色…</p> : null
                ) : (
                  <table>
                    <thead>
                      <tr>
                        <th>角色</th>
                        <th>技能域</th>
                        <th>状态</th>
                        <th>操作</th>
                      </tr>
                    </thead>
                    <tbody>
                      {roles.map((role) => {
                        const key = `role-${role.id}`;
                        return (
                          <tr key={role.id}>
                            <td>
                              <input
                                className="input"
                                style={{ width: 140 }}
                                value={names[key] ?? role.name}
                                disabled={!editable || busy}
                                onChange={(event) =>
                                  setNames((current) => ({ ...current, [key]: event.target.value }))
                                }
                              />
                            </td>
                            <td className="note">{domainLabel(role)}</td>
                            <td>
                              {role.active ? (
                                <span className="badge badge-success">启用</span>
                              ) : (
                                <span className="badge badge-muted">停用</span>
                              )}
                            </td>
                            <td>
                              <div className="btn-row">
                                <button
                                  type="button"
                                  className="btn btn-sm"
                                  disabled={!editable || busy}
                                  onClick={() => rename(`/api/roles/${role.id}`, key, role.name)}
                                >
                                  改名
                                </button>
                                <button
                                  type="button"
                                  className="btn btn-sm"
                                  disabled={!editable || busy}
                                  onClick={() => toggle(`/api/roles/${role.id}`, role.active)}
                                >
                                  {toggleLabel(role)}
                                </button>
                                <button
                                  type="button"
                                  className="btn btn-sm btn-danger"
                                  disabled={!editable || busy}
                                  onClick={() => remove(`/api/roles/${role.id}`)}
                                >
                                  删除
                                </button>
                              </div>
                            </td>
                          </tr>
                        );
                      })}
                    </tbody>
                  </table>
                )
              ) : skills === null ? (
                error === null ? <p className="note">正在读取技能…</p> : null
              ) : (
                <table>
                  <thead>
                    <tr>
                      <th>技能</th>
                      <th>技能域</th>
                      <th>状态</th>
                      <th>操作</th>
                    </tr>
                  </thead>
                  <tbody>
                    {skills.map((skill) => {
                      const key = `skill-${skill.id}`;
                      return (
                        <tr key={skill.id}>
                          <td>
                            <input
                              className="input"
                              style={{ width: 140 }}
                              value={names[key] ?? skill.name}
                              disabled={!editable || busy}
                              onChange={(event) =>
                                setNames((current) => ({ ...current, [key]: event.target.value }))
                              }
                            />
                          </td>
                          <td>
                            <span className="badge">{skill.roleDomainName}</span>
                          </td>
                          <td>
                            {skill.active ? (
                              <span className="badge badge-success">启用</span>
                            ) : (
                              <span className="badge badge-muted">停用</span>
                            )}
                          </td>
                          <td>
                            <div className="btn-row">
                              <button
                                type="button"
                                className="btn btn-sm"
                                disabled={!editable || busy}
                                onClick={() => rename(`/api/skills/${skill.id}`, key, skill.name)}
                              >
                                改名
                              </button>
                              <button
                                type="button"
                                className="btn btn-sm"
                                disabled={!editable || busy}
                                onClick={() => toggle(`/api/skills/${skill.id}`, skill.active)}
                              >
                                {toggleLabel(skill)}
                              </button>
                              <button
                                type="button"
                                className="btn btn-sm btn-danger"
                                disabled={!editable || busy}
                                onClick={() => remove(`/api/skills/${skill.id}`)}
                              >
                                删除
                              </button>
                            </div>
                          </td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              )}
            </div>
          </section>

          <section className="card">
            <div className="card-head">
              <h2>角色目录快照</h2>
              <span className="note">一人一角色</span>
            </div>
            <div className="card-body stack">
              {roles === null ? (
                <p className="note">正在读取…</p>
              ) : (
                roles.map((role) => {
                  const row = holders(role.name);
                  const who = row.slice(0, 3).map((person) => person.name).join('、');
                  return (
                    <div className="role-row" key={role.id}>
                      <span className="badge">{role.name}</span>
                      <span className="note">
                        {row.length > 0
                          ? `${row.length} 人 · ${who}${row.length > 3 ? ' 等' : ''}`
                          : '暂无人员'}
                        <div>
                          技能域：
                          {role.skills.length > 0 ? domainLabel(role) : '空域 → 该角色岗位无兜底候选人'}
                        </div>
                      </span>
                    </div>
                  );
                })
              )}
              <div className="note">
                每个人员在上面只出现一次；技能目录单独维护，持有某个技能不会让人员多出一个角色。
                技能域只服务于兜底，不放宽岗位的 required 门槛。
              </div>
            </div>
          </section>
        </div>

        <div className="note">
          停用只会让新建岗位与新导入不再提供该项；已有人员、岗位与历史推荐理由继续显示原名。
        </div>
      </div>
    </div>
  );
}
