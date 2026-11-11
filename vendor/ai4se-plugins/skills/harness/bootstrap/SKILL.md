---
name: bootstrap
description: >-
  Establish project baseline layout and knowledge-base slots for an AI-assisted
  SDD repo. Ask execution root, brainstorm checkpoints, write checkpoint log,
  then dry-run/apply skeleton scripts. Do not use for one-off business feature
  coding.
disable-model-invocation: true
status: ready
id: skill.harness.bootstrap
command: /sdd:bootstrap
impl: native
layer: core
gate: false
---

# Bootstrap

## Instructions

Establish or align the AI-assisted SDD baseline (directory skeleton + knowledge
slots + short stubs). **Anti-trigger:** Do not write business feature code; do
not install OpenSpec/Superpowers binaries; do not author full Rules policy
(`/sdd:rules-govern`); do not replace `/sdd:pipeline-iac`.

Resolve `$SKILL_DIR` as the directory containing this `SKILL.md` (the bootstrap
skill root). Invoke pack scripts via `"$SKILL_DIR/scripts/..."` — do not run bare
`scripts/...` from the target repo cwd.

### Pack default tree (review)

Show this tree to the user before apply. Full notes, deferred items, and
checkpoint-driven omissions:
[directory-skeleton.md](./references/directory-skeleton.md).

```text
<repo-root>/
├── AGENTS.md
├── README.md
├── docs/
│   ├── knowledge/                 # when extra-knowledge=docs/knowledge
│   │   ├── _SCHEMA.md
│   │   ├── _INDEX.md
│   │   ├── _LOG.md
│   │   ├── architecture/
│   │   ├── domain/shared/
│   │   ├── rules/
│   │   ├── interfaces/
│   │   ├── operations/
│   │   └── external/
│   ├── feedback/
│   │   ├── verification-matrix.md
│   │   ├── verification-commands.md
│   │   └── evaluator-rubric.md
│   └── ai4se-plugins/             # /sdd:* checkpoint logs only
├── requirement-workspace/
│   ├── README.md
│   ├── handoff.md
│   ├── registry.md
│   ├── intents/
│   ├── requirements/
│   ├── _templates/
│   ├── analysis/
│   ├── ready/
│   └── archive/
├── openspec/                      # when spec-backend=openspec
│   ├── config.yaml
│   ├── templates/{change-spec,main-spec}.md
│   ├── specs/
│   └── changes/archive/
└── scripts/                       # harness tools: README + customize-prompt shells
    ├── README.md
    ├── init.sh                    # placeholder — prompts customize on run
    ├── kb-lint.sh                 # placeholder — KB lint + _INDEX.md
    ├── verify-links.sh            # placeholder — bidirectional links
    └── archive-change.sh          # placeholder — archive helper
```

1. **Resolve execution `root`.** Prefer the root already confirmed earlier in
   this session (e.g. after `/sdd:repo-understand`) — mark C1
   `skipped-preanswered`, do not re-ask. If this skill is the chain head
   (greenfield / standalone), ask once (may suggest cwd). Stop if root is
   unclear. Convention:
   [execution-root.md](../../../references/execution-root.md).
2. **Brainstorm** open choices one-at-a-time using Superpowers brainstorming and
   [brainstorming-agenda.md](./references/brainstorming-agenda.md) (C1–C4). Prefer
   `extra-knowledge=docs/knowledge` for a full KB MECE layout unless the user
   wants `docs-only`. Do **not** scaffold CI/CD here — that is `/sdd:pipeline-iac`
   after `/sdd:rules-govern`. Apply is not a checkpoint.
3. **Init checkpoint log** (process record only — not Living Spec/Rules authority):

   ```bash
   "$SKILL_DIR/scripts/init-checkpoint-log.sh" --root "<root>"
   ```

   Fill Checkpoints C1–C4 (or explicit defer / `skipped-preanswered`) before
   dry-run.
4. **Dry-run plan** with flags matching checkpoints (adjust values per C2–C4):

   ```bash
   "$SKILL_DIR/scripts/plan-skeleton.sh" --root "<root>" \
     --spec-backend openspec \
     --agents-md create-stub \
     --extra-knowledge docs/knowledge
   ```

   Show CREATE/SKIP **and** the review tree above. Update the checkpoint log
   Apply section with the plan summary.
5. **After dry-run:** ask once whether to apply now. Keep the ask short — do not
   say “To materialize”. If the user declines, stop (log mode=`dry-run`). If they
   confirm (or already gave explicit `--apply` intent):

   ```bash
   "$SKILL_DIR/scripts/apply-skeleton.sh" --root "<root>" --apply \
     --spec-backend ... --agents-md ... --extra-knowledge ...
   ```

   Additive only: never delete; never overwrite non-empty files.
6. Summarize created vs skipped. **Optional next:** `/sdd:rules-govern`, then
   `/sdd:quality-checklist` (P0 DoV); later `/sdd:pipeline-iac` only if a
   pipeline skeleton is still needed. Offer as options — user may stop or reorder.

- **In:** execution root; brainstorming outcomes (C2–C4)
- **Out:** additive skeleton + stubs; checkpoint log under
  `docs/ai4se-plugins/<timestamp>-bootstrap-checkpoints.md`; create/skip notes
- **Not:** business code; upstream installs; full Rules policy text; second
  long-term specs authority tree when `other` is named; invented sub-module
  folders without user-named paths
- **Escalate:** human owner if destructive moves would be required; stop if
  required agenda answers refused without safe default

**Gotchas:** Brownfield may already have partial trees — prefer additive
alignment. `requirement-workspace/**` is shared with `/sdd:pre-spec-workdir`;
existing requirement paths are skipped, missing ones are filled. Do not
hand-create large directory trees without dry-run + checkpoint log + user apply
confirmation. `docs/ai4se-plugins/` checkpoint logs are process records only.
C4 custom additive paths are log-only in v1 — scripts accept only `docs-only` or
`docs/knowledge`; do not hand-create custom trees outside scripts.

## Examples

```text
User: /sdd:bootstrap
Agent: Reuses session root if already confirmed (else asks once); shows pack
default tree; brainstorms C2–C4 (prefers full KB). Runs init-checkpoint-log.sh
+ plan-skeleton.sh; shows CREATE/SKIP; asks whether to apply; on confirm runs
apply-skeleton.sh --apply; offers optional next (e.g. rules-govern, same root).
```

```text
User: /sdd:bootstrap (root=/tmp/demo, dry-run only)
Agent: Writes checkpoint log; prints plan + review tree; does not call apply.
```

## Additional resources

- [Execution root convention](../../../references/execution-root.md)
- [Directory skeleton](./references/directory-skeleton.md)
- [AGENTS stub](./references/agents-stub.md)
- [OpenSpec placeholders](./references/openspec-placeholders.md)
- [Brainstorming agenda](./references/brainstorming-agenda.md)
- [Checkpoint log template](./references/checkpoint-log-template.md)
- [OpenSpec integration](../../../integrations/openspec.md)
- [init-checkpoint-log.sh](./scripts/init-checkpoint-log.sh)
- [plan-skeleton.sh](./scripts/plan-skeleton.sh)
- [apply-skeleton.sh](./scripts/apply-skeleton.sh)
