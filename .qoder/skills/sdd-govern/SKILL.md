---
name: sdd-govern
description: >-
  Establish or refresh constitution-level Rules under docs/knowledge/rules/
  (MECE authority). Dual mode init|check-merge; host projections are derived
  copies. Do not embed full CI product YAML or Gate pass/fail here.
disable-model-invocation: true
status: ready
id: skill.rules.govern
command: /sdd:rules-govern
impl: native
layer: core
gate: false
---

# Rules govern

## Instructions

Establish or refresh the project's Rules authority under
`docs/knowledge/rules/` (MECE categories). Host rule files are **projections
only** — authority wins on conflict. `AGENTS.md` carries **pointers**
(`@rule:<id>` / path pointers), not full rule bodies.

**Anti-trigger:** Do not author full CI/CD product YAML (`/sdd:pipeline-iac`);
do not replace Gate pass/fail; do not invent org policy when guessing is unsafe.

Resolve `$SKILL_DIR` as the directory containing this `SKILL.md`. Invoke pack
scripts via `"$SKILL_DIR/scripts/..."` — do not run bare `scripts/...` from the
target repo cwd.

### Authority tree (review)

Show this tree before apply. Full notes:
[authority-tree.md](../../../vendor/ai4se-plugins/skills/rules/govern/references/authority-tree.md). Rulepack source:
[rulepack/](../../../vendor/ai4se-plugins/skills/rules/govern/references/rulepack).

```text
docs/knowledge/rules/
├── _SCHEMA.md / _INDEX.md / _LOG.md / README.md
├── process/           # spec-first (Critical)
├── architecture/      # layer-boundaries, no-circular, orchestration-exec
├── quality/           # coding-style (Critical); stack rules via --langs
├── testing/           # README on common init
├── security/          # baseline, secrets, prompt-injection, pii (Critical)
├── documentation/     # README on common init
└── modules/<id>/      # single-submodule overlay only when needed
```

**Common Critical 9** (no `async-only` in common). Optional stack rules via
`--langs` — **allowlist only:** `cpp` `golang` `java` `kotlin` `python`
`react` `react-native` `rust` `swift` `typescript` `vue` `web`. Example:
`--langs python` rewrites `async-only` into `quality/`.

1. **Resolve execution `root`.** Prefer the root already confirmed earlier in
   this session (e.g. after `/sdd:repo-understand` or `/sdd:bootstrap`) — mark
   C1 `skipped-preanswered`, do not re-ask. If standalone with no prior root,
   ask once (may suggest cwd). Stop if root is unclear. Convention:
   [execution-root.md](../../../vendor/ai4se-plugins/references/execution-root.md).
2. **Inspect then suggest mode (C2).** Before asking, check
   `<root>/docs/knowledge/rules/`:
   - missing / empty → suggest **`init`**
   - `_SCHEMA.md` and/or existing category rule files → suggest **`check-merge`**
   - dirs only, no Critical rules yet → prefer **`init`**, note partial layout

   Tell the user the suggestion + one-line rationale, then confirm (one question).
3. **Brainstorm** remaining open choices one-at-a-time (Superpowers brainstorming)
   using [brainstorming-agenda.md](../../../vendor/ai4se-plugins/skills/rules/govern/references/brainstorming-agenda.md)
   (C1–C5). Apply is not a checkpoint.
4. **Init checkpoint log** (process record only — not Living Spec / Rules SoT):

   ```bash
   "$SKILL_DIR/scripts/init-checkpoint-log.sh" --root "<root>"
   ```

   Fill Checkpoints C1–C5 (or explicit defer / `skipped-preanswered`) before
   dry-run.
5. **Dry-run plan** (no writes):

   ```bash
   "$SKILL_DIR/scripts/plan-rules.sh" --root "<root>" \
     --mode init|check-merge [--langs python,...]
   ```

   Show CREATE/SKIP/CONFLICT **and** the review tree. Update the checkpoint log
   Apply section.
6. **After dry-run:** ask once whether to apply now. Keep the ask short — do not
   say “To materialize”. If the user declines, stop (log mode=`dry-run`). If they
   confirm (or already gave explicit `--apply` intent):

   ```bash
   "$SKILL_DIR/scripts/apply-rules.sh" --root "<root>" --apply \
     --mode init|check-merge [--langs python,...]
   ```

   Additive for non-empty authority files (never overwrite). check-merge
   surfaces CONFLICT in plan; preserve project overrides on apply.
7. **Host projection** when C4 ≠ `none` — **always refresh** after a successful
   apply (including brownfield no-op merges):

   ```bash
   "$SKILL_DIR/scripts/project-host.sh" --root "<root>" \
     --host claude-code|cursor|codex|github-copilot|qoder|none [--apply]
   ```

   Destinations: `.claude/rules/ai4se/`, `.cursor/rules/ai4se/`,
   `.codex/rules/ai4se/`, `.qoder/rules/ai4se/`, `.github/ai4se-rules/` (+
   Copilot pointer). Never flatten category folders.
8. Summarize created / skipped / conflicts / projected. **Optional next:**
   `/sdd:quality-checklist` (P0 DoV). User may stop or reorder.

- **In:** execution root; mode; langs; host; brainstorming outcomes
- **Out:** authority tree under `docs/knowledge/rules/`; AGENTS pointers;
  optional host projection; checkpoint log under
  `docs/ai4se-plugins/<timestamp>-rules-govern-checkpoints.md`
- **Not:** full CI product YAML; Gate verdicts; second SoT in host files;
  `langs/` or `stacks/` directories under authority
- **Escalate:** human when org policy cannot be safely inferred; stop if
  required agenda answers refused without safe default

**Gotchas:** Prefer host hooks/permissions for hard denies when the host
supports them. Projection ≠ SoT — edit authority, then re-project. Brownfield
may already have partial trees — prefer additive alignment + CONFLICT surfacing.

## Examples

```text
User: /sdd:rules-govern
Agent: Reuses session root if already confirmed (else asks once); inspects
       docs/knowledge/rules/ → suggests init or check-merge; brainstorms
       C3–C5; init-checkpoint-log + plan-rules; shows CREATE/SKIP; asks
       whether to apply; on confirm apply-rules --apply + project-host
       --apply; offers optional quality-checklist.
```

```text
User: /sdd:rules-govern (brownfield, host=cursor)
Agent: Suggests check-merge after inspection; plan shows CONFLICT/SKIP;
       after apply always refreshes .cursor/rules/ai4se/.
```

## Additional resources

- [Execution root convention](../../../vendor/ai4se-plugins/references/execution-root.md)
- [Authority tree](../../../vendor/ai4se-plugins/skills/rules/govern/references/authority-tree.md)
- [Rulepack](../../../vendor/ai4se-plugins/skills/rules/govern/references/rulepack)
- [Brainstorming agenda](../../../vendor/ai4se-plugins/skills/rules/govern/references/brainstorming-agenda.md)
- [Checkpoint log template](../../../vendor/ai4se-plugins/skills/rules/govern/references/checkpoint-log-template.md)
- [init-checkpoint-log.sh](../../../vendor/ai4se-plugins/skills/rules/govern/scripts/init-checkpoint-log.sh)
- [plan-rules.sh](../../../vendor/ai4se-plugins/skills/rules/govern/scripts/plan-rules.sh)
- [apply-rules.sh](../../../vendor/ai4se-plugins/skills/rules/govern/scripts/apply-rules.sh)
- [project-host.sh](../../../vendor/ai4se-plugins/skills/rules/govern/scripts/project-host.sh)
- [lib-hosts.sh](../../../vendor/ai4se-plugins/skills/rules/govern/scripts/lib-hosts.sh)
