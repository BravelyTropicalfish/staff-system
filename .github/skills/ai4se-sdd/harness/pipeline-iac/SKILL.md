---
name: pipeline-iac
description: >-
  Generate or align pipeline/IaC skeletons from Rules and existing templates.
  Use when Rules are ready and skeletons are missing. Prefer wrap; dry-run
  then confirm before writing. Do not invent a second CI/CD product.
disable-model-invocation: true
status: ready
id: skill.harness.pipeline-iac
command: /sdd:pipeline-iac
impl: native
layer: optional
gate: false
---

# Pipeline IaC

## Instructions

Generate or align pipeline and/or IaC skeletons from project Rules and existing
org templates. Prefer wrapping customer templates. **Anti-trigger:** Do not
invent a parallel CI/CD product; skip if skeletons already adequate; do not
trigger deploys (`/sdd:deploy-artifacts`); do not aggregate remote CI results
(`/sdd:pipeline-report`).

Resolve `$SKILL_DIR` as the directory containing this `SKILL.md`. Invoke pack
scripts via `"$SKILL_DIR/scripts/..."` — do not run bare `scripts/...` from the
target repo cwd.

1. **Resolve execution `root`.** Prefer the root already confirmed earlier in
   this session (e.g. after `/sdd:bootstrap` or `/sdd:rules-govern`) — do not
   re-ask. If standalone with no prior root, ask once (may suggest cwd). Stop if
   root is unclear. Convention:
   [execution-root.md](../../../../../vendor/ai4se-plugins/references/execution-root.md).
2. **Rules gate** using
   [iac-pipeline-checklist.md](../../../../../vendor/ai4se-plugins/skills/harness/pipeline-iac/references/iac-pipeline-checklist.md). If
   required blocks are missing for safe generation → stop; return to
   `/sdd:rules-govern` (`stop-incomplete`). Partial gaps may continue as
   `gap-listed` with gaps recorded in the plan.
3. **Brainstorm** remaining open choices one-at-a-time (Superpowers
   brainstorming) using
   [brainstorming-agenda.md](../../../../../vendor/ai4se-plugins/skills/harness/pipeline-iac/references/brainstorming-agenda.md) (C1–C3).
   Materialize confirm is not a checkpoint.
4. **Init checkpoint log** (process record only):

   ```bash
   "$SKILL_DIR/scripts/init-checkpoint-log.sh" --root "<root>"
   ```

   Fill C1–C3 (or `skipped-preanswered`) before probe.
5. **Probe** (read-only):

   ```bash
   "$SKILL_DIR/scripts/probe-pipeline-iac.sh" --root "<root>"
   ```

   Show FOUND/MISSING/NOTE. Update checkpoint Probe.
6. **Draft plan** from probe + Rules + org templates using
   [plan-template.md](../../../../../vendor/ai4se-plugins/skills/harness/pipeline-iac/references/plan-template.md). Persist:

   ```bash
   "$SKILL_DIR/scripts/write-plan.sh" --root "<root>" --apply \
     --body-file "<path-to-agent-written-plan.md>"
   ```

   Path: `docs/ai4se-plugins/<ts>-pipeline-iac-plan.md`. Update checkpoint Plan
   (`mode=dry-run`).
7. **After dry-run:** ask once whether to materialize CREATE items now. If the
   user declines, stop (`mode=dry-run`). If they confirm, the **agent** creates
   only approved missing skeleton files — never overwrite CONFLICT/bespoke;
   never commit secrets; prefer wrap from C2 / existing templates.
8. Summarize created vs skipped vs conflicts. **Next:**
   `/sdd:quality-checklist` (P0 DoV); if Rules gaps remain →
   `/sdd:rules-govern`.

- **In:** execution root; Rules pack; optional org templates; scope C1
- **Out:** checkpoint log + plan report under `docs/ai4se-plugins/`; optional
  skeleton files after confirm
- **Not:** enterprise CD replacement; deploy triggers; CI result aggregation;
  silent Rules rewrites; second CI product YAML invented without Rules
- **Escalate:** human owner if overwrite of bespoke pipelines is requested;
  stop if Rules gate is `stop-incomplete`

**Gotchas:** Secrets never committed — point to secret stores. Optional inside
`/sdd:p0` after `rules-govern`. First-time Rules stay with `/sdd:rules-govern`.

## Examples

```text
User: /sdd:pipeline-iac
Agent: Reuses session root; runs Rules gate; brainstorms C1–C3; probe + plan
report; asks once to materialize CREATE; on confirm writes missing skeletons
only; offers /sdd:quality-checklist.
```

```text
User: /sdd:pipeline-iac (Rules incomplete)
Agent: Stops with stop-incomplete; points to /sdd:rules-govern; no skeleton
writes.
```

## Additional resources

- [Execution root convention](../../../../../vendor/ai4se-plugins/references/execution-root.md)
- [Pipeline / IaC checklist](../../../../../vendor/ai4se-plugins/skills/harness/pipeline-iac/references/iac-pipeline-checklist.md)
- [Brainstorming agenda](../../../../../vendor/ai4se-plugins/skills/harness/pipeline-iac/references/brainstorming-agenda.md)
- [Plan template](../../../../../vendor/ai4se-plugins/skills/harness/pipeline-iac/references/plan-template.md)
- [Checkpoint log template](../../../../../vendor/ai4se-plugins/skills/harness/pipeline-iac/references/checkpoint-log-template.md)
- [init-checkpoint-log.sh](../../../../../vendor/ai4se-plugins/skills/harness/pipeline-iac/scripts/init-checkpoint-log.sh)
- [probe-pipeline-iac.sh](../../../../../vendor/ai4se-plugins/skills/harness/pipeline-iac/scripts/probe-pipeline-iac.sh)
- [write-plan.sh](../../../../../vendor/ai4se-plugins/skills/harness/pipeline-iac/scripts/write-plan.sh)
