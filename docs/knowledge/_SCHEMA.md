# Knowledge base schema

Conventions for Markdown entries under `docs/knowledge/`.

- Human-authored source of truth lives in this tree
- `_INDEX.md` is a compiled artifact — prefer regenerating via `scripts/kb-lint.sh`
- `_LOG.md` is append-only

## Suggested frontmatter

```yaml
---
id: kb.<area>.<slug>
title: …
area: architecture|domain|rules|interfaces|operations|external
---
```
