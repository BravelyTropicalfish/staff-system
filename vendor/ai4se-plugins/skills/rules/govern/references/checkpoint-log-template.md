# Checkpoint log — rules-govern

- skill: skill.rules.govern
- command: /sdd:rules-govern
- root: {{ROOT}}
- started_at: {{STARTED_AT}}
- host_agent: {{HOST_AGENT}}

## Checkpoints

| id | topic | choice | rationale | status |
|----|-------|--------|-----------|--------|
| C1 | root | | | pending |
| C2 | mode | | | pending |
| C3 | langs | | | pending |
| C4 | host | | | pending |
| C5 | submodule-mapping | | | pending |

## Apply

- mode: dry-run | apply
- plan_summary:
- created:
- skipped:
- conflicts:

## Host projection

- host:
- dest:
- refreshed: yes | no | skipped-none

## Next

- suggested (optional): /sdd:quality-checklist
- user may skip or reorder
