# rules-govern brainstorming agenda

One question at a time. Record answers in the checkpoint log.

| id | topic | Typical choices |
|---|---|---|
| C1 | Execution `root` | reuse session-confirmed path (`skipped-preanswered`) / ask only if standalone |
| C2 | Mode | `init` (greenfield) / `check-merge` (brownfield) — **inspect first, then suggest** |
| C3 | Languages | none / allowlist only: `cpp,golang,java,kotlin,python,react,react-native,rust,swift,typescript,vue,web` |
| C4 | Host projection | `claude-code` (default) / `cursor` / `codex` / `github-copilot` / `qoder` / `none` |
| C5 | Submodule mapping | `all` / list of submodule ids for Applies To |

**Root reuse:** See [execution-root.md](../../../../vendor/ai4se-plugins/references/execution-root.md).
After `/sdd:repo-understand` and/or `/sdd:bootstrap` in the same session, do not
re-ask C1. Apply consent is never reused from another skill.

Apply is **not** a checkpoint. After dry-run, ask once whether to apply; do not
record an apply id in the checkpoint table.

### C2 mode — inspect then suggest

Before asking C2, inspect `<root>/docs/knowledge/rules/`:

| Observation | Suggested mode |
|---|---|
| Missing, or empty (no rule `.md` beyond empty dirs) | `init` |
| Has `_SCHEMA.md` and/or existing category rule files | `check-merge` |
| Partial tree (dirs only, no Critical rules yet) | Prefer `init`; note partial layout in rationale |

Present the suggestion with a one-line rationale, then confirm with the user.

Notes:
- Authority is always `docs/knowledge/rules/` (MECE). Host files are projections.
- After successful check-merge apply, always refresh host projection (`host=none` skips files).
- Never flatten rule directories.
