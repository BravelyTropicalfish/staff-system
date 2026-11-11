# openspec/

Default Spec backend tree for this repository.

- `specs/` — living specifications (main specs)
- `changes/` — in-flight change packages (`changes/archive/` for archived)
- `templates/` — `change-spec.md` / `main-spec.md` stubs
- `config.yaml` — minimal placeholder; extend after OpenSpec install

Install and operate OpenSpec per project docs. This pack does not vendor OpenSpec.
Handoff from Pre-SDD `requirement-workspace/` into `changes/` — do not treat other
folders as a second long-term Living Spec authority.
