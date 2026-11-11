import { useCallback, useEffect, useRef, useState } from 'react';
import { ApiError, getJson, sendForm } from '../lib/api';
import type { SessionView } from '../lib/confirmation';
import { moment } from '../lib/confirmation';
import type { ImportBatch } from '../lib/imports';
import { canImport, outcomeLine, refusalLine, statusLabel } from '../lib/imports';
import { roleWord } from '../lib/nav';

/**
 * 协调员的报表导入屏：一次上传、批次历史，以及被拒批次留下的每一行。工作簿是人员数据的唯一
 * 入口，一次上传就是一个工作单元，所以两种失败读起来必须不同 —— 形状不对的文件直接被拒、什么
 * 都不留下；行有问题的文件记成一个已拒绝的批次，一字未写，并列出每一行待修。
 *
 * 历史接口只回答计数，所以每个已拒绝的批次再按 id 读一次它的错误行。
 */
function withErrors(batch: ImportBatch): Promise<ImportBatch> {
  if (batch.status !== 'REJECTED') {
    return Promise.resolve(batch);
  }
  return getJson<ImportBatch>(`/api/imports/${batch.id}`).then((detail) => ({
    ...batch,
    errors: detail.errors,
  }));
}

export default function ImportScreen() {
  const [batches, setBatches] = useState<ImportBatch[] | null>(null);
  const [viewer, setViewer] = useState<SessionView | null>(null);
  const [file, setFile] = useState<File | null>(null);
  const [refused, setRefused] = useState<string | null>(null);
  const [uploaded, setUploaded] = useState<ImportBatch | null>(null);
  const [busy, setBusy] = useState(false);
  const [picked, setPicked] = useState<number | null>(null);
  const picker = useRef<HTMLInputElement>(null);

  const load = useCallback(() => {
    let cancelled = false;
    Promise.all([getJson<ImportBatch[]>('/api/imports'), getJson<SessionView>('/api/session')])
      .then(([rows, session]) => Promise.all(rows.map(withErrors)).then((full) => [full, session] as const))
      .then(([full, session]) => {
        if (!cancelled) {
          setBatches(full);
          setViewer(session);
          setPicked((current) => current ?? full[0]?.id ?? null);
        }
      })
      .catch((caught: unknown) => {
        if (!cancelled) {
          setRefused(caught instanceof ApiError ? refusalLine(caught) : String(caught));
        }
      });
    return () => {
      cancelled = true;
    };
  }, []);

  useEffect(() => load(), [load]);

  const upload = () => {
    if (file === null) {
      return;
    }
    const form = new FormData();
    form.append('file', file);
    setBusy(true);
    setRefused(null);
    setUploaded(null);
    sendForm<ImportBatch>('/api/imports', form)
      .then((batch) => {
        setUploaded(batch);
        setFile(null);
        if (picker.current !== null) {
          picker.current.value = '';
        }
        load();
      })
      .catch((caught: unknown) => {
        setRefused(caught instanceof ApiError ? refusalLine(caught) : String(caught));
      })
      .finally(() => setBusy(false));
  };

  const writable = viewer !== null && canImport(viewer.role);
  const selected = batches?.find((batch) => batch.id === picked) ?? null;

  return (
    <div>
      <div className="page-head">
        <div>
          <h1>报表导入</h1>
          <div className="lede">
            一个工作簿 = 一个工作单元。任何一行不合法，整批 rejected，一个字都不写。
          </div>
        </div>
      </div>

      {refused !== null ? <div className="banner danger">{refused}</div> : null}

      <div className="stack">
        <div className="grid-2">
          <section className="card">
            <div className="card-head">
              <h2>上传</h2>
            </div>
            <div className="card-body stack">
              <div className="drop">
                <div className="big">把 .xlsx 拖到这里，或点击选择文件</div>
                <div className="note">只需要一张 people 表，技能写在同一行</div>
                <input
                  ref={picker}
                  type="file"
                  accept=".xlsx"
                  disabled={!writable || busy}
                  className="input"
                  style={{ marginTop: 10 }}
                  onChange={(event) => setFile(event.target.files?.[0] ?? null)}
                />
              </div>
              <div className="note">
                <strong>people</strong>：employee_no, name, email, role, job_level,
                travel_willingness, active, skills
                <br />
                <strong>skills</strong> 一格写一项或多项：<code>技能:档位</code>，多项用分号隔开，例如
                <code>后端开发:Practitioner;测试:Expert</code>；留空表示这个人员没有技能。
                <br />
                角色与技能必须在目录中存在且启用，否则整行报错、整批不写入；单元格 trim；ASCII
                值大小写不敏感。
              </div>
              <div className="btn-row">
                <button
                  type="button"
                  className="btn btn-primary"
                  disabled={!writable || busy || file === null}
                  onClick={upload}
                >
                  上传并校验
                </button>
              </div>
              {uploaded !== null ? (
                <p className="note">
                  {uploaded.filename} · {outcomeLine(uploaded)}
                </p>
              ) : null}
              {viewer !== null && !writable ? (
                <p className="note">
                  以{roleWord(viewer.role)}身份登录；只有协调员可以导入工作簿。
                </p>
              ) : null}
            </div>
          </section>

          <section className="card">
            <div className="card-head">
              <h2>批次历史</h2>
              <span className="note">不可修改，可回看错误</span>
            </div>
            <div className="card-body tight">
              {batches === null ? (
                <p className="note" style={{ padding: 14 }}>
                  正在读取批次…
                </p>
              ) : batches.length === 0 ? (
                <p className="empty">还没有上传过工作簿。</p>
              ) : (
                <table>
                  <thead>
                    <tr>
                      <th>批次</th>
                      <th>上传时间</th>
                      <th>上传人</th>
                      <th className="num">人员</th>
                      <th className="num">技能</th>
                      <th>状态</th>
                    </tr>
                  </thead>
                  <tbody>
                    {batches.map((batch) => (
                      <tr
                        key={batch.id}
                        className={`clickable${batch.id === picked ? ' selected' : ''}`}
                        onClick={() => setPicked(batch.id)}
                      >
                        <td className="mono">{batch.id}</td>
                        <td className="mono note">{moment(batch.uploadedAt)}</td>
                        <td>{batch.uploader}</td>
                        <td className="num">{batch.personRows}</td>
                        <td className="num">{batch.skillRows}</td>
                        <td>
                          {batch.status === 'APPLIED' ? (
                            <span className="badge badge-success">{statusLabel(batch)}</span>
                          ) : (
                            <span className="badge badge-danger">{statusLabel(batch)}</span>
                          )}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              )}
            </div>
          </section>
        </div>

        {selected !== null ? (
          <section className="card">
            <div className="card-head">
              <div>
                <h2>批次 {selected.id}</h2>
                <div className="note mono">
                  {selected.filename} · {moment(selected.uploadedAt)} · {selected.uploader}
                </div>
              </div>
              {selected.status === 'APPLIED' ? (
                <span className="badge badge-success">{statusLabel(selected)}</span>
              ) : (
                <span className="badge badge-danger">{statusLabel(selected)}</span>
              )}
            </div>
            <div className="card-body stack">
              {selected.status === 'REJECTED' ? (
                <>
                  <div className="banner danger">
                    <div>
                      <strong>整份文件未通过校验，本批次没有写入任何数据。</strong>
                      人员池保持上传之前的状态：没有新建人员、没有改动熟练度、没有切换在职标记。
                    </div>
                  </div>
                  <table>
                    <thead>
                      <tr>
                        <th>工作表</th>
                        <th className="num">行号</th>
                        <th>键值</th>
                        <th>原因</th>
                      </tr>
                    </thead>
                    <tbody>
                      {selected.errors.map((error) => (
                        <tr key={`${error.sheet}-${error.rowNo}-${error.key ?? ''}`}>
                          <td className="mono">{error.sheet}</td>
                          <td className="num mono">{error.rowNo}</td>
                          <td className="mono">{error.key ?? '—'}</td>
                          <td className="note">{error.message}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                  <div className="note">一次列全，修好后整份重传；旧批次与它的错误清单保持可读。</div>
                </>
              ) : (
                <div className="banner">
                  <div>{outcomeLine(selected)}</div>
                </div>
              )}
            </div>
          </section>
        ) : null}
      </div>
    </div>
  );
}
