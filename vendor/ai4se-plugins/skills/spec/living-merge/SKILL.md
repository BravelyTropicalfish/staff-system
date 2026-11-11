---
name: living-merge
description: >-
  Merge accepted deltas into Living Spec via OpenSpec archive and run the
  project knowledge-base update script. Use in P5 after humans confirm merge
  readiness. Do not write the G3 decision here.
disable-model-invocation: true
status: ready
id: skill.spec.living-merge
command: /sdd:living-merge
impl: wrap-openspec
layer: core
gate: false
---

# Living merge

## Instructions

Merge accepted change deltas into Living Spec via OpenSpec archive, then run the
project-configured knowledge-base (KB) update script.
**Anti-trigger:** Do not use before humans accept merge readiness; not a G3
decision; do not invent a non-OpenSpec living-spec root.

1. Confirm `change-id` and **explicit human confirmation** that merge readiness
   / archive preconditions are met (reply yes or an explicit confirm flag). No
   confirmation → **stop**.
2. Resolve the KB update command or script path from project Rules / Harness
   (do **not** hardcode a path inside this plugin pack). If none is configured →
   **stop**; ask the human to configure it before continuing.
3. Wrap `/opsx:archive` (or the documented OpenSpec archive equivalent) so Living
   Spec updates under the authoritative OpenSpec tree. If OpenSpec archive is
   unavailable or fails → **stop**; see
   [openspec integration](../../../integrations/openspec.md). Do not fix archive
   failure by copying specs into this plugin pack.
4. Run the resolved KB update script. If it fails → **stop the whole
   `/sdd:living-merge` run** (even if archive already succeeded); escalate to a
   human to restore consistency. Merge conflicts / parallel change collisions →
   **stop**; escalate to SA.
5. On full success, reply with a short overlay only (no required file write):
   what merged, archived location pointers, KB command + result, residual risks.
6. **Next:** `/sdd:change-summary`, then `/sdd:g3-decision-record`.

- **In:** change-id; explicit human merge-ready confirmation; Rules/Harness KB
  update script convention
- **Out:** Living Spec updated via `/opsx:archive`; KB update script succeeded;
  reply overlay with archive + KB outcomes
- **Not:** G3 decision record; business code; hardcoding KB paths in this pack;
  a second living-spec authority root
- **Escalate:** human when confirmation, KB config, OpenSpec, or KB script
  blocks; SA on merge/parallel conflicts

**Gotchas:** Archive success alone is not enough — missing or failing KB update
fails the skill. Overlay prose is not Living Spec authority.

## Examples

```text
User: /sdd:living-merge (confirms merge-ready)
Agent: Resolves KB script from Rules; wraps /opsx:archive; runs KB update; on
both OK, summarizes paths + residual risks; suggests /sdd:change-summary then
/sdd:g3-decision-record.
```

```text
User: /sdd:living-merge (no KB script in Rules)
Agent: Stops; asks human to configure the project KB update script before retry.
```

## Additional resources

- [OpenSpec integration](../../../integrations/openspec.md)
