# Consistency lint — add-staffing-mvp (phase=p2)

- **Change:** `openspec/changes/add-staffing-mvp/`
- **Phase:** p2 (proposal ↔ delta spec)
- **Artifact level:** 2 — `proposal.md` + 6 delta specs; `tasks.md` and `design.md` absent
- **Timestamp:** 2026-11-11 (rev 11 run appended; earlier runs kept as history)
- **Verdict:** **Partial** after the eighth pass — 0 BLOCKER, 0 WARNING, 1 SUGGESTION open (L3,
  the capability name), plus the tooling cap. The seventh pass had found 3 BLOCKER, 5 WARNING
  and 4 SUGGESTION (L20–L31); all twelve were fixed in the eighth pass and re-checked. Earlier
  passes ended Partial with every finding fixed: the first found 0 BLOCKER, 2 WARNING,
  1 SUGGESTION and fixed both warnings in the same run (L3 left open); the second added L4–L6,
  the third (rev 8) L7–L10, the fourth (rev 9) L11–L13, the fifth (rev 10) L14–L16 and the
  sixth (rev 11) L17–L19, each fixed in its own pass
- **Cap:** the `openspec` CLI is unavailable, so no machine validation ran; per the checklist a
  manual-only run cannot reach Pass. The seventh pass does not depend on tooling: its blockers
  are read directly from the proposal and the six specs

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
| S3 (every requirement traces to What Changes) | Pass, then repaired in the second pass | Each of the 40 requirements carries a `**Traces:**` line; the six capabilities match the six "New capabilities" rows one to one. The first pass only checked that a Traces line exists, not that the quoted rule exists — L4 records the seven invented rule names that the shallow check missed |
| S4 (Given / When / Then) | Pass | All 90 scenarios use `**GIVEN**` / `**WHEN**` / `**THEN**` bullets |
| S5 (scenarios belong to a requirement) | Pass | No scenario sits outside a `### Requirement:` block |
| C1 (proposal What Changes covered) | Pass | 8 proposal steps map onto the six caps: step 1 → `people-pool`, step 2 → `people-import`, step 3 → `allocation`, step 4 → `post-requirement`, step 5 → `candidate-matching`, steps 6–8 → `staffing-confirmation` |
| C2 / T1–T3 | N/A | `tasks.md` does not exist (P2 stops before tasks) |
| p2-1 (no silent scope expansion or drop) | Pass (after second pass) | Two authoring slips were corrected: project status as informational was added **and** `proposal.md` step 3 plus `proposal.zh.md` amended in the same edit; the `post-requirement` rule that post dates must sit inside the project's dates was removed because no upstream artifact states it, and the question became open item D7 |
| p2-2 (delta internally consistent) | Pass | Actors are consistent across the six caps: only the lead confirms, only the coordinator recommends and searches, the lead cannot reach the suitable list, and the coordinator cannot edit a post |
| p2-3 (proposal scenarios have a home) | Pass | S1–S40 all referenced; the union of trace lists covers every scenario in REQ-001 §7 |
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

## Fourth pass (2026-11-11, after requirement revision 9)

Revision 9 removes the fit score, the weights and the ranking, and makes the suitable list a
name-ordered list of everyone who passes. Three findings, all fixed in the same pass.

| ID | Severity | Location | Issue | Resolution | Authority | Kind |
|---|---|---|---|---|---|---|
| L11 | BLOCKER (fixed) | `specs/staffing/candidate-matching/spec.md` and its `.zh.md` | The capability still required a 0–100 five-criterion score and a score-ordered ranking, while REQ-001 rev 9 and `proposal.md` now forbid both — the contract contradicted its own requirement | Score and ranking requirements deleted; one "Suitable people are listed by name, never scored" requirement with three scenarios replaces them; `ranked list` reads `suitable list` everywhere | spec | [fact] |
| L12 | WARNING (fixed) | `specs/staffing/staffing-confirmation/spec.md`, `post-requirement/spec.md`, `people-pool/spec.md` and the three `.zh.md` files | The recommendation snapshot still carried score and per-criterion contributions, a `bonus` row still "contributed to the fit score", and one pool scenario still said "ranked list" | Snapshot is now the gates passed plus the qualifying skills; `bonus` is informational only; the pool scenario says "suitable list" | spec | [fact] |
| L13 | WARNING (fixed) | `proposal.md`, `proposal.zh.md`, `dov-ac-review.md`, this file | Counts, weights and a risk statement still described the score (41 / 89 numbers, "the score weights are a policy choice", a score-recompute check) | Counts corrected to 40 requirements / 85 scenarios; the risk now names the gate thresholds; the score-recompute check is marked as rev 8 history | proposal | [fact] |

The Chinese translations were updated in the same pass and re-counted: 40 requirements, 85
scenarios and 40 `**Traces:**` lines in both languages, identical per file.

Counts after the fourth pass: 40 requirements / 85 scenarios — `people-pool` 8/17,
`people-import` 5/15, `allocation` 5/8, `post-requirement` 8/13, `candidate-matching` 5/14,
`staffing-confirmation` 9/18.

## Fifth pass (2026-11-11, after requirement revision 10)

Revision 10 makes an allocation a person × project record with no dates — being staffed locks
the person until the project ends — and gives the project lead a terminal end action that
releases the whole team. Three findings, all fixed in the same pass.

| ID | Severity | Location | Issue | Resolution | Authority | Kind |
|---|---|---|---|---|---|---|
| L14 | WARNING (fixed) | `specs/staffing/allocation/spec.md`, `candidate-matching/spec.md`, `staffing-confirmation/spec.md` and the three `.zh.md` files | Occupancy was still defined by overlapping date ranges: allocation stored start and end, G6 compared intervals, "Sequential postings stay allowed" let a second project win when the dates did not touch — directly contradicting REQ-001 rev 10 and `proposal.md` "Staffed means locked" | Allocation rewritten around person × project with no date range; G6 rewritten to "no active allocation in a project that has not ended"; the sequential-posting scenario replaced by "A second confirmation waits for the project to end"; `people-pool` profile and `people-import` wording no longer mention dates | spec | [fact] |
| L15 | WARNING (fixed) | `specs/staffing/allocation/spec.md`, `post-requirement/spec.md` and their `.zh.md` files | REQ-001 §6 and proposal step 9 describe one terminal project end that closes the project, cancels its posts, voids pending recommendations with `post cancelled` and deletes the allocations, but no delta requirement owned it — `post-requirement` even refused cancelling a post that still had a confirmed person | `allocation` gained "Ending a project is terminal and releases its whole team" with three scenarios (release + cancel + void, lead only, no reopen); `post-requirement` names the project end as the one exception to its cancel refusal | spec | [fact] |
| L16 | SUGGESTION (fixed) | `proposal.zh.md`, `specs/staffing/**/spec.zh.md`, `dov-ac-review.md` | Three Chinese files still said "排期占用" (schedule occupancy), which implies dates, and the AC/DoV counts still read 85 scenarios while the tree held 89 | Wording unified to "占用"; counts and per-capability figures corrected to 40 requirements / 89 scenarios in both languages | proposal | [fact] |

Counts after the fifth pass: 40 requirements / 89 scenarios — `allocation` 5/11,
`candidate-matching` 5/15, `people-import` 5/15, `people-pool` 8/17, `post-requirement` 8/13,
`staffing-confirmation` 9/18. The Chinese translations were re-counted and match per file.

## Sixth pass (2026-11-11, after requirement revision 11)

Revision 11 puts the dates back into G6 — an allocation covers the post's inclusive range and
only an intersecting range blocks — while keeping the rev 10 terminal project end. Three
findings, all fixed in the same pass.

| ID | Severity | Location | Issue | Resolution | Authority | Kind |
|---|---|---|---|---|---|---|
| L17 | BLOCKER (fixed) | `specs/staffing/allocation/spec.md`, `candidate-matching/spec.md`, `staffing-confirmation/spec.md` and the three `.zh.md` files | Occupancy was still defined project-wide with no dates: the allocation had no range, G6 read "no active allocation of the person", and a second confirmation waited for the project to end — directly contradicting REQ-001 rev 11 and `proposal.md` "An allocation occupies its dates" | Allocation rewritten as person × project × inclusive range; G6 rewritten as an intersection test closed on both ends; the project-end requirement kept; the profile lists project, start and end | spec | [fact] |
| L18 | WARNING (fixed) | `specs/staffing/staffing-confirmation/spec.md` and its `.zh.md` | "A second confirmation waits for the project to end" and "release makes the person available for any post, whatever its dates" contradicted the rev 11 allowance for disjoint assignments and overstated what a release returns | Requirement retitled "No two assignments cover the same day"; an intersecting confirmation is refused with `person taken` while two disjoint assignments are allowed; release opens the posts whose dates do not intersect the released range; the waiting scenario now works through a release and names the later start that would have needed none | spec | [fact] |
| L19 | SUGGESTION (fixed) | `specs/staffing/post-requirement/spec.md`, `people-pool/spec.md` and their `.zh.md`; `dov-ac-review.md` | Two specs still said the dates neither create nor bound an allocation and that the profile shows no date range — the rev 10 wording — and the review still counted 89 scenarios | `post-requirement` now makes the post dates the range of the allocation written on confirmation; `people-pool` lists every allocation whose end date is today or later with project, start and end; counts corrected to 40 / 88 | spec | [fact] |

Counts after the sixth pass: 40 requirements / 88 scenarios — `allocation` 5/10,
`candidate-matching` 5/15, `people-import` 5/15, `people-pool` 8/17, `post-requirement` 8/13,
`staffing-confirmation` 9/18. The Chinese translations were re-counted and match per file.

## Seventh pass (2026-11-11, review of the proposal and the six specs at rev 11)

Scope: a full read of `proposal.md` (297 lines) and the six English specs, with the Chinese
mirror checked at every finding site. Counts unchanged at 40 / 88. Twelve findings, none fixed
here: they change normative behaviour and need the product decision first.

| ID | Severity | Location | Issue | Suggested fix | Authority | Kind |
|---|---|---|---|---|---|---|
| L20 | BLOCKER | `specs/staffing/allocation/spec.md` L61–65 and `spec.zh.md` L56 | "The coordinator SHALL be able to change or remove any allocation at any time; removing one deletes the record without touching the project or its posts" also covers the allocation written by a confirmation. After such a removal the assignment and the confirmed post stay, G6 passes for intersecting posts, and two assignments cover the same day — contradicting "No two assignments cover the same day" and the all-or-nothing write-back | Limit hand edit and removal to allocations with source `manual`; a confirmation-sourced allocation goes away only through release or project end, which also remove the assignment. Add one scenario, and scope the proposal's fifth-risk sentence the same way | spec | [fact] |
| L21 | BLOCKER | `staffing-confirmation/spec.md` L93–97 and L45–50, `spec.zh.md` L74 and L38; `proposal.md` L74–77 and L142–144, `proposal.zh.md` L35 and L64 | Confirmation voids **every** other pending recommendation of the person with `person taken`, whatever the dates. Under rev 11 a person confirmed from January to March is free in May, so a May recommendation is still valid; voiding it contradicts "Two assignments with disjoint ranges SHALL be allowed" and the rule that a later post which does not overlap stays open | Void only the pending recommendations whose post dates intersect the written allocation and leave disjoint ones `proposed`. Update the key rule, the write-back requirement, the "other pending proposal" scenario (make `B` overlap) and add a disjoint case | spec + proposal | [fact] |
| L22 | BLOCKER | `post-requirement/spec.md` L11–13 against L124–129 and `spec.zh.md` L13; `people-pool/spec.md` L87–94 | A save is refused unless the role is an active catalog role, while deactivation never rewrites stored references and the title and `bonus` rows SHALL stay editable. A post whose role was deactivated therefore can never be saved, not even for the title change the lifecycle promises | Validate an active role on create; on edit accept the post's own stored role even when deactivated, and require an active role only when the role changes. Add one scenario | spec | [fact] |
| L23 | WARNING | `people-pool/spec.md` L188–191 and `spec.zh.md` L160; `allocation/spec.md` L61–65; `proposal.md` L284–287 | The profile lists only allocations whose end date is today or later, while G6 reads every allocation and the coordinator may remove any. A past allocation blocks a backdated post but is invisible, and the proposal's mitigation claims "the profile lists every allocation with its dates" | List past allocations in a separate collapsed group, or state that expired ones are hidden and not editable. Re-word the mitigation to match | spec | [fact] |
| L24 | WARNING | `staffing-confirmation/spec.md` L145–152 and `spec.zh.md` L111–117 | The GIVEN has the person staffed on `Atlas` and already recommended on intersecting `Borealis` dates, but the recommend requirement refuses anyone who fails G6, so the state cannot be produced by the specified actions as written | Add "the `Atlas` allocation was recorded after the `Borealis` recommendation" to the GIVEN in both languages, matching the sibling scenario "An allocation recorded after the proposal" | spec | [inference] |
| L25 | WARNING | `allocation/spec.md` L32; `post-requirement/spec.md` L78–83 and L140–147 | No requirement defines a `cancelled` post's editability. The field lock applies only while a person is confirmed, so after a project ends the lead can still change the role, dates or `required` rows of a cancelled post in a closed project | State that a `cancelled` post is read-only and that such edits are refused like a locked post | spec | [inference] |
| L26 | WARNING | `proposal.md` L262; `proposal.zh.md` L136 | Open item D8 still promises a re-check of "active and schedule" / "在职与排期两条门槛"; rev 11 replaced the schedule lock with the date intersection | Rename to "active and date overlap" / "在职与占用日期相交" | proposal | [fact] |
| L27 | WARNING | `allocation/spec.md` L84–91; `proposal.md` L54–58 | "Overlapping allocations are accepted at entry" traces proposal step 3, which never says an overlap is stored — only the fifth risk note does, so the rule has no home in What Changes | Add one clause to step 3 (an overlap is stored as a fact, not refused) or re-point the trace | proposal | [fact] |
| L28 | SUGGESTION | Traces in `post-requirement` L85, `people-pool` L55–56, `people-import` L45, `candidate-matching` L64 and the `.zh.md` mirrors | Five quoted rule names start lower-case while the proposal capitalises the sentence ("post lifecycle", "every skill belongs…", "an import is all-or-nothing", "no score, no ranking", "catalog items…"), so they are not literal substrings | Quote each sentence as written, or state that rule quotes are case-insensitive | spec | [fact] |
| L29 | SUGGESTION | `candidate-matching/spec.md` L109–111 | The fallback trigger enumerates G1, G3, G4, G5 and G6, omitting G2; the omission is logical because the fallback searches other roles, but it reads as if G2 were not required | Say "no person of the post's role passes every gate" and note that G2 fails by construction | spec | [inference] |
| L30 | SUGGESTION | `people-import/spec.md` L25–35 against L37–43; `proposal.md` L45–53 | A structurally invalid workbook (one sheet, headers only) is refused with no batch, while a row failure records a `rejected` batch; the proposal describes only the second, so batch history has two different "not applied" outcomes | Distinguish "upload refused, no batch" from "batch rejected" in the spec and in proposal step 2 | spec | [inference] |
| L31 | SUGGESTION | `staffing-confirmation/spec.md` L175–180; `proposal.md` L160–164 | Release is open to the lead or the coordinator in the spec, but the proposal's key rule names no actor | Name the actor in the proposal key rule | proposal | [inference] |

Counts after the seventh pass: 40 requirements / 88 scenarios, unchanged. The Chinese files match
the English ones at every finding above.

## Eighth pass (2026-11-11, resolution of L20–L31)

The user asked for the findings to be fixed before G1. All twelve were closed in both languages;
two scenarios were added (`allocation`, `post-requirement`) and one existing scenario grew a
disjoint case, so the tree now holds 40 requirements / 90 scenarios.

| ID | Severity | Fix applied | Location |
|---|---|---|---|
| L20 | BLOCKER → fixed | Hand edit and removal are limited to `source=manual`; a confirmation-sourced allocation goes away only through release or project end, and a new scenario refuses the hand removal. Proposal fifth risk and the allocation sentence now agree | `allocation/spec.md` + `.zh.md` |
| L21 | BLOCKER → fixed | Voiding is date-scoped in three places — the proposal step, the proposal key rule, and the write-back requirement; the scenario now has `A`/`B` intersecting and `C` disjoint, and `C` stays `proposed` | `staffing-confirmation/spec.md` + `.zh.md`; `proposal.md` + `.zh.md` |
| L22 | BLOCKER → fixed | The active-role check runs on creation and on a role change only; a post keeps saving with its stored deactivated role. A new scenario covers the title edit and the refused switch to another deactivated role | `post-requirement/spec.md` + `.zh.md` |
| L23 | WARNING → fixed | The profile lists every allocation, current or past, and marks ended ones; the scenario now carries a past `Borealis` row and the proposal mitigation matches | `people-pool/spec.md` + `.zh.md`; `proposal.md` + `.zh.md` |
| L24 | WARNING → fixed | The scenario's GIVEN now states that the `Atlas` allocation was recorded after the `Borealis` recommendation | `staffing-confirmation/spec.md` + `.zh.md` |
| L25 | WARNING → fixed | A `cancelled` post is read-only: no field, headcount or skill row may change | `post-requirement/spec.md` + `.zh.md` |
| L26 | WARNING → fixed | D8 now promises a re-check of "active status and date overlap" / "在职与占用日期相交" | `proposal.md` + `.zh.md` |
| L27 | WARNING → fixed | Proposal step 3 stores an overlap as a fact rather than refusing it, so the allocation trace has a home in What Changes | `proposal.md` + `.zh.md` |
| L28 | SUGGESTION → fixed | Eight rule quotes now match the proposal's capitalisation (post lifecycle, every skill belongs…, catalog items…, person master data…, three ordered band scales, an import is all-or-nothing, no score no ranking, cross-role fallback…, a confirmed person…), verified case-sensitively | all six specs, both languages |
| L29 | SUGGESTION → fixed | The fallback trigger reads "no person of the post's role is suitable — every person of that role fails at least one of G1, G3, G4, G5 or G6" | `candidate-matching/spec.md` + `.zh.md` |
| L30 | SUGGESTION → fixed | A structurally unusable upload records no batch, distinct from the `rejected` batch a bad row produces; proposal step 2 says the same | `people-import/spec.md` + `.zh.md`; `proposal.md` + `.zh.md` |
| L31 | SUGGESTION → fixed | The post-lifecycle key rule names the actor: releasing — by the lead or the coordinator | `proposal.md` + `.zh.md` |

Re-check after the fixes: 40 requirements / 90 scenarios in both languages, per file identical
(`allocation` 5/11, `candidate-matching` 5/15, `people-import` 5/15, `people-pool` 8/17,
`post-requirement` 8/14, `staffing-confirmation` 9/18); 40 of 40 requirements traced; 90 of 90
scenarios carry GIVEN / WHEN / THEN with no duplicate title; 11 of 11 distinct rule quotes match
`proposal.md` case-sensitively.

## Residual risks

- The delta is hand-authored without machine validation: a typo in a capability directory name
  would only surface when `openspec` tooling is installed and the change is archived.
- The suitable list is exhaustive by design, so a large pool produces a long list. The MVP
  accepts that; filtering or paging is a UI decision and must not become a hidden ranking.
- The role skill domain is catalog configuration that feeds matching: moving a skill to another
  domain changes who the fallback can reach without touching any person record, so the catalog
  surface must show the domain it is about to change.
- No score exists to compare sections with; if a future change reintroduces one, it is a spec
  change and needs its own G1 rather than a tweak inside the confirmation screen.
- The project end is as coarse as the project: one end action cancels every post, filled ones
  included, and cannot be undone. Since rev 11 the dates, not the project, are what block a person,
  so the end is a convenience rather than the only way to free someone. Accepted for the MVP;
  finer granularity is a later change.
- The seventh pass found three contradictions the earlier passes did not look for, because they
only appear once the rev 11 dates meet actions written for rev 10. All three are fixed in the
eighth pass; the lesson is recorded here so a later change re-checks the paths that touch an
allocation's lifetime, not only the gate that reads it.

## Reviewer metadata

- Agent run, 2026-11-11 (rev 11). The seventh pass was requested by the user as a review of the
  proposal and the six specs; all of its findings were fixed in the eighth pass and re-checked, and
  the human G1 decision followed the same day. This report is not a Gate decision and is not Living
  Spec authority.

## Next

L20–L31 are resolved (eighth pass); only L3, a naming suggestion, stays open. G1 was signed on
**rev 11** on 2026-11-11 (IO raylen, SA kyrie; `g1-decision.md`, appended below the untouched rev 8
record), so the next step is P3 — ATDD design and task planning. A later rewrite of the delta needs
a new G1 record.
