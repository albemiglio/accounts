# Contributing to accounts

Thanks for helping out. `accounts` migrates a Minecraft player's data to a new UUID across every plugin
on a network, so correctness matters more than features — a silent miss means a player loses their rank
or balance. Keep that in mind and you'll fit right in.

## Ways to contribute

- **New plugin templates** are the most valuable contribution. If you've confirmed how a plugin stores
  UUIDs, a `available-modules/<plugin>.yml` template helps everyone. See
  [Writing a module template](docs/writing-a-module-template.md).
- **Bug reports** with a UUID-encoding detail we missed (an undashed column, a `BINARY(16)` table, an
  NBT tag) — open an issue with the schema evidence.
- **New module types** for storage formats the five built-ins don't cover — either a core `Module`
  subclass with tests, or a standalone jar via the `ModuleProvider` SPI.
- **Docs** — clarifications, more worked examples, more verified template schemas.

## Development setup

```bash
git clone https://github.com/albemiglio/accounts.git
cd accounts
mvn -q clean install   # builds core, api, spigot, bungee, velocity and runs the tests
```

- **Java 8** for `core`, `api`, `spigot`, `bungee` (they run on 1.8–1.16 servers, which are Java 8).
  The `velocity` module overrides to Java 17. Don't use Java 9+ APIs in the shared modules.
- Maven multi-module project; `core` holds the engine and all the tests worth reading first.

## Pull request checklist

- [ ] `mvn -q clean install` passes (tests included) — CI runs this on every module via GitHub Actions.
- [ ] New behaviour has a test. This codebase is well-tested by design; a migration path with no test
      is a migration path that will silently break. Look at `core/src/test/...` for the style — plain
      JUnit 5, no mocking framework, in-memory SQLite / temp dirs.
- [ ] A new template was **verified against a real install or the plugin's source**, and the YAML
      comment says where the schema came from (every shipped template does this).
- [ ] No new runtime dependency without discussing it first.
- [ ] Commits are focused and the message explains the *why*.

## Adding a plugin template (the common case)

1. Find how the plugin stores the player UUID — from its source, its schema, or one row of a live
   database. Note whether it's a dashed string, undashed, `BINARY(16)`, or a per-UUID filename.
2. Copy the closest existing template in `available-modules/` and adjust table/column/format (SQL),
   directory/extension (file), or directory (world/json).
3. Add a comment block at the top stating the plugin version and **where you confirmed the schema**.
   If you couldn't confirm the encoding, say so and mark it unverified (see `cmi.yml`).
4. The template is validated by `AvailableModulesTemplatesTest` — make sure it still passes.

## Code style

- Match the surrounding code. Lombok is available (`@Getter` etc.) and used sparingly.
- Comments explain *why*, and load-bearing ones cite the source they were verified against — keep that
  habit; it's why the templates are trustworthy.
- Keep the engine storage-agnostic. Plugin-specific knowledge belongs in a template or a jar module,
  not in `core`.

## License

By contributing you agree your contribution is licensed under the project's license.

> **Maintainer note (unresolved):** the repository ships a **GPL-3.0** `LICENSE` file, but `pom.xml`
> declares the project license as **MIT**. These contradict each other and must be reconciled before a
> public release. The two options:
>
> - **GPL-3.0** (what the `LICENSE` file says): strong copyleft. Anyone distributing a plugin that links
>   `accounts` must release their source under a compatible license. Good if the goal is to keep
>   derivatives open; a real consideration for a Bukkit/proxy library, since it can affect plugins that
>   bundle it.
> - **MIT** (what `pom.xml` says): permissive. Anyone can use it in closed-source plugins. Maximises
>   adoption and stars; the usual choice for a library meant to be embedded widely.
>
> **Recommended default: MIT**, because `accounts` is a *library/engine* designed to be embedded by other
> plugins (including the `api` jar shipped as a `provided` dependency), and the `pom.xml` already declares
> MIT — permissive licensing removes friction for exactly the integrators who'd otherwise avoid it.
> Whichever you pick, make both files agree: keep one `LICENSE` and set `pom.xml` `<licenses>` to match.
> **This is the maintainer's decision to confirm.**
