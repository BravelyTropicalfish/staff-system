---
name: sdd-consistency-lint
description: >-
  Lint Living/Delta specs for consistency and drift: Proposal/Spec/Tasks/Design
  rule tables, cross-artifact checks, Authority [fact]/[inference], residual
  risk. Use as optional P2/P5 quality check with phase=p2|p5. Do not auto-merge
  or approve gates.
disable-model-invocation: true
status: ready
id: skill.spec.consistency-lint
command: /sdd:consistency-lint
impl: native
layer: optional
gate: false
---

# Consistency lint

## Instructions

Check Living Spec / Delta Spec / related artifacts for consistency and drift
(optional P2/P5).
**Anti-trigger:** Not a Gate decision; not a full rewrite of specs; not
`/sdd:living-merge`; not a substitute for `/sdd:delta-craft` AC/DoV review.

1. Confirm `change-id` (or OpenSpec change path). Resolve `phase=p2|p5`:
   - If the user already set `phase` → use it.
   - If missing → **do not guess silently**. Recommend `p2` when the focus is an
     open (non-archived) change / Delta before clear G1; recommend `p5` when the
     context is post-`/sdd:living-merge` / archive / Living update. Ask the user
     to choose `p2` or `p5`. No choice → **stop**.
   - Optional `focus=` path/glob narrows scope; otherwise use phase defaults.
2. Read-only discover roots: change/Delta under authoritative
   `openspec/changes/<id>/` (or project-configured OpenSpec paths); Living Spec
   root per integrations / project Rules. Required roots unreadable → **stop**.
   Missing optional artifacts (e.g. no `tasks.md`) → mark related checks N/A;
   do not fail the whole run solely for that. If OpenSpec/tooling is absent →
   run a manual checklist pass using
   [lint checklist](../../../vendor/ai4se-plugins/skills/spec/consistency-lint/references/lint-checklist.md); overall verdict **capped at
   Partial**.
3. Run the Shared + matching phase sections of the lint checklist (including
   Proposal/Spec/Tasks/Design rule tables and C1/C2/C6 when artifacts exist).
   Prefer structured findings over prose essays. Each finding: id, severity
   (BLOCKER|WARNING|SUGGESTION), phase, location, issue, suggested_fix,
   evidence, authority, `[fact]`|`[inference]`. Drift suspects need human
   confirmation before any mass edit — this skill **never** silently rewrites
   authoritative specs.
4. Map overall verdict: ≥1 BLOCKER → Fail; else ≥1 WARNING → Partial; else
   Pass (manual/tooling-absent runs still capped at Partial). Persist the
   report: project Rules path if set; else
   `openspec/changes/<id>/consistency-lint.md`; if path unclear or the user
   declines a file write → put the full report in the reply. Include a
   **Residual risks** section (explicit empty OK). Never overwrite
   proposal/spec bodies.
5. Summarize verdict + counts. **Next (non-Gate):**
   - `phase=p2` → `/sdd:g1-decision-record` or continue `/sdd:p2` (remind humans
     to address BLOCKERs before treating G1 as ready).
   - `phase=p5` → `/sdd:change-summary` / `/sdd:g3-decision-record`.
   This skill **never** writes Gate Pass/Fail.

- **In:** change-id (or change path); `phase=p2|p5` (or user choice after
  recommendation); optional `focus=`
- **Out:** lint report (file or reply) with findings + Pass|Fail|Partial
- **Not:** G1/G3 decisions; auto-merge; silent rewrites of authoritative specs;
  batch lint of all changes in one invocation; treating the report as Living
  Spec authority
- **Escalate:** human when roots unreadable, phase unchosen, or BLOCKERs need
  product/SA clarification before Gate

**Gotchas:** Fail/Partial here is not a Gate result. Suggested fixes are
guidance only until a human asks to edit.

## Examples

```text
User: /sdd:consistency-lint change-id=add-foo phase=p2
Agent: Discovers change + Living roots; runs Shared+p2 checklist; writes
consistency-lint.md (or reply); reports verdict; suggests g1 or continue p2.
```

```text
User: /sdd:consistency-lint change-id=add-foo
Agent: Recommends phase=p2 or p5 from context; waits for user choice; then runs.
```

```text
User: /sdd:consistency-lint change-id=add-foo phase=p5
Agent: Runs Shared+p5 checks; notes drift suspects as WARNING/BLOCKER with
evidence; does not edit Living Spec; suggests change-summary / g3.
```

## Additional resources

- [Consistency lint checklist](../../../vendor/ai4se-plugins/skills/spec/consistency-lint/references/lint-checklist.md)
- [OpenSpec integration](../../../vendor/ai4se-plugins/integrations/openspec.md)
