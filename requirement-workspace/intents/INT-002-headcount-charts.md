# INT-002 — Role and grade headcount charts (raw intent capture)

- **Captured:** 2026-11-11
- **Source:** course homework brief (`homework.pdf`, Day 13), condensed
- **Status:** promoted to `requirements/REQ-002-headcount-charts.md`

## Raw intent

Add two bar charts to the existing Staffing application:

1. **Headcount by Role** — one bar per role, the vertical axis is headcount.
2. **Headcount by Grade** — one bar per job level, the same axis.

Each bar shows its headcount and its share of the total; each chart shows the
total. The brief carries a sample illustration per chart ("sample illustration
with example headcounts"): roles PM 40 / 35.4%, BA 25 / 22.1%, DEV 14 / 12.4%,
QA 34 / 30.1% with `Total 113`; grades Junior 38 / 35.2%, Senior 42 / 38.9%,
Lead 28 / 25.9% with `Total 108`.

The request must be carried through the full P1–P5 flow of this repository, so
the charts are a normal change with a proposal, a delta spec, a design, tasks,
implementation, verification and an archive.

## Facts taken from the repository (not from the brief)

- The people pool already holds both dimensions: `person.role_id` → `role.name`
  and `person.job_level` (`Junior` < `Senior` < `Lead` < `Principal`), plus
  `person.active`.
- The catalog is seeded with five roles — `PM`, `BA`, `DEV`, `QA`, `UX` — and
  stays extensible, so the role axis is data, not a fixed list.
- `GET /api/people` already answers every person with role, job level and the
  active flag, and it is the coordinator's route alone.

## Clarifications answered in session (P1)

| # | Question | Answer |
|---|---|---|
| 1 | Which people are counted? | Active people only (`person.active = true`). An inactive person is in neither a bar nor the total. |
| 2 | Is a category with nobody shown? | **Rev 2 — yes.** A category with zero active people keeps its bar at height zero and reads `0` and `0.0%`. The role axis is every active catalog role plus any role that still holds an active person; the grade axis is always the four bands. A deactivated role that holds nobody does not appear. |
| 3 | Where does the number come from? | A new read-only aggregate endpoint. The screen renders what the endpoint answers; it does not group the pool itself. |
| 4 | Who may read the report? | The resource coordinator alone. A project lead answers 403 and is shown no link to the screen. |
| 5 | Does the report get its own screen and route? | No. Both charts are a section of the existing people pool screen (`/people`), drawn above the person table, so the pool's shape and its rows are read in one place. No route is added. |
| 6 | What does a project lead see on that screen? | Nothing of the report: the section is not rendered at all, so there is no placeholder and no number. The pool table behind it is already the coordinator's (`GET /api/people` answers 403). |

## Defaults recorded without a question

- **Both charts describe one pool.** The two figures in the brief are separate
  illustrations, each with its own example numbers, so their totals differ
  (113 and 108). In the system both charts read the same people pool, so the two
  totals must be equal; a difference would be a defect. Recorded as open item
  D2 in case the instructor meant two different populations.
- **The axis is stable, not data-driven.** Rev 2 flipped the first answer: an empty category keeps its bar at zero, so a role or band does not vanish from the chart when its last person leaves. The only categories that appear or disappear are roles entering or leaving the catalog. One exception stays: when the pool holds no active person at all, the empty state replaces both charts, because a chart whose tallest bar is zero has no scale to draw against.
- **Bar order is catalog order, not size order.** The illustration is not sorted
  by size (BA 25 sits before DEV 14 and QA 34), and the grade illustration is in
  band order (Junior, Senior, Lead). The MVP keeps that: roles in catalog order,
  grades in band order.
- **Shares are rounded to one decimal, half-up.** They may not add to exactly
  100.0; the raw counts stay exact and the rounding residue is not corrected.
- **The report reads the existing people pool read-only.** No new table, no new
  write path, no change to the import; the charts join the pool screen instead of
  owning a screen.
- **The entry already exists and is already gated.** `web/src/lib/nav.ts` lists
  `人员池` (`/people`) for `COORDINATOR` only, and `web/e2e/smoke.spec.ts` already
  asserts that a lead sees no such link, so "the shell draws the entry for the
  coordinator only" needs no change to the shell — the report sits behind an entry
  that is already correct.

## Not yet decided (deferred, non-blocking)

- Whether the shell should also carry a KPI tile for the total.
- Whether the charts later gain a project or date filter.
- Which charting approach the design picks (a library or plain markup).
