# ATDD design outline (pack default)

Produce a short ATDD design document for `/sdd:task-plan` to consume. Plan
**API** and **E2E** (acceptance) layers only. Keep unit-test (UT) suite design
out of this document — UT belongs to `/sdd:atdd-apply`.

## Required sections

1. **ATDD layers in scope** — which **API** and **E2E** layers apply for this
   change. Use [layer-inference.md](../../../../vendor/ai4se-plugins/skills/design/atdd-design/references/layer-inference.md) to classify scenarios.
   Frame strictly as ATDD (failing acceptance / API checks guide
   implementation). Do **not** specify unit-test suites here; list any
   pure-logic items as **Deferred to `/sdd:atdd-apply` (unit-level TDD)**.
2. **What is written before production code** — acceptance / API examples or
   checks authored (or stubbed red) before feature code. Prefer scenarios
   classified as API or E2E.
3. **Local vs CI gate expectations** — align with Project Rules; do not invent a
   second CI semantics.
4. **Data / fixtures strategy** — shared fixtures, test data ownership, cleanup
   for API and E2E layers in scope.
5. **Non-goals** — no business feature coding in this skill; no detailed UT
   suite plan; no Playwright/POM/Excel generators; no new CI product.

## Optional section (ask first)

6. **NFR tests (optional)** — observability, performance, resilience, etc.
   Include **only after** asking the user whether NFR testing is in scope for
   this change. If declined or deferred, record that explicitly.

Keep the whole doc short enough for task planning.
