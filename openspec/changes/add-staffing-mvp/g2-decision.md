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
