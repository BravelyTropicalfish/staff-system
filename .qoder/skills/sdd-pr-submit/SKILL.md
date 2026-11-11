---
name: sdd-pr-submit
description: >-
  Open or update GitLab merge request(s) for the change after local verify Pass
  or a signed waiver. Do not self-issue waivers, merge, or deploy.
disable-model-invocation: true
status: ready
id: skill.apply.pr-submit
command: /sdd:pr-submit
impl: native
layer: core
gate: false
---

# PR submit

## Instructions

Open or update **GitLab merge request(s)** for the change (supports multi-MR /
submodule coordination). Default remote tooling is **`glab`**. Pack scripts are
**local-only** (no push / auth / network writes). **Anti-trigger:** Do not run if
local verify is Fail without a human-signed waiver; do not merge or deploy from
this skill.

1. Confirm `change-id` / branch and GitLab context (remote, target branch).
   Optional local helpers: [preflight-branch.sh](../../../vendor/ai4se-plugins/skills/apply/pr-submit/scripts/preflight-branch.sh),
   [list-pr-targets.sh](../../../vendor/ai4se-plugins/skills/apply/pr-submit/scripts/list-pr-targets.sh). Do not embed secrets in
   the pack; do not use pack scripts to set credentials.
2. Confirm local verify result: **Pass**, or Fail with a human-signed waiver
   that lists **item**, **owner** (VL or designated role), and **remedy
   deadline**. Missing verify result → stop; run `/sdd:local-verify-report`
   first. Fail without a complete signed waiver, or any agent-self-approved
   waiver → **hard stop**.
3. List the MR target set (repos / submodules). For suggested remote commands,
   run [prompt-remote-mr.sh](../../../vendor/ai4se-plugins/skills/apply/pr-submit/scripts/prompt-remote-mr.sh) (prints hints only).
   Then perform push / `glab` MR create-or-update **outside** the pack scripts;
   note dependency order for multi-MR. If `glab` is unavailable when remote
   steps are required → stop and prompt install/auth (do not configure auth in
   pack scripts).
4. Output all MR URL(s) plus coordination notes. **Next:**
   `/sdd:pipeline-report phase=ci`. Do not merge or trigger deploy.

- **In:** change-id / branch; local verify Pass or Conditional waiver record;
  GitLab remote context
- **Out:** MR URL(s) created or updated; submodule / multi-repo notes if any;
  remote-step hints when scripts only printed prompts
- **Not:** merging the MR; deploy triggers (`/sdd:deploy-artifacts`);
  self-issuing verify waivers; G2 decisions; pack scripts that push or set
  credentials; GitHub `gh` as the default path
- **Escalate:** human owner when auth is missing or waiver signer role is unclear

**Gotchas:** Agents must not approve their own waivers. `local-verify-report`
Fail without a signed waiver is a hard stop. Pack scripts never bypass that gate
and never perform remote write operations.

## Examples

```text
User: /sdd:pr-submit
Agent: Confirms local verify Pass (or reads waiver item/owner/deadline); runs
local preflight + list-pr-targets + prompt-remote-mr; performs push/glab MR
steps only when remote work is required; returns URLs and notes; suggests
/sdd:pipeline-report phase=ci.
```

## Additional resources

- [CI/CD integration (GitLab-first)](../../../vendor/ai4se-plugins/integrations/cicd.md)
- [preflight-branch.sh](../../../vendor/ai4se-plugins/skills/apply/pr-submit/scripts/preflight-branch.sh)
- [list-pr-targets.sh](../../../vendor/ai4se-plugins/skills/apply/pr-submit/scripts/list-pr-targets.sh)
- [prompt-remote-mr.sh](../../../vendor/ai4se-plugins/skills/apply/pr-submit/scripts/prompt-remote-mr.sh)
