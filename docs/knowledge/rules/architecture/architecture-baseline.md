# architecture-baseline — Basic architecture rules

## Metadata

| Field | Value |
|---|---|
| **ID** | architecture-baseline |
| **Category** | architecture |
| **Priority** | High |
| **Activation** | always |
| **Applies To** | all |
| **Stack** | — |

## Rule

1. Keep three parts separate: user interface, business logic, and data access.
   Do not put business rules in the UI or in database code.
2. Dependencies point one way: UI → business logic → data.
   Business logic must not call the UI.
3. Do not create a loop between modules.
   If two modules need each other, move the shared part out or use an interface.
4. Put shared code in one place. Do not copy the same logic into several modules.
5. Keep one clear entry point for each feature. This makes the feature easier to test.
6. Do not add a new framework or service without a stated need and a human decision.
7. Record important architecture decisions in a short note under `docs/knowledge/architecture/`.

## Rationale

Clear boundaries make changes local and tests simple.
A loop or a copy makes every change harder.

## Verification

| Check | Blocks |
|---|---|
| No loop between modules | pr-merge |
| Business logic does not import UI code | pr-merge |
| Important decisions have a short note | pr-merge |

## Examples

Good: an order service calls a data store; the UI calls the order service.

Bad: a database function contains pricing rules.

## Exceptions

- A small script may skip layers if it stays small. Record why in the change description.
