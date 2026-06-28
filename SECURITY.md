# Security Policy

## Reporting a vulnerability

Please **do not open a public issue** for a security vulnerability.

Report it privately to **dev@albemiglio.it**, or use GitHub's
[private vulnerability reporting](https://github.com/albemiglio/accounts/security/advisories/new) on this
repository. Include:

- a description of the issue and its impact,
- the affected version / commit,
- steps to reproduce, if you have them.

You'll get an acknowledgement, and we'll work with you on a fix and disclosure timeline before anything
is made public.

## What's in scope

`accounts` rewrites live player data (databases, files, world NBT) and coordinates over Redis. Things we
especially want to hear about:

- A way to make a migration touch data, a database, a file, or a path it wasn't configured for
  (e.g. a malicious module YAML or jar reaching outside its declared directory).
- A way for an untrusted Redis message to trigger or corrupt a migration on a listening instance.
- SQL injection or path traversal through a template's table/column/directory values.
- A jar module (`ModuleProvider` SPI) being loaded from somewhere other than the configured
  `jar-modules` folder.

## Operational notes (not vulnerabilities, but read these)

- The `/accounts migrate` command is gated behind the `accounts.migrate` permission (default: op).
  Don't grant it more widely — it rewrites player identity network-wide.
- **Module templates and jar modules are trusted input.** A template points the engine at a database or
  directory; a jar module runs arbitrary code. Only install templates and jars you trust, exactly as you
  would any other server plugin.
- **Secure your Redis instance.** The engine trusts the migrations it reads from Redis. Bind it to
  localhost or a private network and set a password (`redis.password`).
- Always back up before a migration. A correctness bug that loses data is treated with the same urgency
  as a security bug — report it the same way.
