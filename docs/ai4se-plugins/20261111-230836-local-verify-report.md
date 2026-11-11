# Local Verify Report: add-headcount-charts

**Date:** 2026-11-11 (timezone: Asia/Shanghai)
**Root:** c:\Users\ZENGKY\Desktop\New folder\baseline
**Scope:** local+evidence
**Change id:** add-headcount-charts

## Summary

| Dimension | Status |
|-----------|--------|
| Build | Pass |
| Static scan | Pass (9 moderate npm advisories, none high or critical) |
| Code review | Pass |
| Evidence (OpenSpec) | Pass |
| **Overall** | **Pass** |

**PR gate:** Pass — `/sdd:pr-submit` may proceed. The Waiver section is not needed.
Agents must not self-approve Waiver.

## Local commands

All seven come from `docs/feedback/verification-commands.md`; none was invented.

| Command | Source | Exit | Summary |
|---------|--------|------|---------|
| `cd web && npm run lint` | verification-commands.md · Static | 0 | ESLint over `src tests e2e`, no problems |
| `cd web && npx tsc --noEmit` | verification-commands.md · Static | 0 | the whole web tree type-checks, `tests/` and `e2e/` included |
| `cd backend && ./mvnw -B -DskipTests compile` | verification-commands.md · Static | 0 | Java 21 sources compile |
| `cd backend && ./mvnw -B verify` | verification-commands.md · Runtime (coverage gate) | 0 | **316 tests**, `All coverage checks have been met.` — 100 % line and branch on `domain*` / `api*` |
| `cd web && npm test` | verification-commands.md · Runtime | 0 | **60 tests**, 0 failures (includes the new `tests/headcount.test.ts`) |
| `cd web && npm run test:e2e` | verification-commands.md · System | 0 | **24 passed (52.4 s)** — 3 setup + 4 headcount + 3 journeys + 4 login + 8 scenarios + 2 smoke |
| `cd web && npm audit --audit-level=high` | verification-commands.md · System | 0 | 9 moderate advisories, **0 high or critical** |

Runner log: `docs/ai4se-plugins/20261111-230458-local-verify-run.log` (7 attempted, 0 failed).
`npm ci` was **not** re-run: the dependency tree is already installed and the registry is the internal Artifactory, so the lint command ran as `npm run lint` on the existing install. Recorded as a deviation, not a pass.

## Code-review highlights

- **The authorisation order is the interesting one.** `PeopleScreen` reads `/api/session` first and mounts `HeadcountSection` only for a coordinator, so a lead's page issues **no** `/api/headcount` request. The E2E test asserts the empty request log rather than the hidden DOM — hiding after the fetch would satisfy the pixels and fail the sentence. [fact]
- **One rounding rule.** `HeadcountService.share` is the only division in the change; the client prints `share.toFixed(1)` and never recomputes. `HeadcountShareTest` pins the half-up branch (`1/8` → `12.5`) and the no-divisor branch. [fact]
- **The zero bar is a deliberate 3 px stub** (`.bar.is-zero`), because a 0 px element cannot be seen while the category still belongs on the axis; the label carries `0` / `0.0%`. [fact]
- **Nothing is stored.** Two read-only queries, no table, no cache; the freshness test reads, imports, reads again and sees both totals move. [fact]
- **The answer carries no personal detail**: the API test asserts the exact key set at both levels (`total`/`byRole`/`byGrade`, and `roleId`/`role`/`count`/`share` versus `grade`/`count`/`share`). [fact]
- Patterns match the neighbours: `Service` + `*View` record + thin controller, `JdbcTemplate` repository, `ApiSession` fixtures, `node --test` readings, Playwright with the shared storage states. [fact]

## Evidence checklist

| ID | Check | Result | Mark | Notes |
|----|-------|--------|------|-------|
| V1 | Task completion | Pass | [fact] | `tasks.md` 10 of 10 `[x]`; `tasks.zh.md` identical |
| V2 | Requirement coverage | Pass | [fact] | 6 requirements in the delta spec; each maps to a named API or E2E check (`tasks.md` traceability table) |
| V3 | Scenario coverage | Pass | [fact] | 12 scenarios, each referenced exactly once, 8 API + 4 E2E — re-counted by script at P3 and again here |
| V4 | Requirement ↔ implementation mapping | Pass | [fact] | `HeadcountRepository`/`Service`/`Controller` for R1–R3, `HeadcountSection`/`ChartCard` for R4, `Auth.coordinator` for R5, the empty answer plus `chart-empty` for R6 |
| V5 | Spec drift | Pass | [fact] | one drift was found and fixed during apply: `design.md` still described the pool screen's old two-column layout; the component tree was corrected to the table + profile modal |
| V6 | Design follow-through | Pass | [fact] | route `GET /api/headcount`, no schema change, session-before-fetch ordering, share computed once — all four design decisions are visible in the code |
| V7 | Code-pattern fit | Pass | [fact] | see the code-review highlights; no new dependency, no new pattern |
| V8 | Upstream basis (necessity) | Pass | [fact] | every changed file traces to a requirement or to a task: 6 backend sources, 2 backend tests, 3 web sources, 2 web test files, `PeopleScreen`, `index.css`, `package.json` (the test list only), and the change/requirement docs. The prototype and `.gitignore` change are review support, not product code |

## Evidence slots (E1 / E2 / E3)

| Slot | Status | Source |
|------|--------|--------|
| E1 Machine | Pass (local) | the seven whitelist commands above; the remote CI run needs the pull request, which this environment cannot open (no `gh`, no `glab`) |
| E2 AI | Pass | this report plus the P1/P3 quality checklists and the P2 AC/DoV review |
| E3 Human | Pending | a human signs G2; this report does not |

## Five-question map

| Q | Maps to | Status |
|---|---------|--------|
| Q1 Complete? | V1–V3 | ✅ 10/10 tasks, 6 requirements, 12 scenarios |
| Q2 Correct? | V4–V5 | ✅ mapping complete; one doc drift fixed |
| Q3 Coherent? | V6–V7 | ✅ design decisions and local patterns both hold |
| Q4 Necessary only? | V8 | ✅ nothing beyond the capability; prototype is review aid |
| Q5 Risk controllable? | residual risks below | ✅ open items are named with owners |

## Findings

### CRITICAL

None.

### WARNING

| # | Finding | Mark | Location | Suggestion |
|---|---------|------|----------|------------|
| 1 | 9 moderate npm advisories (the `typescript-eslint` chain), none high or critical; `--audit-level=high` exits 0 | [fact] | `web/package-lock.json` | dev-only; upgrade the toolchain in a separate change |
| 2 | The empty-pool E2E check depends on running before any spec that imports people | [fact] | `web/e2e/headcount.spec.ts` | the test asserts its own precondition (`total === 0`) so a reordering fails loudly instead of passing silently |
| 3 | The remote pipeline has not run for this change, so there is no CI evidence yet | [fact] | — | open the PR, then `/sdd:pipeline-report` |

### SUGGESTION

| # | Finding | Mark | Location | Suggestion |
|---|---------|------|----------|------------|
| 1 | `web/tests/board.test.ts` is still not in the `npm test` list (pre-existing, unrelated) | [fact] | `web/package.json` | fix in its own change; this change did not widen the gap |
| 2 | A chart of many zero bars reads as empty at a glance | [inference] | `ChartCard` | verify with a screenshot on a lopsided pool; the total line is the current mitigation |

## Residual risks

| # | Risk | Why open | Mitigation |
|---|------|----------|------------|
| 1 | D2 — the brief's two sample totals differ, so the instructor may have meant two populations | carried from G1 | accepted: one rule (requirement 1) would change; nothing else moves |
| 2 | A lopsided pool draws several zero bars | product accepted at G1 | accepted: total line names the pool; the empty state replaces everything when nobody is active |
| 3 | Coverage gate depends on the new classes staying at 100 % | mechanical | tracking: the two risky branches (zero bar, empty pool) ship with tests |
| 4 | P5 archive is blocked by the absent `openspec` CLI | tooling | tracking: the change package is hand-authored to the documented layout |

## Waiver

N/A — no waiver.
