# The plugin-template catalog

These are the ready-made templates shipped in [`available-modules/`](../available-modules/). Each is a
starting point: copy it into `plugins/Accounts/modules/`, point it at *your* database/folder, and
**verify the assumed UUID encoding against a real row before running on live data**.

Looking for a quick ✅/❌ answer instead ("is my plugin covered?") — see the
[plugin compatibility matrix](plugin-compatibility.md), which also lists the plugins that are known
NOT to migrate yet and why.

Every shipped template carries a comment block stating the plugin version and **where its schema was
confirmed** — read it. That provenance is the whole point: a template you can't trust is worse than none.

## How to use one

```bash
cp available-modules/luckperms.yml  plugins/Accounts/modules/luckperms.yml
# edit the database path / connection, confirm the encoding, then restart and migrate
```

## The catalog

| Template | Type | What it touches | UUID encoding | Schema confirmed against |
|----------|:----:|-----------------|---------------|--------------------------|
| `luckperms.yml` | sql | `luckperms_players`, `luckperms_user_permissions` (`uuid`) | dashed | LuckPerms 5.x |
| `essentialsx.yml` | file | `plugins/Essentials/userdata/<uuid>.yml` (rename) | filename | userdata layout |
| `coreprotect.yml` | sql | `co_user`, `co_username_log` (`uuid`) | dashed | `Database.java`, CoreProtect v23.x |
| `towny-sql.yml` | sql | `TOWNY_RESIDENTS` (`uuid`) | dashed | `TownySQLSource.java`, Towny 0.100.x+ |
| `towny-flatfile.yml` | file | `plugins/Towny/data/residents/<uuid>.txt` (rename) | filename | `TownyFlatFileSource.java` |
| `griefprevention.yml` | sql | `griefprevention_playerdata` (`name`), `griefprevention_claimdata` (`owner`) | dashed | `DatabaseDataStore.java` |
| `playerpoints.yml` | sql | `playerpoints_points`, `playerpoints_username_cache` (`uuid`) | dashed | `_1_Create_Tables.java` |
| `mcmmo.yml` | sql | `mcmmo_users` (`uuid`) | dashed | `SQLDatabaseManager.java`, mcMMO master |
| `jobs.yml` | sql | `jobs_users` (`player_uuid`) | dashed | `JobsDAO.java`, Jobs master |
| `quickshop-hikari.yml` | sql | `qs_data` (`owner`) | dashed | `DataTables.java`/`QUserImpl.java`, hikari |
| `cmi.yml` | sql | `users` (`player_uuid`) | **TEXT — unverified** | CMI runtime SQL (issue #1306) |
| `world.yml` | world | a world folder's NBT (pets, heads, boss-bars, ...) | NBT, all 3 forms | — |
| `vanilla-json.yml` | json | `ops.json`, `whitelist.json`, `banned-players.json`, `usercache.json` | — | — |
| `huskhomes.yml` | sql | `huskhomes_users/-cooldowns/-teleports/-homes` | dashed | `{sqlite,mysql}_schema.sql`, HuskHomes 4.x |
| `plotsquared.yml` | sql | `plot`, helpers/trusted/denied, clusters, `player_meta` (9 cols) | dashed | `SQLManager.java`, PlotSquared 7.5 |
| `plotsquared-usercache.yml` | sql | `usercache` in `user_cache.db` | dashed | `SQLiteUUIDService.java` |
| `xconomy.yml` | sql | `xconomy` (`UID`) + optional uuid/login/record tables | dashed | `SQL.java`, XConomy master |
| `superiorskyblock2.yml` | sql | 12 player columns across players/islands/bank tables | dashed | `SQLDatabase.java` + `replacePlayer()` |
| `chestshop.yml` | sql | `accounts` in `users.db` | dashed | `Account.java`/`DaoCreator.java` |
| `worldguard-profilecache.yml` | sql | `uuid_cache` in `cache/profiles.sqlite` | dashed | `WorldGuard.java` + SquirrelID |
| `worldguard-sql-regions.yml` | sql | `user` (rare, deprecated MySQL region store) | dashed | `V2__Bug_fix_and_UUID.sql` |
| `residence-playerdata.yml` | file | `Save/PlayerData/<uuid>.yml` (rename) | filename | `PlayerManager.java`, Residence 6.x |
| `litebans.yml` | sql | bans/mutes/warnings/kicks/history (12 uuid cols) | dashed | official `test_setup.sql` (litebans-php) |
| `axvaults.yml` | sql | `axvaults_data` | dashed | `SQLite.java`/`MySQL.java` |
| `quests-yaml.yml` | file | `plugins/Quests/data/<uuid>.yml` (rename) | filename | `BukkitQuesterYamlStorage.java` |
| `quests-mysql.yml` | sql | 5 `quests_player*` tables | dashed | `BukkitQuesterSqlStorage.java` |
| `auraskills.yml` | sql | `auraskills_users`, `auraskills_logs` | dashed | `TableCreator.java` |
| `gravesx.yml` | sql | `grave` (`owner_uuid`, `killer_uuid`) | dashed | `DataManager.java` |
| `skinsrestorer.yml` | sql | `sr_players/-history/-favourites/-cooldowns` | dashed | `MySQLAdapter.java`, SkinsRestorer 15.x |
| `mmocore-yaml.yml` / `mmocore-mysql.yml` | file/sql | `userdata/<uuid>.yml` / `mmocore_playerdata` | dashed | MythicLib `YAMLFlatDatabase` + `SQLDatabaseImpl` |
| `mmoitems-yaml.yml` / `mmoitems-mysql.yml` | file/sql | `userdata/<uuid>.yml` / `mmoitems_playerdata` | dashed | MythicLib family storage |
| `mmoinventory-yaml.yml` | file | `userdata/<uuid>.yml` (rename) | filename | MMOInventory-API jar |

The batch-added templates (HuskHomes onward) carry their full provenance, caveats and "server stopped"
requirements in their own header comments — read the file before running it.

## Notes per template

- **LuckPerms** — SQLite migrates fine while the server runs; for MySQL swap to the commented `database`
  block. Both keyed by a dashed UUID string.
- **EssentialsX** — pure file rename; works for any plugin that uses one `<uuid>.<ext>` file per player,
  just change `extension`.
- **CoreProtect** — only the two `uuid` columns need rewriting; the big logging tables reference
  `co_user.rowid`, not the UUID. The `co_` prefix is configurable — adjust if you changed it. Databases
  predating UUID support (pre-2014) have empty `uuid` — nothing to migrate.
- **Towny** — two backends, two templates. Use `towny-sql.yml` *or* `towny-flatfile.yml`, not both. The
  SQL table prefix defaults to `TOWNY_`; change the table name if you set a custom `SQL_prefix`.
- **GriefPrevention** — the UUID lives in columns literally named `name` (player data) and `owner`
  (claims), both dashed. Only the **MySQL** backend is templated — the flatfile player files have no
  extension, which the `file` type can't match.
- **PlayerPoints** — both tables key on `uuid VARCHAR(36)` (dashed). Table prefix defaults to
  `playerpoints_`.
- **mcMMO** — **MySQL only.** mcMMO has no SQLite backend, and its flatfile is one shared `mcmmo.users`
  with the UUID as a field per line (not one file per player), so nothing is renamable — migrate it on
  MySQL. The `mcmmo_` table prefix is configurable; adjust the table name if you changed it.
- **Jobs Reborn** — per-player UUID lives in `<prefix>users` (`player_uuid`, dashed); other tables join
  by integer `userid`, so the one column is enough. Prefix defaults to `jobs_`; SQLite file is
  `plugins/Jobs/jobs.sqlite.db`.
- **QuickShop-Hikari** — ownership is `data.owner` (dashed UUID), *not* on `shops` (which only foreign-
  keys into `data`). The template targets **MySQL** (table `qs_data`): the default H2 backend can't be
  migrated live anyway. The `owner` column also holds bracketed `[username]` virtual owners and the
  all-zero server UUID, but a UUID `UPDATE` only matches the real old UUID, so those rows are left alone.
- **CMI** — ⚠️ **unverified.** CMI is closed source; the table/column come from CMI's own runtime SQL in
  a server error log, but the encoding ("TEXT" — dashed or undashed?) couldn't be confirmed. **Check one
  row before use** and add `format: undashed` if needed. Don't run blind.
- **Vanilla world** — one file per world. See [Writing a module template](writing-a-module-template.md#type-world--minecraft-world-nbt)
  for what the NBT rewriter covers and the live-data note.
- **Vanilla JSON** — backend-side; `directory` is the server root.

## Don't see your plugin?

Most plugins are a few lines of YAML away. If you can find how it stores the player UUID (from its source,
schema, or one live row), see [Writing a module template](writing-a-module-template.md) — and consider
[contributing it back](../CONTRIBUTING.md#adding-a-plugin-template-the-common-case) so the next person
doesn't have to.
