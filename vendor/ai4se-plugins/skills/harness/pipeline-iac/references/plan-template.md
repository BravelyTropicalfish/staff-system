# Pipeline / IaC plan template

Agent fills this body, then persists via `write-plan.sh`.

# Pipeline IaC plan

- root: <path>
- scope: ci | iac | both
- rules_gate: pass | gap-listed | stop-incomplete
- timestamp: <ISO or pack ts>
- org_template: <path|none>

## Probe summary

(Paste FOUND/MISSING/NOTE lines or a short table.)

## CREATE

| path | rationale | wrap_from |
|---|---|---|
| | | |

## SKIP

| path | rationale |
|---|---|
| | |

## CONFLICT

| path | rationale | action |
|---|---|---|
| | | do-not-overwrite |

## Rules gaps

| block | note | next |
|---|---|---|
| | | /sdd:rules-govern |

## Disclaimer

Dry-run until the user explicitly confirms materialize. Secrets must not be
committed. This plan does not trigger deploys or replace enterprise CD.
