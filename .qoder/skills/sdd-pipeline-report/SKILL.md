---
name: sdd-pipeline-report
description: >-
  Aggregate remote CI/CD jobs (phase=ci|cd|all) with E1 判定, failure summaries,
  and security/residual-risk structure into docs/ai4se-plugins/ (optional
  ci-report.md E1 slot). Use after PR or deploy. Read-only; do not trigger
  deploys. If CLI/API unavailable, record named human confirmation in the same
  report.
disable-model-invocation: true
status: ready
id: skill.verify.pipeline-report
command: /sdd:pipeline-report
impl: native
layer: core
gate: false
---

# Pipeline report

## Instructions

Aggregate remote CI and/or CD automated test reports (`phase=ci|cd|all`) into one
normalized verify report. **Anti-trigger:** Default read-only; do not trigger
deploys (`/sdd:deploy-artifacts`); do not run local build/scan
(`/sdd:local-verify-report`); do not write G2; do not escalate to deploy
credentials for convenience.

Resolve `$SKILL_DIR` as the directory containing this `SKILL.md`. Invoke pack
scripts via `"$SKILL_DIR/scripts/..."` — do not run bare `scripts/...` from the
target repo cwd.

**Root default:** use cwd as `--root` unless the user names another path.
Honor the frozen permission model in
[cicd integration](../../../vendor/ai4se-plugins/integrations/cicd.md).

1. **Resolve inputs.** `root` = cwd (or user path). Require
   `phase=ci|cd|all` — if omitted, ask (C1). Collect change-id / MR·PR /
   commit SHA / optional pipeline or workflow run id (C2).
2. **Brainstorm** one-at-a-time using Superpowers brainstorming and
   [brainstorming-agenda.md](../../../vendor/ai4se-plugins/skills/verify/pipeline-report/references/brainstorming-agenda.md) (C1–C2).
   Report persist is **always on**.
3. **Init checkpoint log:**

   ```bash
   "$SKILL_DIR/scripts/init-checkpoint-log.sh" --root "<root>"
   ```

   Fill C1–C2 (or `skipped-preanswered`) before probe.
4. **Probe** (read-only) — remotes, host heuristic, `glab`/`gh` on PATH:

   ```bash
   "$SKILL_DIR/scripts/probe-pipeline.sh" --root "<root>"
   ```

   Show FOUND/MISSING/NOTE. Targets:
   [probe-targets.md](../../../vendor/ai4se-plugins/skills/verify/pipeline-report/references/probe-targets.md). Update checkpoint Probe.
5. **Fetch (agent, read-only).** Prefer GitLab via `glab` / read APIs per
   `integrations/cicd.md`. For GitHub Actions, follow the Wave-1 usage notes in
   the same file (secondary). Normalize per job: name, stage, status,
   duration, html/web URL, failure_reason. Prefer stages
   build/test/lint/secret-scan/sast/audit when present. **Never** trigger
   pipelines or deploys from this skill.
6. **No CLI/API/permission.** Do **not** ask for screenshots or oral evidence.
   Ask a **named human** whether to accept the phase(s) without remote fetch.
   - Confirmed → fill `## Human confirmation` in the report; Overall =
     **Partial** with explicit `Pass-by-human-confirm` — never claim
     API-verified Pass.
   - Refused and no remote data → Overall Fail/Partial; document the gap; stop.
7. **Write the report** from [report-template.md](../../../vendor/ai4se-plugins/skills/verify/pipeline-report/references/report-template.md)
   including **E1 判定**, failure details (`[fact]` / `[inference]`), security
   table, and residual risks. `phase=all` → one file with CI and CD sections.
   Always persist:

   ```bash
   "$SKILL_DIR/scripts/write-report.sh" --root "<root>" --apply \
     --body-file "<path-to-agent-written-report.md>"
   ```

   Path: `docs/ai4se-plugins/<ts>-pipeline-report.md`. When a change-id is in
   scope and the project uses the OpenSpec E1 slot, also write
   `openspec/changes/<change-id>/ci-report.md` (same body or E1 subset) for
   `/sdd:local-verify-report` consumption.
8. **Next.** `phase=ci` → often `/sdd:deploy-artifacts` after merge (apply
   lane). After required phases are covered → `/sdd:local-verify-report`
   (E1 ready) and/or `/sdd:g2-decision-record`. On ❌ E1, tell the user to fix
   before verify — do not repair code here.

- **In:** root (default cwd); phase; target refs; read-only CI/CD access
- **Out:** pipeline report + checkpoint log under `docs/ai4se-plugins/`;
  normalized tables with E1 判定 / failure details / security + residual risks;
  optional `ci-report.md` E1 slot; optional Human confirmation block
- **Not:** deploy triggers; local verify; G2 decisions; screenshot/oral
  evidence collection; mandatory bundled API client; self-confirm as the agent;
  code repair / DDRV
- **Escalate:** named human for confirmation when tools/auth missing; setup
  guidance for `glab`/tokens; stop when confirm refused and no remote data

**Gotchas:** Do not elevate to deploy credentials. Apply vs verify stay
separated — this skill is verify-side, read-only by default.
`docs/ai4se-plugins/` files are process records, not Living Spec authority.

## Examples

```text
User: /sdd:pipeline-report phase=ci
Agent: Uses cwd; records C1/C2; probe-pipeline; fetches via glab (read-only);
       writes docs/ai4se-plugins/<ts>-pipeline-report.md; suggests next step.
```

```text
User: /sdd:pipeline-report phase=ci (no glab / no token)
Agent: Does not ask for screenshots; asks named human to confirm acceptance;
       persists report with ## Human confirmation; Overall Partial
       (Pass-by-human-confirm).
```

```text
User: /sdd:pipeline-report phase=all
Agent: One report with CI and CD sections; both must have remote data or their
       own human confirmation before Overall can be Pass / accepted Partial.
```

## Additional resources

- [Brainstorming agenda](../../../vendor/ai4se-plugins/skills/verify/pipeline-report/references/brainstorming-agenda.md)
- [Checkpoint log template](../../../vendor/ai4se-plugins/skills/verify/pipeline-report/references/checkpoint-log-template.md)
- [Report template](../../../vendor/ai4se-plugins/skills/verify/pipeline-report/references/report-template.md)
- [Probe targets](../../../vendor/ai4se-plugins/skills/verify/pipeline-report/references/probe-targets.md)
- [CI/CD integration](../../../vendor/ai4se-plugins/integrations/cicd.md)
- [init-checkpoint-log.sh](../../../vendor/ai4se-plugins/skills/verify/pipeline-report/scripts/init-checkpoint-log.sh)
- [probe-pipeline.sh](../../../vendor/ai4se-plugins/skills/verify/pipeline-report/scripts/probe-pipeline.sh)
- [write-report.sh](../../../vendor/ai4se-plugins/skills/verify/pipeline-report/scripts/write-report.sh)
