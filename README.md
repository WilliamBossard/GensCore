# GensCore

![GensCore Banner](https://img.shields.io/badge/GensCore-Paper%20%26%20Folia-green.svg) ![Java Version](https://img.shields.io/badge/Java-25+-blue.svg) ![Minecraft Version](https://img.shields.io/badge/Minecraft-26.2+-red.svg)

**GensCore** is a comprehensive core plugin developed specifically for the Survival/Faction server *GensBien*. It bundles all the essential server mechanics into a single, optimized plugin, offering excellent performance while avoiding the need to manage dozens of separate small plugins. It is fully compatible with **Paper** and **Folia**!

## Included Features

This plugin is modular and manages the following server aspects:
* **Economy & Jobs:** Money management (`/money`, `/pay`) and job progression.
* **Guilds (Teams):** Guild creation, invitation, and management (`/team`, `/guild`, `/guilde`).
* **Quests & Statistics:** Daily quests and global player statistics.
* **Web Panel & BlueMap:** A fully responsive Web administration panel for PC and Mobile with BlueMap integration, running via Javalin.
* **Discord Bot:** Full synchronization with Discord via the JDA API. Includes auto-deleting `!link` commands for account linking, server status (🟢/🔴), and rich embeds.
* **Cross-play Support:** Integrated support for Bedrock players via Geyser and Floodgate (custom placeholders and Bedrock-specific prefixes).
* **Moderation:** Basic commands (`/mute`, `/ban`, `/freeze`, `/openinv`).
* **Survival Utilities:** Essential commands like `/spawn`, `/sethome`, `/back`, `/tpa`, `/ec`.
* **Loot & Spawners:** Custom loot system and spawner management.
* **Modern Chat System:** Fully migrated to MiniMessage (Kyori Adventure) for a robust and legacy-free chat formatting experience.

---

## Required Dependencies

GensCore relies on the following plugins to function correctly:
* **[LuckPerms](https://luckperms.net/):** Used to manage player ranks, prefixes in chat, and the Scoreboard. Without LuckPerms, GensCore cannot resolve player display names and permissions correctly.
* **[Vault](https://dev.bukkit.org/projects/vault):** The standard economy and permissions API for Bukkit. GensCore hooks into Vault to allow compatibility with other plugins that might need to read or modify player balances.

### Optional Integrations
* **[GeyserMC & Floodgate](https://geysermc.org/):** Highly recommended if you allow Bedrock players. GensCore automatically detects Floodgate to assign specific prefixes and properly format Bedrock UUIDs.
* **[BlueMap](https://bluemap.bluecolored.de/):** If installed, the web panel integrates a live map view for moderators.

### Compatibility: Paper & Folia Only
**GensCore is STRICTLY compatible with PaperMC and Folia.** 
It will **not** start on a standard Spigot server. The plugin relies on modern Paper-exclusive APIs such as *Kyori Adventure (MiniMessage)* for text components, asynchronous Folia events, and the *Cloud Command Framework* for Paper.

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

---

## Internal Code Architecture

For developers wishing to modify or understand the plugin, here are the pillars of its internal architecture:

1. **The `ModuleManager`**: This is the core system. Instead of being a monolithic plugin, GensCore is divided into dozens of "Modules" (such as `EconomyModule`, `TeamsModule`, `DiscordModule`). They all inherit from the `Module` interface and can be dynamically enabled or disabled.
2. **Local Database (`DatabaseManager.java`)**: To remain autonomous, GensCore doesn't use MySQL but a **local SQLite database** (`genscore.db`), which saves everything: player balances, guild inventories, quest progression, and connection histories.
3. **REST Web API (`WebManager.java` & `WebPlayerAPI.java`)**: Javalin is used in the background to spin up an HTTP micro-server on port 8080. The React code in the `/web-panel` folder is built and placed into `/src/main/resources/public/`. Every time an admin visits the web page, Javalin serves these HTML/JS files and communicates with React via a JSON API.

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
The final file will be located in `target/GensCore-1.0-SNAPSHOT.jar`.

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
| `/login <password>` | — | Log in to your account |
| `/register <password> <confirm>` | — | Create an account |
| `/changemdp <old> <new>` | — | Change your password |
| `/resetmdp <player>` | `genscore.admin` | Reset another player's password |

### Economy
| Command | Permission | Description |
|---------|-----------|-------------|
| `/money` | — | View your balance |
| `/money <player>` | `genscore.admin` | View another player's balance |
| `/pay <player> <amount>` | — | Transfer money |
| `/addmoney <player> <amount>` | `genscore.admin` | Add money to a player |
| `/removemoney <player> <amount>` | `genscore.admin` | Remove money from a player |

### Teleportation
| Command | Permission | Description |
|---------|-----------|-------------|
| `/sethome [name]` | — | Set a home |
| `/home [name]` | — | Teleport to a home |
| `/delhome [name]` | — | Delete a home |
| `/spawn` | — | Teleport to spawn |
| `/setspawn` | `genscore.admin` | Set the spawn point |
| `/tpa <player>` | — | Request a teleport |
| `/tpaccept` | — | Accept a TPA request |
| `/tpdeny` | — | Deny a TPA request |
| `/back` | — | Return to your last location |

### Moderation
| Command | Permission | Description |
|---------|-----------|-------------|
| `/ban <player> [reason]` | `genscore.ban` | Permanently ban a player |
| `/unban <player>` | `genscore.ban` | Unban a player |
| `/mute <player> [duration] [reason]` | `genscore.mute` | Mute a player |
| `/unmute <player>` | `genscore.mute` | Unmute a player |
| `/kick <player> [reason]` | `genscore.kick` | Kick a player |
| `/freeze <player>` | `genscore.freeze` | Freeze/unfreeze a player |
| `/openinv <player>` | `genscore.openinv` | Open a player's inventory |

### Teams / Guilds
| Command | Permission | Description |
|---------|-----------|-------------|
| `/team create <name>` | — | Create a team |
| `/team invite <player>` | — | Invite a player |
| `/team leave` | — | Leave your team |
| `/team disband` | — | Disband your team |
| `/team info` | — | View team info |

### Other
| Command | Permission | Description |
|---------|-----------|-------------|
| `/ec` | — | Open your enderchest |
| `/feed` | `genscore.admin` | Feed yourself |
| `/genscore reload` | `genscore.admin` | Reload plugin config |

> **Note:** Players with operator status (`/op`) automatically inherit the `genscore.admin` permission.

---

## Lootr Integration

GensCore integrates configuration options for the [Lootr](https://www.curseforge.com/minecraft/mc-mods/lootr) mod/plugin (per-player loot chests). If Lootr is installed, you can configure its behaviour in the main `config.yml`:

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
