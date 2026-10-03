# Changelog & Version History

Current status and complete release notes for **GensCore** on **Minecraft 26.3** and **Java 25 LTS**.

::: info Project Status
- **Target Engine:** Minecraft 26.1 - 26.3 (Paper Build 143-beta · Folia 26.1+)
- **Runtime Environment:** Java 25 LTS (Classfile 69)
- **Current Version:** **Release 1.0.3** — Stable
- **Quality Assurance:** **81/81 automated tests passing** (0 errors, 0 failures)
- **Repository:** [`dev`](https://github.com/WilliamBossard/GensCore/tree/dev) · [`main`](https://github.com/WilliamBossard/GensCore/tree/main)
:::

---

## Release Notes

> Releases are listed in **reverse chronological order** — latest first.

---

### Release 1.0.3 — Paper build.143-beta (October 3, 2026)

> **Major stability, database and modernization release.** All 81 automated unit and concurrency tests passing with 0 errors. Verified on Paper 26.1, Paper 26.2, and Paper/Folia 26.3.

- **Paper 26.3 Full API Modernization & Zero Deprecation:**
  - `UtilsModule.java`: Migrated virtual menus (`openWorkbench`, `openAnvil`, `openEnchanting`) to Paper 26.3 `MenuType.CRAFTING.builder()`, `MenuType.ANVIL.builder()`, and `MenuType.ENCHANTMENT.builder()`.
  - `ShopModule.java`: Migrated item detection to modern Data Components API (`meta.hasCustomModelDataComponent()`, `meta.hasCustomName()`, `meta.hasItemName()`).
  - `TombListener.java`: Migrated player head skin profile to Paper 26.3 `skull.setProfile(ResolvableProfile.resolvableProfile(profile))`.
  - `JobsModule.java`: Replaced Bukkit `Metadatable` (`FixedMetadataValue`) and deprecated `Block.getBlockKey()` with ultra-fast 64-bit coordinate packing `packBlockKey(x, y, z)` and thread-safe set storage (`Map<UUID, Set<Long>>`).
  - Completely eliminated all `@SuppressWarnings("deprecation")` from active Bukkit/Paper plugin source code.
- **Memory Safety & Resource Leak Elimination:**
  - `TeleportBackModule.java`: Replaced `Location` references with immutable `record BackPosition(String worldName, double x, double y, double z, float yaw, float pitch)` to prevent strong reference retention to `World` instances across world reloads and unloads.
  - `StorageManager.java`: Purged dead legacy YAML file management code (`dataFile`, `dataConfig`, `initDataFile()`, etc.).
  - `ItemSerializer.java`: Isolated and documented the backward-compatibility deserialization fallback for pre-existing legacy Base64 items.
- **Multi-Engine Database Support (MySQL / MariaDB & SQLite):**
  - Added optional support for external MySQL and MariaDB databases via HikariCP and the official MariaDB JDBC driver (`org.mariadb.jdbc:mariadb-java-client:3.5.2`, fully shaded).
  - Maintained local SQLite (`genscore.db`) as the default out-of-the-box storage engine (zero manual setup required).
  - Implemented dynamic SQL dialect translation (`DatabaseManager.adaptQuery()`):
    - Auto-increment translation (`INTEGER PRIMARY KEY AUTOINCREMENT` -> `INT AUTO_INCREMENT PRIMARY KEY`).
    - Timestamp default translation (`TIMESTAMP DEFAULT CURRENT_TIMESTAMP` -> `DATETIME DEFAULT CURRENT_TIMESTAMP`).
    - Upsert statement translation (`ON CONFLICT(...) DO UPDATE SET col = excluded.col` -> `ON DUPLICATE KEY UPDATE col = VALUES(col)`).
  - Multi-engine schema migration and table introspection across `sqlite_master` and `information_schema.tables`.
  - Added resilient automatic fallback: if an external MySQL/MariaDB server is unreachable, times out, or fails authentication, GensCore automatically falls back to local SQLite without crashing the server.
- **System Metrics & Monitoring Service (`MetricsService.java`):**
  - Real-time telemetry collection including JVM memory (used, free, max), CPU load, Folia tick rate, database connection pool statistics, and module registry status.
  - New REST API endpoints: `/api/metrics` and `/api/admin/metrics` exposed via Javalin.
- **Administration & Diagnostics Commands (`GensCommand.java`):**
  - `/gens status` (permission: `genscore.admin`): Displays live server platform, Folia TPS, Java/JVM memory usage, and active module count.
  - `/gens db` (permission: `genscore.admin`): Displays current storage engine (SQLite or MySQL/MariaDB), database URL, active/idle connections, and waiting thread metrics.
- **Automated Test Coverage & CI/CD:**
  - Added unit test suite `SqlDialectTest` validating query rewriting across SQLite and MySQL dialects.
  - Added integration test suite `DatabaseConfigTest` validating configuration loading, driver resolution, and pool property initialization.
  - Test suite expanded to **81 tests passing with 0 errors and 0 failures**.
  - Updated GitHub Actions CI/CD workflows (`ci.yml`, `release.yml`) with automated shaded package verification and test tracking.

---

### Release 1.0.2 — Paper build.143-beta (October 3, 2026)

> **Stable maintenance release.** All 75 automated unit tests passing. Tested & verified on Paper 26.1, Paper 26.2 and Paper/Folia 26.3.

- **TabBoard & Nametags:**
  - Omitted prefix for players in the `default` LuckPerms group when no custom prefix is configured (displays clean username without fallback `Default` text).
  - Synchronized prefix cache refresh rate with nametags (every 5 seconds) for rapid visibility of LuckPerms metadata changes.
  - Guarded platform prefix: `[Java]` tag is no longer displayed if Floodgate is not installed on the server.
- **Scoreboard & Placeholders:**
  - Added native fallback resolution for `%vault_eco_balance%`, `%vault_eco_balance_fixed%`, and `%vault_eco_balance_formatted%` directly from GensCore's economy engine without requiring external plugins.
  - Fallback `<prefix>` placeholder resolution cleanly returns empty component for un-prefixed default players.
- **Chat & Formatting:**
  - Aligned chat prefix formatting with LuckPerms group/meta rules, removing hardcoded `[Joueur]` fallback for players without a prefix.
  - Fixed trailing space handling to eliminate duplicate spaces between prefix and player username.
- **Auth & Localization:**
  - Purged duplicate auto-appended translations in `fr_FR.yml` and `en_US.yml`.
  - Replaced escaped HTML entities (`&lt;`/`&gt;`) with valid MiniMessage tags, restoring the welcome banner on join and resolving login prompt spam.
- **Paper & Workflow:**
  - Upgraded Paper API to `26.3.build.143-beta` with cross-version compatibility for Paper 26.2 and 26.3.
  - Updated GitHub Actions release workflow with latest Paper build metadata.

---

### Release 1.0.1 — Paper build.41-alpha (September 25, 2026)

> **Stable release.** All 69 automated unit tests passing. Production-ready.

- **Paper API:** Upgraded to `26.3.build.41-alpha`.
- **Mobile Admin Panel:** Fixed admin top bar on mobile — now `position: fixed` with a dedicated `admin-content-scroll` scrollable zone, eliminating the black line / overlap artifact on all tabs.
- **Backend Libraries:** `jackson-databind` → `2.22.3`, `JDA` → `6.7.0`.
- **Frontend:** `vite` 8.3.1, `lucide-react` 1.48.0, `react-i18next` 17.0.15, `typescript-eslint` 8.70.1.
- **CI/CD:** GitHub Actions workflows updated to Node.js 24 and Paper `build.41-alpha`.

---

### Release 1.0.0 — Initial Release (September 20, 2026)

- Initial stable release of GensCore.
- Modular architecture with autonomous modules.
- Embedded Javalin & React web dashboard.
- SQLite persistence with HikariCP connection pooling.
- Discord integration via JDA 6.
- Cross-play support for Java & Bedrock (Geyser/Floodgate).
