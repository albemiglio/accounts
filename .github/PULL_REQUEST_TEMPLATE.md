<!-- Thanks for contributing! Keep correctness front of mind — a silent miss costs a player their data. -->

## What this changes

<!-- One or two sentences. Link the issue it closes, if any: "Closes #123". -->

## Type

- [ ] New plugin template
- [ ] New module type / SPI jar support
- [ ] Engine / core change
- [ ] Docs only
- [ ] Bug fix

## Checklist

- [ ] `mvn -q clean install` passes locally (tests included).
- [ ] New behaviour is covered by a test (plain JUnit 5, in-memory SQLite / temp dirs — see `core/src/test`).
- [ ] If this adds/changes a template: the schema was **verified against a real install or the plugin's
      source**, and the YAML comment says where it came from.
- [ ] No new runtime dependency (or it was discussed in an issue first).
- [ ] Docs updated if behaviour or the YAML schema changed.

## Verification notes

<!--
For a template: which plugin version, where you confirmed the schema (source file / live row), and the
UUID encoding (dashed / undashed / binary / filename).
For an engine change: what you ran to confirm it works.
-->
