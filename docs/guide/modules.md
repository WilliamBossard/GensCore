# In-Game Modules (29 Modules)

GensCore is built around a modular architecture managed by `ModuleManager`. Every module can be enabled or disabled dynamically without server restarts.

---

## 1. Death Graves (Tomb Module)
When a player dies, their items are preserved inside a physical grave block at the exact death coordinate.
- **Hologram Protection:** A floating display shows the owner name and protection countdown (e.g. 10 minutes).
- **Owner-Exclusive:** Only the victim can right-click the grave to reclaim all inventory and experience.
- **Admin Bypass:** Admins with `genscore.tomb.admin` can loot or unlock any grave.
- **Expiry:** Once the timer reaches 0, the grave can either be looted publicly or dropped into the world.

---

## 2. Lootr Instanced Chests
Every natural dungeon, fortress, and structure chest is instanced per-player:
- Each player sees their own fresh loot upon first opening.
- Prevents players from racing to empty dungeons on a newly opened world.
- Built-in hopper protection prevents draining items via automation.
- Chest break protection prevents destruction before loot is collected.

---

## 3. Dynamic Shop Algorithm
The `/shop` module dynamically calculates buying and selling prices based on market volume:

$$\text{Current Price} = \text{Base Price} \times \left(1 + \frac{\text{Purchases} - \text{Sales}}{\text{Threshold}}\right)^{\text{exponent}}$$

- High demand increases item prices; oversupply lowers prices.
- The `inflationExponent` can be configured via the Web Panel to balance your server economy.

---

## 4. Custom Spawners & Stacking
To eliminate entity lag on high-population servers:
- **Stacking:** Spawners merge into single blocks (e.g., `x15 Skeleton Spawner`).
- **Internal Storage:** Mob drops can be accumulated directly inside the spawner block; players open the GUI to collect loot in bulk.
- **Speed & XP Upgrades:** Increase spawn frequency and multiply experience drops.

---

## 5. Container Locks & Piston Griefing Shield
- Locks protect **Chests, Trapped Chests, Barrels, and Shulker Boxes**.
- **Anti-Piston Shield:** Pistons cannot move, retract, or crush locked containers.
- **Anti-Shulker Break:** Cancels piston shulker breaking contraptions, eliminating duplication exploits.

---

## 6. Bedrock & Floodgate Cross-Play
- Automatic detection of Bedrock players joining through Floodgate/Geyser.
- **Cumulus Forms:** In-game GUIs open as native Bedrock dialogs for mobile and console players, avoiding touch-screen desyncs.
- **Skin & Head API:** Renders authentic Bedrock player heads via the web API (`/api/head/{name}/{size}`).
- **Prefix Isolation:** Handles Bedrock prefixes (`.` or `*`) smoothly across all commands and LuckPerms lookups.

---

## 7. Jobs & Professions
6 diverse professions:
1. **Miner:** Mining ores, stone, deepslate.
2. **Lumberjack:** Chopping trees and logs.
3. **Hunter:** Killing monsters and hostile bosses.
4. **Farmer:** Harvesting wheat, potatoes, carrots, pumpkins.
5. **Fisherman:** Catching fish and treasure items.
6. **Builder:** Placing construction blocks.

Progress is saved asynchronously to SQLite every 60 seconds with 0 main-thread lag.

---

## 8. Fast Leaf Decay
Automatically detects broken tree trunks and triggers rapid, cascading decay of leaves without leaving floating treetops.

---

## 9. Head Drops
Configurable chance for mobs and defeated players to drop their heads with authentic skin textures upon death.

---

## 10. Modern Chat & MiniMessage
- Pure **Kyori Adventure / MiniMessage** formatting (no legacy `§` codes).
- Automatic LuckPerms prefix and suffix resolution.
- Discord synchronization and channel separation.

---

## 11. Guilds, Claims & Shared Treasury
A complete clan ecosystem supporting Folia multi-threading and Bedrock crossplay:
- **Hierarchy & Roles (Leader, Admin, Member):**
  - **Leader (`LEADER`):** Full ownership, admin promotions/demotions, disbanding the guild, treasury withdrawals, and territory claims.
  - **Administrator (`ADMIN`):** Co-management powers: invitations, kicking regular members, treasury withdrawals, land claims/unclaims, BlueMap color styling, and perk purchases.
  - **Member (`MEMBER`):** Depositing funds & XP, participating in weekly co-op quests, and accessing guild-locked storage containers.
  - *In-Game & Web Management:* In-game `/team` interface (left-click to promote/demote, right-click to kick) and Player Web Portal with confirmation modals.
- **Shared Treasury (Guild Bank):** Guild members deposit dollars (`/team deposit <amount>`) or XP levels (`/team depositxp <levels>`). When the Economy module is disabled, all transactions and perk upgrades automatically switch to player XP levels. Leaders and Admins can withdraw funds via `/team withdraw` or `/team withdrawxp`.
- **Territory Claims ($16 \times 16$ Chunks):** Guilds claim chunks via `/team claim`. Claims are strictly purchased using the guild bank balance ($1500 or 10 XP levels per chunk).
  - *Comprehensive Anti-Grief:* Total protection against block breaking/placing, opening containers (chests, barrels, furnaces, hoppers, shulkers), redstone interactions (doors, trapdoors, buttons, levers), entity/livestock damage, and piston griefing across chunk borders.
  - *On-Screen Boundary Notifications:* Animated Title and Subtitle with an immersive chime sound displayed when a player enters claimed guild territory.
- **Team Perks (10 Active Guild Upgrade Trees):** Permanent team perks funded strictly from the shared guild bank:
  1. *Max Members (`MAX_MEMBERS`, Tiers 1 to 3):* 8, 11, and 14 max members (base 5).
  2. *Extended Territory (`EXTENDED_TERRITORY`, Tiers 1 to 4):* 8, 12, 16, and 20 max claimed chunks (base 4).
  3. *Jobs Boost (`JOBS_BOOST`, Tiers 1 to 3):* +5%, +10%, and +15% jobs XP bonus for online members.
  4. *Auction House Tax Reduction (`AH_TAX_REDUCTION`, Tiers 1 to 2):* -25% and -50% tax cut on sales commission.
  5. *Co-op Quest Boost (`COOP_QUESTS_BOOST`, Tiers 1 to 2):* +10% and +20% guild quest progress points.
  6. *Guild Home Warp (`GUILD_HOME`, Tiers 1 to 3):* Shared teleportation waypoint (`/team home` and `/team sethome`). Reduced delays: 5s warmup and 15m cooldown at tier 1, 3s warmup and 5m cooldown at tier 2, instant warmup inside territory claims and 1m cooldown at tier 3.
  7. *Territory Buffs (`TERRITORY_BUFF`, Tiers 1 to 3):* Passive AoE potion effects radiating to members inside claimed chunks (Tier 1: Regeneration I and slow Saturation, Tier 2: Speed I, Tier 3: Haste I).
     - *Anti-Abuse Stabilization & Warmup:* To prevent nomadic claim-and-mine abuse and combat exploits, newly claimed chunks require a 5-minute chunk stabilization anchor (`CLAIM_ANCHOR_WARMUP_MS = 300_000L`) before radiating buffs (ActionBar countdown). A 15-second presence synchronization is required upon entering territory before buffs apply. Leaving guild territory immediately strips all active effects.
  8. *Daily Bank Interest (`BANK_INTEREST`, Tiers 1 to 2):* Passive interest dividends deposited into the guild treasury every 24 real-world hours (+1% daily capped at $10,000 or 50 XP at tier 1, +2% daily capped at $25,000 or 100 XP at tier 2).
  9. *Spawner Efficiency (`SPAWNER_EFFICIENCY`, Tiers 1 to 2):* Overclocks custom smart spawners placed within the guild's claims via `SpawnerManager.generateTick()` (+15% spawn speed at tier 1, +30% at tier 2).
  10. *Virtual Shared Vault (`GUILD_VAULT`, Tiers 1 to 3):* Secure communal virtual chest accessible in-game via `/team vault` (or `/team coffre`, `/team chest`), direct `/team` action button, or the Player Web Portal (18 slots / 2 rows at tier 1, 36 slots / 4 rows at tier 2, 54 slots / 6 rows at tier 3).
- **Direct GUI Action Buttons & Bedrock Forms:**
  - In `/team` interface (54 slots): Slot 38 (Bed: Left-click for `/team home`, Right-click for `/team sethome`), Slot 40 (Chest/Barrel: Left-click for `/team vault`), Slot 42 (Bank balance status).
  - Right-click shortcuts built into `/team upgrades` (Slot 19 for Home, Slot 23 for Vault).
  - Native Bedrock Cumulus dialogs with buttons for Guild Home, Set Home, and Guild Vault.
- **BlueMap Live Integration:** All guild claims render seamlessly on 3D BlueMap with customized hex colors configured via `/team color <#hex>` or the Web Panel.

---

## 12. Solo Quest Perks (SoloPerkModule)
Comprehensive individual progression and rewards system for solo adventurers, connecting lifetime completed quests to permanent benefits and major masteries:
- **Autonomy & Runtime Control:** 29th autonomous GensCore module (`solo_perks`), dynamically toggled at runtime (`/module solo_perks <on|off>`) or via the web admin panel without server restarts.
- **Free Milestone Perks:** Unlocked automatically or on click once the lifetime completed quest quota is reached:
  1. *Free Reroll (5 quests):* 1 free daily quest reroll (`/quest reroll`).
  2. *Extra Home (15 quests):* +1 additional personal waypoint usable with `/sethome`.
  3. *Celestial Stride (30 quests):* Permanent Speed I effect out of combat.
  4. *Jobs Wisdom (50 quests):* Permanent +5% multiplier on all jobs XP gains.
  5. *Instant Teleport (75 quests):* Halves all teleport warmup delays.
  6. *Endless Feast (100 quests):* Unlocks the `/feed` command with a 15-minute cooldown without requiring a VIP rank.
- **Major Solo Masteries:** Unlocked upon reaching a quest milestone AND paying an acquisition cost in dollars ($) or XP levels if the economy module is disabled:
  1. *Item Magnet (25 quests, $20,000 / 40 XP):* Magnetically draws ground items within a 5-block radius directly to the player. Can be toggled On/Off (`/magnet`, `/perks` GUI, or Web).
  2. *Double Harvest (40 quests, $35,000 / 60 XP):* 5% chance to double ore and log drops.
  3. *Portable Workbench (60 quests, $50,000 / 80 XP):* Unlocks instant access to `/craft` and `/workbench` everywhere.
  4. *Auto-Smelt (80 quests, $75,000 / 100 XP):* Automatically smelts mined raw ores into refined ingots. Can be toggled On/Off (`/autosmelt`, `/perks` GUI, or Web).
  5. *Soul Preservation (100 quests, $100,000 / 120 XP):* Retains 50% of your experience levels upon death.
- **Real-Time In-Game & Web Sync:** All perk acquisitions and On/Off toggle states synchronize instantly between SQLite, the in-game GUI (`/perks`), and the Player Web Portal (`/dashboard/perks`).

---

## Complete Module List (29)
`UtilsModule`, `TombModule`, `TeleportTpaModule`, `TeleportSpawnModule`, `TeleportHomeModule`, `TeleportBackModule`, `TeamModule`, `TabBoardModule`, `StatsModule`, `SpawnerModule`, `ShopModule`, `SoloPerkModule`, `QuestModule`, `MotdModule`, `ModerationModule`, `LootModule`, `LockModule`, `HeadDropModule`, `CustomGuiModule`, `GuiModule`, `JobsModule`, `FastLeafDecayModule`, `EconomyModule`, `ChatModule`, `BlueMapModule`, `DiscordModule`, `AuctionHouseModule`, `AuthModule`, `BedrockSkinModule`.
