# How the migration engine works

This is the engine that turns one `/accounts migrate <from> <to>` into a rewrite of a player's identity
across every plugin and every server on your network — reliably. This page explains what each guarantee
("durable", "idempotent", "atomic per database", "complete") actually means, and what it doesn't.

## The one-line story

> Record the migration durably → apply it locally inside a transaction → broadcast it to every other
> instance → each instance applies it exactly once → it's "complete" only when every live instance has.

## The pieces

| Component | Job |
|-----------|-----|
| `AccountsEngine` | The single handle a platform plugin holds. `migrate()`, `isComplete()`, `isInProgress()`, `close()`. Built on enable. |
| `BroadcastMigrationService` | Records the migration, applies it locally, publishes it to peers. Recovers missed migrations on boot. |
| `InstanceMigrator` | Applies a migration's modules for *this* instance, exactly once. Catches every module failure. |
| `Module` (+ subclasses) | The storage adapters: SQL, file, world (NBT), JSON, jar (SPI). Each runs its work in a transaction. |
| Redis (`RedisMigration*`, `RedisInstanceRegistry`) | The shared, durable state: the migration record, the expected/applied/failed sets, the pub-sub channel, the instance heartbeats. |

## A migration, step by step

When `/accounts migrate <from> <to> [username]` runs (or another plugin calls `MigrationService.migrate`):

1. **Identify.** The migration gets a stable id: `from + ">" + to`, e.g.
   `069a79f4-...-aaf5>c06f8906-...-ab82`. Everything keys off this id.
2. **Snapshot who must apply it.** The service reads the set of currently-live instances (those that have
   heartbeated in the last 30 seconds) and records them as the migration's **expected** set. This is the
   bar for "complete" — the servers that were online when the migration started.
3. **Record it durably.** The task (from, to, username, failure count) is written to Redis *before* any
   data is touched. If this instance crashes mid-migration, the record survives and is replayed on boot.
4. **Apply locally.** `InstanceMigrator` runs every **enabled** module. Disabled modules are skipped.
5. **Broadcast.** The task is published on a Redis pub-sub channel. Every other instance's subscriber
   receives it and runs step 4 for itself.
6. **Mark applied.** When an instance finishes all its modules with no failure, it adds itself to the
   migration's **applied** set. If any module failed, it adds itself to the **failed** set instead, and
   the migration will be retried later.

## What each guarantee means

### Durable

The migration record is written to Redis before any data changes, and an instance, on startup, calls
`recoverPending()` — it asks Redis for every migration it hasn't yet marked applied and runs them. So a
server that was down for the migration, or crashed halfway, catches up automatically when it comes back.
**Durability lives in Redis**, which is why Redis is required.

### Idempotent

`InstanceMigrator.apply()` first checks "have I already applied this migration id?" (via the applied
log). If yes, it returns immediately and touches nothing. So replaying a migration — from a re-broadcast,
a recovery on boot, or a manual re-run — is safe. It never double-applies.

> **The honest edge.** Idempotency is at the *migration* granularity (this id, on this instance, already
> done?), not per-row. The individual SQL rewrite is naturally idempotent too — it's
> `UPDATE ... SET col = <new> WHERE col = <old>`, which finds nothing to change on a second run. But a
> module that *failed partway* and got retried relies on its own transaction (below) having rolled the
> partial work back, so the retry starts clean.

### Atomic per database

Each `Module.execute()` opens a connection, turns off auto-commit, runs all its replacers, and **commits
only if they all succeed** — on any `SQLException` it rolls the whole module back. So within one database,
a migration is all-or-nothing: you never get the rank table updated but the permissions table half-done.

> **The honest edge.** Atomicity is **per database/module, not across the whole network**. Two different
> databases are two transactions; a file rename and an SQL update are not a single atomic unit. If module
> A commits and module B then fails, A stays committed and B is recorded as failed and retried. The
> system converges (B will be reapplied), but there's a window where one store is migrated and another
> isn't. For player data this is the right trade-off — converge-and-retry beats blocking the whole
> network on a two-phase commit — but it's not a distributed transaction, and the docs won't pretend it is.

### Complete (the barrier)

`isComplete(from, to)` is true **only when the applied set contains every instance in the expected set**
(and the expected set is non-empty). `isInProgress(from, to)` is its inverse: expected is non-empty but
not everyone has applied yet.

This is what makes a safe login gate possible. A plugin like [Nyx](https://github.com/albemiglio/Nyx)
calls `isMigrationInProgress(from, to)` and refuses the player's login while it's true — so the player
can't rejoin and start playing on a server that hasn't received their data yet. Once every live instance
has applied, `isComplete` flips true and the gate opens.

Instances that go offline for more than 30 seconds drop out of the live set (their heartbeat expires), so
the barrier doesn't wait forever on a server that's gone — it waits for the servers that are actually up.

## Failure handling

A module failure — a `MigrationException` from a rolled-back transaction, or an unexpected driver/pool
error — is **caught, recorded, and never propagated**. The migrate call doesn't throw, the command
doesn't crash, the player isn't stuck. The instance marks itself failed for that migration, and the
recovery/retry path picks it up again later. Correctness is reached by convergence, not by getting it
right on the first try.

## Where to look in the code

- `core/.../services/AccountsEngine.java` — the handle, and how the stack is wired on `start()`.
- `core/.../services/BroadcastMigrationService.java` — record → apply → publish, and `recoverPending()`.
- `core/.../services/InstanceMigrator.java` — idempotency check, the per-module loop, failure recording,
  and `migrationId(from, to)`.
- `core/.../modules/Module.java` — the per-module transaction (`execute()`).
- `core/.../services/Redis*.java` — the durable store, publisher, subscriber, and instance registry.

See [Multi-instance (Redis) setup](multi-instance-redis.md) for the exact Redis keys and channels.
