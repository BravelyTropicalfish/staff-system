# SKILL.md template (ai4se-plugins)

Aligned with Cursor `/create-skill` shape, plus pack governance fields for `commands/catalog.md`.

```markdown
---
name: skill-folder-name
description: >-
  What this skill does and when to use it (third person). Include anti-triggers.
disable-model-invocation: true
status: stub
id: skill.<domain>.<name>
command: /sdd:<name>
impl: native
layer: core
gate: false
---

# Human Title

## Instructions

Step-by-step guidance. Keep short; put long material in `references/`.

Include contract cues inline when needed:
- **In:** …
- **Out:** …
- **Not:** …
- **Escalate:** …

## Examples

Concrete invocation / outcome examples.
```

Rules:
- `name` must match the parent directory name (kebab-case).
- `description` = WHAT + WHEN (and one Do-not).
- Default `disable-model-invocation: true` (explicit `/sdd` / named invoke). Gate skills also set `gate: true`.
- Body sections are only **Instructions** and **Examples** (create-skill mode). Optional `## Additional resources` for in-pack links.
- Progressive disclosure: details in `references/`, `examples/`, or `scripts/`.
