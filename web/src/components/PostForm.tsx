import type { Bands, PostFormValue, RoleOption, SkillOption, SkillRowValue } from '../lib/post';

const LABEL = 'text-xs font-medium uppercase tracking-wide text-slate-500';
const FIELD =
  'mt-1 w-full rounded border border-slate-300 bg-white px-2 py-1 text-sm text-slate-900 ' +
  'disabled:bg-slate-100 disabled:text-slate-500';

/** The fields the server freezes while a person is confirmed, and the reason shown beside them. */
export type FrozenField =
  | 'role'
  | 'minJobLevel'
  | 'requiredSkills'
  | 'startDate'
  | 'endDate'
  | 'travel';

type Props = {
  value: PostFormValue;
  bands: Bands;
  roles: RoleOption[];
  skills: SkillOption[];
  frozen: FrozenField[];
  frozenReason: string | null;
  floor: number;
  busy: boolean;
  error: string | null;
  submitLabel: string;
  onChange: (patch: Partial<PostFormValue>) => void;
  onSkills: (rows: SkillRowValue[]) => void;
  onSubmit: () => void;
};

function disabled(frozen: FrozenField[], name: FrozenField): boolean {
  return frozen.includes(name);
}

export default function PostForm({
  value,
  bands,
  roles,
  skills,
  frozen,
  frozenReason,
  floor,
  busy,
  error,
  submitLabel,
  onChange,
  onSkills,
  onSubmit,
}: Props) {
  const activeRoles = roles.filter((role) => role.active);
  const activeSkills = skills.filter((skill) => skill.active);

  const changeRow = (index: number, patch: Partial<SkillRowValue>) => {
    onSkills(value.skills.map((row, at) => (at === index ? { ...row, ...patch } : row)));
  };

  return (
    <form
      className="mt-4 grid gap-4"
      onSubmit={(event) => {
        event.preventDefault();
        onSubmit();
      }}
    >
      {frozenReason ? (
        <p className="rounded border border-amber-200 bg-amber-50 px-3 py-2 text-sm text-amber-900">
          {frozenReason}
        </p>
      ) : null}

      <label className="block">
        <span className={LABEL}>Title</span>
        <input
          className={FIELD}
          value={value.title}
          onChange={(event) => onChange({ title: event.target.value })}
        />
      </label>

      <div className="grid gap-4 sm:grid-cols-2">
        <label className="block">
          <span className={LABEL}>Role</span>
          <select
            className={FIELD}
            value={value.role}
            disabled={disabled(frozen, 'role')}
            onChange={(event) => onChange({ role: event.target.value })}
          >
            {activeRoles.map((role) => (
              <option key={role.id} value={role.name}>
                {role.name}
              </option>
            ))}
          </select>
        </label>

        <label className="block">
          <span className={LABEL}>Minimum job level</span>
          <select
            className={FIELD}
            value={value.minJobLevel}
            disabled={disabled(frozen, 'minJobLevel')}
            onChange={(event) => onChange({ minJobLevel: event.target.value })}
          >
            {bands.jobLevels.map((level) => (
              <option key={level} value={level}>
                {level}
              </option>
            ))}
          </select>
        </label>

        <label className="block">
          <span className={LABEL}>Headcount</span>
          <input
            className={FIELD}
            type="number"
            min={floor}
            step={1}
            value={value.headcount}
            onChange={(event) => onChange({ headcount: event.target.value })}
          />
          <span className="mt-1 block text-xs text-slate-500">
            {floor > 1
              ? `${floor} people are confirmed; the headcount cannot fall below them.`
              : 'A whole number of at least 1.'}
          </span>
        </label>

        <label className="block">
          <span className={LABEL}>Travel</span>
          <select
            className={FIELD}
            value={value.travel}
            disabled={disabled(frozen, 'travel')}
            onChange={(event) => onChange({ travel: event.target.value })}
          >
            {bands.travel.map((travel) => (
              <option key={travel} value={travel}>
                {travel}
              </option>
            ))}
          </select>
        </label>

        <label className="block">
          <span className={LABEL}>Start date</span>
          <input
            className={FIELD}
            type="date"
            value={value.startDate}
            disabled={disabled(frozen, 'startDate')}
            onChange={(event) => onChange({ startDate: event.target.value })}
          />
        </label>

        <label className="block">
          <span className={LABEL}>End date</span>
          <input
            className={FIELD}
            type="date"
            value={value.endDate}
            disabled={disabled(frozen, 'endDate')}
            onChange={(event) => onChange({ endDate: event.target.value })}
          />
        </label>
      </div>

      <fieldset className="rounded border border-slate-200 p-3">
        <legend className={`${LABEL} px-1`}>Skills</legend>
        {value.skills.length === 0 ? (
          <p className="note">No skill requirement.</p>
        ) : (
          <ul className="space-y-2">
            {value.skills.map((row, index) => (
              <li key={`${row.skill}-${index}`} className="grid gap-2 sm:grid-cols-4">
                <select
                  className={FIELD}
                  value={row.skill}
                  disabled={row.kind === 'required' && disabled(frozen, 'requiredSkills')}
                  onChange={(event) => changeRow(index, { skill: event.target.value })}
                >
                  {activeSkills.map((skill) => (
                    <option key={skill.id} value={skill.name}>
                      {skill.name}
                    </option>
                  ))}
                </select>
                <select
                  className={FIELD}
                  value={row.kind}
                  disabled={row.kind === 'required' && disabled(frozen, 'requiredSkills')}
                  onChange={(event) =>
                    changeRow(index, { kind: event.target.value === 'bonus' ? 'bonus' : 'required' })
                  }
                >
                  <option value="required">required</option>
                  <option value="bonus">bonus</option>
                </select>
                <select
                  className={FIELD}
                  value={row.band}
                  disabled={row.kind === 'required' && disabled(frozen, 'requiredSkills')}
                  onChange={(event) => changeRow(index, { band: event.target.value })}
                >
                  {bands.proficiencies.map((band) => (
                    <option key={band} value={band}>
                      {band}
                    </option>
                  ))}
                </select>
                <button
                  type="button"
                  className="justify-self-start rounded border border-slate-300 px-2 py-1 text-sm text-slate-700 disabled:text-slate-400"
                  disabled={row.kind === 'required' && disabled(frozen, 'requiredSkills')}
                  onClick={() => onSkills(value.skills.filter((_, at) => at !== index))}
                >
                  Remove
                </button>
              </li>
            ))}
          </ul>
        )}
        <button
          type="button"
          className="mt-3 rounded border border-slate-300 px-2 py-1 text-sm text-slate-700"
          onClick={() =>
            onSkills([
              ...value.skills,
              {
                skill: activeSkills[0]?.name ?? '',
                kind: 'required',
                band: bands.proficiencies[2] ?? bands.proficiencies[0] ?? '',
              },
            ])
          }
        >
          Add a skill row
        </button>
      </fieldset>

      {error ? (
        <p className="rounded border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-800">{error}</p>
      ) : null}

      <button
        type="submit"
        className="justify-self-start rounded bg-slate-900 px-3 py-1.5 text-sm font-medium text-white disabled:bg-slate-400"
        disabled={busy}
      >
        {busy ? 'Saving…' : submitLabel}
      </button>
    </form>
  );
}
