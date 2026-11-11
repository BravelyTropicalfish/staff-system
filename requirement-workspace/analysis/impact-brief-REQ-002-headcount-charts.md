# Impact brief — REQ-002 Role and grade headcount charts

- **Requirement set:** `requirement-workspace/requirements/REQ-002-headcount-charts.md` (rev 1)
- **Change:** `openspec/changes/add-headcount-charts/`
- **Date:** 2026-11-11
- **Verdict:** additive read-only capability over an existing model; no schema change, no
  writer, no contract change to an existing route. No critical blocker for one change.

## 1. As-is asset inventory (inspected)

| Asset | Path | State |
|---|---|---|
| Living specs | `openspec/specs/` | still empty (`.gitkeep`); the six `add-staffing-mvp` capabilities are live in code but not yet merged into `openspec/specs/`, because that change is at P5-pending |
| Change packages | `openspec/changes/add-staffing-mvp/` | complete through G2 (implementation, verify, PR #1, CI green); `openspec/changes/archive/` empty |
| Backend | `backend/src/main/java/com/example/staffing/` | 15 API classes, 11 `data/` classes, 65 `domain/` classes; `Service` + `*View` record + thin controller per slice; 280 tests at 100 % line and branch coverage on `domain*` and `api*` |
| Persistence | `backend/src/main/resources/schema.sql`, `data.sql` | 14 tables; `person.role_id`, `person.job_level`, `person.active` already carry both dimensions; 5 roles / 6 skills / 3 accounts seeded |
| Web | `web/src/` | `App.tsx` with 15 `<Route>` entries (13 real paths, `/login` outside the shell) and 12 screens behind `components/AppShell.tsx`; `lib/nav.ts` holds the three nav groups and filters each item by role, so `人员池` is already `COORDINATOR`-only; `lib/` per slice (fetch wrapper + pure "readings" modules); Tailwind 4, no chart library |
| Tests | `backend/src/test/**` (280 `@Test` across 38 files), `web/tests/**` (10 files, 57 cases; `npm test` runs 9 of them), `web/e2e/**` (15 Playwright checks) | green |
| Knowledge base | `docs/knowledge/**` | rules + one ADR (runtime stack); no domain chapter for reporting |
| Registry / handoff | `requirement-workspace/registry.md`, `handoff.md` | REQ-001 row only |

## 2. As-is → proposed delta

| Touched asset | As-is | Proposed delta |
|---|---|---|
| `openspec/specs/staffing/headcount-report` | does not exist | one ADDED capability created by this change's delta spec |
| `openspec/changes/add-headcount-charts/` | does not exist | new change package (proposal now; specs / design / tasks later) |
| `backend/.../domain/` | no reporting class | one read-only aggregation service plus its view records (`HeadcountService`, `HeadcountView`, `HeadcountBar`, `Share` rounding) |
| `backend/.../api/` | 15 controllers, no reporting route | one `HeadcountController` with a single `GET`; the coordinator guard reuses `api/Auth.coordinator` |
| `backend/.../data/` | repositories per concept | one grouped read over `person` joined to `role`, plus one read of the catalog's `role.id`, `role.name`, `role.active` so the axis can name every role still on offer; no insert, update or delete |
| `backend/src/main/resources/schema.sql` | 14 tables | **unchanged** — the report is derived, never stored |
| `web/src/screens/PeopleScreen.tsx` | two columns (pool table + selected profile) under a page head | a chart section inserted above the two columns, rendered for the coordinator only; no new route, no new screen |
| `web/src/lib/` | one module per slice | one `headcount.ts` with the payload types and the section's pure readings (share text, bar height, total line) |
| `web/src/App.tsx`, `components/AppShell.tsx`, `lib/nav.ts` | 13 routes, nav filtered by role | **unchanged** — `人员池` is already `COORDINATOR`-only in `nav.ts`, so the entry needs no work |
| `web/tests/` | 10 files, 57 cases | one more file for the readings, added to `package.json`'s explicit `npm test` list |
| `web/e2e/` | smoke + scenarios + journeys | optionally one check that a coordinator sees the two totals on `/people` while a lead sees no chart section |
| `requirement-workspace/registry.md`, `handoff.md` | REQ-001 row | one REQ-002 row each |
| Living specs | empty tree | unchanged by this change; the archive merge at P5 writes the capability |

Checked, no impact:

- `docs/knowledge/rules/**` — the architecture, quality, security and testing rules
  already cover this slice (layers, 100 % coverage gate, coordinator-only reads);
  no rule changes.
- The import contract, `WorkbookParser`, `ImportValidator`, `ImportService` — the
  report reads stored rows only.
- `post`, `assignment`, `allocation`, `recommendation`, `notice` — not read.
- Tests that already exist: nothing asserts the coordinator's full route list, so
  adding a route breaks no assertion (each suite proves its own 403s).

## 3. Blast radius

- One new read path over two existing tables; no write, no transaction boundary, no
  lock, no migration.
- One existing screen changes shape: `/people` gains a section above its two columns.
  No route, no screen and no navigation entry is added or removed, so a lead's rendered
  shell is byte-for-byte unchanged and a lead's `/people` view loses nothing.
- The 100 % JaCoCo gate on `domain*` and `api*` applies to the new classes, so the
  change must ship its own tests or the build fails — this is the main mechanical
  constraint, not a risk to existing behaviour.
- Pre-existing gap noticed while measuring, recorded rather than fixed here:
  `web/tests/board.test.ts` holds 4 cases but is missing from `package.json`'s
  `npm test` list, so it never runs. It belongs to a separate change; this change adds
  its own file to that same list and must not silently drop the others.

## 4. Artifact impact

| Artifact | Impact |
|---|---|
| Living specs | one ADDED capability `staffing/headcount-report` |
| Tests | API test for the answer's shape, membership, ordering and shares; unit test for the rounding and the empty pool; web readings test; optional E2E for the link and the 403 |
| Docs / KB | a short note in `docs/knowledge/domain/` describing the report's membership rule, if the P5 merge asks for one |
| Operations | none — no job, no schedule, no delivery |

## 5. Dual-track notes

- OpenSpec track: one small change, one capability delta, a clean vertical slice
  (endpoint → screen) that a reviewer can demo in one step.
- Agile track (optional): one story — "a coordinator sees the pool's shape by role and
  by grade" — with S1–S12 as acceptance hints. No split is needed; the change is
  smaller than one story on the REQ-001 scale.
- No conflict between the tracks.

## 6. Risks and blockers

| # | Risk | Severity | Handling |
|---|---|---|---|
| R1 | The brief's two totals differ (113 / 108), so a reviewer may expect two populations | medium | The requirement states the one-pool invariant (S1) and records the alternative as D2; if the instructor meant two populations, the membership rule is the only thing that changes |
| R2 | Shares that do not add to 100.0 look like a bug | low | One decimal, half-up, residue kept and stated in §5; the raw counts are never adjusted |
| R3 | A chart made mostly of zero bars reads as empty, and the total line is the only thing that says otherwise | low | Rev 2 keeps every offered category at zero on purpose; the mitigation is the total above each chart and the empty state that replaces both charts once nobody is active, so a zero bar always sits beside at least one real bar |
| R3b | A deactivated role that holds nobody leaves the axis while an active one with nobody keeps its bar — two different reasons for a missing bar could confuse | low | The rule is stated as one sentence in the spec (axis = active catalog roles ∪ roles still holding an active person) and pinned by two scenarios: the deactivated-with-people bar exists, the deactivated-with-nobody bar does not |
| R4 | A deactivated role still holds active people and could be mistaken for a bug | low | B-5/S11 pin it, mirroring how G1 already gates on a deactivated role |
| R5 | The 100 % coverage gate fails on the new classes | low (mechanical) | The capability ships its API and unit tests as part of the same change |
| R6 | A chart library would add a dependency for two static bar charts | low | The requirement fixes proportionality and labelling only; P3 decides, and plain markup is the cheaper default in this repo |
| R7 | The report duplicates a read the coordinator already has (`GET /api/people`) | low | Deliberate: the aggregation belongs on the server so the share and the total have one definition; the brief also asks for the endpoint |
| R8 | On one screen, the pool table lists inactive people while the charts count none, which reads as an error | medium | B-14 and S12 pin the difference: the table is the pool, the charts are the active pool; the chart section's own total line says which pool it counts |
| R9 | Adding a section to a screen that already has two columns can crowd the layout | low | The section goes above the columns and spans the width; the two existing columns are untouched |

No split blocker: decidable requirements, no integration boundary, no data ownership
change, no inconsistency with REQ-001.

## 7. Suggested split boundaries

- **One change** (`add-headcount-charts`), size **S**: one capability, one route, one
  screen section, ~2–3 hours.
- Internal order for P3/P4: (1) domain aggregation + rounding, (2) the route and its
  403, (3) the chart section inside `PeopleScreen` and its coordinator gate, (4) tests
  and the verify report.
- No story split and no dependency on `add-staffing-mvp` beyond the schema it already
  wrote; the two changes touch no common file except `PeopleScreen.tsx` and the
  registry.
