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

### Database notes

- **SQLite** migrates fine while the server is running.
- **MySQL / MariaDB** — point `database` at the same DB the plugin uses.
- **H2 is not supported.** The owning plugin holds an exclusive lock on the `.mv.db` file, so it can't be
  migrated live. Configure that plugin to use SQLite or MySQL instead. The engine throws a clear error if
  you try (`type: h2`).

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
