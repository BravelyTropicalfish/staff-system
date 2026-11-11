# repo-understand brainstorming agenda

Use Superpowers brainstorming: **one question at a time**. Record each answer in
the checkpoint log before probe / brief.

| id | topic | Typical choices |
|---|---|---|
| C1 | Execution `root` | user path / confirm suggested cwd — **chain-head ask** unless already confirmed this session |
| C2 | `focus` | `full-repo` / user-named relative paths (comma-separated OK) |

Depth is always **standard** — do not offer a quick mode checkpoint.

**Root reuse:** See [execution-root.md](../../../../../../vendor/ai4se-plugins/references/execution-root.md).
On brownfield P0 this skill is usually the chain head. If root was already
confirmed earlier in the session, record C1 as `skipped-preanswered` and do not
re-ask.

Brief persist (Apply) is **not** a checkpoint. After the session brief is ready,
ask once whether to write `docs/ai4se-plugins/<ts>-repo-understand-brief.md`.

Skip only when the user already answered in this session; mark
`skipped-preanswered` in the log.

**Greenfield gate:** After probe, if the tree looks empty / non-meaningful, ask
the user to confirm skip-deep before writing a full five-section brief.
