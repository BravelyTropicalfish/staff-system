# pipeline-iac brainstorming agenda

Use Superpowers brainstorming: **one question at a time**. Record each answer in
the checkpoint log before writing the plan report.

| id | topic | Typical choices |
|---|---|---|
| C1 | Scope | `ci` \| `iac` \| `both` |
| C2 | Org template path | path / `none` |
| C3 | Rules gate result | `pass` \| `gap-listed` \| `stop-incomplete` |

**Execution root:** Not a brainstorming checkpoint. Prefer the session-confirmed
root from earlier P0 steps (see [execution-root.md](../../../../vendor/ai4se-plugins/references/execution-root.md)).
If invoked standalone with no prior root, ask once (technical ask) or use the
path the user already named.

Apply / materialize confirm is **not** a checkpoint. After dry-run plan, ask once
whether the agent should create approved CREATE items. Do not reuse apply consent
from another skill.

Skip only when the user already answered in this session; mark
`skipped-preanswered` in the log.

**CI product ban:** Do not invent enterprise CD platform semantics. Prefer wrap
of C2 templates or existing repo files discovered by probe.
