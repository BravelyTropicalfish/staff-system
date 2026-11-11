# Change summary outline (pack default)

Use during `/sdd:change-summary`. Prefer facts from the OpenSpec change folder and
linked PR / verify artifacts. Mark missing inputs as gaps — do not invent metrics.

## Required sections

| Section | Content |
|---|---|
| Intent | Why the change existed (from proposal / approved scope) |
| What shipped | What landed (specs merged, code/PRs, KB update if recorded) |
| Verify highlights | Key verify outcomes with evidence paths/links |
| Residual risks | Open risks, known gaps, follow-up candidates (non-authoritative) |
| Links | Spec/design/tasks, PRs, verify reports, archive/KB pointers as available |

## Suggested shape

```markdown
# Change summary — <change-id>

## Intent
…

## What shipped
…

## Verify highlights
…

## Residual risks
…

## Links
- …
```

## Storage

Prefer the change folder: project Rules path if set, else `change-summary.md`.\
If the path is unclear or the user declines a file write → emit the full summary in the reply.

## Non-goals

- Not a G3 Pass/Fail decision
- Not an authoritative backlog (optional later `/sdd:followups`)
- Summary prose is not Living Spec authority
