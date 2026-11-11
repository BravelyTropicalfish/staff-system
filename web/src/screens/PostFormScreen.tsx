import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import PostForm from '../components/PostForm';
import { ApiError, getJson, sendJson } from '../lib/api';
import type { Bands, Post, PostFormValue, RoleOption, SkillOption } from '../lib/post';
import { postDraft } from '../lib/post';

type Loaded = { bands: Bands; roles: RoleOption[]; skills: SkillOption[] };

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
    ])
      .then(([bands, roles, skills]) => {
        if (cancelled) {
          return;
        }
        setLoaded({ bands, roles, skills });
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
    <main className="mx-auto max-w-3xl p-8">
      <h1 className="text-2xl font-semibold tracking-tight text-slate-900">
        A new post on project {projectId}
      </h1>
      {loadError ? (
        <p className="mt-4 rounded border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-800">
          {loadError}
        </p>
      ) : null}
      {loaded ? (
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
          submitLabel="Create the post"
          onChange={(patch) => setValue((current) => ({ ...current, ...patch }))}
          onSkills={(skills) => setValue((current) => ({ ...current, skills }))}
          onSubmit={save}
        />
      ) : loadError ? null : (
        <p className="mt-2 text-sm text-slate-600">Reading the catalogs…</p>
      )}
    </main>
  );
}
