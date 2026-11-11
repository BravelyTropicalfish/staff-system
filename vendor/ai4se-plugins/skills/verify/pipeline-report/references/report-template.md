# Pipeline report template

Fill every section. Phase/Overall status: **Pass | Fail | Partial**.

```markdown
# Pipeline Report: <change-id|ref>

**Date:** <YYYY-MM-DD> (timezone: <offset or name>)
**Root:** <absolute path>
**Phase:** ci | cd | all
**Data source:** glab | API | human-confirm | mixed | none
**Change id:** <id|n/a>
**Target:** <branch|SHA|MR|pipeline id|…>
**Pipeline URL:** <url|n/a>
**Commit:** <sha|n/a>

## Summary

| Phase | Status | Conclusion | Upstream URL | Notes |
|-------|--------|------------|--------------|-------|
| ci | Pass\|Fail\|Partial\|n/a | … | … | … |
| cd | Pass\|Fail\|Partial\|n/a | … | … | … |
| **Overall** | Pass\|Fail\|Partial | … | — | … |

(Omit or mark `n/a` for phases not in scope.)

## E1 判定 (Machine Evidence)

| Condition | E1 status |
|-----------|-----------|
| All in-scope jobs success | ✅ E1 全绿 |
| Any failed job | ❌ E1 有红灯 (list) |
| Skipped / manual not run | ⚠️ E1 不完整 (list) |
| No remote data | ❓ E1 未验证 (reason) — or human-confirm Partial |

**E1:** ✅ 全绿 | ❌ 有红灯 | ⚠️ 不完整 | ❓ 未验证

When writing into an OpenSpec change folder for local-verify consumption, also
persist or copy this body as `openspec/changes/<change-id>/ci-report.md` if the
project uses that E1 slot (in addition to `docs/ai4se-plugins/<ts>-pipeline-report.md`).

## Jobs / checks

| Phase | Job/Stage | Status | Duration | URL | Failure reason |
|-------|-----------|--------|----------|-----|----------------|
| … | … | success\|failed\|skipped\|pending\|… | … | … | … |

Map common stages: build / test / lint → E1 base; secret-scan / sast → security;
audit → residual-risk input.

## 失败详情 (when any failed)

### <job-name>（❌ failed）

**失败原因:** [fact] …
**错误摘要:**
- `path:line` — …
**修复建议:** [inference] …

## 安全与合规

| Check | Status | Notes |
|-------|--------|-------|
| Secret scan | ✅\|❌\|⏭️\|n/a | … |
| SAST | ✅\|❌\|⏭️\|n/a | … |
| Dependency audit | ✅\|❌\|⏭️\|n/a | … |

## Residual risks (for local-verify / G2)

| # | Risk | Source | Status |
|---|------|--------|--------|
| 1 | e.g. sast skipped because lint failed | CI dependency chain | open\|accepted\|fixed |

(MUST — empty OK with “无已知残余风险”.)

## Artifacts (if any)

- …

## Human confirmation

(Only when remote fetch was impossible and a named human accepted the phase.
Otherwise: `N/A`.)

| Field | Value |
|-------|-------|
| Confirmer (named role/person) | … |
| Phase covered | ci\|cd\|all |
| Explicit statement | e.g. I confirm this phase as accepted without remote fetch |
| Timestamp | … |

**Overall when human-confirm only:** Partial (`Pass-by-human-confirm`).
**Agent must not self-confirm this section.**

## Next

- ✅ E1 全绿 → `/sdd:local-verify-report` (E1 ready) and/or `/sdd:g2-decision-record`
- ❌ 有红灯 → fix + re-run CI; re-run this skill
- ⚠️ 不完整 → note skipped jobs in local-verify 「未覆盖范围」
- `phase=ci` after merge often → `/sdd:deploy-artifacts` (apply lane)
```

## Aggregation rules

| Situation | Result |
|-----------|--------|
| Remote jobs all success for in-scope phase(s) | Pass (+ E1 全绿) |
| Any failed job | Fail for that phase; Overall Fail if in scope (+ E1 有红灯) |
| Run not found / incomplete | Partial or Fail; record search criteria (+ E1 不完整/未验证) |
| Human confirmation only | Overall **Partial** + `Pass-by-human-confirm`; never fake API Pass |
| `phase=all` with one side missing data and no confirm | Overall not Pass |

## Human confirmation completeness

All required: confirmer, phase covered, explicit statement, timestamp.
