---
name: sdd-evolve
description: >-
  Propose harness and Rules/ADR evolution after a change. Use in P5 archive
  context with soft change-summary prerequisite. Do not silently rewrite Rules
  without confirmation; hand selected items to /sdd:rules-govern.
disable-model-invocation: true
status: ready
id: skill.harness.evolve
command: /sdd:evolve
impl: native
layer: optional
gate: false
---

# Evolve

## Instructions

During P5 archive context, propose harness tweaks, Rules/ADR candidates, and
prioritized next steps from the completed change. **Anti-trigger:** Not
first-time Rules authoring (`/sdd:rules-govern`); not G3; not tech-debt /
business backlog (`/sdd:followups`). Do not silently rewrite production Rules.

Resolve `$SKILL_DIR` as the directory containing this `SKILL.md`. Invoke pack
scripts via `"$SKILL_DIR/scripts/..."` — do not run bare `scripts/...` from the
target repo cwd.

1. **Confirm `change-id`.** Look for a readable change-summary (project Rules
   path if set, else `change-summary.md` under the change folder).
   - If **missing** → tell the user to run `/sdd:change-summary` first. Do
     **not** auto-run it unless the user explicitly asks. If they confirm
     continue anyway → proceed from change folder / verify residuals /
     conversation context and expect **Partial** (low confidence, possible
     gaps). If they decline both → **stop**.
   - G3 notes are **not** required.
2. **Resolve execution `root`.** Prefer the session-confirmed root — do not
   re-ask. Standalone with no prior root → ask once (may suggest cwd).
   Convention: [execution-root.md](../../../vendor/ai4se-plugins/references/execution-root.md).
3. **Brainstorm** one-at-a-time (Superpowers brainstorming) using
   [brainstorming-agenda.md](../../../vendor/ai4se-plugins/skills/harness/evolve/references/brainstorming-agenda.md) (C1–C3).
   Selecting proposals to adopt is not a checkpoint.
4. **Init checkpoint log** (process record only):

   ```bash
   "$SKILL_DIR/scripts/init-checkpoint-log.sh" --root "<root>"
   ```

   Fill C1–C3 (or `skipped-preanswered`) and Prerequisite before drafting.
5. **Collect evidence read-only** per C1 (summary, optional verify reports,
   harness/Rules layout cues, optional AO asks from C3). Draft three buckets
   from [proposals-outline.md](../../../vendor/ai4se-plugins/skills/harness/evolve/references/proposals-outline.md): `harness`,
   `rules-adr`, `next-steps`. Separate `must-fix` vs `nice-evolve`. Divert
   tech-debt / business backlog to `/sdd:followups`. **Never invent** items;
   thin evidence → empty/Partial + Gaps + low confidence.
6. **Persist proposals** (always):

   ```bash
   "$SKILL_DIR/scripts/write-proposals.sh" --root "<root>" --apply \
     --body-file "<path-to-agent-written-proposals.md>"
   ```

   Path: `docs/ai4se-plugins/<ts>-evolve-proposals.md`. Never overwrite
   proposal / Delta / Living / Rules authority bodies.
7. **Ask which items to adopt.** Do **not** rewrite Rules here. Hand selected
   items to `/sdd:rules-govern` (or record as future change work).
8. Summarize counts. **Next:** `/sdd:rules-govern` for selections; optionally
   `/sdd:followups` and `/sdd:g3-decision-record`. This skill **never** writes
   Gate Pass/Fail.

- **In:** change-id; preferred change-summary; optional verify / AO asks;
  current Rules/harness layout
- **Out:** checkpoint log + evolve-proposals report under `docs/ai4se-plugins/`
- **Not:** silent Rules rewrite; G3; followups tech-debt/business; first-time
  Rules authoring; inventing proposals
- **Escalate:** human when change-id missing, prerequisite declined, or
  auto-apply of Rules is requested

**Gotchas:** First-version Rules stay with `rules-govern`; evolve is P5
refinement. Optional inside `/sdd:p5` before G3. Low-confidence suggestions
must be labeled as such.

## Examples

```text
User: /sdd:evolve change-id=add-foo
Agent: Finds change-summary; brainstorms C1–C3; drafts three-bucket proposals;
writes docs/ai4se-plugins/<ts>-evolve-proposals.md; asks which items to hand
to /sdd:rules-govern.
```

```text
User: /sdd:evolve change-id=add-foo (no change-summary)
Agent: Asks to run /sdd:change-summary first; on “continue anyway”, emits
Partial/low-confidence proposals with gaps; does not invent or rewrite Rules.
```

## Additional resources

- [Execution root convention](../../../vendor/ai4se-plugins/references/execution-root.md)
- [Proposals outline](../../../vendor/ai4se-plugins/skills/harness/evolve/references/proposals-outline.md)
- [Brainstorming agenda](../../../vendor/ai4se-plugins/skills/harness/evolve/references/brainstorming-agenda.md)
- [Checkpoint log template](../../../vendor/ai4se-plugins/skills/harness/evolve/references/checkpoint-log-template.md)
- [init-checkpoint-log.sh](../../../vendor/ai4se-plugins/skills/harness/evolve/scripts/init-checkpoint-log.sh)
- [write-proposals.sh](../../../vendor/ai4se-plugins/skills/harness/evolve/scripts/write-proposals.sh)
