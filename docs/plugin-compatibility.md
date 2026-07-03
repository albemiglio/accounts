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
| [WorldGuard](https://enginehub.org/worldguard) | ✅ | YAML region files (owners/members) + profile cache + (rare) SQL region store | run with the server stopped |
| [Residence](https://github.com/Zrips/Residence) | ✅ | PlayerData files + all Save data (owners, trust keys, rent, permlists, shop votes, backups) | run with the server stopped |
| [AxVaults](https://github.com/Artillex-Studios/AxVaults) | ✅ | **default H2** (version-pinned 2.1.214) + SQLite/MySQL | run with the server stopped |
| [GravesX](https://github.com/legoman99573/GravesX) | ✅ | **default H2** (version-pinned 2.4.240) + MySQL/MariaDB + legacy SQLite | run with the server stopped |
| [DeluxeTags](https://github.com/HelpChat/DeluxeTags) | ✅ | player_tags.yml (uuid keys) | — |
| [BigDoors](https://github.com/PimvanderLoos/BigDoors) | ✅ | SQLite (single uuid column, cascading ids) | — |
| [TradeSystem](https://github.com/CodingAir/TradeSystem) | ✅ | trade-log SQLite/MySQL (uuid table; log itself is name-keyed) | — |
| [BetterEnderChest](https://github.com/rutgerkok/BetterEnderChest) | ✅ | .dat rename **and** MySQL (per world group) | name-mode installs (useUUIDs: false) |
| [CustomFishing](https://github.com/Xiao-MoMi/Custom-Fishing) | ✅ | **default H2** (pinned 2.4.240) + SQLite/MySQL + YAML/JSON renames | MongoDB backend |
| [Shopkeepers](https://github.com/Shopkeepers/Shopkeepers) | ✅ | save.yml owners/members + CSV trade logs (SQLite log documented) | — |
| [Typewriter](https://github.com/gabber235/Typewriter) | ✅ | facts.json (the sole player store) | — |
| [VoteParty](https://github.com/darbyjack/VoteParty) | ✅ | players/*.json (rename+content pair) + party voter cache | — |
| [FastAsyncWorldEdit](https://github.com/IntellectualSites/FastAsyncWorldEdit) | ✅ | clipboards, sessions, rollback DB (binary uuid) + per-uuid history dirs | copy history modules per world |
| [BodyHealth](https://modrinth.com/plugin/bodyhealth) | ✅ | SQLite (live-install verified) | — |
| [RealisticSeasons](https://www.spigotmc.org/resources/realisticseasons.93275/) | ✅ | data.yml uuid entries (live-install verified) | closed source: shape from a live install |
| [TAB](https://github.com/NEZNAMY/TAB) | ✅ | users.yml / playerdata.yml uuid keys | — |
| [BetterRTP](https://github.com/SuperRonanCraft/BetterRTP) | ✅ | SQLite cooldowns (Players) | per-world tables are named after worlds — add manually |
| [AdvancedKits](https://www.spigotmc.org/resources/advancedkits.90054/) | ✅ | default YAML playerdata (rename; + the legacy Reloaded fork) | undocumented SQL backends (author says use YAML) |
| [AdvancedCrates](https://advancedplugins.net/item/AdvancedCrates.9) | ✅ | virtualKeys.yml (uuid map keys; decompile+live verified) | — |
| [MythicDungeons](https://git.mythiccraft.io/mythiccraft/MythicDungeons) | ✅ | global + per-dungeon playerdata (3-module set, decompile-verified) | copy the per-dungeon module per dungeon |
| [ClothesPlus](https://www.spigotmc.org/resources/clothes.44992/) | ✅ | per-uuid directories (live-install observed) | closed source: shapes from a live install |
| [MythicMobs](https://mythiccraft.io/) | ✅ | data/players/*.json (rename+embedded-uniqueId pair, live-verified) | — |
| [FeatherBoard](https://www.spigotmc.org/resources/featherboard.2691/) | ✅ | SQLite players table (live-verified) | — |
| [WeaponMechanics](https://github.com/WeaponMechanics/MechanicsMain) | ✅ | SQLite player/weapon stats (live-verified) | — |
| [ShopGUIPlus](https://www.spigotmc.org/resources/shopgui.6515/) | ✅ | SQLite players (price modifiers; live-verified) | — |
| [TabTPS](https://github.com/jpenilla/TabTPS) | ✅ | userdata/*.json rename (live-verified) | — |
| [DeathMessages-Modern](https://www.spigotmc.org/resources/deathmessages.86894/) | ✅ | UserData.yml uuid keys (live-verified) | — |
| [CommandPanels](https://github.com/rockyhawk64/CommandPanels) | ✅ | data.yml player-input uuid keys (live-verified) | — |
| [MobWave](https://www.spigotmc.org/) | ✅ | data.yml uuid keys (live-verified) | — |
| [Vanilla server](https://www.minecraft.net/) | ✅ | ops/whitelist/bans/usercache JSON + full world NBT | — |

## Partially supported

| Plugin | Status | Covered | Waiting on |
|--------|:------:|---------|-----------|
| [mcMMO](https://github.com/mcMMO-Dev/mcMMO) | ⚠️ | MySQL | flatfile is one shared users file |
| [GriefPrevention](https://github.com/GriefPrevention/GriefPrevention) | ⚠️ | MySQL | flatfile player files have no extension |
| [QuickShop-Hikari](https://github.com/QuickShop-Community/QuickShop-Hikari) | ⚠️ | MySQL | default H2 backend |
| [CMI](https://www.zrips.net/cmi/) | ⚠️ | SQL template (closed source) | uuid encoding unverified — check one row first |
| [Maintenance](https://github.com/kennytv/Maintenance) | ⚠️ | WhitelistedPlayers.yml (uuid keys, all platform paths) | proxies with Maintenance's Redis sync read the whitelist from Redis instead |
| [LiteBans](https://www.spigotmc.org/resources/litebans.3715/) | ⚠️ | MySQL/MariaDB (12 columns, official schema); H2 template ready | its exact bundled H2 version is unverifiable (closed jar) — the engine's header sniff protects you; PostgreSQL |
| [AuraSkills](https://github.com/Archy-X/AuraSkills) | ✅ | MySQL **and** the default YAML backend (rename + embedded-uuid rewrite pair) | run with the server stopped |
| [SkinsRestorer](https://github.com/SkinsRestorer/SkinsRestorer) | ✅ | MySQL/MariaDB **and** the default FILE backend (pair); skin cache correctly untouched | cooldown filenames stay old (they expire) |
| [MMOCore](https://gitlab.com/phoenix-dvpmt/mmocore) | ⚠️ | YAML backend fully (userdata + friends + guilds); MySQL identity column | on MySQL the friends list sits in a LONGTEXT column |
| [MMOInventory](https://www.spigotmc.org/resources/mmoinventory.101946/) | ⚠️ | YAML backend | MySQL table name only ships in the premium jar |
| [MCPets](https://github.com/Nocsy-Workshop/mcpets) | ⚠️ | ownership/inventories/active pet (files **and** MySQL) | pet levels/XP live in Base64(JSON) blobs embedding the owner uuid |
| [KingdomsX](https://github.com/CryptoMorin/KingdomsX) | ⚠️ | JSON/YAML flat-file modes (player rename + embedded members/king/claims rewrite pair) | SQL modes incl. the **default H2**: member lists sit in JSON columns — switch to file mode first |
| [BattlePass](https://github.com/GC-spigot/battle-pass) | ⚠️ | JSON storage (rename+content pair; the default through 4.x) | 5.0 SQL: FKs without cascade + undocumented SQLite filename; legacy MySQL keeps the uuid inside a blob |
| [ItemsAdder](https://itemsadder.devs.beer/) | ⚠️ | player stats .nbt files (docs + live-install verified: uuid only in the filename) | emote-unlock persistence undocumented (typically permission-side) |
| [GriefDefender](https://github.com/bloodmc/GriefDefenderAPI) | ⚠️ | file storage fully: extensionless playerdata rename + claim HOCON content rewrite | SQL storage-method undocumented (closed jar) |

## Not supported yet

| Plugin | Status | Blocker |
|--------|:------:|---------|
| [HuskSync](https://github.com/WiIIiam278/HuskSync) | ❌ | its FK has no ON UPDATE CASCADE, so the two UPDATEs fail in either order; needs engine support for disabling FK checks during the module |
| [Lands](https://www.spigotmc.org/resources/lands.53313/) | ❌ | closed source, schema not officially documented — inspect your own DB (or wait for the scanner) |
| [PlayerAuctions](https://www.spigotmc.org/resources/playerauctions.20055/) | ❌ | closed source, only the table prefix is documented — inspect your own DB (or wait for the scanner) |
| [MMOProfiles](https://phoenixdevt.fr/) | ❌ | closed source; also re-keys the whole MMO family by profile uuid — do not run the MMO templates with it installed |
| [AuxProtect](https://github.com/Heliosares/AuxProtect) | ❌ | stores `$`-prefixed uuids plus a derived Java-hashCode column that must be co-updated — needs two engine features; a naive update breaks the plugin |
| [ajLeaderboards](https://github.com/ajgeiss0702/ajLeaderboards) | ❌ | one table per board (dynamic names) — needs engine-side table enumeration; default H2 2.1.214. Rows are only partially rebuildable: do not just delete |

## No player data (verified — nothing to migrate)

[CustomCrops](https://github.com/Xiao-MoMi/Custom-Crops) (world/chunk-keyed only; farming XP lives in
skill plugins) · [Simple Voice Chat](https://github.com/henkelmax/simple-voice-chat) (groups and
states are in-memory; per-player volumes live on each player's own client) — verified against source,
not assumed.

Additionally verified on a real production network (data folders inspected, nothing player-keyed
found): ProtocolLib, packetevents, PlaceholderAPI, item-nbt-api, Vault/VaultUnlocked, spark, mclogs,
ViaVersion, FreedomChat, FancyHolograms, ModelEngine/ModelAnimator, LevelledMobs, Citizens (NPC data
only), Chunky/ChunkyBorder, VoidGen, Multiverse-Core, WorldEditSUI, minimotd, ServerUtils, PlugManX,
JumpPads, LPC, ChangeSlots, EntityDetection, Corpse, Elevator, HeadDatabase, tooltips, Masks,
Vivecraft, BlockRegen, Craftorithm, CustomCrafting, Codex, ChatFilter, AntiNetherRoof,
custom-ore-generator, MobFarmManager, PetNameFix, SellGUI, TwitchLiveAnnouncer, LibsDisguises,
Nexo (item/glyph configs), nuvotifier/Votifier (no offline queue), tebex (no on-disk queue),
ajQueue(+Plus), unifiedmetrics, autoannouncements, WLib, wolfyutils, SmartInvs, BlueSlimeCore,
LoneLibs, MechanicsCore, NashornJs, mcMMO-style libs. **Name-keyed (a uuid migration doesn't touch
them; only nickname changes would):** AuthMe (external SQL, username column, no uuid), AdvancedReplay
(recordings named by username), RPCorpse, SignShop, AxTrade logs, CustomScreenMenu.

## Recurring blockers (what unlocks the ❌ rows)

1. **H2 default backends** — now supported: each module pins the exact H2 version its plugin bundles
   (`h2-version`), the engine fetches that driver (H2 file formats are incompatible across versions)
   and sniffs the store header before touching the file. Requires the server stopped. Remaining edge:
   closed-source plugins whose bundled version can't be read (LiteBans — sniff still protects you).
2. **UUIDs inside structured content** — now covered by the `content` module type (whole-token dashed
   uuid swap inside text files, comments preserved), used by the WorldGuard/Residence/Maintenance/
   MMOCore templates. Still out of its reach: uuids inside SQL blob/JSON *columns* (KingdomsX members,
   MMOCore friends on MySQL) and binary formats.
3. **Closed source without schema docs** (Lands, PlayerAuctions, MMOProfiles) — verifying against a live
   install works today; an automatic scanner is planned.

Run every migration with the affected servers **stopped** unless a template's header says otherwise:
most plugins cache player data in memory and rewrite their files/rows on autosave, clobbering external
edits.

Don't see your plugin? See [Writing a module template](writing-a-module-template.md) and
[contribute it back](../CONTRIBUTING.md#adding-a-plugin-template-the-common-case).
