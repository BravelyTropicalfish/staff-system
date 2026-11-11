# python-security — Python Security

## Metadata

| Field | Value |
|---|---|
| **ID** | python-security |
| **Category** | security |
| **Priority** | Critical |
| **Activation** | always |
| **Applies To** | all |
| **Stack** | python |

## Rule

## Secret Management

```python
import os
from dotenv import load_dotenv

load_dotenv()

api_key = os.environ["OPENAI_API_KEY"]  # Raises KeyError if missing
```

## Security Scanning

- Use **bandit** for static security analysis:
  ```bash
  bandit -r src/
  ```

## Reference

## Rationale

Stack-specific security norms for `python` keep agent output aligned with the project's chosen stack without duplicating common Rules.

## Verification

| Check | Blocks |
|---|---|
| PR review against this rule when touching `python` sources | pr-merge |

## Examples

See Good/Bad (or CORRECT/WRONG) samples in the Rule section when present.

## Exceptions

- None unless recorded in the project decision log / org policy.
