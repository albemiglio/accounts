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
| [PlayerAuctions](https://www.spigotmc.org/resources/playerauctions.20055/) | ✅ | SQLite **or** MySQL (players + auctions + recents; live-install verified) | closed source: schema from a live install; bidder/target format inferred (verify one row); auction-item blobs untouched |
| [HuskSync](https://github.com/WiIIiam278/HuskSync) | ✅ | MySQL/MariaDB (disable-foreign-key-checks); husksync_users + husksync_user_data, dashed char(36) | v3 dropped SQLite; Postgres/Mongo need their own modules; uuids frozen in old snapshot blobs |
| [AuxProtect](https://github.com/Heliosares/AuxProtect) | ✅ | SQLite (default) + MySQL/MariaDB; `$`-prefixed uuid with the co-updated hashCode lookup column | event tables key by an int uid (untouched, correct) |
| [TicketManager](https://github.com/HoshiKurama/TicketManager) | ✅ | **default H2** (pinned 2.3.232); TICKETMANAGER_V10_TICKETS/_ACTIONS.CREATOR, `USER.`-prefixed dashed | ASSIGNED_TO/console are usernames/sentinels (correctly excluded); Fabric bundles 2.2.224 |
| [GSit](https://github.com/Gecolay/GSit) | ✅ | SQLite (default) / MySQL toggle store: gsit_sit/player/crawl_toggle.uuid | empty unless players used /sit toggle etc. |
| [CombatLogX](https://github.com/SirBlobman/CombatLogX) | ✅ | playerdata/`<uuid>`.data.yml rename (punish-count / toggles) | live combat tags are in-memory (nothing to migrate) |
| [Vehicles](https://www.spigotmc.org/resources/vehicles.90130/) | ✅ | per-model SQLite `databases/<Model>/database.db`, vehicle.owner dashed | one db file per model → copy the module per model; trunk blob out of scope |
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
| [BattlePass](https://github.com/GC-spigot/battle-pass) | ⚠️ | JSON storage (rename+content pair; the default through 4.x) | SQL backend (via simple-spigot) is one key-value table `battlePass-` (id VARCHAR(36) + json MEDIUMBLOB, dashed, no FK) — the uuid also sits inside the blob so a column re-key leaves it stale; the v5 "Improved" schema is in no public source |
| [ItemsAdder](https://itemsadder.devs.beer/) | ⚠️ | player stats .nbt files (docs + live-install verified: uuid only in the filename) | emote-unlock persistence undocumented (typically permission-side) |
| [GriefDefender](https://github.com/bloodmc/GriefDefenderAPI) | ⚠️ | file storage fully: extensionless playerdata rename + claim HOCON content rewrite | SQL storage-method undocumented (closed jar) |
| [ajLeaderboards](https://github.com/ajgeiss0702/ajLeaderboards) | ⚠️ | MySQL/MariaDB boards (table-pattern `ajlb_%`, uuid col `id`) + ajlb_extras | default H2/SQLite: boards have no common prefix + the file is locked — switch to MySQL first. Board `id` is a PK: clear a pre-existing dest row |

## Not supported yet

| Plugin | Status | Blocker |
|--------|:------:|---------|
| [Lands](https://www.spigotmc.org/resources/lands.53313/) | ❌ | closed source, schema not officially documented (sqlite-v2 default / mysql-v2, prefix `lands_`). Run the [scanner](scanning-a-plugin.md) on your own DB to generate a draft module |
| [MMOProfiles](https://phoenixdevt.fr/) | ❌ | closed source + maven now auth-gated, so the userdata filename/backend can't be verified (flat-YAML `plugins/MMOProfiles/userdata/<realUUID>.yml` derived from the open MythicLib layer — verify on your install). Also: with it installed the MMO family keys data by **profile** uuid, which *survives* a real-uuid migration — run the MMO family modules **only** for NONE-mode/default-profile players, never blanket |

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
LoneLibs, MechanicsCore, NashornJs, mcMMO-style libs, LimitCrafting (config + permissions only),
LockTheft (lock state in block/item NBT, keyed by a random lock id), BoostedAudio (ephemeral in-memory
sessions). **[MoneyFromMobs](https://github.com/chocolf/MoneyFromMobs)**: money is Vault passthrough and
the mute toggle is transient; the only persisted uuid is a hopper-owner tag in **world PDC** → already
covered by the world/NBT module, no separate module needed (same for any Bukkit-PDC plugin). **Name-keyed
(a uuid migration doesn't touch them; only nickname changes would):** AuthMe (external SQL, username
column, no uuid), AdvancedReplay (recordings named by username), RPCorpse, SignShop, AxTrade logs,
CustomScreenMenu.

## Recurring blockers (what unlocks the ❌ rows)

1. **H2 default backends** — now supported: each module pins the exact H2 version its plugin bundles
   (`h2-version`), the engine fetches that driver (H2 file formats are incompatible across versions)
   and sniffs the store header before touching the file. Requires the server stopped. Remaining edge:
   closed-source plugins whose bundled version can't be read (LiteBans — sniff still protects you).
2. **UUIDs inside structured content** — now covered by the `content` module type (whole-token dashed
   uuid swap inside text files, comments preserved), used by the WorldGuard/Residence/Maintenance/
   MMOCore templates. Still out of its reach: uuids inside SQL blob/JSON *columns* (KingdomsX members,
   MMOCore friends on MySQL) and binary formats.
3. **Closed source without schema docs** (Lands, MMOProfiles) — the built-in scanner handles these now:
   `/accounts scan <probe-uuid>` points at a player known to have data and drafts a disabled module for
   every place their uuid turns up (file/dir name, text content, or any SQLite column). See
   [Scanning a plugin](scanning-a-plugin.md). PlayerAuctions and others once here now ship real templates.

Run every migration with the affected servers **stopped** unless a template's header says otherwise:
most plugins cache player data in memory and rewrite their files/rows on autosave, clobbering external
edits.

Don't see your plugin? See [Writing a module template](writing-a-module-template.md) and
[contribute it back](../CONTRIBUTING.md#adding-a-plugin-template-the-common-case).
