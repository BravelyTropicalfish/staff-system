# Impact brief outline (pack default)

Default path: `<workdir>/impact-brief.md` (caller may override).

## Required sections

1. **Requirement set** — ids/paths of structured requirements in scope.
2. **As-is asset inventory (evidence paths)** — what was inspected:
   - Knowledge base under `docs/` (or project-documented KB slots)
   - Living Spec: `openspec/specs/`
   - Post-archive SDD assets (archived changes / archive outputs as laid out in
     the repo; if absent, record the gap — do not invent paths)
3. **As-is → proposed delta** — for each **touched** asset:
   - **As-is:** current statement/scope (with evidence path)
   - **Proposed delta:** how current requirements would change it
   For inspected-but-untouched assets: short “checked, no impact” notes.
4. **Blast radius** — systems / runtime capabilities affected.
5. **Artifact impact** — specs / tests / docs / KB entries affected (distinct
   from blast radius).
6. **Dual-track notes** — implications for OpenSpec vs agile storytelling.
7. **Risks & blockers** — critical unknown dependencies must appear as blockers.
8. **Suggested split boundaries** — inputs for `/sdd:proposal-split` and/or
   `/sdd:story-split`.

Prefer evidence paths over speculative architecture essays.
