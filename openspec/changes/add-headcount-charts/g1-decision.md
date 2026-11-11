# G1 decision — add-headcount-charts

- **Gate:** G1 (Align)
- **Change id:** `add-headcount-charts`
- **Decision:** **Pass**
- **Approvers:** IO = cecil; SA = kyrie
- **Timestamp:** 2026-11-11 21:19 (Asia/Shanghai)

The decision belongs to the two named approvers. This file records what they decided; the
agent wrote it on their instruction and issues no approval of its own.

## Input artifacts

| Artifact | Path | Result |
|---|---|---|
| Proposal (English / Chinese) | `proposal.md`, `proposal.zh.md` | 5 What-Changes steps, 1 ADDED capability `staffing/headcount-report`, key rules, non-goals, assumptions, D1–D4 |
| Delta spec — `headcount-report` | `specs/staffing/headcount-report/spec.md` | 6 requirements, 12 scenarios, every requirement carrying a `**Traces:**` line |
| Delta spec, Chinese | `specs/staffing/headcount-report/spec.zh.md` | identical counts (6 / 12 / 6), structure keywords kept |
| AC / DoV review | `dov-ac-review.md` | AC-1–AC-4 and DoV-1–DoV-5 all Pass, machine checks listed |
| Source requirement | `requirement-workspace/requirements/REQ-002-headcount-charts.md` (rev 2) | S1–S12 and B-1–B-14 traced; 0 blocking open questions; DoR base 8 + deep-dive ⑨–⑫ resolved, 0 BLOCKER |
| Impact brief | `requirement-workspace/analysis/impact-brief-REQ-002-headcount-charts.md` | as-is inventory with measured counts, risk table R1–R9, "no split blocker" |
| P1 DoV | `docs/ai4se-plugins/20261111-211144-quality-checklist-report.md` | p1 full-stage **Pass** (P1-1…P1-4 required all Pass) |
| Review aid | `prototype/` | static clickable mock, three states (coordinator / empty pool / project lead) |

Totals re-counted before this decision: **6 requirements / 12 scenarios**, 6/6 `**Traces:**`
lines, 12/12 GIVEN/WHEN/THEN, S1–S12 each referenced exactly once, and **8/8** quoted rule names
present literally in `proposal.md`. Every count came from a script run against the files, not
from reading them.

## Findings

- **Scope.** The single delta capability matches the single "New capabilities" row of
  `proposal.md`, and the five What-Changes steps map onto the six requirements (step 5 maps onto
  two: the chart section and its empty state). No `MODIFIED` or `REMOVED` section — nothing
  outside this capability changes, and the person table's behaviour is untouched (the scenario
  only pins what it already does). Two scope changes were settled before this gate and both
  rippled through every artifact: the charts are a section of the existing `/people` screen
  rather than a screen with its own route, and a category with nobody keeps its bar (rev 2
  reversed rev 1's "non-empty only"). Both are recorded in `dov-ac-review.md` under
  "Corrections applied while writing the delta".
- **Ambiguity.** REQ-002 §11 carries **0 blocking** open questions; D1–D4 are deferred with an
  owner each. Three of them are outside the delta (a KPI tile, the charting mechanism, a future
  filter). **D2** is the only item with an external dependency: the brief's two illustrations
  show different totals (113 and 108) because each is a separate sample, and if the instructor
  meant two different populations then one rule changes. Inside the change the question is
  closed — requirement 1 pins one pool, so the two totals are equal by construction, and S1
  fails if they ever disagree. No rule is therefore left undecided, which is why this gate can
  be Pass rather than Conditional.
- **Conflict.** One internal contradiction was found and removed while the delta was written:
  "every category keeps its bar at zero" and "a deactivated role leaves the axis" cannot both be
  read literally. They are reconciled by defining the axis once — every active catalog role
  **plus** every role still holding an active person — and pinning both sides (B-3b, and the
  second THEN of "A deactivated role keeps its active people counted"). A second collision,
  between zero bars and the empty state, is resolved by giving the empty state precedence, since
  a chart whose tallest bar is zero has no scale to draw against. No cross-capability conflict
  exists: the change adds one capability and reads no other.
- **Traceability.** S1–S12 are each referenced exactly once across the six requirements, and
  every requirement names its proposal step or key rule, its REQ-002 section and its boundaries.
  The eight quoted rule names are literal substrings of `proposal.md`.
- **Tooling limitation, stated rather than hidden.** The `openspec` CLI is not installed on this
  machine (`openspec --version` fails), so `openspec validate` never ran and the change package
  was hand-authored into the documented layout (`changes/<id>/specs/<capability>/spec.md`,
  `## ADDED Requirements`). This gate therefore rests on the script checks above rather than on a
  CLI verdict. If the CLI later reports a structural error, the package is at fault and this
  record must be revisited.

## Conditions + owners

None. The decision is **Pass**, so no remediation is owed at this gate.

## Open risks accepted with this decision

| # | Risk | Handling |
|---|---|---|
| D2 | The brief's two totals differ; if two populations were intended, the membership rule is wrong | The rule lives in one place (requirement 1 and REQ-002 §5). Rewriting it re-opens this gate; nothing else in the delta moves |
| D1 | No KPI tile for the total on the shell | Out of this change; the total sits above each chart |
| D3 | The mechanism that draws the bars is undecided | P3 decides; the requirement fixes proportionality and labelling only |
| D4 | No project or date filter | Out of scope; a filter changes the membership rule and is its own requirement |
| R3 | A chart made mostly of zero bars can read as empty | The total line names the pool, and the empty state replaces both charts once nobody is active |
| Coverage | The 100 % line and branch coverage gate applies to the new `domain*` / `api*` classes | The zero bar and the empty pool are the two branches a reviewer will look for, so P4 must ship tests for them rather than an exclusion |
| SA constraint | "A lead sees no section and requests nothing" needs the role before the fetch | P3's design must read the session (or reuse the nav's role) before requesting the report; rendering first and hiding on 403 would fail S9 |

## Next

- **P3:** `/sdd:atdd-design`, then `/sdd:task-plan` — the change has no `design.md` or `tasks.md`
  yet, and the delta spec above is the contract those artifacts must satisfy.
- This record authorizes nothing beyond that: merge and deploy belong to G2, and the archive to
  G3. Neither is implied here.
- A later change to the membership rule (active only), the axis definition, the share rounding or
  the coordinator-only read re-opens G1 with a new record rather than an edit to this one.
