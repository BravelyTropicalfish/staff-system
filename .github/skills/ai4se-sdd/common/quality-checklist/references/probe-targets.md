# Probe targets — quality-checklist

`probe-stage.sh --stage pN` prints `FOUND` / `MISSING` / `NOTE` for evidence
paths typical of that stage. Read-only — never modify business trees.

## Common (all stages)

| Path | Kind |
|------|------|
| `AGENTS.md` | file |
| `docs/` | dir |
| `docs/ai4se-plugins/` | dir |
| `docs/knowledge/` | dir |
| `docs/knowledge/rules/` | dir |
| `docs/feedback/` | dir |
| `openspec/` | dir |
| `openspec/changes/` | dir |
| `requirement-workspace/` | dir |

Also: `NOTE tool:rg:yes|no` (for optional isolation scans when editing this pack).

## Stage overlays

### p0

- `openspec/`, `docs/knowledge/`, `docs/feedback/`, `AGENTS.md`
- `NOTE active-change:<name>` when non-archive changes exist
- Rules signal: `docs/knowledge/rules/` or project Rules carrier paths if present

### p1

- `requirement-workspace/` (+ `analysis/`, `ready/` if present)
- Pre-SDD / proposal signals under `openspec/changes/` (active-change notes)

### p2

- Active change folder files when detectable: specs, `dov-ac-review.md`,
  `g1-decision.md` (FOUND/MISSING under each `NOTE active-change`)

### p3

- Under active changes: `design.md`, `tasks.md`, ATDD-related paths if present

### p4

- Under `docs/ai4se-plugins/`: latest `*-local-verify-report.md`,
  `*-pipeline-report.md` if any (NOTE names)
- Under active change: `g2-decision.md` if present

### p5

- Under active change or archive signals: living-merge / summary / `g3-decision.md`
- `NOTE` only — do not perform archive

## Forbidden

- Approving Gates
- Mutating source / openspec authority trees
- Inventing Pass without evidence
