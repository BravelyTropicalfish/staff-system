# G1 decision — add-staffing-mvp

- **Gate:** G1 (Align)
- **Change id:** `add-staffing-mvp`
- **Decision:** **Pass**
- **Approvers:** IO = raylen; SA = kyrie
- **Timestamp:** 2026-11-10

The decision belongs to the two named approvers. This file records what they decided; the
agent wrote it on their instruction and issues no approval of its own.

## Input artifacts

| Artifact | Path | Result |
|---|---|---|
| Proposal (English / Chinese) | `proposal.md`, `proposal.zh.md` | 8 What-Changes steps, 6 new capabilities, open items D1–D8 |
| Delta spec — `people-pool` | `specs/staffing/people-pool/spec.md` | 8 requirements, 17 scenarios |
| Delta spec — `people-import` | `specs/staffing/people-import/spec.md` | 5 requirements, 15 scenarios |
| Delta spec — `allocation` | `specs/staffing/allocation/spec.md` | 5 requirements, 8 scenarios |
| Delta spec — `post-requirement` | `specs/staffing/post-requirement/spec.md` | 8 requirements, 13 scenarios |
| Delta spec — `candidate-matching` | `specs/staffing/candidate-matching/spec.md` | 6 requirements, 18 scenarios |
| Delta spec — `staffing-confirmation` | `specs/staffing/staffing-confirmation/spec.md` | 9 requirements, 18 scenarios |
| AC / DoV review | `dov-ac-review.md` | AC-1–AC-4 and DoV-1–DoV-5 all Pass; verdict "fit to go to G1" |
| Consistency lint (p2) | `consistency-lint.md` | **Partial** — 0 BLOCKER; L1–L10 found across three passes and fixed except suggestion L3; capped by the absent `openspec` CLI |
| Source requirement | `requirement-workspace/requirements/REQ-001-staffing-mvp.md` (rev 8) | 41 requirements / 89 scenarios traced; S1–S38 covered |
| Review aid | `prototype/` | Clickable static mock of the agreed behaviour |

Totals re-counted before this decision: **41 requirements / 89 scenarios**, 41/41 `**Traces:**`
lines, 89/89 GIVEN/WHEN/THEN, 11/11 quoted key rules present literally in `proposal.md`.

## Findings

- **Scope.** The six delta capabilities match the six "New capabilities" rows one to one. No
  `MODIFIED` or `REMOVED` section, because `openspec/specs/` is empty. Two scope corrections were
  made before this gate: project status as informational was added and `proposal.md` step 3
  amended in the same pass; a `post-requirement` clause requiring post dates to sit inside the
  project dates was removed because no upstream artifact states it, and the question became
  open item **D7**.
- **Ambiguity.** REQ-001 §11 B1–B18 are resolved. D1–D6 are deferred inside REQ-001; D7–D8 sit
  in `proposal.md`. Every deferred item is outside the delta, so none of them leaves a rule
  undecided. **D8** (cap on the cross-role section, or an extra approval before confirming a
  cross-role person) is recorded with its MVP default: no cap, no extra approval.
- **Conflict.** No contradiction remains across the six capabilities. One draft scenario at
  revision 8 asserted that a cross-role person never outranks a same-role person, which the
  trigger makes impossible; the scenario was deleted and the exclusivity kept as a normative
  sentence. Gate G2 is explicitly not relaxed: the cross-role tier uses its own gates X1
  (required rank + 1) and X2 (a domain skill at `Practitioner` or above), which stand in for G3
  and for G2 + G4, with G1, G5 and G6 still applied.
- **Traceability.** The union of trace lists covers S1–S38; each capability names its proposal
  step and its REQ-001 section.

## Conditions + owners

None. The decision is Pass, so no remediation is owed at this gate.

## Open risks accepted with this decision

1. **Lint is Partial, not machine-validated.** No `openspec` CLI was available; the tree is
   hand-authored to the documented layout. A typo in a capability directory name would surface
   first when the CLI runs at archive time.
2. **D8 stays open with a default.** A later move to "cap it" is a `candidate-matching` change
   (a count limit plus one scenario); a move to "require approval" adds a pending-approval state
   and touches `staffing-confirmation` and REQ-001 §6.
3. **The five weights are frozen in the delta.** Adjusting them later is a spec change, not a
   configuration tweak.
4. **Five IO / SA concerns carry into P3:** show the snapshot time next to a snapshot score;
   apply the renormalisation rule identically in ranking and in the per-criterion display; give
   "no double booking" a storage-level guarantee; present the fallback as "another role, one band
   higher, this domain skill" and never as a role change; test the empty-domain guard rail.
5. **The role skill domain is catalog configuration that feeds matching.** Moving a skill to
   another domain changes who the fallback can reach without touching any person record, so the
   catalog surface must show the domain it is about to change.

## Next

P3 — ATDD design and task planning (`design.md`, `tasks.md`). Delta specs are now the agreed
contract and are not to be rewritten without a new G1 record.
