---
name: deploy-artifacts
description: >-
  Trigger existing CD after confirmed PR merge. Use post-merge. Do not invent a
  new CD product or aggregate CD reports here.
disable-model-invocation: true
status: ready
id: skill.apply.deploy-artifacts
command: /sdd:deploy-artifacts
impl: native
layer: core
gate: false
---

# Deploy artifacts

## Instructions

After PR merge success, trigger the **existing** CD pipeline / deploy for
artifacts (batch OK). **Anti-trigger:** Do not invent a second CD product; do
not run local-only dry runs unless the user explicitly asks; do not write G2 or
aggregate CD reports.

1. Confirm merge with verifiable evidence (merged PR URL/status, merge commit, or
   equivalent change identity). If merge is not confirmed → **hard stop**.
2. Confirm target environment(s) are allowed by Project Rules. If out of policy
   → stop.
3. Trigger only the existing org CD mechanism documented in
   [cicd integration](../../../../../vendor/ai4se-plugins/integrations/cicd.md). Batch artifact/environment
   requests are allowed. If no CD trigger is configured → stop and document the
   gap per that integration. If plaintext secrets would be required → stop.
4. Prefer least privilege; keep deploy trigger tokens separate from verify
   read tokens.
5. Output trigger confirmation (run id / URL) and the list of
   artifacts/environments requested. **Next:**
   `/sdd:pipeline-report phase=cd`.

- **In:** merged change identity / artifact ids; target environment(s) allowed
  by Rules; CD trigger mechanism from cicd integration
- **Out:** trigger confirmation (run id / URL); artifacts/environments requested
- **Not:** rewriting CD YAML as a new platform; writing G2 decisions;
  aggregating CD test reports (`/sdd:pipeline-report phase=cd`)
- **Escalate:** human owner when merge evidence is ambiguous or CD trigger
  auth/role is unclear

**Gotchas:** Deploy privilege ≠ verify read privilege — keep tokens separate.
Do not invent a second CD product inside this skill.

## Examples

```text
User: /sdd:deploy-artifacts
Agent: Confirms merged PR evidence and Rules-allowed envs; triggers existing CD
per integrations/cicd.md; returns run id/URL and artifact/env list; suggests
/sdd:pipeline-report phase=cd.
```

## Additional resources

- [CI/CD integration](../../../../../vendor/ai4se-plugins/integrations/cicd.md)
