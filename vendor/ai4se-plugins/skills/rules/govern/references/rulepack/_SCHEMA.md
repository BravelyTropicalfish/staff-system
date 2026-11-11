# Rule schema (pack default)

Each rule is a Markdown file. Metadata uses a Markdown table (no YAML frontmatter).
Filename stem MUST equal the `ID` field.

## Required metadata fields

| Field | Values |
|---|---|
| ID | Stable id, e.g. `spec-first` |
| Category | `process` · `architecture` · `quality` · `testing` · `security` · `documentation` |
| Priority | Critical \| High \| Medium \| Low |
| Activation | always \| auto \| manual |
| Applies To | `all` or comma-separated submodule ids |
| Stack | optional language/runtime (e.g. `python`, `typescript`) |

## Required sections

1. Metadata table
2. Rule (normative bullets)
3. Rationale (short)
4. Verification (what to check; where it should block — tool wiring is out of scope here)
5. Examples (good / bad)
6. Exceptions (if any)
