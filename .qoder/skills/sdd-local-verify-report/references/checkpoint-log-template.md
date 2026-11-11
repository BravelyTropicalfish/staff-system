# Checkpoint log — local-verify-report

- skill: skill.verify.local-verify-report
- command: /sdd:local-verify-report
- root: {{ROOT}}
- started_at: {{STARTED_AT}}
- host_agent: {{HOST_AGENT}}

## Checkpoints

| id | topic | choice | rationale | status |
|----|-------|--------|-----------|--------|
| C1 | scope | | | pending |
| C2 | command-source | | | pending |

## Probe

- summary:
- whitelist_empty: yes | no
- change_artifacts: found | missing | n/a
- commands_file:

## Run

- commands_attempted:
- commands_failed:
- runner_log:

## Report

- mode: persisted
- report_path:
- overall: Pass | Fail | Partial | pending
- waiver: N/A | complete | incomplete

## Next

- suggested (optional): /sdd:pr-submit (only on Pass, or Fail with complete human-signed Waiver)
- on Fail without Waiver: hard-stop — do not call /sdd:pr-submit
