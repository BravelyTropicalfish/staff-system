# Local Verify Report: add-staffing-mvp

**Date:** 2026-11-11 (Asia/Shanghai, +0800)
**Root:** `c:\Users\ZENGKY\Desktop\New folder\baseline`
**Scope:** local+evidence
**Change id:** `add-staffing-mvp`

## Summary

| Dimension | Status |
|-----------|--------|
| Build | Pass |
| Static scan | Pass |
| Code review | Pass |
| Evidence (OpenSpec) | Pass |
| **Overall** | **Pass** |

**PR gate:** Overall is Pass, so `/sdd:pr-submit` may run. E3 (human sign-off) is still Pending —
this report is machine and agent evidence only, and the agent signed nothing.

## Local commands

Whitelist source: `docs/feedback/verification-commands.md` (the primary source named by the probe).
The probe's bash-fence parser stops at the first fence and emitted only the static layer, so the
runner used `docs/ai4se-plugins/20261111-agent-whitelist-commands.txt`: the same document's
remaining fences copied verbatim, eight commands in total, none invented. Full output:
`docs/ai4se-plugins/20261111-163524-local-verify-run.log`.

| Command | Source | Exit | Summary |
|---------|--------|------|---------|
| `cd web && npm ci && npm run lint` | verification-commands.md (static) | 0 | 385 packages installed; ESLint over `src`, `tests`, `e2e` with 0 problems; `npm ci` prints 9 moderate advisories |
| `cd web && npx tsc --noEmit` | verification-commands.md (static) | 0 | no diagnostics (app, tests and e2e are all inside `tsc`) |
| `cd backend && ./mvnw -B -DskipTests compile` | verification-commands.md (static) | 0 | `BUILD SUCCESS` |
| `cd backend && ./mvnw -B test` | verification-commands.md (runtime) | 0 | `Tests run: 280, Failures: 0, Errors: 0, Skipped: 0` |
| `cd web && npm test` | verification-commands.md (runtime) | 0 | 39 tests, 39 pass, 0 fail |
| `cd backend && ./mvnw -B verify` | verification-commands.md (runtime) | 0 | 280 tests, `All coverage checks have been met.`, `BUILD SUCCESS`; Jacoco `coverage-gaps.mjs` reads 1489/1489 lines and 537/537 branches |
| `cd web && npx playwright install --with-deps chromium && npm run test:e2e` | verification-commands.md (system) | 0 | `15 passed (39.5s)` (3 setup + 2 journeys + 8 scenarios + 2 smoke) |
| `cd web && npm audit --audit-level=high` | verification-commands.md (system) | 0 | 9 moderate, 0 high, 0 critical — the declared threshold is high |

## Code-review highlights

Working tree is the whole implementation (`backend/`, `web/`, `.github/` are new; 21 tracked files
modified inside the change). Reviewed by reading the files that carry risk, not the diff alone.

- **Guards.** `api/SecurityConfig.java` is deny-by-default — only `GET /api/health` and
  `POST /api/session` are open — and answers 401 in the one JSON error shape. Role and project
  scope stay in each handler, and the 31 `FORBIDDEN` assertions across the route-group suites prove
  them with the state unchanged.
- **No SQL built from input.** Every repository uses `JdbcTemplate` with `?` placeholders; a scan for
  a string literal concatenated with a variable in `data/` finds none, so no injection path exists.
- **Locking.** `domain/ConfirmationService.java` takes the post row and then the person row, always
  in that order, and re-reads through `markConfirmed(...) == 0` before writing; the concurrency test
  drives two threads and asserts one assignment and one allocation.
- **Import atomicity.** `domain/ImportService.java` parses and validates before the first write, and
  the batch row commits with the writes it stands for, so a refusal leaves no batch and a rejection
  writes nothing to the pool.
- **One error shape.** `web/src/lib/api.ts` keeps the server's `{code,message,details}` instead of
  inventing a second shape, and sends no `credentials` flag because the SPA is same-origin behind
  the Vite `/api` proxy.
- **Schema as source of truth.** `schema.sql` holds 14 tables with vocabularies as CHECK
  constraints, `ddl-auto=validate` in both profiles, and an insert-only seed, so a restart keeps a
  catalog edit.
- **Test configurability trap, already handled.** `src/test/resources/application.properties`
  shadows the main file on the test classpath; the file carries a comment saying so, which is why
  the strict unknown-field policy is repeated there.

## Evidence checklist

| ID | Check | Result | Mark | Notes |
|----|-------|--------|------|-------|
| V1 | Task completion | Pass | [fact] | `tasks.md` and `tasks.zh.md` both read 30 done / 0 open; each task carries its run note |
| V2 | Requirement coverage | Pass | [fact] | 40 requirements across 6 delta specs, 0 without a scenario; the route inventory in `api/*Controller.java` matches `design.md` §4 one-to-one |
| V3 | Scenario coverage | Pass | [fact] | 90 spec scenarios = 90 `traceability.md` rows (no missing, no extra, set equality per capability 11/15/15/17/14/18); all 82 API ids resolve to a real test method and all 8 E2E titles exist in the spec files — checked by script, not by eye |
| V4 | Requirement↔impl mapping | Pass | [fact] | Spots opened and read: gate order in `domain/Gate.java` (G1→G6), the closed intersection in `domain/Ranges`, the 14 tables in `schema.sql`, the confirmation transaction, the 8 routes per capability group |
| V5 | Spec drift | Pass | [fact] | No undisclosed divergence found. The four known deviations are written down where a reviewer will see them: Vitest → `node --test` (design §9), three sign-in accounts instead of two (design §9/T26 note), the strict unknown-field 400 (design §4), and J1 seeding the proposal through the API because no screen posts one (traceability/T27 note) |
| V6 | Design follow-through | Pass | [fact] | Design §1/§3/§9 verified mechanically: JaCoCo `check` at `phase` `verify`, `PACKAGE` rule over `com.example.staffing.domain*`/`api*` at `COVEREDRATIO 1.0` for LINE and BRANCH; `assignment` has no `state`, `app_user` has no `project_scope`; `playwright.config.ts` picks `mvnw.cmd` vs `./mvnw`; `ci.yml` holds five jobs, `permissions: contents: read` and no deploy job |
| V7 | Code pattern fit | Pass | [fact] | Layering holds (`api` → `domain` → `data`); services carry the invariants in Javadoc that states why (lock order, snapshot copying, refusal wording); naming follows the capability names already used in the specs |
| V8 | Upstream basis (necessity) | Pass | [fact] | Each new module traces to a task: `backend/` T01–T30, `web/` T03/T07/T10/T12/T16/T17/T21/T25, `ci.yml` + `verification-commands.md` T30, the two ADRs design §1/§7, `prototype/` the review aid recorded in the handoff, the `.gitignore` lines the e2e auth states and the dev H2 file. One orphan: `adr-002-import-workbook.md` has no inbound link (SUGGESTION 2) |

## Evidence slots (E1 / E2 / E3)

| Slot | Status | Source |
|------|--------|--------|
| E1 Machine | Partial | local whitelist run, 8/8 exit 0 (`20261111-163524-local-verify-run.log`); no `ci-report.md` because no PR exists yet and `/sdd:pipeline-report` has not run |
| E2 AI | Pass | this report: V1–V8, code review, 8/8 local commands |
| E3 Human | Pending | not signed; the reviewer response guide is below and the agent never signs it |

## Five-question map

| Q | Maps to | Status |
|---|---------|--------|
| Q1 Complete? | V1–V3 | ✅ 30/30 tasks, 40/40 requirements with scenarios, 90/90 scenarios traced and resolvable |
| Q2 Correct? | V4–V5 | ✅ routes, gate order, schema and transaction behaviour match the specs; deviations disclosed |
| Q3 Coherent? | V6–V7 | ✅ design decisions are in the code; layering and naming fit the surrounding project |
| Q4 Necessary only? | V8 | ⚠️ every module has a task basis; one ADR is unlinked |
| Q5 Risk controllable? | residual + red lines | ✅ residual risks are listed with a mitigation each; no red line is crossed |

## Findings

### CRITICAL

| # | Finding | Mark | Location | Suggestion |
|---|---------|------|----------|------------|
| — | none | — | — | — |

### WARNING

| # | Finding | Mark | Location | Suggestion |
|---|---------|------|----------|------------|
| 1 | CSRF protection is switched off while the session cookie is the only credential | [fact] | `backend/src/main/java/com/example/staffing/api/SecurityConfig.java:40` | Exposure is small today: no CORS configuration exists, the SPA is same-origin behind the Vite proxy, and cross-site JSON writes would need a preflight that fails. Before any real deployment, set an explicit cookie `SameSite` policy or add a CSRF token. [inference: verify by sending a cross-site form POST once a deployment exists] |
| 2 | 9 moderate npm advisories in the dev-only chain (`typescript-eslint` → `minimatch`; `exceljs` → `uuid`) | [fact] | `web/package-lock.json`; reported by `npm ci` and `npm audit` | The declared gate is `--audit-level=high` and it passes; nothing here ships in the browser bundle (`exceljs` is an e2e fixture dependency). Track a `typescript-eslint` bump and revisit `exceljs` when it moves |

### SUGGESTION

| # | Finding | Mark | Location | Suggestion |
|---|---------|------|----------|------------|
| 1 | The lint memo still counts 88 scenarios in its "checks that passed" row, while its own verdict section and the specs say 90 | [fact] | `openspec/changes/add-staffing-mvp/consistency-lint.md:34` (verdict at lines 179/196/198) | Change `88` to `90`; the eighth pass already re-checked the tree at 40/90 |
| 2 | `adr-002-import-workbook.md` has no inbound link | [fact] | `docs/knowledge/architecture/adr-002-import-workbook.md` | Link it from `design.md` §7 next to the `ColumnMap` paragraph, the way §1 links ADR-001 |
| 3 | E1 is local-only: the workflow has never run on a pull request | [inference] | `.github/workflows/ci.yml` | Watch the first PR run and attach `/sdd:pipeline-report` output as the E1 evidence for G2 |

## Residual risks

| # | Risk | Why open | Mitigation |
|---|------|----------|------------|
| 1 | Spec consistency lint is capped | the `openspec` CLI is absent, so validation stays manual; L3 (capability name `allocation` mixes projects and allocations) is deliberately open in the MVP | accepted — the eighth pass read the blocker set directly from the files |
| 2 | Concurrency is proven under H2 row locks only | H2 is not a distributed database | accepted — recorded in design §6 and `atdd-design.md` §7 as the reason the idempotency check stays at API level |
| 3 | 9 moderate dev-only advisories | fixes are semver-major or downgrades for tools that only run locally | tracking (WARNING 2) |
| 4 | No screen posts a recommendation | a product gap, not a spec violation: no requirement asks for that screen, so J1 seeds the step through the API | accepted for the MVP; a follow-up could add the screen |
| 5 | Real HR column names are unknown (D4) | the export has not been seen | accepted — `ColumnMap` isolates the rename to one file |
| 6 | No TLS in the local demo | no deployment exists; `security-baseline` 8 applies to network traffic, not to localhost | accepted — must be revisited at deployment |

## Human review assist

- **Risk band:** author self-assesses **M** (permission checks plus the irreversible project end);
  no CRITICAL finding, so no raise is recommended.
- **E1/E2/E3:** Partial / Pass / Pending — E3 stays with the reviewer.
- **Q1–Q5:** ✅ ✅ ✅ ⚠️ ✅ (Q4 carries the unlinked ADR).
- **Findings response guide:**
  - WARNING 1 (CSRF): `accept` for the MVP, because no deployment exists; `need-more-info` if a
    deployment is in scope now.
  - WARNING 2 (advisories): `accept` — the gate is high and the chain is dev-only.
  - SUGGESTION 1–2: `accept` — one-word and one-link fixes.
  - SUGGESTION 3: `accept` — closes itself on the first PR.
- **Red lines checked:** no authz bypass found; no secret or PII in the tree (demo passwords are
  hashes); no irreversible change without a recorded reason; nothing here relies on "AI says pass".
- **Release suggestion:** `放行` (pass), with the two WARNINGs accepted explicitly by the reviewer.
  The agent does not sign this.

## Waiver

N/A — no waiver. Overall is Pass, so no waiver row is needed; E3 remains the reviewer's.

## Follow-up (2026-11-11, after this report was written)

| Finding | Resolution |
|---|---|
| SUGGESTION 1 | `consistency-lint.md` S4 row now reads 90 scenarios (was 88) |
| SUGGESTION 2 | `design.md` §7 and `design.zh.md` §7 now link `adr-002-import-workbook.md` |
| SUGGESTION 3 | still open by nature — it closes when the first PR run is reported |

Dimensions and Overall are unchanged: these were documentation suggestions and they touch neither the
command table nor the evidence rows above.
