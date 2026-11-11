---
name: change-summary
description: >-
  Produce an execution summary for a change during P5 archive. Use after
  living-merge context. Do not write G3 decisions or invent metrics.
disable-model-invocation: true
status: ready
id: skill.archive.change-summary
command: /sdd:change-summary
impl: wrap-openspec
layer: core
gate: false
---

# Change summary

## Instructions

Produce a short execution summary for a change during P5 archive (overlay on
OpenSpec archive context).
**Anti-trigger:** Not a Gate decision; not a backlog generator (`followups` is
separate / later wave).

1. Confirm `change-id`. Collect key artifact paths as available (spec, design,
   tasks, PRs, verify reports, living-merge / archive / KB outcomes).
2. Prefer facts from the OpenSpec change folder and linked PR/verify evidence
   (pack overlay on `/opsx:archive` context per
   [openspec integration](../../../../../vendor/ai4se-plugins/integrations/openspec.md)). If artifacts are
   insufficient to summarize honestly → state gaps; do **not** invent metrics.
3. Draft the summary using
   [change-summary-outline.md](../../../../../vendor/ai4se-plugins/skills/archive/change-summary/references/change-summary-outline.md): intent,
   what shipped, verify highlights, residual risks, links.
4. Persist: project Rules path if set, else `change-summary.md` under the change
   folder; if the path is unclear or the user declines a file write → emit the
   full summary in the reply.
5. **Next:** `/sdd:g3-decision-record` (mention optional `/sdd:evolve` /
   `/sdd:followups` only as skip-while-stub when those skills are not ready).

- **In:** change-id; key artifact paths as available
- **Out:** short change execution summary; optional `change-summary.md` (or
  Rules-agreed path)
- **Not:** G3 Pass/Fail; authoritative tech-debt backlog; treating summary prose
  as Living Spec authority
- **Escalate:** human when critical artifacts are missing and a honest summary
  cannot be formed

**Gotchas:** Keep tone pack-local; do not treat the summary as Living Spec
authority.

## Examples

```text
User: /sdd:change-summary
Agent: Pulls OpenSpec change + PR/verify links; writes change-summary.md (or
reply) with intent/shipped/verify/risks/links; notes gaps if any; suggests
/sdd:g3-decision-record.
```

## Additional resources

- [Change summary outline](../../../../../vendor/ai4se-plugins/skills/archive/change-summary/references/change-summary-outline.md)
- [OpenSpec integration](../../../../../vendor/ai4se-plugins/integrations/openspec.md)
