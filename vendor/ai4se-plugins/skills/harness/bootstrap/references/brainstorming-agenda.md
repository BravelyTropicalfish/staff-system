# Bootstrap brainstorming agenda

Use Superpowers brainstorming: **one question at a time**. Record each answer in
the checkpoint log before dry-run/apply.

| id | topic | Typical choices |
|---|---|---|
| C1 | Execution `root` | reuse session-confirmed path (`skipped-preanswered`) / ask only if chain head |
| C2 | Spec backend slot | `openspec` (default) / `defer` / `other` (name only) |
| C3 | `AGENTS.md` | `create-stub` / `skip-if-exists` / `defer` |
| C4 | Knowledge tree | `docs/knowledge` (full KB MECE layout, recommended) / `docs-only` / custom (log-only in v1) |

**Root reuse:** See [execution-root.md](../../../../references/execution-root.md).
After `/sdd:repo-understand` in the same session, do not re-ask C1. Greenfield
P0 (skipped understand) → this skill is chain head and asks once.

Apply is **not** a checkpoint. After dry-run, ask once whether to apply; do not
record an apply id in the checkpoint table. Apply consent is never reused from
another skill.

Skip only when the user already answered in this session; mark
`skipped-preanswered` in the log. Do not invent a second long-term specs tree
when `other` is chosen — document the name and stop short of scaffolding it.

**Always scaffolded (when apply runs):** `docs/feedback/`, `requirement-workspace/`
(shared layout with `/sdd:pre-spec-workdir`; existing dirs/files are skipped),
`scripts/` slot, `docs/ai4se-plugins/` — see [directory-skeleton.md](./directory-skeleton.md).

**CI / pipeline:** Out of scope for bootstrap. Do not create `.github/workflows/`
here. Pipeline policy belongs in `/sdd:rules-govern`; skeleton generation belongs
in `/sdd:pipeline-iac` after Rules are ready.

**C4 custom paths (v1):** If the user names custom additive paths, record them
in the checkpoint log only. Scripts support `--extra-knowledge docs-only` or
`docs/knowledge` — do not hand-create custom directory trees outside the scripts.

**Sub-module AGENTS.md:** Ask whether named modules exist; record paths in the
checkpoint log. Do not invent module folders in v1 scripts.
