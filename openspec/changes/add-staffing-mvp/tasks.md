# Tasks — add-staffing-mvp

- **Change id:** `add-staffing-mvp`
- **Inputs:** `design.md` (stack, layers, schema, API, gates) and `atdd-design.md` (82 API / 8 E2E /
  10 unit items) over the delta specs (40 requirements / 90 scenarios, G1 Pass on rev 11)
- **Order:** the proposal's delivery order — toolchain → catalogs → import → people → projects and
  allocations → posts → matching → recommendation and confirmation → acceptance gates

## Conventions

- One task is one reviewable change; it starts with the failing check, then the code
  (`testing-strategy` 1), and ends green.
- Every task is a `- [ ]` item: `[x]` means its evidence is green today, `[ ]` means it is not.
  The two sub-bullets under a task carry its layer, its blocking tasks and the evidence that
  closes it, so a task never needs a second lookup.
- A task may not add scope: a check without a scenario is a spec problem, not a test problem
  (`atdd-design.md` §5).
- Each task updates nothing outside its capability except the files it makes obsolete
  (`coding-and-refactoring` 5).
- E2E is written in the task that owns the screen; single-field validation stays at the API layer.
- In the Evidence line a scenario in italics is the **primary** check for that task; a trailing
  `(payload)` marks a companion assertion of a scenario whose primary layer is E2E, so the split
  stays 82 API + 8 E2E and no scenario is counted twice.
- No task designs a unit-test suite: unit tests are written with their code, per `design.md` §9.

## Stage 0 — toolchain and skeleton

- [x] **T01** — Generate the Maven Wrapper pinned to 3.9.9 and a Boot 3.4 skeleton on Java 21; verify `mvnw -v`
  - Layer: `build` · Blocked by: —
  - Evidence: wrapper and compiler report 3.9.9 / 21; build is green with no test

- [x] **T02** — Add the Spring Boot app: web, validation, data-jpa, security, H2 (file `dev`, in-memory `test`), `schema.sql`, `ddl-auto=validate`, health route
  - Layer: `API` · Blocked by: T01
  - Evidence: `mvnw verify` green; `GET /api/health` 200

- [x] **T03** — Add the front end: Vite + React + TS + Tailwind + shadcn/ui + React Router, `/api` proxy to the back end
  - Layer: `UI` · Blocked by: —
  - Evidence: `npm run build` green; the app renders an empty shell

- [x] **T04** — Seed the schema (V1): roles, skills, people, person_skill, project, post, post_skill, assignment, allocation, recommendation, notice, import_batch, import_error, app_user; five roles, six skills with domains, one coordinator and two leads
  - Layer: `data` · Blocked by: T02
  - Evidence: `schema.sql` applies; seed rows visible; one role per person enforced

- [x] **T05** — Auth: session login/logout, hashed demo passwords, role and project-scope guards on every route group
  - Layer: `API` · Blocked by: T04
  - Evidence: 401 unauthenticated (**green** with the session slice); 403 for each role/scope violation, state unchanged — each route group proves its own, so the box closes with the last group (T23)

## Stage 1 — catalogs (`people-pool` 1–4)

- [x] **T06** — Catalog API: roles and skills, create, rename, deactivate; rename and delete refused once referenced; domain readable per role and changeable
  - Layer: `API` · Blocked by: T05
  - Evidence: *Seeded roles are the only choices at first use*, *A new role joins the catalog*, *Skill picker offers the seeded set*, *The seeded mapping is readable per role*, *A role added later has an empty domain*, *A domain change reuses stored data untouched*, *Delete and rename are both refused on a referenced skill*, *Deactivation hides an item from new work only*, *Rename is allowed before the first reference*

- [x] **T07** — Catalog screen for the coordinator: two tables, deactivate toggle, rename blocked with the reason
  - Layer: `UI` · Blocked by: T06
  - Evidence: the screen renders the seeded rows; a refused rename shows the reason

## Stage 2 — import (`people-import`)

- [x] **T08** — Workbook model and validators (POI): sheet presence, row presence, controlled sets, lower-case normalisation, duplicate employee number, duplicate skill row, orphan skill row, multi-role cell, `ColumnMap` for the real export
  - Layer: `unit` · Blocked by: T04
  - Evidence: unit tests for each rule; a multi-role cell is one row error, never two roles

- [x] **T09** — Import endpoint: atomic apply, `APPLIED` / `REJECTED` batch with the full error list, no batch when unreadable, reactivation, omitted skill keeps its band, staffing state untouched
  - Layer: `API` · Blocked by: T08
  - Evidence: *Skills-only refresh is accepted*, *A workbook with one sheet is refused*, *An empty workbook is refused*, *One bad row blocks the whole file*, *The corrected file is a new batch*, *A rejected batch stays readable*, *Lower case is normalised, not rejected*, *A role outside the catalog blocks the file*, *A multi-role cell is a row error, never two roles*, *A duplicated skill row blocks the file*, *An orphan skill row blocks the file*, *A duplicated employee number blocks the file*, *New and existing people in one file*, *An inactive person is reactivated by the report*, *Allocations survive an import*

- [x] **T10** — Import screen: upload, batch history, per-batch error list
  - Layer: `UI` · Blocked by: T09
  - Evidence: the refused-vs-rejected distinction is visible; errors name the row and the sheet

## Stage 3 — people (`people-pool` 5–8)

- [x] **T11** — Profile API: person, one role, skills with bands, every allocation current or past marked as ended; read-only; band names only
  - Layer: `API` · Blocked by: T09
  - Evidence: *A person holds exactly one role*, *Holding a skill grants no second role*, *A band is overwritten, not duplicated*, *An omitted skill keeps its band*, *Rank ordering drives comparisons*, *A band is displayed, never a number* (payload), *Profile lists a person's occupancy* (payload)

- [x] **T12** — People list and profile screen: read-only profile, band names, occupancy including ended ranges
  - Layer: `E2E` · Blocked by: T11
  - Evidence: *The profile is read-only*, *A band is displayed, never a number*, *Profile lists a person's occupancy*

## Stage 4 — projects and allocations (`allocation`)

- [x] **T13** — Project API: create by the coordinator only, list scoped to the lead, end-project transaction (posts cancelled, pending recommendations voided with `post cancelled`, allocations deleted, assignments released, project closed, not reopenable)
  - Layer: `API` · Blocked by: T05
  - Evidence: *A lead cannot create a project*, *The end releases everyone and cancels the posts* (payload), *Only the project's lead may end it*, *A closed project cannot be reopened*

- [x] **T14** — Allocation API and the intersection rule: manual create/edit/delete, confirmation-sourced refused for hand edits, overlapping manual ranges stored as facts
  - Layer: `API` · Blocked by: T13
  - Evidence: *Inverted dates are refused*, *A hand-recorded allocation is stored*, *A confirmed allocation cannot be removed by hand*, *Two overlapping allocations coexist*, *A boundary day still blocks* (unit), *A later post stays open* (unit)

## Stage 5 — posts (`post-requirement`)

- [x] **T15** — Post API: create and validate (role active on creation and on a role change, fractional headcount refused, typed skills, optional skills), status derivation, gate-bearing lock while confirmed, headcount may rise but not fall below the confirmed count, editable changes void pending recommendations, cancel refused while confirmed, cancelled is read-only, actor rules
  - Layer: `API` · Blocked by: T14
  - Evidence: *A valid post is created open*, *A fractional headcount is refused*, *A deactivated role does not block the title*, *Required and bonus rows are distinguished*, *A post without skills is valid*, *Filling the last slot*, *An inactive confirmed person keeps the post filled*, *Dates cannot move under a confirmed person*, *The lock spreads to every gate-bearing field*, *Raising the headcount reopens a filled post*, *Lowering below the confirmed count is refused*, *A bonus target change voids only pending recommendations*, *Cancel is refused, then allowed*, *A coordinator cannot edit a post*

- [x] **T16** — Post screens: create and edit form, gap view per post and per project
  - Layer: `UI` · Blocked by: T15
  - Evidence: the gap numbers match the assignments; a locked field is disabled with its reason

- [x] **T17** — Project screen: project list, detail with posts and gap numbers, end action with its confirmation
  - Layer: `E2E` · Blocked by: T13, T16
  - Evidence: *The end releases everyone and cancels the posts*

## Stage 6 — matching (`candidate-matching`)

- [x] **T18** — Gate engine with the fixed order G1→G6 and the first failing gate reported; `G6` closed-interval intersection
  - Layer: `unit` · Blocked by: T14
  - Evidence: unit tests per gate and per boundary day (boundary day blocks, later post stays open)

- [x] **T19** — Suitable list API: every passer listed, name order with the employee-number tie-break, no score field, exclusions with their first failing gate, empty list
  - Layer: `API` · Blocked by: T18
  - Evidence: *The first failing gate is the reported reason*, *A bonus shortfall never excludes*, *Role equality is by identity*, *Only an intersecting range blocks*, *Everyone who passes is listed, with no score*, *A bonus shortfall is shown, not applied*, *The order comes from the name, not the fit*, *Nothing passes the gates*, *A lead cannot open the suitable list*

- [x] **T20** — Cross-role fallback tier: opens only on an empty same-role list, X1 level +1, X2 domain skill at `Practitioner` or above, own array with evidence, empty domain never widens
  - Layer: `API` · Blocked by: T19
  - Evidence: *The fallback opens when the post role has nobody*, *One same-role candidate closes the fallback*, *A person at the required band is not above it*, *A skill outside the post role's domain never admits*, *The top band has no cross-role candidate*, *Cross-role evidence travels with the recommendation*

- [x] **T21** — Candidate screen: two sections, gate results and qualifying skills per row, exclusion list with the first failing gate
  - Layer: `UI` · Blocked by: T20
  - Evidence: the fallback section appears only when the main one is empty

## Stage 7 — recommendation and confirmation (`staffing-confirmation`)

- [x] **T22** — Recommend API: snapshot of gates and skills, refused on a filled or cancelled post, cross-role evidence carried
  - Layer: `API` · Blocked by: T20
  - Evidence: *A recommendation carries its snapshot*, *A filled post accepts no recommendation*, *A cross-role recommendation carries its evidence*, *Two proposals for one person*

- [x] **T23** — Confirm / reject / withdraw: re-check G1 and G6 under one transaction that locks the post row and then the person row, write assignment and allocation, decrement headcount, date-scoped voiding with `person taken`, lead-only and own-project-only, deactivated person or late allocation blocks
  - Layer: `API` · Blocked by: T22
  - Evidence: *Confirmation writes one allocation for the post's dates*, *A person deactivated after the proposal*, *An allocation recorded after the proposal*, *Another lead's post is out of reach*, *Confirmation is visible to both roles* (payload), *The confirmed person leaves other searches* (payload), *The other pending proposal is voided*, *A concurrent double confirmation*, *A later posting succeeds, an overlapping one waits*, *A rejection frees the candidate*, *The coordinator withdraws a proposal*

- [x] **T24** — Release and notices: release deletes the assignment and allocation and reopens the slot; every void writes a notice to the coordinator; notices stay until dismissed
  - Layer: `API` · Blocked by: T23
  - Evidence: *Release returns the post to the short list*, *The notice list explains each loss* (payload), *An entry stays until it is dismissed*

- [x] **T25** — Confirmation screens: recommended candidates on the post, confirm and reject, release, notice list, gap view for both roles
  - Layer: `E2E` · Blocked by: T24
  - Evidence: *Confirmation is visible to both roles*, *The confirmed person leaves other searches*, *The notice list explains each loss*, *Both roles see the same gap numbers*

## Stage 8 — acceptance, NFR and gates

- [x] **T26** — Playwright setup: config, two-account storage states, fixture seeding through the API, workbook fixture builder for the import
  - Layer: `E2E` · Blocked by: T03, T09
  - Evidence: a smoke test logs in as each role; fixtures reset the database per run

- [x] **T27** — Journeys J1 (import → project → post → search → recommend → confirm → gap) and J2 (project end → release → reappear in a later search); full suite green with a scenario-to-test traceability matrix
  - Layer: `E2E` · Blocked by: T12, T17, T25
  - Evidence: J1 and J2 pass; every one of the 90 scenarios resolves to a test id

- [x] **T28** — Idempotency NFR: identical re-upload adds a batch and changes no state; concurrent double confirmation leaves one assignment; double release refused and unchanged
  - Layer: `API` · Blocked by: T23, T24
  - Evidence: three checks, each asserting row counts

- [x] **T29** — Non-goal reverse checks: the ten rows of `atdd-design.md` §6
  - Layer: `API / config` · Blocked by: T27
  - Evidence: ten checks, one per Non-Goal

- [x] **T30** — CI and verification: `.github/workflows/ci.yml` with the five required checks (lint, type check, unit, acceptance, dependency and security scan), JaCoCo 100% gate, `docs/feedback/verification-commands.md` filled, run notes in the change
  - Layer: `CI` · Blocked by: T27
  - Evidence: the workflow runs on a pull request and holds no deploy job; the commands file is no longer a placeholder

## Definition of done per task

1. The failing check exists first and passes afterwards; no skipped or weakened test
   (`testing-strategy` 4).
2. Coverage of the touched `domain/` and `api/` code is 100%, or the exemption is recorded in the
   pull request with reviewer approval.
3. The task names the scenarios it closes; `T27`'s matrix shows no gap.
4. No dead code, no commented-out block, no unrelated edit (`coding-and-refactoring` 5–7).

## Out of scope for this change

Deployment, containers, Postgres, notifications, part-time allocations, HR/travel integrations,
batch rollback and skill removal (D5/D6), a fallback cap or approval (D8), and any autoscoring or
ranking language.

## Alignment self-check (Spec ↔ Design ↔ Tasks ↔ ATDD)

| Check | Result | Evidence |
|---|---|---|
| Every capability has a design section, tasks and an ATDD layer | Pass | `people-pool` §3/§4 + T06–T12; `people-import` §7 + T08–T10; `allocation` §3/§6 + T13, T14, T17; `post-requirement` §3 + T15, T16; `candidate-matching` §5 + T18–T21; `staffing-confirmation` §6 + T22–T25 |
| Scenario counts match the specs | Pass | 40 requirements / 90 scenarios split 82 API + 8 E2E; the 10 unit items stay apply-time |
| All 8 E2E scenarios are owned by a task | Pass | T12 (3), T17 (1), T25 (4) |
| ATDD non-goals have reverse checks | Pass | T29 carries all ten rows |
| NFR scope (authorisation, idempotency) has tasks | Pass | T05 + per-route guards; T28 |
| Rules gates are wired | Pass | T30 creates the five checks and fills `verification-commands.md` |
| Design records the stack decision | Pass | `design.md` §1 + `adr-001-runtime-stack.md` |
| No task contradicts the ATDD design | Pass | E2E only for the 8 cross-screen scenarios; no UT suite design; no POM or testcase generator |
| Dependencies form a delivery DAG | Pass | every `Blocked by` points backwards; T01/T03 are the only roots |
| No production code written in this step | Pass | tasks and design only |

**Verdict: Pass.** Next: `/sdd:quality-checklist` (P3), then `/sdd:atdd-apply`.

**Carried open items:** D4 (real HR column names) stays behind `ColumnMap`; D5/D6 unchanged;
D7 unconstrained; D8 keeps its MVP default. The Maven 3.0.4 → wrapper decision is the only
environment prerequisite.

## Apply progress (P4)

| Date | Task | Result |
|---|---|---|
| 2026-11-11 | T01 | `mvnw -v` reports Maven 3.9.9 on Java 21; `mvnw verify` green with no test |
| 2026-11-11 | T02 | `HealthApiTest` 1/1; `GET /api/health` 200 `{"status":"UP"}` |
| 2026-11-11 | T03 | `npm run build` green; no browser render check yet (T26/T27 owns it) |
| 2026-11-11 | T04 | 14 tables apply over `schema.sql`; seed 5 roles / 6 skills with domains / 3 accounts; `SchemaSeedTest` 6/6, incl. a second seed run changing no count |
| 2026-11-11 | T05 (session slice) | `SessionApiTest` 7/7: sign in, read session, sign out, 401 on every other route, no internals leaked; the 403 halves stay with their route groups |
| 2026-11-11 | T06 | `CatalogApiTest` 14/14: seeded 5 roles / 6 skills with their domains, a new role with an empty domain, rename before the first reference, delete always 409, deactivation leaves stored rows alone, a domain move keeps people untouched, lead 403 on every write and 200 on reads |
| 2026-11-11 | T08 | `WorkbookParserTest` 8/8 and `ImportValidatorTest` 10/10: sheets, headers, empty file, blank rows, no splitting of a multi-role cell, normalised spelling, every row rule with its row number |
| 2026-11-11 | T09 | `ImportApiTest` 17/17: applied and rejected batches, shape refusal without a batch, atomic refusal, `REJECTED` history stays readable, reactivation, skills-only refresh, allocations untouched, lead 403, unknown batch 404 |
| 2026-11-11 | T11 | `PeopleApiTest` 9/9 and `BandsTest` 4/4: one role per person with a foreign-domain skill adding none, band overwritten in place, omitted skill keeps its band, current and past allocations with the ended one marked, band names on every payload, no write method answers (405), lead 403 on both routes, unknown employee 404 |
| 2026-11-11 | T13 | `ProjectApiTest` 9/9: create refused for a lead and granted with its range for the coordinator, inverted dates 422 with nothing stored, unknown or non-lead account 422, list scoped to the owning lead, end closes the project and cancels every post while voiding the pending recommendation with `post cancelled` and its notice, allocations and assignments gone, coordinator and other lead 403, second end 409, no reopen route, unknown project 404 |
| 2026-11-11 | T14 | `AllocationApiTest` 9/9 and `RangesTest` 5/5: hand record stored and editable, inverted dates 422 with nothing stored, two overlapping ranges coexist, a confirmation-sourced range answers 409 `release first` on both edit and delete with its dates intact, lead 403 on all three verbs, unknown person or project 422, closed project 409, unknown allocation 404; the predicate blocks on a shared boundary day and on the post's end touching the allocation's start while a later range stays open |
| 2026-11-11 | T15 | `PostApiTest` 15/15 and `PostStatusTest` 4/4: a valid post open with its range, fractional headcount 422 with nothing stored, a deactivated role keeps the title saving but refuses a move to another deactivated role, required and bonus rows carry kind and band with the bonus labelled informational, a post without skills valid, gap derives `partially filled` then `filled` and drops the post from the still-short list, an inactive person keeps the seat filled and is flagged, the gate-bearing set (role, level, required row, start, end, travel) refused with `release first` naming the person and nothing changing, headcount up reopens, down below the confirmed count refused, a bonus target change voids only the pending recommendation with `post changed` and its notice, cancel refused then allowed with `post cancelled` then read-only, coordinator reads but 403 on every write, other lead 403 on reads and writes |
| 2026-11-11 | T18 | `GatesTest` 18/18: the enum walks G1→G6 in the spec's order, one evaluation reports per-gate results and admits a person passing all six, G1 to G5 each refuse with their own reason (inactive, another role, level below the requirement with the required band itself passing, a required skill missing or under its band with every required row demanded and a post without rows asking nothing, travel under the requirement with the equal rank passing), role equality is the row id and not the name, the walk stops at the first failure so a person below the level and under the skill reports the level, and G6 blocks on an allocation ending on the post's start day and on one starting on its end day while a post before or after the allocation stays open |
| 2026-11-11 | T19 | `CandidateApiTest` 10/10: `GET /api/posts/{id}/candidates` answers the coordinator only and 404 for an unknown post, a role-deactivated post returns the same suitable list with the other role in neither list, the first failing gate is the reported reason with its count, a bonus shortfall is listed with the band held against the target while every gate count stays zero, only an allocation sharing the post's dates excludes while a later post lists the person, three passers come back with role, level, travel and all six gates and no score or rank key, the order is the name then the employee number even when the later name carries the larger surplus, a pool under the travel requirement answers an empty list with the G5 count, and an inactive person is counted under G1 |
| 2026-11-11 | T20 | `CrossRoleApiTest` 6/6: the tier opens only on an empty same-role list and says `no QA person passes the gates`, X1 refuses a person at the required band and, at the top band, says no band sits above `Principal`, X2 refuses the highest band of another domain, one same-role candidate closes the tier and leaves the X counts at zero, and every row carries the role, the level, the bands above and the qualifying skill while the stored role stays `DEV`; `mvnw verify` 152 |
| 2026-11-11 | T21 | `npm test` 5/5 (the screen's own readings, `node --test`) and `npm run build` green: the cross-role section shows only on an empty main list and stays hidden even when the payload still carries rows, an empty tier still shows its reason, the gate strip carries all eight codes with zeros for the ones that did not run, a band is named with the bonus target, and the gap reads `+1 band` / `+2 bands`; the screen lives at `/posts/:id/candidates`; no browser render check yet |
| 2026-11-11 | T22 | `RecommendationApiTest` 9/9: a suitable person is stored `proposed` with the six gates and the post's skill row at the band held, and the answer echoes the `snapshot_json` the database holds; a filled post answers 409 `post filled` and a cancelled one 409 `post cancelled`, both storing nothing; a DEV admitted by the fallback carries `G1, G5, G6, X1, X2` plus the role, the one-band gap and 测试 at `Practitioner` in its snapshot while the stored role stays `DEV`; `E100` proposed on two projects leaves two `proposed` rows and no assignment or allocation; the same person twice on one post answers 409 `already proposed`, a person deactivated after the listing 409 with the `G1` reason, a cross-role person while the tier is closed 409 `not a candidate`, an unknown person 404, and a lead 403 for the coordinator's route; `mvnw verify` 161 |
| 2026-11-11 | T23 | `ConfirmationApiTest` 11/11: a confirmation writes one assignment and one `CONFIRMATION` allocation over the post's dates, marks the recommendation `confirmed` with its snapshot untouched, answers `partially filled` with `remaining 1`, and leaves the person visible under the post for both roles; the confirmed person leaves an intersecting search with the allocation reason while a disjoint post still lists them; the competing proposal on intersecting dates becomes `voided` `person taken` with its notice while the disjoint one stays `proposed`; a person deactivated after the proposal answers 409 `G1`, a late allocation 409 `person taken`, and both leave it `proposed` with no assignment; another lead and the coordinator answer 403; two threads confirming one person on intersecting posts yield one assignment, one allocation, one `confirmed` and one `voided`, the loser answering 409 `person taken`; after an out-of-band occupancy is removed the same confirmation succeeds; a rejection stores its note and frees the person, and the coordinator withdraws only their own proposal; `mvnw verify` 172 |
| 2026-11-11 | T24 | `ReleaseApiTest` 4/4 and `NoticeApiTest` 3/3: a release deletes the assignment and its allocation, answers `open` with `remaining 1`, puts the post back in the gap as `stillShort` and lets the person pass G6 on a post in another project; releasing one of two seats leaves `partially filled` with the other person still listed; another lead answers 403 and the owning lead and the coordinator both release; a second release answers 404 and changes nothing. The notice list shows the three reasons with post, person, reason and time each, newest first; an entry backdated ninety days is still listed with its original time, a dismissal removes it and answers `already dismissed` on the second try, an unknown id answers 404, and a lead is refused 403 on both routes; `mvnw verify` 179 |
| 2026-11-11 | T16 | `npm test` 12/12 (the two screens' own readings, `node --test`) and `npm run build` green: the create form sends the catalog's own role, level and travel through `POST /api/projects/{id}/posts` and lands on the saved post, the edit form starts from `GET /api/posts/{id}` and patches the same body back, a blank title is sent as no title and the headcount as a number, a confirmation freezes the six gate-bearing fields with the reason naming the people by count while the headcount keeps a floor at the confirmed count, and the gap panel shows the server's three numbers as `x of y chosen · z still needed` with each seat naming the person, the day and the deactivation; `GET /api/posts/{id}` answers the post's own lead and the coordinator and 403 to another lead, `GET /api/bands` is readable by both roles (`PostApiTest` 17/17, `CatalogApiTest` 15/15); `mvnw verify` 182; no browser render check yet |
| 2026-11-11 | T25 | `npm test` 18/18 (`candidates` 5, `post` 7, `confirmation` 6) and `npm run build` green: the post screen splits open proposals from the history keeping the server order, a voided row names its reason, the lead is offered confirm and reject while the coordinator is offered withdraw and a closed row none, the snapshot reads as the gates it passed plus the evidence behind it (the same-role skill rows or the cross-role band gap with the qualifying skill), a notice line names its post, person, reason and minute, and a seat releases by its assignment id; `GET /api/posts/{id}/recommendations` answers the coordinator every proposal with its snapshot, the post's own lead the same rows with `snapshot` null, another project's lead 403 and an unknown post 404 (`RecommendationApiTest` 11/11); the coordinator's `/notices` screen lists each loss and dismisses it; `mvnw verify` 184; no browser render check yet |
| 2026-11-11 | T05 (closed) | All 31 `HttpStatus.FORBIDDEN` assertions across the eleven route-group suites are green (catalogs 3, import 2, people 2, projects 3, allocations 3, posts 9, candidates 1, recommendations 2, confirmation 3, release 1, notices 2), so every guard group now proves its own 403 with the state unchanged; with `SessionApiTest` 7/7 for the 401 half the box closes |
| 2026-11-11 | T07 | `npm test` 22/22 (`candidates` 5, `post` 7, `confirmation` 6, `catalog` 4) and `npm run build` green: `/catalogs` reads `GET /api/roles` and `GET /api/skills` into two tables, a role names its domain as the skill names or `no domain skills`, the toggle offers the state the item is not in, a rename sends the trimmed `name` alone so a refusal leaves the row as it was, a 409 prints `code: message` (`referenced` for a referenced item), and a lead sees the same rows with the controls disabled while the coordinator gets them; the shell now links `/catalogs` and `/notices` instead of promising them; `GET /api/session` drives the role; no delete button because the delete route answers an empty body and always 409 `delete refused`; no browser render check yet |
| 2026-11-11 | T10 | `npm test` 28/28 (`candidates` 5, `post` 7, `confirmation` 6, `catalog` 4, `imports` 6) and `npm run build` green: `/imports` offers one `.xlsx` and one Upload button to the coordinator alone, a refusal reads `refused · code: message · no batch was recorded` (that sentence exists only for the refusal) while a rejected batch reads `rejected · nothing was written · n rows to fix`, and the history lists filename, status, uploader, minute, batch id and its counts with every refused row printed as `sheet row n · key — reason`; the history answers counts only, so each `REJECTED` batch is read again by `GET /api/imports/{id}` for its rows; `sendForm` is the multipart member of the one fetch wrapper; no browser render check yet |
| 2026-11-11 | T12 | `npm test` 33/33 (`candidates` 5, `post` 7, `confirmation` 6, `catalog` 4, `imports` 6, `people` 5) and `npm run build` green: `/people` lists the pool by employee number with the one role, the job level, the travel band and the import's active word, and each name links to its profile; `/people/{employeeNo}` renders the master data, every band as `skill · band · domain` — a name, never a number, which the test pins by refusing a digit — and every occupancy as `project · start → end · confirmed seat|hand record`, a past range kept and only marked `· ended`; the screen draws no input and no button at all, because person data arrives by import and a field would be a write the server does not have, and the note under the name says exactly that; 403, 404 and a missing session print as their own sentence; no browser render check yet |
| 2026-11-11 | T28 | `IdempotencyApiTest` 3/3, each check a row count: the same workbook applied twice leaves two `APPLIED` batches with distinct ids while `person` and `person_skill` keep one row each and the person row reads back identical, band unchanged at `Expert`; two threads confirming one person on intersecting posts leave one assignment, one `CONFIRMATION` allocation, one `confirmed`, one `voided` and one `person taken` notice, the loser answering 409 `person taken`; a release answers `open` with `remaining 2` and a second release with the same id answers 404 with zero assignments and zero allocations, the post still `open` and the proposal still `confirmed`; `mvnw verify` 187 |
| 2026-11-11 | T26 | Playwright 1.65 and `exceljs` installed as dev dependencies; `web/playwright.config.ts` starts both servers itself — the backend through `mvnw spring-boot:run` with `cwd: ../backend` and `--spring.datasource.url=jdbc:h2:mem:e2e` (so the schema and the seed are rebuilt on every run, nothing outlives a run) and the Vite dev server on 5173, whose `/api` proxy reaches it; a setup project signs in as coordinator, lead.one and lead.two and writes one `storageState` per account (the tasks said two accounts; the second lead exists because a test must prove a project is not his), and the e2e project reuses them — a cookie ignores the port, so the session from 8080 is valid on 5173; `e2e/support/workbook.ts` writes both sheets with `exceljs` under the column names `WorkbookParser` reads and the words `Bands` accepts, `fixtures.ts` seeds only through the public routes (`POST /api/imports` 201, `POST /api/projects` 201, `POST /api/projects/{id}/posts` 201, `POST /api/posts/{id}/recommendations` 201, `POST /api/recommendations/{id}/confirm` 200) so a fixture cannot build what the server refuses, and `smoke.spec.ts` reads each account's role back from `GET /api/session` and opens `/` as lead.one to see the shell's links; `npx playwright test` 5/5 (3 setup + 2 spec, 21.9s), `npm test` still 39/39 and `npm run build` green now that `tests` and `e2e` sit inside `tsc --noEmit`; no journey or scenario spec yet (T27 owns them) |
| 2026-11-11 | T27 | `npx playwright test` 15/15 (3 setup + 2 journeys + 8 scenarios + 2 smoke, 38.8s): `scenarios.spec.ts` holds the eight browser-only checks the ATDD design names and each one proves its own claim — the end leaves `closed · every post is cancelled and every allocation of this project is gone`, every post `cancelled` and the coordinator's profile of the released person reading `No allocation is recorded for this person.`; the profile draws no `input, textarea, select, button` at all and its skills section contains no digit; the occupancy list shows the past range `· ended` beside the current one; a confirmation reads `E2E Person <tag> (E2E-<tag>) · confirmed` on lead.one's screen and the coordinator's alike, and `1 of 2 chosen · 1 still needed` on both; the confirmed person is absent from `Suitable people` on a later post and present in `Excluded people` with `G6` and `intersects the post`; `/notices` carries the losing post's title with `person taken`. `journeys.spec.ts` runs J1 (import 1 person at `/imports` → seeded project and post → the person reads as suitable → seeded proposal → the lead confirms on `/posts/{id}` until `1 of 1 chosen · filled` → the same gap on `/projects/{id}`) and J2 (the lead ends the first project twice on its screen → the coordinator's later search lists the released person again). `openspec/changes/add-staffing-mvp/traceability.md` maps every scenario: 90 rows = 82 API + 8 E2E, per capability 11/15/15/17/14/18, 105 distinct API ids and all 8 E2E titles checked by script against the sources. The journeys found one product hole, recorded here rather than papered over: no screen posts a recommendation, so J1 seeds that step through `POST /api/posts/{id}/recommendations` |
| 2026-11-11 | T29 | `NonGoalApiTest` 9/9, one test per Non-Goal row of `atdd-design.md` §6. 1: no `@EnableScheduling` class and no `@Scheduled` method anywhere under `com.example.staffing` (the class files are read with `SimpleMetadataReaderFactory`, never loaded), and `PATCH`/`POST`/`DELETE /api/people/E100` answer 405 with `job_level` still `Senior`. 2: a hand band edit answers 405 and `person_skill.band` stays `Novice`. 3: `percentage: 50` on `POST /api/allocations` answers 400 `unreadable body` and the allocation count stays 0. 4: a suitable candidate leaves the post `open` / `confirmed 0` / `remaining 1` with zero assignments and zero allocations. 5: `JavaMailSender` and `SimpMessagingTemplate` are absent from the classpath and `POST /api/notices/1/send` answers 404. 6: `hourlyRate` answers 400 and the post view carries no key matching rate/cost/budget/utilisation. 7: `PATCH /api/recommendations/{id}` answers 405 with the row still `proposed`, and one `POST .../confirm` turns it `confirmed` — no intermediate state exists. 8: `/api/people?tenant=acme` answers the same body as `/api/people`. 9: `travelDestination` answers 400 while the view reads `travel: none` and carries no destination key. Row 10 is the review: `.github/workflows/ci.yml` holds the five checks and no deploy job. Disclosure: to make "refused as an unknown field" true rather than silently ignored, `spring.jackson.deserialization.fail-on-unknown-properties=true` was added to the main **and** the test `application.properties` — the test file shadows the main one on the test classpath — and `ApiExceptionHandler` maps `HttpMessageNotReadableException` to 400 `unreadable body`; `mvnw verify` 280 tests, lines 1489/1489, branches 537/537 |
| 2026-11-11 | T30 | `.github/workflows/ci.yml` runs on `pull_request` and on a push to `main`, with `permissions: contents: read` and five jobs: `lint` (`npm ci` + `npm run lint`), `type-check` (`npx tsc --noEmit` + `./mvnw -B -DskipTests compile`), `unit` (`npm test` + `./mvnw -B test`), `acceptance` (`./mvnw -B verify` plus a Playwright chromium run, uploading the JaCoCo and Playwright reports for 30 days) and `scan` (`npm audit --audit-level=high` + `actions/dependency-review-action@v4` on pull requests); there is no deploy job and no secret. The repo had no linter, so ESLint 9 with `typescript-eslint` was added (`web/eslint.config.js`, `npm run lint` 0 problems) instead of a check that always passes; `web/playwright.config.ts` now picks `mvnw.cmd` on Windows and `./mvnw` elsewhere so the acceptance job's web server starts on Linux. JaCoCo `check` runs at `verify` with one `PACKAGE` rule over `com.example.staffing.domain*` and `com.example.staffing.api*` at `COVEREDRATIO 1.0` for LINE and BRANCH — proven live by temporarily raising the minimum to `1.000001`, which failed the build, then restoring `1.0`, which prints "All coverage checks have been met." (the dotted package form is required: a slash path matched no package at all and the gate passed vacuously). `docs/feedback/verification-commands.md` is no longer a placeholder: static, runtime and system layers plus the table mapping the five required checks to their CI job. Two first-run failures were fixed rather than papered over: the lockfile resolved every package from the internal Artifactory, which a runner cannot reach, so all five jobs failed at `npm ci` — the 455 `resolved` URLs now point at `registry.npmjs.org` with the hashes untouched; and `actions/dependency-review-action` refuses a private repository without Advanced Security, so the repository was made public and the step now runs on every pull request rather than being skipped or softened. `npm test` 39/39, `npm run build` green, `npm audit --audit-level=high` exit 0 (9 moderate), `mvnw verify` 280 tests; after the `MAVEN` switch the suite was re-run on Windows and stayed 15/15 (40.1s), with `npm run lint` 0 problems |
| 2026-11-11 | T31 | Added after G2, from a manual walkthrough of the running application rather than from a plan. The application had no door: no screen posted `POST /api/session`, so every screen answered `unauthenticated: Sign in to continue.` and the only way in was calling that route by hand — REQ-001 §2 calls sign-in "a lightweight seeded account with a role", and a seeded account nobody can reach is not a product. `/login` now posts that route and returns to the shell (`web/src/screens/LoginScreen.tsx`, `web/src/lib/session.ts`, linked from the shell and declared in `App.tsx`), and the screens that kept their `Reading …` placeholder after a refused read now drop it once `error` is set (`PeopleScreen`, `PersonScreen`, `ProjectsScreen`, `NoticesScreen` and both tables of `CatalogScreen`) — a lead opening `/people` used to show `forbidden: This action is not yours to take.` and `Reading the pool…` together. Evidence: `npm test` 41/41 with `tests/session.test.ts` added to the `test` script, `npx playwright test` 18/18 including three new specs in `e2e/login.spec.ts` (a signed-out screen names the reason and the form opens it, an empty form is refused without a request, a forbidden screen keeps its reason and stops claiming to read), `npm run lint` 0 problems, `npx tsc --noEmit` clean, `npm run build` green. The walkthrough also proved the product against the seeded accounts once the state was built through the public routes (import 6 people / 7 bands, two projects, two posts, one hand allocation, one proposal): the DEV post listed `李四 E1004 · 林一 E1001 · 陈二 E1002` as suitable and excluded `张三 G3 the job level Junior is below the required Senior`, `王五 G6 the allocation 2027-03-01 to 2027-03-31 intersects the post's 2027-02-01 to 2027-06-30`, `赵六 G1 the person is not active`; the QA post answered `0 suitable` and opened the cross-role fallback with only `李四 · DEV · +1 band · 测试 · Practitioner` |
| 2026-11-11 | T32 | The UI was built screen by screen with no visual contract, so it never matched the prototype the reviewers clicked through: the prototype draws a 232px sidebar (brand + grouped links + footnote), a 56px top bar (breadcrumb + account chip), a full-width view with `padding: 14px 24px` and no max-width, cards at `radius 0.6rem`, KPI tiles and a gate strip; the app had a centred `max-w-4xl` column per screen and no chrome at all. `design.md` §8 never required the match, which is the real defect — this task removes the drift. `web/src/index.css` now carries the prototype's tokens and class names (`.app/.sidebar/.topbar/.view/.card/.card-head/.card-body/.btn/.badge/.kpi/.gate-grid/.kv/.banner/.login-*`), `web/src/components/AppShell.tsx` renders the chrome around every screen through a layout route, `web/src/lib/nav.ts` holds the grouped links (`Delivery`, `People`, `Data`, `Inbox`) filtered by the role the server named, the breadcrumb per route and the active-link rule, and each screen lost its own width/margins: `/projects/:id` gained three KPI tiles (`Posts` 2 · `Seats chosen` 0 of 3 · `Seats still needed` 3) over a `grid-2` of posts and gap, `/posts/:id/candidates` gained the eight-cell gate strip, and `/login` became the prototype's centred card. Verified by measurement, not by eye (screenshots are unavailable on this machine): `.sidebar` 232px + `.topbar` 56px + `.view` 1342px at a 1574px viewport, `scrollWidth === clientWidth` on `/`, `/people`, `/imports`, `/catalogs`, `/projects/1`, `/posts/1/candidates`; `document.querySelectorAll('.gate-cell').length` is 8 with the first reading `1 / G1 active`; zero console errors on all seven screens. Tests: `npm test` 46/46 (five new readings in `tests/nav.test.ts` — role filtering, signed-out map, active link, breadcrumb, role word/initial), `npx playwright test` 18/18 (the shell spec now asserts a lead is offered `Projects and their posts` and **not** `The people pool`, because the sidebar filters by role), `npm run lint` 0 problems, `npx tsc --noEmit` clean, `npm run build` green |
| 2026-11-11 | T17 | `npm test` 39/39 (`candidates` 5, `post` 7, `confirmation` 6, `catalog` 4, `imports` 6, `people` 5, `projects` 6) and `npm run build` green: `/projects` lists the projects the server scoped to the caller and each name opens its own screen; `/projects/{id}` reads `GET /api/projects/{id}` with the posts and the gap rows, sums the gap into one sentence (`2 posts · 3 of 5 seats chosen · 2 seats still needed`, singular and `filled` included), and lists every post with its own `gapLabel` or a dash for a cancelled one; the end is offered to the project's own lead alone while it is open, asks twice — the button opens the consequence, `Ending Atlas cancels its 2 posts and releases 3 confirmed people. It cannot be undone.`, then `End it` sends `POST /api/projects/{id}/end` and the screen reloads into `closed · every post is cancelled and every allocation of this project is gone` with every post `cancelled`; `GET /api/projects/{id}` answers the coordinator any project, a lead only its own and 403 to another lead, and 404 for an unknown id (`ProjectApiTest` 11/11); `mvnw verify` 189; no browser render check yet |
