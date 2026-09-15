# GensCore - Comprehensive Feature, Command & Web Documentation

Welcome to the official documentation for **GensCore**, the all-in-one survival/faction core plugin engineered for **PaperMC & Folia (Minecraft 26.3+)** running on **Java 25 (LTS)**.

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
   - [Bedrock & Floodgate Cross-Play Features](#bedrock--floodgate-cross-play-features)
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

- **Engine Target:** **PaperMC** and **Folia** (Minecraft 26.3+).
- **Runtime:** **Java 25 (LTS)** with Classfile 69 compatibility and ASM 9.10.1 shading.
- **Concurrency & Folia Threading:** Utilizes *FoliaLib* for regional multi-threading. Global actions run on `GlobalRegionScheduler`, chunk tasks on `RegionScheduler`, and player actions on `EntityScheduler`. Console commands and inventory manipulations are isolated to prevent cross-thread synchronization crashes.
- **Autonomous Database:** Embedded **SQLite** engine operating in **WAL (Write-Ahead Logging)** mode (`PRAGMA journal_mode = WAL; PRAGMA synchronous = NORMAL;`). No external SQL server is required, though transactions are thread-safe and isolated.
- **Cross-Play Ready (Geyser & Floodgate):** Bedrock players are natively detected. In-game menus automatically open as native Bedrock dialogs (Cumulus Forms API) on Bedrock clients, while Java players receive virtual chest GUIs. Bedrock custom skins and heads are supported via the built-in Web Avatar API.
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
Full-featured clan and guild system with private team chat, shared banks, and cooperative guild quests.

| Command | Arguments | Permission | Default | Description |
|---|---|---|---|---|
| `/team` | *None* | *None* | Everyone | Opens the interactive Guild Management GUI. *(Aliases: `/guild`, `/guilde`, `/teams`)* |
| `/team create` | `<name>` | *None* | Everyone | Creates a new guild with the sender as leader (max 16 characters). |
| `/team invite` | `<player>` | *None* | Everyone | Invites a player to join your guild (leader only). |
| `/team accept` | *None* | *None* | Everyone | Accepts a pending guild invitation. |
| `/team quest` | *None* | *None* | Everyone | Opens the shared Guild Quests progression menu. |

**In-GUI Guild Features:**
- View online and offline members with roles.
- Guild vault / shared bank deposit and withdrawal.
- Team progression and leveling.
- Kick members, transfer leadership, or disband guild.

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

### Bedrock & Floodgate Cross-Play Features
- Automatic detection of Bedrock players joining via GeyserMC / Floodgate.
- **Cumulus Forms API:** Opening menus (`/team`, `/jobs`, `/shop`) automatically serves native Bedrock window forms instead of Java inventory containers, avoiding desyncs and touch-screen misclicks.
- **Bedrock Skin & Head Support:** Head requests (`/api/head/{name}`) resolve Geyser XUIDs to fetch authentic Bedrock skins instead of fallback Steve avatars.
- **Bedrock Prefix:** Configurable prefix (e.g. `[Bedrock]` or `.`) automatically handled in commands and LuckPerms group lookups.

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
- **Live Module Switchboard:** Toggle any of the 28 modules on or off instantly with a click—no server reboot required.
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
Upon first run, GensCore creates the following directory structure inside `plugins/GensCore/`:

```
plugins/GensCore/
├── config.yml              # Main configuration (language, database settings)
├── modules.yml             # Global enable/disable switches for all modules
├── genscore.db             # Local SQLite database (WAL mode)
├── lang/
│   ├── fr_FR.yml           # French localization strings
│   └── en_US.yml           # English localization strings
├── menus/
│   └── default.yml         # Custom GUI menu definitions
├── modules/
│   ├── web.yml             # Web panel port, admin password, CORS settings
│   ├── discord.yml         # Discord bot token, channels, embed settings
│   ├── minigames.yml       # Wheel of fortune rewards and chances
│   ├── lootr.yml           # Lootr chest behavior and particle settings
│   ├── shop.yml            # Dynamic shop items, base prices, categories
│   └── ...
└── web/                    # Extracted React / Vite frontend files (index.html, assets)
```

### Network Ports & Port Forwarding
If running GensCore on a dedicated server or VPS (e.g., Pterodactyl, OVH, Hetzner):

| Port | Protocol | Usage | Requirement |
|---|---|---|---|
| **25565** | TCP/UDP | Minecraft Game Server | Mandatory for player connection |
| **8080** | TCP | GensCore Web Panel & API | Mandatory if Web Panel is enabled |
| **8100** | TCP | BlueMap Live Map (Optional) | Required only if BlueMap is used |

> [!TIP]
> **Reverse Proxy / SSL:** To expose your Web Panel securely with HTTPS, you can place an Nginx, Caddy, or Cloudflare reverse proxy in front of port `8080`. Set `web.allowed_origin` in `modules/web.yml` if you configure a custom domain name.

### Discord Bot Setup
1. Visit the [Discord Developer Portal](https://discord.com/developers/applications) and create a New Application.
2. Under the **Bot** tab, generate a **Bot Token**.
3. Enable **Server Members Intent** and **Message Content Intent**.
4. Open `plugins/GensCore/modules/discord.yml` and paste your token:
   ```yaml
   bot_token: "YOUR_DISCORD_BOT_TOKEN_HERE"
   channel_id: "YOUR_STATUS_OR_CHAT_CHANNEL_ID"
   ```
5. Restart your server or run `/module discord on`.

---

*GensCore is maintained and built for performance, security, and next-generation Minecraft survival.*
