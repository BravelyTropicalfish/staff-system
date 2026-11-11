# Human review assist (optional overlay)

When C1=`local+evidence`, after the verify report body is drafted, optionally
append or write a short **Human review assist** section (same report file, or
`<change>/human-review-assist.md` when the project prefers a sibling file).
Purpose: help QA respond — **not** to auto-approve.

## Contents

1. **Risk band check** — author self-assess L/M/H vs findings; any CRITICAL →
   recommend raise. Five-dimension floor (≥M if any of: money / safety /
   permission / irreversible / detectability gap).
2. **E1 / E2 / E3 table** — status from
   [evidence-checklist.md](../../../../../../vendor/ai4se-plugins/skills/verify/local-verify-report/references/evidence-checklist.md).
3. **Q1–Q5 map** — from the same checklist.
4. **Findings response guide** — every WARNING+ row: suggested
   `accept | reject | need-more-info` + reason. Mark `[fact]` / `[inference]` /
   `[uncertain]`. **Do not** suggest accept on every row (suspected AI-only
   release).
5. **Red lines (project may rename)** — bypass of authz; secret/PII leak; H-band
   without owner; “AI says pass” as sole basis; irreversible change without
   rollback. Trigger → recommend block.
6. **Release suggestion** — `放行` / `带例外放行` / `阻断` + named release
   owner when known. Agent never self-signs.
7. **Residual risks** — MUST section; empty → explicit “无已知残余风险”.
8. **Feedback hook** — covered scope / skipped checks / “on escape, re-check
   residual risks + uncovered range”.

## Guardrails

- Assist only — E3 remains human.
- Incomplete evidence → cannot recommend unconditional 放行.
- Prefer linking to `/sdd:pipeline-report` when E1 is empty.
