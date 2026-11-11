# Pack rulepack

Source material for `/sdd:rules-govern`.

- `common/` — MECE-mirrored Critical rules (and category README stubs)
- `langs/<lang>/` — language source material rewritten into MECE on apply (not a parallel authority tree)

**Allowed langs only:** `cpp` `golang` `java` `kotlin` `python` `react` `react-native` `rust` `swift` `typescript` `vue` `web`\
Layout: `langs/<lang>/<category>/<lang>-<topic>.md`. Do not ship other language folders.

Select languages during brainstorming (`--langs`). Never flatten directories when projecting to hosts.
