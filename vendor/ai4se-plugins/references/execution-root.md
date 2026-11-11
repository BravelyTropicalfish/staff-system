# Execution root (P0 chain)

Confirm `root` **once** at the chain head in this session, then reuse.

| Situation | Chain head |
|---|---|
| Brownfield `/sdd:p0` | `/sdd:repo-understand` |
| Greenfield (skip understand) | `/sdd:bootstrap` |
| Standalone skill (no prior root in session) | That skill asks once |

**Later skills in the same session** (`bootstrap` after understand, `rules-govern` after either, etc.):

1. Reuse the already-confirmed absolute `root`.
2. In the checkpoint log, mark C1 (`root`) as `skipped-preanswered` with that path in `choice`.
3. Do **not** ask again unless the user names a different path or starts a new session.

Apply authorization (dry-run → apply) remains **per skill** — never reuse apply consent across skills.

Skills that default to cwd without a root checkpoint (`quality-checklist`, verify reports) keep that behavior; if the session already has a confirmed root, prefer it over a silent cwd guess when they would otherwise write under the wrong tree.
