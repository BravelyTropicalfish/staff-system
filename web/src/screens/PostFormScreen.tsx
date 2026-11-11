import { useEffect, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import PostForm from '../components/PostForm';
import { ApiError, getJson, sendJson } from '../lib/api';
import type { SessionView } from '../lib/confirmation';
import type { Bands, Post, PostFormValue, RoleOption, SkillOption } from '../lib/post';
import { postDraft } from '../lib/post';

type Loaded = { bands: Bands; roles: RoleOption[]; skills: SkillOption[]; viewer: SessionView };

const EMPTY: PostFormValue = {
  title: '',
  role: '',
  minJobLevel: '',
  headcount: '1',
  startDate: '',
  endDate: '',
  travel: '',
  skills: [],
};

function failure(error: unknown): string {
  return error instanceof ApiError ? `${error.code}: ${error.message}` : String(error);
}

/** The create form: a new post on a project, every field taken from the server's own catalogs. */
export default function PostFormScreen() {
  const { projectId = '' } = useParams();
  const navigate = useNavigate();
  const [loaded, setLoaded] = useState<Loaded | null>(null);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [value, setValue] = useState<PostFormValue>(EMPTY);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    let cancelled = false;
    Promise.all([
      getJson<Bands>('/api/bands'),
      getJson<RoleOption[]>('/api/roles'),
      getJson<SkillOption[]>('/api/skills?active=true'),
      getJson<SessionView>('/api/session'),
    ])
      .then(([bands, roles, skills, viewer]) => {
        if (cancelled) {
          return;
        }
        setLoaded({ bands, roles, skills, viewer });
        setValue((current) => ({
          ...current,
          role: current.role || (roles.find((role) => role.active)?.name ?? ''),
          minJobLevel: current.minJobLevel || (bands.jobLevels[0] ?? ''),
          travel: current.travel || (bands.travel[0] ?? ''),
        }));
      })
      .catch((caught: unknown) => {
        if (!cancelled) {
          setLoadError(failure(caught));
        }
      });
    return () => {
      cancelled = true;
    };
  }, []);

  const save = () => {
    setBusy(true);
    setError(null);
    sendJson<Post>('POST', `/api/projects/${encodeURIComponent(projectId)}/posts`, postDraft(value))
      .then((post) => navigate(`/posts/${post.id}`))
      .catch((caught: unknown) => setError(failure(caught)))
      .finally(() => setBusy(false));
  };

  return (
    <div>
      <div className="btn-row" style={{ marginBottom: 8 }}>
        <Link className="btn btn-sm" to={`/projects/${projectId}`}>
          ← 返回项目
        </Link>
      </div>
      <h1>
        项目 {projectId} 的新岗位
      </h1>
      {loadError ? (
        <p className="mt-4 rounded border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-800">
          {loadError}
        </p>
      ) : null}
      {loaded ? (
        loaded.viewer.role === 'LEAD' ? (
          <PostForm
            value={value}
            bands={loaded.bands}
            roles={loaded.roles}
            skills={loaded.skills}
            frozen={[]}
            frozenReason={null}
            floor={1}
            busy={busy}
            error={error}
            submitLabel="创建岗位"
            onChange={(patch) => setValue((current) => ({ ...current, ...patch }))}
            onSkills={(skills) => setValue((current) => ({ ...current, skills }))}
            onSubmit={save}
          />
        ) : (
          <div className="banner danger">
            只有项目自己的项目经理可以新增岗位；协调员只读。
          </div>
        )
      ) : loadError ? null : (
        <p className="mt-2 text-sm text-slate-600">正在读取目录…</p>
      )}
    </div>
  );
}
