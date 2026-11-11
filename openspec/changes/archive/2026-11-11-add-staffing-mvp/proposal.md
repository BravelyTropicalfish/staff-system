# Proposal: Staffing MVP — fill project posts from the people pool

- **Change ID:** `add-staffing-mvp`
- **Status:** proposed — G1 signed 2026-11-11 on rev 11 (`g1-decision.md`, appended below the untouched rev 8 record); P3 done (`atdd-design.md`, `design.md`, `tasks.md` — 30 tasks), next `/sdd:quality-checklist`
- **Requirement:** `requirement-workspace/requirements/REQ-001-staffing-mvp.md`
- **Impact brief:** `requirement-workspace/analysis/impact-brief.md`
- **Size:** L (one end-to-end change, six new capabilities)

## Why

The company runs several projects at once and repeatedly has to fill a new project or
an existing project's empty posts. Today the decision is made by hand: a project lead
describes what the post needs, a resource coordinator searches the people pool from
memory, and the outcome depends on who remembers which employee has the right role,
level and skills, is free for the project's dates, and accepts its travel.

That way of working produces three concrete losses:

1. **Double-booking.** Nothing records that a confirmed person is now occupied, so the
   same employee can be counted into two projects at once.
2. **Slow, inconsistent search.** Role, level, skill proficiency, occupancy and travel
   willingness are weighed differently by each coordinator, and the reasoning is lost
   as soon as the conversation ends.
3. **No shared view of the gap.** The lead and the coordinator disagree about which
   posts are still short, because the confirmed people and the remaining headcount
   live in different heads or spreadsheets.

The MVP gives both roles one flow on one dataset: the lead raises posts, the
coordinator picks from the people who qualify and recommends them, the lead confirms, and
both see who is staffed and what is still missing.

Person master data already leaves HR as a report, so the system must accept that
report as a bulk feed instead of asking the coordinator to retype every employee,
level and skill by hand.

## What Changes

A new Staffing application, built from scratch on the existing empty baseline, with
the following end-to-end behaviour:

1. The **resource coordinator** maintains the role and skill catalogs that both
   people and posts draw on. An item that anything already references can no longer be
   renamed or deleted — it is deactivated instead, so existing people and posts keep
   matching on the same item.
2. Person master data arrives through a **report**: the coordinator uploads one
   `.xlsx` workbook with a `people` sheet and a `skills` sheet, keyed by employee
   number. The file is one unit of work — every row must validate, otherwise nothing is
   written and every bad row comes back in a per-batch error list that the coordinator
   fixes and re-uploads. A workbook that cannot be read as one — a sheet missing, or no data
   row at all — is refused at upload and records no batch, so "refused" and "rejected" stay
   distinct. An unknown employee number in `people` creates the person; an
   unknown skill, a duplicated key, a missing required cell, an orphan skill row or a
   value outside the controlled sets rejects the batch. A skill left out of the report
   keeps its stored proficiency. Every cell is validated against the controlled value
   sets: five roles, four job levels, the seeded skills and five proficiency bands.
3. A project is opened by its own **lead**, or by the coordinator on that lead's behalf, and
   the coordinator also maintains **allocations**: a person occupied by a project for an
   inclusive date range. Confirmation writes an
   allocation from the post's dates and the coordinator may record one by hand; two
   hand-entered ranges may overlap, and the overlap is stored as a fact rather than refused. The
   project lead ends a project once: the project becomes `closed`, its posts are
   cancelled and everyone staffed on it is released in the same step.
4. The **project lead** raises a post on a project: role, minimum job level band, skills
   typed `required` or `bonus` with a minimum or target proficiency band, headcount,
   project dates and travel requirement.
5. For a post, the system **excludes** people who fail a hard gate (inactive, wrong
   role, level too low, a required skill missing or below the minimum, travel not
   accepted, or already allocated over dates that intersect the post's) and **lists
   everyone who passes**, ordered by name, with no score and no ranking: the list says
   "these people fit", never "this person fits best". Each row shows the gates passed and
   the skills that qualify the person with their bands; each excluded person is listed
   with the first failing gate. When the post's role has nobody at all, a **cross-role fallback**
   offers people of other roles in a separate, lower section — one job level above the
   requirement and holding a skill of the post role's skill domain at `Practitioner` or
   above.
6. The coordinator **recommends** a listed candidate. The recommendation carries the
   reason snapshot — the gates passed and the qualifying skills — taken at that moment.
7. The lead **confirms or rejects**. Confirming re-checks the active and allocation
   gates, writes an assignment and an allocation covering the post's inclusive dates,
   decrements the remaining headcount, flips the post to `filled` when the last slot is
   taken, and voids every other pending recommendation of that person whose post dates
   intersect the new allocation. Rejecting frees the
   person so the coordinator can propose someone else. Once a person is confirmed, the
   post's gate-bearing fields are locked and its headcount may only rise; the title and
   bonus rows stay editable. Cancelling a post requires releasing its confirmed people
   first.
8. Both roles get the **shared gap view**: per post, the chosen people, the filled
   count and the remaining headcount.
9. The project lead **ends the project** when the work is done: the project becomes
   `closed` for good, its posts are cancelled, pending recommendations are voided and
   everyone staffed on it is released at once, so they show up again in other projects'
   searches.

### New capabilities

| Capability | Sub-goal | Effort |
|---|---|---|
| `staffing/people-pool` | Role and skill catalogs (each skill declares the role whose domain it belongs to), employee profiles, proficiency and travel willingness as applied from the report | M |
| `staffing/people-import` | Upload the master workbook: whole-file validation, all-or-nothing apply, incremental merge, per-batch error list, batch history | M |
| `staffing/allocation` | Projects, person × project allocations with an inclusive date range, and the terminal project end that releases them | S |
| `staffing/post-requirement` | Posts with role, level, required / bonus skills, headcount, dates, travel; edit and cancel rules; open / partially filled / filled / cancelled | M |
| `staffing/candidate-matching` | Hard gates, the name-ordered suitable list with gate and skill reasons, exclusion counts, cross-role fallback tier | M |
| `staffing/staffing-confirmation` | Recommend, confirm, reject, withdraw, release; void competing recommendations; write-back of assignment and allocation | M |

No capability is MODIFIED or REMOVED: the living spec tree is empty.

### Key rules fixed by this proposal

- Roles are a catalog of five seeded values — `PM`, `BA`, `DEV`, `QA`, `UX` — and skills
  a separate catalog seeded with 后端开发, 前端开发, 项目管理, 业务分析, 测试 and UI设计.
  Both are shared by people and posts and stay extensible.
- **One person, one role.** A person holds exactly one role, stored as a single catalog
  reference and never a set. Skills are a separate catalog, so holding 测试 never makes a
  `DEV` also a `QA`; a report row that names two roles is rejected instead of split. A
  post therefore only ever sees candidates of its own role in its suitable list.
- **Cross-role fallback is a lower tier, never a relaxed gate.** When — and only when —
  no person of the post's role qualifies, the coordinator is also shown people of other
  roles that sit at least one job level above the post's requirement and hold at least
  one skill of the post role's skill domain at `Practitioner` or above. They appear in
  their own section, ordered by name like the main list and never mixed into it. The
  requirement's own role is never added to the person: the fallback is temporary
  staffing advice, not a second role.
- **Every skill belongs to exactly one role domain.** 后端开发 and 前端开发 form the `DEV`
  domain, 测试 the `QA` domain, 业务分析 the `BA` domain, 项目管理 the `PM` domain and
  UI设计 the `UX` domain; a role added later starts empty. The domain feeds the fallback
  only — it never widens G4, which stays per post and per skill.
- **Three ordered band scales.** Job level is one of `Junior` < `Senior` < `Lead` <
  `Principal`; skill proficiency is one of `Novice` < `Advanced beginner` <
  `Practitioner` < `Proficient` < `Expert`; travel willingness is one of `none` <
  `limited` < `full`. Every comparison is a rank comparison and the UI always shows the
  band name, never a number.
- **Person master data comes from a report, not from a form.** Employee number is the
  key; a blank or duplicated number is an error. The report overwrites the fields it
  carries, a skill omitted from the report keeps its stored proficiency, and nothing is
  deleted.
- **An import is all-or-nothing.** One invalid row rejects the whole batch and the pool
  keeps its previous state; the error list names every bad row, so a single corrected
  re-upload clears them all.
- Person master data has no in-app edit path. A wrong value is corrected by
  re-uploading a fixed report, so every change leaves an immutable batch record.
- **Job level is a person attribute and a post minimum.** Skill proficiency is a
  separate band on each person-skill pair and each post-skill requirement.
- **Post skills are typed.** `required` skills are hard gates; `bonus` skills are
  informational only, shown with the band held and the target. A candidate short on a
  bonus skill still passes, and the shortfall never reorders anyone. Missing a required
  skill excludes.
- **One person may be recommended for several posts.** The first confirmation wins;
  every other pending recommendation whose post dates intersect the new allocation is voided
  with reason "person taken" and listed for the coordinator, while a recommendation on a post
  whose dates do not intersect stays `proposed`.
- Travel is an ordered three-level value on both sides — `none` < `limited` < `full` —
  with no dates attached. A person qualifies when their willingness is at or above the
  post requirement; nothing else about travel is modelled.
- **An allocation occupies its dates.** An allocation covers an inclusive start and end
  date — the post's dates on confirmation, or a hand-entered range. G6 blocks any post
  whose dates intersect that range, inclusive on both ends, so an allocation ending on a
  post's start date still blocks it; a later post that does not overlap stays open. The
  project's end still releases every allocation it holds.
- Only the lead of the post's project may confirm; only the coordinator may recommend.
- A confirmed person is written back as an allocation for the post's dates, so the same
  person is automatically excluded from every post whose dates overlap it.
- **Post lifecycle.** While a post has a confirmed person, role, required job level,
  `required` skill rows, dates and travel are locked, and headcount may only rise — a
  rise above the assignment count returns the post to `partially filled`. The title and
  `bonus` rows stay editable and void pending recommendations. Cancelling a post is
  refused until its confirmed people are released; releasing — by the lead or the coordinator —
  deletes the assignment and allocation and reopens the slot. **Ending a project** is the lead's terminal action:
  the posts are cancelled, pending recommendations are voided and every allocation of
  the project is removed in one step, so ending the project is the only way to clear a
  post without releasing its people one by one. A closed project cannot be reopened.
- **Catalog items are deactivated, never deleted.** Once a person or a post references a
  role or skill, its name is frozen; the item can be switched off so new posts and
  imports no longer offer it, while every stored reference keeps working.
- **The project lead sees only recommended candidates**, with their reason snapshot —
  the gates passed and the qualifying skills. The full candidate pool, its suitable list
  and its exclusion reasons belong to the coordinator.
- **No score, no ranking.** Passing the gates is the whole decision aid: every suitable
  person is listed by name, and the system never computes a fit score, never sorts by
  preference and never names a best candidate. Adding a score later is a spec change.

### Delivery order inside the single change

The flow is linear, so tasks follow it: catalogs → report import → projects and
allocations → posts and gap view → matching → recommendation and confirmation. Each
stage is demoable against the previous one.

## Non-Goals

- **No external integration.** No HR, project-management or calendar sync in the MVP,
  and no scheduled or delivered file drops: the coordinator uploads the workbook by
  hand.
- **No hand-edited person master data.** People, roles, job levels, skills and travel
  willingness change only through an import batch.
- **No part-time staffing.** An allocation occupies the person fully for its inclusive
  date range; allocation percentage is deferred.
- **No automatic assignment.** The system never confirms a person; humans decide.
- **No notifications.** No email or chat; status changes are visible only in the app.
- **No cost, rate, budget or utilization reporting.**
- **No interview or multi-step approval chain** beyond lead confirm / reject.
- **No mobile client, offline mode or multi-company tenancy.**
- **No destination-specific travel.** Travel stays a three-level willingness.
- **No production deployment pipeline** in this change; local run and tests only.

## Impact

### Self (this change)

- Creates the whole application tree: UI, business logic (gates, the suitable list,
  write-back) and data access, kept in separate layers per `architecture-baseline.md`.
- Creates six capability delta specs under `staffing/`, plus design and tasks.
- Creates the first test suites: unit tests for gates, the suitable list order, the cross-role
  fallback tier, import validation, post lifecycle, catalog deactivation, the project end and
  write-back; E2E coverage for scenarios S1–S40 of REQ-001.
- Requires one architecture decision note for the runtime stack (see Assumptions).

### Upstream

- None. The system consumes no existing service, message or dataset; people arrive
  through the import, while catalogs, projects and allocations are entered in the app
  or loaded by a local seed script.
- Repository rules (`docs/knowledge/rules/**`) constrain the build but do not change.

### Downstream

- No existing consumer depends on this system, so nothing breaks.
- Future work that would build on it: HR master-data sync (would replace the manual
  file upload), allocation percentage and utilization reporting, notifications, and
  destination-specific travel. Each is a separate change; none is implied here.
- `requirement-workspace/registry.md` and `handoff.md` gain this change's rows.

## Assumptions

1. Single company, internal users; two roles only (project lead, resource coordinator).
   Sign-in is a lightweight account with a role, not SSO.
2. A post belongs to exactly one project, and a project has one lead. A lead sees only
   their own projects; the coordinator sees all.
3. Headcount is a positive integer; one confirmed person fills one slot fully.
4. Dates are calendar dates in one company time zone; no time-of-day handling.
5. Job level is a four-band scale and skill proficiency a five-band scale. The band
   names and their order are fixed; only the thresholds a post demands are data.
6. Runtime stack is not decided by this proposal. The working default is a
   `shadcn/ui` front end with a small typed backend and a relational database; the
   choice is recorded as an ADR under `docs/knowledge/architecture/` before
   implementation.
7. The report is one `.xlsx` workbook with a `people` sheet and a `skills` sheet. The
   exact column names of the real HR export may differ and are mapped in one import
   layer (open item D4).
8. Employee number is the person key; email is an ordinary attribute. Role and skill
   catalogs are curated in the app and the report cannot extend them.
9. The import runs synchronously at MVP scale (a few thousand rows per file), so no
   queue or background worker is introduced.
10. A lead opens a project of their own and cannot open one for another lead or move a
   post to another project; the coordinator opens a project for any lead and maintains
   the project list.
11. A skill's role domain is catalog configuration, not person data: the report never
   carries it, and changing it affects future searches only.

## Open questions carried forward (non-blocking)

| # | Question | Owner | Handling |
|---|---|---|---|
| D1 | Runtime stack and sign-in mechanism | Dev | **Closed at design (ADR-001):** React 19 + TypeScript SPA, Spring Boot 3.4 on Java 21, H2; sign-in stays a lightweight seeded account, not SSO |
| D2 | Part-time staffing / allocation percentage | Product | Deferred after MVP usage data |
| D3 | Destination-specific travel willingness | Product | Deferred after MVP usage data |
| D4 | Exact column names of the real HR report | BA / Product | Mapped in the import layer; MVP contract is the two-sheet layout above |
| D5 | Rolling back or re-applying an old batch | Product | Batches are read-only history in the MVP |
| D6 | Removing a wrong skill from a person | Product | No removal channel in the MVP; a deactivated skill is the only workaround |
| D7 | Whether a post's dates must fall inside its project's dates | Product | Not constrained in the MVP: a post carries its own start and end, and nothing blocks a range that runs past the project |
| D8 | Whether the cross-role fallback needs a cap (for example at most 3 people per post) or an approval step before a cross-role person can be confirmed | Product | Not capped in the MVP: the section is ordered by name like the main list and the coordinator decides whom to recommend; every confirmation keeps its existing re-check of active status and date overlap |

## Effort and risk

- **Size:** L. Six capabilities, one flow, one new application; roughly a
  medium-sized feature team for one iteration.
- **Main risk:** the gate thresholds are a policy choice, not a fact — a post's minimum
  level, its `required` skills and the fallback's one-band step decide who appears at all.
  Mitigation is that each decision is data on the post (or on the capability spec for the
  fallback tier), every row shows which gates it passed, and every excluded person shows
  the gate that excluded them, so a wrong threshold is visible instead of hidden behind a
  blend of factors.
- **Second risk:** one large change means a long review. Mitigation is the linear task
  order above, which keeps each stage independently demoable.
- **Third risk:** all-or-nothing means one bad cell blocks a whole upload. Mitigation is
  the complete error list, so a single correction round-trip clears the file.
- **Fourth risk:** the cross-role fallback looks like a contradiction of "one person, one
  role". Mitigation is the shape of the rule: G2 is untouched, the fallback only opens on
  an empty same-role list, it is a separate lower section, and it never writes a second
  role onto the person — it records, per recommendation, why a different role was
  proposed.

- **Fifth risk:** hand-entered allocations may overlap, so a person can look occupied on
  paper while the coordinator believes them free, and both ranges block matching.
  Mitigation is that the profile lists every allocation, current or past, with its dates, G6
  compares the ranges inclusively, and the coordinator can correct or remove a hand-entered
  one at any time; the allocation behind a confirmation moves only with its assignment.

## Definition of done for this proposal

- G1 sign-off on the six capabilities, the six eligibility gates, the date-range
  allocation and its project-end release, the cross-role fallback tier, the import
  contract and the write-back rules.
- Delta specs under `openspec/changes/add-staffing-mvp/specs/staffing/` covering
  S1–S40 as `SHALL` requirements with observable outcomes.
- Design note recording the runtime stack decision.
