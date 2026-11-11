# Story splitting patterns (short card)

Use when one requirement (or Epic) yields multiple stories. Align vocabulary
with Change-split heuristics on the OpenSpec track; this card is for the agile
story list only.

## Patterns

| Pattern | Split by | Prefer when | Example shape |
|---|---|---|---|
| **Workflow** | Stages of the user journey | Multi-step flows with clear phases | See list → run action → handle failure |
| **Happy / Sad** | Success path vs exception / error | Risk lives in failure modes; happy path is enough for a first sprint | Happy: submit succeeds; Sad: validation / timeout |
| **Simple / Complex** | Baseline case vs variants | One core path, then richer rules / edge cases | Simple: single item; Complex: bulk / multi-currency |

## Rules of thumb

- Prefer vertical, user-valuable slices over tech-layer stories.
- Each story keeps an AC summary and links to requirement ids.
- One scenario / AC theme should land in one story (no duplicate coverage);
  if a scenario spans stories, split the AC into independent pieces.
- When OpenSpec Changes already exist, attribute Ready scenarios to the matching
  Change’s story; cover every scenario (no orphans).
- Size hints stay light; do not invent tracker estimates without a team policy.
- Optional workdir registry / tracking table: insert idempotent Change↔Story
  rows; do not invent ALM Story IDs.

## Anti-patterns

- Stories that only say “build the API” / “add the component” with no
  user-observable outcome.
- Packing every happy + sad + complex variant into one oversized story when the
  team wants parallel backlog items.
- Same scenario copied into multiple stories without splitting the AC.
