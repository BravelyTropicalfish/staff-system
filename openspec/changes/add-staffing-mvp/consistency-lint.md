# Consistency lint — add-staffing-mvp (phase=p2)

- **Change:** `openspec/changes/add-staffing-mvp/`
- **Phase:** p2 (proposal ↔ delta spec)
- **Artifact level:** 2 — `proposal.md` + 6 delta specs; `tasks.md` and `design.md` absent
- **Timestamp:** 2026-11-10
- **Verdict:** **Partial** — 0 BLOCKER, 2 WARNING, 1 SUGGESTION, 2 N/A as found; both warnings
  were fixed inside this run (see "Resolved during this run"), leaving L3 open. A second pass
  added L4–L6, three further findings, all fixed (see "Second pass"); a third pass after
  requirement revision 8 added L7–L10, all fixed (see "Third pass")
- **Cap:** the `openspec` CLI is unavailable, so no machine validation ran; per the checklist a
  manual-only run is capped at Partial even with no blocking finding

## Findings

| ID | Severity | Location | Issue | Suggested fix | Authority | Kind |
|---|---|---|---|---|---|---|
| L1 | WARNING | `specs/staffing/staffing-confirmation/spec.md` → "Every void carries a reason and reaches the coordinator" | The notice list is required but its lifetime and acknowledgement behaviour are not: nothing says whether entries persist, can be dismissed, or expire | State the retention in the requirement, or defer it explicitly in REQ-001 §10 | spec | [inference] |
| L2 | WARNING | `requirement-workspace/requirements/REQ-001-staffing-mvp.md` §6, "Default weights are a policy starting point; they are frozen in the delta spec at G1" | The delta now fixes the weights; the sentence still points at a gate that has not been signed, so a reader can think the values are provisional | Change to "frozen in `specs/staffing/candidate-matching/spec.md`" | proposal/spec | [fact] |
| L3 | SUGGESTION | `specs/staffing/allocation/spec.md` | The capability mixes project maintenance with allocation records, so its name under-describes its scope | Keep for the MVP; rename to `staffing/projects-and-allocations` only if the capability is ever split | spec | [inference] |

## Checks that passed

| Rule | Result | Evidence |
|---|---|---|
| P1–P4 (proposal shape) | Pass | `## Why`, `## What Changes`, non-goals, three-dimensional Impact all present in `proposal.md` |
| S1 (spec structure) | Pass | Six files under `changes/add-staffing-mvp/specs/<capability>/spec.md`, each opening with `## ADDED Requirements`; no `MODIFIED` / `REMOVED` because `openspec/specs/` is empty |
| S2 / S2b / S2c (citation prefixes) | N/A | The delta uses plain `**Traces:**` references and no `@knowledge:` / `@rule:` prefixes |
| S3 (every requirement traces to What Changes) | Pass, then repaired in the second pass | Each of the 41 requirements carries a `**Traces:**` line; the six capabilities match the six "New capabilities" rows one to one. The first pass only checked that a Traces line exists, not that the quoted rule exists — L4 records the seven invented rule names that the shallow check missed |
| S4 (Given / When / Then) | Pass | All 89 scenarios use `**GIVEN**` / `**WHEN**` / `**THEN**` bullets |
| S5 (scenarios belong to a requirement) | Pass | No scenario sits outside a `### Requirement:` block |
| C1 (proposal What Changes covered) | Pass | 8 proposal steps map onto the six caps: step 1 → `people-pool`, step 2 → `people-import`, step 3 → `allocation`, step 4 → `post-requirement`, step 5 → `candidate-matching`, steps 6–8 → `staffing-confirmation` |
| C2 / T1–T3 | N/A | `tasks.md` does not exist (P2 stops before tasks) |
| p2-1 (no silent scope expansion or drop) | Pass (after second pass) | Two authoring slips were corrected: project status as informational was added **and** `proposal.md` step 3 plus `proposal.zh.md` amended in the same edit; the `post-requirement` rule that post dates must sit inside the project's dates was removed because no upstream artifact states it, and the question became open item D7 |
| p2-2 (delta internally consistent) | Pass | Actors are consistent across the six caps: only the lead confirms, only the coordinator recommends and searches, the lead cannot reach the ranking, and the coordinator cannot edit a post |
| p2-3 (proposal scenarios have a home) | Pass | S1–S38 all referenced; the union of trace lists covers every scenario in REQ-001 §7 |
| p2-5 (unmatched requirements) | Pass | No requirement lacks a proposal step or a REQ-001 anchor |
| Vague acceptance criteria | Pass | No `correctly` / `properly` / `quickly` / `appropriately`; scores, dates, counts and band names are numerical or enumerated |

## N/A and skipped

- **C2 / T-rules:** no `tasks.md` — not created in P2 by design.
- **D-rules:** no `design.md`.
- **`openspec validate`:** CLI absent; structure was checked by hand against
  `openspec/README.md` and `openspec/templates/change-spec.md`.

## Fix order

1. **L1** — decide notice-list retention before G1, or record it in REQ-001 §10 as deferred.
2. **L2** — one-line correction in REQ-001 §6 so the weights read as settled.
3. **L3** — optional naming hygiene; no action needed for the MVP.

## Resolved during this run

- **L1** fixed in `specs/staffing/staffing-confirmation/spec.md`: an entry "SHALL remain in the
  list until the coordinator dismisses it, and SHALL NOT expire on its own", with a scenario for
  a 90-day-old entry.
- **L2** fixed in `REQ-001-staffing-mvp.md` §6: the weights now point at
  `specs/staffing/candidate-matching/` instead of an unsigned gate.
- **L3** left open on purpose: renaming a capability would churn the tree for no behavioural
  gain in the MVP.

## Second pass (2026-11-10)

A review of the artifacts against each other produced three more findings. All are fixed.

| ID | Severity | Location | Issue | Resolution | Authority | Kind |
|---|---|---|---|---|---|---|
| L4 | WARNING | all six specs, `**Traces:**` lines | Seven requirements quoted `key rule "…"` names that `proposal.md` never uses: "six hard gates, first failure explains the exclusion", "five weighted criteria", "occupancy is a date range" (twice), "status is derived", "a post locks once someone is confirmed", "confirmation is the only write path", "no double booking", "no silent loss" | Each replaced with the proposal's own wording or the step / REQ section it really comes from; the remaining seven quotations were verified as literal substrings of `proposal.md` | proposal | [fact] |
| L5 | WARNING | `specs/staffing/post-requirement/spec.md` | "the range lies inside the project's own date range" had no basis in REQ-001 or the proposal, and would refuse a post that legitimately runs past the project | Clause and its scenario removed; `proposal.md` gained open item **D7** so the gap is visible instead of invented | spec | [fact] |
| L6 | SUGGESTION | `specs/staffing/candidate-matching/spec.md`, `specs/staffing/people-pool/spec.md` | "Contributions add up to the score" assumed the 89.5 it was meant to prove, and `people-pool` "A person holds exactly one role" asserted an import outcome its WHEN never exercised | First now derives 24.5 / 15 / 20 / 20 / 10 from the inputs; the import half moved to `people-import` as its own scenario | spec | [inference] |

Counts stayed at 39 requirements / 78 scenarios: one scenario left `post-requirement`, one
joined `people-import`.

## Third pass (2026-11-10, after requirement revision 8)

Revision 8 added a cross-role fallback tier to REQ-001 §6.1 and a role skill domain to the
skill catalog. Four findings, all fixed in the same pass.

| ID | Severity | Location | Issue | Resolution | Authority | Kind |
|---|---|---|---|---|---|---|
| L7 | WARNING | `specs/staffing/candidate-matching/spec.md` → “Gates are evaluated in a fixed order” | Read alone, the gate requirement still said a person failing G2 never appears in "the ranked list", so the new tier could look like a relaxation of G2 | Added "G2 SHALL NOT be relaxed anywhere… never a same-role candidate" plus the §6.1 trace, so the fallback is a separate list rather than a gate exemption | spec | [fact] |
| L8 | WARNING | `specs/staffing/people-pool/spec.md` → “Skill catalog” | `REQ-001` §5, §6 and `proposal.md` carried a per-skill role domain, but no delta requirement owned it, so X2 had no data contract | New requirement “Every skill belongs to exactly one role domain” with the seeded mapping, the empty-domain rule and change-affects-future-searches-only | spec | [fact] |
| L9 | SUGGESTION | `specs/staffing/candidate-matching/spec.md` → “Exclusions are reported, never ranked” | An empty cross-role section had no explanation path, so S37 would show nothing instead of why | Exclusion list now also counts X1 and X2 rejections | spec | [inference] |
| L10 | WARNING | `specs/staffing/staffing-confirmation/spec.md` → “The coordinator recommends a ranked candidate” | The snapshot contract listed score and reasons only, while §6.1 promises the fallback evidence travels with the recommendation | Snapshot now carries the person's role, the band gap and the qualifying domain skill | spec | [fact] |

One correction went the other way: a draft scenario asserted that a cross-role person never
outranks a same-role person on the same screen, which the trigger makes impossible (the fallback
only opens on an empty same-role list). The scenario was deleted and the rule kept as a
normative sentence, because the exclusivity is the stronger guarantee.

Counts after the third pass: 41 requirements / 89 scenarios — `people-pool` 8/17,
`candidate-matching` 6/18, `staffing-confirmation` 9/18; `allocation` 5/8,
`people-import` 5/15, `post-requirement` 8/13 unchanged.

## Residual risks

- The delta is hand-authored without machine validation: a typo in a capability directory name
  would only surface when `openspec` tooling is installed and the change is archived.
- The score formula is now fixed in the delta, so a later change of weights becomes a spec
  change rather than a configuration tweak.
- The role skill domain is catalog configuration that feeds matching: moving a skill to another
  domain changes who the fallback can reach without touching any person record, so the catalog
  surface must show the domain it is about to change.
- The two sections share a 0–100 scale but are never compared; if a future UI ever sorts them
  together, the fallback tier silently becomes a ranking change and needs its own decision.
- No known contradiction between the six capabilities at the time of this run.

## Reviewer metadata

- Agent run, 2026-11-10. Human sign-off pending. This report is not a Gate decision and is not
  Living Spec authority.

## Next

`/sdd:g1-decision-record` (human signs), or continue `/sdd:p2` after fixing L1 and L2.
