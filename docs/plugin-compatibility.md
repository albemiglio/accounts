# Plugin compatibility matrix

Which plugins' player data follows a UUID migration today — including the ones that **can't** yet, and
why. Every ✅/⚠️ row has a ready-made template in [`available-modules/`](../available-modules/) (usage:
[the catalog](plugin-template-catalog.md)); every ❌ row states the exact blocker, so you know what
you're accepting before migrating a network that runs it.

**Legend** — ✅ supported (template ships) · ⚠️ partially supported (template ships; the stated part
doesn't migrate yet) · ❌ not supported yet (no template; blocker stated). "Verified against source"
means the schema was read from the plugin's own code or official schema docs, never guessed.

## Supported

| Plugin | Status | Covered | Not covered / caveat |
|--------|:------:|---------|----------------------|
| [LuckPerms](https://luckperms.net/) | ✅ | SQL (SQLite/MySQL) | — |
| [EssentialsX](https://essentialsx.net/) | ✅ | userdata files | — |
| [CoreProtect](https://coreprotect.net/) | ✅ | SQL (SQLite/MySQL) | pre-2014 rows have no uuid |
| [Towny](https://github.com/TownyAdvanced/Towny) | ✅ | SQL **or** flatfile residents | — |
| [Jobs Reborn](https://www.spigotmc.org/resources/jobs-reborn.4216/) | ✅ | SQL (SQLite/MySQL) | — |
| [PlayerPoints](https://www.spigotmc.org/resources/playerpoints.80745/) | ✅ | SQL | — |
| [Quests](https://github.com/PikaMug/Quests) | ✅ | YAML **and** MySQL backends | — |
| [PlotSquared](https://github.com/IntellectualSites/PlotSquared) | ✅ | main DB (9 columns) + usercache | backup dirs (ephemeral) |
| [XConomy](https://github.com/YiC200333/XConomy) | ✅ | SQL (SQLite/MySQL/MariaDB) | name-keyed non-player accounts untouched (correct) |
| [SuperiorSkyblock2](https://github.com/BG-Software-LLC/SuperiorSkyblock2) | ✅ | SQL, 12 columns (mirrors the plugin's own replacePlayer) | addon custom-data blobs |
| [ChestShop](https://github.com/ChestShop-authors/ChestShop-3) | ✅ | users.db (signs resolve by name → follow automatically) | name changes are a different problem |
| [HuskHomes](https://github.com/WiIIiam278/HuskHomes) | ✅ | SQLite/MySQL/MariaDB (4 tables) | H2/PostgreSQL backends |
| [MMOItems](https://gitlab.com/phoenix-dvpmt/mmoitems) | ✅ | YAML **and** MySQL backends | soulbound uuid inside item NBT (cosmetic) |
| [Vanilla server](https://www.minecraft.net/) | ✅ | ops/whitelist/bans/usercache JSON + full world NBT | — |

## Partially supported

| Plugin | Status | Covered | Waiting on |
|--------|:------:|---------|-----------|
| [mcMMO](https://github.com/mcMMO-Dev/mcMMO) | ⚠️ | MySQL | flatfile is one shared users file |
| [GriefPrevention](https://github.com/GriefPrevention/GriefPrevention) | ⚠️ | MySQL | flatfile player files have no extension |
| [QuickShop-Hikari](https://github.com/QuickShop-Community/QuickShop-Hikari) | ⚠️ | MySQL | default H2 backend |
| [CMI](https://www.zrips.net/cmi/) | ⚠️ | SQL template (closed source) | uuid encoding unverified — check one row first |
| [WorldGuard](https://enginehub.org/worldguard) | ⚠️ | profile cache + (rare) SQL region store | the YAML region files (owners/members lists) |
| [Residence](https://github.com/Zrips/Residence) | ⚠️ | PlayerData files | residence/rent/permlist data (uuids inside shared YAML) |
| [LiteBans](https://www.spigotmc.org/resources/litebans.3715/) | ⚠️ | MySQL/MariaDB (12 columns, official schema) | **default H2 backend**; PostgreSQL |
| [AxVaults](https://github.com/Artillex-Studios/AxVaults) | ⚠️ | SQLite/MySQL | **default H2 backend** |
| [GravesX](https://github.com/legoman99573/GravesX) | ⚠️ | MySQL/MariaDB + legacy SQLite | **default H2 backend** |
| [AuraSkills](https://github.com/Archy-X/AuraSkills) | ⚠️ | MySQL | default YAML backend embeds the uuid inside each file |
| [SkinsRestorer](https://github.com/SkinsRestorer/SkinsRestorer) | ⚠️ | MySQL/MariaDB (4 identity tables; skin cache correctly untouched) | default FILE backend embeds the uuid inside JSON |
| [MMOCore](https://gitlab.com/phoenix-dvpmt/mmocore) | ⚠️ | YAML and MySQL backends | friends lists + guild files reference other players' uuids |
| [MMOInventory](https://www.spigotmc.org/resources/mmoinventory.101946/) | ⚠️ | YAML backend | MySQL table name only ships in the premium jar |

## Not supported yet

| Plugin | Status | Blocker |
|--------|:------:|---------|
| [KingdomsX](https://github.com/CryptoMorin/KingdomsX) | ❌ | member lists live INSIDE JSON blobs even on SQL — rewriting only the uuid columns would desync kingdoms; needs the content-rewrite module. Default backend is H2 on top. |
| [HuskSync](https://github.com/WiIIiam278/HuskSync) | ❌ | its FK has no ON UPDATE CASCADE, so the two UPDATEs fail in either order; needs engine support for disabling FK checks during the module |
| [Maintenance](https://github.com/kennytv/Maintenance) | ❌ | whitelist is uuid-as-KEY inside one shared WhitelistedPlayers.yml (+ a Redis hash on 5.x proxies) |
| [GriefDefender](https://github.com/bloodmc/GriefDefenderAPI) | ❌ | playerdata files have no extension; claim files embed owner/trust uuids inside HOCON; SQL schema not public |
| [Lands](https://www.spigotmc.org/resources/lands.53313/) | ❌ | closed source, schema not officially documented — inspect your own DB (or wait for the scanner) |
| [PlayerAuctions](https://www.spigotmc.org/resources/playerauctions.20055/) | ❌ | closed source, only the table prefix is documented — inspect your own DB (or wait for the scanner) |
| [MMOProfiles](https://phoenixdevt.fr/) | ❌ | closed source; also re-keys the whole MMO family by profile uuid — do not run the MMO templates with it installed |

## Recurring blockers (what unlocks the ❌ rows)

1. **H2 default backends** (LiteBans, AxVaults, GravesX, KingdomsX, QuickShop-Hikari…) — H2 holds an
   exclusive file lock; the engine deliberately refuses it. Operators can switch the plugin to
   SQLite/MySQL first (most of these plugins ship converters).
2. **UUIDs inside structured content** (WorldGuard regions, Residence saves, Maintenance whitelist,
   KingdomsX blobs, AuraSkills/SkinsRestorer file backends, MMOCore friends/guilds) — one planned
   content-rewrite module type covers the whole class.
3. **Closed source without schema docs** (Lands, PlayerAuctions, MMOProfiles) — verifying against a live
   install works today; an automatic scanner is planned.

Run every migration with the affected servers **stopped** unless a template's header says otherwise:
most plugins cache player data in memory and rewrite their files/rows on autosave, clobbering external
edits.

Don't see your plugin? See [Writing a module template](writing-a-module-template.md) and
[contribute it back](../CONTRIBUTING.md#adding-a-plugin-template-the-common-case).
