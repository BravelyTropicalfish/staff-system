# Handoff — requirement workspace → OpenSpec

- **Workdir path:** `requirement-workspace/` (override if relocated)
- **OpenSpec changes root:** `openspec/changes/`
- **Rule:** Copy or link stabilized requirements into a change package. This
  workdir is never the long-term Living Spec authority (`openspec/specs/`).

| Artifact | Destination | Notes |
|---|---|---|
| `requirements/` entries | `openspec/changes/<id>/` proposal inputs | After `/sdd:pre-spec-req` |
| Ready / analysis packs | change package or archive | Project convention |

## Active handoff

| Requirement | Change | Handed over | State |
|---|---|---|---|
| `requirements/REQ-001-staffing-mvp.md` (rev 8, + `analysis/impact-brief.md`) | `openspec/changes/add-staffing-mvp/` | 2026-11-10 | P2 done: proposal + 6 delta specs (`specs/staffing/*/spec.md`, 41 requirements, 89 scenarios) + `dov-ac-review.md` (AC/DoV all Pass, AC-2/AC-4/DoV-1/DoV-5 after fixes) + `consistency-lint.md` (Partial, capped by absent `openspec` CLI; L1/L2 and second-pass L4–L6 fixed, L3 open). Rev 8 added the cross-role fallback tier: a new `candidate-matching` requirement with six scenarios (S33–S38), a new `people-pool` requirement for the role skill domain, and a snapshot-evidence scenario in `staffing-confirmation`; the third pass re-verified counts, traces, G/W/T, all 11 rule quotations and `S1`–`S38` coverage. G1 signed 2026-11-10 (IO raylen, SA kyrie) — Pass, see `g1-decision.md`; D8 left open with its MVP default (no cap, no extra approval). `design.md` and `tasks.md` not yet created (P3). Review aid: `prototype/` (static clickable mock, login page for the two seeded accounts; candidate search demonstrates the cross-role fallback on P-1043, the catalog shows each skill's role domain). |
