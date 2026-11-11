# Integration: Superpowers (process layer)

## Role

Default process/engineering method skills for AO asset building and complex change work.

## Install

- Preferred: `node install/bin/ai4se-plugins.mjs install` (detects brainstorming skill; clones + links into `.agents/skills/superpowers` when missing). Host marketplace commands (Claude `/plugin install …`, Cursor `/add-plugin superpowers`) remain optional extras.
- Manual: https://github.com/obra/superpowers
- Expect skills such as brainstorming, writing-plans, TDD, systematic-debugging, code-review (names per upstream).

## When /sdd should wrap Superpowers

| /sdd | Upstream use |
|---|---|
| `/sdd:explore` | Brainstorming / exploration methods; pack adds method cards via `method=` |
| Asset construction / planning | Prefer Superpowers writing-plans / brainstorming — do not copy those flows into long native SKILLs |

## Forbidden

- Re-author Superpowers process essays as "new" pack skills without `impl: wrap-superpowers` and a short overlay.
