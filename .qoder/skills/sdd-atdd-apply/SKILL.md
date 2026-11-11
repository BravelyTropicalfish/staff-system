---
name: sdd-atdd-apply
description: >-
  Implement code and tests per ATDD design and tasks using TDD. Use in P4.
  Owns unit-level TDD (UT). Do not open PRs or write Gate decisions.
disable-model-invocation: true
status: ready
id: skill.apply.atdd-apply
command: /sdd:atdd-apply
impl: wrap-openspec
layer: core
gate: false
---

# ATDD apply

## Instructions

Implement the change by generating code and tests according to ATDD design and
tasks (P4). **Anti-trigger:** Do not redesign ATDD (`/sdd:atdd-design`), open a
PR, or write G2 decisions.

1. Confirm `change-id`, ATDD design, and `tasks.md` (or equivalent design
   artifacts). If ATDD design is missing → stop; run `/sdd:atdd-design` first.
   If the task list contradicts ATDD → stop; return to `/sdd:task-plan`.
2. Load Project Rules (test/branch constraints) and obey them throughout.
3. Work **one task slice at a time**. For each slice, follow Superpowers
   **`test-driven-development`** (red → green → refactor). Do not delete or
   weaken tests to go green.
4. **Convention — unit-level TDD (UT):** this skill owns unit tests during
   red-green-refactor for each slice. Acceptance / **API** / **E2E** intent
   comes from the ATDD design; implement or wire those checks as the design and
   Project Rules specify. Do **not** invent Playwright/POM pipelines or Excel
   testcase generators here.
5. When OpenSpec change context is needed, wrap `/opsx:apply`; do not use it to
   bulk-generate implementation that skips TDD. If OpenSpec apply is unavailable
   → stop; see [openspec integration](../../../vendor/ai4se-plugins/integrations/openspec.md).
6. Summarize files touched and remaining tasks. **Next:**
   `/sdd:local-verify-report`. Do not proceed to PR or G2 from this skill.

- **In:** change-id; ATDD design; tasks/design artifacts; Project Rules
- **Out:** code + tests in the repo (including UT per slice); touched-files
  list; remaining tasks
- **Not:** PR creation (`/sdd:pr-submit`); local verify report; G2 decisions;
  redesigning ATDD layers; Playwright/Excel generators; skipping failing ATDD
  intent to “just ship code”
- **Escalate:** human owner if Rules require destructive moves, or ATDD/tasks
  conflict cannot be resolved in-skill

**Gotchas:** Apply agent must not also write G2 decisions. Whole-repo codegen
that bypasses red-green-refactor is out of contract.

## Examples

```text
User: /sdd:atdd-apply
Agent: Confirms ATDD design + tasks for change-id; implements the next slice via
Superpowers test-driven-development (UT here; API/E2E per design); wraps
/opsx:apply only when OpenSpec context is needed; lists touched files and
remaining tasks; suggests /sdd:local-verify-report.
```

## Additional resources

- [OpenSpec integration](../../../vendor/ai4se-plugins/integrations/openspec.md)
