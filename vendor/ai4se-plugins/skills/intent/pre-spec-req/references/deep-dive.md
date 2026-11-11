# Detail deep-dive (optional Phase 2.5)

Active Dev-hat probing **after** impact brief exists and solution direction is
**add / extend / enable-by-config**. Skip when impact says already-covered,
continue-explore, or requirement must be re-split.

Consume impact outputs (affected capabilities, gaps, existing coverage) — do
**not** re-scan all source materials from scratch.

## Four fixed categories

Ask one question at a time (prefer multiple choice). Mark unknowns `{TBD}`;
never fabricate business data. Persist answers under workdir
`deep-dive.md` (or project-equivalent) as four tables + a short probe log.

| # | Category | Pain addressed | Required row shape |
|---|---|---|---|
| ① | **Data contract** | Unclear grain / fields / lifecycle | concept / grain / field list / state transitions / source |
| ② | **Boundary attribution** | In/out scope per rule branch | rule / branch / in\|out / rationale — **must** enumerate input-integrity branches (missing required fields / extract fail / bad format) |
| ③ | **Cross-REQ consistency** | Same capability touched by ≥2 entries with divergent fields | capability / per-REQ grain+fields / aligned? / anchor REQ if not |
| ④ | **Notification / display contract** | Who is notified; what UI surfaces show | trigger or surface / recipient role / content + display fields / channel / cardinality behavior / source |

**Data concept** = business entity with its own fields/lifecycle that Dev could
model from the row (not a tech component name alone).

## open-questions handoff

Blocking items resolved in deep-dive → move to open-questions **Resolved**
(date + pointer to deep-dive row). Do not leave the same blocker open in both
places. Unresolved blockers stay in open-questions and keep DoR item ⑧ / ⑨–⑫
failing.

## Active vs passive

| Artifact | Mode |
|---|---|
| open-questions | Passive accumulation during analysis |
| deep-dive | Active probing — do not mix the two |

## Exit

When deep-dive is in scope for a new entry, DoR items ⑨–⑫ must pass before
impact/split handoff (see [dor-checklist.md](./dor-checklist.md)). Legacy
entries without deep-dive may proceed under project cutover rules — document
the skip explicitly.
