# coding-style — Simple and consistent code

## Metadata

| Field | Value |
|---|---|
| **ID** | coding-style |
| **Category** | quality |
| **Priority** | Critical |
| **Activation** | always |
| **Applies To** | all |
| **Stack** | — |

## Rule

1. Use clear names. A variable or function name should say what it does.
2. Keep functions small. As a default, stay under 50 lines.
3. Keep files focused. As a default, stay under 800 lines.
4. Use early returns instead of deep nesting.
5. Use named constants for meaningful numbers. Do not use magic numbers.
6. Handle errors. Do not swallow them silently.
7. Validate input at boundaries: user input, files, and network data.
8. Prefer a new value over changing shared data in place.
9. Follow the formatter and linter. Do not argue about style in review.

## Rationale

Simple, consistent code is easier to read and less likely to hide bugs.

## Verification

| Check | Blocks |
|---|---|
| Formatter and linter pass | pr-merge |
| Review checklist has no open style issues | pr-merge |

## Examples

Good: `isExpired`, `MAX_RETRY_COUNT`, and early returns.

Bad: `x`, `doIt`, and `if (a) { if (b) { ... } }`.

## Exceptions

- Generated code may keep the generator's style.
