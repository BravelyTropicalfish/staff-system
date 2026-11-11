# AC / DoV review — add-headcount-charts (P2)

- **Change:** `openspec/changes/add-headcount-charts/`
- **Phase:** P2 / `step=all` (generate + review)
- **Artifact level:** 2 (proposal + delta spec; no `design.md`, no `tasks.md` at this phase)
- **Delta specs:** 1 capability, 6 requirements, 12 scenarios (`staffing/headcount-report`)
- **Tooling:** the `openspec` CLI is not installed (`openspec --version` fails), so `openspec
  validate` could not run; the tree is hand-authored to the documented layout
  (`changes/<id>/specs/<capability>/spec.md`, `## ADDED Requirements`)
- **Date:** 2026-11-11 (rev 2: every category on the axis keeps its bar, zero included)

## Acceptance criteria

| Item | Result | Evidence | Notes |
|---|---|---|---|
| AC-1 Every requirement has at least one testable AC | Pass | 6 `### Requirement:` blocks vs 12 `#### Scenario:` blocks in `specs/staffing/headcount-report/spec.md`, counted by script | Two scenarios per requirement, except "The report is the coordinator's alone" and "An empty pool is stated" which carry one each because their whole claim is a single refusal / a single empty state |
| AC-2 ACs are observable | Pass | "Both charts describe one pool" asserts three equal numbers (total 113 and both axis sums); "A bar carries its count and its share" asserts `40` and `35.4`; "A category with nobody keeps its bar" asserts the bar is drawn at zero and reads `0` / `0.0`; "A lead sees no report" asserts no section drawn, no report requested and `403` on a direct call | Every scenario asserts a number, an order, a presence/absence or a status code; no scenario says "displays correctly" |
| AC-3 Negative and edge cases are called out | Pass | Negative: inactive person moves nothing; a deactivated role that holds nobody leaves the axis; a lead refused. Boundary: "A share rounds to one decimal" (1 of 8 = `12.5`, the exact half), "A deactivated role keeps its active people counted" (bar present) beside the absent one, "An empty pool says so" (total 0, no divisor, and the empty state replaces the zero bars), "The table and the charts differ on purpose" | The two riskiest edges are pinned by scenario rather than prose: rounding at a half, and the pool whose only rows are inactive. B-8 (`100.0` for a single category) and B-10 (residue accepted) are stated as normative sentences, not scenarios, because they are the same rounding rule |
| AC-4 No disguised implementation detail, no vague wording | Pass | The spec names outcomes only: a count, a share, an order, a presence, a status code. The route's mechanism, the bar markup and the rounding function are left to P3 | One deliberate exception, stated as a requirement rather than a mechanism: "one read-only request that writes nothing". It is observable (a write verb answers 405 and no row changes) and it is what keeps a second aggregation out of the browser. No `correctly` / `quickly` / `appropriately` appears |

## Definition of verified

| Item | Result | Evidence | Notes |
|---|---|---|---|
| DoV-1 Scope matches the proposal, no silent expansion | Pass | 1 delta capability vs the single row of `proposal.md` "New capabilities"; the five `What Changes` steps map onto the six requirements (step 5 maps onto two: the section and its empty state) | No `MODIFIED` / `REMOVED` section: the capability is new. The person table's behaviour is **not** changed by this delta — it already lists inactive people, and the scenario only pins that it keeps doing so |
| DoV-2 Ambiguities listed or resolved | Pass | REQ-002 §11: 0 blocking, D1–D4 deferred. `proposal.md` "Open questions carried forward" mirrors the same four | The one ambiguity that could invalidate this delta is D2 (whether the brief's two totals describe two populations). It is recorded, not hidden: if two populations were meant, the membership rule in requirement 1 is what changes, and nothing else |
| DoV-3 Role concerns noted (IO / SA) | Pass | "Residual concerns for IO / SA" below | Six items; two of them are load-bearing for P3 (the coordinator gate must be decided before the fetch, and the rounding lives in one place) |
| DoV-4 No business implementation claimed | Pass | No code, no `tasks.md`, no `design.md` in the change folder | P2 shapes the contract only |
| DoV-5 Requirements trace to the proposal / approved requirements | Pass | Every requirement carries a `**Traces:**` line naming a proposal step or key rule, a REQ-002 section and its boundaries, and its scenarios; S1–S12 are each referenced exactly once | The five `key rule "…"` quotations plus three further quoted rule names were checked mechanically as literal substrings of `proposal.md` (all True). No invented rule name remains |

## Machine checks run

| Check | How | Result |
|---|---|---|
| Requirement / scenario / trace counts, English | counted `### Requirement:`, `#### Scenario:`, `**Traces:**` | 6 / 12 / 6 |
| Same counts, Chinese | same three patterns in `spec.zh.md` | 6 / 12 / 6 — identical to English |
| Every scenario has GIVEN / WHEN / THEN | counted `**GIVEN**`, `**WHEN**`, `**THEN**` | 12 / 12 / 12 in both languages |
| No duplicate scenario titles | grouped the 12 titles | none |
| Scenario coverage | extracted every `S<n>` from the spec | exactly `1..12`, no gap and no extra |
| Rule quotations are real | normalized `proposal.md` whitespace, then `Contains` each quoted rule name | 8 of 8 True (`One pool, two axes`, `Active only`, `Empty categories keep their place`, `Order comes from the model`, `A deactivated role still counts its people`, `Shares are rounded once, and never corrected`, `Two numbers on one screen, two meanings`, `The report is the coordinator's, and a lead sees no trace of it`) |
| One pool invariant is asserted, not implied | read requirement 1 and "Both charts describe one pool" | the total equals both axis sums, so the brief's 113 / 108 cannot disagree in the product without failing a scenario |

## Corrections applied while writing the delta

- **Rev 2 — empty categories kept their place, the opposite of rev 1.** The first delta dropped a
  category whose count reached zero. The product decision is the reverse: a role or band that the
  catalog still offers keeps its bar at zero, so the chart's shape stops changing whenever someone
  is imported or deactivated. The rewrite touched requirement 2, its four scenarios, the empty-pool
  requirement and both language copies; the counts stayed 6 / 12 so S1–S12 still map one to one.
- **The zero rule needed a second boundary.** "Zero keeps its bar" and "a deactivated role leaves the
  axis" can contradict each other, so the axis is defined once — active catalog roles ∪ roles still
  holding an active person — and pinned twice: the deactivated role with three people keeps its bar,
  the deactivated role with nobody does not. Recorded as B-3b and as a second THEN in the
  "A deactivated role keeps its active people counted" scenario.
- **The empty state had to be given precedence.** With zero bars allowed, a pool of nobody would
  draw eight zero bars and print "no active person" at once. The empty-pool requirement now says it
  replaces both charts, zero bars included, because a chart whose tallest bar is zero has no scale.
- **The report stopped being its own screen.** P1 first described a dedicated screen behind a
  new route; the product decision moved it into `/people`. The delta says so as a requirement
  rather than a remark — "No separate screen and no additional route SHALL be introduced" — so
  a reviewer can refuse a later reintroduction of a route.
- **Two numbers on one screen became a requirement.** Embedding exposed a conflict the
  standalone screen did not have: the person table lists inactive people while the charts
  count none. Left unwritten, the first reviewer would call it a bug. It is now requirement 4's
  second sentence plus the scenario "The table and the charts differ on purpose".
- **The lead's view became "not rendered", not "403 shown".** The earlier wording let a refusal
  message appear in the section; the requirement now forbids a placeholder and forbids even
  requesting the report from that screen, so the screen cannot leak a number through an error
  path.
- **The empty pool got its own requirement.** It began as a sentence in the display contract;
  writing a testable AC showed it is the one branch with a division in it (total = 0), so it
  became its own requirement with its own scenario.
- **The impact brief's as-is inventory was wrong and was corrected.** It said 11 routes / 10
  screens / 7 web test files, taken from a stale read of `App.tsx`; measured values are 13 real
  paths (15 `<Route>` entries) / 12 screens behind `AppShell` / 10 web test files with 57 cases
  (53 run by `npm test`). Recorded here because the delta's feasibility argument rests on it.

## Residual concerns for IO / SA

- **SA:** "no chart section and no request for a lead" forces an order of operations the screen
  does not have today — `PeopleScreen` does not read the session. If P3 renders the section and
  then hides it on a 403, the report has already been requested and S9 fails. The design must
  read the role first (or reuse the nav's role) **before** fetching the report.
- **SA:** the share is computed once, on the server. If the view recomputes a percentage from
  the returned counts, two roundings can disagree and S3 / S4 stop being reproducible. One
  rounding rule, one place.
- **SA:** the 100 % line and branch coverage gate on `domain*` / `api*` applies to the new
  classes; the empty pool and the rounding half are exactly the branches a reviewer will look
  for, so they need tests rather than an exclusion.
- **IO:** the total above each chart should say which pool it counts. On a screen whose table
  shows inactive people, an unexplained total reads as a miscount.
- **Product:** the axis no longer moves with the data (rev 2), which is what makes the chart
  comparable over time; the cost is that a pool concentrated in one role draws several zero bars.
  Accepted: the total line names the pool, and a fully empty pool is replaced by the empty state
  rather than by a wall of zeros.
- **Product / instructor:** D2 remains open. If the brief's 113 and 108 were meant to describe
  two different populations, requirement 1's membership rule is the only thing to rewrite.

## Verdict

No blocking gap: AC-1 to AC-4 and DoV-1 to DoV-5 all Pass at rev 1, and the machine checks
above back each claim. The delta is fit to go to **G1**.

This review is not a Gate decision. G1 is signed by a human via `/sdd:g1-decision-record`; until
that record exists, `openspec/changes/add-headcount-charts/` is proposal + delta spec only.
