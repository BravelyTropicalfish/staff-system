# iac-conventions — Infrastructure file rules

## Metadata

| Field | Value |
|---|---|
| **ID** | iac-conventions |
| **Category** | architecture |
| **Priority** | High |
| **Activation** | always |
| **Applies To** | all |
| **Stack** | — |

## Rule

1. If the project manages infrastructure as code, use one tool for all environments.
   Do not mix tools for the same resource.
2. Keep infrastructure files in one top-level directory.
   Do not scatter them across application folders.
3. If the tool uses state, store state in a remote backend with locking.
   Do not commit state files.
4. Run a plan or preview before apply.
   A human must review the plan for production.
5. Do not change production infrastructure by hand in a console.
   If an emergency change is needed, record it and add it to code afterwards.
6. Secrets must come from the secret store, not from committed variable files.

## Rationale

One tool and one directory are easier to review. A reviewed plan prevents surprise changes in production.

## Verification

| Check | Blocks |
|---|---|
| Infrastructure files are in the agreed directory | pr-merge |
| Production plan has a human reviewer | deploy |
| No state or secret files in Git | pr-merge |

## Examples

Good: a plan is attached to the change and reviewed before apply.

Bad: a manual console change with no code update.

## Exceptions

- A small project may defer infrastructure code until it has a shared environment.
  Record the decision in the P0 report.
