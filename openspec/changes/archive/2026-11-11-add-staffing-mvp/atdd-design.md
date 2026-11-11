# ATDD design — add-staffing-mvp

- **Change id:** `add-staffing-mvp`
- **Gate context:** G1 **Pass** on rev 11, 2026-11-11 (`g1-decision.md`); this document is P3 input
  for `/sdd:task-plan`
- **Specs under test:** `specs/staffing/{allocation,candidate-matching,people-import,people-pool,
  post-requirement,staffing-confirmation}/spec.md` — 40 requirements / 90 scenarios
- **Rules applied:** `testing-strategy`, `pipeline-gates`, `architecture-baseline`
- **Scope of this document:** acceptance layers (API + E2E), the checks written before production
  code, gate alignment, fixtures. It designs no unit-test suite and writes no feature code.

## 1. ATDD layers in scope

Two acceptance layers, plus unit work deferred to apply.

| Layer | What it proves | Shape |
|---|---|---|
| **API** | validation, authorisation, state transitions, write-back, absence of rows | one HTTP request against a real relational database, no UI |
| **E2E** | user-visible behaviour across screens, and "not shown" only a full path can prove | browser, seeded accounts, 8 scenarios plus 2 journeys |
| **Defer UT** | pure logic listed at the end of this section | apply-time TDD via `/sdd:atdd-apply` |

The stack was still open when this document was drafted (proposal assumption 6, item D1); D1 is
now closed by ADR-001. This document still fixes contracts, checks and data — not frameworks:
`design.md` records the runtime stack and the E2E runner, and no test product is invented here.

### Layer assignment by capability

| Capability | Req | Scen | API | E2E | Deferred to apply (unit TDD) |
|---|---|---|---|---|---|
| `allocation` | 5 | 11 | 10 | 1 | inclusive intersection predicate; manual-vs-confirmation source rule |
| `candidate-matching` | 5 | 15 | 15 | 0 | gate order and first-failure selection; band rank; name collation; fallback tier predicate |
| `people-import` | 5 | 15 | 15 | 0 | cell normalisation; controlled-set membership; batch transaction boundary |
| `people-pool` | 8 | 17 | 14 | 3 | band rank reuse; one-role invariant |
| `post-requirement` | 8 | 14 | 14 | 0 | status derivation from headcount; the locked-field set |
| `staffing-confirmation` | 9 | 18 | 14 | 4 | voiding set (intersecting dates only); write-back steps |
| **Total** | **40** | **90** | **82** | **8** | — |

Every scenario has exactly one primary layer; the API column is the complement of the E2E list, so
no scenario is dropped. Seven of the eight E2E scenarios also carry a cheap API companion check (the
end-release payload, the read-only profile, the band name, the occupancy list, the confirmation
visibility, the search exclusion and the notice list); a companion adds no scenario row and never
becomes the primary layer.

### The 8 E2E scenarios

| Capability | Scenario | Why not API |
|---|---|---|
| `allocation` | The end releases everyone and cancels the posts | the proof spans project, posts, allocations and a later search |
| `people-pool` | The profile is read-only | asserts the absence of an affordance |
| `people-pool` | A band is displayed, never a number | asserts what the user reads |
| `people-pool` | Profile lists a person's occupancy | current **and** ended ranges on one screen |
| `staffing-confirmation` | Confirmation is visible to both roles | two accounts, two screens, one state |
| `staffing-confirmation` | The confirmed person leaves other searches | absence in another role's search |
| `staffing-confirmation` | The notice list explains each loss | asserts the reason text the coordinator sees |
| `staffing-confirmation` | Both roles see the same gap numbers | cross-role equality of a rendered view |

Two journeys stitch these without adding scenarios: (J1) import → project → post → search →
recommend → confirm → gap view, and (J2) project end → everyone released → the same people appear
in a later post's search.

### Deferred to `/sdd:atdd-apply` (unit-level TDD)

`[start, end]` intersection closed at both ends · band rank across the three scales · gate
evaluation order and first failing gate · name ordering with the employee-number tie-break · cell
normalisation and controlled-set membership · cross-role tier predicate (level + 1 and a domain
skill at `Practitioner` or above) · headcount-to-status derivation · all-or-nothing batch boundary ·
voiding set for intersecting dates · allocation source rule. These are advisory entry points for
apply-time red-green, not a suite plan.

## 2. What is written before production code

Fixed order, matching the proposal's delivery order; each stage's acceptance checks are red before
its feature code exists.

1. **Seed and fixtures** (section 4) — the catalogs, the two accounts and the builders.
2. **API checks per stage** — `people-pool` → `people-import` → `allocation` (projects, ranges,
   project end) → `post-requirement` → `candidate-matching` → `staffing-confirmation`. One route
   group and one service entry point per capability, so a check never reaches into internals
   (`architecture-baseline` rule 5).
3. **E2E checks** — the 8 scenarios and J1/J2, written once the screen for that stage exists; J1 is
   the last to go green.
4. **Unit tests** — written with their code at apply time, never designed here.

Every check is traceable to a scenario title in the delta specs; a check without a scenario means
the scenario set is wrong, not the test.

## 3. Local vs CI gate expectations

Aligned to `pipeline-gates` (no second CI semantics):

| Check | Where | Note |
|---|---|---|
| lint, type check | local stop check + CI | required |
| unit tests | local stop check + CI | written with code; 100% coverage (`testing-strategy`) |
| acceptance tests — API | local stop check + CI | the 82 API scenarios |
| acceptance tests — E2E | local before PR + CI | the 8 scenarios and 2 journeys; runner chosen in `design.md` |
| dependency and security scan | CI | required |

Order of stop checks: static → unit → API acceptance → E2E. A red stage stops the next one.

Two gaps to close inside this change, both owned by `/sdd:task-plan`:

- **No CI workflow exists yet.** The repository has `.github/` agent assets but no workflow, so
  this change adds one that runs the five required checks. It does not introduce a new CI product.
- **`docs/feedback/verification-commands.md` is still a placeholder.** `pipeline-gates` rule 7
  requires the real commands in the same change that introduces the checks.

## 4. Data and fixtures

- **Catalog seed (fixed):** five roles `PM, BA, DEV, QA, UX`; six skills 后端开发, 前端开发,
  项目管理, 业务分析, 测试, UI设计 with their `DEV/DEV/PM/BA/QA/UX` domains; the three band scales.
- **Accounts (fixed):** one coordinator and two leads (the second lead exists only to prove scope
  isolation), seeded with a role, never SSO.
- **People:** fake names only; employee numbers from the `E1xx` block, namespaced per test group so
  an import batch cannot collide across tests. No production data, ever (`testing-strategy` rule 5).
- **Workbooks are built in code from a table**, not committed as `.xlsx` binaries: valid, one bad
  row, missing sheet, empty sheet, duplicated employee number, duplicated skill row, orphan skill
  row, multi-role cell, inactive-person reactivation, skills-only refresh.
- **Dates are explicit literals** (`2027-01-01` …), so a boundary case never depends on the clock.
  Persona `E100` spans the fixture line: 2027-01-01 to 03-31, 02-01 to 05-31, 06-01 to 08-31.
- **Builders** for project, post, allocation, recommendation and notice, each taking the minimum
  arguments and defaulting the rest, so a scenario reads as its GIVEN.
- **Isolation and cleanup:** one fresh schema (or rolled-back transaction) per test; tests pass in
  any order and share no mutable state (`testing-strategy` rule 7). Import fixtures are deleted
  with the schema, so batch history never leaks between tests.

## 5. Non-goals

- No production feature code, no UI implementation, no schema migration in this document.
- No unit-test suite plan; unit work is deferred to `/sdd:atdd-apply`.
- No Playwright page object model, no Excel or bulk-testcase generator — the workbook fixtures are
  data builders, not a testcase generator.
- No new CI product, no deployment pipeline (`proposal` Non-Goals: local run and tests only).
- No scenario invented to make a test easier; a scenario change goes back to the spec lane.

## 6. Non-goal reverse verification

Each proposal Non-Goal gets at least one reverse case — the system must visibly **not** do it.

| # | Non-goal | Reverse check | Layer |
|---|---|---|---|
| 1 | No external integration or scheduled drops | no outbound job or scheduler is registered; a person's data changes only through a batch id | API |
| 2 | No hand-edited person master data | edit of a person's role, level, skill band or travel is refused; no route accepts it | API |
| 3 | No part-time staffing | an allocation carries no percentage; posting one changes nothing and is refused as an unknown field | API |
| 4 | No automatic assignment | a post whose only candidate passes every gate stays `open` until a human confirms | API + E2E (J1) |
| 5 | No notifications | no mail or chat transport exists; the loss notice is readable only in the app | API |
| 6 | No cost, rate, budget or utilisation | post and allocation payloads reject those fields; no route reads them | API |
| 7 | No interview or approval chain | a recommendation is confirmed by the lead in one step, with no intermediate state | API |
| 8 | No mobile, offline or multi-tenancy | a tenant parameter is not accepted anywhere; one company's data is the only scope | API |
| 9 | No destination-specific travel | travel accepts exactly `none < limited < full`; a destination field is refused | API |
| 10 | No production deployment pipeline | the added workflow runs tests only and holds no deploy job | CI config review |

Landing: rows 1–9 run in `NonGoalApiTest`, row 10 is a review of `.github/workflows/ci.yml`. Rows 3,
6 and 9 depend on a body field being *refused*, so the application turns on strict unknown-field
handling (`spring.jackson.deserialization.fail-on-unknown-properties=true`) and answers 400
`unreadable body`; without it the field would be dropped and the Non-Goal would quietly be
supported.

## 7. NFR tests (in scope: security and idempotency only)

Confirmed with the product owner in this session; the rest is recorded as deferred.

**Authorisation (API).** Every route enforces the role scope from the specs: a lead creates no
project and edits no post (403, no state change); a lead cannot open another lead's post or the
suitable list; the coordinator cannot confirm; only the project's lead ends the project; only the
coordinator recommends, withdraws and records allocations. Each check also asserts the refused call
left the row count unchanged.

**Idempotency (API).** Re-uploading an identical workbook creates a new batch and changes no person
or skill state (no duplicate rows, no renumbered bands); two concurrent confirmations of the same
person on the same dates leave exactly one assignment and one allocation, the loser refused with
its reason; releasing twice leaves the second call refused and the slot state unchanged.
`IdempotencyApiTest` carries the three checks, one test each, every one of them a row count.

**Deferred, with reasons:**

- *Performance* — the import runs synchronously at a few thousand rows (proposal assumption 9); a
  latency budget without a usage profile would be a made-up number.
- *Observability* — the batch error list is the audit trail in the MVP; structured logs and metrics
  belong to a later change.
- *Resilience, compatibility, accessibility, protocol, AI evaluation* — no signal in the change: no
  external service, no streaming, no model, one browser target.

## 8. Handover to `/sdd:task-plan`

- Task order = section 2 (seed → API per capability → E2E → unit alongside code).
- `/sdd:task-plan` writes `design.md` (stack, E2E runner, schema) and `tasks.md`, adds the CI
  workflow, and fills `docs/feedback/verification-commands.md`.
- Entry condition met: G1 Pass on rev 11. A later spec change voids these checks and needs a new
  G1 record.
