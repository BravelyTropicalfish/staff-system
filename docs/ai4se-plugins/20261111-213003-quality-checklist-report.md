# Quality Checklist Report: p3

**Date:** 2026-11-11 (timezone: Asia/Shanghai)
**Root:** c:\Users\ZENGKY\Desktop\New folder\baseline
**Stages:** p3
**Focus:** full-stage

## Summary

### p3

| Check id | Required? | Status | Evidence |
|----------|-----------|--------|----------|
| P3-1 ATDD design artifact exists | yes | Pass | `openspec/changes/add-headcount-charts/atdd-design.md` (155 lines) + `atdd-design.zh.md` (120 lines): layers table, 8 API / 4 E2E split with the E2E set S8/S9/S10/S12 and its reasons, deferred unit list, what is written before code, gate alignment, fixtures, non-goals, **§6 prototype alignment** (element → built-as, with the two review-only parts named) and §7 NFR (authorisation + freshness, confirmed with the product owner this session) |
| P3-2 Design / tasks artifacts exist | yes | Pass | `design.md` + `design.zh.md`: packages per layer, `GET /api/headcount` shape, the two SQL reads, "no schema change", the component tree above the pool's two columns, the session-before-fetch ordering, a decisions table with rejected alternatives, the three test commands and the risks carried into apply. `tasks.md` + `tasks.zh.md`: T01–T10 |
| P3-3 Alignment self-check vs Delta Spec, no silent scope creep | yes | Pass | Script run against the files (not by eye): spec 6 req / 12 scen in **both** languages; `atdd-design.md` claims 8 / 4 and its E2E table is exactly S8, S9, S10, S12; tasks hold 10 tasks with 10 `Layer:`, 10 `Blocked by:` and 10 `Evidence:` lines; the traceability table references S1–S12 exactly once with **API = 8, E2E = 4**; every one of the 12 spec scenario titles appears verbatim in the tasks evidence (`missing: none`); the tasks' E2E set equals the ATDD E2E set; `design.md` ↔ `atdd-design.md` ↔ `tasks.md` cross-reference each other. Scope: one capability, matching the single row of the proposal; the prototype's state switcher and legend card are explicitly **not** built |
| P3-4 Task list is actionable | yes | Pass | 10 `- [ ]` items grouped in four stages, each carrying Layer / Blocked by / Evidence; the dependency chain is stated (T02–T05 after T01, T06 after T01–T05, T07 after T06, T10 last) so the order is not left to the reader. No per-task named owner — a single-developer change — which this row allows ("checkboxes / clear owners") |

| **Stage Overall** | — | **Pass** | all four required rows Pass |
### Roll-up

| Scope | Overall | Notes |
|-------|---------|-------|
| selected stages (p3) | Pass | single stage; no Fail and no Partial |

## Gaps

| Check id | Gap | Owner (if known) | Suggested next |
|----------|-----|------------------|----------------|
| — | No blocking gap for P3 | — | `/sdd:atdd-apply` (P4). G2 stays closed until local verify is green |

## Notes

- Optional / not-run / out-of-focus items marked Skipped do not block Overall Pass.
- Overall aggregates in-focus required items only.
- This report is not a Gate decision.
- **Tooling gap observed, not a change gap:** the `probe-stage.sh` p3 target list checks `design.md` and `tasks.md` but not `atdd-design.md`, so P3-1's evidence is a direct read of the file rather than a `FOUND` line. Worth fixing in the harness, out of scope here.
- **What P3 did not do:** no production code, no unit-test suite, no CI change (the five-check workflow already exists). The new web test file joins the explicit `npm test` list in T10 — and T10 also carries the instruction not to drop the existing entries, because `web/tests/board.test.ts` is still missing from that list today.
- **Prototype as the visual contract:** `atdd-design.md` §6 is the artifact that keeps the build honest against the prototype the reviewers accepted, including the two prototype behaviours that must survive (the zero-bar stub and the role-known-before-fetch ordering) and the two that must not (the state switcher and the legend card).
