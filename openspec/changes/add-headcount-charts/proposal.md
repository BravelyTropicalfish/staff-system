# Proposal: Role and grade headcount charts

- **Change ID:** `add-headcount-charts`
- **Status:** proposed — P1 done; next `/sdd:delta-craft` (P2) and a G1 signature
- **Requirement:** `requirement-workspace/requirements/REQ-002-headcount-charts.md` (rev 1, S1–S12)
- **Impact brief:** `requirement-workspace/analysis/impact-brief-REQ-002-headcount-charts.md`
- **Source intent:** `requirement-workspace/intents/INT-002-headcount-charts.md`
- **Size:** S (one capability, one read-only route, one chart section in the existing pool screen)
- **Review aid:** `prototype/` — a static clickable mock of the chart section with three
  states (coordinator with data, coordinator with an empty pool, project lead); no build, no
  dependency, open `index.html` through any static server.
- **Scaffolding note:** the `openspec` CLI is not installed on this machine, so this
  package is written directly in the repository's layout instead of through
  `openspec new change`; the file set and headings follow `add-staffing-mvp`.

## Why

The pool already stores every person's role, job level and active flag, but nothing
shows the shape of the pool. A resource coordinator who must judge whether a role is
scarce, or whether the company is top-heavy, currently reads the pool person by
person through `/people`.

That produces two concrete costs:

1. **A question the data already answers stays unasked.** "How many `QA` do we have,
   and what share of the pool is that?" needs a manual count over a table sorted by
   employee number.
2. **The answer is not shareable.** Two coordinators counting by hand can disagree
   about whether an inactive person was included, and neither can point at a number
   that was defined once.

Two bar charts — headcount by role and headcount by grade — turn that manual count
into one read: each bar carries its headcount and its share of the total, and each
chart carries its total.

## What Changes

1. A new **read-only aggregate endpoint** answers, in one snapshot, the number of
   active people, one bar per category on the role axis and one bar per job-level band. It
   reads `person`, `role.name` and `role.active` and writes nothing; there is no new table and
   no schema change.
2. **Membership and the axis are fixed once, on the server.** Only `active = true` people are
   counted; every category on the axis keeps a bar even at zero, so nothing vanishes when its
   last person leaves; the total is the count of active people and therefore equals the sum of
   either axis, because both axes describe the same pool.
3. **Each bar carries two numbers.** Its raw headcount, and its share of the total as
   a percentage with one decimal, rounded half-up (`40` of `113` reads `35.4`).
   Shares may add to 99.9 or 100.1 after rounding; the counts are never adjusted to
   force 100.0.
4. **Ordering follows the model, not the size.** Role bars come in catalog order (role
   id), grade bars in band order (`Junior` < `Senior` < `Lead` < `Principal`), so the
   chart's shape changes only when the data does.
5. The two charts become a **section of the existing people pool screen** (`/people`),
   drawn above the person table, with a proportional bar per category, the headcount and
   the share on each bar, and a stated empty state when the pool has no active person.
   No route and no screen is added: the pool's shape and the people it counts are read in
   one place, for the coordinator alone — the shell's `人员池` entry already belongs to
   that role, so the navigation needs no change.

### New capabilities

| Capability | Sub-goal | Effort |
|---|---|---|
| `staffing/headcount-report` | One read-only aggregate of the active pool: bars by role and by grade with counts, shares, totals, ordering and the empty state | S |

No capability is MODIFIED or REMOVED: the living spec tree is still empty, and this
change adds one capability beside the six `add-staffing-mvp` capabilities.

### Key rules fixed by this proposal

- **One pool, two axes.** Both charts describe the same active people, so their two
  totals are equal by construction and each equals the sum of its own bars. The
  brief's two illustrations carry different example numbers (113 and 108) because each
  is a separate sample; in the system a difference between the two totals is a defect.
- **Active only.** An inactive person is in neither a bar nor a total, so the report
  matches what the matching gates already treat as a candidate (G1 reads the same
  flag).
- **Empty categories keep their place.** The axis is every role the catalog still offers
  (`active`), plus every role that still holds an active person, plus the four job-level bands.
  A category with nobody keeps a bar at height zero reading `0` / `0.0%`, so a role or band
  never disappears from the chart while it is still offered. The one category that leaves the
  axis is a deactivated role that holds nobody, because the catalog no longer offers it — and
  when the pool holds no active person at all, the empty state replaces both charts, since a
  chart whose tallest bar is zero has no scale to draw against.
- **Shares are rounded once, and never corrected.** One decimal, half-up. The raw
  counts stay exact, and a residue that leaves the shares at 99.9 or 100.1 is accepted
  rather than redistributed.
- **Order comes from the model.** Roles by catalog order, grades by band order. Neither
  axis is sorted by size, so the same pool always produces the same layout and the
  chart never implies a ranking.
- **A deactivated role still counts its people.** The bar is named by the stored
  `role.name`; switching a role off in the catalog changes no number, exactly as a
  deactivated role keeps gating in the candidate search.
- **The report is the coordinator's, and a lead sees no trace of it.** It is an
  aggregate of the same pool the coordinator already reads, so the endpoint answers a
  lead 403 and the pool screen draws the chart section only for the coordinator — no
  placeholder, no refusal text, no number. The shell already agrees: `lib/nav.ts` offers
  `人员池` to `COORDINATOR` alone and `smoke.spec.ts` asserts a lead sees no such link.
  The report never widens that audience and carries no personal detail — a bar is a
  count, never a list of names.
- **Two numbers on one screen, two meanings.** The person table lists every imported
  person, inactive ones included; the charts count only the active ones. That is by
  design, and the chart section's total line names which pool it counts.
- **Nothing is stored.** The answer is derived on every read; there is no report table,
  no snapshot, no job. A pool change is visible on the next read.

### Delivery order inside the single change

Domain aggregation and rounding → the read-only route and its 403 → the chart section
inside `PeopleScreen` and its coordinator gate → the tests and the verify report. Each
step is demoable against the previous one.

## Non-Goals

- **No filter, drill-down or time series.** The charts always describe the whole active
  pool as it stands now; a filter would change the membership rule and is its own
  requirement.
- **No write path from the report.** Nothing on the screen changes a person, a role or
  a job level.
- **No new table, column or migration.** The data already exists.
- **No personal detail.** No employee number, name, email, skill or allocation reaches
  the answer.
- **No export, image download or scheduled delivery.**
- **No per-project, per-lead or per-role breakdown of the breakdown.**
- **No charting library requirement.** The requirement fixes proportionality and
  labelling; the design chooses the mechanism, and plain markup is the cheaper default
  in this repository.

## Impact

### Self (this change)

- One capability delta spec under `staffing/`, plus its Chinese translation, design and
  tasks.
- Backend: one read-only aggregation service with its view records, one repository read,
  one controller with a single `GET`, and their tests. The existing 100 % line and
  branch coverage gate on `domain*` and `api*` applies, so the tests ship with the code.
- Web: one section inside `PeopleScreen` with its coordinator gate, one `lib` module for
  the section's pure readings, and one test file added to `package.json`'s explicit test
  list. No route, no new screen, and no change to `App.tsx`, `AppShell.tsx` or `nav.ts`.
- Registry and handoff gain a REQ-002 row.

### Upstream

- Reads `person` (`role_id`, `job_level`, `active`) and `role.name` from the schema
  `add-staffing-mvp` already wrote. No route or contract of that change is modified.
- Reuses `api/Auth.coordinator`, the same guard `/api/people` uses.

### Downstream

- No existing consumer is affected: no route changes shape, and the shell's lead view
  differs by one absent link.
- Future work this would enable, each a separate change: a pool KPI tile, a project or
  date filter, utilisation reporting. None is implied here.

## Assumptions

1. Headcount means the number of people, not the sum of post headcounts.
2. The pool is small enough (hundreds to a few thousand rows) that one grouped read per
   request is fine; no cache, no materialised view.
3. "Share of the total" means the share of the active pool, the only total either chart
   shows.
4. The role axis is data-driven in one direction only: a role appears as soon as it is an
   active catalog item, and stays on the chart at zero until the catalog drops it; no code names
   the five seeded roles. The grade axis is always the four bands.
5. Job level is the four-band scale of REQ-001; the report never shows a numeric rank.
6. The runtime is the one ADR-001 recorded — React 19 + TypeScript SPA over Spring
   Boot on H2; this change adds no dependency.

## Open questions carried forward (non-blocking)

| # | Question | Owner | Handling |
|---|---|---|---|
| D1 | Whether the shell should also carry a KPI tile for the total | Product | Not in this change: the total sits above each chart only |
| D2 | Whether the two charts were meant to cover two different populations (the brief's 113 and 108) | Product / instructor | The MVP reads one pool, so the two totals are equal by construction; a different population would need its own stated membership rule and its own scenario |
| D3 | Which mechanism draws the bars | Dev | Design-time choice at P3; the requirement fixes proportionality and labelling only |
| D4 | Whether the report later gains a project or date filter | Product | Out of scope; a filter changes the membership rule and becomes its own requirement |

## Effort and risk

- **Size:** S. One capability, one route, one chart section, no schema change; the smallest
  change this repository can carry end to end.
- **Main risk:** the brief's two totals differ, so a reader may believe the charts cover
  two populations. Mitigation is that the one-pool invariant is stated as a rule and as
  scenario S1, and D2 records the alternative.
- **Second risk:** rounding that does not reach 100.0 reads as a bug. Mitigation is one
  rounding rule, half-up, one decimal, with the residue accepted and stated.
- **Third risk:** a chart half made of zero bars reads as empty. Mitigation is that the total
  line names the pool, and the empty state replaces both charts entirely once nobody is active —
  so zero bars only ever appear beside at least one real bar.
- **Fourth risk:** the coverage gate fails the build if the new classes ship untested.
  That is a constraint, not a risk: the tests are part of the change.

## Definition of done for this proposal

- G1 sign-off on the membership rule (active only), the non-empty rule, the share
  rounding, the ordering rule, the one-pool invariant and the coordinator-only read.
- A delta spec under `openspec/changes/add-headcount-charts/specs/staffing/headcount-report/`
  covering S1–S12 as `SHALL` requirements with observable outcomes.
- Design and tasks at P3, implementation and a local verify report at P4, archive at P5.
