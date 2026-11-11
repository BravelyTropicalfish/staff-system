# Consistency lint checklist (pack default)

Use during `/sdd:consistency-lint` for the confirmed `phase=p2|p5`.
Emit findings with fields: id, severity (BLOCKER|WARNING|SUGGESTION), phase,
location, issue, suggested_fix, evidence, **authority**
(`proposal|spec|code|AI-inference`), and mark **`[fact]`** vs **`[inference]`**.
Mark checks **N/A** when the optional artifact is absent — do not fail the whole
run solely for missing optional files.

## Finding severity

| Severity | Use when |
|---|---|
| BLOCKER | Contradiction or broken cross-ref that blocks safe Gate progress;
  hard-rule violation (MUST / SHALL / 必须 / 禁止) |
| WARNING | Likely drift / weak traceability; human should confirm |
| SUGGESTION | Clarity or hygiene; non-blocking |
| N/A | Artifact missing — note reason; exclude from pass/warn/block counts |

## Overall verdict mapping

| Condition | Verdict |
|---|---|
| ≥1 BLOCKER | Fail |
| No BLOCKER, ≥1 WARNING | Partial |
| Only SUGGESTION or no findings | Pass |
| Required spec roots unreadable | stop (do not report Pass) |
| Tooling absent — manual checklist only | verdict capped at Partial |

## Artifact levels (per change)

| Level | Present | Run |
|---|---|---|
| 1 | proposal only | Proposal rules; Spec/Tasks N/A; Design only if `design.md` exists |
| 2 | proposal + specs | Proposal + Spec; Tasks N/A; Design if present |
| 3 | proposal + specs + tasks | All core; Design if present |
| +design | `design.md` exists | Enable Design rule section |

Missing `proposal.md` → SKIP that change. Missing optional artifacts → N/A the
dependent rules (do not auto-fail the whole run).

## Proposal rules (P1–P4)

| # | Rule | Severity | Method |
|---|---|---|---|
| P1 | `## Why` present and non-empty | BLOCKER | Heading + substance |
| P2 | `## What Changes` present and non-empty | BLOCKER | Heading + substance |
| P3 | Non-Goals / Out-of-scope present and non-empty | BLOCKER | Named section with content |
| P4 | Impact covers self / upstream / downstream | BLOCKER | Explicit three dimensions |

## Spec rules (S1–S5)

| # | Rule | Severity | Method |
|---|---|---|---|
| S1 | Spec structure follows project OpenSpec schema | WARNING+ | Frontmatter / core sections |
| S2 | Citation prefixes stay in agreed set (`@knowledge:` `@code:`
  `@external:` `@spec:` `@rule:` …) | WARNING | Unknown prefix → flag |
| S2b | `@knowledge:` paths resolve | WARNING | File exists under KB root or relative |
| S2c | `@rule:<ID>` resolves to rules file | WARNING | Matching `<ID>.md` under rules root |
| S3 | Every Requirement traces to proposal What Changes | BLOCKER | Orphan Requirement → Fail |
| S4 | Scenarios use Given / When / Then | WARNING | Missing element |
| S5 | Scenarios belong to a numbered Requirement | WARNING | Orphan Scenario |

## Tasks rules (T1–T3) — when tasks.md exists

| # | Rule | Severity | Method |
|---|---|---|---|
| T1 | Each task ≤ ~2h when estimates are present | WARNING | Oversize → flag |
| T2 | Last task is verify / validation / regression | WARNING | Missing verify tail |
| T3 | Tasks stay inside Spec Requirements | WARNING | Out-of-spec task |

## Design rules (D1–D3) — when design.md exists

| # | Rule | Severity | Method |
|---|---|---|---|
| D1 | Decisions explicitly numbered (D1, D2, …) | WARNING | Missing numbers |
| D2 | Each decision has Context / Background | WARNING | Bare choice |
| D3 | Each decision notes rejected alternatives | WARNING | List which lack alternatives |

## Shared / cross-artifact checks

1. Cross-references / IDs that point at missing sections or capabilities
2. Same capability or requirement stated incompatibly in two places
3. Acceptance criteria that are vague or untestable (e.g. “works well”)
4. **C1** proposal → spec: What Changes covered by Spec Requirements (level 2+)
5. **C2** spec → tasks: each Requirement has ≥1 task (level 3)
6. **C6** `@knowledge:` / `@rule:` reachability (level 2+)
7. Do not treat this lint report as Living Spec authority

## phase=p2 checks

1. Proposal scope vs Delta Spec scope — no silent expansion or drop
2. Delta capabilities internally consistent (actors, constraints, scenarios)
3. Key scenarios / constraints from proposal have a home in Delta (or explicit
   deferral)
4. If `tasks.md` exists — tasks do not contradict Delta intent (direction check
   only)
5. Requirements / deltas that cannot trace to the proposal → BLOCKER or WARNING
   with location
6. Apply P/S/T/D + C1/C2/C6 tables above for the detected artifact level

## phase=p5 checks

1. Living Spec vs this change’s merge intent — residual contradictions
2. Leftover open Delta / archive side notes that should have been cleared or
   linked
3. Post-merge terminology or cross-ref drift in touched Living areas
4. If `change-summary` (or equivalent) exists — summary claims vs Living/archive
   evidence do not obviously conflict

## Quality judgment (fuzzy language)

- Non-Goals must be precise enough (“不重构 XXX”, not “不做不相关的事”).
- Design decision depth: context + rejected alternative + why chosen.

## Report skeleton

1. Header: change-id, phase, focus, timestamp, artifact level
2. Verdict + counts (BLOCKER / WARNING / SUGGESTION / N/A)
3. Findings table ordered by severity — each row: Authority + `[fact]` /
   `[inference]`
4. N/A / skipped items
5. Fix suggestions by priority (blockers → warnings → suggestions)
6. **Residual risks** section (MUST — may be empty with explicit
   “无已知残余风险”)
7. Reviewer metadata (agent / human sign-off pending / date)
8. Next (non-Gate): p2 → `/sdd:g1-decision-record` or continue `/sdd:p2`;
   p5 → `/sdd:change-summary` / `/sdd:g3-decision-record`

Batch mode (optional, when user asks): one report per change under
`openspec/changes/<id>/consistency-lint.md` plus a summary table — still one
invocation intent; do not silently lint the whole tree without user choice.
