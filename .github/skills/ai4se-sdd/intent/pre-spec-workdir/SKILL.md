---
name: pre-spec-workdir
description: >-
  Create or align the pre-spec requirements working directory and OpenSpec
  handoff notes. Use before clarifying requirements. Do not write OpenSpec
  proposals here.
disable-model-invocation: true
status: ready
id: skill.intent.pre-spec-workdir
command: /sdd:pre-spec-workdir
impl: native
layer: core
gate: false
---

# Pre-spec workdir

## Instructions

Create or align the pre-spec requirements working directory before/while
clarifying intent. **Anti-trigger:** Do not write OpenSpec proposal bodies
(`/sdd:proposal-split`) or Living Spec merges; do not perform requirement
analysis (`/sdd:pre-spec-req`).

1. **Resolve execution `root`.** Prefer the root already confirmed earlier in
   this session (e.g. after `/sdd:repo-understand` or `/sdd:bootstrap`) — reuse
   it; do not re-ask. If this skill runs **standalone** with no prior root, ask
   once (may suggest cwd). Stop if root is unclear. Convention:
   [execution-root.md](../../../../../vendor/ai4se-plugins/references/execution-root.md).
2. Resolve workdir path under that root: use the **user-supplied path parameter**
   when provided; otherwise default to `requirement-workspace/` at `root`.
3. Apply the layout in [workdir-skeleton.md](../../../../../vendor/ai4se-plugins/skills/intent/pre-spec-workdir/references/workdir-skeleton.md)
   **additively** (create missing folders/files only). Never delete or silently
   overwrite conflicting user content — stop and ask for confirmation instead.
   The same requirement subtree is also created by `/sdd:bootstrap`; existing
   paths are skipped (shared skeleton).
4. Write or update `handoff.md` with the **actual** workdir path and how it maps
   into OpenSpec `openspec/changes/` (or the configured backend). If a safe
   handoff path cannot be described → document the gap; do not invent a second
   long-term Living Spec root. If `handoff.md` already has non-empty content →
   update only with user confirmation (do not silently overwrite).
5. Summarize created vs skipped paths. **Next:** `/sdd:pre-spec-req`.

- **In:** execution root; optional workdir path parameter; greenfield/brownfield
  flag
- **Out:** workdir per skeleton (additive); `handoff.md` with OpenSpec connection
- **Not:** requirement analysis; creating `openspec/changes/<id>/`; Living Spec
  authorship
- **Escalate:** human owner if destructive moves would be required; when root is
  unclear

**Gotchas:** Workdir failure ≠ requirements failure — keep this skill separate
from `pre-spec-req`. The workdir is never the long-term spec authority.

## Examples

```text
User: /sdd:pre-spec-workdir
Agent: Asks root once (standalone) or reuses session root; defaults workdir to
<root>/requirement-workspace/; applies shared skeleton additively; writes or
confirms handoff.md; suggests /sdd:pre-spec-req.
```

```text
User: /sdd:pre-spec-workdir (path=docs/req-ws)
Agent: Reuses session root (or asks if none); aligns <root>/docs/req-ws to
skeleton additively; writes handoff.md pointing at openspec/changes/; lists
created/skipped; suggests /sdd:pre-spec-req.
```

## Additional resources

- [Workdir skeleton](../../../../../vendor/ai4se-plugins/skills/intent/pre-spec-workdir/references/workdir-skeleton.md)
- [Execution root convention](../../../../../vendor/ai4se-plugins/references/execution-root.md)
- [OpenSpec integration](../../../../../vendor/ai4se-plugins/integrations/openspec.md)
