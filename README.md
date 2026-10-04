# GensCore

![GensCore Banner](https://img.shields.io/badge/GensCore-Paper%20%26%20Folia-green.svg) ![Java Version](https://img.shields.io/badge/Java-25+-blue.svg) ![Minecraft Version](https://img.shields.io/badge/Minecraft-26.1+-red.svg) ![Status](https://img.shields.io/badge/Status-Release%20v1.0.3-blue.svg) ![Tests](https://img.shields.io/badge/Tests-81%20passing-brightgreen.svg)

**GensCore** is a comprehensive, production-ready core plugin developed specifically for the Survival/Faction server *GensBien*. It bundles all essential server mechanics into a single, high-performance plugin, eliminating the complexity and overhead of managing dozens of fragmented plugins. Engineered natively for **PaperMC** and **Folia (Minecraft 26.1+)** running on **Java 25 LTS**!

> **Official Documentation Website:** [**williambossard.github.io/GensCore**](https://williambossard.github.io/GensCore/)  
> **Full offline reference:** [**Comprehensive Documentation (DOCUMENTATION.md)**](DOCUMENTATION.md).

## Included Features (30 Modules)

GensCore is 100% modular with zero mandatory external dependencies:
* **Dual-Engine Database Persistence:** Runs out of the box on embedded **SQLite** (WAL mode) or connects to remote **MySQL / MariaDB** with HikariCP pooling, dynamic SQL dialect translation, and automatic safe fallback to SQLite if remote DB is unreachable.
* **Economy, Dynamic Shop & Auction House:** Centralized currency (`/money`, `/pay`, `/baltop`), dynamic mathematical supply/demand shop (`/shop`, 319 survival items), and player-to-player marketplace (`/ah`).
* **Guilds & Claims (Teams):** Complete clan system (`/team`) featuring shared treasury ($ or XP levels), territory chunk protection ($16 \times 16$), real-time 3D BlueMap markers with custom colors, 10 team upgrade trees, shared vaults (`/team vault`), and web roster administration.
* **Jobs & Daily Quests:** 6 professions (Miner, Lumberjack, Hunter, Farmer, Fisherman, Builder) with anti-farm block caching, paired with a daily rotating quest system (`/quests`).
* **Solo Quest Perks & Masteries:** Individual progression rewarding completed quests (`/perks`), unlocking abilities like instant `/autosmelt`, 5-block `/magnet`, and extra homes.
* **Container Security & Grief Shield:** Container locking (`/lock`, `/lock guild`), anti-hopper protection, and anti-piston push/retraction shields.
* **Web Panel & Player Portal:** Fully responsive React 18 / Vite web application served by embedded Javalin, featuring live server telemetry, console, player management, module toggles, and interactive web minigames (Wheel of Fortune, Casino Slots).
* **Discord Integration:** Two-way synchronization via JDA (Java Discord API) with account linking (`!link`), chat bridge, and staff action embeds.
* **Cross-Play Bedrock Ready:** Native Geyser & Floodgate integration with automatic Cumulus Forms menus, Bedrock prefixes, and high-res avatar rendering.
* **Multi-Protocol Compatibility:** Decoupled ViaVersion integration detecting client protocols (`/check <player>`), mapping materials for legacy versions, and providing PlaceholderAPI expansions.
* **Death Graves (Tombs) & Instanced Lootr:** Holographic player graves preserving inventory/XP and per-player instanced dungeon chests.
* **System Telemetry & Administration:** In-game health monitoring (`/gens status`, `/gens db`) and live hot-swappable module toggling (`/module`).
* **Modern MiniMessage Formatting:** Pure Kyori Adventure chat and tablist styling with zero legacy formatting codes.

---

## Dependencies & Integrations

GensCore is designed to be fully **autonomous**. It can run standalone without requiring any mandatory external plugins, but seamlessly integrates with the standard server ecosystem:

### Recommended Integrations (Soft Dependencies)
* **[Vault](https://dev.bukkit.org/projects/vault):** 
  - **If Vault is installed:** GensCore automatically registers its internal economy engine into Vault's `Economy` service provider (`GensVaultEconomy`). This enables any other 3rd-party plugins on your server (like chest shops, claim plugins, or auction addons) to seamlessly access and modify GensCore player balances.
  - **If Vault is NOT installed:** GensCore's economy continues to work 100% autonomously! All internal economy features (`/money`, `/balance`, `/baltop`, `/pay`, `/eco`, Dynamic Shop, and Auction House) persist to the local SQLite database without interruption.
* **[LuckPerms](https://luckperms.net/):** 
  - **If LuckPerms is installed:** GensCore integrates with the LuckPerms API to fetch player ranks, primary groups, weights, and prefixes for the custom Chat and Scoreboard/Nametags.
  - **If LuckPerms is NOT installed:** GensCore gracefully falls back to default Bukkit permission checks (e.g. `genscore.admin`) and standard display names.

### Optional Integrations
* **[GeyserMC & Floodgate](https://geysermc.org/):** Highly recommended if you allow Bedrock cross-play. GensCore automatically detects Floodgate to open native Bedrock Forms (Cumulus API), assign platform prefixes (`[Bedrock]`), resolve Bedrock UUIDs/skins, and optimize inventory interactions.
* **[ViaVersion & ViaBackwards](https://viaversion.com/):** Multi-version interop bridge. Automatically detects client versions (e.g. 26.2, 1.21.x, 1.20.x), maps 26.3 menu materials, supplies `%genscore_client_version%` placeholders, and enables the `/check <player>` staff command.
* **[BlueMap](https://bluemap.bluecolored.de/):** If installed, the web panel integrates a live map view for administrators.
* **[PlaceholderAPI](https://placeholderapi.com/):** Integrated with the official `GensCoreExpansion` (%genscore_balance%, %genscore_client_version%, %genscore_guild%, etc.).

### Compatibility: Paper & Folia Only
**GensCore is STRICTLY compatible with PaperMC and Folia (Minecraft 26.1+, Java 25+).** 
It will **not** start on a standard legacy Spigot server. The plugin relies on modern Paper APIs such as *Kyori Adventure (MiniMessage)* for text components, regional multi-threaded Folia schedulers (*FoliaLib*), and the *Cloud Command Framework* for Paper.

---

## Configuration & Networks (Ports)

To avoid hardcoded secrets or addresses, GensCore relies on a modular configuration system located in `plugins/GensCore/`. The main `config.yml` handles core settings (database, language), while a `modules/` folder contains specific files for each feature.

### Web Panel
The Web administration panel runs locally on your Minecraft server, alongside the game, using **Javalin**. It has been built with **Vite** and **React** for a fully responsive mobile-friendly experience.
- The default port is **8080**.
- It can be modified via the `port: 8080` option in `modules/web.yml`.
- If your Minecraft server runs on a host (like Pterodactyl), you will need to open a second free port and specify it in the configuration for the Web Panel to be accessible.

**Web Panel Security (Admin Password):**
By default, the password is set to `"gens"`. Upon the first startup, GensCore will automatically hash this password in `web.yml` using **BCrypt** to secure it. If you forget your password, simply open `modules/web.yml`, replace the long hashed string with a new password in plain text, and restart the server. The plugin will automatically hash it again.

### Discord Bot
The Discord module requires a bot Token. For security reasons, this Token must **never** be shared or hardcoded in the Java source code.
- Provide it via the `bot_token: "YOUR_TOKEN"` option in `modules/discord.yml`.
- The bot ensures clean server shutdowns without throwing `zip file closed` errors by disconnecting its WebSockets properly before the Minecraft server halts.

### Database Engine
GensCore uses a dual-engine database layer managed by HikariCP:
- **SQLite (Default):** Zero setup required. All data is saved locally in `plugins/GensCore/genscore.db`.
- **MySQL / MariaDB (Optional):** Can be activated in `config.yml` (`database.type: "mysql"`). Ideal for external databases or containerized setups.
- **Automatic Fallback:** If the external database server is unreachable or offline, GensCore automatically falls back to local SQLite without interrupting server startup.

```yaml
database:
  type: "sqlite" # "sqlite" or "mysql"
  sqlite:
    file: "genscore.db"
  mysql:
    host: "127.0.0.1"
    port: 3306
    database: "genscore"
    username: "genscore_user"
    password: "change_me"
```

---

## Internal Code Architecture

For developers wishing to modify or understand the plugin, here are the pillars of its internal architecture:

1. **The `ModuleManager`**: This is the core system. Instead of being a monolithic plugin, GensCore is divided into dozens of "Modules" (such as `EconomyModule`, `TeamsModule`, `DiscordModule`). They all inherit from the `Module` interface and can be dynamically enabled or disabled.
2. **Database Engine (`DatabaseManager.java`)**: By default, GensCore uses a local SQLite database (`genscore.db`) with HikariCP connection pooling. An optional external MySQL/MariaDB database can be enabled with automatic schema migration, SQL dialect translation (`adaptQuery`), and resilient automatic fallback to SQLite upon connection failure.
3. **REST Web API (`WebManager.java` & `WebPlayerAPI.java`)**: Javalin is used in the background to spin up an HTTP micro-server on port 8080. The React code in the `/web-panel` folder is built and placed into `/src/main/resources/public/`. Every time an admin visits the web page, Javalin serves these HTML/JS files and communicates with React via a JSON API.
4. **Metrics & Health Service (`MetricsService.java`)**: Collects real-time JVM telemetry, Folia tick rate, database connection pool statistics, and active module status exposed via `/api/metrics` and in-game commands.
5. **Command Engine & Cloud Patch (`CommandManager.java`)**: Powered by the Incendo Cloud Command Framework with asynchronous coordination and Brigadier suggestions. Includes an architecture patch (`ItemStackParser.java`) to ensure flawless item argument parsing on Paper 26.3 until Cloud 2.1+ is released.

---

## Build & Installation

If you wish to compile GensCore, the ideal environment is **Java 25**, managed by **Maven** and **Node.js/npm** (for the web panel).

### Manual Build (Local)
```bash
git clone https://github.com/WilliamBossard/GensCore.git
cd GensCore
# First, build the web panel
cd web-panel
npm install
npm run build
cd ..
# Then, package the Java plugin
mvn clean package -DskipTests
```
The final file will be located in `target/GensCore-1.0.3.jar`.

### Build via GitHub Actions
GensCore has a configured Workflow (in `.github/workflows/release.yml`).
1. Go to your GitHub Repository, in the **"Actions"** tab.
2. Select **"Build and Release GensCore"** on the left.
3. Click on **"Run workflow"**.
4. GitHub will compile the project (both the React panel and the Java plugin) on its servers (for free) in a few seconds, then create a **Draft Release**.
5. All you have to do is download the `.jar` file from the GitHub Release without needing to install Java, Maven, or Node.js on your computer!

---
*Plugin developed with passion for GensBien.*

---

## Language Support

GensCore fully supports multiple languages. The active language is set in `config.yml`:

```yaml
lang: fr_FR  # or en_US
```

Available language files are located in `plugins/GensCore/lang/`. Each file contains all player-facing messages that can be freely customized. Supported languages out of the box:

| Code | Language |
|------|----------|
| `fr_FR` | French |
| `en_US` | English |

---

## Automatic Update Checker

On startup and when a player with the `genscore.admin` permission (or operator status) joins the server, GensCore automatically checks for a new release on [GitHub](https://github.com/WilliamBossard/GensCore/releases).

- If a new version is available, a notification is sent in **chat** (visible to admins/ops only) and logged to the **server console**.
- The check is non-blocking and runs asynchronously to avoid any impact on startup time.
- The installed version is compared against the latest GitHub Release tag (e.g. `1.0.0` vs `1.0.1`).

---

## Commands & Permissions

### Authentication
| Command | Permission | Description |
|---------|-----------|-------------|
| `/register <password> <confirm>` | — | Register your account (hashed via BCrypt) |
| `/login <password>` | — | Log in to your account *(Alias: `/l`)* |
| `/changemdp <old> <new>` | — | Change your account password *(Alias: `/changepassword`)* |
| `/resetmdp <player>` | `genscore.admin` | Force-reset a player's password |

### Economy, Dynamic Shop & Auction House
| Command | Permission | Description |
|---------|-----------|-------------|
| `/money` | — | View your current balance *(Alias: `/balance`)* |
| `/money <player>` | `genscore.admin` | View another player's balance |
| `/baltop` | — | Display top server balances leaderboard |
| `/pay <player> <amount>` | — | Transfer money to another online player |
| `/eco <set\|give\|take\|reset>` | `genscore.admin` | Administer player balances |
| `/shop` | — | Open the dynamic mathematical server shop GUI |
| `/ah` | — | Open the Auction House marketplace *(Alias: `/hdv`)* |
| `/ah sell <price>` | — | List the item in hand on the Auction House |

### Guilds & Clans (`/team`)
| Command | Permission | Description |
|---------|-----------|-------------|
| `/team` | — | Open the Guild management GUI *(Aliases: `/guild`, `/teams`)* |
| `/team create <name>` | — | Create a new guild (max 16 chars) |
| `/team invite <player>` | — | Invite a player to join (Admin/Leader) |
| `/team accept` | — | Accept a pending guild invitation |
| `/team kick <player>` | — | Kick a member from the guild (Admin/Leader) |
| `/team promote/demote` | — | Promote to Admin or demote to Member (Leader) |
| `/team leave` | — | Leave current guild (Leader must disband) |
| `/team disband` | — | Permanently disband the guild and release claims |
| `/team quest` | — | Open the weekly co-op guild quest menu |
| `/team upgrades` | — | Open the 10 guild perks upgrade shop |
| `/team sethome` / `/team home` | — | Set or teleport to shared guild waypoint |
| `/team vault` | — | Open communal guild virtual chest *(Aliases: `/team coffre`, `/team chest`)* |
| `/team deposit/withdraw` | — | Deposit or withdraw dollars ($) from guild bank |
| `/team depositxp/withdrawxp` | — | Deposit or withdraw XP levels from guild bank |
| `/team claim` / `/team unclaim` | — | Claim or unclaim current chunk ($16 \times 16$) |
| `/team color <#hex>` | — | Set guild territory marker color on BlueMap |

### Jobs, Daily Quests & Solo Perks
| Command | Permission | Description |
|---------|-----------|-------------|
| `/jobs` | — | Open the Jobs & Professions menu *(Alias: `/job`)* |
| `/quests` | — | Open the Daily Quests menu *(Aliases: `/quest`, `/quete`)* |
| `/perks` | — | Open the Solo Quest Perks GUI *(Aliases: `/bonus`, `/passe`)* |
| `/autosmelt` | — | Toggle instant raw ore auto-smelting On/Off |
| `/magnet` | — | Toggle 5-block ground item magnet On/Off |

### Teleportation & Navigation
| Command | Permission | Description |
|---------|-----------|-------------|
| `/spawn` | `genscore.spawn` | Teleport to world spawn |
| `/setspawn` | `genscore.admin` | Set global server spawn location |
| `/sethome [name]` | `genscore.home` | Set a personal home waypoint |
| `/home [name]` | `genscore.home` | Teleport to a personal home waypoint |
| `/delhome [name]` | `genscore.home` | Delete a personal home waypoint |
| `/back` | `genscore.back` | Return to last death or teleport location |
| `/tpa <player>` | `genscore.tpa` | Send a teleport request to a player |
| `/tpaccept` | `genscore.tpa` | Accept incoming teleport request |
| `/tpdeny` / `/tpadeny` | `genscore.tpa` | Deny incoming teleport request |
| `/tpacancel` | `genscore.tpa` | Cancel your outgoing teleport request |

### Container Security & Locks
| Command | Permission | Description |
|---------|-----------|-------------|
| `/lock [private]` | — | Lock a container (chest, barrel, furnace, shulker) |
| `/lock unlock` | — | Remove the lock from your container |
| `/lock guild` | — | Share container access with all guild members |

### Survival Utilities
| Command | Permission | Description |
|---------|-----------|-------------|
| `/ec [player]` | `genscore.ec` | Open virtual Ender Chest *(Alias: `/enderchest`)* |
| `/craft` | `genscore.craft` | Open virtual 3x3 workbench *(Alias: `/workbench`)* |
| `/anvil` | `genscore.anvil` | Open virtual Anvil |
| `/enchant` | `genscore.enchant` | Open virtual Enchanting Table *(Alias: `/enchanttable`)* |
| `/feed` | `genscore.feed` | Satisfy hunger bar |

### Staff & Moderation
| Command | Permission | Description |
|---------|-----------|-------------|
| `/ban <player> [time] [reason]` | `genscore.ban` | Ban a player permanently or temporarily |
| `/unban <player>` | `genscore.ban` | Unban a player |
| `/mute <player> [time] [reason]` | `genscore.mute` | Mute a player in public chat |
| `/unmute <player>` | `genscore.mute` | Unmute a player |
| `/kick <player> [reason]` | `genscore.kick` | Kick an online player from the server |
| `/freeze <player>` | `genscore.freeze` | Freeze a player for screenshare checks |
| `/openinv <player>` | `genscore.openinv` | Live inspect inventory & armor *(Alias: `/invsee`)* |
| `/check <player>` | `genscore.check` | Technical dossier (Bedrock/Java, version, ping, coords) *(Alias: `/whois`)* |

### Discord & Web Casino Bridge
| Command | Permission | Description |
|---------|-----------|-------------|
| `/discord link` | — | Generate a 6-character linking code for Discord (`!link <code>`) |
| `/discord reg` | — | Display Discord linking instructions and invite |
| `/web` | — | Help message for web casino deposit inventory |
| `/web deposit` | — | Deposit held item into web casino betting inventory |
| `/web withdraw` | — | Retrieve won items from the web slot machine |

### Administration & Diagnostics
| Command | Permission | Description |
|---------|-----------|-------------|
| `/gens status` | `genscore.admin` | Live telemetry: platform, JVM memory, CPU load, Folia TPS, active modules |
| `/gens db` | `genscore.admin` | Live database status: storage engine (SQLite/MySQL) and HikariCP pool stats |
| `/module <name> <on\|off>` | `genscore.admin` | Dynamically toggle any of the 30 modules at runtime |
| `/menu [name]` | — | Open a custom YAML inventory menu from `plugins/GensCore/menus/` |
| `/spawner give <player> <type>` | `genscore.admin.spawner` | Give custom stacked smart spawner block |

> **Note:** Players with operator status (`/op`) automatically inherit the `genscore.admin` permission.
> 
> **For the complete list of all 30 modules, advanced subcommands, bypass permissions, and ready-to-use LuckPerms templates, see [DOCUMENTATION.md](DOCUMENTATION.md) and the [Online Docs](https://williambossard.github.io/GensCore/).**

---

## Lootr Integration

GensCore completely integrates a custom Lootr-like system (per-player loot chests) directly into the core, meaning **you do not need to install any external mods or plugins**. Chest data and per-player loot inventories are persisted in the configured database (SQLite or MySQL, in `lootr_chests` & `lootr_player_chests` tables). If a legacy `chests.yml` file is detected on startup, it will be migrated automatically and transparently.

You can configure its behaviour in the `modules/lootr.yml` file:

```yaml
lootr:
  prevent-hopper: true
  prevent-break: false
  particles-enabled: true
  break-confirm-time: 3
```

---

## Server Wipe

> [!WARNING]
> **This action is irreversible.** A server wipe permanently deletes all player data from the database (homes, economy, stats, auth, quests, etc.). It does **not** delete world files.

The Web Admin Panel includes a "Wipe Server Data" button at the bottom of the Configuration page. To prevent accidents:
1. The button is prominently marked in red.
2. Clicking it requires entering the **admin password** to confirm.
3. The console will log a confirmation message when the wipe completes.

To perform a wipe from the console, the Web Panel must be running and accessible.
