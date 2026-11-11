# Scenario → layer inference (UT / API / E2E + recommended layers)

Classify acceptance scenarios for **ATDD design**. Plan **API** and **E2E**
layers in this skill. Pure-logic / single-unit checks are labeled **Defer UT**
(or noted as UT for apply-time TDD) — do **not** expand a full UT suite plan
here; hand that to `/sdd:atdd-apply`.

## Decision order (first match wins)

| Priority | Signal | Layer | Notes |
|---|---|---|---|
| 1 | WHEN/THEN is HTTP method + status/body (or single endpoint contract) without
  requiring full UI or multi-system orchestration | **API** | Isolate to one
  service boundary when possible |
| 2 | THEN asserts UI / page state / user-visible behavior; or WHEN is user
  navigation / form / click across screens | **E2E** | Acceptance through the
  user-perceivable path |
| 3 | Multi-system or full workflow state (intake → processing → visible outcome /
  durable store) needed to observe the result — **including “data must not
  appear”** when only a full path can prove absence | **E2E** | DB-state THEN
  (written or not written) beats “single API” alone |
| 4 | Pure function / in-memory transformation / single-module behavior with no I/O
  or UI | **Defer UT** | List as “apply-time unit TDD”; do not expand a UT suite
  plan in this doc |
| 5 | Ambiguous | Prefer **API** if a single endpoint can prove it; else **E2E**;
  never invent a UT plan to resolve ambiguity | Record rationale |

### Compact decision tree

```
UI interaction across screens / pages?
  ├─ yes, multi-surface → E2E
  └─ no → THEN needs durable store / “no row written” proof?
          ├─ yes → E2E
          └─ no → single API endpoint isolatable?
                  ├─ yes → API
                  └─ no → multi-system → E2E; else Defer UT
```

## Layer definitions (short)

| Layer | Definition | Typical |
|---|---|---|
| **UT** (defer) | Single function/class/module; mock externals | transforms, validators, state machines |
| **API** | One endpoint request/response; may use real DB; no UI | input validation, status codes, authz on route |
| **E2E** | Cross-system business path with real dependencies | intake → process → UI/notification visible |

Same business theme may spawn API + E2E rows; annotate Defer UT for core logic.

## Recommended extra layers (advisory inference)

Scan requirement / design keywords; attach **recommended** layers (not gates):

| Dimension | Trigger signals | Recommend |
|---|---|---|
| Protocol | SSE, WebSocket, GraphQL, streaming | Integration (+ E2E) |
| Performance | SLA, QPS, latency, load | Stress (advisory) |
| Security | authn/z, JWT, PII, encryption | Security (advisory) |
| AI / LLM | intent classify, model quality, accuracy | LLM eval (advisory) |
| Compatibility | multi-browser / device / a11y | Compatibility (advisory) |
| Consistency | payment, idempotency, rollback | Integration + E2E |
| Third-party | webhook, queue, external API | Integration |

Mark “待确认” until tech design review; QA may adjust.

## Non-goal reverse verification

For each Proposal / Ready **Non-goal** / Out-of-scope item, add at least one
**reverse** case: assert the system does **not** do that behavior (prefer E2E
or API depending on observability). Do not skip Non-goals when enumerating
scenarios.

## Output shape (short)

For each scenario / AC theme:

| # | Scenario | Layer | Recommended extras | Rationale |
|---|---|---|---|---|
| … | … | API / E2E / Defer-UT | e.g. SSE integration | one-line signal used |

## Explicit non-goals

- No Playwright / POM / Excel / bulk testcase generators in this skill.
- No ratio policing as a Gate; optional note if E2E seems overloaded vs API —
  suggest moving isolatable checks to API, not to a designed UT suite.
- Project Rules may name local frameworks; follow them without inventing a new
  test product.
