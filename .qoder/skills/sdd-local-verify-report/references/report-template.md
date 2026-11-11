# Local verify report template

Fill every section. Status values: **Pass | Fail | Partial | Skipped** (Overall:
Pass | Fail | Partial only).

```markdown
# Local Verify Report: <change-id|working-tree>

**Date:** <YYYY-MM-DD> (timezone: <offset or name>)
**Root:** <absolute path>
**Scope:** local-only | local+evidence
**Change id:** <id|n/a>

## Summary

| Dimension | Status |
|-----------|--------|
| Build | Pass\|Fail\|Partial\|Skipped |
| Static scan | Pass\|Fail\|Partial\|Skipped |
| Code review | Pass\|Fail\|Partial |
| Evidence (OpenSpec) | Pass\|Fail\|Partial\|Skipped |
| **Overall** | Pass\|Fail\|Partial |

**PR gate:** Fail blocks `/sdd:pr-submit` unless Waiver below is complete.
Agents must not self-approve Waiver.

## Local commands

| Command | Source | Exit | Summary |
|---------|--------|------|---------|
| … | docs/feedback/verification-commands.md \| Rules \| README | 0\|n | … |

## Code-review highlights

- …

## Evidence checklist

| ID | Check | Result | Mark | Notes |
|----|-------|--------|------|-------|
| V1 | … | Pass\|Fail\|Partial\|Skipped | [fact]\|[inference]\|[uncertain] | … |

(If scope=local-only or no change artifacts: state **Evidence: Skipped** and
leave the table empty or mark all Skipped with reason.)

## Evidence slots (E1 / E2 / E3)

| Slot | Status | Source |
|------|--------|--------|
| E1 Machine | Pass\|Fail\|Partial\|Missing | ci-report / local commands |
| E2 AI | Pass\|Fail\|Partial\|Missing | this report (+ eval if any) |
| E3 Human | Pending\|Signed | review assist / Waiver — never agent-signed |

## Five-question map (when evidence in scope)

| Q | Maps to | Status |
|---|---------|--------|
| Q1 Complete? | V1–V3 | ✅\|⚠️\|❌\|n/a |
| Q2 Correct? | V4–V5 | … |
| Q3 Coherent? | V6–V7 | … |
| Q4 Necessary only? | V8 | … |
| Q5 Risk controllable? | residual + red lines | … |

## Findings

### CRITICAL

| # | Finding | Mark | Location | Suggestion |
|---|---------|------|----------|------------|
| 1 | … | [fact]\|[inference] | … | … |

### WARNING

| # | Finding | Mark | Location | Suggestion |
|---|---------|------|----------|------------|
| 1 | … | … | … | … |

### SUGGESTION

| # | Finding | Mark | Location | Suggestion |
|---|---------|------|----------|------------|
| 1 | … | … | … | … |

## Residual risks

(MUST — may be empty with explicit “无已知残余风险”.)

| # | Risk | Why open | Mitigation |
|---|------|----------|------------|
| 1 | … | … | accepted\|tracking\|mitigated |

## Waiver

(Only when Overall=Fail and human intends to continue to pr-submit. Otherwise:
`N/A — no waiver`.)

| Item | Owner (VL\|designated) | Remedy deadline | Human sign-off |
|------|------------------------|-----------------|----------------|
| … | … | YYYY-MM-DD | <name/role + explicit confirm> |

**Agent must not self-approve this section.** Incomplete rows ≡ no waiver.
```

## Aggregation rules

| Situation | Overall |
|-----------|---------|
| Any blocking local command Fail or CRITICAL evidence gap | Fail |
| Whitelist empty or required tools missing | Fail or Partial (never Pass) |
| Evidence in scope but artifacts incomplete | Evidence Partial/Fail; Overall not Pass |
| All required dimensions Pass; optional scans Skipped with reason | Pass |
| Mixed non-blocking gaps | Partial |

## Waiver completeness (all required)

- Item (what is waived)
- Owner (VL or designated role)
- Remedy deadline (`YYYY-MM-DD`)
- Human sign-off (named person/role + explicit confirm)
