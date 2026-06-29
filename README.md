# accounts

**Move a player's entire history to a new UUID — across every plugin on your network — in one command.**

[![Build](https://img.shields.io/github/actions/workflow/status/albemiglio/accounts/build.yml?branch=main)](../../actions)
[![License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)
[![Version](https://img.shields.io/badge/version-1.0--SNAPSHOT-orange.svg)](pom.xml)
[![Java](https://img.shields.io/badge/Java-8%2B-red.svg)](pom.xml)
[![Platforms](https://img.shields.io/badge/platforms-Spigot%20%7C%20Bungee%20%7C%20Velocity-green.svg)](#supported-platforms)

When a player's UUID changes — cracked-to-premium, an offline-mode network adopting online UUIDs, a
forced account merge — their data doesn't follow. LuckPerms still keys their rank under the old UUID.
Their Essentials home, their CoreProtect logs, their tamed wolves in the world file, their PlayerPoints
balance: all stranded under an identity that no longer logs in. The player rejoins as a stranger.

`accounts` rewrites every one of those references from the old UUID to the new one, in a single
network-wide operation, and tells you when it's done so the player can be let back in safely.

---

## Why this is hard (and why one regex won't do it)

Every plugin stores a UUID its own way, and "find and replace the UUID" quietly misses most of them:

- **SQL plugins** store it as a dashed string, *or* 32 undashed hex chars, *or* a raw `BINARY(16)` blob.
  A migration that only rewrites dashed strings silently skips the other two — balances and ranks just
  don't move, with no error.
- **Flat-file plugins** key the player by the *filename* (`<uuid>.yml`), not by anything inside the file.
- **The world itself** holds UUIDs in NBT — tamed pets, player heads, boss-bar viewers — and Minecraft
  has encoded those three different ways across its history (a 4-int array since 1.16, a dashed string
  before it, and a `Most`/`Least` long pair before that). A world from any version can hold any of them.
- **Vanilla JSON** (`ops.json`, `whitelist.json`, `banned-players.json`, `usercache.json`) is its own format again.

`accounts` handles all of these as first-class **module types**, so "rewrite this player's identity"
actually means *everywhere*, not just the easy half.

> **Honest scope.** `accounts` is an **engine plus admin-configured templates**, not magic. You tell it
> which databases, files, and worlds to touch (a few lines of YAML per plugin — most are shipped ready to
> copy). It does the cross-encoding, cross-instance, transactional rewrite. It does **not** auto-discover
> your plugins, and it does **not** invent a schema it wasn't told about. Verify the template against your
> install before you run it on live data — the shipped ones say where their schema came from.

---

## Features

- **One engine, five storage types.** SQL (`sql`), flat files (`file`), Minecraft world NBT (`world`),
  vanilla server JSON (`json`), and community-supplied custom types via an SPI — all driven by the same
  migrate call.
- **All three SQL UUID encodings.** Per column you pick `dashed` (default), `undashed`, or `binary`; the
  value is bound the same way the owning plugin would write it, so `BINARY(16)` columns migrate instead
  of being skipped.
- **The NBT world-data capstone.** A scan-all rewriter walks the entire NBT tree of region and `.dat`
  files and rewrites the old UUID in *all three* historical encodings, so vanilla, modded, and datapack
  tags from 1.8 onward are caught — no fixed tag list to fall behind. On Spigot, in-memory world objects
  (loaded pets, heads) are rewritten live so the next world save can't overwrite the on-disk change.
- **Network-wide and reliable.** Multi-instance coordination over Redis makes a migration durable
  (survives a restart), idempotent (safe to replay), atomic per database (commit-or-rollback), and
  catch-up capable (an instance that was down applies what it missed on boot).
- **A completion barrier.** `isComplete(from, to)` is true only once *every* live instance has applied
  the migration — so a login gate (e.g. [Nyx](#integrating-with-another-plugin)) can hold the player out
  until their data has actually arrived everywhere.
- **Ten ready-to-use plugin templates.** LuckPerms, EssentialsX, CoreProtect, Towny (SQL + flatfile),
  GriefPrevention, PlayerPoints, CMI, plus vanilla world and JSON. Copy, point at your database/folder,
  enable.
- **Three platforms, one command.** Spigot, BungeeCord, and Velocity all expose
  `/accounts migrate <fromUuid> <toUuid> [username]` and ship the same module engine.

---

## Supported platforms

| Platform   | Runs the engine | Admin command | Notes |
|------------|:---------------:|:-------------:|-------|
| **Spigot/Paper** | ✅ | ✅ | Also rewrites *live* in-memory world data when a `world` module is enabled. Java 8, 1.8–latest. |
| **BungeeCord**   | ✅ | ✅ | Proxy-side; exposes the in-process `MigrationService` API for other plugins. |
| **Velocity**     | ✅ | ✅ | Proxy-side; Java 17. Same API. |

## Supported plugins (shipped templates)

These live in [`available-modules/`](available-modules/) — copy the one you need into your server's
`plugins/Accounts/modules/` folder and adjust the connection details.

| Plugin | Template | Type | UUID storage | Verified against |
|--------|----------|:----:|--------------|------------------|
| LuckPerms | `luckperms.yml` | sql | dashed string | LuckPerms 5.x schema |
| EssentialsX | `essentialsx.yml` | file | `<uuid>.yml` filename | userdata layout |
| CoreProtect | `coreprotect.yml` | sql | dashed string | `Database.java` (v23.x) |
| Towny (SQL) | `towny-sql.yml` | sql | dashed string | `TownySQLSource.java` |
| Towny (flatfile) | `towny-flatfile.yml` | file | `<uuid>.txt` filename | `TownyFlatFileSource.java` |
| GriefPrevention | `griefprevention.yml` | sql | dashed string (`name`/`owner` cols) | `DatabaseDataStore.java` |
| PlayerPoints | `playerpoints.yml` | sql | dashed string | migration `_1_Create_Tables.java` |
| CMI | `cmi.yml` | sql | `TEXT` (**unverified — confirm encoding**) | runtime SQL from issue #1306 |
| Vanilla world | `world.yml` | world | NBT (all 3 encodings) | — |
| Vanilla JSON | `vanilla-json.yml` | json | ops/whitelist/bans/usercache | — |

Anything not on this list is a few lines of YAML away — see
[Writing a module template](docs/writing-a-module-template.md). For a plugin with an exotic storage
format, ship a jar module ([SPI guide](docs/writing-a-module-template.md#custom-module-types-via-spi)).

---

## Quickstart

### 1. Build

```bash
git clone https://github.com/albemiglio/accounts.git
cd accounts
mvn -q clean package
```

The platform jars land in `spigot/target/`, `bungee/target/`, and `velocity/target/`. Drop the one for
your server into its `plugins/` folder.

### 2. Point it at Redis

The engine coordinates over Redis (required, even for a single instance — see
[Multi-instance setup](docs/multi-instance-redis.md)). Edit `plugins/Accounts/config.yml`:

```yaml
redis:
  host: localhost
  port: 6379
  password: ""

# Folder (under this plugin's data directory) holding one YAML per database/folder/world to migrate.
modules-dir: modules
```

### 3. Enable a plugin template

Copy a shipped template into the modules folder. To migrate LuckPerms data, for example:

```bash
cp available-modules/luckperms.yml plugins/Accounts/modules/luckperms.yml
```

```yaml
# plugins/Accounts/modules/luckperms.yml
name: luckperms
platform: SPIGOT
type: sql
enabled: true
database:
  type: sqlite
  database: plugins/LuckPerms/luckperms-sqlite.db
replacers:
  - table: luckperms_players
    column: uuid
  - table: luckperms_user_permissions
    column: uuid
```

> **Verify before you run.** Open one row of the target table and confirm the UUID is stored the way the
> template assumes (dashed string here). If it's undashed or `BINARY(16)`, add `format: undashed` /
> `format: binary` to that replacer. Running blind on the wrong encoding silently migrates nothing.

### 4. Migrate

Restart the server (so the module loads), then run the command as an operator:

```
/accounts migrate 069a79f4-44e9-4726-a5be-fca90e38aaf5 c06f8906-4c8a-4911-9c29-ea1dbd1aab82 Steve
```

That rewrites every `uuid` row in both LuckPerms tables from the old UUID to the new one, in a single
transaction (all-or-nothing), then broadcasts it to every other instance on the network so they apply it
too. When `isComplete(from, to)` returns true, the data has landed everywhere.

A full worked example — including the world file and a second server — is in
[docs/quickstart-example.md](docs/quickstart-example.md).

---

## Architecture overview

```
              /accounts migrate <from> <to>            another plugin (e.g. Nyx)
                          │                                   │ MigrationService API
                          ▼                                   ▼
                 ┌──────────────────────────────────────────────────┐
                 │            AccountsEngine  (per instance)         │
                 │   migrate() · isComplete() · isInProgress()       │
                 └───────────────┬──────────────────┬───────────────┘
                                 │                  │
                  BroadcastMigrationService    RedisMigrationSubscriber
                   (record → apply → publish)   (apply broadcasts from peers)
                                 │
                          InstanceMigrator   ── idempotent, per-instance "have I applied this?"
                                 │
              ┌──────────────┬───┴────────┬──────────────┬──────────────┐
           SQL module    file module   world module   json module   jar module (SPI)
        (ColumnReplacer  (rename       (NBT scan-all  (vanilla      (community
         + UuidCodec)     <uuid>.ext)   rewriter)      json)         ModuleProvider)
                                 │
                         Redis: durable record · expected/applied/failed sets · pub-sub · instance heartbeats
```

- **`AccountsEngine`** is the single handle a platform plugin holds: build on enable, `migrate()` from
  the command or the API, ask `isComplete()` for an unlock gate, `close()` on disable.
- **`BroadcastMigrationService`** records the migration durably, applies it locally, and publishes it to
  peers. On boot it recovers anything it missed while down.
- **`InstanceMigrator`** runs the enabled modules for one instance, exactly once (idempotent), and never
  lets a module failure crash the caller — it records the failure and retries later.
- **Modules** are the storage adapters; each `Module.execute()` wraps its work in a per-database
  transaction. New types arrive via the `ModuleProvider` SPI without touching the core.

Full walkthrough: [docs/how-the-engine-works.md](docs/how-the-engine-works.md).

### Integrating with another plugin

A plugin on the same proxy (this is how [Nyx](https://github.com/albemiglio/Nyx) drives it) can trigger a
migration in-process, instead of via the command, through the `MigrationService` interface in the `api`
module:

```java
MigrationService accounts = /* look up the accounts plugin */;
accounts.migrate(oldUuid, newUuid, username);
if (accounts.isMigrationInProgress(oldUuid, newUuid)) {
    // hold the login until the transfer finishes everywhere
}
```

Ship the `api` jar as a **`provided`** dependency — never shade it, or the two copies get different
classloader identity and the cast fails.

---

## Documentation

- [How the migration engine works](docs/how-the-engine-works.md) — durability, idempotency, the
  completion barrier, what "atomic per database" really guarantees.
- [Writing a module template](docs/writing-a-module-template.md) — the full YAML schema, every type,
  the `format: dashed|undashed|binary` option, and the jar-module SPI.
- [The plugin-template catalog](docs/plugin-template-catalog.md) — every shipped template, what it
  touches, and how to verify it against your install.
- [Multi-instance (Redis) setup](docs/multi-instance-redis.md) — why Redis is required, the keys it
  uses, and how the completion barrier works across servers.
- [Quickstart example](docs/quickstart-example.md) — one real end-to-end migration, including the world.

---

## Caveats — read before production

- **Redis is required**, even for a single server: the engine always coordinates through it.
- **Back up first.** A UUID migration rewrites live player data in place. Take a backup and test on a
  copy. Verify each template's assumed UUID encoding against a real row before running it.
- **H2 is not supported** for live migration — the owning plugin holds an exclusive lock on the
  `.mv.db` file. Point that plugin at SQLite or MySQL instead (both migrate fine).
- **World/NBT migration is safest with the world at rest.** On Spigot the live in-memory objects are
  rewritten too, but the on-disk NBT rewrite touches data that isn't currently held in memory; run it
  during low activity, with a backup.
- **The CMI template is unverified** (CMI is closed source) — confirm its UUID column encoding before use.

---

## Contributing

PRs welcome — new plugin templates especially. See [CONTRIBUTING.md](CONTRIBUTING.md) and the
[Code of Conduct](CODE_OF_CONDUCT.md). Security issues: [SECURITY.md](SECURITY.md).

## License

Released under the [MIT License](LICENSE) © 2023 Alberto Migliorato — permissive, so you can embed
`accounts` in closed- or open-source plugins alike.
