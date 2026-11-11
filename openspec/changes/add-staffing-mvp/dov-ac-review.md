# AC / DoV review — add-staffing-mvp (P2)

- **Change:** `openspec/changes/add-staffing-mvp/`
- **Phase:** P2 / `step=all` (generate + review)
- **Artifact level:** 2 (proposal + delta specs; no `design.md`, no `tasks.md` by instruction)
- **Delta specs:** 6 capabilities, 41 requirements, 89 scenarios
- **Tooling:** `openspec` CLI is not installed, so `openspec validate` could not run; the tree
  was hand-authored to the documented layout (`changes/<id>/specs/<capability>/spec.md`,
  `## ADDED Requirements`)
- **Date:** 2026-11-10

## Acceptance criteria

| Item | Result | Evidence | Notes |
|---|---|---|---|
| AC-1 Every requirement has at least one testable AC | Pass | 41 `### Requirement:` blocks vs 89 `#### Scenario:` blocks under `specs/staffing/*/spec.md` | Minimum 2 scenarios per requirement; `people-pool` 8/17, `people-import` 5/15, `allocation` 5/8, `post-requirement` 8/13, `candidate-matching` 6/18, `staffing-confirmation` 9/18 |
| AC-2 ACs are observable | Pass (after review fix) | `specs/staffing/candidate-matching/spec.md` "A candidate who exactly meets every requirement" asserts 24.5 and 87.6; "Contributions add up to the score" now derives 24.5 / 15 / 20 / 20 / 10 = 89.5 from the inputs instead of assuming the total; `specs/staffing/people-import/spec.md` "One bad row blocks the whole file" asserts batch `rejected` plus zero data change | Every scenario asserts a state, a count, a stored value or a refusal with a named reason; the earlier circular "a candidate scoring 89.5 → the parts add up to 89.5" was replaced |
| AC-3 Negative and edge cases are called out | Pass | Boundary: `allocation` "A boundary day still blocks"; `post-requirement` "Dates cannot move under a confirmed person", "Cancel is refused, then allowed"; `people-import` orphan row, duplicated key, empty workbook; `staffing-confirmation` "A concurrent double confirmation"; `candidate-matching` "A person at the required band is not above it", "A skill outside the post role's domain never admits", "The top band has no cross-role candidate" | Negative coverage grew with rev 8: the fallback cases sit alongside the earlier 13, so every new tier has a refusal case rather than only a happy path |
| AC-4 No disguised implementation detail, no vague wording | Pass (after review fix) | `specs/staffing/staffing-confirmation/spec.md` now states "apply every effect below together, or none of them" instead of the earlier "in one transaction" | The atomicity is the requirement; the mechanism word was removed so `design.md` owns it. No vague adjectives (`correctly`, `quickly`, `appropriately`) remain; the earlier "slightly less when more flexible" in REQ-001 §6 was replaced by the concrete 10 / 7 / 5 |

## Definition of verified

| Item | Result | Evidence | Notes |
|---|---|---|---|
| DoV-1 Scope matches the proposal, no silent expansion | Pass (after review fix) | 6 delta capabilities vs 6 rows of `proposal.md` "New capabilities"; no `MODIFIED` / `REMOVED` section because `openspec/specs/` is empty | `allocation` also fixes project status as informational; `proposal.md` step 3 was amended in the same pass so the two artifacts agree. One over-reach was removed in the second pass: `post-requirement` had required post dates to sit inside the project's dates, which no upstream artifact says, so the clause and its scenario went and the question became open item **D7** |
| DoV-2 Ambiguities listed or resolved | Pass | REQ-001 §11 B1–B18 resolved; D1–D6 deferred in REQ-001, D7–D8 recorded in `proposal.md`; every deferred item is outside the delta (`D6` = removing a person's wrong skill, `D7` = post dates vs project dates, `D8` = capping the cross-role section or requiring an extra approval) | The delta pins the numbers the proposal left open: five weights, 30-day headroom windows, travel 10 / 7 / 5, renormalisation |
| DoV-3 Role concerns noted (IO / SA) | Pass | "Residual concerns" below | Two items need an owner decision before implementation, none blocks G1 |
| DoV-4 No business implementation claimed | Pass | No code, no `tasks.md`, no `design.md` in the change folder | P2 only shapes the contract |
| DoV-5 Requirements trace to the proposal / approved requirements | Pass (after review fix) | Every requirement carries a `**Traces:**` line; S1–S38 all referenced across the six specs; the eleven `key rule "…"` quotations were re-checked as literal substrings of `proposal.md` | Trace union covers S1–S38; each capability's traces name its proposal step and its REQ-001 section. The first pass had invented rule names (`"no double booking"`, `"confirmation is the only write path"`, `"five weighted criteria"`, …) that do not appear in the proposal; all eleven now quote text that exists |

## Corrections applied while writing the delta

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

| Check | How | Result |
|---|---|---|
| Requirement / scenario counts | counted `### Requirement:` and `#### Scenario:` per file | 41 / 89, per capability 8/17, 5/15, 5/8, 8/13, 6/18, 9/18 |
| Every requirement traced | scanned each requirement block for a `**Traces:**` line | 41 of 41 |
| Every scenario given/when/then | parsed each scenario block for GIVEN / WHEN / THEN | 89 of 89, no duplicate titles across the six files |
| Citations resolve | extracted every `key rule "…"` and searched `proposal.md` lower-cased | 11 of 11 match literally |
| Scenario coverage | searched the six files for `S1`–`S38` | all 38 present |
| Scores recompute | applied the stated formulas to the scenario inputs | 24.5 / 87.6, 74.5 over 85; 89.5; 29.75; 71.5 all consistent |
| Fallback tier closure | checked the trigger, X1, X2 and the two stand-ins against REQ-001 §6.1 | trigger = empty same-role list; X1 = required rank + 1; X2 = domain skill ≥ `Practitioner`; G1 / G5 / G6 still applied; X1 replaces G3, X2 replaces G2 + G4; empty domain never degrades to "any skill" |
| Domain mapping is single-valued | read the skill catalog requirement and the five seeded roles | each seeded skill appears under exactly one role, and a role added later starts with an empty domain |

## Residual concerns for IO / SA

- **IO:** the recommendation snapshot is immutable, so a lead may see a score that no longer
  matches current data. The screen should show the snapshot time next to the score, otherwise
  the lead reads it as "live". Decision needed on wording, not on rule.
- **SA:** the renormalisation rule ("a criterion with no rows is dropped and the remaining
  weights rescaled to 100") is cheap to implement but must be applied identically in the
  ranking and in the per-criterion display, or the contributions stop adding up.
- **SA:** "no double booking" needs a storage-level guarantee, not only a pre-check — two
  confirmations can pass the G6 re-check concurrently. The spec states the outcome; the
  mechanism belongs to `design.md`.
- **IO:** the fallback section must read as "another role, one band higher, this domain skill"
  and never as a role change for the person. The post's role stays the only role they are being
  staffed into; the evidence belongs next to the score, not in a tooltip.
- **SA:** X2 depends on catalog configuration, so moving a skill to another domain silently
  changes who the fallback can reach. The empty-domain rule is the guard rail and needs its own
  test, or a role added later would behave like "matches any skill".

## Verdict

No blocking gap: AC-1 to AC-4 and DoV-1 to DoV-5 all Pass after the one wording fix applied during
this review. The delta is fit to go to G1. This review is not a Gate decision: G1 is signed by
a human via `/sdd:g1-decision-record`.
