# Probe targets — pipeline-report

`probe-pipeline.sh` checks these relative to `--root`. Print `FOUND` /
`MISSING` / `NOTE` only — never modify business trees, never trigger pipelines
or deploys.

## Always

| Path / signal | Kind |
|---------------|------|
| `.git/` | dir |
| `openspec/` | dir |
| `openspec/changes/` | dir |
| `docs/ai4se-plugins/` | dir |
| `AGENTS.md` | file |
| `README.md` | file |

## Git remotes

If `.git/` exists, run `git -C <root> remote -v` and emit
`NOTE remote:<name> <url>` (prefer listing `origin`).

Host heuristic from remote URL:

- contains `gitlab` → `NOTE host:gitlab`
- contains `github.com` or `github.` → `NOTE host:github`
- other known git host → `NOTE host:other`
- else → `NOTE host:unknown`

Also emit when available:

- `NOTE branch:<name>`
- `NOTE head_sha:<short-or-full>`

## Tools (PATH)

- `NOTE tool:glab:yes|no`
- `NOTE tool:gh:yes|no`

## Active changes (optional hint)

If `openspec/changes/` has non-archive children, emit
`NOTE active-change:<name>` for each (agent may use for change-id).

## Forbidden

- Invoking `glab ci run`, deploy triggers, or any write API
- Inventing pipeline IDs or job results
