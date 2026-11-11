---
name: quality-checklist
description: >-
  Run stage DoV completeness/consistency checks (p0–p5) with evidence paths.
  Persist a report under docs/ai4se-plugins/. Use at recipe checkpoints. Do not
  approve Gates or silently rewrite production code to force a Pass.
disable-model-invocation: true
status: ready
id: skill.common.quality-checklist
command: /sdd:quality-checklist
impl: native
layer: core
gate: false
---

# Quality checklist

## Instructions

Run stage DoV checks and mark each item Pass / Fail / Partial / Skipped with
evidence paths. **Anti-trigger:** Do not approve G1/G2/G3; do not silently
convert Partial to Pass; do not rewrite production code just to force Pass
(unless the user explicitly asks to fix a named gap).

Resolve `$SKILL_DIR` as the directory containing this `SKILL.md`. Invoke pack
scripts via `"$SKILL_DIR/scripts/..."` — do not run bare `scripts/...` from the
target repo cwd.

**Root default:** use cwd as `--root` unless the user names another path.
Report and checkpoint persist are **always on**.

1. **Resolve inputs.** `root` = cwd (or user path). For C1, present stages as a
   **list** that includes `all` (全部阶段) plus `p0`…`p5` with English + 中文
   labels — see [brainstorming-agenda.md](./references/brainstorming-agenda.md).
   Multi-select allowed. If the user picks `all` / says all / selects none /
   leaves stage omitted, use `stage=all` (`p0,p1,p2,p3,p4,p5`).
2. **Brainstorm** one-at-a-time using Superpowers brainstorming and
   [brainstorming-agenda.md](./references/brainstorming-agenda.md) (C1–C2). For
   C2 `focus`, list `full-stage` plus each check id for the selected stage(s)
   (e.g. `P0-1`) with English meaning **and** 中文 from the agenda tables —
   do not ask for bare ids without explanations.
3. **Init checkpoint log:**

   ```bash
   "$SKILL_DIR/scripts/init-checkpoint-log.sh" --root "<root>"
   ```

   Fill C1–C2 (or `skipped-preanswered`) before probe.
4. **Probe** (read-only) evidence paths for selected stage(s):

   ```bash
   "$SKILL_DIR/scripts/probe-stage.sh" --root "<root>" --stage "<all|pN|p0,p2>"
   ```

   Show FOUND/MISSING/NOTE. When multiple stages are selected, keep findings
   grouped by `NOTE stage:<pN>`. Targets:
   [probe-targets.md](./references/probe-targets.md). Update checkpoint Probe.
5. **Evaluate** using [checklists](./references/checklists/) for each selected
   stage (`p0.md`…`p5.md`), applying **focus**:
   - `full-stage` (default): evaluate every checklist row for the selected
     stage(s).
   - Subset of check ids (e.g. `P0-1,P0-2`): evaluate **only** those ids.
     Other rows → **Skipped** with rationale `out-of-focus` (do not Fail them).
   - **Overall scope:** aggregate **required items that were in focus** only.
     Out-of-focus Skipped rows never block Pass. Optional items with no
     evidence / not yet run → **Skipped** (do not block Overall Pass); if
     evidence exists, score Pass/Fail/Partial normally. Do not treat catalog
     `ready` skills as automatic Skipped.
   - Required Fail → Overall Fail; no Fail but any required Partial → Partial;
     all in-focus required Pass → Overall Pass. Multi-stage: per-stage Overall
     plus roll-up (Fail if any selected stage fails; else Partial if any
     selected stage is partial; else Pass).
6. **Always persist** the report from
   [report-template.md](./references/report-template.md):

   ```bash
   "$SKILL_DIR/scripts/write-report.sh" --root "<root>" --apply \
     --body-file "<path-to-agent-written-report.md>"
   ```

   Path: `docs/ai4se-plugins/<ts>-quality-checklist-report.md`.
7. **Summarize** gaps with owners if known. Suggest next `/sdd` remediations.
   Never invoke or impersonate Gate decision skills.

- **In:** root (default cwd); stage selection (`all` or selected stage ids);
  optional focus subset; artifact paths
- **Out:** Pass/Fail/Partial/Skipped table + Overall; checkpoint log + report
  under `docs/ai4se-plugins/`; open gaps
- **Not:** Gate approval; silent Partial→Pass; production edits unless user
  asks to fix a named gap; replacing delta-craft AC/DoV deep review
- **Escalate:** user when stage/DoV set is unclear; human owners for open Gaps
- **Failure:**
  - Invalid `--root` / script exit ≠ 0 on `init-checkpoint-log` or
    `probe-stage` → **stop**; report the error; do **not** invent Pass; do
    **not** write a report that claims a completed evaluation.
  - Probe hard-fails (permission, missing root) → stop after updating the
    checkpoint Probe section with the error if the log already exists.
  - Evaluate/write path: if the agent produced a partial evaluation body and
    `write-report` fails → surface the script error; keep the
    agent-written body in session; do not claim Overall from an unpersisted
    draft unless the user accepts session-only.
  - Never convert a failed run into Overall Pass.

**Gotchas:** This skill is a stage DoV runner, not a Gate. Optional / not-run
items are Skipped. Focus subsets use `out-of-focus` Skipped. `docs/ai4se-plugins/`
files are process records only.

## Examples

```text
User: /sdd:quality-checklist stage=p0
Agent: Uses cwd; C1/C2; init-checkpoint-log + probe-stage; evaluates p0
       checklist; write-report --apply; Overall from in-focus required items.
```

```text
User: /sdd:quality-checklist
Agent: Shows C1 as a list including all + p0–p5 (EN + 中文). If user picks all
       or none, probes all stages and writes one combined report with per-stage
       Overall plus roll-up Overall. Then C2 lists full-stage and check ids
       (e.g. P0-1) with EN + 中文 meanings for the selected stage(s).
```

```text
User: /sdd:quality-checklist stage=p1
Agent: Scores required Pre-SDD / impact / proposal|stories; optional explore /
       agent-evalset → Skipped only when no evidence / not run; persists report.
```

```text
User: /sdd:quality-checklist stage=p0 focus=P0-1,P0-2
Agent: Evaluates only P0-1/P0-2; other p0 rows Skipped out-of-focus; Overall from
       those in-focus required items only; persists report.
```

```text
User: /sdd:quality-checklist (Partial on Rules gaps)
Agent: Overall Partial; lists gaps; does not convert to Pass or write a Gate.
```

```text
User: /sdd:quality-checklist (probe-stage exits non-zero)
Agent: Stops; shows script error; does not write a completed Overall Pass report.
```

## Additional resources

- [Brainstorming agenda](./references/brainstorming-agenda.md)
- [Checkpoint log template](./references/checkpoint-log-template.md)
- [Report template](./references/report-template.md)
- [Probe targets](./references/probe-targets.md)
- [Stage checklists](./references/checklists/)
- [init-checkpoint-log.sh](./scripts/init-checkpoint-log.sh)
- [probe-stage.sh](./scripts/probe-stage.sh)
- [write-report.sh](./scripts/write-report.sh)
