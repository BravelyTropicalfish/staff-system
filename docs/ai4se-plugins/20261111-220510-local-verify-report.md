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

The working tree is the whole application change plus the post-G2 walkthrough work: 153 entries
(111 modified, 40 untracked, 2 deleted); the tracked diff is 113 files, +3315 / −1956 lines. T31–T43
are the work done after the G2 signature of 2026-11-11 and are the subject of this re-verification.

## Local commands

Whitelist source: `docs/feedback/verification-commands.md` (the primary source named by the probe);
the eight commands are the same document's fences in
`docs/ai4se-plugins/20261111-agent-whitelist-commands.txt`, none invented. Full output:
`docs/ai4se-plugins/20261111-220510-local-verify-run.log`.

| Command | Source | Exit | Summary |
|---------|--------|------|---------|
| `cd web && npm ci && npm run lint` | verification-commands.md (static) | 0 | after stopping the dev Vite server that held `node_modules` (first attempt: EPERM); one npm warning (`esbuild` postinstall skipped by the allow-scripts policy, Vite still builds); ESLint over `src`, `tests`, `e2e` with 0 problems |
| `cd web && npx tsc --noEmit` | verification-commands.md (static) | 0 | no diagnostics (app, tests and e2e are all inside `tsc`) |
| `cd backend && ./mvnw -B -DskipTests compile` | verification-commands.md (static) | 0 | `BUILD SUCCESS` |
| `cd backend && ./mvnw -B test` | verification-commands.md (runtime) | 0 | `Tests run: 297, Failures: 0, Errors: 0, Skipped: 0` |
| `cd web && npm test` | verification-commands.md (runtime) | 0 | tests 54 / pass 54 / fail 0 |
| `cd backend && ./mvnw -B verify` | verification-commands.md (runtime) | 0 | 297 tests, `All coverage checks have been met.`, `BUILD SUCCESS`; the first run failed the JaCoCo gate (api lines 0.9 / branches 0.8) on the new token classes, `SessionTokenRepositoryTest` closed the gap and the rerun passed |
| `cd web && npx playwright install --with-deps chromium && npm run test:e2e` | verification-commands.md (system) | 0 | Chromium already present, so the test half ran: `20 passed (55.8s)` — 3 setup + 2 journeys + 8 scenarios + 2 smoke + 3 login + J1/J2/J3 |
| `cd web && npm audit --audit-level=high` | verification-commands.md (system) | 0 | 9 moderate, 0 high, 0 critical — the declared threshold is high |

## Code-review highlights

Reviewed by reading the files that carry risk, not the diff alone.

- **Two identities, one precedence rule.** `api/SessionTokens.java` keeps a token → `SecurityContext`
  copy in memory; `api/SessionTokenRepository.java` answers a request that names a token from that
  token alone and never falls back to the shared cookie, while a request without a token keeps the
  cookie path the API tests and Playwright storage states use. `SessionController` issues the token,
  revokes the one the tab held before, and a sign-out that carries a token ends that tab only. The
  precedence is pinned by `SessionApiTest` (token over cookie; unknown token refused) and covered
  line-by-line by `SessionTokenRepositoryTest`.
- **The door.** `web/src/components/AppShell.tsx` reads `GET /api/session` before drawing and returns
  `<Navigate to="/login">` when refused; `web/src/lib/api.ts` clears the tab token and emits
  `staffing:unauthorized` on any 401; `LoginScreen` stores the returned token and returns to the
  screen that was refused. `login.spec.ts` drives both the redirect and a two-tab, two-account run.
- **Import is one sheet.** `WorkbookParser` reads `people` with the eight columns including `skills`;
  `ImportValidator` splits `skill:band;skill:band` (both colons, empty segments ignored, duplicate or
  unknown skill and out-of-vocabulary band reject the row), and a rejected row rejects the whole
  batch. The row-count contract is re-pinned in `ImportApiTest` / `ImportValidatorTest`.
- **Rules the owner set.** `ProjectService.create` refuses the coordinator (`只有项目经理可以新建项目`)
  and a lead naming another lead; `PostService` keeps the post's own lead as the only writer and
  `PostForm`/`PostScreen` now render the coordinator's view read-only; `CatalogService.deleteRole`
  counts people, posts and domain skills before deleting and otherwise answers `referenced`.
- **Messages, not codes.** Every user-facing server message is Chinese, while every `code`
  (`invalid post`, `post cancelled`, `person taken`, …) stays the English machine word the tests and
  the screens branch on; `web/src/lib/confirmation.ts` maps void/notice reasons to Chinese labels
  instead of printing the raw code.
- **No SQL built from input.** A scan for a literal SQL fragment concatenated with a variable in
  `data/` finds none; every repository uses `JdbcTemplate` with `?` placeholders.
- **Test configurability trap, already handled.** `src/test/resources/application.properties`
  shadows the main file on the test classpath; the strict unknown-field policy is repeated there.

## Evidence checklist

| ID | Check | Result | Mark | Notes |
|----|-------|--------|------|-------|
| V1 | Task completion | Pass | [fact] | `tasks.md` and `tasks.zh.md` both read 30/30 ticked for T01–T30; T31–T43 are table rows, each carrying its run note and evidence, and no task is left open |
| V2 | Requirement coverage | Pass | [fact] | 40 requirements across 6 delta specs; the English and Chinese copies agree 40/90 exactly (counted, not eyeballed); the route inventory in `api/*Controller.java` matches `design.md` §4, including the three read-only board routes |
| V3 | Scenario coverage | Pass | [fact] | 90 scenarios = 90 traceability rows: a script compared the two sets and reported `missing=0`; the only three extra rows are the J1/J2 journey rows, which are not spec scenarios |
| V4 | Requirement→impl mapping | Pass | [fact] | Spots read: gate order G1→G6 in `gates`/`Gate`, `Ranges` closed-intersection, the confirmation transaction and lock order, one-sheet import, catalog reference counting, token precedence, lead-only project creation, coordinator read-only post form |
| V5 | Spec drift | Pass | [fact] | No undisclosed divergence. The disclosed ones are written where a reviewer sees them: `node --test` instead of Vitest (design §9), three seeded accounts (design §9), strict unknown-field 400 (design §4), J1 seeding one proposal through the API (traceability/T27), and the API `code` staying English inside a Chinese UI (design §8, T43) |
| V6 | Design follow-through | Pass | [fact] | Design §8 now records the redirect and the per-tab token; design §4 lists the board routes; JaCoCo `check` still binds `verify` with `PACKAGE` rules at 1.0; `ci.yml` still holds the five jobs and no deploy; `playwright.config.ts` still picks `mvnw.cmd` vs `./mvnw` |
| V7 | Code pattern fit | Pass | [fact] | New classes sit in the api layer beside `Auth`/`SecurityConfig` and follow the same package-private style and why-comments; tests mirror the existing Mockito + AssertJ shape; frontend helpers stay in `lib/*` with `node --test` coverage |
| V8 | Upstream basis (necessity) | Pass | [fact] | Every changed module traces to a task: T31 sign-in, T32 shell parity, T33/T34 project creation and recommend button, T35 information architecture, T36 Chinese UI, T37 the four rules, T38 dialogs, T39 proposed marks, T40 back links, T41 read-only post form, T42 redirect + token, T43 server messages. The tree also carries owner-side dev tooling not authored by this change (`backend/pom.xml`, `application.properties`, `dev/H2ConsoleConfig.java`, the `add-headcount-charts` change package, REQ-002 files, `sample-people.xlsx`) — excluded from the change's necessity map, see WARNING 3 |

## Evidence slots (E1 / E2 / E3)

| Slot | Status | Source |
|------|--------|--------|
| E1 Machine | Partial | the eight local commands all exited 0 (`20261111-220510-local-verify-run.log`); no `ci-report.md` for this commit yet — the branch has not been pushed since T31–T43, so `/sdd:pipeline-report` has nothing to read |
| E2 AI | Pass | this report: V1–V8, code review, 8/8 local commands |
| E3 Human | Pending | not signed; the agent never signs it |

## Five-question map

| Q | Maps to | Status |
|---|---------|--------|
| Q1 Complete? | V1–V3 | ✅ 30/30 tasks, 40 requirements, 90/90 scenarios mapped |
| Q2 Correct? | V4–V5 | ✅ spot-checked against specs; disclosed deviations recorded |
| Q3 Coherent? | V6–V7 | ✅ design §8/§4 and the house patterns hold |
| Q4 Necessary only? | V8 | ⚠️ change modules all traced; owner-side dev files sit in the same tree but are out of scope |
| Q5 Risk controllable? | residual + red lines | ⚠️ token lifetime is open and carried to G3; G1 rev 13 is signed |

## Findings

### CRITICAL

None.

### WARNING

| # | Finding | Mark | Location | Suggestion |
|---|---------|------|----------|------------|
| 1 | G1 rev 13 was unsigned when this run started; the owner then instructed the record, and the delta (one-sheet import, deletable catalog items, lead-only project creation, coordinator read-only) now carries its two signatures | [fact] | `openspec/changes/add-staffing-mvp/g1-decision.md` (rev 13: raylen IO, kyrie SA, 2026-11-11) | keep the signed record with the archive; no further action |
| 2 | Session tokens live in process memory with no expiry or eviction: every sign-in adds one entry until the server restarts | [inference] | `backend/.../api/SessionTokens.java` | acceptable for the single-process MVP; add a TTL or size bound in a later change, or state the restart-clears rule in design §8 (verify with a long-running sign-in loop) |
| 3 | The working tree mixes the change with owner-side dev tooling (`dev/H2ConsoleConfig.java`, `application.properties`, `pom.xml`) | [fact] | `backend/` | keep those out of the change commit, or record them as owner work; they are not part of T31–T43 |
| 4 | E3 (human sign-off) is pending and must stay pending for the agent | [fact] | this report | VL reviews this report and the G2 addendum before the archive lane |

### SUGGESTION

| # | Finding | Mark | Location | Suggestion |
|---|---------|------|----------|------------|
| 1 | The bilingual badges (`open 空缺`, `required 必备`) and English API `code`s are deliberate, not leftovers | [fact] | `web/src/screens/PostsScreen.tsx`, `web/src/lib/*` | state the rule once in design §8 so a later pass does not "fix" them |
| 2 | `npm ci` fails while a dev Vite server is running on Windows because it holds `node_modules` | [fact] | local environment | note in `docs/feedback/verification-commands.md` that the dev server is stopped before the whitelist run |

## Residual risks

| # | Risk | Why open | Mitigation |
|---|------|----------|------------|
| 1 | CSRF is disabled while the session cookie remains a credential | pre-existing and unchanged by this work | accepted for the single-origin SPA; tracked from the first verify report |
| 2 | A server restart invalidates every token, so open tabs fall back to the sign-in screen | the token store is in-memory by design | accepted: the 401 path redirects cleanly (proven by `login.spec.ts`) |
| 3 | 9 moderate npm advisories in the dev toolchain | no high or critical finding; `npm audit --audit-level=high` passes | tracking; re-run at each verify |
| 4 | G1 rev 13 signature was recorded during this run | the gate needs two humans | closed: raylen (IO) and kyrie (SA) signed 2026-11-11 |
| 5 | E1 lacks a CI run for this commit | the branch is not pushed since T31–T43 | push, then `/sdd:pipeline-report phase=ci` after the PR opens |

## Waiver

N/A — no waiver.
