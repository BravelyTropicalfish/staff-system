# REQ-002 — Role and grade headcount charts

- **ID:** REQ-002
- **Status:** ready — rev 2 (a category with nobody keeps its bar at zero)
- **Source:** `intents/INT-002-headcount-charts.md`
- **OpenSpec change:** `openspec/changes/add-headcount-charts/`
- **Impact brief:** `analysis/impact-brief-REQ-002-headcount-charts.md`
- **Date:** 2026-11-11

## 1. Intent

The people pool already knows every person's role and job level, but nothing shows
the shape of the pool: how many people sit in each role and how many in each grade.
A resource coordinator who is about to judge whether a post's role is scarce, or
whether the company is top-heavy, has to read the pool person by person. Two bar
charts — headcount by role and headcount by grade — answer that question at a
glance: each bar carries its headcount and its share of the total, and each chart
carries its total.

## 2. Actors and permission boundaries

| Actor | May do | May not do |
|---|---|---|
| Resource coordinator | Open the people pool screen and read both charts in its chart section — the bars, the shares and the totals | Change any number from this screen |
| Project lead | — | Read the charts: the read answers 403, and the people pool screen draws no chart section at all — no placeholder, no number |
| System | Count the active people by role and by grade, compute each share, and answer one snapshot | Decide what a share means, or filter the pool by anything |

The report is an aggregate of the same pool the coordinator already reads through
`GET /api/people`, so it inherits that route's audience and is never widened to the
decision-maker role that only sees recommended candidates. The shell already
agrees with that boundary: `web/src/lib/nav.ts` offers `人员池` to `COORDINATOR`
only, so no navigation change is needed for this requirement.

## 3. In scope

- One read-only aggregate endpoint that answers, in one snapshot: the number of
  active people, the bars per role, and the bars per grade.
- **Headcount by Role:** one bar per role on the role axis — every active catalog role,
  plus any role that still holds an active person even after being deactivated —
  ordered by catalog order (role id ascending). A role on the axis with nobody in it
  keeps a bar at height zero.
- **Headcount by Grade:** one bar per job level band, always the four of them
  (`Junior`, `Senior`, `Lead`, `Principal`), ordered by band order. A band with
  nobody keeps a bar at height zero.
- Every bar carries its raw headcount and its share of the total, the share a
  percentage with one decimal.
- Each chart carries its total, which is the sum of its own bars.
- Both charts as a **section of the existing people pool screen** (`/people`),
  drawn above the person table next to the rows they summarise. No new route and no
  new screen: the pool's shape and its people are read in one place.
- The chart section is rendered for the coordinator only; a lead opening the screen
  sees no chart section at all.
- An empty state for a pool with no active person.

## 4. Out of scope

- Any filter, drill-down or time series: the charts always describe the whole
  active pool as it stands now.
- Any write, any new table, any column, any change to the import contract or the
  person master data.
- Any personal detail: a bar is a count, never a list of names, and the report
  carries no employee number, email, skill or allocation.
- Any export, image download or scheduled delivery of the charts.
- Any per-project or per-lead breakdown.

## 5. Domain concepts and data contract (deep-dive summary)

The report owns no state. It is derived on every read from two stored concepts of
REQ-001 and is never persisted.

| Concept | Grain | Key fields | States / transitions |
|---|---|---|---|
| HeadcountReport | one read of the whole active pool | total, `byRole[]`, `byGrade[]` | derived on every read; no states, nothing stored |
| RoleBar | one role on the role axis (every active catalog role, plus any role still holding an active person) | role id, role name, headcount (≥ 0), share | always present while the role is on the axis; a count of 0 is a bar at zero, not an absence |
| GradeBar | one job level band, always the four | band name, headcount (≥ 0), share | always present; a count of 0 is a bar at zero, not an absence |

Source rows (`schema.sql`): `person.role_id`, `person.job_level`, `person.active`,
joined to `role.name`, plus `role.active` so the role axis can name every role the
catalog still offers. Nothing else is read.

Proposed wire contract, one route, one snapshot:

```json
{
  "total": 113,
  "byRole": [
    { "roleId": 1, "role": "PM", "count": 40, "share": 35.4 },
    { "roleId": 2, "role": "BA", "count": 25, "share": 22.1 }
  ],
  "byGrade": [
    { "grade": "Junior", "count": 38, "share": 35.2 }
  ]
}
```

Rules the contract fixes:

- `total` is the number of **active** people and equals the sum of `byRole`
  counts and of `byGrade` counts, because both axes describe the same pool.
- `count` is an integer ≥ 0 in every bar. Every category on the axis is present,
  including one whose count is 0; nothing is dropped for being empty. The axis is:
  every `role` row with `active = true`, **plus** every role that holds at least one
  active person, plus the four job-level bands. A deactivated role that holds nobody
  is the one category that does not appear, because the catalog no longer offers it.
- `share = round(count / total × 100, 1)` with half-up rounding, so a bar of 40
  out of 113 reads `35.4`. Shares may add to 99.9 or 100.1; the residue is never
  redistributed and the counts are never adjusted.
- `byRole` is ordered by `roleId` ascending and `byGrade` by band rank ascending.
  Any tie-break beyond that is unnecessary, because both orders are total.
- The bar's label is the stored name (`role.name`, the job-level band name), so a
  renamed catalog item shows its new name and a deactivated one keeps its stored
  name.
- A field rename in P2 is allowed; the grain, the membership rule (active only,
  non-empty only) and the ordering rule are not.

## 6. Display contract (deep-dive item ⑫)

| Surface | Content | State |
|---|---|---|
| People pool screen (`/people`), chart section above the person table | Two titled charts, each with its total above the bars: "Headcount by Role — Total `<n>`" and "Headcount by Grade — Total `<n>`" | reads the endpoint once, beside the pool read |
| A bar | the headcount printed with the bar and the share printed as `35.4%`; a zero bar reads `0` and `0.0%` | one per category on the axis, empty ones included |
| Vertical axis | headcount, with a readable scale drawn from the tallest bar | — |
| Empty pool | both chart areas print `No active people` and draw no bar, with a total of `0` | `total = 0`, and this state wins over the zero bars: a chart whose tallest bar is zero has no scale |
| Shell navigation | the existing `人员池` entry | unchanged — `nav.ts` already offers it to the coordinator only |
| A lead opens `/people` | no chart section is drawn and the report is not requested | section absent, not a refusal message |
| A lead calls the endpoint directly | 403 | nothing rendered |

No charting library is required by the requirement; the bars must be proportional
to their counts and labelled, and the implementation choice belongs to P3.

## 7. Boundary enumeration (deep-dive item ⑩)

| # | Branch | In / out |
|---|---|---|
| B-1 | An active person with a role and a job level | counted once on each axis |
| B-2 | An inactive person | in neither axis and in neither total |
| B-3 | A role on the axis with no active person | present with a zero-height bar reading `0` / `0.0%` |
| B-4 | A job level with no active person (for example `Principal`) | present with a zero-height bar |
| B-3b | A deactivated role that holds nobody | absent from the axis, because the catalog no longer offers it |
| B-5 | A role deactivated in the catalog while it still holds active people | in `byRole` under its stored name |
| B-6 | A role renamed | in `byRole` under the new name |
| B-7 | The pool has no active person at all (`total = 0`) | empty state; no bar, no division by zero, no `NaN` |
| B-8 | One category holds the whole pool (`count = total`) | that bar reads `100.0` |
| B-9 | A share lands exactly on a half (for example 1 of 8 = `12.5`) | half-up, `12.5` |
| B-10 | Shares do not add to 100.0 after rounding | accepted as is; counts stay exact |
| B-11 | The stored role of a person was deactivated *and* renamed | both rules apply: the bar exists, under the new name |
| B-12 | A lead asks the endpoint for the report | 403, nothing answered |
| B-13 | A lead opens the pool screen | no chart section is rendered and the endpoint is not called; the table itself still answers its own 403 |
| B-14 | The table lists inactive people while the charts count none | the two numbers on one screen disagree by design: the table is the pool, the charts are the active pool |

## 8. Cross-REQ alignment (deep-dive item ⑪)

| Touches | REQ-001 statement | Anchor in this entry |
|---|---|---|
| `person.active` | The active flag arrives by import and is the first gate (G1) | §3 "active people only"; B-2 |
| `role` catalog | Five seeded, extensible roles; a delegated name freezes once referenced | §5 ordering by role id; B-5, B-6 |
| `job_level` | Four ordered bands, compared by rank, never by number | §5 band order; B-4 |
| Coordinator-only read of the pool | "The full candidate pool … belong to the coordinator" | §2; B-12 |

No ✗ on the table: every touched statement is satisfied by this entry, and this
entry adds no vocabulary of its own beyond `HeadcountReport`, `RoleBar` and
`GradeBar`.

## 9. Scenarios and acceptance hints

REQ-002 numbers its own scenarios S1–S12; they are independent of REQ-001's S1–S40.

| # | Scenario | Observable outcome |
|---|---|---|
| S1 | Both charts describe one pool | `total` equals the sum of `byRole` and equals the sum of `byGrade`, and equals the number of active people |
| S2 | An inactive person is in neither chart | the pool gains an inactive person: `total` is unchanged and no bar moves |
| S3 | A bar carries its count and its share | PM at 40 of 113 reads `40` and `35.4%` |
| S4 | A share rounds to one decimal | 1 of 3 reads `33.3%`; 2 of 3 reads `66.7%`; 1 of 8 reads `12.5%` |
| S5 | A category with nobody keeps its bar | a role or band with no active person is drawn at zero and reads `0` and `0.0%` |
| S6 | Role bars follow catalog order | counts PM 40, BA 25, DEV 34 with QA and UX at 0 are drawn PM, BA, DEV, QA, UX — the two zeros still get bars, and the order is not by size |
| S7 | Grade bars follow band order | counts Senior 42, Junior 38, Lead 28 and Principal 0 are drawn Junior, Senior, Lead, Principal |
| S8 | Both charts sit on the pool screen | the coordinator opens `/people` and reads both totals above the person table, on one page |
| S9 | A lead sees no report | a lead opens `/people`: no chart section is drawn, no report is requested, and a direct call answers 403 |
| S10 | An empty pool says so | with no active person the screen prints `No active people` and a total of `0`, and no bar |
| S11 | A deactivated role keeps its active people counted | the bar is present under the stored role name and `total` is unchanged |
| S12 | The table and the charts differ on purpose | a pool with one inactive person lists that person in the table while `total` and the bars count only the active ones |

## 10. DoR self-check (light, base 8 + deep-dive ⑨–⑫)

| # | Item | Status | Note |
|---|---|---|---|
| 1 | Intent clear | resolved | §1 |
| 2 | Actors defined | resolved | §2, with the 403 boundary |
| 3 | Scope bounded | resolved | §3 in, §4 out |
| 4 | Scenarios / AC hints concrete | resolved | S1–S12, every outcome observable |
| 5 | Exception / boundary | resolved | B-1–B-14 (B-3b included), zero bars, empty pool, rounding and the lead's view included |
| 6 | Results verifiable | resolved | counts, shares, order and total are all readable from one answer |
| 7 | Dependencies / app-arch touchpoints | resolved | one new read-only route over `person` + `role`; no new table; no new screen or route — the charts join `PeopleScreen`, and the shell's entry is already coordinator-only |
| 8 | Open-questions gate | resolved | 0 blocking; D1–D4 non-blocking |
| ⑨ | Data contract decided | resolved | §5 grain, fields, membership and rounding |
| ⑩ | Boundary enumerated | resolved | §7 |
| ⑪ | Cross-REQ aligned | resolved | §8, no ✗ |
| ⑫ | Notification / display contract | resolved | §6, the pool screen's chart section and its states |

Verdict: **0 BLOCKER, 0 open blocking question** — ready for impact analysis and
`/sdd:proposal-split`.

## 11. Open questions

Blocking: **none** (all resolved in session, `intents/INT-002-headcount-charts.md`).

| # | Question | Owner | Status | Handling |
|---|---|---|---|---|
| D1 | Whether the shell should also carry a KPI tile for the total | Product | deferred | The MVP keeps the total above each chart only |
| D2 | Whether the two charts were meant to cover two different populations (the brief's 113 and 108) | Product / instructor | deferred | The MVP reads one pool, so the two totals are equal by construction; a different population would need its own stated membership rule |
| D3 | Which charting approach the screen uses (library or plain markup) | Dev | deferred | Design-time choice at P3; the requirement only fixes proportionality and labelling |
| D4 | Whether the report later gains a project or date filter | Product | deferred | Out of scope for this change; a filter changes the membership rule and would be its own requirement |
