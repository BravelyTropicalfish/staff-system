# AC / DoV review — add-staffing-mvp (P2)

- **Change:** `openspec/changes/add-staffing-mvp/`
- **Phase:** P2 / `step=all` (generate + review)
- **Artifact level:** 2 (proposal + delta specs; no `design.md`, no `tasks.md` by instruction)
- **Delta specs:** 6 capabilities, 40 requirements, 90 scenarios
- **Tooling:** `openspec` CLI is not installed, so `openspec validate` could not run; the tree
  was hand-authored to the documented layout (`changes/<id>/specs/<capability>/spec.md`,
  `## ADDED Requirements`)
- **Date:** 2026-11-11 (rev 11: an allocation covers the post's inclusive dates, so only an
  intersecting range blocks; the project end still releases the whole team). Re-checked after the
  seventh lint pass: voiding is date-scoped, a confirmation-sourced allocation is not removable
  by hand, and a deactivated role no longer freezes the post

## Acceptance criteria

| Item | Result | Evidence | Notes |
|---|---|---|---|
| AC-1 Every requirement has at least one testable AC | Pass | 40 `### Requirement:` blocks vs 90 `#### Scenario:` blocks under `specs/staffing/*/spec.md` | Minimum 2 scenarios per requirement; `people-pool` 8/17, `people-import` 5/15, `allocation` 5/11, `post-requirement` 8/14, `candidate-matching` 5/15, `staffing-confirmation` 9/18 |
| AC-2 ACs are observable | Pass (rev 9) | `specs/staffing/candidate-matching/spec.md` "Everyone who passes is listed, with no score" asserts three listed people and no score, grade or rank on any row; "The order comes from the name, not the fit" asserts name order despite a larger skill surplus on the later name; `specs/staffing/people-import/spec.md` "One bad row blocks the whole file" asserts batch `rejected` plus zero data change | Every scenario asserts a state, a count, a stored value or a refusal with a named reason; since rev 9 no scenario depends on a computed score |
| AC-3 Negative and edge cases are called out | Pass | Boundary: `allocation` "A boundary day still blocks", "A later post stays open", "A closed project cannot be reopened"; `post-requirement` "Dates cannot move under a confirmed person", "Cancel is refused, then allowed"; `people-import` orphan row, duplicated key, empty workbook; `staffing-confirmation` "A concurrent double confirmation", "A later posting succeeds, an overlapping one waits"; `candidate-matching` "A person at the required band is not above it", "A skill outside the post role's domain never admits", "The top band has no cross-role candidate" | Negative coverage holds: the fallback cases sit alongside the earlier ones, so every tier has a refusal case rather than only a happy path; rev 10 added the project-end refusals and rev 11 restores the inclusive boundary cases on both ends of the range |
| AC-4 No disguised implementation detail, no vague wording | Pass (after review fix) | `specs/staffing/staffing-confirmation/spec.md` now states "apply every effect below together, or none of them" instead of the earlier "in one transaction" | The atomicity is the requirement; the mechanism word was removed so `design.md` owns it. No vague adjectives (`correctly`, `quickly`, `appropriately`) remain; REQ-001 §6 now states a binary gate result and a name order, so no comparative "slightly more / less" wording survives anywhere |

## Definition of verified

| Item | Result | Evidence | Notes |
|---|---|---|---|
| DoV-1 Scope matches the proposal, no silent expansion | Pass (after review fix) | 6 delta capabilities vs 6 rows of `proposal.md` "New capabilities"; no `MODIFIED` / `REMOVED` section because `openspec/specs/` is empty | `allocation` also fixes project status as informational; `proposal.md` step 3 was amended in the same pass so the two artifacts agree. One over-reach was removed in the second pass: `post-requirement` had required post dates to sit inside the project's dates, which no upstream artifact says, so the clause and its scenario went and the question became open item **D7** |
| DoV-2 Ambiguities listed or resolved | Pass | REQ-001 §11 B1–B19 resolved; D1–D6 deferred in REQ-001, D7–D8 recorded in `proposal.md`; every deferred item is outside the delta (`D6` = removing a person's wrong skill, `D7` = post dates vs project dates, `D8` = capping the cross-role section or requiring an extra approval) | The delta pins no scoring numbers since rev 9: the fixed values are the six gates, the one-band fallback step, the single-role rule and the name-order rule. Rev 10 pins the project end (one terminal action releasing the whole team); rev 11 restores the inclusive date comparison on top of that end, so a person may hold allocations that do not intersect |
| DoV-3 Role concerns noted (IO / SA) | Pass | "Residual concerns" below | None blocks the gate; seven items are listed for IO / SA, including the snapshot wording, the concurrency guard and the coarse project end |
| DoV-4 No business implementation claimed | Pass | No code, no `tasks.md`, no `design.md` in the change folder | P2 only shapes the contract |
| DoV-5 Requirements trace to the proposal / approved requirements | Pass (after review fix) | Every requirement carries a `**Traces:**` line; S1–S40 all referenced across the six specs; the `key rule "…"` quotations were re-checked as literal substrings of `proposal.md` | Trace union covers S1–S40; each capability's traces name its proposal step and its REQ-001 section. The first pass had invented rule names (`"no double booking"`, `"confirmation is the only write path"`, `"five weighted criteria"`, …) that do not appear in the proposal; all are now quotations of existing text, including `"An allocation occupies its dates"` and `"Ending a project"` at rev 11, while the rev 10 name `"Staffed means locked"` was removed together with the lock |

## Corrections applied while writing the delta

> The two scoring corrections below are history: rev 9 deleted the fit score outright, so no
> formula, weight or renormalisation rule remains in any spec.

- **Required-skill criterion.** The rev 6 formula `min(surplus, 2) / 2` gave a candidate who met
every requirement exactly **0 of 35** — roughly 59 overall — because it only counted surplus.
Writing a testable AC made that visible, so each row now scores
`0.7 + 0.3 × min(surplus, 2) / 2`: meeting the minimum earns 70 % of the row, surplus earns the
rest. The criterion is renamed to "required-skill fit" and REQ-001 §6, `proposal.md` and
`proposal.zh.md` were updated with the delta.
- **Write-back wording.** "In one transaction" became "apply every effect below together, or
none of them", so `design.md` owns the mechanism.
- **Notice-list retention.** An entry now explicitly stays until the coordinator dismisses it.
- **One role per person, now fixed upstream too (rev 7).** The model said "role reference" and
  the import rule only said "must be an active catalog role", so a `role` cell reading
  `DEV, QA` had no defined outcome. The rule now lives in REQ-001 §5 (Person row and the Role
  vocabulary entry), §5.1 (Validity) and G2, and in `proposal.md` / `proposal.zh.md` as the key
  rule "one person, one role". In the delta, `people-pool` states the role is a single
  reference never a set and backs it with "A person holds exactly one role"; `people-import`
  rejects a multi-role cell as a row error instead of splitting it, in its own scenario.
- **Invented trace citations.** Seven requirements cited `key rule "…"` names that the
  proposal never used. Each was replaced with either the proposal's own wording or the step /
  REQ section it really comes from; the seven quotations are now literal substrings of
  `proposal.md` (checked mechanically).
- **Post dates vs project dates.** `post-requirement` had quietly required a post's range to
  sit inside the project's range; nothing upstream says so and it would refuse legitimate
  posts, so the clause and its scenario were removed and the question is now open item D7.
- **Circular AC.** "Contributions add up to the score" assumed the 89.5 it was supposed to
  prove; it now starts from the post and candidate inputs and derives 24.5 / 15 / 20 / 20 / 10.
- **One scenario, two subjects.** `people-pool` "A person holds exactly one role" asserted an
  import outcome its WHEN never exercised; the import half moved to
  `people-import` "A multi-role cell is a row error, never two roles".
- **Cross-role fallback (rev 8).** The new tier needed one correction while it was written: a
  draft scenario had a cross-role person outranked by a same-role person on one screen, which
  cannot happen because the trigger makes the two lists mutually exclusive. The scenario was
  dropped and the rule stayed as a normative sentence, since the exclusivity is the stronger
  guarantee. Cross-role scoring also had to be stated explicitly — a `required` row they do not
  hold contributes 0, and the two sections are never compared — otherwise the renormalisation
  rule would have made their scores read as comparable to a same-role score.

## Second and third pass verification (rev 8, before G1)

> Kept as history. Rev 9 removed every scoring row; the gate and fallback rows still hold.

| Check | How | Result |
|---|---|---|
| Requirement / scenario counts | counted `### Requirement:` and `#### Scenario:` per file | 41 / 89 at rev 8 (rev 9: 40 / 85; rev 10: 40 / 89; rev 11: 40 / 88, then 40 / 90 after the review fixes), per capability 8/17, 5/15, 5/8, 8/13, 6/18, 9/18 |
| Every requirement traced | scanned each requirement block for a `**Traces:**` line | 41 of 41 |
| Every scenario given/when/then | parsed each scenario block for GIVEN / WHEN / THEN | 89 of 89, no duplicate titles across the six files |
| Citations resolve | extracted every `key rule "…"` and searched `proposal.md` case-sensitively | 11 of 11 match literally |
| Scenario coverage | searched the six files for `S1`–`S38` | all 38 present |
| Scores recompute (rev 8 only) | applied the stated formulas to the scenario inputs | 24.5 / 87.6, 74.5 over 85; 89.5; 29.75; 71.5 all consistent at the time; the whole criterion is deleted at rev 9 |
| Fallback tier closure | checked the trigger, X1, X2 and the two stand-ins against REQ-001 §6.1 | trigger = empty same-role list; X1 = required rank + 1; X2 = domain skill ≥ `Practitioner`; G1 / G5 / G6 still applied; X1 replaces G3, X2 replaces G2 + G4; empty domain never degrades to "any skill" |
| Domain mapping is single-valued | read the skill catalog requirement and the five seeded roles | each seeded skill appears under exactly one role, and a role added later starts with an empty domain |

## Rev 9 re-verification (no scoring)

| Check | How | Result |
|---|---|---|
| Requirement / scenario counts | counted `### Requirement:` and `#### Scenario:` per file | 40 / 85; per capability 8/17, 5/15, 5/8, 8/13, 5/14, 9/18 |
| Chinese translation parity | counted requirements, scenarios and `**Traces:**` lines in each `spec.zh.md` | 40 / 85 / 40 in both languages, per file identical |
| Every requirement traced | scanned each requirement block for a `**Traces:**` line | 40 of 40 |
| Every scenario given/when/then | parsed each scenario block for GIVEN / WHEN / THEN | 85 of 85, no duplicate titles across the six files |
| Scoring removed | searched the six English specs for `score`, `ranking`, `ranked` | only the prohibition sentences remain ("never scored", "no score"); no formula, weight or 0–100 value survives |
| Order is stated | read the suitable-list requirement and its scenario | name ascending, then employee number; the scenario puts the later name first despite a larger surplus |
| Bonus is informational | read the post-requirement and candidate-matching requirements | `bonus` affects neither eligibility nor order; the shortfall is displayed only |
| Fallback tier closure | re-checked trigger, X1, X2 and display against REQ-001 §6.1 | trigger = empty same-role suitable list; X1 = required rank + 1; X2 = domain skill ≥ `Practitioner`; the section is name-ordered like the main list and carries no score |
| Recommendation snapshot | read the staffing-confirmation requirement | the snapshot holds the gates passed and the qualifying skills; no score field remains |

## Rev 10 re-verification (project-wide lock, project end)

| Check | How | Result |
|---|---|---|
| Requirement / scenario counts | counted `### Requirement:` and `#### Scenario:` per file | 40 / 89; per capability `allocation` 5/11, `candidate-matching` 5/15, `people-import` 5/15, `people-pool` 8/17, `post-requirement` 8/13, `staffing-confirmation` 9/18 |
| Chinese translation parity | counted requirements, scenarios and `**Traces:**` lines in each `spec.zh.md` | 40 / 89 / 40 in both languages, per file identical |
| Every requirement traced | scanned each requirement block for a `**Traces:**` line | 40 of 40 |
| Every scenario given/when/then | parsed each scenario block for GIVEN / WHEN / THEN | 89 of 89, no duplicate titles across the six files |
| The lock ignores dates | read `allocation` "Disjoint dates still block" and G6 in `candidate-matching` | a post running 2028 is blocked by an allocation whose project runs through 2027; no spec compares date ranges for occupancy any more |
| One allocation per person | read `allocation` "A second project is refused" and `staffing-confirmation` "A second confirmation waits for the project to end" | a second recording is refused with "person already staffed"; the earlier allowance for sequential postings is gone |
| Project end is terminal | read `allocation` "Ending a project is terminal and releases its whole team" | one atomic change: project `closed`, its posts `cancelled`, pending recommendations `voided` with `post cancelled`, allocations deleted; only the project's lead may do it; no reopen |
| Post dates no longer schedule (superseded at rev 11) | searched the six specs for date-range matching on occupancy | at rev 10 only the field validation and the freeze remained; `post-requirement` then stated the dates neither create nor bound an allocation |
| Scoring removal still holds | searched the six English specs for `score`, `ranking`, `ranked` | only the prohibition sentences remain |

## Rev 11 re-verification (dates return to G6, project end kept)

| Check | How | Result |
|---|---|---|
| Requirement / scenario counts | counted `### Requirement:` and `#### Scenario:` per file | 40 / 90; per capability `allocation` 5/11, `candidate-matching` 5/15, `people-import` 5/15, `people-pool` 8/17, `post-requirement` 8/14, `staffing-confirmation` 9/18 |
| Chinese translation parity | counted requirements, scenarios and `**Traces:**` lines in each `spec.zh.md` | 40 / 90 / 40 in both languages, per file identical |
| Every requirement traced | scanned each requirement block for a `**Traces:**` line | 40 of 40 |
| Every scenario given/when/then | parsed each scenario block for GIVEN / WHEN / THEN | 90 of 90, no duplicate titles across the six files |
| Dates are back in the contract | read `allocation` "An allocation records a person on a project for an inclusive date range" and the write-back step | confirmation stores the post's inclusive dates; a hand-entered allocation stores its own range; the coordinator may correct or remove a hand-entered one, while a confirmation-sourced allocation moves only with its assignment |
| Only an intersection blocks | read `candidate-matching` G6 and `allocation` "An allocation makes a person unavailable for an intersecting post" | `E100` allocated 2027-01-01 to 2027-03-31 passes G6 for a post running 2027-05-01 to 2027-07-31 and is excluded by one running 2027-03-31 to 2027-05-31 |
| Both end points are closed | read "A boundary day still blocks" and the G6 sentence | an allocation ending on `post.start` and one starting on `post.end` both refuse the person |
| Overlap is a fact, not an error | read `allocation` "Overlapping allocations are accepted at entry" | two intersecting hand-entered allocations for one person are stored with no blocking validation, and either can be corrected later |
| Same-day exclusivity survives | read `staffing-confirmation` "No two assignments cover the same day" | an intersecting confirmation is refused with `person taken`; two disjoint assignments are allowed, so one person can follow one project with another |
| Voiding is date-scoped | read `staffing-confirmation` "The other pending proposal is voided" and the write-back step | only recommendations whose post dates intersect the new allocation are voided; the disjoint one stays `proposed` |
| A confirmed allocation is not removable by hand | read `allocation` "A confirmed allocation cannot be removed by hand" | the removal is refused with the release-first reason, so an assignment can never outlive its occupancy |
| A deactivated role does not freeze the post | read `post-requirement` "A deactivated role does not block the title" | the title still saves with the stored role; switching to another deactivated role is refused |
| A cancelled post is read-only | read `post-requirement` "Cancelling is refused while a person is confirmed" | after cancellation no field, headcount or skill row changes |
| Refusal and rejection are distinct | read `people-import` "A workbook with one sheet is refused" and "An import is atomic" | a structurally unusable upload records no batch; a readable file with a bad row records a `rejected` batch |
| Project end still terminal | re-read `allocation` "Ending a project is terminal and releases its whole team" | unchanged from rev 10: project `closed`, posts `cancelled`, pending recommendations `voided` with `post cancelled`, allocations deleted, lead only, no reopen |
| Profile shows dates again | read `people-pool` "Person profile is readable by the coordinator" | every allocation, current or past, is listed with project, start and end, and an ended one is marked |
| Post dates schedule the allocation | read `post-requirement` "Post fields are validated on save" | the dates place the post on the calendar and become the range of the allocation written on confirmation; an unfilled post creates none |
| Scoring removal still holds | searched the six English specs for `score`, `ranking`, `ranked` | only the prohibition sentences remain |

## Residual concerns for IO / SA

- **IO:** the recommendation snapshot is immutable, so a lead may see gate results that no longer
  match current data. The screen should show the snapshot time next to the reason snapshot,
  otherwise the lead reads it as "live". Decision needed on wording, not on rule.
- **SA:** the same-day rule needs a guard that closes the race, not only a pre-check — two
  confirmations whose date ranges intersect can pass the G6 re-check concurrently. A uniqueness
  key no longer applies, because one person may hold several disjoint allocations. The spec states
  the outcome; the mechanism belongs to `design.md`.
- **IO:** the fallback section must read as "another role, one band higher, this domain skill"
  and never as a role change for the person. The post's role stays the only role they are being
  staffed into; the evidence belongs in the row, not in a tooltip.
- **SA:** X2 depends on catalog configuration, so moving a skill to another domain silently
  changes who the fallback can reach. The empty-domain rule is the guard rail and needs its own
  test, or a role added later would behave like "matches any skill".
- **Product:** the suitable list can grow long once many people pass. The MVP lists everyone in
  name order; a later filter or paging control is a UI decision, not a change to eligibility.
- **Product / SA:** ending a project is coarse — it cancels every post of the project, filled ones
  included, and cannot be undone. Accepted at rev 10 for the MVP; a partial close or a reopen
  path would be a later change, not a variant of this one.
- **IO:** because both end points are inclusive, one mis-typed end date blocks an extra day and can
  hide a person from a post that would otherwise fit. The profile and the post both show their
  dates, and the coordinator can correct either record; a warning on a near-miss range would be a
  later convenience, not a rule.

## Verdict

No blocking gap: AC-1 to AC-4 and DoV-1 to DoV-5 all Pass after the rev 11 rewrite. The delta is
fit to go to a **new G1** — the signed rev 8 record stays as it is and covers neither rev 9, nor
rev 10, nor rev 11. That gate has since been held: `g1-decision.md` carries a G1 **Pass** on rev 11
dated 2026-11-11 (IO raylen, SA kyrie), appended below the untouched rev 8 record.
This review is not a Gate decision: G1 is signed by a human via `/sdd:g1-decision-record`.
