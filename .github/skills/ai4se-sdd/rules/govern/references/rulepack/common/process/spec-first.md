# spec-first — Spec-First before production code

## Metadata

| Field | Value |
|---|---|
| **ID** | spec-first |
| **Category** | process |
| **Priority** | Critical |
| **Activation** | always |
| **Applies To** | all |
| **Stack** | — |

## Rule

1. Do not write production code without an agreed change-spec package for that change.
2. Prefer a clear artifact order: intent/proposal → spec (+ optional design) → tasks → implementation → archive (or the equivalent sequence of the project's chosen SDD backend).
3. PRs must cite a stable Spec ID for the change and keep change artifacts under the project's SDD workspace (path and layout follow the selected backend).
4. The Spec-First **obligation** is backend-agnostic. OpenSpec, Speckit, and other SDD frameworks are interchangeable carriers — choose one per project (or per change family) and record it; do not hard-bind policy text to a single tool name or folder layout.

## Rationale

Keeps Spec-First executable and prevents implementation drift from unverified intent, while remaining compatible with multiple SDD backends as the pack evolves.

## Verification

| Check | Blocks |
|---|---|
| Change-spec package + required artifacts present for the selected backend | pr-merge |
| PR body contains Spec ID | pr-merge |

## Examples

### Good

- PR cites `Spec ID: add-foo` and includes the change package required by the project's SDD backend (e.g. OpenSpec change under `openspec/changes/…`, or Speckit / other equivalent layout).

### Bad

- Feature branch with production code only and no Spec ID / no change-spec package.

## Exceptions

- Pure docs under `docs/**` may be exempt when org policy says so.
- Edits solely inside the project's SDD workspace (spec tooling trees) do not require a nested change for meta-edits of existing packages.
