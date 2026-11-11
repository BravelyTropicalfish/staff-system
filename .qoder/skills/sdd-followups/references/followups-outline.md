# Follow-ups outline (pack default)

Use during `/sdd:followups`. Produce **one** work package with two labeled
sections. Do not treat this document as Living Spec authority.

## Item fields

| Field | Notes |
|---|---|
| `id` | Stable short id (e.g. `TD-1`, `BZ-1`) |
| `type` | `tech-debt` \| `business` |
| `title` | One line |
| `why` | Why raised now |
| `evidence` | Paths / links to summary, verify, or change artifacts |
| `owner` | If known; else leave empty |
| `priority` | Optional: high / med / low / unset |
| `links` | Back to change-id / requirement ids when possible |

## Classification

| Bucket | Include |
|---|---|
| Tech debt | Code, test, docs, known engineering shortcuts left after the change |
| Business backlog | Product / process asks deferred from this change |
| Redirect → `/sdd:evolve` | Harness, Rules, pipeline/IaC template evolution — **do not** put in the two body sections |

## Document skeleton

```markdown
# Follow-ups — <change-id>

- change-id: …
- change-summary present: yes | no (continued with gaps)
- timestamp: …

## Tech debt

| id | title | why | evidence | owner | priority | links |
|---|---|---|---|---|---|---|
| TD-1 | … | … | … | … | … | … |

## Business backlog

| id | title | why | evidence | owner | priority | links |
|---|---|---|---|---|---|---|
| BZ-1 | … | … | … | … | … | … |

## Gaps

- …

## Redirect — /sdd:evolve

- … (pointers only; no evolution design here)

## Disclaimer

This file is not Living Spec authority. Do not auto-file tracker tickets unless
the user explicitly asks and a project integration exists.
```

## Quality rules

1. Prefer evidence-backed items; if residuals are thin → empty tables + Gaps (Partial).
2. Never invent debt or business asks to “look complete.”
3. Keep tech debt vs business labeled separately in one artifact.
4. Link back to requirement/change ids when possible.
