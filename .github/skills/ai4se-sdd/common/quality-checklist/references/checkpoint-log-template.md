# Checkpoint log — quality-checklist

- skill: skill.common.quality-checklist
- command: /sdd:quality-checklist
- root: {{ROOT}}
- started_at: {{STARTED_AT}}
- host_agent: {{HOST_AGENT}}

## Checkpoints

| id | topic | choice | rationale | status |
|----|-------|--------|-----------|--------|
| C1 | stage-selection | all | default when user leaves stage unselected | pending |
| C2 | focus | | | pending |

## Probe

- stages:
- summary:
- notable_missing:

## Report

- mode: persisted
- report_path:
- overall: Pass | Fail | Partial | pending
- required_failed:
- skipped_optional:

## Next

- suggested remediations (optional `/sdd`):
- do not invoke Gate skills from this checklist
