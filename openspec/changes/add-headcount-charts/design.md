# Design — add-headcount-charts

- **Change id:** `add-headcount-charts`
- **Inputs:** delta spec (1 capability, 6 requirements / 12 scenarios, G1 **Pass** on rev 2),
  `atdd-design.md` (8 API / 4 E2E, unit items deferred), `proposal.md` (key rules, Non-Goals,
  D1–D4), `prototype/` (the agreed picture)
- **Out of scope of this document:** production code, task breakdown (`tasks.md`), Gate decisions

## 1. Stack

Unchanged. ADR-001 already fixed React 19 + TypeScript + Vite + Tailwind on the front, Spring Boot
3.4 on Java 21 with H2 behind it; this change adds **no dependency** — in particular no chart
library. The bars are markup with an inline height, which is what the prototype already proves is
enough for two static bar charts.

Rejected alternatives: a chart library (a dependency for two rectangles and a label);
a canvas/SVG renderer (harder to assert in E2E than DOM elements); computing the shares in the
browser (two roundings, two answers — see §6).

## 2. Layering

`architecture-baseline` rules 1–3 hold: UI → domain → data, one way.

```text
web/src/components/HeadcountSection.tsx   section: two cards, the empty state, the coordinator gate
web/src/components/ChartCard.tsx          one chart: title, total, axis, bars, labels
web/src/lib/headcount.ts                  payload types + pure readings (share text, ticks, height)
api/HeadcountController.java              one GET; the coordinator guard; nothing else
domain/HeadcountService.java              the aggregation: axis, counts, shares
domain/HeadcountView.java                 the answer: total + byRole + byGrade (records)
data/HeadcountRepository.java             two read-only queries; no insert, update or delete
```

The controller decides nothing but the role; the service owns the axis, the counts and the
rounding; the repository owns two reads. That keeps the rule testable without HTTP, and it mirrors
the `Service` + `*View` + thin controller shape every other slice already uses.

## 3. Data

**No schema change, no migration, no new table.** The two reads:

1. **The role axis** — every `role` row with `active = true`, plus every role that at least one
   active person still holds, ordered by `role.id`:

   ```sql
   select r.id, r.name, count(p.employee_no)
     from role r
     left join person p on p.role_id = r.id and p.active = true
    where r.active = true or p.employee_no is not null
    group by r.id, r.name
    order by r.id
   ```

2. **The grade axis** — four bands, so the grouping is data-derived and the bands come from the
   ordered scale, not from the rows:

   ```sql
   select p.job_level, count(*)
     from person p
    where p.active = true
    group by p.job_level
   ```

   The service walks `Bands.JOB_LEVELS` and reads a count per band, so a band with nobody is a zero
   rather than a missing key. `Bands` is the existing ordered scale; the report adds no vocabulary.

**Nothing is stored.** There is no report table, no materialised view and no cache — the answer is
derived per request, which is what makes the freshness NFR (S-fresh) hold by construction.

## 4. API

One route, one snapshot, coordinator only.

```http
GET /api/headcount
```

```json
{
  "total": 113,
  "byRole":  [ { "roleId": 1, "role": "PM", "count": 40, "share": 35.4 }, "…" ],
  "byGrade": [ { "grade": "Junior", "count": 38, "share": 33.6 }, "…" ]
}
```

- **Guard:** `Auth.coordinator(authentication)` — the same call `/api/people` and `/api/imports`
  use. A lead answers 403 with the shared `{code,message}` body and no numbers.
- **Write verbs are not routes:** `POST`, `PATCH` and `DELETE` on `/api/headcount` answer 405
  because no handler exists, which is the existing pattern for `/api/people`.
- **Empty pool:** `total: 0`, both groupings present with `count: 0` and `share: 0.0`. The shape
  never changes, so the client has one payload to parse and the empty state is a rendering
  decision (requirement 6).
- **No filter parameter:** `?project=`, `?role=` and friends are simply not read; the route has no
  query arguments at all. That is the reverse check for the "no filter" Non-Goal.

## 5. Front end

`PeopleScreen` gains one section above its pool table; nothing else on the screen moves.

```text
PeopleScreen
├─ page head                          (unchanged)
├─ HeadcountSection                   (new, coordinator only)
│  ├─ ChartCard  "Headcount by Role"   → axis ticks, N bars, labels with count and share
│  └─ ChartCard  "Headcount by Grade"
└─ pool table card                    (unchanged; a row opens the profile modal)
```

- **Order of operations (S9 depends on it).** The screen reads `GET /api/session` first. Only when
  the role is `COORDINATOR` does `HeadcountSection` mount and request `/api/headcount`. A lead
  therefore renders no section **and** puts no report request on the wire. Reading the report first
  and hiding the result on a 403 would satisfy the pixels and fail the sentence.
- **Readings are pure functions** in `lib/headcount.ts` — `shareText(share)` → `"35.4%"`,
  `chartTicks(top)` → the axis labels, `barHeight(count, top)` → pixels, `isEmpty(view)` → whether to
  draw the empty block, `sectionTotal(view)` → the total line. The component renders them; a unit
  test exercises them without a browser, in the same style as `lib/people.ts`.
- **The client never recounts.** It receives `count` and `share` and prints them. Recomputing a
  share from the counts would introduce a second rounding rule (design §6).
- **Zero bar.** A count of 0 renders a 3px grey stub with a dimmed label reading `0` and `0.0%`. A
  zero-height element would be invisible, and the category's presence is exactly what S5 asserts.
- **Styles** follow the prototype: two cards in a responsive grid, the plot with a baseline and a
  light grid, the value above the bar, the name and the share under it. The prototype's yellow
  "评审状态" bar and "图上元素与规格条款" card are **not** built — they are review devices
  (`atdd-design.md` §6).

## 6. Decisions and trade-offs

| Decision | Rationale | Rejected |
|---|---|---|
| `share` is a `double` with one decimal, computed once on the server | the number is a display value with a stated rounding rule; a `BigDecimal` would add a type the client then has to parse | computing it in the browser (two roundings can disagree) |
| The axis is computed from `role` left-joined to `person`, not from the person rows alone | an active role with nobody must still get a bar, and a deactivated role with nobody must not — the join expresses both in one read | grouping the person rows and then padding the catalog in the service (two sources of order) |
| The empty pool still answers both groupings with zeros | the payload shape stays constant, so the client has no second branch to parse | answering `null` groupings (a second shape the client must guard) |
| No cache, no snapshot | the pool is small, the query is one grouped read, and a cache is the only way the report could ever be stale | an in-memory cache (invents an invalidation rule the spec never asked for) |
| Native `<div>` bars, no chart library | two static bars per chart; a library adds a dependency and a rendering surface E2E cannot read as text | Recharts / Chart.js |
| The section reads the session itself | `PeopleScreen` already knows how to read `/api/session` (the profile panel does not, but `CatalogScreen` and `PostScreen` do); the gate has to be decided before the fetch | gating on a 403 from the report (the request has already happened) |

## 7. Testing commands (three layers)

| Layer | Command | Where it runs |
|---|---|---|
| backend unit + API acceptance | `cd backend && ./mvnw verify` | local stop check, CI `unit` + `acceptance` jobs |
| web readings unit | `cd web && npm test` (the new `tests/headcount.test.ts` joins the explicit list) | local stop check, CI `unit` job |
| E2E | `cd web && npx playwright test` | local before PR, CI `acceptance` job |

JaCoCo keeps 100 % line and branch coverage on `com.example.staffing.domain*` and
`com.example.staffing.api*`; the zero bar, the empty pool and the half-up rounding are the three
branches a reviewer will look for, so they ship with tests rather than an exclusion.

## 8. Risks carried into apply

- The 100 % coverage gate fails the build if the new classes ship untested (mechanical, not
  behavioural).
- A lead's `/people` view changes only by the **absence** of the section; nothing else on that
  screen moves, so no existing E2E assertion should need editing — but the smoke test does assert
  the pool link is absent for a lead, and the new section must not add a link.
- `web/tests/board.test.ts` is still missing from `npm test` (a pre-existing gap recorded in the
  impact brief). This change adds its own file to that list and must not silently drop the others.
