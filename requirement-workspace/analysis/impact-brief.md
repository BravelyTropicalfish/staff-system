# Impact brief — REQ-001 Staffing MVP

- **Requirement set:** `requirement-workspace/requirements/REQ-001-staffing-mvp.md`
- **Change:** `openspec/changes/add-staffing-mvp/`
- **Date:** 2026-11-10
- **Verdict:** greenfield build, no as-is behaviour to preserve; no critical blocker
  for splitting into one change.

## 1. As-is asset inventory (inspected)

| Asset | Path | State |
|---|---|---|
| Living specs | `openspec/specs/` | empty (`.gitkeep` only) |
| Change packages | `openspec/changes/` | empty (`archive/.gitkeep` only) |
| Knowledge base | `docs/knowledge/_INDEX.md`, `docs/knowledge/**/README.md` | placeholder slots, no domain content |
| Rules | `docs/knowledge/rules/**` | architecture / quality / security / testing / process rules only |
| Application code | repository root | none (no package manifest, no source tree) |
| Pre-spec workdir | `requirement-workspace/` | README / handoff / registry stubs only |

## 2. As-is → proposed delta

| Touched asset | As-is | Proposed delta |
|---|---|---|
| `openspec/specs/staffing/*` | does not exist | six new capabilities created by this change's delta specs |
| `openspec/changes/add-staffing-mvp/` | does not exist | new change package (proposal now; specs / design / tasks later) |
| Application tree | does not exist | new application: UI, business logic (gates, score, write-back), data access, report importer |
| Person master data flow | does not exist | people are created and refreshed only through an uploaded workbook (employee number key, incremental merge, per-batch error list) |
| Role / skill catalogs | do not exist | catalog items carry an active flag, freeze their name once referenced and are never deleted; seeded with five roles and six skills |
| `requirement-workspace/registry.md` | empty table | REQ-001 row pointing at this change |
| `requirement-workspace/handoff.md` | generic mapping | change-specific handoff row |

Checked, no impact:

- `docs/knowledge/rules/**` — rules constrain the build; no rule changes required.
- `docs/knowledge/architecture/README.md` — placeholder; a short ADR for the runtime
  stack is expected at design time (rule 7 of `architecture-baseline.md`).

## 3. Blast radius

- New runtime only. No existing system is called or modified.
- New persistence for people, skills, projects, allocations, posts, recommendations
  and assignments.
- No external interface is created or consumed (no HR sync, no notification channel).

## 4. Artifact impact

| Artifact | Impact |
|---|---|
| Living specs | six ADDED capabilities under `staffing/` |
| Tests | new unit tests for gates, score, ranking, the cross-role fallback tier, import validation, post lifecycle, catalog deactivation and write-back; E2E for S1–S38 |
| Docs / KB | new domain KB entry for staffing vocabulary; one ADR for the runtime stack |
| Operations | none at MVP (local run only) |

## 5. Dual-track notes

- OpenSpec track: one change, six capability deltas; clean vertical slice with a
  demo-able path from "post raised" to "person confirmed".
- Agile track (optional): would naturally split into four stories — people import,
  people pool and catalogs, post requirement and lifecycle, match-and-confirm — with
  S1–S38 as acceptance hints.
- No conflict between the two tracks: story boundaries map onto capability
  boundaries.

## 6. Risks and blockers

| # | Risk | Severity | Handling |
|---|---|---|---|
| R1 | Score weights are policy, not fact; poor defaults could bury good candidates | medium | Show per-criterion reasons and exclusion gates; weights fixed in delta spec at G1 |
| R2 | Date-only, full-time allocation hides over-allocation | medium | Explicit non-goal D2; state it in the UI where allocations are entered |
| R3 | Inclusive date intersection surprises users (S9) | low | Stated rule + literal test case |
| R4 | Runtime stack undecided | medium (design-time blocker, not split blocker) | Human decision at design; `shadcn/ui` front end is the repo default |
| R5 | Single large change increases review load | low | Task order in tasks.md follows the flow: catalogs → import → pool → post → match → confirm |
| R6 | Report quality: blank or duplicated employee numbers, out-of-range bands, unknown skills | medium | Whole-file validation with a complete per-row error list; a bad row now blocks the batch instead of being skipped |
| R7 | No in-app edit path means a typo needs a re-upload | low | Batch history makes each correction traceable; catalogs stay editable for the skill-catalog gap |
| R8 | One person recommended to several posts confuses coordinators when a confirmation voids the others | medium | Every voided recommendation lands in a notice list with its reason; the notice is part of the acceptance scenarios (S19) |
| R9 | Locking a post's gate-bearing fields while a person is confirmed will feel restrictive to leads | medium | The refusal names the exact field and tells the lead to release first; the alternative (silent allocation drift) was rejected in B14 |
| R10 | All-or-nothing import: one wrong cell blocks a whole upload, which can annoy the coordinator on large files | medium | The error list names every bad row at once, so one correction round-trip suffices; the batch history shows why a file failed |

No split blocker: requirements are decidable, no integration boundary, no legacy
behaviour to protect.

## 7. Suggested split boundaries

- One change (`add-staffing-mvp`) per the product owner's decision.
- Internal task phasing for later steps: (1) catalogs, (2) report import and batch history,
  (3) projects + allocations, (4) posts + gap view, (5) matching, (6) recommendation
  and confirmation write-back. Phases are the capability boundaries above, so the
  capability deltas double as task groups.
