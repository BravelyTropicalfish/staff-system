# orchestration-execution-separation — Keep orchestration free of business judgments

## Metadata

| Field | Value |
|---|---|
| **ID** | orchestration-execution-separation |
| **Category** | architecture |
| **Priority** | Critical |
| **Activation** | auto |
| **Applies To** | all |
| **Stack** | — |

## Rule

1. Orchestration/workflow layers route and sequence work; they do not encode domain policy decisions.
2. Business judgments live in domain/capability modules with tests.
3. Prompts/tools may assist execution modules; orchestration still must not own policy.

## Rationale

Mixing orchestration with policy makes systems untestable and unsafe to evolve with agents.

## Verification

| Check | Blocks |
|---|---|
| Architecture review for policy in orchestrators | pr-merge |

## Examples

### Good

- Orchestrator calls `decide_refund()` in domain service.

### Bad

- Orchestrator embeds refund eligibility if/else trees.

## Exceptions

- None.
