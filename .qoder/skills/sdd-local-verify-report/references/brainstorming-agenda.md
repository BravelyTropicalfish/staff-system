# local-verify-report brainstorming agenda

Use Superpowers brainstorming: **one question at a time**. Record each answer in
the checkpoint log before probe / run.

| id | topic | Typical choices |
|---|---|---|
| C1 | `scope` | `local-only` / `local+evidence` (default `local+evidence` when change artifacts exist) |
| C2 | `command-source` | Confirm whitelist file path(s) from probe (e.g. `docs/feedback/verification-commands.md`) |

Report and checkpoint persist are **always on** — do not offer a session-only
checkpoint.

Skip only when the user already answered in this session; mark
`skipped-preanswered` in the log.

**Root:** default to cwd; do not add a root confirmation checkpoint unless the
user names a different path.
