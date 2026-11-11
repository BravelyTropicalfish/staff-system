---
name: repo-understand
description: >-
  Map code, specs, and harness slots before baseline or change work. Ask
  execution root, brainstorm focus, write checkpoint log, probe layout
  (read-only), then produce a standard-depth brief with optional persist.
  Do not modify the repo business tree.
disable-model-invocation: true
status: ready
id: skill.common.repo-understand
command: /sdd:repo-understand
impl: native
layer: core
gate: false
---

# Repo understand

## Instructions

Produce a grounded short brief of the repository (brownfield P0 first step;
optional refresh before impact-analysis). **Anti-trigger:** Do not modify
business/authority trees; do not write feature code; do not replace
`/sdd:bootstrap` or `/sdd:rules-govern`; do not approve Gates.

Resolve `$SKILL_DIR` as the directory containing this `SKILL.md`. Invoke pack
scripts via `"$SKILL_DIR/scripts/..."` — do not run bare `scripts/...` from the
target repo cwd.

Depth is always **standard** (no quick mode). Prefer manifests, README,
AGENTS, and spec/harness roots before deep random files. Do not dump huge
file lists.

1. **Resolve execution `root` (chain-head).** On brownfield `/sdd:p0` this skill
   is usually the chain head — ask once (may suggest cwd). If this session
   already confirmed a root earlier, reuse it and mark C1
   `skipped-preanswered`. Stop if root is unclear. Convention:
   [execution-root.md](../../../references/execution-root.md).
2. **Brainstorm** open choices one-at-a-time using Superpowers brainstorming and
   [brainstorming-agenda.md](./references/brainstorming-agenda.md) (C1–C2).
   Brief persist is not a checkpoint.
3. **Init checkpoint log** (process record only — not Living Spec/Rules
   authority):

   ```bash
   "$SKILL_DIR/scripts/init-checkpoint-log.sh" --root "<root>"
   ```

   Fill Checkpoints C1–C2 (or explicit defer / `skipped-preanswered`) before
   probe.
4. **Probe layout** (read-only):

   ```bash
   "$SKILL_DIR/scripts/probe-layout.sh" --root "<root>" [--focus "path,path"]
   ```

   Show FOUND/MISSING/NOTE. Update the checkpoint log Probe section. Probe
   targets: [probe-targets.md](./references/probe-targets.md).
5. **Greenfield gate:** If `NOTE greenfield_suspected:yes` (or the tree looks
   empty), ask the user to confirm skip-deep.
   - Confirmed skip → short conclusion + suggest `/sdd:bootstrap`; optionally
     persist a short brief; stop.
   - Confirmed brownfield / denied → continue.
6. **Write session brief** using [brief-template.md](./references/brief-template.md)
   (all five sections). Mark uncertainties explicitly. Suggest next `/sdd`
   (usually `/sdd:bootstrap`; if harness exists, a change-oriented command).
7. **After brief:** ask once whether to persist under `docs/ai4se-plugins/`.
   Keep the ask short. If the user declines, stop (Brief mode=`session-only`).
   If they confirm:

   ```bash
   "$SKILL_DIR/scripts/write-brief.sh" --root "<root>" --apply \
     --body-file "<path-to-agent-written-brief.md>"
   ```

   Writes only the brief file; never mutates AGENTS/openspec/knowledge/source.

- **In:** execution root; focus; optional brief-persist authorization
- **Out:** session brief; checkpoint log under
  `docs/ai4se-plugins/<timestamp>-repo-understand-checkpoints.md`; optional
  brief file `<timestamp>-repo-understand-brief.md`
- **Not:** repo business-tree modifications; inventing missing specs; full-tree
  dumps; treating log/brief as Living Spec or Rules authority
- **Escalate:** human when root unclear; greenfield confirmation refused without
  safe path; focus still too large; probe permission failures

**Gotchas:** Brownfield may already have partial harness — report FOUND/MISSING
honestly. `docs/ai4se-plugins/` files are process records only. Never hand-write
large trees; this skill is read-only except checkpoint + optional brief.

## Examples

```text
User: /sdd:repo-understand
Agent: Confirms root once (chain-head); brainstorms focus; init-checkpoint-log +
       probe-layout; if not greenfield, writes five-section brief; asks whether
       to persist; on confirm write-brief --apply; suggests /sdd:bootstrap
       (same root, no re-ask).
```

```text
User: /sdd:repo-understand (empty root)
Agent: Probe notes greenfield_suspected; confirms skip-deep with user; short
       conclusion + /sdd:bootstrap; optional short brief persist.
```

## Additional resources

- [Execution root convention](../../../references/execution-root.md)
- [Brainstorming agenda](./references/brainstorming-agenda.md)
- [Checkpoint log template](./references/checkpoint-log-template.md)
- [Brief template](./references/brief-template.md)
- [Probe targets](./references/probe-targets.md)
- [init-checkpoint-log.sh](./scripts/init-checkpoint-log.sh)
- [probe-layout.sh](./scripts/probe-layout.sh)
- [write-brief.sh](./scripts/write-brief.sh)
