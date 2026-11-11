# INT-001 — Staffing system (raw intent capture)

- **Captured:** 2026-11-10
- **Source:** Product owner brief (verbatim intent, condensed)
- **Status:** promoted to `requirements/REQ-001-staffing-mvp.md`

## Raw intent

A company runs several projects at once and often has to find suitable employees for
a new project or for the empty posts of an existing one. It wants a Staffing system
for the project lead and the resource coordinator: the project lead raises the post
requirement, the resource coordinator selects employees from the company's people
pool and recommends them, and after the project lead confirms, both can see the
chosen people and the posts that are still short.

Finding people for a project has to weigh:

1. whether the employee's role, level and skills meet the post requirement;
2. whether the employee's existing and future project arrangements fit this
   project's dates;
3. whether the employee accepts the travel the project requires.

Follow-up facts stated by the product owner:

- Employees have roles (project manager, product, design, development, test) and
  skills; each skill has a level.
- An employee also has a job level of their own (separate from skill levels).

## Clarifications answered in session (P1)

| # | Question | Answer |
|---|---|---|
| 1 | How does the system "weigh" fit? | Ranked scoring with reasons; coordinator still decides. |
| 2 | Where do people, skills, travel and schedule data live? | Maintained inside the Staffing system for the MVP; no external sync. |
| 3 | Does confirmation write back the employee's schedule? | Yes — a confirmed person is occupied for the post dates and blocks later matches. |
| 4 | How many OpenSpec changes for this MVP? | One end-to-end change. |
| 5 | Which fields arrive by report? | Full person master data: employee number, name, email, role, job level, travel willingness, active flag, skills with levels. |
| 6 | Merge or full snapshot on import? | Incremental merge; a skill missing from the report keeps its stored level; nothing is deleted. |
| 7 | What happens to invalid rows? | All-or-nothing (reversed at rev 6): one invalid row rejects the whole batch, nothing is written, and every bad row is listed. |
| 8 | Unknown employee / unknown skill? | An unknown employee number in `people` creates the person; an unknown skill is an error that rejects the batch. |
| 9 | Person key in the report? | Employee number, unique; a blank or duplicated number rejects the batch. |
| 10 | Are all post skills hard gates? | No — skills are `required` (gate) or `bonus` (score only). |
| 11 | Can one person be pending on several posts? | Yes; the first confirmation wins and the other pending recommendations are voided. |
| 12 | What may change after a post has candidates? | Pending recommendations are voided on edit; confirmed assignments stay; cancelling needs a release first. |
| 13 | How much can a project lead see? | Only the candidates recommended to them, with score and reasons; never the full pool or ranking. |
| 14 | May a post's dates change while someone is confirmed? | No — the confirmed people must be released first. The same lock covers role, required job level, `required` skill rows and travel. |
| 15 | May a post's headcount change while someone is confirmed? | It may rise (and the post returns to `partially filled`); it may not fall below the confirmed count. |
| 16 | May a referenced role or skill be renamed or deleted? | No. The name freezes once referenced and the item is deactivated instead of deleted. |
| 17 | A `skills` row for an employee number that neither the system nor the file's `people` sheet knows? | Rejected as "person not found" — a person cannot exist without a role and a job level. |
| 18 | Which roles does a person and a post have? | Five: `PM`, `BA`, `DEV`, `QA`, `UX`. This re-labels the earlier "product" as `BA` and "design" as `UX`. |
| 19 | Which job levels exist? | Four ordered bands: `Junior` < `Senior` < `Lead` < `Principal`. |
| 20 | Which skills does the catalog start with? | Six: 后端开发, 前端开发, 项目管理, 业务分析, 测试, UI设计. |
| 21 | Which proficiency bands exist? | Five ordered bands: `Novice` < `Advanced beginner` < `Practitioner` < `Proficient` < `Expert`. |

## Requirement change — rev 2 (2026-11-10)

The product owner added: *person job level and skill information is imported through a
report*. This refines clarification 2 rather than reversing it — the data is still
owned and stored by the Staffing system, but the entry channel is a report upload
rather than per-record form entry. Consequences:

- A new `staffing/people-import` capability with a batch history and error list.
- Person master data has no in-app edit path; corrections go through a corrected
  report.
- Impact brief and proposal updated; REQ-001 is at revision 2.

## Requirement change — rev 3 (2026-11-10)

Four state-machine / visibility decisions were settled after a gap review: post skills
are split into `required` and `bonus`; a person may be pending on several posts with
first-confirmation-wins; editing or cancelling a post voids pending recommendations
while confirmed assignments stay; and a project lead sees only the candidates
recommended to them. Defaults recorded without a question: the coordinator owns
projects and their lead; allocations may overlap when entered by hand; confirmation
re-runs the active and schedule gates.

## Requirement change — rev 4 (2026-11-10)

A second gap review asked what happens to a confirmed post when its requirement changes.
The owner chose the strict option everywhere: an edit that could invalidate a confirmed
person is refused instead of tolerated. Four answers, recorded above as clarifications
14–17, narrow clarification 12:

- Dates and every other gate-bearing post field are locked while a person is confirmed;
  only the title and `bonus` rows stay editable (and those void pending recommendations).
- Headcount may rise but never fall below the confirmed count, so a post is never
  overfilled.
- Catalog items are deactivated, never deleted, and their names freeze once referenced.
- A `skills` row with an unknown employee number is rejected rather than creating an
  incomplete person.

Consequences: `Skill` and `Role` become first-class catalog concepts with an `active`
flag; the import contract gains required-cell, duplicate-row and orphan-row rules;
the display contract gains a person profile, a catalog surface and a batch history.
REQ-001 is at revision 4; scenarios now run S1–S29.

## Requirement change — rev 5 (2026-11-10)

The product owner fixed the controlled value sets (clarifications 18–21). Two
consequences worth naming:

- The role wording changes: what the first brief called "product" is now `BA`, and
  "design" is now `UX`. The count stays five; only the labels move.
- Both scales switch from anonymous numbers to named bands — four job levels and five
  proficiency bands — so every gate, score and import rule becomes a rank comparison
  over band names, and the UI never shows a number.

The skill catalog is seeded with six items; the catalog stays extensible, and the role
catalog likewise. REQ-001 is at revision 5; scenarios now run S1–S31.

## Requirement change — rev 6 (2026-11-10)

The product owner rejected partial success: *if the uploaded sheet has any problem, it
fails completely*. Clarification 7 is therefore reversed. An import is one unit of
work — every row in both sheets must validate, otherwise nothing is written, the batch
is recorded as `rejected`, and the error list names every bad row so one corrected
re-upload clears them all.

Consequences: `ImportBatch` has two states, `applied` and `rejected`; the "that row is
rejected, the rest applies" wording is gone from every rule; S12, S14, S17, S29 and S31
now assert that nothing was written; S32 covers a skills-only file whose `people` sheet
is empty. REQ-001 is at revision 6; scenarios run S1–S32.

## Requirement change — rev 7 (2026-11-10)

*Every person holds exactly one role.* Rev 6 described the field as a "role reference" but never
said it was single-valued, and the import rule only required "an active catalog role", so a
`role` cell reading `DEV, QA` had no defined outcome. The rule is now explicit in three places:
the `Person` row and the `Role` vocabulary entry in §5, the `Validity` row in §5.1, and gate G2.
A multi-role cell is a row error and is never split into two roles; roles and skills stay
separate catalogs, so holding 测试 never makes a `DEV` also a `QA`. S31 was extended to cover
both a role outside the catalog and a multi-role cell, so the scenario set stays S1–S32.
REQ-001 is at revision 7.

Two smaller corrections went with this revision: `proposal.md` no longer calls the roles a
"fixed five-value catalog" (they are seeded and extensible, as §5 says), and open item **D7**
now records that the MVP does not require a post's dates to fall inside its project's dates.

## Requirement change — rev 8 (2026-11-10)

*When a post's role has nobody, other roles may be offered as a lower tier.* The product owner
added a cross-role fallback: a post that names one role, one skill and one job level may, when
no person of that role qualifies, also be shown people of other roles whose job level is at
least one band above the requirement and who hold at least one skill of the post role's skill
domain at `Practitioner` or above. Three answers pinned its shape:

- It opens only when the same-role ranked list is empty, and is not shown at all as soon as one
  same-role candidate qualifies; the two lists are never merged or re-sorted together.
- Both conditions are hard: a person level with the requirement is not above it, and a skill
  outside the post role's domain never admits anyone.
- Cross-role people get their own section, are scored on the same five criteria, and never rank
  above a same-role candidate.

Consequences: the skill catalog gains one **role skill domain** per skill (`DEV` → 后端开发,
前端开发; `QA` → 测试; `BA` → 业务分析; `PM` → 项目管理; `UX` → UI设计; a role added later
starts empty), the fallback gates X1 (level at least one band higher) and X2 (a domain skill at
`Practitioner` or above) stand in for G3 and for G2 + G4, while G1, G5 and G6 stay in force, and
the recommendation snapshot carries the evidence that admitted the person. G2 itself is
untouched, so "one person, one role" still holds. REQ-001 is at revision 8; scenarios run
S1–S38. Open item **D8** records that the MVP caps neither the section nor the confirmation.

## Spec alignment — P2 (2026-11-10)

P2 added no new product requirement. Four numbers that rev 6 left loose were pinned so the
delta could be tested, and one scoring criterion changed shape after a testable AC exposed a
defect:

- Travel fit scores 10 / 7 / 5 (exact match / one band more flexible / two), replacing
  "slightly less".
- Schedule headroom scores 20 / 10 / 0 across the two 30-day windows around the post.
- A score criterion with no rows of its kind is dropped and the remaining weights are
  rescaled to 100, so a post without `required` skills still produces a comparable 0–100.
- A voided recommendation stays in the coordinator's notice list until it is dismissed; it
  never expires.

One criterion changed shape, which is the only behaviour change in P2: the required-skill
criterion used to score `min(surplus, 2) / 2`, so a candidate who met every requirement
exactly earned 0 of 35 points and about 59 overall — the heaviest criterion rewarded only
surplus. Each row now scores `0.7 + 0.3 × min(surplus, 2) / 2`, and the criterion is renamed
from "surplus" to "required-skill fit".

Two wording corrections went with them: the write-back is specified as "every effect or
none" instead of "in one transaction", and project status became explicitly informational
in `proposal.md` step 3 so the delta does not read as a silent scope addition.

The result lives in `openspec/changes/add-staffing-mvp/specs/staffing/*/spec.md` — six
capabilities, 41 requirements, 89 scenarios — with `dov-ac-review.md` (all Pass) and
`consistency-lint.md` (Partial, capped by the absent `openspec` CLI). G1 is still open.
Revision 8 added one requirement each to `people-pool` (the role skill domain) and
`candidate-matching` (the fallback tier) plus one scenario to `staffing-confirmation` (the
evidence carried into the snapshot), which is where the 39 / 78 grew to 41 / 89.

### Added after the prototype review (2026-11-10)

A person holds exactly one role. The model only said "role reference" and the import rule only
said "must be an active catalog role", so a `role` cell reading `DEV,QA` had no defined
outcome. The delta now says the role is a single reference that is never a set, asserts it in
scenario "A person holds exactly one role", and rejects a multi-role cell as a row error rather
than splitting it. This is a clarification that matches the agreed vocabulary, not a new
capability.

## Not yet decided (deferred, non-blocking)

- Sign-in mechanism (seeded demo accounts vs company SSO).
- Whether a person can be staffed part-time (allocation percentage).
- Whether travel willingness eventually becomes destination-specific.
- Exact column names of the real HR report export.
- Rolling back or replaying an old import batch.
- Removing a wrong skill from a person (the MVP has no removal channel).
- Whether the cross-role fallback needs a cap or an extra approval before confirmation (D8).
