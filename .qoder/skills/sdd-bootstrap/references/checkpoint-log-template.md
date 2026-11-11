# Checkpoint log — bootstrap

- skill: skill.harness.bootstrap
- command: /sdd:bootstrap
- root: {{ROOT}}
- started_at: {{STARTED_AT}}
- host_agent: {{HOST_AGENT}}

## Checkpoints

| id | topic | choice | rationale | status |
|----|-------|--------|-----------|--------|
| C1 | root | | | pending |
| C2 | spec-backend | | | pending |
| C3 | agents-md | | | pending |
| C4 | extra-knowledge | | | pending |

## Apply

- mode: dry-run | apply
- plan_summary:
- created:
- skipped:

## Next

- suggested (optional): /sdd:rules-govern
- suggested (optional): /sdd:quality-checklist when ready for P0 DoV
- later (optional, not bootstrap): /sdd:pipeline-iac for CI/CD skeleton after Rules
- user may skip or reorder — do not treat these as mandatory gates of bootstrap
