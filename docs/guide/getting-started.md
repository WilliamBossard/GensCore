# Getting Started with GensCore

<div style="display: flex; gap: 8px; margin: 1rem 0; flex-wrap: wrap;">
  <img src="https://img.shields.io/badge/GensCore-Paper%20%26%20Folia-green.svg" alt="Paper and Folia" />
  <img src="https://img.shields.io/badge/Java-25+-blue.svg" alt="Java 25" />
  <img src="https://img.shields.io/badge/Minecraft-26.2+-red.svg" alt="Minecraft 26.2" />
</div>

**GensCore** is an all-in-one survival and faction core plugin developed for **PaperMC & Folia (Minecraft 26.2+)** running on **Java 25 (LTS)**.

---

## Key Highlights

- **Server Loaders Supported:** **PaperMC**, **Folia**, and **Purpur** (Minecraft 26.2+).
- **Runtime Requirement:** **Java 25+** (uses modern JVM features, pattern matching, and ASM 9.10.1).
- **Zero Mandatory Dependencies:** GensCore includes its own economy engine, SQLite database, container locking, and permission fallback.
- **Cross-Play Out of the Box:** Native Bedrock support when Geyser & Floodgate are installed.

---

## Installation Steps

1. **Check your Java version:**
   Make sure your Minecraft server is running on **Java 25 or higher**:
   ```bash
   java -version
   ```
2. **Download the latest `.jar`:**
   Get `GensCore-vX.Y.Z.jar` from [GitHub Releases](https://github.com/WilliamBossard/GensCore/releases) or [Modrinth](https://modrinth.com/).
3. **Place in the `plugins/` directory:**
   Drop the jar file into your server's `plugins/` folder.
4. **Start the server:**
   Start or restart your server. GensCore will automatically:
   - Create the `plugins/GensCore/` configuration directory.
   - Initialize the local SQLite database (`genscore.db`) with WAL concurrency.
   - Extract the web panel assets to `plugins/GensCore/web/`.
   - Start the Javalin web micro-server on port `8080`.

---

## Ecosystem & Integrations

GensCore operates 100% autonomously, but automatically enhances its capabilities if these plugins are detected:

| Plugin | Integration Type | Description |
|---|---|---|
| **Vault** | Soft Dependency | Automatically registers `GensVaultEconomy`. Allows 3rd-party plugins (ChestShop, Towny, Claims) to access GensCore balances. |
| **LuckPerms** | Soft Dependency | Automatically reads player ranks, primary groups, weights, and prefixes for Chat and Tablist/Scoreboard. |
| **Floodgate / Geyser** | Optional | Automatically enables native Bedrock UI forms (Cumulus API), handles Bedrock UUIDs, and resolves Bedrock skin heads. |
| **BlueMap** | Optional | If installed, embeds a live 2D/3D map viewer directly into the Web Admin Panel. |
| **PlaceholderAPI** | Optional | Expands GensCore variables across other plugins. |

---

## Next Steps

- Explore the complete [Commands Reference](/guide/commands).
- Set up player and staff ranks with [Permissions & LuckPerms](/guide/permissions).
- Dive into the [In-Game Modules](/guide/modules).
- Connect to the [Web Panel & Minigames](/guide/web-panel).
