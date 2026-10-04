# GensCore - Comprehensive Feature, Command & Web Documentation

Welcome to the official documentation for **GensCore**, the all-in-one survival/faction core plugin engineered for **PaperMC & Folia (Minecraft 26.1+)** running on **Java 25 (LTS)**.

---

## Table of Contents
1. [Architecture & Technical Overview](#1-architecture--technical-overview)
2. [Complete Command Reference](#2-complete-command-reference)
   - [Authentication & Account Management](#authentication--account-management)
   - [Economy & Dynamic Shop](#economy--dynamic-shop)
   - [Auction House](#auction-house)
   - [Guilds & Teams](#guilds--teams)
   - [Jobs & Professions](#jobs--professions)
   - [Daily Quests](#daily-quests)
   - [Solo Perks & Quest Masteries](#solo-perks--quest-masteries)
   - [Teleportation & Navigation](#teleportation--navigation)
   - [Container Security & Lock Protection](#container-security--lock-protection)
   - [Custom Spawners & Stacking](#custom-spawners--stacking)
   - [Survival Utilities](#survival-utilities)
   - [Staff & Moderation](#staff--moderation)
   - [Discord Integration](#discord-integration)
   - [Web Casino & Inventory Bridge](#web-casino--inventory-bridge)
   - [Core & Module Management](#core--module-management)
3. [Complete Permissions Reference & Setup Guide](#3-complete-permissions-reference--setup-guide)
   - [Permission Nodes Matrix](#permission-nodes-matrix)
   - [LuckPerms Configuration Templates](#luckperms-configuration-templates)
4. [In-Game Modules & Deep-Dive Mechanics](#4-in-game-modules--deep-dive-mechanics)
   - [Death Graves (Tomb Module)](#death-graves-tomb-module)
   - [Lootr Instanced Chests](#lootr-instanced-chests)
   - [Dynamic Shop Pricing Algorithm](#dynamic-shop-pricing-algorithm)
   - [Custom Spawner Leveling](#custom-spawner-leveling)
   - [Jobs Leveling & Economy Rewards](#jobs-leveling--economy-rewards)
   - [Container Locking & Anti-Piston Security](#container-locking--anti-piston-security)
   - [Guilds, Claims & Shared Treasury](#guilds-claims--shared-treasury)
   - [Solo Quest Perks & Masteries](#solo-quest-perks--masteries)
   - [Minigames & Casino](#minigames--casino)
   - [Bedrock & Floodgate Cross-Play Features](#bedrock--floodgate-cross-play-features)
   - [ViaVersion & Multi-Protocol Interoperability](#viaversion--multi-protocol-interoperability)
   - [Custom YAML Menus](#custom-yaml-menus)
5. [The Web Application & Administration Panel](#5-the-web-application--administration-panel)
   - [Web Server Architecture & Security](#web-server-architecture--security)
   - [Admin Control Dashboard](#admin-control-dashboard)
   - [Player Web Portal](#player-web-portal)
   - [Web Minigames (Wheel of Fortune & Slots)](#web-minigames-wheel-of-fortune--slots)
6. [Configuration & Deployment Guide](#6-configuration--deployment-guide)

---

## 1. Architecture & Technical Overview

GensCore is designed as an autonomous, self-contained server core replacing dozens of fragmented plugins.

- **Engine Target:** **PaperMC** and **Folia** (Minecraft 26.1+).
- **Runtime:** **Java 25 (LTS)** with Classfile 69 compatibility and ASM 9.10.1 shading.
- **Concurrency & Folia Threading:** Utilizes *FoliaLib* for regional multi-threading. Global actions run on `GlobalRegionScheduler`, chunk tasks on `RegionScheduler`, and player actions on `EntityScheduler`. Console commands and inventory manipulations are isolated to prevent cross-thread synchronization crashes.
- **Dual Database Architecture (SQLite Default & Optional MySQL/MariaDB):** By default, GensCore runs on an embedded **SQLite** engine operating in **WAL (Write-Ahead Logging)** mode (`PRAGMA journal_mode = WAL; PRAGMA synchronous = NORMAL;`). For multi-server networks or external storage, an optional **MySQL / MariaDB** connection can be activated in `config.yml`. If the external database is unreachable at startup, an automatic safe fallback to local SQLite engages to ensure the server always boots without interruption.
- **Cross-Play Ready (Geyser & Floodgate):** Bedrock players are natively detected. In-game menus automatically open as native Bedrock dialogs (Cumulus Forms API) on Bedrock clients, while Java players receive virtual chest GUIs. Bedrock custom skins and heads are supported via the built-in Web Avatar API.
- **Multi-Protocol & ViaVersion Interoperability:** Uses reflection-isolated `ViaVersionUtil` to seamlessly support legacy Java clients (26.2, 1.21.x, 1.20.x). Automatically substitutes 26.3 blocks (Pale Oak, Resin) in menus with cross-version equivalents, exposes `%genscore_client_version%` via PlaceholderAPI, and provides `/check <player>` for staff diagnostics.
- **Vault & LuckPerms Optionality:** Built-in economy and permission systems function with or without Vault and LuckPerms. If Vault is present, GensCore registers its `GensVaultEconomy` provider automatically.

---

## 2. Complete Command Reference

### Authentication & Account Management
Designed for offline/cracked server security or auxiliary server authentication.

| Command | Arguments | Permission | Default | Description |
|---|---|---|---|---|
| `/register` | `<password> <confirmPassword>` | *None* | Everyone | Creates an account and hashes the password using **BCrypt**. |
| `/login` | `<password>` | *None* | Everyone | Authenticates the player session. Freezes movement and interactions until completed. |
| `/changemdp` | `<oldPassword> <newPassword>` | *None* | Everyone | Updates the player's account password. *(Alias: `/changepassword`)* |
| `/resetmdp` | `<player>` | `genscore.admin` | OP | Deletes the targeted player's authentication entry, forcing them to re-register. |

---

### Economy & Dynamic Shop
Centralized currency management with persistent balances and dynamic inflation calculation.

| Command | Arguments | Permission | Default | Description |
|---|---|---|---|---|
| `/money` | *None* | *None* | Everyone | Displays your current monetary balance. *(Alias: `/balance`)* |
| `/money` | `<player>` | `genscore.admin` | OP | Inspects the balance of another player. |
| `/baltop` | *None* | *None* | Everyone | Displays the server's wealth leaderboard (top balances). |
| `/pay` | `<player> <amount>` | *None* | Everyone | Securely transfers money from your balance to another online player. |
| `/eco set` | `<player> <amount>` | `genscore.admin` | OP | Sets a player's balance to a specific amount. |
| `/eco give` | `<player> <amount>` | `genscore.admin` | OP | Adds funds to a player's balance. |
| `/eco take` | `<player> <amount>` | `genscore.admin` | OP | Withdraws funds from a player's balance. |
| `/eco reset` | `<player>` | `genscore.admin` | OP | Resets a player's balance to the starting default. |
| `/shop` | *None* | *None* | Everyone | Opens the dynamic server shop GUI with supply/demand price changes. |

---

### Auction House
Player-to-player marketplace with expiration timers, listing limits, and automated sales taxation.

| Command | Arguments | Permission | Default | Description |
|---|---|---|---|---|
| `/ah` | *None* | *None* | Everyone | Opens the Auction House browsing GUI. *(Aliases: `/auctionhouse`, `/hdv`)* |
| `/ah sell` | `<price>` | *None* | Everyone | Puts the item currently held in your main hand up for sale at the specified price. |

- **Selling Fee / Tax:** Configurable via Web Panel or `config.yml` (default 5%).
- **Listing Recovery:** Expired or unsold items can be reclaimed directly through the player's `/ah` interface.

---

### Guilds & Teams
Comprehensive guild and clan management system with shared treasury, chunk claim territorial protections, on-screen boundary notifications, BlueMap marker synchronization, role hierarchy (Leader, Admin, Member), in-game perks, and integrated web portal control.

| Command | Arguments | Permission | Default | Description |
|---|---|---|---|---|
| `/team` | *None* | *None* | Everyone | Opens the interactive Guild Management GUI. *(Aliases: `/guild`, `/guilde`, `/teams`)* |
| `/team create` | `<name>` | *None* | Everyone | Creates a new guild with the sender as leader (max 16 characters). |
| `/team invite` | `<player>` | *None* | Admin / Leader | Invites a player to join your guild. |
| `/team accept` | *None* | *None* | Everyone | Accepts a pending guild invitation. |
| `/team kick` | `<player>` | *None* | Admin / Leader | Kicks a member from the guild (Admins cannot kick the Leader or other Admins). |
| `/team promote` | `<player>` | *None* | Leader | Promotes a guild member to Administrator (`ADMIN`). |
| `/team demote` | `<player>` | *None* | Leader | Demotes an Administrator back to regular Member (`MEMBER`). |
| `/team leave` | *None* | *None* | Member / Admin | Leaves your current guild (Leaders must disband or transfer leadership first). |
| `/team disband` | *None* | *None* | Leader | Permanently disbands the guild and unclaims all territory. |
| `/team quest` | *None* | *None* | Everyone | Opens the shared Guild Quests progression menu. |
| `/team upgrades` | *None* | *None* | Everyone | Opens the Guild Upgrades shop GUI. |
| `/team sethome` | *None* | *None* | Admin / Leader | Defines the shared guild home waypoint at your current location (requires `GUILD_HOME`). |
| `/team home` | *None* | *None* | Everyone | Teleports to the shared guild home (requires `GUILD_HOME`). |
| `/team vault` | *None* | *None* | Everyone | Opens the communal guild virtual vault. *(Aliases: `/team coffre`, `/team chest`)* (requires `GUILD_VAULT`). |
| `/team deposit` | `<amount>` | *None* | Everyone | Deposits dollars into the guild bank (when Economy is active). |
| `/team depositxp` | `<levels>` | *None* | Everyone | Deposits XP levels into the guild bank (alternative when Economy is disabled). |
| `/team withdraw` | `<amount>` | *None* | Admin / Leader | Withdraws dollars from the guild bank into personal balance. |
| `/team withdrawxp` | `<levels>` | *None* | Admin / Leader | Withdraws XP levels from the guild bank into player levels. |
| `/team claim` | *None* | *None* | Admin / Leader | Claims the current chunk ($16 \times 16$) for the guild. Strictly funded from the guild bank. |
| `/team unclaim` | *None* | *None* | Admin / Leader | Releases the current chunk claim back to the wild. |
| `/team color` | `<hex>` | *None* | Admin / Leader | Sets the guild territory color for BlueMap rendering (e.g. `#3498db`). |

#### Guild Hierarchy & Role Permissions
GensCore implements a 3-tier hierarchy system for guild security and co-management:

| Permission / Action | Chef (`LEADER`) | Admin (`ADMIN`) | Membre (`MEMBER`) |
|---|:---:|:---:|:---:|
| Disband Guild (`/team disband`) | Oui | Non | Non |
| Promote / Demote Admins | Oui | Non | Non |
| Kick Members | Oui (Tous) | Oui (Membres uniquement) | Non |
| Invite Players (`/team invite`) | Oui | Oui | Non |
| Define Guild Home (`/team sethome`) | Oui | Oui | Non |
| Claim / Unclaim Territory | Oui | Oui | Non |
| Change BlueMap Color (`/team color`) | Oui | Oui | Non |
| Purchase Upgrades (`/team upgrades`) | Oui | Oui | Non |
| Withdraw Bank Funds / XP | Oui | Oui | Non |
| Teleport to Guild Home (`/team home`) | Oui | Oui | Oui |
| Access Guild Vault (`/team vault`) | Oui | Oui | Oui |
| Deposit Bank Funds / XP | Oui | Oui | Oui |
| Access Guild-Locked Chests (`/lock guild`) | Oui | Oui | Oui |
| Build / Interact in Guild Claims | Oui | Oui | Oui |
| Participate in Guild Quests | Oui | Oui | Oui |

- **Management via In-Game GUI (`/team`):**
  - **Quick Action Buttons:**
    - Slot 38 (Bed): Guild Home. Left-click to teleport (`/team home`), Right-click for Admin/Leader to set the home (`/team sethome`).
    - Slot 40 (Chest/Barrel): Guild Vault. Left-click to open the communal virtual chest (`/team vault`).
    - Slot 42 (Gold Ingot/Bottle of Enchanting): Guild Bank treasury status and balance.
    - Slot 19 and 23 in `/team upgrades`: Right-click shortcuts to teleport to home and open vault directly from the upgrade menu.
  - **Bedrock / Floodgate Dialogs:**
    - Native Cumulus form buttons dynamically adapt to player permissions with dedicated actions for "Home de Guilde", "Definir le Home", and "Coffre de Guilde".
  - **Member Management:**
    - Clicking on a member's player head allows quick role actions:
      - **Left-Click:** Promote to Admin or Demote to Member (Leader only).
      - **Right-Click:** Kick the player from the guild (with role validation).
- **Management via Web Player Portal:**
  - Guild leaders and admins can log into the server Web Portal (`/portal` or via website) and access the **Guilde** tab.
  - View real-time treasury balances, manage members (Promote, Demote, Kick with confirmation modal), adjust the BlueMap territory color using an intuitive color picker, and purchase guild upgrades in a single click.

#### Territory Claims & Anti-Grief Protection
- **Comprehensive Anti-Grief Shield:** Unaffiliated players cannot break blocks, place blocks, open chests/barrels/shulker boxes, use hoppers/furnaces, or interact with redstone mechanisms (doors, trapdoors, buttons, levers) in claimed chunks.
- **Entity & Livestock Protection:** Armor stands, item frames, paintings, villagers, and passive animals within claimed chunks cannot be damaged or altered by non-members.
- **Anti-Piston Boundary Safeguard:** Pistons cannot push or pull blocks across chunk boundaries into or out of guild territory.
- **On-Screen Border Notifications:**
  - When crossing into a claimed guild territory, players receive an animated Title and Subtitle on their screen displaying the guild's name and protection status, accompanied by an immersive chime sound effect.
- **Claim Limits & Base Costs:**
  - Base claim limit: 4 chunks ($16 \times 16$, expandable up to 20 chunks via the `EXTENDED_TERRITORY` upgrade).
  - Cost per claim: $1500.00 (Economy ON) or 10 XP Levels (Economy OFF), charged strictly to the shared guild bank.
- **BlueMap Live Visualization:** All claimed chunks are rendered in real time on the interactive BlueMap web layer using the hex color chosen by the guild leaders.

#### Guild Shared Treasury (Bank)
- **Economy-Aware Currency Duality:**
  - When `EconomyModule` is enabled: Treasury transactions use in-game currency ($).
  - When `EconomyModule` is disabled: Treasury automatically switches to player Experience (XP levels). The web portal and in-game GUIs dynamically hide currency inputs and display XP levels.
- **Strict Treasury Funding Rule:** All land claims and guild perk upgrades must be funded directly and exclusively from the guild bank. Direct player purchases are prohibited to ensure collaborative teamwork.

#### Guild Perks & Upgrades System (10 Active Trees)
Guilds can unlock 10 permanent team-wide upgrades paid through the guild bank:
1. **MAX_MEMBERS (Levels 1 to 3):**
   - Base: 5 members.
   - Formula: `5 + (level * 3)` -> 8, 11, 14 members max.
   - Pricing ($ / XP): Level 1: $5,000 (25 XP), Level 2: $15,000 (45 XP), Level 3: $35,000 (70 XP).
2. **EXTENDED_TERRITORY (Levels 1 to 4):**
   - Base: 4 claims.
   - Formula: `4 + (level * 4)` -> 8, 12, 16, 20 chunks max.
   - Pricing ($ / XP): Level 1: $4,000 (20 XP), Level 2: $10,000 (35 XP), Level 3: $20,000 (55 XP), Level 4: $40,000 (80 XP).
3. **JOBS_BOOST (Levels 1 to 3):**
   - Grants a permanent job experience multiplier to all online guild members.
   - Formula: `+5%` XP per level (`1.05x`, `1.10x`, `1.15x`).
   - Pricing ($ / XP): Level 1: $10,000 (30 XP), Level 2: $25,000 (50 XP), Level 3: $50,000 (80 XP).
4. **AH_TAX_REDUCTION (Levels 1 to 2):**
   - Reduces the sales commission tax when selling items on the Auction House (`/ah`).
   - Formula: `-25%` tax reduction per level (Level 1: -25%, Level 2: -50% tax).
   - Pricing ($ / XP): Level 1: $8,000 (30 XP), Level 2: $20,000 (55 XP).
5. **COOP_QUESTS_BOOST (Levels 1 to 2):**
   - Boosts the points and contribution earned towards weekly guild quests.
   - Formula: `+10%` guild quest points per level (`1.10x`, `1.20x`).
   - Pricing ($ / XP): Level 1: $12,000 (35 XP), Level 2: $30,000 (60 XP).
6. **GUILD_HOME (Levels 1 to 3):**
   - Shared teleportation waypoint accessible by all members via `/team home` and defined by leaders/admins via `/team sethome`.
   - Level 1: 5-second warmup delay, 15-minute cooldown.
   - Level 2: 3-second warmup delay, 5-minute cooldown.
   - Level 3: Instant warmup inside claimed territory, 1-minute cooldown.
   - Pricing ($ / XP): Level 1: $15,000 (40 XP), Level 2: $35,000 (65 XP), Level 3: $75,000 (95 XP).
7. **TERRITORY_BUFF (Levels 1 to 3):**
   - Passive area-of-effect potion buffs radiating to guild members located within claimed chunks.
   - Level 1: Passive Regeneration I and slow Saturation.
   - Level 2: Speed I inside territory claims.
   - Level 3: Haste I inside territory claims.
   - Anti-Abuse Protection: To prevent combat logging and nomadic claim-and-mine abuse, newly claimed chunks require a 5-minute stabilization anchor (`CLAIM_ANCHOR_WARMUP_MS = 300_000L`) before radiating buffs (displayed via ActionBar countdown). Players entering guild territory must remain inside for 15 seconds of synchronization warmup before receiving effects. Leaving guild territory immediately strips all active territory buffs.
   - Pricing ($ / XP): Level 1: $20,000 (50 XP), Level 2: $45,000 (75 XP), Level 3: $90,000 (110 XP).
8. **BANK_INTEREST (Levels 1 to 2):**
   - Automatically deposits passive interest dividends into the shared guild bank every 24 real-world hours based on the current treasury balance.
   - Level 1: +1% daily interest (capped at $10,000 or 50 XP max per day).
   - Level 2: +2% daily interest (capped at $25,000 or 100 XP max per day).
   - Pricing ($ / XP): Level 1: $25,000 (60 XP), Level 2: $60,000 (90 XP).
9. **SPAWNER_EFFICIENCY (Levels 1 to 2):**
   - Boosts custom smart spawners placed within the guild's claimed chunks (`SpawnerManager.generateTick()`).
   - Level 1: +15% spawn tick speed.
   - Level 2: +30% spawn tick speed.
   - Pricing ($ / XP): Level 1: $30,000 (70 XP), Level 2: $70,000 (100 XP).
10. **GUILD_VAULT (Levels 1 to 3):**
    - Communal virtual storage chest accessible in-game via `/team vault` (or aliases `/team coffre`, `/team chest`), direct `/team` action button, or the Player Web Portal.
    - Level 1: 18 slots (2 rows).
    - Level 2: 36 slots (4 rows).
    - Level 3: 54 slots (6 rows).
    - Pricing ($ / XP): Level 1: $15,000 (45 XP), Level 2: $40,000 (70 XP), Level 3: $80,000 (100 XP).

---

### Jobs & Professions
Earn income and level up by interacting with the game world.

| Command | Arguments | Permission | Default | Description |
|---|---|---|---|---|
| `/jobs` | *None* | *None* | Everyone | Opens the Jobs GUI displaying available professions and progression. *(Aliases: `/job`, `/metier`, `/metiers`)* |

**Available Professions:**
1. **Miner:** Earn money and XP by mining ores and stone underground.
2. **Lumberjack:** Gain rewards by chopping logs and harvesting trees.
3. **Hunter:** Earn bounties by hunting hostile monsters and wildlife.
4. **Farmer:** Earn by harvesting crops (wheat, carrots, potatoes, melons, pumpkins).
5. **Fisherman:** Reel in fish and oceanic treasures.
6. **Builder:** Place structural materials to advance your construction skill.

---

### Daily Quests
A revolving quest system providing daily objectives with rewards.

| Command | Arguments | Permission | Default | Description |
|---|---|---|---|---|
| `/quests` | *None* | *None* | Everyone | Opens the Daily Quests GUI showing active objectives. *(Aliases: `/quest`, `/quete`, `/quetes`)* |

- **Quest Rerolls:** Players with `genscore.quests.reroll` can right-click an objective in the GUI to reroll it.
- **Admin Reset:** Admins with `genscore.quests.admin` can force-reroll all active quests.

---

### Solo Perks & Quest Masteries
Individual progression system rewarding lifetime completed quests with permanent advantages and major masteries. Fully managed in-game via chest GUI (`/perks`) or on the Player Web Portal (`/dashboard/perks`).

| Command | Arguments | Permission | Default | Description |
|---|---|---|---|---|
| `/perks` | *None* | *None* | Everyone | Opens the Solo Quest Perks GUI. *(Aliases: `/bonus`, `/passe`)* |
| `/autosmelt` | *None* | *None* | Everyone | Toggles instant raw ore smelting On/Off (requires Auto-Smelt mastery). |
| `/magnet` | *None* | *None* | Everyone | Toggles 5-block item attraction magnet On/Off (requires Magnet mastery). |

#### Two-Tier Progression Mechanics:
1. **Free Milestone Perks (Paliers Gratuits):**
   - **Free Reroll (5 quests):** 1 additional free daily quest reroll (`/quest reroll`).
   - **Extra Home (15 quests):** +1 additional personal waypoint usable with `/sethome`.
   - **Celestial Stride (30 quests):** Permanent Speed I effect out of combat.
   - **Jobs Wisdom (50 quests):** Permanent +5% multiplier on all jobs XP gains.
   - **Instant Teleport (75 quests):** Halves all teleport warmup delays.
   - **Endless Feast (100 quests):** Unlocks `/feed` command with 15-minute cooldown without VIP rank.
2. **Major Solo Masteries (Maîtrises Majeures Payantes):**
   - **Item Magnet (25 quests, $20,000 / 40 XP):** Magnetically draws items within 5 blocks directly to the player. Can be toggled On/Off (`/magnet`, `/perks` GUI, or Web).
   - **Double Harvest (40 quests, $35,000 / 60 XP):** 5% chance to double ore and log drops.
   - **Portable Workbench (60 quests, $50,000 / 80 XP):** Unlocks instant access to `/craft` and `/workbench` everywhere.
   - **Auto-Smelt (80 quests, $75,000 / 100 XP):** Automatically smelts mined raw ores into refined ingots. Can be toggled On/Off (`/autosmelt`, `/perks` GUI, or Web).
   - **Soul Preservation (100 quests, $100,000 / 120 XP):** Retains 50% of experience levels upon death.

---

### Teleportation & Navigation
Essential survival travel commands with configurable delays, warmup timers, and cooldown bypasses.

| Command | Arguments | Permission | Default | Description |
|---|---|---|---|---|
| `/spawn` | *None* | `genscore.spawn` | `true` | Teleports to the world spawn location. |
| `/setspawn` | *None* | `genscore.admin` | OP | Sets the global spawn coordinate to your current position. |
| `/sethome` | `[name]` | `genscore.home` | `true` | Creates a personal home waypoint (default name: `home`). |
| `/home` | `[name]` | `genscore.home` | `true` | Teleports to the specified home waypoint. |
| `/delhome` | `[name]` | `genscore.home` | `true` | Deletes an existing home waypoint. |
| `/back` | *None* | `genscore.back` | `false` | Returns to your previous death point or teleport departure point. |
| `/tpa` | `<player>` | `genscore.tpa` | `true` | Sends a request to teleport to another player. |
| `/tpaccept` | *None* | `genscore.tpa` | `true` | Accepts the latest incoming teleport request. |
| `/tpadeny` | *None* | `genscore.tpa` | `true` | Denies the incoming teleport request. |
| `/tpacancel` | *None* | `genscore.tpa` | `true` | Cancels an outgoing teleport request you previously sent. |

---

### Container Security & Lock Protection
Protects storage blocks from unauthorized access and piston griefing.

| Command | Arguments | Permission | Default | Description |
|---|---|---|---|---|
| `/lock` | `[private]` | *None* | Everyone | Arms your cursor; right-click a chest, barrel, or shulker box to claim it as your private container. *(Alias: `/cprivate`)* |
| `/lock unlock` | *None* | *None* | Everyone | Arms your cursor; right-click your locked container to remove the lock. |
| `/lock guild` | *None* | *None* | Everyone | Arms your cursor; right-click a container to share access with all members of your guild. |

**Anti-Exploit Safeguards:**
- Locked containers cannot be broken by non-owners.
- Pistons cannot push, pull, or shatter locked containers or shulkers.
- Explosions respect lock protection.

---

### Custom Spawners & Stacking
Enhanced mob spawners that stack, store mob drops, and upgrade with currency or experience.

| Command | Arguments | Permission | Default | Description |
|---|---|---|---|---|
| `/spawner give` | `<player> <type> [stack]` | `genscore.admin.spawner` | OP | Spawns a custom stacked spawner item into the target's inventory. |

- **Right-Click Spawner in-game:** Opens the Spawner Upgrade GUI to increase spawn rate, upgrade internal storage capacity, or boost experience drops.
- **Stacking:** Shift-right clicking an existing spawner with a matching spawner item merges them into a single block, saving server performance.

---

### Survival Utilities
Quality-of-life virtual workbenches and survival commands.

| Command | Arguments | Permission | Default | Description |
|---|---|---|---|---|
| `/ec` | *None* | `genscore.ec` | OP | Opens your Ender Chest from anywhere. *(Alias: `/enderchest`)* |
| `/craft` | *None* | `genscore.craft` | OP | Opens a virtual Crafting Table (3x3 workbench). *(Aliases: `/craftingtable`, `/workbench`)* |
| `/anvil` | *None* | `genscore.anvil` | OP | Opens a virtual Anvil interface. |
| `/enchant` | *None* | `genscore.enchant` | OP | Opens a virtual Enchanting Table interface. *(Alias: `/enchanttable`)* |
| `/feed` | *None* | `genscore.feed` | OP | Restores your hunger bar to full saturation. |

---

### Staff & Moderation
Enforcement suite logging actions to database and Discord webhooks.

| Command | Arguments | Permission | Default | Description |
|---|---|---|---|---|
| `/ban` | `<player> [duration] [reason]` | `genscore.ban` | OP | Bans a player permanently or temporarily. Disconnects immediately with formatted reason. |
| `/unban` | `<player>` | `genscore.ban` | OP | Revokes an active ban. |
| `/mute` | `<player> [duration] [reason]` | `genscore.mute` | OP | Mutes a player in chat. Duration accepts formats like `1h`, `30m`, `7d`. |
| `/unmute` | `<player>` | `genscore.mute` | OP | Unmutes a muted player. |
| `/kick` | `<player> [reason]` | `genscore.kick` | OP | Kicks an online player from the server. |
| `/freeze` | `<player>` | `genscore.freeze` | OP | Freezes a player in place for screensharing/investigation (disables movement, interaction, and commands). |
| `/openinv` | `<player>` | `genscore.openinv` | OP | Live inspection of another player's inventory and armor slots. *(Alias: `/invsee`)* |
| `/check` | `<player>` | `genscore.check` | OP | Detailed technical dossier of a player (Bedrock/Java, client version via ViaVersion, protocol ID, ping, health, moderation status, and coordinates). *(Alias: `/whois`)* |

---

### Discord Integration
Links Minecraft accounts with your Discord server using JDA (Java Discord API).

| Command | Arguments | Permission | Default | Description |
|---|---|---|---|---|
| `/discord link` | *None* | *None* | Everyone | Generates a 6-character linking code to be entered in your Discord server via `!link <code>`. |
| `/discord reg` | *None* | *None* | Everyone | Alias for linking assistance and Discord server link. |

- When linked, players receive the `genscore.discord.linked` status, displaying a verified badge in chat and tablist.

---

### Web Casino & Inventory Bridge
Interacts directly with the Web Panel Slot Machine / Casino game.

| Command | Arguments | Permission | Default | Description |
|---|---|---|---|---|
| `/web` | *None* | *None* | Everyone | Displays instructions for depositing and withdrawing web casino items. |
| `/web deposit` | *None* | *None* | Everyone | Transfers the item held in your main hand into your web casino balance. |
| `/web withdraw` | *None* | *None* | Everyone | Opens a retrieval GUI to collect items won on the web slot machine. |

---

### Core & Module Management
Master administration commands for server operators.

| Command | Arguments | Permission | Default | Description |
|---|---|---|---|---|
| `/gens` | `[status|db]` | `genscore.admin` | OP | Master administration command displaying system health, JVM metrics, Folia status, and database pool statistics. |
| `/module` | `<moduleName> <on\|off>` | `genscore.admin` | OP | Dynamically enables or disables any internal module at runtime without restarting. |
| `/menu` | `[name]` | *None* | Everyone | Opens a custom YAML menu configured in `plugins/GensCore/menus/`. |

---

## 3. Complete Permissions Reference & Setup Guide

### Permission Nodes Matrix

| Permission Node | Description | Default Assignment | Recommended Rank |
|---|---|---|---|
| `genscore.chat` | Allows chatting in the public chat channel | `true` (All players) | Default |
| `genscore.home` | Allows `/sethome`, `/home`, and `/delhome` | `true` (All players) | Default |
| `genscore.spawn` | Allows `/spawn` | `true` (All players) | Default |
| `genscore.tpa` | Allows `/tpa`, `/tpaccept`, `/tpadeny`, `/tpacancel` | `true` (All players) | Default |
| `genscore.back` | Allows returning to last location with `/back` | `false` | VIP / Donator |
| `genscore.ec` | Allows opening virtual Ender Chest with `/ec` | `op` | VIP / Donator |
| `genscore.craft` | Allows opening virtual Crafting Table with `/craft` | `op` | VIP / Donator |
| `genscore.anvil` | Allows opening virtual Anvil with `/anvil` | `op` | VIP+ |
| `genscore.enchant` | Allows opening virtual Enchant Table with `/enchant` | `op` | VIP+ |
| `genscore.feed` | Allows satisfying hunger with `/feed` | `op` | VIP+ / Mod |
| `genscore.quests.reroll` | Allows right-click rerolling of daily quests | `op` | VIP / Donator |
| `genscore.quests.admin` | Allows force-rerolling all active quests | `op` | Admin |
| `genscore.discord.linked` | Displays verified Discord badge | `false` | Auto-assigned |
| `genscore.freeze` | Allows freezing and unfreezing suspected players | `op` | Helper / Mod |
| `genscore.openinv` | Allows `/openinv` live inventory inspection | `op` | Moderator |
| `genscore.check` | Allows `/check` and `/whois` technical & client dossier inspection | `op` | Moderator |
| `genscore.mute` | Allows `/mute` and `/unmute` | `op` | Moderator |
| `genscore.kick` | Allows `/kick` | `op` | Moderator |
| `genscore.ban` | Allows `/ban` and `/unban` | `op` | Administrator |
| `genscore.admin.spawner` | Allows giving custom stacked spawners | `op` | Administrator |
| `genscore.tomb.admin` | Allows opening or looting any player's death tomb | `op` | Administrator |
| `genscore.admin` | Master permission: bypasses all limits and grants all admin commands | `op` | Owner / Admin |

#### Cooldown Bypass Permissions
| Permission Node | Description |
|---|---|
| `genscore.bypass.cooldown.all` | Bypasses teleport warmup and cooldowns for every command. |
| `genscore.bypass.cooldown.home` | Bypasses `/home` delay. |
| `genscore.bypass.cooldown.spawn` | Bypasses `/spawn` delay. |
| `genscore.bypass.cooldown.tpa` | Bypasses `/tpa` delay. |
| `genscore.bypass.cooldown.back` | Bypasses `/back` delay. |

---

### LuckPerms Configuration Templates

If you are using **LuckPerms**, you can configure your server roles using the following command sequences:

#### 1. Default Player Group (`default`)
```bash
# Core survival essentials (granted by default, but good to ensure)
lp group default permission set genscore.chat true
lp group default permission set genscore.home true
lp group default permission set genscore.spawn true
lp group default permission set genscore.tpa true
```

#### 2. VIP / Donator Group (`vip`)
```bash
lp group vip parent add default
lp group vip permission set genscore.back true
lp group vip permission set genscore.ec true
lp group vip permission set genscore.craft true
lp group vip permission set genscore.quests.reroll true
lp group vip permission set genscore.bypass.cooldown.home true
```

#### 3. Moderator Group (`moderator`)
```bash
lp group moderator parent add vip
lp group moderator permission set genscore.freeze true
lp group moderator permission set genscore.openinv true
lp group moderator permission set genscore.check true
lp group moderator permission set genscore.mute true
lp group moderator permission set genscore.kick true
lp group moderator permission set genscore.feed true
```

#### 4. Administrator Group (`admin`)
```bash
lp group admin parent add moderator
lp group admin permission set genscore.ban true
lp group admin permission set genscore.admin true
lp group admin permission set genscore.admin.spawner true
lp group admin permission set genscore.tomb.admin true
lp group admin permission set genscore.quests.admin true
lp group admin permission set genscore.bypass.cooldown.all true
```

---

## 4. In-Game Modules & Deep-Dive Mechanics

### Death Graves (Tomb Module)
When a player dies, their items are not scattered into the void or lava.
1. A physical **Grave / Tombstone** spawns at the death coordinates featuring the player's skull and an interactive armor stand.
2. A temporary floating hologram displays the owner's name and a protection timer (e.g., 10 minutes).
3. During the protection window, **only the owner** can right-click the tomb to reclaim all stored items and experience.
4. Admins with `genscore.tomb.admin` can unlock or retrieve items from any grave.
5. If the protection expires, the grave unlocks for public looting or automatically drops the items.

### Lootr Instanced Chests
Built directly into GensCore without requiring external mods or plugins.
- Every natural dungeon or structure chest is **per-player instanced**.
- When Player A opens a dungeon chest, they receive unique loot generated for them.
- When Player B visits the same chest, it appears unopened and full of fresh loot specifically for Player B.
- **Hopper Prevention:** Hoppers cannot drain items from Lootr chests.
- **Break Protection:** Chests cannot be broken until all items are claimed, or are completely unbreakable based on configuration.

### Dynamic Shop Pricing Algorithm
The `/shop` module features a simulated supply-and-demand economy:
$$\text{Current Price} = \text{Base Price} \times \left(1 + \frac{\text{Purchases} - \text{Sales}}{\text{Threshold}}\right)^{\text{exponent}}$$
- When players heavily buy an item (e.g., Diamond), its price automatically escalates.
- When players mass-sell items (e.g., Cobblestone from mining jobs), its market price deflates.
- The `inflationExponent` can be fine-tuned via the Web Panel to prevent economy hyperinflation.

### Custom Spawner Leveling
Unlike vanilla spawners that cause entity lag, GensCore custom spawners optimize server tick times:
- **Stacking:** Spawners of the same type merge into a single block (e.g., `x10 Iron Golem Spawner`).
- **Storage Level:** Accumulated mob drops are stored directly inside the spawner block; players open the spawner GUI to claim the loot in bulk.
- **Speed Level:** Reduces delay between spawn cycles.
- **Experience Level:** Multiplies XP generated per cycle.

### Jobs Leveling & Economy Rewards
Progression curves calculate both income and job level:
- As players perform job tasks (e.g., breaking Diamond Ore for Miner, slaying Withers for Hunter), they earn direct cash deposits and job experience points.
- Reaching new job levels unlocks title achievements, cash bonuses, and multiplies future earnings.
- Player stats are saved asynchronously to SQLite every 60 seconds to avoid main thread latency.

### Container Locking & Anti-Piston Security
Chest and container security operates at the block-event level:
- Locks protect **Chests, Trapped Chests, Barrels, and Shulker Boxes**.
- **Anti-Piston Shield:** Pistons cannot move, retract, or push locked blocks.
- **Anti-Shulker Break:** Piston-based shulker breaking contraptions are cancelled, preventing duplication or theft exploits.

### Guilds, Claims & Shared Treasury
A complete clan ecosystem supporting Folia multi-threading and Bedrock crossplay:
- **Hierarchy & Roles (Leader, Admin, Member):**
  - **Leader (`LEADER`):** Full ownership, admin promotions/demotions, disbanding the guild, treasury withdrawals, and territory claims.
  - **Administrator (`ADMIN`):** Co-management powers: invitations, kicking regular members, treasury withdrawals, land claims/unclaims, BlueMap color styling, and perk purchases.
  - **Member (`MEMBER`):** Depositing funds & XP, participating in weekly co-op quests, and accessing guild-locked storage containers.
- **Shared Treasury (Guild Bank):** Guild members deposit dollars (`/team deposit <amount>`) or XP levels (`/team depositxp <levels>`). When the Economy module is disabled, all transactions and perk upgrades automatically switch to player XP levels.
- **Territory Claims ($16 \times 16$ Chunks):** Guilds claim chunks via `/team claim` ($1500 or 10 XP levels per chunk). Comprehensive anti-grief prevents breaking, placing, container access, redstone, and entity damage by outsiders.
- **Team Perks (10 Active Guild Upgrade Trees):** Max Members, Extended Territory, Jobs Boost, Auction House Tax Reduction, Co-op Quest Boost, Guild Home Warp, Territory Buffs (Regen, Speed, Haste with 5-minute chunk stabilization anchor), Daily Bank Interest, Spawner Efficiency, and Shared Virtual Vault (`/team vault`).
- **BlueMap Live Integration:** All guild claims render seamlessly on 3D BlueMap with customized hex colors.

### Solo Quest Perks & Masteries
Comprehensive individual progression system connecting lifetime completed daily quests to permanent benefits:
- **Free Milestone Perks:**
  1. *Free Reroll (5 quests):* 1 free daily quest reroll (`/quest reroll`).
  2. *Extra Home (15 quests):* +1 additional personal waypoint usable with `/sethome`.
  3. *Celestial Stride (30 quests):* Permanent Speed I effect out of combat.
  4. *Jobs Wisdom (50 quests):* Permanent +5% multiplier on all jobs XP gains.
  5. *Instant Teleport (75 quests):* Halves all teleport warmup delays.
  6. *Endless Feast (100 quests):* Unlocks the `/feed` command with a 15-minute cooldown.
- **Major Solo Masteries:**
  1. *Item Magnet (25 quests, $20,000 / 40 XP):* Magnetically draws ground items within 5 blocks. Toggleable (`/magnet`).
  2. *Double Harvest (40 quests, $35,000 / 60 XP):* 5% chance to double mined ores and chopped logs.
  3. *Portable Workbench (60 quests, $50,000 / 80 XP):* Instant access to `/craft` anywhere.
  4. *Auto-Smelt (80 quests, $75,000 / 100 XP):* Automatically smelts mined raw ores into ingots. Toggleable (`/autosmelt`).
  5. *Soul Preservation (100 quests, $100,000 / 120 XP):* Retains 50% of your experience levels upon death.

### Minigames & Casino
Dynamic interactive minigames designed for server engagement and item circulation:
- **Wheel of Fortune:** Daily reward spinner giving players chances to win resources, netherite, spawners, and items with offline queue persistence.
- **Web Slot Machine:** 3-reel casino machine powered by in-game items deposited via `/web deposit` (balanced aggregate RTP of ~84%).
- **CoinFlip & Bets:** High-stakes peer-to-peer duels synchronized with SQLite/MySQL balances.

### Bedrock & Floodgate Cross-Play Features
- Automatic detection of Bedrock players joining via GeyserMC / Floodgate.
- **Cumulus Forms API:** Opening menus (`/team`, `/jobs`, `/shop`) automatically serves native Bedrock window forms instead of Java inventory containers, avoiding desyncs and touch-screen misclicks.
- **Bedrock Skin & Head Support:** Head requests (`/api/head/{name}`) resolve Geyser XUIDs to fetch authentic Bedrock skins instead of fallback Steve avatars.
- **Bedrock Prefix:** Configurable prefix (e.g. `[Bedrock]` or `.`) automatically handled in commands and LuckPerms group lookups.

### ViaVersion & Multi-Protocol Interoperability
- **Decoupled Reflection API:** `ViaVersionUtil` accesses ViaVersion through dynamic reflection, ensuring zero runtime crashes whether ViaVersion is installed or not.
- **Material Fallback System:** Exclusive Minecraft 26.3 materials (Pale Oak variants, Resin, Creaking Heart) are converted on the fly to universal equivalents (`DARK_OAK_*`, `ORANGE_TERRACOTTA`) when building chest GUIs or Bedrock forms for legacy clients.
- **PlaceholderAPI Official Expansion (`genscore`):** Exposes `%genscore_client_version%`, `%genscore_client_protocol%`, `%genscore_client_type%`, `%genscore_is_legacy%`, `%genscore_is_bedrock%`, `%genscore_balance%`, `%genscore_guild%`, and `%genscore_ping%`.
- **Internal MiniMessage & TabBoard Tags:** `%client_version%`, `%client_protocol%`, `%client_type%`, and `%is_legacy%` available directly across TabBoard headers, footers, and scoreboards.

### Custom YAML Menus
Server owners can build unlimited graphical menus in `plugins/GensCore/menus/*.yml`:
- Specify custom titles, rows (1 to 6), items, display names, and lores.
- Bind custom commands (e.g., `/warp`, `/rules`, `/menu server`).
- Execute console commands, player commands, or open other menus upon clicking items.

---

## 5. The Web Application & Administration Panel

GensCore hosts an integrated **HTTP/HTTPS Web Application** built on **Javalin (Java)** with a **React / Vite** mobile-responsive front-end. It runs concurrently with your Minecraft server on port **8080** (configurable).

```
+--------------------------------------------------------------------+
|                         GensCore Web Panel                         |
|                                                                    |
|  [Admin Dashboard]     [Player Portal]       [Live Games]          |
|  - Real-time TPS/RAM   - Balance & Stats     - Wheel of Fortune    |
|  - Player Moderation   - 7-Day Activity      - Slot Machine        |
|  - Module Toggles      - Transaction Log     - Item In/Out Bridge  |
|  - Embedded BlueMap    - Bedrock Avatars                           |
+--------------------------------------------------------------------+
```

### Web Server Architecture & Security
1. **Zero External Web Dependencies:** The web server runs inside your Minecraft server process. Web assets are automatically unpacked to `plugins/GensCore/web/` on first startup.
2. **Admin Password & BCrypt Hashing:**
   - Default password in `modules/web.yml` is `"gens"`.
   - On initial startup, GensCore automatically detects plain-text strings and replaces them with a salted **BCrypt hash** (`$2a$...`).
   - If you ever need to reset the admin password, replace the hash in `modules/web.yml` with plain text; the plugin will re-hash it automatically on next boot.
3. **Session Authentication:**
   - Admin routes (`/api/admin/*`) require a cryptographically random Bearer token with 24-hour expiration.
   - Player routes (`/api/games/*`, `/api/player/*`) require authenticated session tokens matching their in-game `/register` credentials.
4. **Brute Force Protection:** Built-in IP rate limiting locks out attackers after 5 consecutive failed login attempts within 60 seconds.

---

### Admin Control Dashboard
Accessible at `http://<your-server-ip>:8080/admin`:

- **Live Server Metrics:** Real-time gauges for TPS (Ticks Per Second), active player count, JVM Heap Memory usage, and system CPU load.
- **Interactive Web Console:** Full two-way server console. View live log outputs and send server commands safely executed on Folia's `GlobalRegionScheduler`.
- **Player Manager:**
  - View all registered players, their online/offline status, balances, and UUIDs.
  - Search by name or UUID.
  - Kick, ban, mute, or adjust player money directly through the browser.
- **Live Module Switchboard:** Toggle any of the 30 modules on or off instantly with a click—no server reboot required.
- **Configuration Tuner:**
  - Adjust shop inflation rates.
  - Set auction house taxation percentages.
  - Configure daily quest reroll maximums.
  - Toggle Lootr particles and break restrictions.
  - Update dynamic Server List MOTD (Line 1 & Line 2).
- **Embedded BlueMap:** Integrated live map viewer for administrative territory inspection.
- **Emergency Server Wipe:**
  - A secure, password-guarded button located in the web configuration view.
  - Atomically purges all player tables (economy, homes, auth, stats, quests) while preserving world files and core configuration.

---

### Player Web Portal
Accessible to everyday players at `http://<your-server-ip>:8080`:

- **Player Login:** Log in using your in-game username and `/register` password.
- **Player Dashboard:**
  - View current wallet balance and rank.
  - View cumulative statistics: total blocks broken, mob kills, player kills, death count, and total playtime in hours.
  - Overall Job Level across all 6 professions.
  - **7-Day Quest Activity Chart:** Visual SVG line graph charting your quest completions over the past week.
  - **Recent Transactions Log:** Review your last 5 financial transactions (payments sent, received, or shop purchases).
- **Guild Management Portal (`/dashboard/team`):**
  - **Shared Bank Interface:** Deposit or withdraw funds directly from the web browser. Automatically switches to player XP levels if server economy is disabled.
  - **BlueMap Territory Control:** Real-time claim usage indicator ($X/Y$ chunks) and live hexadecimal color picker for instant BlueMap territory styling.
  - **Perks Shop:** Purchase permanent guild upgrades (Members capacity, Claims, Jobs boost, AH tax reduction, Coop Quests multiplier) funded strictly by the shared bank.
  - **Roster & Role Administration:** View all guild members with badges (`Chef`, `Admin`, `Membre`). Leaders can promote members to Admin or demote them back; Leaders and Admins can kick members with confirmation modals directly from the browser.
- **Player Perks Portal (`/dashboard/perks`):**
  - **Lifetime Quest Tracker:** Global progress bar tracking completed daily quests towards the 100-quest milestone.
  - **Free Milestone Perks:** Claim unlocked rewards upon reaching thresholds (Free Rerolls, Extra Homes, Speed Boost, Jobs XP multiplier, Warmup reduction, /feed access).
  - **Major Masteries Shop:** Acquire high-tier masteries with funds or XP levels (Item Magnet, Double Drop, Portable Workbench, Auto-Smelt, Soul Preservation).
  - **Live Toggle Switches:** Toggle active masteries (Item Magnet, Auto-Smelt) On or Off in real-time with instant SQLite and in-game state synchronization.
- **Avatar Engine (`/api/head/{name}/{size}`):** High-speed 3D head and avatar rendering supporting both Java skins and Bedrock Floodgate avatars.

---

### Web Minigames (Wheel of Fortune & Slots)

#### 1. Wheel of Fortune (Roue de la Fortune)
- A visual spinning wheel accessible through the Web Portal.
- **Cooldown:** Once every 24 hours per account.
- **Weighted Rewards Pool:**
  - 10 Diamonds (40% chance)
  - 2 Netherite Ingots (30% chance)
  - 1 Enchanted Golden Apple (20% chance)
  - 1 Elytra (9% chance)
  - 1 Cow Spawner (1% jackpot chance)
- **Offline Delivery:** If the player is offline when spinning, rewards are queued in `genscore_pending_rewards` and automatically dispatched via Folia's scheduler the next time the player logs into the server.

#### 2. Web Casino (Slot Machine)
- Players bet real in-game items on a 3-reel animated slot machine.
- **1-Hour Cooldown** between machine plays to prevent gambling abuse.
- **In-Game Item Deposit (`/web deposit`):**
  - Hold any item in your main hand in-game and type `/web deposit`.
  - The item is serialized into Base64 and stored in the `player_web_bets` database table.
- **Web Betting:** In your web browser, select your deposited item as your bet and spin the slots.
- **Winning & Withdrawing (`/web withdraw`):**
  - Winning spins multiply your reward items into the `player_web_rewards` table.
  - In Minecraft, run `/web withdraw` to open a 54-slot GUI containing all your won prizes.
  - Click any item in the GUI to transfer it safely back into your player inventory.

---

## 6. Configuration & Deployment Guide

### Directory & File Structure
Upon first run, GensCore automatically unpacks all necessary default configuration files and web assets into `plugins/GensCore/`:

```
plugins/GensCore/
├── config.yml              # Core settings (language, database options)
├── modules.yml             # Global enable/disable switches for all 30 modules
├── genscore.db             # Local SQLite database (WAL mode, connection pooled)
├── lang/
│   ├── fr_FR.yml           # French localization strings (MiniMessage tags)
│   └── en_US.yml           # English localization strings (MiniMessage tags)
├── menus/
│   └── default.yml         # Custom GUI menu definitions (virtual chest layouts)
├── modules/
│   ├── bluemap.yml         # BlueMap web integration URL
│   ├── chat.yml            # Custom chat format and join/quit alerts
│   ├── discord.yml         # Discord bot token, channels, and synchronization
│   ├── economy.yml         # Shop dynamic inflation exponent and AH sales tax
│   ├── headdrop.yml        # Mob and player skull drop probabilities
│   ├── lootr.yml           # Instanced dungeon chests and anti-break security
│   ├── minigames.yml       # Fortune Wheel rewards, Casino Slot RTP, CoinFlip
│   ├── motd.yml            # Server list MOTD lines with MiniMessage tags
│   ├── quests.yml          # Daily quest limits and reroll quotas
│   ├── shop.yml            # Dynamic shop items, base prices, price elasticity
│   ├── spawners.yml        # Custom spawner stacking, tick rate, and Silk Touch
│   ├── tabboard.yml        # Dynamic Scoreboard and Tablist layout & placeholders
│   ├── teams.yml           # Guild weekly quest rotation parameters
│   ├── teleport.yml        # Default max homes and global teleportation delays
│   ├── tomb.yml            # Death graves, XP retention, and access policies
│   └── web.yml             # Embedded Javalin web server, BCrypt auth, CORS, limits
└── web/                    # Extracted React / Vite production frontend bundle
```

---

### Core Configuration (`config.yml`)

The primary configuration file governs language localization and the dual-engine database persistence layer.

```yaml
# Main language file (fr_FR or en_US)
lang: "en_US"

# Database Configuration
database:
  # Storage engine type: "sqlite" (default, autonomous) or "mysql" (remote database)
  type: "sqlite"

  # Local SQLite configuration (active by default if type: "sqlite")
  sqlite:
    file: "genscore.db"

  # Remote MySQL / MariaDB configuration (active only if type: "mysql")
  # If remote connection fails at startup, GensCore automatically falls back to SQLite
  mysql:
    host: "localhost"
    port: 3306
    database: "genscore"
    username: "root"
    password: "YOUR_SECURE_PASSWORD"
    ssl: false
    max_pool_size: 10
    minimum_idle: 2
    connection_timeout: 30000
```

| Key | Type | Default | Description |
|---|---|---|---|
| `lang` | String | `"en_US"` | Active localization file name in `plugins/GensCore/lang/` (without `.yml`). Supports hot-reloading. |
| `database.type` | String | `"sqlite"` | Active storage engine. Valid values: `"sqlite"` or `"mysql"`. |
| `database.sqlite.file` | String | `"genscore.db"` | SQLite database filename stored inside the `plugins/GensCore/` directory. |
| `database.mysql.host` | String | `"localhost"` | IP address or hostname of the MySQL/MariaDB server. |
| `database.mysql.port` | Integer | `3306` | Port of the MySQL/MariaDB service. |
| `database.mysql.database` | String | `"genscore"` | Database/schema name. The schema must exist before connecting. |
| `database.mysql.username` | String | `"root"` | Database authentication username. |
| `database.mysql.password` | String | `""` | Database authentication password. |
| `database.mysql.ssl` | Boolean | `false` | Enable or disable TLS/SSL encryption for the JDBC connection. |
| `database.mysql.max_pool_size` | Integer | `10` | Maximum number of concurrent connections in the HikariCP connection pool. |
| `database.mysql.minimum_idle` | Integer | `2` | Minimum number of idle connections maintained in the pool. |
| `database.mysql.connection_timeout` | Integer | `30000` | Connection timeout in milliseconds before triggering the automatic SQLite fallback. |

> [!NOTE]
> **Resilient Automatic Fallback:** If `type: "mysql"` is specified but the remote server is unreachable, timed out, or rejects authentication, GensCore outputs an actionable warning to the server console and instantly switches to the local SQLite engine. The server will never crash or fail to boot due to a database connection failure.

---

### Global Module Switchboard (`modules.yml`)

GensCore operates on a modular architecture where each feature can be independently toggled on or off. Modules can also be toggled at runtime via `/module <name> <on|off>`.

```yaml
# Enable or disable specific GensCore modules (30 modules).
# Set to true to enable, false to disable.
modules:
  dynamicshop: true
  economy: true
  auctionhouse: true
  jobs: true
  stats: true
  spawners: true
  quests: true
  solo_perks: true
  lootr: true
  headdrop: true
  minigames: true
  motd: true
  tabboard: true
  discord: true
  gui: true
  customgui: true
  tomb: false
  home: true
  back: true
  spawn: true
  tpa: true
  teams: true
  locks: true
  auth: true
  moderation: true
  chat: true
  bluemap: false
  fastleafdecay: true
  utils: true
  bedrockskin: true
```

| Module Key | In-Game Feature | Default | Dependencies / Notes |
|---|---|---|---|
| `dynamicshop` | Supply & demand server shop (`/shop`) | `true` | Requires `economy` module |
| `economy` | Player balances, `/money`, `/pay`, Vault provider | `true` | Registers Vault `GensVaultEconomy` if Vault is present |
| `auctionhouse` | Player-to-player marketplace (`/ah`) | `true` | Requires `economy` module |
| `jobs` | Mining, woodcutting, hunting, farming professions | `true` | Folia thread-safe anti-farm block key cache |
| `stats` | Persistent block break, kill, death & playtime tracking | `true` | Visible in Player Web Dashboard |
| `spawners` | Smart stacked spawners with GUI upgrades & Silk Touch | `true` | Configured via `modules/spawners.yml` |
| `quests` | Daily rotating quests with reroll limits | `true` | Configured via `modules/quests.yml` |
| `lootr` | Per-player instanced dungeon & structure chests | `true` | Configured via `modules/lootr.yml` |
| `headdrop` | Mob and player skull drop probabilities | `true` | Configured via `modules/headdrop.yml` |
| `minigames` | Fortune Wheel, Casino Slots & CoinFlip | `true` | Configured via `modules/minigames.yml` & Web Panel |
| `motd` | Dynamic server list ping message | `true` | Configured via `modules/motd.yml` |
| `tabboard` | Custom Scoreboard and Tablist layout | `true` | Configured via `modules/tabboard.yml` |
| `discord` | Discord Bot bridge (chat sync, staff logs) | `true` | Requires valid bot token in `modules/discord.yml` |
| `gui` | In-game menu provider | `true` | Auto-detects Bedrock clients (Cumulus Forms) |
| `tomb` | Death graves preserving items & XP upon player death | `false` | Enable if you want graves instead of standard item drops |
| `home` | Player home waypoints (`/sethome`, `/home`) | `true` | Limits set via permissions and `modules/teleport.yml` |
| `back` | Return to last death or teleport location (`/back`) | `true` | Zero-leak `BackPosition` memory safety |
| `spawn` | Server spawn location (`/setspawn`, `/spawn`) | `true` | Persistent spawn coordinates in database |
| `tpa` | Teleport request system (`/tpa`, `/tpaccept`, etc.) | `true` | Configurable warmup and movement cancellation |
| `teams` | Guilds, territory claims, shared bank & weekly quests | `true` | Integrated with BlueMap and Web Panel |
| `locks` | Container protection for chests, barrels & furnaces | `true` | Anti-hopper and anti-piston theft protection |
| `auth` | In-game authentication for offline servers (`/login`) | `true` | BCrypt password hashing |
| `moderation` | Staff toolset (`/ban`, `/mute`, `/kick`, `/freeze`) | `true` | Persistent punishment records in database |
| `chat` | Chat formatting and join/quit messages | `true` | LuckPerms prefix/suffix synchronization |
| `bluemap` | BlueMap guild territory claim visualization | `false` | Requires BlueMap installed on the server |
| `fastleafdecay` | Instant natural decay for orphaned tree leaves | `true` | High-performance leaves cleanup |
| `solo_perks` | Solo quest progression perks & masteries (`/perks`) | `true` | Integrated with daily quests & economy |
| `customgui` | Custom YAML inventory menus loader (`/menu`) | `true` | Configured via `plugins/GensCore/menus/` |
| `utils` | Virtual utilities (`/craft`, `/anvil`, `/ec`, `/feed`) | `true` | Native Paper 26.3 MenuType API |
| `bedrockskin` | Bedrock player skin & avatar proxying | `true` | Auto-caches Floodgate skins for 3D avatars |

---

### Module-by-Module Configuration Guide

#### 1. Embedded Web Application & API (`modules/web.yml`)
Configures the Javalin embedded web server, admin password, and security restrictions.

```yaml
web:
  enabled: true
  auto_update_panel: true
  port: 8080
  force_lang: ""
  server_ip: "play.yourserver.com"
  allowed_origin: ""
  deposit_limit: 27
  public_features_text: |-
    Welcome to the server!
    - Jobs System (/jobs)
    - Auction House (/ah)
    - Guild Claims (/team)
admin-password: "gens"
```

| Key | Default | Explanation & Setup Instructions |
|---|---|---|
| `web.enabled` | `false` | Set to `true` to start the embedded web server on server boot. |
| `web.auto_update_panel` | `true` | Automatically extracts updated web assets from the plugin JAR upon version upgrades. Set to `false` if you customize frontend assets in `plugins/GensCore/web/`. |
| `web.port` | `8080` | Port for the web interface. Ensure this TCP port is forwarded or exposed in your firewall. |
| `web.force_lang` | `""` | Forces a specific language (`"fr"` or `"en"`). If left empty, the browser's language header is automatically used. |
| `web.server_ip` | `"localhost"` | Server address displayed on the public landing page. |
| `web.allowed_origin` | `""` | Restricts CORS requests to a specific domain (e.g., `"https://panel.genscore.com"`). Leave empty during local development to allow any origin. |
| `web.deposit_limit` | `27` | Maximum number of items a player can store in their web casino deposit box at any given time. |
| `web.public_features_text` | Text | Markdown text shown to visitors on the landing page before logging in. |
| `admin-password` | `"gens"` | Administrator password for accessing `http://<ip>:8080/admin`. When specified in plain text, GensCore automatically replaces it on startup with a secure BCrypt hash. To reset, replace the hash with a new plain text password and restart the server. |

---

#### 2. Discord Bot Integration (`modules/discord.yml`)
Bridges in-game chat, death events, and staff moderation with your Discord server via JDA 6.

```yaml
discord:
  bot_token: "YOUR_TOKEN_HERE"
  chat_channel_id: "123456789012345678"
  linked_role_id: "Linked"
  log_channel_id: ""
```

| Key | Default | Setup Instructions |
|---|---|---|
| `discord.bot_token` | `"YOUR_TOKEN_HERE"` | Bot token generated on the [Discord Developer Portal](https://discord.com/developers/applications). Mandatory intents: **Server Members Intent** and **Message Content Intent**. |
| `discord.chat_channel_id` | `"YOUR_CHANNEL_ID_HERE"` | Snowflake ID of the text channel where in-game chat messages are relayed and Discord messages are forwarded to Minecraft. |
| `discord.linked_role_id` | `"Linked"` | Role name or role ID assigned automatically to players who link their Discord account in-game. |
| `discord.log_channel_id` | `""` | Optional snowflake ID for security logs (bans, mutes, staff commands, economy anomalies). |

---

#### 3. Custom Spawners (`modules/spawners.yml`)
Governs stacked smart spawners, spawn frequency, and silk touch harvesting.

```yaml
spawners:
  delay: 25
  holograms: true
  hoppers: false
  max-stack: 100000
  upgrade-base-cost: 1000.0
  vanilla-require-silktouch: true
```

| Key | Default | How It Works & Optimization Advice |
|---|---|---|
| `spawners.delay` | `25` | Delay in server ticks between mob spawn attempts (20 ticks = 1 second). Increase to reduce entity load on high-population servers. |
| `spawners.holograms` | `true` | Displays dynamic floating text above spawners showing the mob type, current tier level, and stacked quantity. |
| `spawners.hoppers` | `false` | When set to `true`, entities spawned directly drop their items into attached hoppers beneath the spawner block. |
| `spawners.max-stack` | `100000` | Maximum number of spawner blocks that can be merged into a single block location to prevent world entity lag. |
| `spawners.upgrade-base-cost` | `1000.0` | Initial cost in server currency to upgrade a spawner from Level 1 to Level 2. Each subsequent tier scales mathematically. |
| `spawners.vanilla-require-silktouch` | `true` | If `true`, breaking a custom spawner without a Silk Touch pickaxe drops experience instead of the spawner item. |

---

#### 4. Instanced Chests / Lootr (`modules/lootr.yml`)
Transforms dungeon and structure chests into per-player instanced containers. Every player gets their own personal loot roll from the exact same chest.

```yaml
lootr:
  prevent-hopper: true
  prevent-break: false
  particles-enabled: true
  break-confirm-time: 3
  messages:
    break-confirm: "<yellow>Break it again within 3 seconds to confirm!"
    chest-broken: "<green>Lootr chest removed!"
    cannot-break: "<red>You cannot break this chest!"
  inventory:
    title: "<dark_gray>[<gold><dark_gray>] <yellow>Loot Chest"
```

| Key | Default | Purpose |
|---|---|---|
| `lootr.prevent-hopper` | `true` | Blocks hoppers, hopper minecarts, and redstone systems from siphoning instanced loot chest contents. |
| `lootr.prevent-break` | `false` | If `true`, instanced chests can never be broken in survival mode. |
| `lootr.particles-enabled` | `true` | Emits subtle particle swirls around unopened chests to indicate fresh loot for the observing player. |
| `lootr.break-confirm-time` | `3` | Seconds within which a player must hit the chest a second time to confirm destruction (prevents accidental loss). |
| `lootr.messages.*` | MiniMessage | Customizable chat messages with full Adventure MiniMessage formatting support. |
| `lootr.inventory.title` | MiniMessage | Custom GUI title displayed when opening an instanced loot container. |

---

#### 5. Economy & Auction House (`modules/economy.yml`)
Controls market dynamics, price inflation formulas, and player trading fees.

```yaml
shop:
  inflation_exponent: 0.5
ah:
  tax_percentage: 0.0
```

| Key | Default | Description |
|---|---|---|
| `shop.inflation_exponent` | `0.5` | The mathematical exponent governing dynamic price elasticity. A higher exponent makes prices drop faster when players oversell goods, and rise faster when players buy up stock. |
| `ah.tax_percentage` | `0.0` | Commission fee percentage deducted from the total sale price when a listing is purchased on `/ah`. Setting this to `5.0` imposes a 5% server sales tax. |

---

#### 6. Death Graves / Tombs (`modules/tomb.yml`)
Safeguards player inventories upon death by creating an interactive grave block.

```yaml
modules:
  tomb:
    block_type: CHEST
    store_xp: true
    xp_keep_percentage: 100
    expiration_time_seconds: 3600
    expiration_action: UNLOCK
    default_access: OWNER_ONLY
```

| Key | Default | Options & Description |
|---|---|---|
| `block_type` | `CHEST` | Block placed as the tomb marker. Valid materials: `CHEST`, `BARREL`, `PLAYER_HEAD`. Under Paper 26.3, player heads automatically render the deceased player's skin profile. |
| `store_xp` | `true` | If `true`, the player's experience points are preserved inside the tomb instead of dropping on the ground. |
| `xp_keep_percentage` | `100` | Percentage of experience retained upon death (0 to 100). |
| `expiration_time_seconds` | `3600` | Lifetime of the grave before the expiration action triggers (3600 = 1 hour). |
| `expiration_action` | `UNLOCK` | Action taken when expiration time is reached: `UNLOCK` (makes chest accessible to anyone), `DROP` (drops items on the ground), or `DESTROY` (permanently voids items). |
| `default_access` | `OWNER_ONLY` | Initial security policy: `OWNER_ONLY` (only the deceased player or staff with `genscore.tomb.admin` can loot) or `EVERYONE`. |

---

#### 7. Teleportation & Homes (`modules/teleport.yml`)
Governs home waypoint allowances and teleportation warmup delays.

```yaml
modules:
  home:
    default_max: 3
teleport-cooldown: 3
```

| Key | Default | Usage & Permissions |
|---|---|---|
| `modules.home.default_max` | `3` | Maximum number of `/sethome` locations allowed for standard players without extra permissions. |
| `teleport-cooldown` | `3` | Cooldown delay in seconds between teleportation commands (`/spawn`, `/home`, `/tpa`). |

---

#### 8. Daily Quests (`modules/quests.yml`)
Controls player progression through rotating daily quest assignments.

```yaml
quests:
  max_rerolls_per_day: 3
```

| Key | Default | Description |
|---|---|---|
| `quests.max_rerolls_per_day` | `3` | Maximum number of times a player can reject an undesirable daily quest to roll a new objective. |

---

#### 9. Server List MOTD (`modules/motd.yml`)
Customizes the multiplayer server list display with MiniMessage styling.

```yaml
motd:
  line1: "<dark_aqua><bold>A Minecraft Server"
  line2: "<gray><bold>>> <yellow>Powered by GensCore"
```

| Key | Default | Description |
|---|---|---|
| `motd.line1` | MiniMessage | Top line displayed in the Minecraft server list. Supports gradients, hex colors, and formatting tags. |
| `motd.line2` | MiniMessage | Bottom line displayed in the server list. |

---

#### 10. Tablist & Scoreboard (`modules/tabboard.yml`)
Governs real-time on-screen player feedback and leaderboard presentation.

```yaml
tabboard:
  scoreboard:
    title: "<gold><bold>Server"
    lines:
      - ""
      - "<gray>Money: <yellow>%money%$"
      - ""
  tablist:
    header: "\n<dark_aqua><bold>A Minecraft Server\n"
    footer: "\n<gray>Online: %online%\n"
    show_platform_prefix: true
    discord_not_linked: "<red>Discord not linked"
    discord_linked: "<green>Discord linked"
```

| Key | Default | Explanation |
|---|---|---|
| `scoreboard.title` | MiniMessage | Scoreboard sidebar header. |
| `scoreboard.lines` | List | Sidebar content lines. Supports native placeholders (`%money%`, `%online%`, `%ping%`, `%vault_eco_balance%`) and PlaceholderAPI. |
| `tablist.header` / `footer` | MiniMessage | Multi-line text banner pinned above and below the player list in the Tab menu. |
| `tablist.show_platform_prefix` | `true` | When Floodgate is detected, displays `[Bedrock]` or `[Java]` badges next to player names in Tab. |
| `tablist.discord_linked` / `not_linked` | MiniMessage | Text badge indicating whether the player has linked their Discord account. |

---

#### 11. Minigames & Casino (`modules/minigames.yml`)
Powers the Web Panel Fortune Wheel and 3-reel Slot Machine with customizable rewards.

```yaml
minigames:
  wheel:
    enabled: true
    rewards:
      '1':
        name: "16x Pain"
        command: "give %player% bread 16"
        chance: 32
        color: "#eab308"
      '2':
        name: "150$"
        command: "eco give %player% 150"
        chance: 25
        color: "#10b981"
      # ... (slices 3 to 8)
  casino:
    enabled: true
    jackpot_chance: 4
    medium_win_chance: 8
    small_win_chance: 20
  coinflip:
    enabled: true
    multiplier: 2
```

| Key | Default | Description |
|---|---|---|
| `wheel.enabled` | `true` | Enables the daily 24-hour Fortune Wheel on the Player Web Dashboard. |
| `wheel.rewards.*` | Map | 8 balanced slices. Each slice requires `name`, `command` executed by console upon winning, `chance` (percentages should sum to 100), and a hex `color` for web rendering. |
| `casino.enabled` | `true` | Enables the web-based 3-reel item slot machine. |
| `casino.jackpot_chance` | `4` | Percentage chance for a 3-of-a-kind jackpot (multiplies bet item count by 5x). |
| `casino.medium_win_chance` | `8` | Percentage chance for a 2-of-a-kind medium win (3x item multiplier). |
| `casino.small_win_chance` | `20` | Percentage chance for a small win (2x item multiplier). |
| `coinflip.enabled` | `true` | Enables the 50/50 CoinFlip minigame. |
| `coinflip.multiplier` | `2` | Payout multiplier awarded upon winning a CoinFlip round. |

---

#### 12. HeadDrop Module (`modules/headdrop.yml`)
Controls mob and player skull trophy collection.

```yaml
headdrop:
  chance: 10.0
```

| Key | Default | Description |
|---|---|---|
| `headdrop.chance` | `10.0` | Probability (percentage) that a killed entity (player or mob) drops their skull upon death. |

---

#### 13. BlueMap Territory Integration (`modules/bluemap.yml`)
Configures the live map endpoint embedded directly inside the Web Control Panel.

```yaml
bluemap:
  url: "http://localhost:8100"
```

| Key | Default | Setup Advice |
|---|---|---|
| `bluemap.url` | `"http://localhost:8100"` | Address of your BlueMap web server. If using a custom domain or reverse proxy, enter the public URL (e.g., `"https://map.yourserver.com"`). |

---

#### 14. Guilds & Co-Op Quests (`modules/teams.yml`)
Tracks automated weekly guild quest rotation timestamps.

```yaml
teams:
  last_rotation: 0
  active_quest: "weekly_1"
```

| Key | Default | Purpose |
|---|---|---|
| `teams.last_rotation` | Timestamp | Epoch millisecond timestamp of the last weekly quest rotation. Rotates automatically every 7 days. |
| `teams.active_quest` | String | Key of the current active cooperative guild objective. |

---

#### 15. Chat System (`modules/chat.yml`)
Controls custom join and leave announcements.

```yaml
chat:
  custom-join-messages: true
```

| Key | Default | Purpose |
|---|---|---|
| `chat.custom-join-messages` | `true` | Enables formatted welcome and departure announcements synchronized with LuckPerms ranks. |

---

#### 16. Dynamic Shop & Items Catalog (`modules/shop.yml`)
Defines item categories, materials, baseline buy/sell prices, and dynamic stock target quotas.

```yaml
categories:
  ores:
    displayName: "<gradient:#00c6ff:#0072ff><bold>Ores & Minerals</bold></gradient>"
    icon: "DIAMOND"
    items:
      DIAMOND:
        buyPrice: 150.0
        sellPrice: 50.0
        stock: 500
        targetStock: 500
```

| Key | Default | Purpose |
|---|---|---|
| `categories.<category_name>.displayName` | String | Formatted title displayed in `/shop` menu. |
| `categories.<category_name>.icon` | Material | Vanilla material identifier used as the category icon. |
| `categories.<cat>.items.<mat>.buyPrice` | Double | Base unit purchase price from the server shop. |
| `categories.<cat>.items.<mat>.sellPrice` | Double | Base unit sale price to the server shop (~25-35% of buyPrice). |
| `categories.<cat>.items.<mat>.stock` | Integer | Current dynamic stock units. |
| `categories.<cat>.items.<mat>.targetStock` | Integer | Equilibrium target capacity. Purchasing decreases stock (increasing prices); selling adds stock (decreasing prices). |

---

### Network Ports & Reverse Proxy Deployment

If running GensCore on a dedicated host or VPS:

| Port | Protocol | Purpose | Requirement |
|---|---|---|---|
| **25565** | TCP/UDP | Minecraft Game Server | Mandatory for player connection |
| **8080** | TCP | GensCore Web Panel & API | Mandatory if Web Panel is enabled |
| **8100** | TCP | BlueMap Live Map (Optional) | Required only if BlueMap is enabled |

#### Recommended Nginx Reverse Proxy for SSL (`https://panel.yourserver.com`)
```nginx
server {
    server_name panel.yourserver.com;

    location / {
        proxy_pass http://127.0.0.1:8080;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

---

*GensCore is maintained and built for performance, security, and next-generation Minecraft survival.*

