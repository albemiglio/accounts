# Quickstart example: one real migration, end to end

A player joined your cracked (offline-mode) server as `Steve` and got the offline UUID
`069a79f4-44e9-4726-a5be-fca90e38aaf5`. They've now bought the game, and you're moving them to their
premium UUID `c06f8906-4c8a-4911-9c29-ea1dbd1aab82`. You want their LuckPerms rank, their EssentialsX
home, and their tamed wolf in the world to come with them.

This walks the whole thing on a single Spigot/Paper server. The multi-server case is the same command —
the engine fans it out for you (see [multi-instance setup](multi-instance-redis.md)).

> **Before you start: take a backup.** This rewrites live player data in place. Stop the server, copy the
> world folder and the plugin databases, then proceed on the copy first if you can.

## 1. Build and install

```bash
git clone https://github.com/albemiglio/accounts.git
cd accounts
mvn -q clean package
cp spigot/target/accounts-spigot-*.jar /path/to/server/plugins/
```

Start the server once so the plugin generates its data folder, then stop it. You now have
`plugins/Accounts/config.yml` and an empty `plugins/Accounts/modules/` folder.

## 2. Point at Redis

Install and start Redis (`localhost:6379` is fine for one server), then check
`plugins/Accounts/config.yml`:

```yaml
redis:
  host: localhost
  port: 6379
  password: ""
modules-dir: modules
```

Redis is required even here — it's where the migration is recorded and tracked.

## 3. Add the three modules

Copy the shipped templates into the modules folder:

```bash
cd /path/to/server
cp /path/to/accounts/available-modules/luckperms.yml    plugins/Accounts/modules/
cp /path/to/accounts/available-modules/essentialsx.yml  plugins/Accounts/modules/
cp /path/to/accounts/available-modules/world.yml        plugins/Accounts/modules/
```

**LuckPerms** (`plugins/Accounts/modules/luckperms.yml`) — verify the DB path matches yours:

```yaml
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

**EssentialsX** (`plugins/Accounts/modules/essentialsx.yml`) — a file rename, nothing to configure:

```yaml
name: essentialsx
platform: SPIGOT
type: file
directory: plugins/Essentials/userdata
extension: yml
enabled: true
```

**World** (`plugins/Accounts/modules/world.yml`) — point at your overworld folder:

```yaml
name: world
platform: SPIGOT
type: world
directory: world
enabled: true
```

> Got a Nether and End too? Copy `world.yml` to `world_nether.yml` / `world_the_end.yml`, change `name`
> and `directory`. One file per world.

### Verify the encoding (the step people skip)

Before running, confirm LuckPerms really stores a dashed UUID:

```bash
sqlite3 plugins/LuckPerms/luckperms-sqlite.db "SELECT uuid FROM luckperms_players LIMIT 1;"
# 069a79f4-44e9-4726-a5be-fca90e38aaf5   ← dashed, so the template's default is correct
```

If it had come back as 32 hex chars with no hyphens, you'd add `format: undashed` to those replacers. If
it were a `BINARY(16)` column, `format: binary`. Running the wrong encoding migrates nothing, silently.

## 4. Run the migration

Start the server. Then, as an operator (the command needs the `accounts.migrate` permission, default op):

```
/accounts migrate 069a79f4-44e9-4726-a5be-fca90e38aaf5 c06f8906-4c8a-4911-9c29-ea1dbd1aab82 Steve
```

What happens, in order:

1. The migration is recorded in Redis under the id
   `069a79f4-44e9-4726-a5be-fca90e38aaf5>c06f8906-4c8a-4911-9c29-ea1dbd1aab82`.
2. **LuckPerms** — `UPDATE luckperms_players SET uuid = '<new>' WHERE uuid = '<old>'`, and the same on
   `luckperms_user_permissions`, in one transaction. Rank and permissions now sit under the premium UUID.
3. **EssentialsX** — `userdata/069a79f4-...-aaf5.yml` is renamed to `userdata/c06f8906-...-ab82.yml`. The
   home, money, and kits inside come along untouched.
4. **World** — the NBT rewriter walks the world's region and `.dat` files and rewrites every reference to
   the old UUID — including the wolf's `Owner`. The Spigot plugin also fixes the loaded copy in memory so
   the next save doesn't undo it.
5. On a network, the same migration is broadcast to every other server, which applies steps 2–4 for its
   own data.

## 5. Confirm it landed

The data is "complete" once every live instance has applied it. On one server that's immediate; on a
network the login gate uses it. Spot-check:

```bash
sqlite3 plugins/LuckPerms/luckperms-sqlite.db \
  "SELECT uuid FROM luckperms_players WHERE uuid = 'c06f8906-4c8a-4911-9c29-ea1dbd1aab82';"
# one row → rank moved

ls plugins/Essentials/userdata/c06f8906-4c8a-4911-9c29-ea1dbd1aab82.yml   # exists → home moved
```

Log in with the premium account: same rank, same home, same wolf following you. Steve is Steve again —
just with the right UUID this time.

## Where to go next

- [How the engine works](how-the-engine-works.md) — what "durable / idempotent / atomic / complete"
  actually guarantee.
- [Writing a module template](writing-a-module-template.md) — add a plugin we don't ship yet.
- [Multi-instance (Redis) setup](multi-instance-redis.md) — run this across a whole network with a login
  gate.
