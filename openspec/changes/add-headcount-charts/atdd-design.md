# ATDD design — add-headcount-charts

- **Change id:** `add-headcount-charts`
- **Gate context:** G1 **Pass** on rev 2, 2026-11-11 (`g1-decision.md`); this document is the P3
  input for `/sdd:task-plan`
- **Spec under test:** `specs/staffing/headcount-report/spec.md` — 6 requirements / 12 scenarios
- **Rules applied:** `testing-strategy`, `pipeline-gates`, `architecture-baseline`
- **Visual reference:** `prototype/` — the three-state mock G1 reviewers accepted. Section 6 maps
  every element of it to what will be built and names the two parts that are review-only.
- **Scope of this document:** the acceptance layers (API + E2E), what is written before production
  code, gate alignment and fixtures. It designs no unit-test suite and writes no feature code.

## 1. ATDD layers in scope

| Layer | What it proves | Shape |
|---|---|---|
| **API** | membership, ordering, shares, the empty answer, the refusal | one HTTP request against a real H2 schema, no UI |
| **E2E** | what only a rendered screen can show: the section exists, bars are proportional and labelled, a lead sees no section, the empty text | browser, seeded accounts, 4 scenarios + 1 journey |
| **Defer UT** | pure logic listed below | apply-time TDD via `/sdd:atdd-apply` |

| Requirement | Req | Scen | API primary | E2E primary | Deferred to apply (unit TDD) |
|---|---|---|---|---|---|
| R1 describes the active pool only | 1 | 2 | 2 | 0 | the active-only predicate; the total = both sums invariant |
| R2 axis and its order | 1 | 4 | 4 | 0 | axis = active catalog roles ∪ roles holding an active person; band order from `Bands` |
| R3 count and share | 1 | 2 | 2 | 0 | half-up rounding to one decimal; the 100.0 and residue cases |
| R4 the section on the pool screen | 1 | 2 | 0 | 2 | bar height from the tallest bar; the zero stub |
| R5 coordinator alone | 1 | 1 | 0 | 1 | — (guard reuse) |
| R6 an empty pool is stated | 1 | 1 | 0 | 1 | the empty answer's shape |
| **Total** | **6** | **12** | **8** | **4** | — |

Every scenario has exactly one primary layer, so no scenario is dropped and none is counted
twice. Two of the four E2E scenarios carry a cheap **API companion** — the direct 403 for S9 and
the zero-total answer for S10 — which adds no scenario row.

### The 4 E2E scenarios

| Scenario | Why not API |
|---|---|
| S8 Both charts sit on the pool screen | "no other screen is reachable for the report" is a statement about the app's navigation, not about a payload |
| S9 A lead sees no report | the claim is the **absence** of a section plus the absence of a request; only a browser proves both |
| S10 An empty pool says so | the claim is the rendered text replacing both charts |
| S12 The table and the charts differ on purpose | the two numbers must be read on one screen at the same time |

Journey **J1** stitches them without adding a scenario: import a workbook that moves one person
into `PM` and deactivates another → open `/people` → both charts change and the table keeps the
inactive row. J1 doubles as the freshness check in §7.

### Deferred to `/sdd:atdd-apply` (unit-level TDD)

Half-up rounding at an exact half (`1/8` → `12.5`) · the axis predicate (active role with nobody is
on the axis; deactivated role with nobody is not) · band order and role order come from the stored
order, not from size · bar height with a zero tallest bar (guarded by the empty state) · the share
of a zero count is `0.0` and never a division. These are entry points for apply-time red-green,
not a suite plan.

## 2. What is written before production code

1. **Fixtures** (§4): the accounts and the imported pools, built through the import route.
2. **API checks** for the report route, in this order — membership and the total invariant →
   ordering → shares → empty answer → the 403. They are red before `HeadcountController` exists.
3. **E2E checks** for S8, S9, S10, S12 and J1, written once the section exists.
4. **Unit tests** written with their code at apply time, never designed here.

Every check names its scenario title; a check without a scenario means the spec set is wrong, not
the test.

## 3. Local vs CI gate expectations

Aligned to `pipeline-gates`; no second CI semantics, no new job. The five-check workflow already
exists (`.github/workflows/ci.yml`), so this change only adds tests to it:

| Check | Where | Note |
|---|---|---|
| lint, type check | local + CI | the new `web/tests/headcount.test.ts` must join `package.json`'s explicit `npm test` list, and `tsc --noEmit` must see it |
| unit tests | local + CI | written with the code; JaCoCo keeps 100 % line and branch on `domain*` / `api*` |
| acceptance — API | local + CI | the 8 API scenarios |
| acceptance — E2E | local + CI | the 4 scenarios and J1 |
| dependency and security scan | CI | unchanged; this change adds no dependency |

Order of stop checks: static → unit → API acceptance → E2E; a red stage stops the next one.

## 4. Data and fixtures

- **Accounts (fixed):** the seeded `coordinator`, `lead.one`, `lead.two`; the second lead exists to
  prove that the 403 is about the role and not about ownership.
- **The axis needs a role with nobody.** The seeded catalog offers `PM, BA, DEV, QA, UX`; a test
  that wants a zero bar imports nobody into one role and asserts that role is still on the axis.
- **Pools are built from the import route**, never by direct SQL, so a fixture cannot construct what
  the server would refuse. Numeric shapes worth fixing in fixtures: 113 = 40 + 25 + 14 + 34 + 0 for
  role order, and 3 / 8-person pools for the rounding cases (`1/3` → `33.3`, `2/3` → `66.7`,
  `1/8` → `12.5`).
- **Employee numbers** come from a per-test namespace (`H1xx`) so two classes cannot collide; one
  fresh schema per test class (the existing `${random.uuid}` datasource URL), so a report never
  reads another test's rows.
- **No dates, no clock, no randomness** in this change: every assertion is a count, an order, a
  share or a status code.

## 5. Non-goals

- No production code, no UI implementation, no schema change in this document.
- No unit-test suite plan; unit work is deferred to `/sdd:atdd-apply`.
- No charting library, no Playwright page object model, no Excel generator.
- No new CI product, no deployment pipeline (proposal Non-Goals).
- No scenario invented to make a test easier; a change to a scenario goes back to the spec lane.

## 6. Prototype alignment

The prototype is the agreed picture; this table is what keeps the build honest against it.

| Prototype element | Built as | Note |
|---|---|---|
| Yellow "评审状态" bar with three buttons | **not built** | review-only device; the product has one state per signed-in role |
| Section above the pool's two columns | `HeadcountSection` inserted above `.grid-2` in `PeopleScreen` | order matters: section, then table + profile |
| Two cards with `Headcount by Role` / `Headcount by Grade` and `Total <n>` | `ChartCard` with a title and a total line | the total is the **active** pool, and the label says so |
| Y-axis ticks derived from the tallest bar | `ticks(top)` in `lib/headcount.ts` | a nice step (10 or 2), top rounded up |
| Bar proportional to its count, value printed above | inline height on `.bar`, value in a sibling element | height comes from the server's count, never from a client-side recount |
| Zero bar as a thin grey stub, label `0` / `0.0%` | `.bar.is-zero` (3px) + dimmed label | a 0px element would be invisible; the stub is the "this category exists" evidence |
| Labels in catalog / band order | server order, rendered as received | the client never sorts |
| Dashed empty block reading "无启用人员" | `chart-empty` in both cards | replaces the plot, as the spec requires |
| "图上元素与规格条款" legend card | **not built** | review aid only |
| Lead state: no section, 403 banner | section not rendered, and the report is **not fetched** | the role is read first — see §7 |

Two prototype behaviours that must **not** leak into the product: the state switcher and the legend
card. Two that must: the zero stub and the "role known before fetch" ordering.

## 7. NFR tests (in scope: authorisation + freshness)

Confirmed with the product owner in this session.

**Authorisation (API + E2E).** The report answers the coordinator and refuses a lead with 403 while
changing nothing; the pool screen renders no chart section for a lead and issues no report request.
The second half is the reason the design reads `GET /api/session` **before** the report: rendering
first and hiding on a 403 would still put the request on the wire and fail S9. This mirrors how
`CatalogScreen` and `PostScreen` already obtain their role.

**Freshness (API).** The report is derived on every read and owns no cache. One test reads it, then
applies an import that moves a person into another role, then reads again: the counts, the shares
and the axis change on the second read, and no row for a report exists to go stale. The same test
asserts the inactive person leaves the charts while the table keeps showing them.

**Deferred, with reasons:** performance (the pool is a few thousand rows and the report is one
grouped read — a latency budget without a usage profile would be a made-up number); observability
(there is no batch to audit and no scheduled job; the report has nothing to log that the request log
does not already carry); resilience, compatibility, accessibility and protocol (one browser target,
no external service, no streaming). Accessibility is called out because the bars are visual: each
bar carries its number as text, so the information is never only a rectangle — that is a spec-level
property (R3), not an ARIA audit.

## 8. Handover to `/sdd:task-plan`

- Task order = section 2: fixtures → API checks → the section → E2E → NFR → the full verify.
- `/sdd:task-plan` writes `design.md` (packages, route shape, component tree, no schema change) and
  `tasks.md`, and adds the new web test file to the existing `npm test` list.
- Entry condition met: G1 Pass on rev 2. A later spec change voids these checks and needs a new G1
  record.
