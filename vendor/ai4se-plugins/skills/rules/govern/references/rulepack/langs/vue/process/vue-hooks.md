# vue-hooks — Vue Hooks

## Metadata

| Field | Value |
|---|---|
| **ID** | vue-hooks |
| **Category** | process |
| **Priority** | Medium |
| **Activation** | auto |
| **Applies To** | all |
| **Stack** | vue |

## Rule

## PostToolUse Targets

Run on `*.vue`, `*.ts`, and `*.tsx` after edits. Scope to changed files where possible.

## Typecheck

- Use `vue-tsc --noEmit` for SFC plus TypeScript checking. Plain `tsc` cannot read `.vue` single-file components, so it must not be the typecheck hook for this project.
- Typecheck is project-wide. Debounce or scope it so a save-on-every-keystroke loop does not stall the editor.

## Lint and Format

- `eslint --fix` with `eslint-plugin-vue` (flat-config `vue/vue3-recommended`) covers both template and script lint.
- `prettier --write` for formatting. Prefer Prettier-via-ESLint over a separate Prettier pass to avoid double formatting and fight loops.

## Architecture Boundaries

- Optional: enforce Feature-Sliced Design slice boundaries with `@feature-sliced/steiger` or `eslint-plugin-boundaries` to block deep cross-slice imports.

## Sequencing

```bash
# changed files only
eslint --fix "$FILE"
prettier --write "$FILE"
# project-wide, debounced
vue-tsc --noEmit
```

- Run lint and format per-file first, then the project-wide typecheck last so type errors reflect the formatted source.

## Reference

- Docs: <https://github.com/vuejs/language-tools> (vue-tsc) · <https://eslint.vuejs.org/> · <https://github.com/feature-sliced/steiger>

## Rationale

Stack-specific hooks and automation norms for `vue` keep agent output aligned with the project's chosen stack without duplicating common Rules.

## Verification

| Check | Blocks |
|---|---|
| PR review against this rule when touching `vue` sources | pr-merge |

## Examples

See Good/Bad (or CORRECT/WRONG) samples in the Rule section when present.

## Exceptions

- None unless recorded in the project decision log / org policy.
