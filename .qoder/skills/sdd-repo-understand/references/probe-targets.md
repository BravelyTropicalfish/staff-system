# Probe targets (pack default)

`probe-layout.sh` checks these relative to `--root` (and optionally under each
`--focus` path). Print `FOUND` / `MISSING` / `NOTE` only — never modify files.

## Always (repo root)

| Path | Kind |
|---|---|
| `AGENTS.md` | file |
| `README.md` | file |
| `docs/` | dir |
| `docs/ai4se-plugins/` | dir |
| `docs/knowledge/` | dir |
| `docs/knowledge/rules/` | dir |
| `docs/feedback/` | dir |
| `requirement-workspace/` | dir |
| `openspec/` | dir |
| `openspec/specs/` | dir |
| `openspec/changes/` | dir |
| `package.json` | file |
| `pnpm-workspace.yaml` | file |
| `go.mod` | file |
| `Cargo.toml` | file |
| `pom.xml` | file |
| `build.gradle` | file |
| `build.gradle.kts` | file |
| `pyproject.toml` | file |
| `requirements.txt` | file |

## Spec active changes (when `openspec/changes/` exists)

List immediate child directory names under `openspec/changes/` excluding
`archive` as `NOTE active-change:<name>`. Do not recurse into change bodies.

## Focus overlays

When `--focus` lists relative paths, also emit `FOUND`/`MISSING` for each focus
path itself, then re-check the Always table **relative to each existing focus
directory** for the same basenames (manifests + `openspec/` + `AGENTS.md` only
— skip inventing nested `docs/knowledge` probes beyond existence under focus).

## Greenfield heuristic (HINT for agent; script prints NOTE)

If **all** of these are MISSING: `AGENTS.md`, `openspec/`, `docs/knowledge/`,
and **no** common manifest (`package.json`, `go.mod`, `Cargo.toml`, `pom.xml`,
`build.gradle`, `build.gradle.kts`, `pyproject.toml`, `requirements.txt`) is
FOUND → print `NOTE greenfield_suspected:yes`. Otherwise
`NOTE greenfield_suspected:no`.
