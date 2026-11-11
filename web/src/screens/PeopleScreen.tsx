import { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import HeadcountSection from '../components/HeadcountSection';
import Modal from '../components/Modal';
import { ApiError, getJson } from '../lib/api';
import type { PersonProfile, PersonSummary } from '../lib/people';
import { occupancyLabel, profileNote } from '../lib/people';
import type { SignedIn } from '../lib/session';

function failure(error: unknown): string {
  return error instanceof ApiError ? `${error.code}: ${error.message}` : String(error);
}

/** 弹窗里的一份档案：主数据、技能档位与占用，全部只读。 */
function PersonDetail({ profile }: { profile: PersonProfile }) {
  return (
    <div className="stack">
      <div className="banner">
        <div>{profileNote(profile)}</div>
      </div>

      <dl className="kv">
        <dt>角色</dt>
        <dd>
          <span className="badge">{profile.role}</span> <span className="note">唯一角色</span>
        </dd>
        <dt>职级</dt>
        <dd>{profile.jobLevel}</dd>
        <dt>差旅接受度</dt>
        <dd>{profile.travel}</dd>
        <dt>邮箱</dt>
        <dd>{profile.email ?? '—'}</dd>
      </dl>

      <div>
        <h3 style={{ marginBottom: 4 }}>技能项与档位</h3>
        <div className="note" style={{ marginBottom: 6 }}>
          技能是能力项，可以跨角色，但它不会让人拥有第二个角色。
        </div>
        {profile.skills.length === 0 ? (
          <p className="empty">这个人没有记录任何技能。</p>
        ) : (
          <table>
            <tbody>
              {profile.skills.map((skill) => (
                <tr key={skill.skill}>
                  <td>{skill.skill}</td>
                  <td className="note">
                    {skill.band} · {skill.roleDomain} 域
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

      <div>
        <h3 style={{ marginBottom: 6 }}>占用（含未来）</h3>
        {profile.allocations.length === 0 ? (
          <p className="empty">当前无占用。</p>
        ) : (
          <table>
            <tbody>
              {profile.allocations.map((allocation) => (
                <tr key={allocation.id}>
                  <td>{occupancyLabel(allocation)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

      <div className="btn-row">
        <button type="button" className="btn" disabled title="没有手工编辑入口：人员数据只由报表导入">
          编辑主数据
        </button>
        <Link className="btn" to={`/people/${profile.employeeNo}`}>
          打开档案页
        </Link>
      </div>
    </div>
  );
}

/**
 * 人员池：顶上是人数报表（只对协调员），下面是一张整宽的人员表。点一行打开该人的档案弹窗 ——
 * 人员数据只由报表导入，所以表与弹窗都只读，界面上没有任何写入口。
 *
 * <p>报表区块只在角色确认为协调员之后才挂载：项目经理这一页不应该发出报表请求。
 */
export default function PeopleScreen() {
  const [people, setPeople] = useState<PersonSummary[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [open, setOpen] = useState<string | null>(null);
  const [profile, setProfile] = useState<PersonProfile | null>(null);
  const [profileError, setProfileError] = useState<string | null>(null);
  const [role, setRole] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    getJson<SignedIn>('/api/session')
      .then((me) => {
        if (!cancelled) {
          setRole(me.role);
        }
      })
      .catch(() => {
        // 没有会话时页面本来就会在下面报 401，这里不必再画一条。
      });
    return () => {
      cancelled = true;
    };
  }, []);

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

  useEffect(() => {
    if (open === null) {
      return;
    }
    let cancelled = false;
    setProfile(null);
    setProfileError(null);
    getJson<PersonProfile>(`/api/people/${encodeURIComponent(open)}`)
      .then((row) => {
        if (!cancelled) {
          setProfile(row);
        }
      })
      .catch((caught: unknown) => {
        if (!cancelled) {
          setProfileError(failure(caught));
        }
      });
    return () => {
      cancelled = true;
    };
  }, [open]);

  return (
    <div>
      <div className="page-head">
        <div>
          <h1>人员池</h1>
          <div className="lede">
            每个人员只有一个角色；角色由报表写入，界面不能新增或修改。技能是能力项，不是角色。
            点一行打开档案。
          </div>
        </div>
      </div>
      {error ? <div className="banner danger">{error}</div> : null}

      {role === 'COORDINATOR' ? <HeadcountSection /> : null}

      <section className="card">
        <div className="card-body tight">
          {people === null ? (
            error === null ? <p className="note">正在读取人员池…</p> : null
          ) : people.length === 0 ? (
            <p className="empty">还没有导入任何人员。</p>
          ) : (
            <table>
              <thead>
                <tr>
                  <th>工号</th>
                  <th>姓名</th>
                  <th>角色</th>
                  <th>职级</th>
                  <th>差旅</th>
                  <th>状态</th>
                </tr>
              </thead>
              <tbody>
                {people.map((person) => (
                  <tr key={person.employeeNo} className="clickable" onClick={() => setOpen(person.employeeNo)}>
                    <td className="mono">{person.employeeNo}</td>
                    <td>
                      <strong>{person.name}</strong>
                    </td>
                    <td>
                      <span className="badge">{person.role}</span>
                    </td>
                    <td>{person.jobLevel}</td>
                    <td>{person.travel}</td>
                    <td>
                      {person.active ? (
                        <span className="badge badge-success">在职</span>
                      ) : (
                        <span className="badge badge-muted">已停用</span>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      </section>

      {open !== null ? (
        <Modal
          title={profile?.name ?? open}
          note={profile === null ? open : profile.employeeNo}
          onClose={() => setOpen(null)}
        >
          {profileError ? (
            <div className="banner danger">{profileError}</div>
          ) : profile === null ? (
            <p className="note">正在读取档案…</p>
          ) : (
            <PersonDetail profile={profile} />
          )}
        </Modal>
      ) : null}
    </div>
  );
}
