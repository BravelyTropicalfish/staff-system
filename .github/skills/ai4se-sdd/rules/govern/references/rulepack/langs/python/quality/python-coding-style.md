# python-coding-style — Python Coding Style

## Metadata

| Field | Value |
|---|---|
| **ID** | python-coding-style |
| **Category** | quality |
| **Priority** | High |
| **Activation** | auto |
| **Applies To** | all |
| **Stack** | python |

## Rule

## Standards

- Follow **PEP 8** conventions
- Use **type annotations** on all function signatures

## Immutability

Prefer immutable data structures:

```python
from dataclasses import dataclass

@dataclass(frozen=True)
class User:
    name: str
    email: str

from typing import NamedTuple

class Point(NamedTuple):
    x: float
    y: float
```

## Formatting

- **black** for code formatting
- **isort** for import sorting
- **ruff** for linting

## Reference

## Rationale

Stack-specific coding style norms for `python` keep agent output aligned with the project's chosen stack without duplicating common Rules.

## Verification

| Check | Blocks |
|---|---|
| PR review against this rule when touching `python` sources | pr-merge |

## Examples

See Good/Bad (or CORRECT/WRONG) samples in the Rule section when present.

## Exceptions

- None unless recorded in the project decision log / org policy.
