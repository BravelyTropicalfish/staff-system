# G1 decision — add-staffing-mvp

> This file holds three records: the **rev 8** decision from 2026-11-10 (below, unchanged), the
> **rev 11** decision from 2026-11-11, and the **rev 12** decision from 2026-11-11 (both at the
> end). The header block and every section down to rev 8's `## Next` belong to rev 8 only.

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

---

## G1 record — rev 11 (2026-11-11)

- **Gate:** G1 (Align)
- **Change id:** `add-staffing-mvp`
- **Revision under decision:** REQ-001 **rev 11**, including the review fixes applied inside rev 11
  (no new revision number)
- **Decision:** **Pass**
- **Approvers:** IO = raylen; SA = kyrie
- **Timestamp:** 2026-11-11

The decision belongs to the two named approvers; the agent recorded it on their instruction and
issues no approval of its own. The rev 8 record above is left unchanged and still covers rev 8
only.

### Input artifacts

| Artifact | Path | Result |
|---|---|---|
| Proposal (English / Chinese) | `proposal.md`, `proposal.zh.md` | 8 What-Changes steps, 6 new capabilities, D1–D8 carried forward |
| Delta spec — `people-pool` | `specs/staffing/people-pool/spec.md` + `spec.zh.md` | 8 requirements / 17 scenarios |
| Delta spec — `people-import` | `specs/staffing/people-import/spec.md` + `spec.zh.md` | 5 requirements / 15 scenarios |
| Delta spec — `allocation` | `specs/staffing/allocation/spec.md` + `spec.zh.md` | 5 requirements / 11 scenarios |
| Delta spec — `post-requirement` | `specs/staffing/post-requirement/spec.md` + `spec.zh.md` | 8 requirements / 14 scenarios |
| Delta spec — `candidate-matching` | `specs/staffing/candidate-matching/spec.md` + `spec.zh.md` | 5 requirements / 15 scenarios |
| Delta spec — `staffing-confirmation` | `specs/staffing/staffing-confirmation/spec.md` + `spec.zh.md` | 9 requirements / 18 scenarios |
| AC / DoV review | `dov-ac-review.md` | AC-1–AC-4 and DoV-1–DoV-5 all Pass after the rev 11 rewrite; verdict "fit to go to a new G1" |
| Consistency lint (p2) | `consistency-lint.md` | **Partial** — 0 BLOCKER, 0 WARNING, 1 SUGGESTION open (L3, the capability name); capped by the absent `openspec` CLI |
| Source requirement | `requirement-workspace/requirements/REQ-001-staffing-mvp.md` (rev 11) | S1–S40 covered by the union of the trace lists |
| Review aid | `prototype/` | Unchanged; confirming only raises a toast, so it holds no voiding logic to reconcile |

Totals counted immediately before this decision: **40 requirements / 90 scenarios** in each
language, 40/40 `**Traces:**` lines, 90/90 scenarios carrying GIVEN, WHEN and THEN, 90 distinct
scenario titles, and 11/11 quoted key rules present in `proposal.md` under a case-sensitive match.

### Findings

- **Scope.** The six delta capabilities still map one to one onto the six "New capabilities" rows;
  no `MODIFIED` or `REMOVED` section, because `openspec/specs/` is empty. Since rev 9 the scope
  excludes the fit score, the weights and the score-ordered ranking; since rev 10 it includes the
  terminal project end; rev 11 returns the dates to the exclusion rule, so an allocation is
  person × project × an inclusive range and only a range intersecting `[post.start, post.end]`
  blocks a person.
- **Ambiguity.** REQ-001 §11 B1–B18 stay resolved. D1–D6 remain deferred inside REQ-001 and
  D7–D8 in `proposal.md`; D8 now reads "active status and date overlap", so the cross-role cap is
  still open with its MVP default (no cap, no extra approval). No unresolved ambiguity changes a
  rule in the delta.
- **Conflict.** The seventh pass found three contradictions that appear only where the rev 11
  dates meet actions written for rev 10, and all three were fixed before this gate. Removing a
  confirmation-written allocation would have left its assignment standing and let G6 admit the
  person again on the same day, so such an allocation is now removable only by a release or by the
  project end **(L20)**. Voiding every pending recommendation regardless of dates would have
  cancelled a proposal on a later, disjoint post, so `person taken` is now scoped to intersecting
  post dates **(L21)**. Requiring an active catalog role on every save would have frozen a post
  whose role was deactivated afterwards, so the check now runs on creation and on a role change
  only **(L22)**. Five warnings and four suggestions were resolved in the same pass **(L23–L31)**.
- **Traceability.** Each capability names its proposal step and its REQ-001 section, and the union
  of the trace lists covers S1–S40.

### Conditions + owners

None. The decision is Pass, so no remediation is owed at this gate.

### Open risks accepted with this decision

1. **Lint is Partial, not machine-validated.** No `openspec` CLI was available, so the tree is
   hand-authored to the documented layout and a capability-directory typo would surface first when
   the CLI runs at archive time.
2. **L3 stays open.** The capability name is a suggestion, not a rule; renaming it later touches
   every spec path and the proposal.
3. **D8 stays open with its default.** A later "cap it" is a `candidate-matching` change; a later
   "require approval" adds a pending-approval state and touches `staffing-confirmation` and
   REQ-001 §6.
4. **Hand-entered overlaps are stored, not refused.** Two hand-entered ranges for the same person
   may overlap by design, so the only double-booking guard is G6 against a post's own dates; the
   coordinator sees the overlap on the profile and corrects it if it was a mistake.
5. **The project end is coarse and final.** It cancels every post of the project, filled ones
   included, and cannot be undone; a partial close or a reopen path would be a later change.
6. **The prototype is a review aid only.** It was not updated for the rev 11 rules — its confirm
   action only raises a toast — so it is not evidence of behaviour.
7. **Standing concerns for P3, re-scoped.** The snapshot-time display, the renormalisation rule and
   the five frozen weights are retired with the score in rev 9. What carries: a storage-level
   guarantee for the date intersection under concurrent confirmations; presenting the cross-role
   fallback as "another role, one band higher, this domain skill" and never as a role change; the
   empty-domain guard rail, since a role with no skill domain would let the fallback match anyone;
   and showing the domain on the catalog surface before a skill is moved to another one.

### Next

P3 — ATDD design and task planning (`design.md`, `tasks.md`). The rev 11 delta specs are the
agreed contract and are not to be rewritten without a new G1 record.

---

## G1 record — rev 12 (2026-11-11)

- **Gate:** G1 (Align)
- **Change id:** `add-staffing-mvp`
- **Revision under decision:** REQ-001 **rev 12** — project creation moves from "the coordinator's
  alone" to "a lead opens a project of their own, the coordinator opens one for any lead"
- **Decision:** **Pass**
- **Approvers:** IO = raylen; SA = kyrie
- **Timestamp:** 2026-11-11

The decision belongs to the two named approvers; the agent recorded it on their instruction and
issues no approval of its own. The rev 8 and rev 11 records above are left unchanged and still
cover their own revisions only.

### Why the delta is rewritten again

`spec-gates` rule 5: an agreed delta spec is not rewritten without a new record saying why, and
the rev 11 record closes with the same sentence. The product owner walked the running application
and reported that a project manager cannot open a project, so the chain the MVP is bought for —
the lead states the posts a project is short of, the coordinator recommends from the pool, the
lead decides — has no place to start. The rev 11 rule is exactly what the code enforced, so this
is a requirement change and not a defect fix.

### Input artifacts

| Artifact | Path | Result |
|---|---|---|
| Proposal (English / Chinese) | `proposal.md`, `proposal.zh.md` | What-Changes step 3 and assumption 10 now read "opened by its own lead, or by the coordinator on that lead's behalf"; D1–D8 unchanged |
| Delta spec — `allocation` | `specs/staffing/allocation/spec.md` + `spec.zh.md` | 5 requirements / 11 scenarios, unchanged counts: the rev 11 scenario `A lead cannot create a project` became `A lead opens a project of their own`, one row for one row |
| Delta spec — the other five | `specs/staffing/*/spec.md` + `spec.zh.md` | untouched (35 requirements / 79 scenarios) |
| Design | `design.md` §4 + `design.zh.md` | `POST /api/projects` documented as answering both roles, with the 403 a lead gets for naming another lead |
| ATDD design / tasks | `atdd-design.md`, `tasks.md` + `tasks.zh.md` | T33 (the rule and the server guard) and T34 (the two screens) recorded in the apply-progress table |
| Scenario traceability | `traceability.md` | allocation row 16 renamed to `ProjectApiTest#aLeadCreatesAProjectOfTheirOwnOnly` with the new J3 journey as its companion |
| Source requirement | `requirement-workspace/requirements/REQ-001-staffing-mvp.md` (rev 12) | §2 actor table, §3 bullets and §8 project-list row updated; scenario **S41** added for the same rule |
| Review aid | `prototype/` | unchanged; it draws no `New project` action at all, which is why the screens follow its shell and cards rather than its action set |

Totals recounted immediately before this record: **40 requirements / 90 scenarios** in each
language, 40/40 `**Traces:**` lines, 90/90 scenarios carrying GIVEN, WHEN and THEN, and 90
distinct scenario titles — the same totals rev 11 was signed against, because the change replaced
one scenario instead of adding one.

### Findings

- **Scope.** One capability changed, in one requirement and one scenario; no new capability and no
  `MODIFIED` section, because `openspec/specs/` is still empty. The six capabilities still map one
  to one onto the six "New capabilities" rows.
- **Ambiguity.** REQ-001 §11 B1–B19 stay resolved and D1–D8 stay as they were. The new rule needs
  no new open item: "a lead may open a project whose lead is themselves" leaves exactly one
  question — who may name somebody else — and the requirement answers it (the coordinator only).
- **Conflict.** The rev 11 requirement said the coordinator creates and a lead does not; REQ-001 §2,
  §3, `proposal.md` step 3 and assumption 10 said the same. All five now say the lead opens their
  own and the coordinator opens for any lead, so no artifact contradicts another. Ending a project
  is untouched: still the owning lead's alone and still terminal.
- **Traceability.** S41 was added to REQ-001 and mapped to the renamed scenario; the union of the
  trace lists now covers S1–S41.

### Conditions + owners

None. The decision is Pass, so no remediation is owed at this gate.

### Open risks accepted with this decision

1. **The project list now opens for both roles with an empty database.** A lead who opens `/projects`
   for the first time sees `No project has been created yet` and the create action; the rev 11
   wording would have hidden the action entirely.
2. **Editing a project stays unimplemented.** The requirement has said "the coordinator … edit a
   project" since rev 8 and no route or screen offers it (`PATCH /api/projects/{id}` answers 405).
   This record does not close that gap; it is carried as a follow-up rather than implied to work.
3. **The prototype still has no create-project action.** The reviewer's mock has a "new post" button
   but no "new project" one, so the screen is the app's own composition of the prototype's shell,
   cards and form classes rather than a traced drawing.
4. **A lead can hold several open projects at once.** Nothing caps the number a lead may open, by
   the same reasoning that caps nothing else a lead creates.

### Next

P4 continues with T33 and T34 (already applied in the same change), then `/sdd:local-verify-report`
and the G2 addendum recorded in `g2-decision.md`.
