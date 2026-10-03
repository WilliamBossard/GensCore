# Changelog

All notable changes to **GensCore** will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [1.0.2] - 2026-10-03

### Compatibility
- Tested and verified on **Paper 26.2** (build 129+) and **Paper / Folia 26.3** (build 143-beta).
- Requires Java 25 LTS+.
- 100% automated test suite passing (71/71 tests).

### Fixed & Improved
- **TabBoard & Nametags (`TabBoardModule`):**
  - Omitted prefix fallback for players in the `default` LuckPerms group when no custom prefix is set (displays clean username in Tablist and nametags without fallback `Default` text).
  - Synchronized prefix cache refresh rate with nametags (every 5 seconds) for immediate visibility of LuckPerms metadata changes.
  - Guarded platform prefix display: `[Java]` tag is no longer displayed if Floodgate is not installed on the server.
- **Scoreboard & Placeholders (`PlaceholderUtils`):**
  - Added native fallback resolution for `%vault_eco_balance%`, `%vault_eco_balance_fixed%`, and `%vault_eco_balance_formatted%` directly from GensCore's economy engine without requiring external plugins.
  - Fallback `<prefix>` placeholder resolution cleanly returns empty component for un-prefixed default players.
- **Chat & Formatting (`ChatModule`):**
  - Aligned chat prefix formatting with LuckPerms group/meta rules, removing hardcoded `[Joueur]` fallback for players without a prefix.
  - Fixed trailing space handling to eliminate duplicate spaces between prefix and player username.
- **Auth & Localization:**
  - Purged duplicate auto-appended translations in `fr_FR.yml` and `en_US.yml`.
  - Replaced escaped HTML entities (`&lt;`/`&gt;`) with valid MiniMessage tags, restoring the welcome banner on join and resolving login prompt spam.
- **Paper & Workflow:**
  - Upgraded Paper API to `26.3.build.143-beta` with cross-version compatibility for Paper 26.2 and 26.3.
  - Updated GitHub Actions release workflow with latest Paper build metadata.

---

## [1.0.1] - 2026-09-25

### Fixed & Improved
- **Paper API:** Upgraded to `26.3.build.41-alpha`.
- **Mobile Admin Panel:** Fixed admin top bar on mobile — now `position: fixed` with dedicated `admin-content-scroll` zone.
- **Backend Libraries:** `jackson-databind` → `2.22.3`, `JDA` → `6.7.0`.
- **Frontend:** `vite` 8.3.1, `lucide-react` 1.48.0, `react-i18next` 17.0.15, `typescript-eslint` 8.70.1.
- **CI/CD:** GitHub Actions workflows updated to Node.js 24 and Paper `build.41-alpha`.

---

## [1.0.0] - 2026-09-20

### Initial Release
- Initial stable release of GensCore.
- Modular architecture with 29 autonomous modules.
- Embedded Javalin & React web dashboard.
- SQLite persistence with HikariCP connection pooling.
- Discord integration via JDA 6.
- Cross-play support for Java & Bedrock (Geyser/Floodgate).
