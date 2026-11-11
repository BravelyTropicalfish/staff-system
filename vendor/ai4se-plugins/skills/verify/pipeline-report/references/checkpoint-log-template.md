# Checkpoint log — pipeline-report

- skill: skill.verify.pipeline-report
- command: /sdd:pipeline-report
- root: {{ROOT}}
- started_at: {{STARTED_AT}}
- host_agent: {{HOST_AGENT}}

## Checkpoints

| id | topic | choice | rationale | status |
|----|-------|--------|-----------|--------|
| C1 | phase | | | pending |
| C2 | target | | | pending |

## Probe

- summary:
- host: gitlab | github | other | unknown
- tool_glab: yes | no
- tool_gh: yes | no
- remotes:

## Fetch

- data_source: glab | API | human-confirm | none
- phases_covered:
- upstream_urls:

## Report

- mode: persisted
- report_path:
- overall: Pass | Fail | Partial | pending
- human_confirmation: N/A | complete | incomplete | refused

## Next

- suggested (optional): /sdd:deploy-artifacts (after merge; apply lane)
- suggested (optional): /sdd:pipeline-report phase=cd
- suggested (optional): /sdd:g2-decision-record when required phases covered
