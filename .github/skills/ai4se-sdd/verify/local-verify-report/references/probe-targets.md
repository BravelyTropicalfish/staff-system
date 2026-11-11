# Probe targets — local-verify-report

`probe-verify.sh` checks these relative to `--root`. Print `FOUND` / `MISSING` /
`NOTE` only for existence probes — never modify business trees. May write a
commands list file under `docs/ai4se-plugins/` for the runner.

## Command whitelist sources (priority order)

| Path | Kind | Role |
|------|------|------|
| `docs/feedback/verification-commands.md` | file | Primary whitelist |
| `docs/feedback/` | dir | Presence signal |
| `docs/knowledge/rules/` | dir | Rules may point to verify commands (NOTE only unless a clear commands file is linked) |
| `README.md` | file | Last-resort **explicit** verify/test/build command blocks |

### Parsing `verification-commands.md`

Preferred machine block:

1. After HTML comment `<!-- ai4se-verify-commands -->`, take subsequent non-empty
   lines that are not markdown headings and not `<!-- … -->`, until a blank line
   or next HTML comment.
2. Else: first fenced ` ```bash ` or ` ```sh ` block — non-empty, non-`#` lines.
3. Else: `NOTE whitelist_format:ambiguous` and do not invent commands.

Emit each discovered command as `COMMAND <line>` and write them one-per-line to
a commands file; print `COMMANDS_FILE <path>`.

If zero commands: `NOTE whitelist_empty:yes`.

## Tools (existence on PATH — NOTE only)

Emit `NOTE tool:<name>:yes|no` for: `node`, `npm`, `pnpm`, `yarn`, `mvn`,
`gradle`, `go`, `cargo`, `python3`, `pytest`, `git`.

## Change artifacts (optional `--change-id`)

When `--change-id` is set, probe `openspec/changes/<change-id>/` (or MISSING):

| Relative path | Kind |
|---------------|------|
| `tasks.md` | file |
| `design.md` | file |
| `proposal.md` | file |
| `specs/` | dir |

Also list delta spec files under `specs/` as `NOTE change-spec:<relpath>` (cap
reasonable; do not dump huge trees).

Without `--change-id`, if `openspec/changes/` has non-archive children, emit
`NOTE active-change:<name>` for each (agent may pick for C1).

## Feedback / harness slots

| Path | Kind |
|------|------|
| `docs/feedback/verification-matrix.md` | file |
| `docs/feedback/evaluator-rubric.md` | file |
| `AGENTS.md` | file |
| `openspec/` | dir |
| `openspec/changes/` | dir |
