---
name: sdd-local-verify-report
description: >-
  Collect local build/scan/review and optional OpenSpec evidence (V1–V8,
  Necessity, E1/E2/E3, five-question map, human-review assist) into a verify
  report under docs/ai4se-plugins/. Use before PR. Fail blocks pr-submit unless
  a complete human-signed Waiver is embedded in the report.
disable-model-invocation: true
status: ready
id: skill.verify.local-verify-report
command: /sdd:local-verify-report
impl: native
layer: core
gate: false
---

# Local verify report

## Instructions

Run/collect local build, static scans, code-review notes, and (when in scope)
OpenSpec evidence into one human-readable verify report before PR.
**Anti-trigger:** Not a G2 decision; not remote CI aggregation
(`/sdd:pipeline-report`); do not open PRs; do not self-approve waivers.

Resolve `$SKILL_DIR` as the directory containing this `SKILL.md`. Invoke pack
scripts via `"$SKILL_DIR/scripts/..."` — do not run bare `scripts/...` from the
target repo cwd.

**Root default:** use the current working directory as `--root` unless the user
names another path. Do not force a root confirmation ask.

1. **Resolve inputs.** `root` = cwd (or user path). Collect `change-id` when
   evidence may apply. If the user states there is no OpenSpec change, evidence
   stays Skipped — do not fake Pass.
2. **Brainstorm** open choices one-at-a-time using Superpowers brainstorming and
   [brainstorming-agenda.md](../../../vendor/ai4se-plugins/skills/verify/local-verify-report/references/brainstorming-agenda.md) (C1–C2).
   Record answers in the checkpoint log. Report persist is **always on** (no
   session-only checkpoint).
3. **Init checkpoint log** (process record only — not Living Spec/Rules
   authority):

   ```bash
   "$SKILL_DIR/scripts/init-checkpoint-log.sh" --root "<root>"
   ```

   Fill Checkpoints C1–C2 (or `skipped-preanswered`) before probe.
4. **Probe** (read-only) — command whitelist sources, tools, change artifacts:

   ```bash
   "$SKILL_DIR/scripts/probe-verify.sh" --root "<root>" [--change-id "<id>"]
   ```

   Show FOUND/MISSING/NOTE. Targets:
   [probe-targets.md](../../../vendor/ai4se-plugins/skills/verify/local-verify-report/references/probe-targets.md). Update the checkpoint log
   Probe section. If `NOTE whitelist_empty:yes` → Overall must not be Pass.
5. **Run whitelist commands only** — never invent or infer commands:

   ```bash
   "$SKILL_DIR/scripts/run-verify.sh" --root "<root>" \
     --commands-file "<path-from-probe-or-agent>"
   ```

   Prefer the commands file emitted by probe (`COMMANDS_FILE …`). If probe found
   no commands, skip the runner and mark Build/Scan Fail or Partial with the
   gap. Capture each exit code + summary into the report table.
6. **Evidence layer (agent)** when C1=`local+evidence` and change artifacts
   exist: run [evidence-checklist.md](../../../vendor/ai4se-plugins/skills/verify/local-verify-report/references/evidence-checklist.md)
   (V1–V8 with graceful degrade; Necessity V8; E1/E2/E3 slots; Q1–Q5 map).
   Mark findings `[fact]` / `[inference]` / `[uncertain]`. Prefer reading
   `ci-report.md` (from `/sdd:pipeline-report`) for E1 when present. If
   artifacts missing or C1=`local-only` → Evidence=Skipped (explicit).
   Incomplete evidence must not yield Overall=Pass when evidence was in scope.
   Optionally add [human-review-assist.md](../../../vendor/ai4se-plugins/skills/verify/local-verify-report/references/human-review-assist.md)
   overlay for QA (never self-approve E3).
7. **Code-review highlights (agent)** for the current working-tree / change
   diff — include in the same report (not a separate skill).
8. **Aggregate** Pass|Fail|Partial per
   [report-template.md](../../../vendor/ai4se-plugins/skills/verify/local-verify-report/references/report-template.md). Any blocking local
   Fail or CRITICAL evidence gap → Overall Fail. **Do not soft-pass** to
   unblock apply agents.
9. **Always persist** the report:

   ```bash
   "$SKILL_DIR/scripts/write-report.sh" --root "<root>" --apply \
     --body-file "<path-to-agent-written-report.md>"
   ```

   Path: `docs/ai4se-plugins/<ts>-local-verify-report.md`. If write fails →
   emit the full report in the reply and mark persist failed.
10. **PR gate statement.** Overall=Fail without a **complete** `## Waiver`
    (item, VL|designated owner, remedy deadline, human sign-off) → instruct
    that `/sdd:pr-submit` **must stop**. Agent must not self-approve Waiver.
    Overall=Pass → **Next:** `/sdd:pr-submit`.

- **In:** root (default cwd); optional change-id; C1–C2 outcomes; whitelist
  verify commands from project sources
- **Out:** verify report + checkpoint log under `docs/ai4se-plugins/`;
  dimensions + Overall Pass|Fail|Partial; explicit PR-gate / Waiver state
- **Not:** opening PRs; approving waivers; remote pipeline fetch; G2 decision;
  mutating business code or Living Spec
- **Escalate:** VL (or designated) for Waiver sign-off; human when whitelist is
  empty and Pass is requested; stop if required checks cannot run but Pass is
  claimed

**Gotchas:** Incomplete Waiver ≡ no waiver. Apply and verify stay separated —
this skill is verify-side only. `docs/ai4se-plugins/` files are process records,
not Living Spec authority.

## Examples

```text
User: /sdd:local-verify-report
Agent: Uses cwd as root; brainstorms C1–C2; init-checkpoint-log + probe-verify;
       run-verify on whitelist only; evidence checklist if in scope; writes
       report via write-report --apply; on Pass suggests /sdd:pr-submit.
```

```text
User: /sdd:local-verify-report (build Fail, no waiver)
Agent: Overall Fail; persists report with empty/N/A Waiver; states hard-stop —
       do not call /sdd:pr-submit until VL-signed Waiver is complete.
```

```text
User: /sdd:local-verify-report (Fail + complete Waiver in report)
Agent: Persists report including Waiver rows; restates waiver constraints;
       human may continue to /sdd:pr-submit — agent does not self-approve.
```

## Additional resources

- [Brainstorming agenda](../../../vendor/ai4se-plugins/skills/verify/local-verify-report/references/brainstorming-agenda.md)
- [Checkpoint log template](../../../vendor/ai4se-plugins/skills/verify/local-verify-report/references/checkpoint-log-template.md)
- [Report template](../../../vendor/ai4se-plugins/skills/verify/local-verify-report/references/report-template.md)
- [Evidence checklist](../../../vendor/ai4se-plugins/skills/verify/local-verify-report/references/evidence-checklist.md)
- [Human review assist](../../../vendor/ai4se-plugins/skills/verify/local-verify-report/references/human-review-assist.md)
- [Probe targets](../../../vendor/ai4se-plugins/skills/verify/local-verify-report/references/probe-targets.md)
- [init-checkpoint-log.sh](../../../vendor/ai4se-plugins/skills/verify/local-verify-report/scripts/init-checkpoint-log.sh)
- [probe-verify.sh](../../../vendor/ai4se-plugins/skills/verify/local-verify-report/scripts/probe-verify.sh)
- [run-verify.sh](../../../vendor/ai4se-plugins/skills/verify/local-verify-report/scripts/run-verify.sh)
- [write-report.sh](../../../vendor/ai4se-plugins/skills/verify/local-verify-report/scripts/write-report.sh)
