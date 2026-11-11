# documentation-baseline — Documentation rules

## Metadata

| Field | Value |
|---|---|
| **ID** | documentation-baseline |
| **Category** | documentation |
| **Priority** | Medium |
| **Activation** | always |
| **Applies To** | all |
| **Stack** | — |

## Rule

1. Write repository documentation in English.
2. Update the docs in the same change as the code they describe.
3. Put each doc in the right place:
   - architecture: `docs/knowledge/architecture/`;
   - domain: `docs/knowledge/domain/`;
   - operations: `docs/knowledge/operations/`;
   - specs and changes: `openspec/`.
4. Give each doc one purpose and one owner.
   Do not copy the same content into two places.
5. Use relative links. Check them before merge.
6. Keep the README short: what this is, how to start, where to find more.
7. Record decisions that affect future work.
   Do not store meeting notes as docs.

## Rationale

One source per topic saves time and prevents contradictory docs.

## Verification

| Check | Blocks |
|---|---|
| Docs updated with the code | pr-merge |
| Links resolve inside the repository | pr-merge |
| No duplicate source for the same topic | pr-merge |

## Examples

Good: the API doc is updated in the same pull request as the API change.

Bad: a second copy of the same spec in another folder.

## Exceptions

- Generated docs such as `_INDEX.md` are updated by script, not by hand.
