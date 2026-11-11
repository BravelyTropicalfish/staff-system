# Design — add-staffing-mvp

- **Change id:** `add-staffing-mvp`
- **Inputs:** delta specs (40 requirements / 90 scenarios, G1 **Pass** on rev 11), `atdd-design.md`
  (82 API / 8 E2E / 10 unit items deferred), `proposal.md` (Non-Goals, assumptions, D1–D8)
- **Out of scope of this document:** production code, task breakdown (`tasks.md`), Gate decisions

## 1. Stack decision (D1, resolved 2026-11-11)

The product owner chose: **React front end, Java back end, H2 database.** Realisation:

| Part | Choice | Why |
|---|---|---|
| Back end | Java 21 (Liberica 21.0.11 is installed), Spring Boot 3.4, Spring Web + Validation + Data JPA + Security | Java is the chosen language; Boot is the conventional runtime for H2 and gives transactional services without hand-rolled plumbing |
| Build | **Maven Wrapper pinned to 3.9.9** | the machine's global Maven is **3.0.4 (2012)** and cannot build Boot 3 — a blocker, not a preference (see risk 1) |
| Database | H2 2.x — file DB for `dev`, in-memory for tests, schema from `schema.sql` with `ddl-auto=validate` | chosen by the owner; no Docker on this machine, so Postgres is not available |
| Front end | React 19 + TypeScript + Vite + Tailwind + shadcn/ui + React Router, one `fetch` wrapper, no state library | React is the owner's choice; the rest is the smallest set that renders the agreed screens |
| API layer | Spring MVC controllers under `/api` with JSON DTOs | keeps one HTTP contract for the 82 API scenarios (`atdd-design.md` §1) |
| Tests | JUnit 5 + `@SpringBootTest(RANDOM_PORT)` + `TestRestTemplate` for API, JaCoCo for coverage, Vitest for front-end helpers, **Playwright (Node 24 present)** for the 8 E2E scenarios | Java for the back end, Node for the browser — both runtimes already exist |
| Excel | Apache POI on the server, one dev dependency in the E2E fixture script | the workbook is read server-side; the fixture builder is data, not a testcase generator |

The quote "react+React" is read as **React with TypeScript**; no second React-adjacent framework is
introduced. **Assumption to confirm:** if a different front-end toolchain was meant, only `web/`
changes — no spec, no API and no test intent moves.

ADR: `docs/knowledge/architecture/adr-001-runtime-stack.md`.

**Rejected alternatives:** Next.js single app (allowed a Node back end, but the owner chose Java);
React + Fastify (same reason); FastAPI + React (same); Postgres in Docker (no Docker installed);
an SPA with server-side rendered templates (loses the shadcn/ui component base the repository
requires).

## 2. Layering

`architecture-baseline` rules 1–3: UI → domain → data, one-way, no loops.

```text
web/            React SPA: pages, components, fetch client. Holds no business rule.
api/            Spring controllers, DTOs, validation annotations, role guards. Thin.
domain/         Services and rules: gate engine, post lifecycle, confirmation, import, project end.
data/           JPA entities, repositories, schema.sql. No rule beyond constraints.
```

Physical layout: `backend/` is the Maven project (`src/main/java/com/example/staffing/{api,domain,data}`,
`src/main/resources/schema.sql`) and `web/` the Vite app; both are new in this change.

Package root `com.example.staffing`; one service entry point per capability (`CatalogService`,
`PersonService`, `ImportService`, `ProjectService`, `AllocationService`, `PostService`,
`MatchingService`, `ConfirmationService`, `ReleaseService`, `NoticeService`), so an acceptance
check never reaches into internals. `PersonService` is the pool's read side only: the import is
the sole writer of a person.

## 3. Data model (H2)

| Table | Key columns | Notes |
|---|---|---|
| `role` | `id`, `name`, `active` | five seeded rows; rename/delete refused once referenced |
| `skill` | `id`, `name`, `role_domain_id`, `active` | domain feeds the fallback only |
| `person` | `employee_no` PK, `name`, `role_id`, `job_level`, `travel`, `active` | one role per person; no edit path outside the import |
| `person_skill` | `(employee_no, skill_id)` PK, `band` | omitted skill keeps its row |
| `project` | `id`, `name`, `lead_user_id`, `start_date`, `end_date`, `status` | `OPEN` → `CLOSED`, terminal; the range is the spec's project range (inverted dates are refused at entry) |
| `post` | `id`, `project_id`, `title`, `role_id`, `min_job_level`, `headcount`, `start_date`, `end_date`, `travel_required`, `status` | status derived from the confirmed count: recomputed on every read and written back on every save, so a stale cache never surfaces |
| `post_skill` | `(post_id, skill_id)` PK, `kind` `REQUIRED|BONUS`, `min_band`, `target_band` | |
| `assignment` | `id`, `post_id`, `person_no`, `created_at` | a release **deletes** the assignment with its allocation (spec: releasing reopens the slot), so a row is always active and carries no state column |
| `allocation` | `id`, `person_no`, `project_id`, `start_date`, `end_date`, `source` `MANUAL|CONFIRMATION`, `assignment_id` | check: `source='CONFIRMATION'` ⇒ `assignment_id` not null |
| `recommendation` | `id`, `post_id`, `person_no`, `state`, `void_reason`, `snapshot_json`, `created_at` | snapshot keeps gates + qualifying skills |
| `notice` | `id`, `coordinator_id`, `person_no`, `post_id`, `reason`, `created_at`, `dismissed_at` | one row per lost proposal |
| `import_batch` | `id`, `filename`, `status` `APPLIED|REJECTED`, `uploaded_by`, `uploaded_at`, `person_rows`, `skill_rows`, `error_count` | no row at all when the file is unreadable; the history the spec asks for needs uploader and row counts, which is why they are columns and not derived |
| `import_error` | `id`, `batch_id`, `sheet`, `row_no`, `key_value`, `message` | complete list, never truncated; `key_value` is the employee number the row was about |
| `app_user` | `id`, `username`, `password_hash`, `role` `LEAD|COORDINATOR`, `display_name` | seeded demo accounts, fake passwords hashed; a lead's scope is read from `project.lead_user_id`, so no duplicated scope column exists |

Dates are `date` columns (no time). `headcount` is a positive integer. Everything the spec calls
"frozen" is enforced in the domain service, not in the UI.

## 4. API contract (one route group per capability)

| Capability | Routes |
|---|---|
| `people-pool` | `GET/POST /api/roles`, `PATCH /api/roles/{id}`, `DELETE /api/roles/{id}`; `GET/POST /api/skills`, `PATCH /api/skills/{id}`, `DELETE /api/skills/{id}`; the two `DELETE` routes exist only to answer 409 "deactivated instead" (a catalog item is never deleted); `GET /api/people`, `GET /api/people/{employeeNo}` (read-only, coordinator only; person data arrives by import); `GET /api/bands` (the three scales — job levels, proficiencies, travel — so the post form never copies a vocabulary) |
| `people-import` | `POST /api/imports` (multipart), `GET /api/imports`, `GET /api/imports/{id}` (batch + errors); the history answers counts only and leaves `errors` empty, so the error rows are read from the detail route — which is also the refusal-vs-rejection difference: a shape refusal answers 422 and records no batch, a bad-row file answers 201 with a `REJECTED` batch |
| `allocation` | `GET/POST /api/projects`, `GET /api/projects/{id}` (the coordinator any project, a lead only its own — the project screen reads this), `POST /api/projects/{id}/end`; `POST /api/allocations`, `PATCH/DELETE /api/allocations/{id}` |
| `post-requirement` | `GET/POST /api/projects/{id}/posts`, `GET /api/posts/{id}` (the post's own lead or the coordinator), `PATCH /api/posts/{id}`, `POST /api/posts/{id}/cancel`, `GET /api/projects/{id}/gap` (each confirmed seat carries its assignment id, because a release names the assignment rather than the person) |
| `candidate-matching` | `GET /api/posts/{id}/candidates` (suitable, fallback, exclusions) |
| `staffing-confirmation` | `GET /api/posts/{id}/recommendations` (the coordinator with each snapshot, the post's own lead without it), `POST /api/posts/{id}/recommendations`, `DELETE /api/recommendations/{id}`, `POST /api/recommendations/{id}/confirm`, `POST /api/recommendations/{id}/reject`, `POST /api/assignments/{id}/release`, `GET /api/notices`, `POST /api/notices/{id}/dismiss` |
| auth | `POST /api/session`, `DELETE /api/session`, `GET /api/session` |

Error shape: `{"code": "...", "message": "...", "details": [...]}` with codes the specs name
(`person taken`, `post cancelled`, `release first`, refusals by gate). 403 for role or scope
violations, 409 for state conflicts, 422 for validation, and no stack trace ever leaves the server
(`security-baseline` 6).

A body carrying a field no request record declares answers 400 `unreadable body` rather than being
deserialised with the field dropped: silently ignoring `percentage`, a rate or a travel destination
would be a Non-Goal that the server quietly supports (T29). The policy is
`spring.jackson.deserialization.fail-on-unknown-properties=true`, and `ApiExceptionHandler` maps
`HttpMessageNotReadableException` onto the error shape above.

## 5. Gate engine and the suitable list

Order fixed: **G1** active → **G2** role → **G3** job level → **G4** required skills → **G5**
travel → **G6** date intersection. The first failing gate is the reported reason; one evaluation
returns per-gate results so the row can show what passed.

`G6` compares `[allocation.start, allocation.end]` with `[post.start, post.end]`, closed at both
ends: `start1 <= end2 && start2 <= end1`.

Roles and skills are compared by row id, never by name: a catalog row a person or a post
references can be neither renamed nor removed, so the id is the identity and the name travels only
into the reason text.

The suitable list contains every person of the post's role who passes all six gates, sorted by name
and then by employee number, with no score field anywhere in the DTO. A bonus shortfall is carried
as information, never as an ordering key.

The exclusion list covers that same role pool: each person with their first failing gate, and a
count per gate with all six carried so zeros show. That is why G2 never appears as a reason — a
person of another role is not a same-role candidate at all — and why an inactive person is reported
under G1 rather than dropped.

The fallback opens only when that list is empty; it needs **X1** (job level at least one band above
the post minimum) and **X2** (a skill of the post role's domain at `Practitioner` or above), and it
is returned as its own array with its evidence. X1 stands in for G3 and X2 for G2 and G4, so the
post's required rows never gate this tier; G1, G5 and G6 still do, and are reported before X1 and X2
so an inactive or booked person keeps a gate reason. Each row carries the role, the bands above the
requirement and the qualifying domain skill with the band held, because the recommendation snapshot
will copy exactly that row, and nothing adds the requirement's role to the person. A role whose
skill domain is empty admits nobody rather than widening to any skill, and a post already at the top
band can have no cross-role candidate — X1 then says no band sits above it. The `X1`/`X2` counts
join `exclusionCounts` (zero while the fallback did not open) and `fallbackReason` states why the
section exists, so an empty cross-role array is explained rather than silent.

## 6. Confirmation, voiding and concurrency

`POST /api/posts/{id}/recommendations` is the coordinator's alone and writes one row, `proposed`,
with `snapshot_json` and `proposed_by`. The slot is checked first — a `cancelled` post answers 409
`post cancelled`, a post whose confirmed count reached its headcount answers 409 `post filled` — and
then the person must sit on the list the search answers at that moment: a `suitable` row or, while
that section is open, a `fallback` row. Anyone off both lists is refused with the gate that keeps
them off (`G1`…`G6`, `X1`, `X2`) and the reason the exclusion list already shows, so the two reads
can never disagree; a person of another role while the cross-role section is closed answers 409
`not a candidate`, and a second proposal for the same person on the same post answers 409
`already proposed`. The snapshot copies the admitting row — the gates that ran and the post's skill
rows with the bands held — and for a fallback row it carries the role, the bands above and the
qualifying domain skill instead of same-role skill rows. The response reads the row back rather
than echoing the request, so what the coordinator sees is what the database holds, and nothing about
the person's role changes.

`GET /api/posts/{id}/recommendations` is the read the confirmation screen runs: open proposals
first (`proposed`), then the closed history, newest first inside each. The coordinator reads every
row with its reason snapshot; the post's own lead reads the same rows with `snapshot` null, because
the spec gives the snapshot to the coordinator alone; another project's lead answers 403, the same
guard the post read already applies.

One transaction per confirmation, its locks taken in one order — the post row
(`SELECT … FOR UPDATE`) and then the person row, both before the re-check, so two confirmations of
one person queue instead of both reading an unbooked person and two on one post never over-fill it:

1. re-run G1 and G6 against the post's dates;
2. write `assignment` + `allocation(source=CONFIRMATION, dates=post dates)`;
3. decrement the remaining headcount, set `FILLED` at zero;
4. void every pending recommendation of that person **whose post dates intersect** the new
   allocation, reason `person taken`, one `notice` each;
5. leave a recommendation on disjoint dates `PROPOSED`.

The loser of a concurrent double confirmation gets 409 `person taken`: the winner's write-back voids
the loser's proposal, and a confirmation on a proposal already voided for that reason answers in
that voice rather than as a generic state clash. The test drives two threads and asserts exactly
one assignment and one allocation. Release and project end run in the same transaction shape: end
cancels the project's posts, voids pending recommendations with `post cancelled`, deletes the
project's allocations and releases the assignments.

**Isolation, chosen deliberately:** the rows are locked explicitly and the transaction keeps the
default isolation instead of `SERIALIZABLE`, because H2 maps `SERIALIZABLE` to snapshot isolation
and reports the losing confirmation as `40001` (deadlock) instead of the `person taken` refusal the
spec asks for. The post row lock guards the seat count, the person row lock guards the calendar,
and every state change stays guarded by `state = 'proposed'` in its `WHERE` clause.

**H2 limitation, accepted:** H2 locks rows but is not a distributed database, so this check proves
the invariant under a row lock rather than under real concurrent replicas. Recorded in
`atdd-design.md` §7 as the reason the idempotency check stays at API level.

## 7. Import

Apache POI reads the workbook; validation runs over every row of both sheets before anything is
written. Unreadable (missing sheet, no data row) → 422 and **no batch**; readable with bad rows →
`REJECTED` batch holding every error, pool untouched. Apply writes people, skills and bands in one
transaction, reactivates inactive people, keeps an omitted skill's band, and never touches
allocation, assignment, recommendation or project rows. Column mapping for the real HR export (D4)
sits in one `ColumnMap` class so a later rename is a one-file change. The workbook decision itself is
recorded in `docs/knowledge/architecture/adr-002-import-workbook.md`.

## 8. Front end

Routes: `/login`, `/imports`, `/people`, `/people/:employeeNo`, `/projects`, `/projects/:projectId`,
`/projects/:projectId/posts/new`, `/posts/:postId`, `/posts/:id/candidates`, `/catalogs`, `/notices`.
Signing in is `/login`: the requirement calls sign-in "a lightweight seeded account with a role", so
the screen posts `POST /api/session` and goes to the shell — added after G2, because without it the
only way in was posting that route by hand. The coordinator sees catalogs, import and the full pool; a lead sees
only their own projects, the posts, the recommended candidates and the gap view. Hiding a control is convenience only — the server guard is the
authority (`security-baseline` 1). Bands render as names; no numeric proficiency appears anywhere.

## 9. Tests and gates

| Layer | Tool | Scope |
|---|---|---|
| unit | JUnit 5 | gate order, band rank, intersection, voiding set, status derivation, column mapping |
| API | `@SpringBootTest(RANDOM_PORT)` + `TestRestTemplate`, H2 in-memory | the 82 scenarios; one fresh schema per class |
| E2E | Playwright (TypeScript) | the 8 scenarios plus journeys J1/J2 |
| front-end unit | Vitest | formatting and client-side helpers only |

T21 could not install Vitest: the registry is unreachable from this machine, so the screen's pure
readings run under Node's built-in runner (`node --test`, TypeScript by type stripping) with no new
dependency. The functions are plain TS, so moving them under Vitest later is a command change, not a
rewrite; Vitest stays the recorded tool for this layer.

T26 added Playwright 1.65 and `exceljs` as dev dependencies. `web/playwright.config.ts` starts both
servers itself: the backend through `mvnw spring-boot:run` with `cwd: ../backend` and
`--spring.datasource.url=jdbc:h2:mem:e2e`, so every run begins from the seeded schema, then the Vite
dev server on 5173, whose `/api` proxy reaches the backend. A setup project signs in as coordinator,
lead.one and lead.two — the tasks asked for two accounts, the tests need the second lead to prove a
project is not theirs — and stores one `storageState` per account; a cookie ignores the port, so the
session obtained from 8080 is valid for the app on 5173. The workbook fixture is written by `exceljs`
with the column names `WorkbookParser` reads and the vocabulary `Bands` accepts, so an import fixture
cannot drift from the parser without failing.

Coverage: JaCoCo 100% line and branch on `domain/` and `api/`; a line that cannot be tested needs
a written reason and reviewer approval in the PR (`testing-strategy` 6). The gate is a `check`
execution at `verify` with one `PACKAGE` rule over `com.example.staffing.domain*` and
`com.example.staffing.api*` at `COVEREDRATIO 1.0` for LINE and BRANCH, so a regression fails the
build instead of being noticed in a review. Two details were proved rather than assumed: the rule
needs the dotted package form (a slash path matches no package, and an empty rule passes), and the
ratio is enforced by raising the minimum to `1.000001` and watching `verify` fail. The current run
reads 1489/1489 lines and 537/537 branches across 280 tests.

CI runs the five required checks from `pipeline-gates`; `.github/workflows/ci.yml` is created by
this change and `docs/feedback/verification-commands.md` is filled in the same change (rule 7). The
workflow holds no deploy job. Two environment facts were learned from the first run rather than
assumed: `web/package-lock.json` has to resolve from `registry.npmjs.org`, because a runner cannot
reach the internal Artifactory that the developer machine uses (npm rewrites the public host back to
the configured mirror, so a local install keeps working); and `dependency-review-action` needs
GitHub Advanced Security unless the repository is public — it is public, so the review runs on every
pull request, and the private attempt that failed first is why this gate is not softened with
`continue-on-error`. The repository had no linter, so ESLint 9 (with `typescript-eslint`)
was added for the lint check instead of a command that always passes; `web/playwright.config.ts`
picks `mvnw.cmd` on Windows and `./mvnw` elsewhere, so the acceptance job starts the backend on
Linux.

The ten reverse checks of `atdd-design.md` §6 live in `NonGoalApiTest` (rows 1–9) and a review of
the workflow (row 10). Row 4 is asserted on the API reading — a post with a passing candidate stays
`open` with zero assignments — as well as through J1, because the screen proof and the stored state
are two different claims.

## 10. Risks and open items

1. **Toolchain:** global Maven 3.0.4 cannot build Boot 3. The wrapper pins 3.9.9; if the wrapper
   cannot be generated offline, installing Maven 3.9.x is a prerequisite, not a code change.
2. **D4** (real HR column names) stays open; `ColumnMap` isolates it.
3. **D5/D6** (batch rollback, skill removal) stay out: batches are read-only history, no removal
   channel exists.
4. **D7** (post dates vs project dates) stays unconstrained; nothing validates containment.
5. **D8** (fallback cap or approval) keeps its MVP default; the confirm re-check is the only gate.
6. **POI** is a new dependency: reviewed for CVEs at add time (`security-baseline` 7).
