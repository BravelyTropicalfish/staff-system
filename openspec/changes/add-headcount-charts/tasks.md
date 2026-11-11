# Tasks — add-headcount-charts

- **Change id:** `add-headcount-charts`
- **Inputs:** `design.md` (packages, route, component tree) and `atdd-design.md` (8 API / 4 E2E,
  unit items deferred) over the delta spec (6 requirements / 12 scenarios, G1 Pass on rev 2)
- **Order:** `atdd-design.md` §2 — fixtures → API checks → the section → E2E → NFR → the full verify

## Conventions

- One task is one reviewable change: it starts with the failing check, then the code, and ends
  green.
- Every task is a `- [ ]` item: `[x]` means its evidence is green today.
- A scenario in *italics* is the **primary** check for that task; a trailing `(payload)` marks an
  API companion of a scenario whose primary layer is E2E, so the split stays 8 API + 4 E2E and no
  scenario is counted twice.
- No task adds scope: a check without a scenario is a spec problem, not a test problem
  (`atdd-design.md` §5).
- No task designs a unit-test suite; unit tests are written with their code.

## Stage 1 — the report route

- [x] **T01** — `GET /api/headcount` answers the active pool: `total`, `byRole`, `byGrade`; only
  `active = true` people are counted and an inactive person moves nothing
  - Layer: `API` · Blocked by: —
  - Evidence: `HeadcountApiTest` — *Both charts describe one pool*, *An inactive person is in
    neither chart*; the total equals both axis sums in the same answer

- [x] **T02** — The axis is every active catalog role ∪ every role still holding an active person,
  plus the four bands, both in model order; a zero count keeps its bar and a deactivated role with
  nobody leaves the axis
  - Layer: `API` · Blocked by: T01
  - Evidence: `HeadcountApiTest` — *A category with nobody keeps its bar*, *Role bars follow catalog
    order*, *Grade bars follow band order*, *A deactivated role keeps its active people counted*

- [x] **T03** — Each bar carries its count and its share of the total, one decimal, half-up; the
  residue is left alone and the counts are never adjusted
  - Layer: `API` · Blocked by: T01
  - Evidence: `HeadcountApiTest` — *A bar carries its count and its share* (`40` / `35.4`), *A
    share rounds to one decimal* (`33.3`, `66.7`, `12.5`); `HeadcountShareTest` pins the half-up
    branch and the `100.0` case

- [x] **T04** — A lead answers 403 with no numbers, through the same `Auth.coordinator` guard the
  pool read uses
  - Layer: `API` · Blocked by: T01
  - Evidence: `HeadcountApiTest` — *A lead sees no report* (payload): 403, the shared
    `{code,message}` body, and an unchanged row count

- [x] **T05** — An empty pool answers `total: 0` with both groupings present at `count: 0` /
  `share: 0.0` and no division
  - Layer: `API` · Blocked by: T01
  - Evidence: `HeadcountApiTest` — *An empty pool says so* (payload): the shape is unchanged and
    every count is zero

## Stage 2 — the section

- [x] **T06** — `HeadcountSection` + `ChartCard` render above the pool's two columns for the
  coordinator only; the screen reads `GET /api/session` **before** requesting the report, and the
  bars are proportional with their count and share in text
  - Layer: `E2E` · Blocked by: T01–T05
  - Evidence: `headcount.spec.ts` — *Both charts sit on the pool screen*, *A lead sees no report*
    (no section, and no `/api/headcount` request in the network log), *The table and the charts
    differ on purpose*; `npm test` gains `tests/headcount.test.ts` for the readings (`shareText`,
    `chartTicks`, `barHeight`, `isEmpty`, `sectionTotal`)

- [x] **T07** — The empty pool replaces both plots with the stated text instead of drawing zero bars
  - Layer: `E2E` · Blocked by: T06
  - Evidence: `headcount.spec.ts` — *An empty pool says so*: both chart areas read the empty
    sentence with `Total 0`, and no bar element exists

## Stage 3 — NFR and reverse checks

- [x] **T08** — Freshness: the report is derived per read, so an import applied between two reads
  changes the counts, the shares and the axis; journey J1 drives it through the screens
  - Layer: `API` + `E2E` · Blocked by: T01–T07
  - Evidence: `HeadcountApiTest` reads twice around an import and sees the change; `journeys.spec.ts`
    J1 imports, opens `/people`, and reads the changed charts beside the unchanged table row;
    the inactive person stays in the table and leaves the charts

- [x] **T09** — Non-goal reverse checks: no filter parameter changes the answer, no write verb is a
  route on `/api/headcount`, the answer carries no personal key, no export or scheduled delivery
  exists, and no chart library was added
  - Layer: `API` + config review · Blocked by: T01–T05
  - Evidence: `?project=`, `?role=` return the same body; `POST` / `PATCH` / `DELETE` answer 405 and
    change nothing; the answer's key set is asserted exactly (no `employeeNo`, `name`, `email`,
    `skill`, `allocation`); no `@Scheduled` method and no file-answering route; `web/package.json`
    is unchanged (dependency review)

## Stage 4 — the full verify

- [x] **T10** — Local verify: the three layers green in order, coverage at 100 %, and the new web
  test file joined the explicit `npm test` list without dropping the existing entries
  - Layer: `build` · Blocked by: T01–T09
  - Evidence: `cd backend && ./mvnw verify` green with JaCoCo 100 % line and branch on `domain*` /
    `api*`; `cd web && npm test` green including the new file; `npx playwright test` green;
    `npm run build` and `npm run lint` clean

## Traceability

| Scenario | Primary task | Layer |
|---|---|---|
| S1 Both charts describe one pool | T01 | API |
| S2 An inactive person is in neither chart | T01 | API |
| S3 A bar carries its count and its share | T03 | API |
| S4 A share rounds to one decimal | T03 | API |
| S5 A category with nobody keeps its bar | T02 | API |
| S6 Role bars follow catalog order | T02 | API |
| S7 Grade bars follow band order | T02 | API |
| S8 Both charts sit on the pool screen | T06 | E2E |
| S9 A lead sees no report | T04 (payload) + T06 | E2E primary |
| S10 An empty pool says so | T05 (payload) + T07 | E2E primary |
| S11 A deactivated role keeps its active people counted | T02 | API |
| S12 The table and the charts differ on purpose | T06 | E2E |

Totals: 12 scenarios = 8 API + 4 E2E, each counted once. The two E2E scenarios with a payload
companion (S9, S10) stay E2E-primary, so the split does not drift.

## Result (2026-11-11)

All ten tasks are `[x]`. Measured, not asserted:

| Layer | Command | Result |
|---|---|---|
| backend unit + API | `cd backend && ./mvnw verify` | exit 0 — **316 tests**, `All coverage checks have been met.` (100 % line and branch on `domain*` / `api*`) |
| web readings | `cd web && npm test` | **60 tests**, 0 failures (the new `tests/headcount.test.ts` is in the list) |
| E2E | `cd web && npx playwright test` | **24 passed**, 0 failed — 4 headcount checks plus the 20 that existed before |
| static | `npm run build`, `npm run lint` | build green (72 modules), lint clean |

`HeadcountApiTest` is 15 cases covering S1–S11 plus freshness and the three reverse checks;
`HeadcountShareTest` is 4 cases pinning the rounding branches. The API compiles to nothing stored:
the schema is untouched.
