# Checkpoint log — pipeline-iac

- skill: skill.harness.pipeline-iac
- command: /sdd:pipeline-iac
- root: {{ROOT}}
- started_at: {{STARTED_AT}}
- host_agent: {{HOST_AGENT}}

## Checkpoints

| id | topic | choice | rationale | status |
|----|-------|--------|-----------|--------|
| C1 | scope | | | pending |
| C2 | org-template | | | pending |
| C3 | rules-gate | | | pending |

## Probe

- summary:
- host: gitlab | github | other | unknown

## Plan

- mode: dry-run | materialize
- plan_path:
- create_count:
- skip_count:
- conflict_count:

## Next

- suggested (optional): /sdd:quality-checklist
- suggested (optional): /sdd:rules-govern when Rules gaps remain
