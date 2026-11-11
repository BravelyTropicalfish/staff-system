import { useState } from 'react';
import { ApiError, sendJson } from '../lib/api';
import type { Project } from '../lib/projects';
import type { ProjectFormValue, Viewer } from '../lib/projectForm';
import { emptyProject, leadLocked, projectDraft, projectProblem } from '../lib/projectForm';

function failure(error: unknown): string {
  return error instanceof ApiError ? `${error.code}: ${error.message}` : String(error);
}

/**
 * The create form. A lead opens a project of their own, so the lead field is their account and
 * locked; the coordinator opens one for any lead. The same rule sits in the server, and the form
 * only spares the round trip that would repeat it.
 */
export default function ProjectForm({
  viewer,
  onCreated,
  onCancel,
  bare = false,
}: {
  viewer: Viewer;
  onCreated: (project: Project) => void;
  onCancel?: () => void;
  /** Inside a modal the form drops its own card frame and heading. */
  bare?: boolean;
}) {
  const locked = leadLocked(viewer);
  const [value, setValue] = useState<ProjectFormValue>(() => emptyProject(viewer));
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  const change = (patch: Partial<ProjectFormValue>) =>
    setValue((current) => ({ ...current, ...patch }));

  const submit = () => {
    const problem = projectProblem(value);
    if (problem !== null) {
      setError(problem);
      return;
    }
    setBusy(true);
    setError(null);
    sendJson<Project>('POST', '/api/projects', projectDraft(value))
      .then(onCreated)
      .catch((caught: unknown) => setError(failure(caught)))
      .finally(() => setBusy(false));
  };

  return (
    <section className={bare ? '' : 'card'} style={bare ? undefined : { marginBottom: 12 }}>
      {bare ? null : (
        <div className="card-head">
          <h2>新建项目</h2>
          <p className="note">
            {locked
              ? '你开的项目由你担任项目经理；协调员可以替任意项目经理开项目。'
              : '协调员可以替任意项目经理开项目。'}
          </p>
        </div>
      )}
      <div className={bare ? 'stack' : 'card-body stack'}>
        <div style={{ display: 'grid', gridTemplateColumns: '1.4fr 1fr 1fr 1fr', gap: 12 }}>
          <div className="field">
            <label htmlFor="project-name">项目名称</label>
            <input
              id="project-name"
              className="input"
              value={value.name}
              onChange={(event) => change({ name: event.target.value })}
            />
          </div>
          <div className="field">
            <label htmlFor="project-lead">项目经理账号</label>
            <input
              id="project-lead"
              className="input"
              value={value.lead}
              disabled={locked}
              onChange={(event) => change({ lead: event.target.value })}
            />
          </div>
          <div className="field">
            <label htmlFor="project-start">开始日期</label>
            <input
              id="project-start"
              className="input"
              type="date"
              value={value.startDate}
              onChange={(event) => change({ startDate: event.target.value })}
            />
          </div>
          <div className="field">
            <label htmlFor="project-end">结束日期</label>
            <input
              id="project-end"
              className="input"
              type="date"
              value={value.endDate}
              onChange={(event) => change({ endDate: event.target.value })}
            />
          </div>
        </div>
        {error ? <div className="banner danger">{error}</div> : null}
        <div className="btn-row">
          <button type="button" className="btn btn-primary" disabled={busy} onClick={submit}>
            创建项目
          </button>
          {onCancel ? (
            <button type="button" className="btn" disabled={busy} onClick={onCancel}>
              取消
            </button>
          ) : null}
        </div>
      </div>
    </section>
  );
}
