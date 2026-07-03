# Writing a module template

A **module template** is a YAML file that tells `accounts` about one thing to migrate: a database, a
folder of per-player files, a world, or the vanilla JSON files. You drop it in `plugins/Accounts/modules/`
and the engine loads it on startup. This page is the full schema for every type.

Most popular plugins already have a template in [`available-modules/`](../available-modules/) — copy and
adjust before writing one from scratch. See the [catalog](plugin-template-catalog.md).

## Common fields (every type)

```yaml
name: luckperms          # a label for logs; make it unique per file
platform: SPIGOT         # SPIGOT, BUNGEECORD, or VELOCITY — where this data lives
type: sql                # sql (default) | file | world | json
enabled: true            # false = loaded but skipped during migration
```

- **`name`** — identifies the module in logs. One per file.
- **`platform`** — which server/proxy owns this data. Templates are typically `SPIGOT` because that's
  where plugin databases and world files live.
- **`type`** — picks the storage adapter (below). Defaults to `sql` if omitted.
- **`enabled`** — only enabled modules run. A disabled module is loaded but skipped.

One file per database/folder/world. To migrate three worlds (`world`, `world_nether`, `world_the_end`),
that's three `world`-type files.

---

## `type: sql` — database columns

The workhorse. Rewrites a UUID value in one or more table columns. For each replacer the engine runs:

```sql
UPDATE <table> SET <column> = ? WHERE <column> = ?
```

binding the new UUID to the first `?` and the old UUID to the second — in the **encoding you specify**.
All replacers for one file run in a single transaction (all-or-nothing).

```yaml
name: luckperms
platform: SPIGOT
type: sql
enabled: true
database:
  type: sqlite                                    # sqlite | mysql | mariadb
  database: plugins/LuckPerms/luckperms-sqlite.db # for sqlite: the .db file path
# database:                                       # for mysql/mariadb, use this block instead:
#   type: mysql
#   host: localhost
#   port: 3306
#   username: minecraft
#   password: secret
#   database: luckperms
replacers:
  - table: luckperms_players
    column: uuid
  - table: luckperms_user_permissions
    column: uuid
```

### `format: dashed | undashed | binary` (the important one)

Plugins don't agree on how a UUID is stored in a column, and **a migration that rewrites the wrong
encoding silently changes nothing** — no error, no migrated data. Set `format` per replacer to match:

| `format` | Stored value | Example | When |
|----------|--------------|---------|------|
| `dashed` *(default)* | 36-char dashed string | `069a79f4-44e9-4726-a5be-fca90e38aaf5` | most plugins |
| `undashed` | 32 hex chars, no hyphens | `069a79f444e94726a5befca90e38aaf5` | some plugins strip hyphens |
| `binary` | raw `BINARY(16)` blob | (16 bytes) | CoreProtect-style, custom schemas |

```yaml
replacers:
  - table: luckperms_players
    column: uuid
    # format omitted → dashed
  - table: some_binary_table
    column: uuid
    format: binary
```

**How to check which one you need:** open one row of the target table.

```sql
SELECT uuid FROM <table> LIMIT 1;
```

- 36 chars with hyphens → `dashed` (leave it out).
- 32 hex chars, no hyphens → `undashed`.
- unreadable / shows as bytes, column declared `BINARY(16)` → `binary`.

### `disable-foreign-key-checks` — FKs without `ON UPDATE CASCADE`

Some plugins (HuskSync, BattlePass) declare foreign keys between their uuid columns **without
`ON UPDATE CASCADE`** — whichever table is updated first violates the constraint, in either order.
This top-level key suspends enforcement for the module's transaction (`FOREIGN_KEY_CHECKS` on
MySQL/MariaDB, `SET REFERENTIAL_INTEGRITY` on H2) and restores it afterwards, even on rollback.
On SQLite it's ignored with a log line — SQLite doesn't enforce foreign keys by default.

```yaml
type: sql
disable-foreign-key-checks: true   # default false
```

### Prefixed and derived columns

AuxProtect stores `'$' + dashed-uuid` and looks rows up **by a companion hash column** — rewriting the
uuid without the hash bricks the plugin. `prefix` makes matching and rewriting use `prefix + encoded
uuid`; each `derived` column is written **in the same UPDATE**, computed from the full new stored
string. The only `fn` today is `java-string-hashcode` (`INT = String.hashCode()`). Both keys work on
string encodings only — combining them with `format: binary` is rejected when the template loads.

```yaml
replacers:
  - table: auxprotect_uids
    column: uuid
    prefix: "$"                    # stored value = prefix + encoded uuid (dashed default)
    derived:
      - column: hash
        fn: java-string-hashcode   # INT = String.hashCode() of the FULL stored string (prefix+uuid)
```

### `table-pattern` — runtime table discovery

Plugins like ajLeaderboards, BetterRTP and BetterEnderChest create **one table per
board/world/group**, so the table names aren't known when the template is written. Give
`table-pattern` (SQL `LIKE` syntax) **instead of** `table:` — exactly one of the two — and the engine
enumerates the matching tables at migration time, running the same UPDATE on each. A matched table
without the column is skipped with a log line (heterogeneous tables may share a prefix).

```yaml
replacers:
  - table-pattern: "ajlb_%"
    column: uuid
```

### Database notes

- **SQLite** migrates fine while the server is running.
- **MySQL / MariaDB** — point `database` at the same DB the plugin uses.
- **H2** — supported, with one extra required key: `h2-version`, pinned to **the exact version the
  owning plugin bundles** (e.g. GravesX → `"2.4.240"`, AxVaults → `"2.1.214"`). H2 file formats are
  mutually incompatible across versions (1.4 / 2.0–2.1 / 2.2+ each speak a different store format), so
  the engine downloads exactly that driver from Maven Central (cached in `plugins/Accounts/h2-drivers`,
  sha256-verified for known versions), sniffs the store header first and **refuses a mismatched file**
  instead of corrupting it. Connections use `IFEXISTS=TRUE` (a typoed path errors instead of creating an
  empty store). The `database` path is given WITHOUT the `.mv.db` suffix. H2 is single-process: run the
  migration with every server that loads the plugin **stopped**.

---

## `type: file` — one file per UUID

For plugins that key each player by the **filename** (`<uuid>.<ext>`), like EssentialsX userdata or
Towny flatfile residents. Migration renames `old-uuid.ext` → `new-uuid.ext`; the file contents are
untouched (the name is the key).

```yaml
name: essentialsx
platform: SPIGOT
type: file
directory: plugins/Essentials/userdata   # folder holding the <uuid>.<ext> files
extension: yml                            # without the dot
enabled: true
```

- **`extension` is required** and must match exactly (`yml`, `txt`, ...). Files with no extension can't be
  matched by this type (that's why GriefPrevention's extensionless flatfile player files aren't
  templated — only its SQL backend is).

---

## `type: world` — Minecraft world NBT

The capstone. Walks a world's region and `.dat` files and rewrites the old UUID **everywhere it appears
in the NBT tree** — tamed pets, player heads, boss-bar viewers, projectile owners, plugin tags, anything.
Minecraft has stored UUIDs three ways across versions, and this rewrites **all three**:

- a **4-int array** (since 1.16),
- a **dashed string** (before 1.16),
- a **`<name>Most` / `<name>Least` long pair** (older projectile/owner references).

It scans every tag rather than a fixed list, so it catches vanilla, modded, and datapack tags from 1.8
onward. (A 128-bit value colliding with a UUID by accident is impossible, so scan-all has no false hits.)

```yaml
name: world
platform: SPIGOT
type: world
directory: world          # the world folder; one file per world
enabled: true
```

One file per world: `world`, `world_nether`, `world_the_end`, etc.

> **On Spigot, live data is handled too.** When a `world` module is enabled, the Spigot plugin also
> rewrites in-memory world objects (loaded pets, heads in open inventories) on the main thread, so the
> next world save doesn't overwrite the on-disk rewrite. Still, run world migrations during low activity
> and with a backup — see [Caveats in the README](../README.md#caveats--read-before-production).

---

## `type: json` — vanilla server JSON

The vanilla server-root JSON files keyed by UUID: `ops.json`, `whitelist.json`, `banned-players.json`,
`usercache.json`. Rewrites the old UUID to the new one in each.

```yaml
name: vanilla-json
platform: SPIGOT
type: json
directory: .              # the server root (where those .json files live)
enabled: true
```

---

## `type: content` — UUIDs inside text files

For plugins that embed the dashed UUID *inside* a file's content — as a value (`OwnerUUID: <uuid>`),
as a map **key** (`<uuid>: Notch`), in a list (`unique-ids: ["<uuid>"]`), or buried in a composite
string (`"Steve:<uuid>%vote!time"`). The module does a whole-token text swap of the old dashed UUID
(neighbouring hex/dash characters disqualify a match, so fragments of longer ids are never touched),
so comments, formatting and key order survive byte-for-byte. Non-UTF-8 (binary) files and absent
directories are skipped; writes are atomic.

```yaml
name: worldguard-regions
platform: SPIGOT
type: content
enabled: true
targets:
  - directory: plugins/WorldGuard/worlds   # a directory scanned by file-NAME glob...
    pattern: regions.yml                   # (glob matches the file name only, not the path)
    recursive: true
  - directory: plugins/Residence/Save/rent.yml   # ...or a single file (pattern/recursive ignored)
```

Only the lowercase dashed `UUID.toString()` form is matched — plugins storing undashed or binary
UUIDs in text need a different approach. Most plugins that qualify here cache these files in memory
and rewrite them on autosave: say so in the template header and require the server to be stopped.

---

## Shipping a module inside your plugin

If you maintain the plugin itself, you don't have to ask server owners to install a template: bundle
it in your jar, plugin.yml-style. Add `src/main/resources/accounts-module.yml` (or several files
under `accounts-modules/*.yml` — handy for the rename+content pairs) with exactly the same schema as
the templates above. No code, no dependency on accounts: the engine scans the plugins folder on
startup and picks it up automatically.

```yaml
# accounts-module.yml, at your jar's root
name: myplugin-userdata
platform: SPIGOT
type: file
directory: plugins/MyPlugin/userdata
extension: yml
enabled: true
```

A server-local file with the same `name` in the modules folder overrides the bundled one, so
operators can still tweak or disable what you ship.

---

## Scanning an unknown plugin

For a closed-source plugin with no template, you don't have to reverse-engineer its storage. The
engine's scanner takes a **probe UUID** — a real player known to have data in that plugin — and hunts
for it across each `plugins/<Plugin>/` folder: uuid-named files and directories, the dashed uuid
inside text files (same whole-token rule as `type: content`), every column of every `.db`/`.sqlite`
file in all three `format` encodings, and raw bytes in binary files. SQLite files are probed on a
**temporary copy**; the scanner never opens or writes live plugin data.

Each hit becomes a draft template (`<plugin>.auto.yml`) that parses like any hand-written one, with
deliberate limits:

- **Every draft is `enabled: false`.** A scan is a lead, not a verified template — review before
  enabling.
- **Content drafts list the exact files that matched** the probe. Other players' data may live in
  differently-named files, so widen `targets` to a directory + glob if that's the plugin's pattern.
- **SQL drafts' `format` still needs a human eye**: the probe found *a* matching column, but confirm
  the table really keys player data by it (the `SELECT ... LIMIT 1` check above).
- Binary files that contain the uuid but match no known layout are reported (`UNKNOWN_BINARY`) with
  **no draft** — that's jar-module territory (below).

---

## Custom module types via SPI

If a plugin stores UUIDs in a format none of the five built-ins cover (an exotic binary file, a
proprietary store), you don't need to fork the core — ship a **jar module**.

1. Implement `it.albemiglio.accounts.core.modules.ModuleProvider`:

   ```java
   public final class MyProvider implements ModuleProvider {
       @Override
       public Collection<Module> modules() {
           return List.of(new MyCustomModule(...));   // your own Module subclass
       }
   }
   ```

2. Declare it for `ServiceLoader`, in
   `META-INF/services/it.albemiglio.accounts.core.modules.ModuleProvider`:

   ```
   com.example.MyProvider
   ```

3. Build the jar and drop it in **`plugins/Accounts/jar-modules/`**. The engine discovers every provider
   on that folder's jars via `ServiceLoader`, calls `modules()`, and adds whatever it returns — including
   `Module` subclasses with completely custom storage logic.

Your `Module.execute(Pair<UUID, UUID>)` does the actual rewrite; extend `Module` and use the same
transaction discipline the built-ins do.

---

## Validation

Every file in `available-modules/` is covered by `AvailableModulesTemplatesTest`, which loads each one
through the real factory — so a malformed template fails the build. If you contribute a template, that
test keeps it honest. See [CONTRIBUTING.md](../CONTRIBUTING.md).
