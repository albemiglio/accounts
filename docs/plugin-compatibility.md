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
| VendingMachines | ✅ | `UserSavedPrices.yml` dashed-uuid keys (content) | placed-machine `OwnerName` is username-based (not a uuid — correctly untouched); schema from the decompiled jar |
| [ItemJoin](https://www.spigotmc.org/resources/itemjoin.12661/) | ✅ | SQLite (default) / MySQL: 9 `ij_*` tables, `Player_UUID` dashed | `ij_map_ids` (no uuid) skipped; usually empty in prod; verified vs decompiled ChaosCore + live db |
| [pvparena](https://www.spigotmc.org/resources/pvp-arena.14477/) | ✅ | SQLite (default) / MySQL: `pvparena_statistics.player_uuid` dashed | ORMLite build (not slipcor's players.yml); `arena_uuid` untouched; idle if `stats: false` |
| [PlayerVaultsX](https://github.com/drtshock/PlayerVaults) | ✅ | flatfile `newvaults/<uuid>.yml` rename (uuid in filename only) | legacy `uuidvaults` auto-migrated at boot; no SQL backend |
| [CrazyCrates](https://github.com/Crazy-Crew/CrazyCrates) | ✅ | `data.yml` dashed-uuid keys (content): virtual/offline keys, respins | legacy `Offline-Players.<name>` section is name-keyed (untouched) |
| [DiscordSRV](https://github.com/DiscordSRV/DiscordSRV) | ✅ | account links: `accounts.aof` `<discordId> <uuid>` (content, default) or `accounts.uuid` (opt-in MySQL) | dashed; deprecated `linkedaccounts.json` auto-imported at boot |
| [EliteMobs](https://github.com/MagmaGuy/EliteMobs) | ✅ | SQLite (default `player_data.db`) / MySQL: `PlayerData.PlayerUUID` dashed | quest/cooldown state in serialized-Java BLOB columns (opaque) |
| [Duels](https://github.com/Realizedd/Duels) | ✅ | `users/<uuid>.json` rename + embedded `uuid` (file+content pair) | match history is name-keyed; no SQL backend |
| [Parties](https://github.com/AlessioDP/Parties) | ✅ | **default H2** (pinned 1.4.200) / SQLite / MySQL: `parties_players.uuid` + `parties_parties.leader` | party ids (`id`/`party`) not re-keyed; members derived (no blob) |
| [AuctionHouse](https://github.com/kiranhart/Auction-House) | ✅ | SQLite (default) / MySQL: 18 seller/buyer/owner/bidder cols across 11 `auctionhouse_*` tables | item/currency blobs out of scope; `*_name` are cached usernames |
| [BetonQuest](https://github.com/BetonQuest/BetonQuest) | ✅ | SQLite (default) / MySQL: 10 `betonquest_*` columns (disable-foreign-key-checks), dashed | stock single-profile install; custom multi-profile providers need care |
| [VotingPlugin](https://github.com/BenCodez/VotingPlugin) | ✅ | SQLite (default `Users.db`/table `Users`) / MySQL (`VotingPlugin_Users`): `uuid` dashed | PostgreSQL backend uses a native UUID type (not text-swappable) |
| [Slimefun4](https://github.com/Slimefun/Slimefun4) | ✅ | flatfile `data-storage/Slimefun/Players/<uuid>.yml` + `waypoints/<uuid>.yml` rename | paths are server-root relative; researches/backpacks keyed internally, not by uuid |
| [PerWorldInventory](https://github.com/Gnat008/PerWorldInventory) | ✅ | per-uuid **directory** `data/<uuid>/` rename (empty extension) | flatfile only; the group `.json` inside hold no uuid |
| [PlayerParticles](https://github.com/Rosewood-Development/PlayerParticles) | ✅ | SQLite (default) / MySQL: `playerparticles_settings.player_uuid` + group/fixed `owner_uuid` | group/particle `.uuid` are random object ids (correctly excluded) |
| [CMI](https://www.zrips.net/cmi/) | ✅ | SQLite (default `cmi.sqlite.db`, table `users`) / MySQL (`CMI_users`): `player_uuid` dashed | verified: CFR-decompiled 9.8.8.3 + live MySQL; homes/economy/bans ride the same row |
| [GriefPrevention](https://github.com/GriefPrevention/GriefPrevention) | ✅ | MySQL **and** flatfile: extensionless `PlayerData/<uuid>` rename + `ClaimData/*.yml` owner/trust content | — |
| [Vanilla server](https://www.minecraft.net/) | ✅ | ops/whitelist/bans/usercache JSON + full world NBT | — |

## Partially supported

| Plugin | Status | Covered | Waiting on |
|--------|:------:|---------|-----------|
| [mcMMO](https://github.com/mcMMO-Dev/mcMMO) | ⚠️ | MySQL | flatfile is one shared users file |
| [QuickShop-Hikari](https://github.com/QuickShop-Community/QuickShop-Hikari) | ⚠️ | MySQL | default H2 backend |
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
| [Vulcan](https://www.spigotmc.org/resources/vulcan-anti-cheat-1-7-1-21-4.83626/) | ⚠️ | optional: `logs/punishments.txt` UUID lines (content) | violation levels are in-memory; `violations.txt` is name-keyed; the AC never reads the logs back by uuid — **not migration-critical**, the template is audit-trail only |
| [Lands](https://www.spigotmc.org/resources/lands.53313/) | ⚠️ | `lands_players.uuid` (dashed CHAR(36)) on the default SQLite **and** MySQL/PostgreSQL — verified against the decompiled jar v7.16.13 | land **owner + trusted** players aren't rows: they're the dashed-uuid JSON keys of the ULID-keyed `lands.members` blob (and per-area role JSON), so they stay on the old uuid until a content-rewrite module lands. Run with server stopped |
| [MMOProfiles](https://phoenixdevt.fr/) | ⚠️ | the real-uuid→profiles **index** (`plugins/MMOProfiles/userdata/<realUUID>.yml`, file rename) — this is the ONLY family module to run here | closed jar unobtainable, so the exact index path is open-layer-derived (**verify on your install**); keying is source-verified. With MMOProfiles active the MMO family (MMOItems/MMOCore/MMOInventory) is **profile-uuid-keyed** and survives the migration — **skip those modules here**; run them by real uuid only on NONE-mode servers |
| [BentoBox](https://github.com/BentoBoxWorld/BentoBox) | ⚠️ | default JSON: `database/Players/<uuid>.json` rename + content rewrite of the uuid inside Players/Island/Names docs | Island filename is an island id (content-only); the SQL backends store the whole doc in one `json` column — a JSON-blob edit, not covered. Verify the `members` map token on a sample first |
| [FactionsUUID](https://github.com/drtshock/Factions) | ⚠️ | default JSON: `data/players.json` (uuid keys) + `data/factions.json` (invites/claims) content swap | assumes the default JSON backend; a MySQL install needs a sql template. board.json has no player uuids |
| [Multiverse-Inventories](https://github.com/Multiverse/Multiverse-Inventories) | ⚠️ | only the global `players/<uuid>.json` + `playernames.json` index | **the per-world/group inventories are name-keyed, not uuid** — a uuid migration doesn't move them (they follow the in-game name) |
| [BeautyQuests](https://github.com/SkytAsul/BeautyQuests) | ⚠️ | default flatfile: `questers/00_index.yml` identifier (content) | identifier == uuid only without an account-linking hook; MySQL backend needs a sql template |
| [AngelChest](https://github.com/mfnalex/AngelChest) | ⚠️ | `angelchests/*.yml` `owner`/`killer` value (content) | filename is location-based (not renamed); harmless cosmetic name in it |
| [AdvancedBan](https://github.com/DevLeoko/AdvancedBan) | ⚠️ | MySQL only: Punishments/PunishmentHistory `uuid` **undashed** | default HSQLDB backend has no engine adapter; IP-ban rows skip automatically |
| [BreweryX](https://github.com/BreweryTeam/BreweryX) | ⚠️ | FlatFile `brewery-data.yml` `players.<uuid>` (content) | SQL/Mongo backends re-embed the uuid in a Base64-JSON blob (not covered) |
| [CoinsEngine](https://github.com/nulli0n/CoinsEngine-spigot) | ⚠️ | SQLite/MySQL: `economy_users.uuid` (currencies ride the same row) | rebranded to ExcellentEconomy (table/folder renamed); classic table is `coinsengine_users`; SQLite filename per-install |

## Not supported yet

| Plugin | Status | Blocker |
|--------|:------:|---------|
| [Heroes](https://www.spigotmc.org/resources/heroes.305/) | ❌ | the only open-source repo is an abandoned Sponge rewrite with an interface-only storage layer; the shipping Spigot plugin is closed source — needs its real DB schema or a flatfile sample to template |

Hit a plugin that isn't listed? Run the [scanner](scanning-a-plugin.md) on its database to generate a
draft module, then [diagnose](writing-a-module-template.md#dry-run-diagnose-before-you-migrate) it.

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
sessions), ProfileButtons (config-driven GUI; only a transient in-memory cooldown map, bundled HikariCP
is dead/unreferenced). **Shared libraries / utilities (no standalone player store):** CMILib, CMIEInjector, PowerLib
(powerlib-velocity/bukkit, Novaverse's shared lib), VPacketEvents, MythicLib (the MMO family's storage
layer — the family data itself is templated via MMOItems/MMOCore/MMOInventory), proxyrestart,
OfflineMaintenance, SimpleAutoRestart, MinecraftITALIA-Votifier-addon (no offline queue), GPS,
BigDoorsOpener, LM_Items (stateless item-provider bridge), MMOItemsFix (runtime listener-override hotfix,
no storage), Kingdoms-Addon-Outposts (own `outposts.yml` is name-keyed; its only player-uuid — a join-log
entry — rides in KingdomsX core's kingdom `logs` column, covered by the KingdomsX migration).
**[MoneyFromMobs](https://github.com/chocolf/MoneyFromMobs)**: money is Vault passthrough
and the mute toggle is transient; the only persisted uuid is a hopper-owner tag in **world PDC** → already
covered by the world/NBT module, no separate module needed (same for any Bukkit-PDC plugin). **Rpsize**
likewise stores only the player scale in the PDC (`rpsize:player_scale`, a DOUBLE in world playerdata) →
world-covered, no plugin store.
**Name-keyed (a uuid migration doesn't touch them; only nickname changes would):** AuthMe (external SQL,
username column, no uuid), AuthMeVelocity (proxy auth bridge, delegates to AuthMe), ThirstBar (players.db
keyed by name), AdvancedReplay (recordings named by username), RPCorpse, SignShop, AxTrade logs,
CustomScreenMenu. (Nyx itself is the migration engine, not a data plugin that gets migrated.)

## Recurring blockers (what unlocks the ❌ rows)

1. **H2 default backends** — now supported: each module pins the exact H2 version its plugin bundles
   (`h2-version`), the engine fetches that driver (H2 file formats are incompatible across versions)
   and sniffs the store header before touching the file. Requires the server stopped. Remaining edge:
   closed-source plugins whose bundled version can't be read (LiteBans — sniff still protects you).
2. **UUIDs inside structured content** — now covered by the `content` module type (whole-token dashed
   uuid swap inside text files, comments preserved), used by the WorldGuard/Residence/Maintenance/
   MMOCore templates. Still out of its reach: uuids inside SQL blob/JSON *columns* (KingdomsX members,
   MMOCore friends on MySQL) and binary formats.
3. **Closed source without schema docs** — decompiling the jar (static bytecode read, CFR) recovers the
   schema even when it's obfuscated: SQL string literals and file paths survive obfuscation. Lands
   (jar v7.16.13: `lands_players.uuid` dashed, owner/trust in a `members` blob) and MMOProfiles (keying
   verified from the open MythicLib/Profile-API layer; jar itself paywalled) were both unlocked this way.
   When a jar is unobtainable, the built-in scanner is the fallback: `/accounts scan <probe-uuid>` points
   at a player known to have data and drafts a disabled module for every place their uuid turns up
   (file/dir name, text content, or any SQLite column). See [Scanning a plugin](scanning-a-plugin.md).

Run every migration with the affected servers **stopped** unless a template's header says otherwise:
most plugins cache player data in memory and rewrite their files/rows on autosave, clobbering external
edits.

## Bedrock / Geyser players (Floodgate)

A Floodgate UUID (`00000000-0000-0000-XXXX-XXXXXXXXXXXX`, the XUID in the low 64 bits) is an ordinary
128-bit UUID, so **every template above already migrates Bedrock players** — no special handling. The one
Bedrock-specific case is **account linking**: while a Bedrock player is unlinked, their data is saved under
the Floodgate UUID; when they link, they start joining under their real **Java** UUID, and that old data is
stranded. The fix is a normal `migrate(floodgateUuid → javaUuid)` — the same operation `accounts` runs for a
cracked→premium upgrade. Floodgate has no link event to hook, so the **trigger** lives in the proxy layer
(Nyx detects the link at login and fires the migration through the Redis broadcast); `accounts` is the sink
and needs no new code. Pass the player's Java username **without** the Floodgate prefix (default `.`) so
name-keyed stores match. Floodgate's own `LinkedPlayers` table maps bedrock↔java and does not need re-keying.

Don't see your plugin? See [Writing a module template](writing-a-module-template.md) and
[contribute it back](../CONTRIBUTING.md#adding-a-plugin-template-the-common-case).
