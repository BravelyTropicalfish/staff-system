# G2 decision record outline (pack default)

Use when writing `/sdd:g2-decision-record`. Fill every required field.\
Decision must be one of **Pass | Fail | Conditional**.

## Required fields (common Gate set + G2)

| Field | Content |
|---|---|
| Change id | OpenSpec / project change identifier |
| Gate | `G2` (Verify) |
| Input artifacts | Local-verify report path/ref; pipeline report path(s) for required `phase=ci` and `phase=cd` |
| Dimension results | build / scan / review / evidence / ci / cd (as applicable) with Pass\|Fail\|Partial\|Skipped |
| Defect grades | Summary of CRITICAL / WARNING / SUGGESTION (or project-equivalent) |
| Decision | Pass \| Fail \| Conditional |
| Waivers / conditions + owners | Required when Conditional; include deadlines when the project requires them. Restate or link any local-verify Waiver still in force |
| Approvers | Named **VL** (required) |
| Timestamp | When the decision was recorded |
| Traceability appendix | Optional Spec↔Code matrix — not required for Pass |

## Conditional / Pass rules

- Conditional **must** list remediation items with owners (and deadlines if the project requires them).
- Conditional without remediation is **invalid** — not Verified.
- Decision=Pass with missing local-verify or required pipeline evidence → **reject**.
- Pipeline Overall Partial marked `Pass-by-human-confirm` may count as pipeline evidence **only when cited in Inputs**.
- Incomplete evidence must not be papered over by agent self-approval.

## Suggested record shape

```markdown
# G2 decision — <change-id>

- Gate: G2
- Inputs:
  - local-verify: <path or ref>
  - pipeline ci: <path or ref | n/a>
  - pipeline cd: <path or ref | n/a>
- Dimension results: …
- Defect grades: …
- Decision: Pass | Fail | Conditional
- Waivers / conditions + owners: …
- Approvers: VL=…
- Timestamp: …

## Traceability appendix (optional)

| Requirement | Spec | Code | Test |
|-------------|------|------|------|
| … | … | … | … |
```

## Storage

Prefer the change folder: project Rules path if set, else `g2-decision.md`.\
If the path is unclear or the user declines a file write → emit the full record in the reply.

Evidence reports usually live under `docs/ai4se-plugins/`; cite them — do not duplicate full report bodies in this record.
