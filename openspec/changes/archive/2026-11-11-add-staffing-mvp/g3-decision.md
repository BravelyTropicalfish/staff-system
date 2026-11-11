# G3 decision — add-staffing-mvp

- **Gate:** G3 (Merge & Archive)
- **Change id:** `add-staffing-mvp` (REQ-001, rev 13 in force)
- **Timestamp:** 2026-11-11T23:32+08:00

## Input artifacts

| Input | Ref | Result |
|---|---|---|
| Living merge | `openspec archive add-staffing-mvp --yes` (OpenSpec CLI 1.17.0) | Delta specs merged into `openspec/specs/staffing/*/spec.md`: **+40** requirements, 0 modified, 0 removed |
| Change summary | `openspec/changes/archive/2026-11-11-add-staffing-mvp/change-summary.md` | Intent, shipped scope, verify highlights, residual risks, links |
| Archive completeness | `openspec/changes/archive/2026-11-11-add-staffing-mvp/` | Copy complete: 30 files (proposal, design, tasks, traceability, six `spec.md` + six `spec.zh.md`, prototype, G1/G2 records, change summary, ci report) |
| Merge conflicts | — | None: all six capabilities were new (`create`), no existing living spec to reconcile |
| Knowledge base | `scripts/kb-lint.sh` → `scripts/kb-lint.mjs` | Exit 0 — `24 entries, every _INDEX.md link resolves`; the repository has no KB writer, so the KB step validates the tree instead of importing change prose |
| Local verify | `docs/ai4se-plugins/20261111-220510-local-verify-report.md` | Overall **Pass** (E3 Pending, no waiver): 8/8 commands exit 0, 297 backend tests with the JaCoCo gate, 54 web unit tests, 20 Playwright tests |
| Pipeline ci | `docs/ai4se-plugins/20261111-231243-pipeline-report.md` | **Pass** — E1 全绿, run #12 on `a11bffd`, five jobs, 2m28s |
| Pipeline cd | n/a | Non-Goal 10 rules out a deployment pipeline |

## Check conclusions

| Check | Result | Evidence |
|---|---|---|
| Merge outcome | Pass | 40 requirements added across the six capabilities (+5 allocation, +5 candidate-matching, +5 people-import, +8 people-pool, +8 post-requirement, +9 staffing-confirmation); the merged tree counts 40 requirements / 90 scenarios |
| Conflicts | None | Every capability was absent from `openspec/specs/` before the merge |
| Archive completeness | **Conditional** | The destination copy is complete (30 files). The source folder could not be removed completely: its emptied `prototype` directory is held by an external Windows handle, so `openspec/changes/add-staffing-mvp/` still exists and `openspec list` shows a task-less `add-staffing-mvp` |
| Harness / Rules evolve | Not required | The CI browser-install failure was fixed in the workflow (browser-only install + cache, job pinned to `ubuntu-24.04`); no Rules change is implied |
| Gate records | Pass | G1 rev 13 signed 2026-11-11 (IO raylen / SA kyrie); G2 Pass with addenda 1–8 |

## Decision

**Conditional.**

A Pass is not recorded because the archive is not complete: the emptied source folder is still on
disk (blocked by a file handle, not by content). Everything else — spec merge, archive copy, KB check,
local verify and CI — is green.

## Conditions + owners

| # | Remediation item | Owner | Deadline |
|---|---|---|---|
| 1 | Close the browser/editor handle on `openspec/changes/add-staffing-mvp/prototype`, delete the leftover `openspec/changes/add-staffing-mvp/` directory, and confirm `openspec list` shows only `add-headcount-charts` (no task-less `add-staffing-mvp`) | raylen (implementation owner) | 2026-11-12 |
| 2 | Record the closure here (a short addendum) once the directory is gone; the decision then reads Pass without re-running the lane | SA cecil | 2026-11-12 |

## Open risks carried past G3

| # | Risk | Disposition |
|---|---|---|
| R1 | `SessionTokens` keeps per-tab tokens in process memory with no expiry; a restart clears them | Accepted for the single-process MVP; a TTL belongs to a later change |
| R2 | CSRF stays disabled while the session cookie remains a credential | Accepted (pre-existing, same-origin demo) |
| R3 | 9 moderate npm advisories in the dev-only chain; 19 OpenSSF Scorecard warnings below threshold 3 | Accepted; `npm audit --audit-level=high` and dependency review are green |
| R4 | `web/playwright-report` is never produced (list reporter only), so the upload step warns | Open; add an `html` reporter when the harness is next touched |
| R5 | Node 20 / `setup-java@v4` deprecation notices | Open; bump the actions before the runtime is removed |
| R6 | The working tree still carries owner-side dev tooling (`dev/H2ConsoleConfig.java`, application properties, `pom.xml`) and the parallel REQ-002 work | Not part of this change; belongs to the owner's own commits |

## Approvers

| Role | Name | Date |
|---|---|---|
| SA (required) | cecil | 2026-11-11 |

The agent that ran the merge recorded this decision on the approver's instruction and signs nothing
itself. AO AI4SE improvement asks: none attached.

## Next

- The owner closes condition 1, the SA appends the closure, and the record reads Pass.
- `/sdd:deploy-artifacts` does not apply (Non-Goal 10). CD remains out of scope by design.
