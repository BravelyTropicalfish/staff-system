# Authority rules tree (pack default)

Review tree materialized under `<root>/docs/knowledge/rules/` by `/sdd:rules-govern`.

```text
docs/knowledge/rules/
├── _SCHEMA.md
├── _INDEX.md
├── _LOG.md
├── README.md
├── process/
│   └── spec-first.md
├── architecture/
│   ├── layer-boundaries.md
│   ├── no-circular-dependency.md
│   └── orchestration-execution-separation.md
├── quality/
│   ├── coding-style.md
│   └── README.md
├── testing/
│   └── README.md
├── security/
│   ├── security-baseline.md
│   ├── no-hardcoded-secrets.md
│   ├── prompt-injection-defense.md
│   └── pii-scrubbing.md
├── documentation/
│   └── README.md
└── modules/
    └── <submodule>/
```

`async-only` is **not** in common Critical; optional via `--langs python` → `quality/async-only.md`.

Allowed `--langs` values (non-common source only):
`cpp` `golang` `java` `kotlin` `python` `react` `react-native` `rust` `swift` `typescript` `vue` `web`.
