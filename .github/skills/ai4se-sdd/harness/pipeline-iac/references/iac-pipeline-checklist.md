# Pipeline / IaC skeleton checklist (pack default)

Before generating or aligning skeletons, confirm Rules cover:

1. CI stages and gates
2. CD trigger boundaries
3. Artifact naming
4. IaC tool + directory conventions
5. Secret handling (no plaintext in git)

## Principles

- Prefer wrapping customer/org templates over greenfield invention.
- Align names/stages to Rules; document deviations in the plan report.
- Generate only missing skeleton files after dry-run + explicit confirm.
- Never invent a parallel CI/CD product; never commit secrets.
- This skill writes skeletons; remote result aggregation belongs elsewhere
  (`/sdd:pipeline-report`). Deploy triggers belong to `/sdd:deploy-artifacts`.

## Rules gate outcomes

| Result | Meaning |
|---|---|
| `pass` | Required blocks covered; safe to draft CREATE items |
| `gap-listed` | Partial gaps; may draft wrap/align items; list gaps; remind `/sdd:rules-govern` |
| `stop-incomplete` | Missing blocks needed for safe generation → stop; return to `/sdd:rules-govern` |
