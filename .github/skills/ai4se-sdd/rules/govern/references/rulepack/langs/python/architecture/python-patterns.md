# python-patterns — Python Patterns

## Metadata

| Field | Value |
|---|---|
| **ID** | python-patterns |
| **Category** | architecture |
| **Priority** | High |
| **Activation** | auto |
| **Applies To** | all |
| **Stack** | python |

## Rule

## Protocol (Duck Typing)

```python
from typing import Protocol

class Repository(Protocol):
    def find_by_id(self, id: str) -> dict | None: ...
    def save(self, entity: dict) -> dict: ...
```

## Dataclasses as DTOs

```python
from dataclasses import dataclass

@dataclass
class CreateUserRequest:
    name: str
    email: str
    age: int | None = None
```

## Context Managers & Generators

- Use context managers (`with` statement) for resource management
- Use generators for lazy evaluation and memory-efficient iteration

## Reference

## Rationale

Stack-specific patterns norms for `python` keep agent output aligned with the project's chosen stack without duplicating common Rules.

## Verification

| Check | Blocks |
|---|---|
| PR review against this rule when touching `python` sources | pr-merge |

## Examples

See Good/Bad (or CORRECT/WRONG) samples in the Rule section when present.

## Exceptions

- None unless recorded in the project decision log / org policy.
