# Ready-shaped outline (optional enrichment)

Optional expansion when six-field entries need a fuller Ready shape before
split. Gaps are allowed — mark unknowns; **do not invent facts**.

## Sections

1. **Intent** — one sentence: what + why (outsider-readable).
2. **Actors** — roles and permission boundaries involved.
3. **In / Out scope** — explicit in-scope list; at least one out-of-scope item
   (or “None” with a short explanation).
4. **Scenarios + AC hints** — concrete situations with observable expected
   results (`SHALL`-style). Prefer ≥1 exception or boundary scenario when risk
   warrants it. These feed acceptance hints; do not invent fake AC.
5. **Constraints** — facts only (dependencies, rules, timing, app-arch
   boundaries). Not class-level solutions or tech-stack bikesheds.
6. **Context** — enough background for an outsider; cite evidence paths when
   available.
7. **Affected capabilities** — living-spec / capability / app-service names that
   would change (ADDED / MODIFIED / REMOVED). Unchanged consumers belong under
   constraints / dependencies, not here. Prefer application-architecture names
   over code symbols.
8. **Open questions** — blocking vs non-blocking at **application architecture**
   granularity; leave gaps explicit. Use
   `status: open | resolved | deferred` (后续解决). Blocking items pause for
   Superpowers brainstorming and **must** become `resolved` (or escalate → hard
   stop) before impact/split; non-blocking deferral is asked only after
   generation (see [open-questions.md](../../../../vendor/ai4se-plugins/skills/intent/pre-spec-req/references/open-questions.md)).

## Mapping to six-field default

| Six-field | Ready-shaped home |
|---|---|
| id | Metadata / filename convention in workdir |
| statement | Intent (+ short Context if needed) |
| actors | Actors |
| rationale | Context / Intent why |
| acceptance hints | Scenarios + AC hints |
| links to evidence | Context, Constraints, Open questions |

Keep entries backend-agnostic enough for both OpenSpec and agile tracks.
