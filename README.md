# GensCore

![GensCore Banner](https://img.shields.io/badge/GensCore-Paper_Plugin-green.svg) ![Java Version](https://img.shields.io/badge/Java-25+-blue.svg) ![Minecraft Version](https://img.shields.io/badge/Minecraft-26.1+-red.svg)

**GensCore** is a comprehensive core plugin developed specifically for the Survival/Faction server *GensBien*. It bundles all the essential server mechanics into a single, optimized plugin, offering excellent performance while avoiding the need to manage dozens of separate small plugins.

## Included Features

This plugin is modular and manages the following server aspects:
* **Economy & Jobs:** Money management (`/money`, `/pay`) and job progression.
* **Guilds (Teams):** Guild creation, invitation, and management (`/team`).
* **Quests & Statistics:** Daily quests and global player statistics.
* **Web Panel & BlueMap:** A reactive Web administration panel with BlueMap integration, running via Javalin.
* **Discord Bot:** Full synchronization with Discord via the JDA API.
* **Moderation:** Basic commands (`/mute`, `/ban`, `/freeze`, `/openinv`).
* **Survival Utilities:** Essential commands like `/spawn`, `/sethome`, `/back`, `/tpa`, `/ec`.
* **Loot & Spawners:** Custom loot system and spawner management.

---

## Required Dependencies

GensCore relies on the following plugins to function correctly:
* **[LuckPerms](https://luckperms.net/):** Used to manage player ranks, prefixes in chat, and the Scoreboard. Without LuckPerms, GensCore cannot resolve player display names and permissions correctly.
* **[Vault](https://dev.bukkit.org/projects/vault):** The standard economy and permissions API for Bukkit. GensCore hooks into Vault to allow compatibility with other plugins that might need to read or modify player balances.

> **Note:** If either of these plugins is missing from your `plugins/` folder, GensCore will refuse to load to prevent severe errors during runtime.

---

## Configuration & Networks (Ports)

To avoid hardcoded secrets or addresses, GensCore relies on a modular configuration system located in `plugins/GensCore/`. The main `config.yml` handles core settings (database, language), while a `modules/` folder contains specific files for each feature.

### Web Panel
The Web administration panel runs locally on your Minecraft server, alongside the game, using **Javalin**.
- The default port is **8080**.
- It can be modified via the `port: 8080` option in `modules/web.yml`.
- If your Minecraft server runs on a host (like Pterodactyl), you will need to open a second free port and specify it in the configuration for the Web Panel to be accessible.

**Web Panel Security (Admin Password):**
By default, the password is set to `"gens"`. Upon the first startup, GensCore will automatically hash this password in `web.yml` using **BCrypt** to secure it. If you forget your password, simply open `modules/web.yml`, replace the long hashed string with a new password in plain text, and restart the server. The plugin will automatically hash it again.

### Discord Bot
The Discord module requires a bot Token. For security reasons, this Token must **never** be shared or hardcoded in the Java source code.
- Provide it via the `bot_token: "YOUR_TOKEN"` option in `modules/discord.yml`.

### BlueMap
GensCore's Web Panel integrates a BlueMap iframe to display the live map directly to moderators.
- It fetches the local URL via `url: "http://localhost:8100"`, which is customizable in `modules/bluemap.yml`.
- You can completely disable the BlueMap integration via the Web Panel's Modules tab (which updates `modules.yml`).

---

## Internal Code Architecture

For developers wishing to modify or understand the plugin, here are the pillars of its internal architecture:

1. **The `ModuleManager`**: This is the core system. Instead of being a monolithic plugin, GensCore is divided into dozens of "Modules" (such as `EconomyModule`, `TeamsModule`, `DiscordModule`). They all inherit from the `Module` interface and can be dynamically enabled or disabled.
2. **Local Database (`DatabaseManager.java`)**: To remain autonomous, GensCore doesn't use MySQL but a **local SQLite database** (`genscore.db`), which saves everything: player balances, guild inventories, quest progression, and connection histories.
3. **REST Web API (`WebManager.java` & `WebPlayerAPI.java`)**: Javalin is used in the background to spin up an HTTP micro-server on port 8080. The React code in the `/web-panel` folder is built and placed into `/src/main/resources/public/`. Every time an admin visits the web page, Javalin serves these HTML/JS files and communicates with React via a JSON API.

---

## Build & Installation

If you wish to compile GensCore, the ideal environment is **Java 25**, managed by **Maven**.

### Manual Build (Local)
```bash
git clone https://github.com/WilliamBossard/GensCore.git
cd GensCore
mvn clean package -DskipTests
```
The final file will be located in `target/GensCore-1.0-SNAPSHOT.jar`.

### Build via GitHub Actions
GensCore has a configured Workflow (in `.github/workflows/release.yml`).
1. Go to your GitHub Repository, in the **"Actions"** tab.
2. Select **"Build and Release GensCore"** on the left.
3. Click on **"Run workflow"**.
4. GitHub will compile the project on its servers (for free) in a few seconds, then create a **Draft Release**.
5. All you have to do is download the `.jar` file from the GitHub Release without needing to install Java or Maven on your computer!

---
*Plugin developed with passion for GensBien.*
