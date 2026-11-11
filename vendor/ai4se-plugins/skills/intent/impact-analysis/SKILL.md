---
name: impact-analysis
description: >-
  Assess change impact against as-is docs knowledge base and OpenSpec/SDD
  archived assets before splitting proposals or stories. Embedded by default at
  the end of /sdd:pre-spec-req; also runnable standalone. Do not approve Gates or
  produce detailed design here.
disable-model-invocation: true
status: ready
id: skill.intent.impact-analysis
command: /sdd:impact-analysis
impl: native
layer: core
gate: false
---

# Impact analysis

## Instructions

Assess change impact across specs, code, systems, roles, and **existing as-is
knowledge/spec assets** before splitting proposals/stories. **Anti-trigger:**
Not a Gate decision; not detailed design or task planning (`/sdd:task-plan`);
not requirement authoring (`/sdd:pre-spec-req` owns drafting — this skill only
analyzes impact).

**Invocation modes:**

| Mode | When | Notes |
|---|---|---|
| **Embedded** | Final step of `/sdd:pre-spec-req` after blocking OQs and DoR BLOCKERs are cleared | Default P1 path; do not skip inside `pre-spec-req` |
| **Standalone** | User runs `/sdd:impact-analysis` | Re-run after requirement changes, or refresh impact without re-authoring |

1. Confirm structured requirements from `/sdd:pre-spec-req` (same workdir /
   user path as upstream). If the requirement set is missing → **hard stop**.
   When embedded, also confirm the caller already cleared blocking open
   questions and DoR BLOCKERs; if not → return control to `pre-spec-req` (do
   not invent clearance).
2. Optionally refresh repo/spec context via `/sdd:repo-understand` when brownfield
   impact is unclear.
3. **Inventory as-is assets with evidence paths** (required):
   - Knowledge base under `docs/` (or project-documented KB slots)
   - Living Spec tree `openspec/specs/`
   - Post-archive SDD assets (archived changes / archive outputs as present in
     the repo). If an expected tree is missing → record the gap; do not invent
     a competing authority root. If **no** as-is authority tree can be located
     and the user has not confirmed greenfield-with-no-baseline → **stop** or
     record a **blocker**.
4. Produce an impact brief following
   [impact-brief-outline.md](./references/impact-brief-outline.md). For each
   touched asset, write **as-is → proposed delta** relative to the current
   requirements. Prefer **application-architecture** blast radius (apps /
   services / capabilities / integration boundaries) alongside artifact impact
   (specs/tests/docs), dual-track notes, risks, suggested split boundaries.
   Prefer evidence paths over speculative essays.
5. Persist the brief to `<workdir>/impact-brief.md` by default (user may override
   the path parameter). Also summarize in the session. Critical unknown
   dependencies → list as **blockers**; do not hide them. Critical blockers that
   invalidate requirements → caller must return to `/sdd:pre-spec-req` (do not
   suggest split).
6. **Next:** `/sdd:proposal-split` and/or `/sdd:story-split` when the brief has
   no critical blockers. (Standalone re-runs may stop after writing the brief.)

- **In:** structured requirements; as-is trees (`docs/`, `openspec/specs/`,
  archive assets); optional repo/spec context; optional brief path parameter;
  when embedded — confirmation that blocking OQs / DoR BLOCKERs are cleared
- **Out:** impact brief file + session summary including as-is inventory and
  as-is→delta comparisons, blast radius, artifact impact, risks, split
  boundaries, blockers
- **Not:** final scope approval; implementation plan tasks; Gate decisions;
  inventing Living Spec roots when OpenSpec is absent; clearing requirement
  blockers on behalf of `pre-spec-req`
- **Escalate:** human when blockers block both split tracks, or as-is baseline
  cannot be confirmed

**Gotchas:** Do not treat this brief as G1 approval. A requirements-only brief
without as-is comparison is out of contract. Standalone does not replace
`pre-spec-req` authoring or its hard gates.

## Examples

```text
User: /sdd:pre-spec-req  (embedded)
Agent: …after blockers cleared… runs impact-analysis contract; writes
<workdir>/impact-brief.md; suggests proposal-split and/or story-split.
```

```text
User: /sdd:impact-analysis  (standalone)
Agent: Reads requirements; inventories docs/ + openspec/specs/ + archive assets;
writes <workdir>/impact-brief.md with as-is→delta rows, blast radius, risks,
blockers; suggests proposal-split and/or story-split (or return to pre-spec-req
if critical blockers).
```

## Additional resources

- [Impact brief outline](./references/impact-brief-outline.md)
- [OpenSpec integration](../../../integrations/openspec.md)
- [Pre-spec-req skill](../pre-spec-req/SKILL.md)
