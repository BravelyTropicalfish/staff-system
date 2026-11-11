---
name: sdd-atdd-design
description: >-
  Design the ATDD approach before coding (API + E2E acceptance layers, gates,
  fixtures). Use in P3. Do not write production feature code, unit-test suites,
  or full tasks.md here.
disable-model-invocation: true
status: ready
id: skill.design.atdd-design
command: /sdd:atdd-design
impl: native
layer: core
gate: false
---

# ATDD design

## Instructions

Before coding, design the ATDD approach for the change (P3). **Anti-trigger:**
Does not write business implementation or tests-as-code yet (`/sdd:atdd-apply`);
does not produce full `tasks.md` (`/sdd:task-plan`); does not design unit-test
suites (unit-level TDD is apply-time).

1. Confirm accepted Delta Spec / change-id context and Project Rules
   test/pipeline expectations when present. If the Spec is too ambiguous for a
   testable ATDD design → stop; send back to Spec lane / humans.
2. Classify scenarios with
   [layer-inference.md](../../../vendor/ai4se-plugins/skills/design/atdd-design/references/layer-inference.md) into **API** vs
   **E2E** (DB-absence → E2E); defer pure-logic / UT items to apply; attach
   advisory recommended layers; add reverse cases for Non-goals.
3. Produce an ATDD design doc following
   [atdd-design-outline.md](../../../vendor/ai4se-plugins/skills/design/atdd-design/references/atdd-design-outline.md):
   API + E2E layers only, what is written before production code, local vs CI
   gates aligned to Rules, fixtures strategy, explicit non-goals (including no
   Playwright/Excel generators) **and** Non-goal reverse verification rows.
4. **Ask** whether NFR testing (observability, performance, etc.) is in scope.
   Only if the user confirms, add an NFR test section; otherwise record deferred
   / out of scope.
5. Keep the doc short for `/sdd:task-plan` consumption. **Next:**
   `/sdd:task-plan`.

- **In:** accepted Delta Spec / change-id; Project Rules test/pipeline hints
- **Out:** ATDD design doc per outline (API + E2E; optional NFR section only
  after ask; UT deferred list only)
- **Not:** production feature code; full task decomposition; UT suite design;
  Playwright/POM/Excel generators; CI product reinvention
- **Escalate:** SA/humans when Spec ambiguity blocks testable ATDD design

**Gotchas:** ATDD design is a P3 prerequisite — do not defer acceptance thinking
to apply time. Apply owns red-green TDD including unit tests.

## Examples

```text
User: /sdd:atdd-design
Agent: Classifies scenarios (API vs E2E; UT deferred); writes ATDD design
(gates, fixtures); asks about NFR tests; omits UT suite plan; suggests
/sdd:task-plan.
```

## Additional resources

- [ATDD design outline](../../../vendor/ai4se-plugins/skills/design/atdd-design/references/atdd-design-outline.md)
- [Layer inference (UT/API/E2E + recommended)](../../../vendor/ai4se-plugins/skills/design/atdd-design/references/layer-inference.md)
