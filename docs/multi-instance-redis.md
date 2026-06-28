# Multi-instance (Redis) setup

`accounts` coordinates migrations over **Redis**. This page covers why it's required, how to configure
it, the keys it uses, and how the cross-server completion barrier works.

## Why Redis is required (even for one server)

The engine's durability, idempotency, and cross-instance coordination all live in Redis:

- the **durable migration record** (so a crash mid-migration is replayed on boot),
- the **expected / applied / failed** sets per migration (the completion barrier),
- the **pub-sub channel** that broadcasts a migration to every other server,
- the **instance heartbeats** that say which servers are currently live.

`AccountsEngine.start()` always builds a Redis connection pool — **there is no Redis-free mode**, even for
a single Spigot server. If that's a dealbreaker for a single-server setup, it's a known design choice:
the same code path runs whether you have one server or twenty, which is what keeps the multi-server case
correct.

## Configuration

Every platform (`spigot`, `bungee`, `velocity`) reads the same block from its `config.yml`:

```yaml
redis:
  host: localhost
  port: 6379
  password: ""        # leave empty for no auth; set it if your Redis requires AUTH

modules-dir: modules  # folder (under the plugin's data dir) holding your module YAMLs
```

Point **every instance on the network at the same Redis**. That shared Redis is what lets a migration run
on the proxy and land on each backend.

## Instance identity

Each instance has a stable **instance id**, persisted to a file so it survives restarts (so a rebooted
server is recognised as the same instance, not a new one). On `start()` the instance registers itself and
begins heartbeating every 10 seconds.

## The keys it uses

| Redis key / channel | Type | Purpose |
|---------------------|------|---------|
| `accounts:migrations` | hash | the durable migration record, keyed by migration id (`from>to`) |
| `accounts:expected:<id>` | set | instance ids that were live when the migration started (the bar for "complete") |
| `accounts:applied:<id>` | set | instance ids that have successfully applied the migration |
| `accounts:failed:<id>` | set | instance ids whose modules failed (will be retried) |
| `accounts:broadcast` | pub-sub channel | a published migration that every subscriber applies |
| `accounts:instances` | sorted set | live instances, scored by last-heartbeat timestamp (30s liveness window) |

(Key names reflect the current implementation; treat them as internal — interact via the engine, not by
poking Redis directly.)

## The completion barrier across servers

This is the mechanism that makes a safe login gate possible on a network.

1. When a migration starts, the initiating instance snapshots the **live** instances (heartbeated within
   the last 30 seconds) into the migration's **expected** set.
2. Each instance that applies the migration adds itself to the **applied** set.
3. `isComplete(from, to)` is true **only when applied ⊇ expected** (and expected is non-empty).
   `isInProgress(from, to)` is its inverse.

A plugin like [Nyx](https://github.com/albemiglio/Nyx) calls `isMigrationInProgress(from, to)` on login
and refuses the player while it's true — so they can't rejoin on a server that hasn't received their data
yet. The gate opens automatically once every expected instance has applied.

### What happens if a server is down

An instance that hasn't heartbeated for 30 seconds **drops out of the live set**, so it won't be added to
a new migration's expected set, and the barrier won't wait on it. When it comes back, `recoverPending()`
makes it apply every migration it missed while down — so its data converges even though it wasn't part of
the original expected set.

## Sizing and security

- **One Redis, shared by all instances.** It only stores small coordination records and pub-sub messages
  — it is not in the data path of the actual rewrites (those go straight to each plugin's database/files).
  A modest Redis is plenty.
- **Lock it down.** The engine trusts the migrations it reads from Redis, so a writable Redis is a way to
  trigger migrations. Bind it to localhost or a private network and set `redis.password`. See
  [SECURITY.md](../SECURITY.md).

## Single-server checklist

Even on one Spigot server you still need Redis:

```yaml
redis:
  host: localhost
  port: 6379
  password: ""
modules-dir: modules
```

Install Redis locally, start it, point the config at it, and you're set — the engine handles the rest.
