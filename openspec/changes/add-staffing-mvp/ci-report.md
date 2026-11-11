# Pipeline Report: add-staffing-mvp

**Date:** 2026-11-11 (Asia/Shanghai, +0800)
**Root:** `c:\Users\ZENGKY\Desktop\New folder\baseline`
**Phase:** ci
**Data source:** mixed — the run page was read by the agent from the public repository (read-only); the
repository owner confirmed acceptance below, because no `gh`/`glab` and no token exist on the machine
**Change id:** `add-staffing-mvp`
**Target:** PR #1 `feat/add-staffing-mvp` → `main`, commit `56c96b9`
**Pipeline URL:** https://github.com/BravelyTropicalfish/staff-system/actions/runs/43009438047
**Commit:** `56c96b9`

## Summary

| Phase | Status | Conclusion | Upstream URL | Notes |
|-------|--------|------------|--------------|-------|
| ci | Pass | five required jobs success, 2m49s | https://github.com/BravelyTropicalfish/staff-system/actions/runs/43009438047 | run #5 on `56c96b9` |
| cd | n/a | no deployment pipeline exists | — | Non-Goal 10 of the proposal: no production deploy |
| **Overall** | **Pass** | ci phase green; cd out of scope by design | — | E1 全绿 |

## E1 判定 (Machine Evidence)

| Condition | E1 status |
|-----------|-----------|
| All in-scope jobs success | ✅ E1 全绿 — lint, type check, unit tests, acceptance tests, dependency and security scan |
| Any failed job | — none in this run |
| Skipped / manual not run | — none |
| No remote data | — the run page was read directly |

**E1:** ✅ 全绿

## Jobs / checks

| Phase | Job/Stage | Status | Duration | URL | Failure reason |
|-------|-----------|--------|----------|-----|----------------|
| ci | lint | success | 42s | run #5 | — |
| ci | type check | success | 1m 8s | run #5 | — |
| ci | unit tests | success | 2m 30s | run #5 | — |
| ci | acceptance tests | success | 2m 46s | run #5 | — |
| ci | dependency and security scan | success | 1m 3s | run #5 | — |

Stage mapping: lint → E1 base; type check → E1 base; unit tests → E1 base; acceptance tests → E1
base (82 API scenarios, JaCoCo gate, 8 E2E + 2 journeys); dependency and security scan → security.

## History — why the green run is the fifth one

This is recorded because a first-try green would be the misleading version of this story. Three
attempts failed, none of them because of the product code:

1. **run #3 (`5cd17a0`)** — all five jobs failed at `npm ci`:
   `getaddrinfo ENOTFOUND artifact.oocl.com`. `web/package-lock.json` pinned 455 `resolved` URLs at
   the internal Artifactory, which a GitHub runner cannot resolve. Fixed in `5cd17a0` by pointing the
   lockfile at `registry.npmjs.org` (hashes untouched; npm rewrites the host back to the corporate
   mirror for local installs).
2. **run #4 (`452680c`)** — four jobs green, `dependency-review-action` failed with "Dependency
   review is not supported on this repository": a private repository without GitHub Advanced
   Security refuses the action.
3. **the repository was made public**, which removes the licence requirement; the gate was restored
   to `if: github.event_name == 'pull_request'` (`452680c`) instead of being skipped or softened with
   `continue-on-error`, and the dependency graph was enabled in Settings → Advanced Security.
4. **run #5 (`56c96b9`)** — green.

## 失败详情

None in the in-scope run.

## 安全与合规

| Check | Status | Notes |
|-------|--------|-------|
| Secret scan | n/a | no secret-scanning workflow in this change; GitHub's public-repository defaults apply |
| SAST | n/a | no CodeQL workflow — adding one was out of scope, the change only claims the five required checks |
| Dependency audit | ✅ | `npm audit --audit-level=high` exit 0 (9 moderate, 0 high, 0 critical) and `dependency-review-action` on `fail-on-severity: high`; the review's first effective run produced 19 OpenSSF Scorecard warnings below threshold 3 and no high advisory |

## Residual risks (for local-verify / G2)

| # | Risk | Source | Status |
|---|------|--------|--------|
| 1 | E1 was read from the public run page rather than the REST API (no `gh`/`glab`, the anonymous API was rate-limited at 0/60) | tooling gap | accepted — the run URL above is public and the human confirmation below covers acceptance |
| 2 | 19 OpenSSF Scorecard warnings on transitive packages below threshold 3 (e.g. `duplexer2` 1.9, `deep-is` 1.9) | dependency review | accepted — informational, the gate fails only on high severity |
| 3 | `web/playwright-report` is never produced because the Playwright config uses the `list` reporter only, so the upload step warns and the acceptance job's sole artifact is `coverage-report` | workflow step | open — add an `html` reporter when the next change touches the harness |
| 4 | Node 20 deprecation and `setup-java@v4` deprecation notices from GitHub | actions versions | open — bump `checkout`/`setup-node`/`setup-java` before they stop working |
| 5 | No CD exists | Non-Goal 10 | accepted by design — there is no deployment to report |

## Artifacts (if any)

- `coverage-report` (550 KB) — uploaded by the acceptance job with 30-day retention.

## Human confirmation

| Field | Value |
|-------|-------|
| Confirmer (named role/person) | repository owner, `BravelyTropicalfish` |
| Phase covered | ci |
| Explicit statement | I confirm the ci phase is accepted on the evidence read from the public run page |
| Timestamp | 2026-11-11T19:15+0800 |

## Next

- ✅ E1 全绿 → `/sdd:g2-decision-record` (G2 still needs the human sign-off; the agent does not sign).
- `/sdd:deploy-artifacts` does not apply: the change's Non-Goal 10 rules out a deployment pipeline,
  so there is no CD phase to report.

## Update — 2026-11-11, an API-verified run

The unauthenticated REST quota reset later the same evening, so the run above was re-read through the
API instead of the page. `GET /repos/BravelyTropicalfish/staff-system/actions/runs?per_page=1` answers
run **#7** (`id=43014284135`) on `842feff`, `conclusion: success`, and its jobs endpoint gives:

| Job | Conclusion | Duration |
|---|---|---|
| lint | success | 49s |
| type check | success | 49s |
| unit tests | success | 110s |
| acceptance tests | success | 142s |
| dependency and security scan | success | 47s |

Runs #5 (`56c96b9`) and #6 (`36b37d4`) are `success` as well, and #2 (`5cd17a0`) and #4 (`452680c`) are
the `failure` runs the history section above explains. With this update E1 is API-queried for the head
commit rather than page-read; the human confirmation stands as the record for the run #5 snapshot.
