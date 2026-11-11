---
name: delta-craft
description: >-
  Craft or refine a Delta Spec with AC/DoV review via OpenSpec wrap. Use after
  an approved proposal; before G1. Do not write business code or approve gates.
disable-model-invocation: true
status: ready
id: skill.spec.delta-craft
command: /sdd:delta-craft
impl: wrap-openspec
layer: core
gate: false
---

# Delta craft

## Instructions

Turn an approved proposal/change into a Delta Spec with AC/DoV review.
**Anti-trigger:** Do not write business code; do not approve G1; do not invent a
second long-term specs tree beside OpenSpec.

1. Confirm `change-id` (or OpenSpec change path) and optional
   `step=generate|review|all` (default `all`). Confirm proposal / requirements
   artifacts for the change are available.
2. **Generate** (`step=generate` or `all`): wrap `/opsx:propose` or the continue
   family so Delta Specs land under the authoritative `openspec/changes/` tree
   (or project-configured OpenSpec paths). If OpenSpec / `/opsx:*` is missing →
   **stop**; see [openspec integration](../../../integrations/openspec.md). Do
   **not** hand-author a competing specs root. Brownfield with partial deltas →
   prefer continue/update over duplicate changes. If upstream generate fails →
   stop; do not invent a parallel tree.
3. **Review** (`step=review` or `all`): run
   [AC / DoV checklist](./references/dov-ac-checklist.md). Mark each item
   Pass/Fail/Partial with evidence paths; note IO/SA residual concerns. Prefer
   writing the review table under the change folder (project Rules path if set,
   else `dov-ac-review.md`); if the path is unclear or the user declines a file
   write → put the same table in the reply.
4. Blocking DoV gaps → mark Fail/Partial; **do not** claim G1-ready. This skill
   **never** writes the Gate decision.
5. Summarize Delta Spec paths + review outcome. **Next:**
   `/sdd:g1-decision-record` or continue `/sdd:p2`.

- **In:** change-id (or change path); optional `step`; proposal / requirements
  artifacts
- **Out:** Delta specs under OpenSpec tree; AC/DoV review table; optional
  `dov-ac-review.md` (or Rules-agreed path)
- **Not:** business implementation code; G1 Pass/Fail decision; a second
  long-term specs authority root
- **Escalate:** human when OpenSpec install/auth is blocked; IO/SA when DoV
  Fail/Partial needs clarify before G1

**Gotchas:** Do not copy OpenSpec trees into this plugin pack. Review prose is
not Living Spec authority.

## Examples

```text
User: /sdd:delta-craft
Agent: Confirms change-id (step=all); wraps /opsx:propose or continue into
openspec/changes/; runs AC/DoV checklist; writes dov-ac-review.md when possible;
suggests /sdd:g1-decision-record (never writes G1 itself).
```

```text
User: /sdd:delta-craft step=review
Agent: Assumes specs exist; runs checklist only; records Pass/Fail/Partial with
evidence; stops short of G1-ready on blocking Fail.
```

## Additional resources

- [AC / DoV checklist](./references/dov-ac-checklist.md)
- [OpenSpec integration](../../../integrations/openspec.md)
