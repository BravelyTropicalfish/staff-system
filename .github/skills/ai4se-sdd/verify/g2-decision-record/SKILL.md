---
name: g2-decision-record
description: >-
  Record the G2 Verify decision with VL sign-off after local-verify and required
  pipeline evidence. Use only when explicitly invoked. Do not use for G1/G3 or
  auto-approve.
disable-model-invocation: true
status: ready
id: skill.verify.g2-decision-record
command: /sdd:g2-decision-record
impl: native
layer: core
gate: true
---

# G2 decision record

## Instructions

Human explicitly records the G2 Verify decision after local/CI/CD evidence.
**Anti-trigger:** Model must not auto-invoke; do not use for G1/G3; do not
re-run apply or pipeline fetch; do not self-approve as the implementing agent.

1. Confirm explicit `/sdd:g2-decision-record` (or named invoke). Collect
   `change-id`, paths/refs to the **local-verify** report and **required
   pipeline** report(s) (`phase=ci`, and `phase=cd` when the recipe requires),
   and **named VL** sign-off. Missing VL → **stop**.
2. Draft the record using
   [g2-decision-outline.md](../../../../../vendor/ai4se-plugins/skills/verify/g2-decision-record/references/g2-decision-outline.md): change id,
   gate=G2, inputs, dimension results, defect grades, decision
   Pass|Fail|Conditional, waivers/conditions+owners, timestamp. Optional
   Spec↔Code traceability appendix on request — missing appendix does **not**
   block Pass.
3. Validate:
   - Conditional without remediation items/owners → **invalid**, require
     completion.
   - Decision=Pass with missing local-verify or required pipeline evidence →
     **reject**; use Fail or Conditional with owners.
   - Pipeline Overall Partial with explicit `Pass-by-human-confirm` may satisfy
     the pipeline input **only if cited in Inputs**.
4. Persist: project Rules path if set, else `g2-decision.md` under the change
   folder; if the path is unclear or the user declines a file write → emit the
   full record in the reply. Summarize the path in the reply. Cite evidence
   under `docs/ai4se-plugins/` (or equivalent) — do not rewrite full evidence
   bodies here.
5. **Next:** on Pass → merge then P5 archive lane skills; on Fail → return to
   verify/apply to remediate.

- **In:** change-id; local-verify + required pipeline report refs; named VL;
  optional traceability request
- **Out:** G2 decision record (Pass|Fail|Conditional) with required fields;
  optional `g2-decision.md` (or Rules-agreed path); optional traceability
  appendix
- **Not:** re-running apply; fetching CI/CD; forcing traceability; Living Spec
  edits; agent self-approval in place of VL
- **Escalate:** stop when VL missing; reject Pass when evidence incomplete

**Gotchas:** Apply and verify agents must stay separated — this skill is
verify-side only. Conditional without remediation is not Verified. This skill
records the human decision — it does not invent approval.

## Examples

```text
User: /sdd:g2-decision-record
Agent: Confirms VL + local-verify/pipeline inputs; writes g2-decision.md
(or reply); Conditional only with remediation+owners; on Pass suggests merge
then P5 archive lane.
```

```text
User: /sdd:g2-decision-record (Pass, missing pipeline report)
Agent: Rejects Pass; requires Fail or Conditional with owners, or a cited
pipeline report (including Pass-by-human-confirm Partial if applicable).
```

```text
User: /sdd:g2-decision-record (Pass, pipeline Partial with human confirm cited)
Agent: Accepts pipeline input when Inputs cite the human-confirm report;
       still requires local-verify; records G2 under VL sign-off.
```

## Additional resources

- [G2 decision outline](../../../../../vendor/ai4se-plugins/skills/verify/g2-decision-record/references/g2-decision-outline.md)
