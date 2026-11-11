# prompt-injection-defense — Defend against prompt injection

## Metadata

| Field | Value |
|---|---|
| **ID** | prompt-injection-defense |
| **Category** | security |
| **Priority** | Critical |
| **Activation** | auto |
| **Applies To** | all |
| **Stack** | — |

## Rule

1. Never concatenate untrusted user content into system prompts without boundaries.
2. Tool/file access from model output must be allowlisted and validated.
3. Treat retrieved documents as untrusted data, not instructions.

## Rationale

LLM features amplify injection risk; pack defaults require defense-in-depth.

## Verification

| Check | Blocks |
|---|---|
| Prompt construction review + runtime guards | pr-merge / runtime |

## Examples

### Good

- User text passed as structured data fields with explicit delimiters.

### Bad

- `system = base + user_message` with no separation.

## Exceptions

- None.
