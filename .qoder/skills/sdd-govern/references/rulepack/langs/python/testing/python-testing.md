# python-testing — Python Testing

## Metadata

| Field | Value |
|---|---|
| **ID** | python-testing |
| **Category** | testing |
| **Priority** | High |
| **Activation** | auto |
| **Applies To** | all |
| **Stack** | python |

## Rule

## Framework

Use **pytest** as the testing framework.

## Coverage

```bash
pytest --cov=src --cov-report=term-missing
```

## Test Organization

Use `pytest.mark` for test categorization:

```python
import pytest

@pytest.mark.unit
def test_calculate_total():
    ...

@pytest.mark.integration
def test_database_connection():
    ...
```

## Reference

## Rationale

Stack-specific testing norms for `python` keep agent output aligned with the project's chosen stack without duplicating common Rules.

## Verification

| Check | Blocks |
|---|---|
| PR review against this rule when touching `python` sources | pr-merge |

## Examples

See Good/Bad (or CORRECT/WRONG) samples in the Rule section when present.

## Exceptions

- None unless recorded in the project decision log / org policy.
