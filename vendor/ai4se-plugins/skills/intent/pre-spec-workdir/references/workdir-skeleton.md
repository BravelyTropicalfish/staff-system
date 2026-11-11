# Pre-spec workdir skeleton (pack default)

Shared with `/sdd:bootstrap` for the `requirement-workspace/` subtree. Default
layout for organizing pre-change requirements before opening an OpenSpec
change. The root directory name defaults to `requirement-workspace/` at the
repo `root`; callers may override with a user-supplied workdir path parameter.

```text
<workdir-root>/
├── README.md           # purpose + boundary + OpenSpec handoff pointer
├── handoff.md          # how this maps into openspec/changes/<id>/
├── registry.md         # traceability registry stub
├── intents/            # raw intent captures
├── requirements/       # structured requirement entries (pre-spec-req)
├── _templates/         # requirement / story / checklist templates
├── analysis/           # in-progress requirement analyses
├── ready/              # input-ready requirements (+ stories)
└── archive/            # archived after Spec-ready handoff
```

Apply **additively**: create missing folders/files only; never delete or
silently overwrite non-empty user content.

## Handoff rule

When creating an OpenSpec change, copy or link stabilized requirements into the
change proposal package. Do **not** treat this workdir as the long-term Living
Spec authority — that remains OpenSpec `openspec/specs/` (+ changes tree).
