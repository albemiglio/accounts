# Scanning a plugin

Some plugins are closed source and have no schema docs, so there's no way to hand-write a
[module template](writing-a-module-template.md) for them without guessing. The scanner solves this the
empirical way: instead of reverse-engineering the storage, you point it at data a player is **known** to
have, and it reports every place that player's UUID actually shows up — then drafts a module from what it
found.

## Running it

On the backend that runs the plugin (a Spigot/Paper server with Accounts installed):

```
/accounts scan <probe-uuid>
```

The **probe** is a real player who is known to have data on this server (yourself, a staff member — anyone
who has actually used the plugin). The scan runs off the main thread and reports back in chat; drafts land
in `plugins/Accounts/scan-drafts/`.

## What it looks at

For every plugin **data folder** under `plugins/` it checks, for the probe UUID:

- **file / directory name** — `<uuid>.yml`, `<uuid>.dat`, a bare `<uuid>`, or a whole `<uuid>/` directory
  (EssentialsX-style stores) → drafts a `file` module.
- **text file content** — the dashed UUID as a whole token inside `.yml/.yaml/.json/.txt/.conf/.properties/.csv`
  (same whole-token rule as the `content` module, so a UUID that's a fragment of a longer id never matches)
  → drafts a `content` module.
- **SQLite columns** — every column of every `.db`/`.sqlite` file, tried in all three encodings
  (dashed, undashed, binary) → drafts a `sql` module with the exact table/column/format that matched.
- **unknown binary** — a non-text file that contains the raw UUID bytes. Reported as **evidence only**: no
  built-in module type can safely rewrite an unknown binary format, so no draft is written for it.

It is strictly **read-only** on plugin data. SQLite files are probed on a temp-directory *copy* (the owning
plugin may hold the live file open), and nothing under `plugins/` is ever written. You can scan a running
server; only the eventual migration needs the server stopped.

## The drafts are leads, not templates

Every generated `plugins/Accounts/scan-drafts/<plugin>.auto.yml` is written with **`enabled: false`** and a
header telling you to verify it. A scan tells you *where* a UUID is, not what the plugin *means* by it:

1. Read the draft. Confirm the table/column/format is the player's identity and not, say, a foreign key to
   something else, or a value the plugin also caches elsewhere.
2. Move it into your modules directory and flip `enabled: true`.
3. Run the migration with the server **stopped**.

Known limits (by design — they keep a lead from masquerading as a finished module):

- One draft per store: if a plugin has several `.db` files or several uuid-named directories, the draft
  covers the first; the rest are reported in chat so you can add them by hand.
- Only SQLite is probed among databases — an H2 or MySQL backend won't be found by a scan (open its DB
  yourself, or check whether a real template already ships).
- A UUID inside a blob/JSON *column* shows up as a binary or content hit at best; re-keying it may need the
  `content` module or may not be reachable at all (see the [compatibility matrix](plugin-compatibility.md)).
