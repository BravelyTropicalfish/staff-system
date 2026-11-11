# Impact brief — REQ-001 Staffing MVP

- **Requirement set:** `requirement-workspace/requirements/REQ-001-staffing-mvp.md`
- **Change:** `openspec/changes/add-staffing-mvp/`
- **Date:** 2026-11-11 (rev 11: allocations cover the post's inclusive dates, only an intersecting
  range blocks, and the project end still releases the team)
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
| Application tree | does not exist | new application: UI, business logic (gates, the suitable list, write-back), data access, report importer |
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
| Tests | new unit tests for gates, the date-overlap test (both boundary days), the suitable list's name order, the cross-role fallback tier, import validation, post lifecycle, catalog deactivation, the project end and write-back; E2E for S1–S40 |
| Docs / KB | new domain KB entry for staffing vocabulary; one ADR for the runtime stack |
| Operations | none at MVP (local run only) |

## 5. Dual-track notes

- OpenSpec track: one change, six capability deltas; clean vertical slice with a
  demo-able path from "post raised" to "person confirmed".
- Agile track (optional): would naturally split into four stories — people import,
  people pool and catalogs, post requirement and lifecycle, match-and-confirm — with
  S1–S40 as acceptance hints.
- No conflict between the two tracks: story boundaries map onto capability
  boundaries.

## 6. Risks and blockers

| # | Risk | Severity | Handling |
|---|---|---|---|
| R1 | Gate thresholds are policy, not fact; a wrong minimum level or `required` skill hides good people | medium | Each row shows the gates it passed and each excluded person shows the gate that excluded them, so a wrong threshold is visible; the values live on the post, not in a hidden blend |
| R2 | The dates bound the occupancy, so a person on a light project is blocked only on the overlapping days — but a mis-typed range can still hide them from the wrong posts | medium | Explicit non-goal D2 (no allocation percentage); the profile lists every allocation with project, start and end, and the exclusion reason names the overlapping allocation, so the coordinator can correct or remove it |
| R3 | Both end points are inclusive, so one day of overlap blocks and a typo in an end date can hide a person from a post that would otherwise fit (S5, S9) | low | Stated rule + literal test case + the post and the profile both show their dates; a warning on a near-miss range is a later convenience, not a rule |
| R4 | Runtime stack undecided | medium (design-time blocker, not split blocker) | Human decision at design; `shadcn/ui` front end is the repo default |
| R5 | Single large change increases review load | low | Task order in tasks.md follows the flow: catalogs → import → pool → post → match → confirm |
| R6 | Report quality: blank or duplicated employee numbers, out-of-range bands, unknown skills | medium | Whole-file validation with a complete per-row error list; a bad row now blocks the batch instead of being skipped |
| R7 | No in-app edit path means a typo needs a re-upload | low | Batch history makes each correction traceable; catalogs stay editable for the skill-catalog gap |
| R8 | One person recommended to several posts confuses coordinators when a confirmation voids the others | medium | Every voided recommendation lands in a notice list with its reason; the notice is part of the acceptance scenarios (S19) |
| R9 | Locking a post's gate-bearing fields while a person is confirmed will feel restrictive to leads | medium | The refusal names the exact field and tells the lead to release first; the alternative (silent allocation drift) was rejected in B14 |
| R10 | All-or-nothing import: one wrong cell blocks a whole upload, which can annoy the coordinator on large files | medium | The error list names every bad row at once, so one correction round-trip suffices; the batch history shows why a file failed |
| R11 | Ending a project is coarse and irreversible: it cancels every post, filled ones included, and cannot be undone | medium | The confirmation names the posts and people that will be released; since rev 11 the dates, not the project, block a person, so the end is a convenience rather than the only way to free someone; a partial close or reopen path is a later change |

No split blocker: requirements are decidable, no integration boundary, no legacy
behaviour to protect.

## 7. Suggested split boundaries

- One change (`add-staffing-mvp`) per the product owner's decision.
- Internal task phasing for later steps: (1) catalogs, (2) report import and batch history,
  (3) projects + allocations and the project end, (4) posts + gap view, (5) matching,
  (6) recommendation and confirmation write-back. Phases are the capability boundaries
  above, so the capability deltas double as task groups.
