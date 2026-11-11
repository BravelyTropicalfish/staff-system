# G3 decision — add-staffing-mvp

- **Gate:** G3 (Merge & Archive)
- **Change id:** `add-staffing-mvp` (REQ-001, rev 13 in force)
- **Timestamp:** 2026-11-11T23:32+08:00 (condition 1 closed 2026-11-11T23:58+08:00)

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
| Archive completeness | Pass | The destination copy is complete (30 files). The emptied source folder was removed after the owner closed the terminal that held it; `openspec list` shows only `add-headcount-charts` (see Closure below) |
| Harness / Rules evolve | Not required | The CI browser-install failure was fixed in the workflow (browser-only install + cache, job pinned to `ubuntu-24.04`); no Rules change is implied |
| Gate records | Pass | G1 rev 13 signed 2026-11-11 (IO raylen / SA kyrie); G2 Pass with addenda 1–8 |

## Decision

**Pass.**

The merge, the archive copy and the knowledge-base check are complete, the emptied source folder is
gone, and both `local verify` and CI are green. The decision started as Conditional on that one file
handle and was closed the same day (see Closure).

## Conditions + owners

None — condition 1 was met before the record became final.

## Closure — 2026-11-11, condition 1 met

| Field | Value |
|---|---|
| Condition | The emptied `openspec/changes/add-staffing-mvp/prototype` directory held by an external handle |
| Closed by | raylen closed the terminal whose working directory was that folder; the agent then removed the leftover directory |
| Verification | `Remove-Item openspec\changes\add-staffing-mvp -Recurse -Force` succeeded, and `openspec list` now shows only `add-headcount-charts` |
| Decision after closure | **Pass** (SA cecil, 2026-11-11) |

Re-verification after the closure: the archive copy still holds 30 files, `openspec/specs/staffing/*/spec.md`
still counts 40 requirements / 90 scenarios, and `node scripts/kb-lint.mjs` prints
`24 entries, every _INDEX.md link resolves`.

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

- G3 is **Pass**; the change is archived and the living specs carry the 40 requirements.
- `/sdd:deploy-artifacts` does not apply (Non-Goal 10). CD remains out of scope by design.
