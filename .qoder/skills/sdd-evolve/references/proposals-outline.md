# Evolve proposals outline (pack default)

Use during `/sdd:evolve` after the soft change-summary prerequisite. Emit one
proposals document with three buckets. Never invent items; thin evidence →
empty/Partial sections + Gaps + low confidence.

## Item fields

| Field | Notes |
|---|---|
| `id` | Stable short id (e.g. EV-H-1 / EV-R-1 / EV-N-1) |
| `bucket` | `harness` \| `rules-adr` \| `next-steps` |
| `priority` | `must-fix` \| `nice-evolve` |
| `title` | One line |
| `why` | Why raised from this change |
| `evidence` | Paths/links |
| `confidence` | `high` \| `med` \| `low` |
| `suggested_next` | Usually `/sdd:rules-govern` or “future change” |

## Document shape

1. Header: change-id, whether summary was present, evidence scope, timestamp
2. Harness section (table of items)
3. Rules / ADR section
4. Next-steps section
5. Gaps
6. Disclaimer: proposals only — do not silently rewrite Rules; not a G3 decision

## Bucket guidance

| Bucket | Put here | Do not put here |
|---|---|---|
| `harness` | Bootstrap layout, pipeline/IaC skeleton policy, pack harness tweaks | Tech-debt / business backlog → `/sdd:followups` |
| `rules-adr` | Rules category updates, ADR candidates, authority-tree refinements | Full first-time Rules authoring → `/sdd:rules-govern` |
| `next-steps` | Prioritized follow-on work that is neither harness nor Rules/ADR | Living Spec edits / Gate decisions |

## Priority

- `must-fix` — blocking friction observed in this change; fix soon via Rules/harness
- `nice-evolve` — useful refinement; deferrable

## Persistence

Always persist via `write-proposals.sh` to
`docs/ai4se-plugins/<ts>-evolve-proposals.md` (process report). Do not overwrite
proposal / Delta / Living / Rules authority bodies from this skill.
