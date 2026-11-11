# nfr-baseline — Quality targets

## Metadata

| Field | Value |
|---|---|
| **ID** | nfr-baseline |
| **Category** | architecture |
| **Priority** | Medium |
| **Activation** | always |
| **Applies To** | all |
| **Stack** | — |

## Rule

1. Before a public release, write down targets for:
   - response time;
   - availability;
   - error rate;
   - recovery time.
2. Add one target for observability: logs, metrics, and alerts for the main user path.
3. Each target must say how it is checked and who owns it.
4. If a target is not known yet, write `TBD`, the owner, and the date to decide.
   Do not leave it blank.
5. A change that makes a target worse needs an explicit human decision.
6. Keep this file short. Put detailed test plans in the project's test documents.

## Rationale

Quality targets turn "it feels slow" into a check that a team can verify.

## Verification

| Check | Blocks |
|---|---|
| Targets exist for the main user path | release |
| Each target has a check and an owner | release |
| A regression has a human decision | release |

## Examples

Good: "Search returns in under 1 second for 95% of requests; checked by the load test."

Bad: "The system should be fast."

## Exceptions

- Before the first release, `TBD` is allowed if an owner and a decision date are recorded.
