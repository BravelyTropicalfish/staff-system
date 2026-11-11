# G2 decision — add-staffing-mvp

- **Gate:** G2 (Verify)
- **Change id:** `add-staffing-mvp` (REQ-001 rev 11)
- **Decision:** **Pass**
- **Timestamp:** 2026-11-11T19:14+08:00

## Inputs

| Input | Ref | Result |
|---|---|---|
| Local verify | `docs/ai4se-plugins/20261111-164232-local-verify-report.md` | Overall Pass — 8/8 allow-listed commands exit 0, V1–V8 all Pass, no Waiver in force |
| Pipeline ci | `docs/ai4se-plugins/20261111-191403-pipeline-report.md` (same body as `openspec/changes/add-staffing-mvp/ci-report.md`) | Pass — E1 全绿, run #5 on `56c96b9`, five jobs, 2m49s |
| Pipeline cd | n/a | The change's Non-Goal 10 rules out a production deployment pipeline, so there is no CD phase to verify |
| Traceability | `openspec/changes/add-staffing-mvp/traceability.md` | 90 rows = 82 API scenarios + 8 E2E, ids resolved against the sources by script |

The pipeline input is a read of the public run page plus a named human confirmation rather than a REST
API query (no `gh`/`glab`, the anonymous API was rate-limited); the report records both the URL and the
confirmer, so this is a UI-verified Pass, not an API-queried one.

## Dimension results

| Dimension | Result | Evidence |
|---|---|---|
| build | Pass | `./mvnw verify` 280 tests, JaCoCo lines 1489/1489 and branches 537/537 with "All coverage checks have been met."; `npm run lint` 0 problems, `npx tsc --noEmit` clean, `npm test` 39/39, `npm run build` green |
| scan | Pass with warnings | `npm audit --audit-level=high` exit 0 (9 moderate, all in the dev-only chain, 0 high, 0 critical); `dependency-review-action` green with 19 OpenSSF Scorecard warnings below the threshold of 3 |
| review | Pass | The local verify's V1–V8 checks were mechanical, not by eye (task count, scenario counts per capability 11/15/15/17/14/18, 82 API ids and 8 E2E titles resolved in the sources); spot review of `SecurityConfig`, `ConfirmationService` (fixed lock order post → person), `ImportService` (parse → validate → write in one transaction) and `web/src/lib/api.ts`; the data layer holds no SQL concatenation |
| evidence | Pass | 30/30 tasks closed with their own proof; 40 requirements carry a scenario (0 unbound); spec ↔ traceability sets equal at 90 rows |
| ci | Pass | Run #5, commit `56c96b9`: lint 42s, type check 1m 8s, unit tests 2m 30s, acceptance tests 2m 46s (82 API scenarios + JaCoCo gate + 10 Playwright tests), dependency and security scan 1m 3s |
| cd | n/a | No deployment pipeline exists; Non-Goal 10 says there will not be one |

## Defect grades

| Grade | Count | Items |
|---|---|---|
| CRITICAL | 0 | — |
| WARNING | 2 | W1: `SecurityConfig` disables CSRF for a session-cookie API with no CORS configuration, so the defence rests on same-origin alone. W2: 9 moderate advisories, all reachable only through the dev/test dependency chain (`npm audit --omit=dev` is clean) |
| SUGGESTION | 3 | All three from the local-verify report and all three closed before this gate: the consistency-lint S4 count (88 → 90 scenarios), the missing ADR-002 link in design §7 (both languages), and E1 having no remote evidence — now supplied by the CI report above |

## Waivers / conditions + owners

None. The local verify produced no Waiver, and this decision is unconditional.

## Open risks carried out of G2 (for G3 / archive)

| # | Risk | Owner | Disposition |
|---|---|---|---|
| R1 | CSRF is disabled while authentication rides on a session cookie (W1) | implementation owner | Accepted for a same-origin demo application with no cross-site surface; revisit before any multi-origin deployment |
| R2 | 9 moderate advisories in the dev-only chain (W2) | implementation owner | Accepted; `npm audit --omit=dev` is clean and the production tree carries none |
| R3 | `web/playwright-report` is never produced (the config uses the `list` reporter only), so the acceptance job's upload step warns | implementation owner | Open; add an `html` reporter when the harness is next touched |
| R4 | Node 20 and `actions/setup-java@v4` deprecation notices from GitHub | implementation owner | Open; bump `checkout` / `setup-node` / `setup-java` before GitHub removes the runtime |
| R5 | The candidate screen cannot post a recommendation, so journey J1 seeds that step through the API | product owner | Recorded as a product hole in T27 and in the journey; it is a UI gap, not a verification gap |

## Approvers

| Role | Name |
|---|---|
| VL (required) | repository owner, `BravelyTropicalfish` |

The agent that implemented the change did not sign this record.

## Traceability appendix (optional)

Not reproduced here — `openspec/changes/add-staffing-mvp/traceability.md` is the matrix (90 rows, every
scenario mapped to an API test id or an E2E title) and was checked by script rather than by eye.

## Next

- On Pass: merge PR #1, then the P5 archive lane (`/sdd:living-merge` → `/sdd:change-summary` →
  `/sdd:g3-decision-record`). G3 needs its own human signature.

## Addendum 1 — 2026-11-11, a walkthrough after the signature

`spec-gates` rule 5 says an earlier decision is not changed without a new record that explains why.
This is that record.

**What prompted it.** After signing, the running application was walked through screen by screen
instead of trusted from the test output. Two user-visible defects came out of it:

1. No screen posted `POST /api/session`, so a person with a browser could never get past
   `unauthenticated: Sign in to continue.` — every page was readable only by calling the API by hand.
   REQ-001 §2 defines sign-in as "a lightweight seeded account with a role": the accounts existed,
   the door did not. The T21 note had recorded "the login screen has no owning task" and moved on;
   that was the wrong call, because it makes the application unusable rather than merely incomplete.
2. A refused read printed its reason and kept the `Reading …` placeholder underneath it, so
   `forbidden: This action is not yours to take.` and `Reading the pool…` appeared together.

**What changed.** T31 in `tasks.md`: `/login` (`web/src/screens/LoginScreen.tsx`,
`web/src/lib/session.ts`, route in `App.tsx`, link in the shell) and the placeholder fix in
`PeopleScreen`, `PersonScreen`, `ProjectsScreen`, `NoticesScreen` and both tables of `CatalogScreen`.
`design.md` §8 now lists `/login`. No requirement, spec or acceptance criterion changed, and no test
was weakened.

**Re-verification on the same commit.** `npm test` 41/41 (two new readings in `tests/session.test.ts`);
`npx playwright test` 18/18, including three new specs in `e2e/login.spec.ts` — one of them asserts the
refused screen keeps its reason and stops saying it is reading; `npm run lint` 0 problems;
`npx tsc --noEmit` clean; `npm run build` green. The walkthrough itself also served as a manual pass
over the seeded accounts: the DEV post listed `李四 · 林一 · 陈二` as suitable and excluded
`张三 G3`, `王五 G6`, `赵六 G1` with the reasons the spec words, and the QA post opened the
cross-role fallback on an empty same-role list with `李四 · DEV · +1 band · 测试 · Practitioner`.

**Decision: unchanged — Pass.** The work is additive, tested at both unit and browser level, and the
five required CI checks are still the merge condition.

**Risks updated.** The sign-in gap in T21 is closed (R6, closed). R1–R5 stand: CSRF is still off,
the dev-only advisories are still moderate, the Playwright HTML report is still not produced, the
action runtimes are still deprecated, and the candidate screen still cannot post a recommendation
from the UI.

## Addendum 2 — 2026-11-11, the UI never followed the prototype

**What prompted it.** A reviewer asked why the running application does not look like
`prototype/` — the clickable mock that P2 produced. Measured, the gap was total: the prototype draws a
232px sidebar, a 56px top bar with the breadcrumb and the account chip, a full-width view with no
max-width, `radius 0.6rem` cards, KPI tiles and an eight-cell gate strip; the application rendered a
centred `max-w-4xl` column per screen with no chrome at all.

**Root cause, stated plainly.** `design.md` §8 fixed the routes and the wording each screen shows and
never required visual parity, and no task in `tasks.md` carried it as an acceptance criterion, so
every screen was built to be functionally correct and nothing else. Worse, at T26 the missing `/login` route
was used to *correct* the prototype's route list — the direction is backwards: a design artifact is
the visual contract, and a missing screen is a defect to fix, not a reason to rewrite the design.

**What changed.** T32: the prototype's tokens and class names now live in `web/src/index.css`, the
chrome in `web/src/components/AppShell.tsx` (a layout route around every screen), the grouped and
role-filtered links plus the breadcrumb rule in `web/src/lib/nav.ts`, KPI tiles on the project screen
and the gate strip on the candidate screen, and the sign-in screen restyled as the prototype's
centred card. No requirement, spec or wording asserted by a test changed — the shell spec was only
tightened to assert that a lead is offered the projects and *not* the pool.

**Re-verification.** `npm test` 46/46 (five new readings in `tests/nav.test.ts`), `npx playwright test`
18/18, `npm run lint` 0 problems, `npx tsc --noEmit` clean, `npm run build` green. Geometry measured
rather than eyeballed: at a 1574px viewport `.sidebar` is 232px, `.topbar` 56px, `.view` 1342px with
`padding: 14px 24px`, `scrollWidth === clientWidth` on all six screens, eight `.gate-cell` nodes with
the first reading `1 / G1 active`, and zero console errors.

**Decision: unchanged — Pass.** Additive presentation work, proven at unit, browser and measurement
level.

**Risks updated.** R7 (new): the overview at `/` shows three descriptive cards rather than numeric
KPI tiles, and the breadcrumb names the area (`Delivery / Project`) instead of the entity
(`Projects / Atlas`) — both cosmetic, both open.

## Addendum 3 — 2026-11-11, a project manager could not open a project

**What prompted it.** The product owner walked the application and described the flow the product
exists for: a project manager raises the requirement and opens a project, states the posts it is
short of, the coordinator reads the pool to see who qualifies and recommends them, and the project
manager decides whether to take each person. Three of those steps had no screen and one had the
wrong owner.

**What the walkthrough found, in the order the flow runs.**

1. `/projects` listed projects but offered no way to open one, and the rule said it should not be a
   lead's to open: `allocation/spec.md` read "The coordinator SHALL be able to create and edit a
   project; a lead SHALL NOT create a project", and `ProjectController.create` called
   `Auth.coordinator`. The chain therefore began nowhere for the role the product starts with.
2. `/posts/{id}/candidates` listed who qualifies and offered no way to recommend them. T27 had
   already recorded this as a product hole ("no screen posts a recommendation") when it seeded that
   step through the route; it was left open then and is closed now.

**What changed.** T33 moved the rule itself, not just the screen: REQ-001 rev 12 and the
`allocation` delta spec now say a lead opens a project of their own and the coordinator opens one
for any lead, recorded as **G1 rev 12** (Pass, IO = raylen, SA = kyrie) because `spec-gates` rule 5
forbids rewriting an agreed delta without a new record. `ProjectService.create` takes the signed-in
account, resolves the named lead first, and refuses a lead who names somebody else with `a lead
creates only a project of their own`; the coordinator's path is unchanged. T34 added the two
screens: `New project` on `/projects` (lead prefilled and locked for a lead) and a `Recommend`
button on every suitable and cross-role row that posts the same route the fixtures use, then
disables itself as `Proposed`.

**Re-verification.** `mvnw verify` 281 tests with "All coverage checks have been met." — the new
guard is covered at both branches (`ProjectApiTest` 11/11 with
`aLeadCreatesAProjectOfTheirOwnOnly`, `ProjectServiceTest` 5/5). `npm test` 49/49, `npx playwright
test` 19/19 (J1 now recommends on the screen; the new J3 drives the whole chain through screens
only), `npm run lint` 0 problems, `npx tsc --noEmit` clean, `npm run build` green. The same path was
then walked in the running application against the seeded accounts.

**Decision: unchanged — Pass.** The gate's own scope is why: G2 verifies that the delta the G1
record approved is implemented and proven. The delta changed, so the change re-entered P2 for a
recorded G1 rev 12, and the implementation of that delta is what this addendum re-verifies.

**Risks updated.** R8 (new): editing a project still has no route or screen although the
requirement says the coordinator may edit one — carried as a follow-up, not implied to work. R6's
follow-on ("the candidate screen still cannot post a recommendation") is closed. R7 stands. The
open items D1–D8 are untouched.
