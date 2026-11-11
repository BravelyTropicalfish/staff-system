# async-only — Async-only I/O for Python services

## Metadata

| Field | Value |
|---|---|
| **ID** | async-only |
| **Category** | quality |
| **Priority** | Critical |
| **Activation** | auto |
| **Applies To** | all |
| **Stack** | python |

## Rule

1. Python service I/O paths use async APIs consistently within an async boundary.
2. Do not block the event loop with sync I/O in request handlers.
3. Document any sync escape hatches with owner and deadline.

## Rationale

Python async/sync mixes are a frequent production footgun; keep this stack-scoped.

## Verification

| Check | Blocks |
|---|---|
| Lint for blocking I/O in async handlers | pre-commit |

## Examples

### Good

- `await httpx.AsyncClient...` in async handlers.

### Bad

- `requests.get` inside `async def` handler.

## Exceptions

- None.
