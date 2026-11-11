# REQ-001 — Staffing MVP: match people to project posts

- **ID:** REQ-001
- **Status:** ready — rev 11 (an allocation covers the post's inclusive dates, so only an intersecting range blocks; the project end still releases the whole team)
- **Source:** `intents/INT-001-staffing.md`
- **OpenSpec change:** `openspec/changes/add-staffing-mvp/`
- **Date:** 2026-11-11

## 1. Intent

A company running several projects at once needs one place where a project lead can
state which posts a project is short of, and a resource coordinator can find and
recommend employees from the company people pool, so that the lead can confirm
people and both sides share one view of who is staffed and what is still missing.

## 2. Actors and permission boundaries

| Actor | May do | May not do |
|---|---|---|
| Project lead | Create / edit / cancel posts of own projects; end an own project (terminal: cancels its posts and releases everyone staffed on it); view the candidates recommended to them with the reasons snapshot; confirm or reject recommendations; release a confirmed person from a post; view chosen people and remaining gaps | Edit the people pool, edit allocations, recommend people, browse the full candidate pool or its suitable list |
| Resource coordinator | Maintain role and skill catalogs (add, activate, deactivate); upload the people master report (identity, role, job level, travel willingness, active flag, skills) and read its error list; maintain projects and their lead; record an allocation for a person staffed outside the app; search the pool and read the suitable list; recommend / withdraw recommendations; release a confirmed person from a post; view all projects' posts and gaps | Confirm a recommendation; edit posts; end a project; create or change a person's master data by hand; delete a referenced catalog item or rename it; cancel a post that still has a confirmed person |
| System | Compute eligibility and list the suitable people; write the allocation on confirmation; release a project's people when the project is ended; keep post remaining count and status consistent | Decide who is recommended or confirmed |

Visibility: a project lead sees only projects they own, and inside those projects
only the candidates the coordinator recommended to them — never the full candidate
pool or its suitable list. The coordinator sees all projects and the full pool. Sign-in is
a lightweight seeded account with a role; SSO is deferred.

## 3. In scope

- People pool, refreshed from a report: employee number, name, email, role (exactly
  one), job level, travel willingness, active flag, and skills with proficiency levels.
- Report import: upload one workbook, validate every row, apply nothing unless the
  whole file is valid, and keep a per-batch error list (contract in section 5.1).
- Role and skill catalogs maintained in the app, each item `active` or `inactive`; a
  name freezes once anything references it, and an item is deactivated rather than
  deleted. A report can reference catalog items but never create them.
- Projects with a lead, start and end dates, created and maintained by the
  coordinator. The project lead ends a project exactly once.
- Project allocations: a person occupied by a project for an inclusive date range. An
  allocation is written by confirmation for the post's dates, or entered by the
  coordinator for a person staffed outside the app; the coordinator may correct or
  remove one at any time. Two ranges that do not intersect never block each other.
- Post requirement: role, required job level, skills with minimum proficiency,
  headcount, project dates, travel requirement, status.
- Post skills are typed: `required` (a hard gate) or `bonus` (informational only,
  with a target proficiency).
- Travel willingness is a band with no dates: G5 compares the person's willingness with
  the post's requirement and nothing else.
- Post lifecycle: while a post has a confirmed person its gate-bearing fields (role,
  job level, `required` skills, dates, travel) are locked and its headcount may only
  rise; the title and `bonus` rows stay editable. Cancelling is refused while a
  confirmed person remains.
- Release: the lead or the coordinator can remove a confirmed person from a post;
  the assignment and its allocation are deleted and the slot reopens. Ending a project
  releases everyone staffed on it in one step and closes the project for good.
- Candidate search for a post: hard eligibility gates (including the date-overlap gate),
  a suitable list ordered by name, and a human-readable reason per candidate (including
  why a candidate is excluded).
- Cross-role fallback: when nobody of the post's role qualifies, people of other roles
  are offered in a separate list if their job level is at least one band above the post's
  requirement and they hold a skill of the post role's skill domain at `Practitioner` or
  above. The role gate is never relaxed; the fallback is an explicitly lower tier.
- Recommendation lifecycle: proposed by coordinator, accepted (confirmed) or
  rejected by lead, withdrawable by coordinator.
- Confirmation write-back: assignment recorded, allocation created, post remaining
  count and status updated.
- Shared "still short" view: per post, filled people and remaining headcount.

## 4. Out of scope

- Any HR / project-management system integration, including scheduled or
  delivered file drops. The coordinator uploads the workbook by hand; the data
  still lands and lives in-system.
- Allocation percentage / part-time staffing: one allocation occupies the person
  fully for its date range.
- Cost, rate, budget and utilization reporting.
- Notifications (email, chat) — status is visible in the app only.
- Automatic assignment without a human decision.
- Interview / approval chains beyond lead confirm-reject.
- Multi-company tenancy, mobile client, offline mode.

## 5. Domain concepts and data contract (deep-dive summary)

| Concept | Grain | Key fields | States / transitions |
|---|---|---|---|
| Person | one employee (key: employee number) | employee number (unique), name, email, role (exactly one catalog role, never a set), job level, travel willingness, active | created by import; active ⇄ inactive (set by report) |
| Role | one entry of the role catalog | id, name, active | active ⇄ inactive; name frozen once referenced; never deleted |
| Skill | one skill in the catalog | id, name, domain (the single role whose skill domain it holds the skill), active | active ⇄ inactive; name frozen once referenced; never deleted |
| PersonSkill | one person × one skill | person, skill, proficiency band | upsert on import; no removal in the MVP |
| Project | one project | id, name, lead, start date, end date | planned → running → closed; `closed` is terminal, cancels the project's posts and releases every allocation of the project |
| Allocation | one person × one project × inclusive date range | person, project, start date, end date, source (confirmation or manual entry), created by, created at | active until removed by a single release or by the project end; the coordinator may correct or remove it at any time |
| Post | one post of a project | project, title, role, required job level, headcount, start, end, travel requirement, status | open → partially filled → filled; any → cancelled; gate-bearing fields frozen while an assignment exists |
| PostSkillRequirement | one post × one skill | post, skill, kind (`required` or `bonus`), minimum / target proficiency band | `required` rows frozen once the post has an assignment; `bonus` rows stay editable |
| Recommendation | one post × one person × one attempt | post, person, reason snapshot (gate results and qualifying skills), proposed by, decided by | proposed → confirmed / rejected / withdrawn / voided (reason: person taken, post changed, post cancelled) |
| Assignment | one confirmed post × person | post, person, confirmed by, confirmed at, allocation | active → removed (post reopened) |
| ImportBatch | one uploaded workbook | id, file name, uploaded by, uploaded at, row counts, status | `applied` (every row valid, everything written) or `rejected` (nothing written); history is immutable |
| ImportRowError | one rejected row of one batch | batch, sheet, row number, key value, reason | append-only |

Vocabulary:

- **Role** — one of `PM` (project manager), `BA` (business analyst), `DEV`
  (developer), `QA` (quality assurance), `UX` (user-experience designer). The catalog
  is seeded with these five and stays extensible. **A person holds exactly one role:**
  the person's role is a single catalog reference, never a set, and no channel — form,
  import or API — may add a second one. Roles and skills are separate catalogs; holding
  a skill never grants a role.
- **Job level** — one of four ordered bands, rank 1–4: `Junior` < `Senior` < `Lead` <
  `Principal`. A post requires a minimum band.
- **Skill** — the catalog is seeded with six items: 后端开发 (backend), 前端开发
  (frontend), 项目管理 (project management), 业务分析 (business analysis), 测试
  (testing), UI设计 (user-interface design); it stays extensible.
- **Skill proficiency** — one of five ordered bands, rank 1–5: `Novice` <
  `Advanced beginner` < `Practitioner` < `Proficient` < `Expert`, held on each
  person-skill pair. A post skill requirement carries a minimum or target band.
- **Role skill domain** — every skill belongs to exactly one role, its domain; the domain
  of a role is the set of skills assigned to it. Seeded domains: `DEV` → 后端开发,
  前端开发; `QA` → 测试; `BA` → 业务分析; `PM` → 项目管理; `UX` → UI设计. A role
  added later starts with an empty domain until the coordinator assigns skills to it. A
  domain is a matching aid only: it never grants a person a second role, and it never
  becomes a role a person can hold.
- **Travel willingness / requirement** — ordered levels: `none` < `limited` < `full`.
  A person accepts a post when their willingness rank is at or above the post rank.
- **Remaining headcount** — `headcount − count(active assignments)`.
- **Allocation / occupied** — a person staffed on a project for an inclusive date range.
  Confirmation writes it from the post's dates; the coordinator may record one by hand for
  a person staffed outside the app. While a post's range intersects an active allocation
  the person fails G6 for that post; a range that does not intersect blocks nothing, so two
  projects can follow one another.
- **Project end** — the project lead closes the project once. It is terminal: the
  project cannot be reopened, each of its posts becomes `cancelled`, their pending
  recommendations are voided and every allocation of the project is removed in the same
  step, which puts its people back in the pool.

### 5.1 People master report (import contract)

The report is the only channel that creates or changes person master data.

| Item | Rule |
|---|---|
| File | One `.xlsx` workbook with two sheets, `people` and `skills` |
| `people` columns | `employee_no`, `name`, `email`, `role`, `job_level`, `travel_willingness`, `active` |
| `skills` columns | `employee_no`, `skill`, `proficiency` |
| Required cells | In a `people` row, `employee_no`, `name`, `role`, `job_level`, `travel_willingness` and `active` must be non-blank; `email` may be blank |
| Identity | `employee_no` is the unique key; a missing, blank or duplicated number is an error |
| Value sets | Seeded roles {`PM`, `BA`, `DEV`, `QA`, `UX`} — a role added to the catalog later is equally valid; `job_level` ∈ {`Junior`, `Senior`, `Lead`, `Principal`}; `skill` ∈ the catalog, seeded with {后端开发, 前端开发, 项目管理, 业务分析, 测试, UI设计}; `proficiency` ∈ {`Novice`, `Advanced beginner`, `Practitioner`, `Proficient`, `Expert`}. Cells are trimmed, and ASCII values match case-insensitively |
| Sheet presence | Both sheets must be present. The `people` sheet may be empty when the file only refreshes skills of existing employees; a file with no data rows at all is rejected |
| New person | An `employee_no` unknown to the system creates the person (the report is the master source) |
| Unknown skill | A skill name that is not in the catalog is an error |
| Merge semantics | Values present in the report overwrite what is stored; a skill missing from the report keeps its stored proficiency; nothing is deleted |
| Duplicate skill row | The same `employee_no` twice for the same `skill` is an error |
| Orphan skill row | A `skills` row whose `employee_no` exists neither in the system nor in the `people` sheet of the same file is an error ("person not found"), because a person cannot be created without a role and a job level |
| Validity | `role` must be exactly one `active` role in the catalog: a deactivated role is an error, and a cell naming two roles or a delimited list is an error that is never split into two roles; `job_level` and `proficiency` must be one of the bands above; `travel_willingness` ∈ {none, limited, full}; `active` ∈ {active, inactive} |
| Atomicity | The workbook is one unit of work. Every row in both sheets must validate before anything is written: a single error makes the batch `rejected`, nothing at all is stored — no person created, no proficiency changed, no flag flipped — and the coordinator fixes every listed row and uploads the file again |
| Error report | Every invalid row is listed with sheet, row number, key value and reason, so one corrected re-upload can clear them all; the list belongs to the rejected batch and stays readable |
| Side effects | Import never touches allocations, posts, recommendations or assignments; the next candidate search simply uses the new data |

## 6. Matching rules (decided)

Eligibility gates are evaluated in this order; the first failing gate becomes the
candidate's displayed exclusion reason. A person failing any gate is never
listed as suitable.

| Gate | Rule |
|---|---|
| G1 Active | Person is active. |
| G2 Role | Person role equals post role. The person's role is a single reference, so the comparison is identity against that one role; a person is never eligible for two roles' posts. Section 6.1 covers the separate cross-role fallback, which never relaxes this gate. |
| G3 Level | Person job level rank ≥ post required job level rank. |
| G4 Required skills | For every `required` PostSkillRequirement, the person holds that skill at a proficiency rank ≥ the minimum band. `bonus` skills never gate. |
| G5 Travel | Person travel willingness rank ≥ post travel requirement rank. |
| G6 Allocation | No allocation of the person intersects the post's inclusive date range. An allocation ending on the post's start date and one starting on its end date both block the person, because the intersection is inclusive on both ends. |

People who pass every gate are suitable. The allocation is the schedule record: it carries
an inclusive start and end date, written from the post's dates on confirmation or entered by
the coordinator. G6 compares those dates with the post's dates, so a later post that does
not overlap stays open to the person; the project itself blocks nothing beyond its
allocation ranges.

The MVP computes no score and applies no
grading: every suitable person is listed, ordered by name ascending, so the order
carries no preference. Each row shows the person's role, job level, travel
willingness, the gates passed, and the skills that qualify them with their bands —
`required` rows with the band held, `bonus` rows with the band held and the target.
A `bonus` shortfall is shown as information: it neither excludes nor reorders anyone.
The coordinator chooses whom to recommend; the system never names a best candidate.

The list is deterministic for the same data: name ascending, then employee number
when two names are equal, so the same post and pool always produce the same order.

People excluded by a gate are listed separately with their first failing gate, plus a
count per gate, so an empty list is explained rather than silent.

### 6.1 Cross-role fallback (decided)

- **Trigger.** The fallback exists only when the post's role has nobody: no person of
  that role passes G1, G3, G4, G5 and G6, so the same-role suitable list is empty. As
  soon as one same-role person is suitable, the fallback list is not shown at all.
- **X1 Level above.** The person's job level rank ≥ the post's required rank + 1 — at
  least one band higher. A post that requires the top band has no cross-role candidate.
- **X2 Domain skill.** The person holds at least one skill of the post role's skill
  domain at `Practitioner` (rank 3) or above. The post's `required` rows are not a gate
  for these people; a post role with an empty domain finds nobody, so the rule never
  degrades to "any skill".
- **Gates still applied.** G1 active, G5 travel and G6 dates. X1 stands in for G3; X2
  stands in for G2 and G4.
- **Display.** Cross-role people form a separate section below the same-role list,
  ordered by name like the main list and never mixed into it. Each row shows the
  person's role, how many bands above the requirement they sit, and the domain skill
  and band that qualified them.
- **Explanation.** The coordinator sees the fact that the post role had no candidate; a
  recommendation made from this section carries the same evidence into its snapshot,
  and confirmation, allocation and voiding are unchanged.

Confirmation write-back:

1. The lead confirms a recommendation. Before committing, the system re-runs G1 and G6.
   If either now fails, the confirmation is refused with that reason and the
   recommendation stays `proposed`.
2. Recommendation `proposed` → `confirmed`.
3. An `Assignment` is created for the post and person.
4. An `Allocation` is created for the person and the project, covering the post's inclusive
   date range, so every post whose dates intersect that range now fails G6.
5. The post's remaining headcount decreases; at zero the post becomes `filled` and
   leaves the "still short" list.
6. Every other pending recommendation for the same person is `voided` with reason
   `person taken`; the coordinator sees each one in a notice list.
7. Rejecting a recommendation marks it `rejected` and frees the person; the
   coordinator may recommend another candidate.

Post and assignment lifecycle:

- While a post has at least one active assignment, its gate-bearing fields — role,
  required job level, `required` skill rows, start date, end date, travel requirement
  — are locked. An edit attempt is refused with the reason that the confirmed people
  must be released first, and nothing changes.
- Headcount may rise while people are confirmed: raising it above the assignment count
  moves the post back to `partially filled`. Lowering it below the assignment count is
  refused, because a post is never overfilled.
- The title and the `bonus` skill rows stay editable while people are confirmed; such
  an edit voids the pending recommendations with reason `post changed` and leaves the
  confirmed assignments and their allocations untouched.
- Cancelling a post is refused while it has an active assignment; the lead or the
  coordinator must release those people first. Cancelling voids any pending
  recommendation with reason `post cancelled`.
- Ending a project is the project lead's terminal action: the project becomes
  `closed`, each of its posts becomes `cancelled`, every pending recommendation of
  those posts is voided with reason `post cancelled`, and every allocation of the
  project is removed in the same step, releasing its people. It is the one path that
  cancels a post without first releasing its confirmed people. A closed project cannot
  be reopened or edited, and its posts cannot be revived.
- Releasing an assignment deletes it and its allocation, reopens the slot and returns
  the post to `open` or `partially filled` according to the remaining count. The
  person becomes available again for any post whose dates do not intersect the released
  range.

Catalog lifecycle:

- A catalog item (role or skill) is created active. Its name may be edited only while
  no person and no post references it.
- A referenced item cannot be deleted and cannot be renamed; it can be deactivated and
  reactivated. Deactivation changes no stored record: existing people and posts keep
  showing it, while new posts and new imports can no longer select it.
- Matching compares role identity, not role names, so deactivating a role never
  silently changes an existing gate result.
- A skill declares one role domain when it is created; changing that domain later
  applies to future searches only and changes no stored person, post or recommendation.

## 7. Scenarios and acceptance hints

| # | Scenario | Observable result |
|---|---|---|
| S1 | Lead creates a post with role `DEV`, minimum job level `Senior`, `required` 后端开发 at `Practitioner` and 前端开发 at `Advanced beginner`, headcount 2, dates and travel `limited`. | The post is listed as `open` with remaining headcount 2 and appears in the project's "still short" list. |
| S2 | Coordinator opens candidate search for a post. | Every eligible person is listed once, ordered by name, with the gates passed and the skills that qualify them; excluded people are listed separately with the first failing gate as reason. |
| S3 | Coordinator recommends a listed candidate. | The recommendation appears as `proposed` for the lead, carrying the reason snapshot (gate results and qualifying skill bands) taken at recommendation time. |
| S4 | Lead confirms the recommendation. | Both roles see the person under the post's chosen people; remaining headcount drops by one; the person now shows an allocation for the post's inclusive dates. |
| S5 | A post in another project whose dates intersect the confirmed person's allocation searches candidates. | The just-confirmed person is excluded with reason "already allocated" (G6); a post whose dates fall entirely after the allocation ends is unaffected. |
| S6 | Lead rejects a recommendation with a reason. | The recommendation becomes `rejected`; the person is available again; the coordinator can propose someone else. |
| S7 | Confirming fills the last slot of a headcount-2 post. | Post status becomes `filled` and it disappears from the "still short" list. |
| S8 | No candidate passes the gates for a post. | Search returns an empty suitable list plus counts of exclusions per gate, so the coordinator can report which requirement blocks hiring; when the cross-role fallback is evaluated, it is shown as a separate section with its own X1/X2 counts. |
| S9 (boundary) | A person's allocation ends on the post's start date. | Inclusive intersection means the person is excluded; the coordinator must free the overlapping range first. |
| S10 (boundary) | Lead tries to confirm a recommendation for another lead's post. | The action is refused and no assignment or allocation is written. |
| S11 | Coordinator uploads a workbook where one employee's job level goes `Junior` → `Senior` and 后端开发 goes `Novice` → `Practitioner`. | The batch is `applied`; the profile shows the new band and proficiency; existing allocations and assignments are unchanged. |
| S12 | Report holds five rows, two of them invalid: one carries `job_level` = `Mid`, which is not one of the four bands, and one carries `skill` = `Kotlin`, which is not in the catalog. | The batch is `rejected` and nothing is written: all five people keep their previous data, the two bad rows are listed with sheet, row number, key and reason, and the coordinator re-uploads the whole file. |
| S13 | Report contains an `employee_no` the system has never seen, with a valid role and skills. | A new person is created with the reported attributes and becomes eligible for matching immediately. |
| S14 | Report lists the same `employee_no` twice. | The batch is rejected with a duplicate-key error on both rows; no other person in the file is created or updated. |
| S15 | Report omits a skill the person already has. | The stored proficiency is kept (incremental merge), not cleared. |
| S16 | Report marks an already-confirmed employee `inactive`. | Import succeeds; the person leaves candidate lists (gate G1) while the existing assignment stays visible and is flagged as inactive in the gap view. |
| S17 | Coordinator re-uploads a corrected file after fixing the S12 errors. | The new batch is `applied` and every row is written; the earlier rejected batch and its error list stay readable. |
| S18 | Post has 后端开发 as `required` at `Practitioner` and 测试 as `bonus` at `Practitioner`; a candidate holds 后端开发 = `Practitioner` and 测试 = `Novice`. | The candidate passes the gates and is listed; the bonus shortfall is shown as information only — it neither excludes the person nor moves them in the list. |
| S19 | The same person is recommended for two posts and the first is confirmed. | The second recommendation becomes `voided` with reason "person taken"; the coordinator sees it in the notice list and can propose someone else. |
| S20 | Lead edits the dates of a post that has one confirmed person and one pending recommendation. | The edit is refused because the confirmed person must be released first; the pending recommendation, the assignment and its allocation are unchanged. |
| S21 | Lead tries to cancel a post that still has a confirmed person. | The cancellation is refused with the reason that the person must be released first; after the release the post can be cancelled. |
| S22 | Between recommendation and confirmation the person's report marks them `inactive`. | Confirmation is refused with gate G1 as the reason; the recommendation stays `proposed` and the lead sees why. |
| S23 | Lead opens a post they own. | They see only the people recommended to them with their reasons snapshot; the full suitable list and exclusion list are not reachable with their account. |
| S24 | After a recommendation, the coordinator records an allocation of that person overlapping the post dates. | Confirmation is refused with gate G6 as the reason; the recommendation stays `proposed` until the overlap is resolved. |
| S25 | Lead edits a `bonus` skill target on a post with one pending recommendation and one confirmed person. | The pending recommendation is voided with reason "post changed"; the confirmed assignment and its allocation are unchanged. |
| S26 | Lead lowers the headcount of a post from 2 to 1 while 2 people are confirmed. | The edit is refused; the post keeps headcount 2 and both assignments. |
| S27 | Lead raises the headcount of a `filled` post from 2 to 3. | The post becomes `partially filled` with remaining headcount 1 and reappears in the project's "still short" list. |
| S28 | Coordinator tries to delete the skill 后端开发, which 40 people and 3 posts reference, and then tries to rename it. | Both actions are refused; the coordinator can deactivate it instead, after which it disappears from new-post and import pickers while existing people and posts still show it. |
| S29 | The `skills` sheet holds a row for employee number `E999`, which exists neither in the system nor in the `people` sheet of the same file. | The batch is rejected with reason "person not found" on that row; the otherwise valid `people` rows are not applied either. |
| S30 | A `people` row writes `job_level` = `senior` and `proficiency` = `practitioner` in lower case. | Both values are accepted and stored as `Senior` and `Practitioner`; the row is not rejected. |
| S31 | A `people` row writes `role` = `Product`, and another row writes `role` = `DEV, QA`. | The batch is rejected: `Product` is not one of the five roles, and the `DEV, QA` cell is a multi-role error rather than two roles for one person. The error names the allowed values, no person is left holding two roles, and no row of the file is applied. |
| S32 | The file carries an empty `people` sheet and `skills` rows that reference employees already in the system. | The batch is `applied` and only the skill bands change; no person is created. |
| S33 | A `Senior` `QA` post requires 测试 at `Practitioner`, and no `QA` person passes the gates. | The same-role suitable list is empty, and a separate cross-role section is offered; the coordinator sees "no candidate of this role" as the reason the section exists. |
| S34 | The same post has one `QA` person who passes every gate, while a `Lead` `DEV` holding 测试 at `Practitioner` would also qualify. | Only the `QA` person is listed; no cross-role section appears at all. |
| S35 | The post role has nobody, and a `Senior` person of another role is considered for the `Senior` post. | The person is excluded with the reason "cross-role: level not above the requirement"; only people at least one band higher (`Lead`, `Principal`) are considered. |
| S36 | The post role is `QA` (domain 测试); a `Lead` `PM` holds 项目管理 at `Expert` but no 测试 skill. | The person is excluded with the reason "cross-role: no domain skill at `Practitioner`"; a skill outside the post role's domain never admits them. |
| S37 | A post requires job level `Principal` and no `Principal` person of its role passes the gates. | The cross-role section is empty because no band sits above `Principal`, and the exclusion list says so. |
| S38 | The `QA` post above has nobody; a `Lead` `DEV` holds 测试 at `Practitioner`. | The person appears only in the cross-role section, with the +1 band gap and the qualifying skill shown; they never appear in the same-role list, and recommending them carries that evidence to the lead. |
| S39 | Lead ends a project that has two confirmed people, one `open` post and one pending recommendation. | The project becomes `closed`; both posts are `cancelled`; the pending recommendation is `voided` with reason `post cancelled`; both allocations are gone, so both people are suitable again for posts whose dates do not intersect their old ranges. |
| S40 | The coordinator tries to end a project; after the lead ends it, the lead tries to reopen it. | The coordinator's action is refused and the project stays `running`; reopening is refused because `closed` is terminal. |

## 8. Display contract

Notifications: none in the MVP. Surfaces to be specified in the delta spec:

| Surface | Audience | Must show |
|---|---|---|
| Project post board | Lead | Posts with status, remaining headcount, pending recommendations, chosen people, the project's status and the end-project action; never the full candidate pool |
| Candidate search | Coordinator | Suitable people ordered by name with the gates passed and the qualifying skill bands; excluded rows grouped by gate; when the post role has nobody, a separate cross-role section with each row's band gap and qualifying domain skill |
| Recommendation inbox | Lead | Person, post, reason snapshot (gates and skills), confirm / reject |
| Voided recommendation notice | Coordinator | Person, post, void reason (person taken / post changed / post cancelled) |
| Import result | Coordinator | Batch status (`applied` / `rejected`), row count and the full error list with sheet, row, key and reason |
| Import batch history | Coordinator | Past batches with their counts and error lists, read-only |
| Person profile | Coordinator | Master data, every skill with proficiency, current and future allocations with their project and dates; no in-app edit of master data |
| Role and skill catalogs | Coordinator | Items with their active / inactive state; rename only while unreferenced; no delete action |
| Project gap view | Both | Per post: headcount, filled count, chosen people, remaining |
| Project list | Both | Name, lead, status; the end-project action only for the project's lead |

## 9. Constraints and dependencies

- Greenfield repository: no application code, no living specs, no archived changes.
- UI follows `shadcn/ui` (see `AGENTS.md`); the runtime stack still needs a human
  decision at design time (see deferred item D1).
- Business logic (gates, the suitable list, write-back) must not live in UI or database code
  (`docs/knowledge/rules/architecture/architecture-baseline.md`).
- All dates are calendar dates in one company time zone; no time-of-day modelling.
- Person master data has no in-app edit path in the MVP: a wrong value is fixed by
  uploading a corrected report, which leaves an audit trail in the batch history.
- The import runs synchronously for a file of up to a few thousand rows; no queue or
  background worker is needed at MVP scale.
- The import is all-or-nothing: a single invalid row blocks the file, so the master data
  never sits in a half-updated state and the batch history records attempts rather than
  partial outcomes.
- Catalog items are deactivated, never deleted, so a person's or a post's reference
  never dangles. Post titles remain freely editable because nothing matches on them.

## 10. Affected capabilities

| Capability | Change |
|---|---|
| `staffing/people-pool` | ADDED |
| `staffing/allocation` | ADDED |
| `staffing/post-requirement` | ADDED |
| `staffing/candidate-matching` | ADDED |
| `staffing/staffing-confirmation` | ADDED |
| `staffing/people-import` | ADDED |

## 11. Open questions

Blocking: none.

| # | Question | Severity | Status | Answer / owner |
|---|---|---|---|---|
| B1 | Scored ranking vs plain filtering vs auto-assign | BLOCKER | resolved | Plain filtering: every eligible person is listed by name with the gates and skills that qualify them, no score; the coordinator decides (revised at rev 9) |
| B2 | Data ownership: in-system vs external sync | BLOCKER | resolved | In-system only for MVP |
| B3 | Does confirmation occupy the person's schedule | BLOCKER | resolved | Yes: an allocation is written back for the post's inclusive dates, and any post whose dates intersect it fails G6 (revised at rev 10 and rev 11) |
| B4 | One change or several | WARNING | resolved | One end-to-end change |
| B5 | Which fields the report carries | BLOCKER | resolved | Full person master data: identity, role, job level, travel willingness, active flag, skills |
| B6 | Merge or snapshot | BLOCKER | resolved | Incremental merge; absent skills are kept, nothing is deleted |
| B7 | Behaviour on invalid rows | BLOCKER | resolved | All-or-nothing: one invalid row rejects the whole batch, nothing is written, and every bad row is listed (reversed at rev 6) |
| B8 | Unknown employee / unknown skill | BLOCKER | resolved | Unknown employee creates a person; an unknown skill is an error that rejects the batch |
| B9 | Person identity key | BLOCKER | resolved | Employee number, unique; a blank or duplicated number rejects the batch |
| B10 | Can one person be recommended to several posts at once | BLOCKER | resolved | Yes; whoever is confirmed first wins, the other pending recommendations are voided with reason "person taken" |
| B11 | Are all post skills hard gates | BLOCKER | resolved | No: `required` skills gate, `bonus` skills are informational only (revised at rev 9) |
| B12 | What happens when a post changes or is cancelled | BLOCKER | resolved | Pending recommendations are voided; confirmed assignments stay; cancelling requires releasing confirmed people first |
| B13 | What the project lead may see | BLOCKER | resolved | Only the recommended candidates with their reasons snapshot, never the full pool or suitable list |
| B14 | Changing the dates of a post that already has confirmed people | BLOCKER | resolved | Refused; the confirmed people must be released first. The same lock covers role, required job level, `required` skill rows and travel requirement |
| B15 | Changing the headcount of a post that already has confirmed people | BLOCKER | resolved | Lowering it below the confirmed count is refused; raising it re-derives `partially filled` |
| B16 | Renaming or deleting a catalog item that people or posts already reference | BLOCKER | resolved | Name frozen, no deletion; deactivate and reactivate instead |
| B17 | A `skills` row whose employee number is unknown to both the system and the file's `people` sheet | BLOCKER | resolved | It is an error ("person not found") that rejects the batch; a person cannot be created without role and job level |
| B18 | Controlled value sets for role, job level, skill and proficiency | BLOCKER | resolved | Roles `PM`/`BA`/`DEV`/`QA`/`UX`; job levels `Junior` < `Senior` < `Lead` < `Principal`; skills 后端开发, 前端开发, 项目管理, 业务分析, 测试, UI设计; proficiency `Novice` < `Advanced beginner` < `Practitioner` < `Proficient` < `Expert` |
| B19 | When does a staffed person become available again | BLOCKER | resolved | When the searched post's dates stop intersecting the allocation, when the person is released from that post, or when the project lead ends the project — terminal, releases everyone at once (added at rev 10, refined at rev 11) |
| D1 | Sign-in mechanism and runtime stack | WARNING | deferred | Dev, at design time; seeded accounts + `shadcn/ui` front end is the working default |
| D2 | Part-time staffing / allocation percentage | SUGGESTION | deferred | Product, after MVP usage data |
| D3 | Destination-specific travel willingness | SUGGESTION | deferred | Product, after MVP usage data |
| D4 | Exact column names of the real HR report | WARNING | deferred | BA / Product; the MVP mappable contract is section 5.1 |
| D5 | Rolling back or replaying an old import batch | SUGGESTION | deferred | Product; a rejected batch needs no rollback, and applied batches stay read-only history |
| D6 | Removing a wrong skill from a person | SUGGESTION | deferred | Product; the MVP has no removal channel, so a deactivated skill item is the only workaround |

## 12. DoR self-check

| Item | Result |
|---|---|
| 1 Intent clear | pass |
| 2 Actors defined | pass (permission table, section 2) |
| 3 Scope bounded | pass (sections 3–4) |
| 4 Scenarios concrete | pass (S1–S40) |
| 5 Exception / boundary | pass (S9–S10, S14, S19–S40) |
| 6 Verifiable results | pass (counts, statuses, exclusion reasons) |
| 7 Dependencies / app-arch touchpoints | pass (section 9; no integration) |
| 8 Blocking open questions = 0 | pass |
| 9 Data contract | pass at MVP grain (section 5) |
| 10 Boundary enumerated | pass (gates G1–G6, write-back steps 1–5) |
| 11 Cross-REQ aligned | N/A — single requirement |
| 12 Notification / display contract | pass (section 8; no notifications) |
