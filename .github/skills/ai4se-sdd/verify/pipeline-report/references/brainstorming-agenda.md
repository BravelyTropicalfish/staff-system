# pipeline-report brainstorming agenda

Use Superpowers brainstorming: **one question at a time**. Record each answer in
the checkpoint log before probe / fetch.

| id | topic | Typical choices |
|---|---|---|
| C1 | `phase` | `ci` / `cd` / `all` |
| C2 | `target` | branch / commit SHA / MR iid / pipeline id / workflow_run_id |

Report and checkpoint persist are **always on**.

Skip only when the user already answered in this session; mark
`skipped-preanswered` in the log.

**Root:** default to cwd; do not add a root confirmation checkpoint unless the
user names a different path.
