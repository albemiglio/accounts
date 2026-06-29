# Live-test rig

A real, runnable end-to-end proof of the migration engine. It spins up a **real MySQL** and a **real
Redis** in Docker, seeds a LuckPerms-style schema and an EssentialsX-style userdata file keyed by an
**old** UUID, then runs a genuine migration through the engine's own code path and asserts the data is
now keyed by the **new** UUID and the old one is gone.

This is the proof that used to live only on the owner's VPS, made to travel with the code. CI (or anyone)
can run it.

## What it proves

A single command migrates one player from offline UUID `069a79f4-…-aaf5` to premium UUID
`c06f8906-…-ab82` across two backends at once, and verifies the result against the live stores:

- **SQL (LuckPerms on MySQL)** — the rows in `luckperms_players` and `luckperms_user_permissions` move
  from the old UUID to the new one, in one transaction per module (`ColumnReplacer` + `UuidCodec`).
- **Flat file (EssentialsX)** — `userdata/<old-uuid>.yml` is renamed to `userdata/<new-uuid>.yml`,
  contents untouched (`FileModule`).
- **Redis coordination** — the migration is driven through `BroadcastMigrationService.migrate(...)`:
  recorded in Redis, applied locally, and the completion barrier (`expected` vs `applied` instances) is
  asserted closed — the same path the admin command and Nyx use.

The harness builds the modules with the engine's real `YamlModuleFactory` from the same YAML shapes
shipped in [`available-modules/`](../available-modules), so a green run means the production code path
works on real data stores, not a mock.

## Run it (one command)

```bash
cd livetest
docker compose up -d && ./run.sh
```

`run.sh` builds the engine + harness (`mvn -Plive`), waits for MySQL and Redis to be healthy, seeds a
fresh EssentialsX fixture, runs the migration, and prints per-target `OK`/`FAIL`. It exits non-zero if
anything didn't migrate, so CI fails loudly.

Needs a JDK 17 to build (the project targets Java 8 bytecode). If your default `java` is older:

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 17) ./run.sh
```

### Offline variant (no Docker)

To prove the data-migration logic without Docker — the same engine path against a throwaway SQLite file
and an in-memory store:

```bash
cd livetest
LIVETEST_DB=sqlite ./run.sh
```

## Teardown

```bash
cd livetest
./run.sh --down        # docker compose down -v (removes the volumes too)
```

## How it's wired (and why it's opt-in)

- `docker-compose.yml` — pinned MySQL + Redis, healthchecks, published on non-default host ports
  (`33070`, `63790`) so they never clash with a MySQL/Redis you already run.
- `seed/luckperms.sql` — runs once on first MySQL init; creates the schema and the old-UUID rows.
- `seed/essentialsx-userdata.yml` — the userdata master `run.sh` copies to the old-UUID file each run.
- `modules/*.yml` — the same module config shapes the plugin uses, pointed at the dockerized backends.
- `src/main/java/.../LiveTestRunner.java` — the harness (modules → migrate → assert).

The harness lives in its own Maven module that is **only built under the `-Plive` profile**, declared in
the root `pom.xml`. The default `mvn` build never touches it and stays fully offline — the rig is opt-in.
