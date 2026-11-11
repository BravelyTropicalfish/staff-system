# Quality checklist report template

Status values per item: **Pass | Fail | Partial | Skipped**.\
Overall: **Pass | Fail | Partial** (required items only).

```markdown
# Quality Checklist Report: <stage-selection>

**Date:** <YYYY-MM-DD> (timezone: <offset or name>)
**Root:** <absolute path>
**Stages:** all | p0 | p1 | p2 | p3 | p4 | p5 | comma-separated stage ids
**Focus:** full-stage | <check ids>

## Summary

### <stage>

| Check id | Required? | Status | Evidence |
|----------|-----------|--------|----------|
| … | yes\|no | Pass\|Fail\|Partial\|Skipped | path or note |

| **Stage Overall** | — | Pass\|Fail\|Partial | required-only aggregation |

### Roll-up

| Scope | Overall | Notes |
|-------|---------|-------|
| selected stages | Pass\|Fail\|Partial | Fail if any selected stage fails; else Partial if any selected stage is partial |

## Gaps

| Check id | Gap | Owner (if known) | Suggested next |
|----------|-----|------------------|----------------|
| … | … | … | `/sdd:…` or human |

## Notes

- Optional / not-run / out-of-focus items marked Skipped do not block Overall Pass.
- Overall aggregates in-focus required items only.
- This report is not a Gate decision.
```

## Aggregation (required items only)

Scope = checklist rows that are **in focus** (`full-stage` ⇒ all rows for
selected stages; otherwise only the listed check ids).

| Situation | Overall |
|-----------|---------|
| Any in-focus required Fail | Fail |
| No Fail; any in-focus required Partial | Partial |
| All in-focus required Pass (Skipped optional / out-of-focus / not-run OK) | Pass |
