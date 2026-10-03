# Changelog

All notable changes to **GensCore** will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [1.0.3] - 2026-10-03

### Compatibility
- Tested and verified on **Paper 26.1** (build 74+), **Paper 26.2** (build 129+) and **Paper / Folia 26.3** (build 143-beta).
- Requires Java 25 LTS+ with release 25 compiler target.
- 100% automated test suite passing (**81/81 tests** including multi-threaded stress tests).

### Added
- **Multi-Engine Database Support (MySQL / MariaDB & SQLite):**
  - Added optional support for external MySQL and MariaDB databases via HikariCP and the official MariaDB JDBC driver (`org.mariadb.jdbc:mariadb-java-client:3.5.2`, fully shaded).
  - Maintained local SQLite (`genscore.db`) as the default out-of-the-box storage engine (zero manual setup required).
  - Implemented dynamic SQL dialect translation (`DatabaseManager.adaptQuery()`):
    - Translates auto-increment definitions (`INTEGER PRIMARY KEY AUTOINCREMENT` -> `INT AUTO_INCREMENT PRIMARY KEY`).
    - Translates timestamp defaults (`TIMESTAMP DEFAULT CURRENT_TIMESTAMP` -> `DATETIME DEFAULT CURRENT_TIMESTAMP`).
    - Translates upsert statements (`ON CONFLICT(...) DO UPDATE SET col = excluded.col` -> `ON DUPLICATE KEY UPDATE col = VALUES(col)`).
  - Implemented multi-engine schema migration and table introspection across `sqlite_master` and `information_schema.tables`.
  - Added resilient automatic fallback: if an external MySQL/MariaDB server is unreachable, times out, or fails authentication, GensCore automatically falls back to local SQLite and logs an actionable warning without crashing the server.
- **System Metrics & Monitoring Service (`MetricsService.java`):**
  - Real-time telemetry collection including JVM memory (used, free, max), CPU load, Folia tick rate, database connection pool statistics, and module registry status.
  - New REST API endpoints: `/api/metrics` and `/api/admin/metrics` exposed via Javalin.
- **Administration & Diagnostics Commands (`GensCommand.java`):**
  - `/gens status` (permission: `genscore.admin`): Displays live server platform, Folia TPS, Java/JVM memory usage, and active module count.
  - `/gens db` (permission: `genscore.admin`): Displays current storage engine (SQLite or MySQL/MariaDB), database URL, active/idle connections, and waiting thread metrics.
- **Paper 26.3 API Modernization & Complete Deprecation Purge:**
  - `UtilsModule.java`: Migrated virtual menus (`openWorkbench`, `openAnvil`, `openEnchanting`) to Paper 26.3 `MenuType.CRAFTING.builder()`, `MenuType.ANVIL.builder()`, and `MenuType.ENCHANTMENT.builder()`.
  - `ShopModule.java`: Migrated item detection to modern Data Components API (`meta.hasCustomModelDataComponent()`, `meta.hasCustomName()`, `meta.hasItemName()`).
  - `TombListener.java`: Migrated player head skin profile to Paper 26.3 `skull.setProfile(ResolvableProfile.resolvableProfile(profile))`.
  - `JobsModule.java`: Replaced Bukkit `Metadatable` (`FixedMetadataValue`) and deprecated `Block.getBlockKey()` with ultra-fast 64-bit coordinate packing `packBlockKey(x, y, z)` and thread-safe set storage (`Map<UUID, Set<Long>>`).
  - Removed all `@SuppressWarnings("deprecation")` from active Bukkit/Paper plugin source code.
- **Memory Safety & Resource Leak Elimination:**
  - `TeleportBackModule.java`: Replaced `Location` references with immutable `record BackPosition(String worldName, double x, double y, double z, float yaw, float pitch)` to prevent strong reference retention to `World` instances across world reloads and unloads.
  - `StorageManager.java`: Purged dead legacy YAML file management code (`dataFile`, `dataConfig`, `initDataFile()`, etc.).
  - `ItemSerializer.java`: Isolated and documented the backward-compatibility deserialization fallback for pre-existing legacy Base64 items.
- **Automated Test Coverage & CI/CD:**
  - Added unit test suite `SqlDialectTest` validating query rewriting across SQLite and MySQL dialects.
  - Added integration test suite `DatabaseConfigTest` validating configuration loading, driver resolution, and pool property initialization.
  - Test suite expanded to **81 tests passing with 0 errors and 0 failures**.
  - Updated GitHub Actions CI/CD workflows (`ci.yml`, `release.yml`) with automated shaded package verification and test tracking.

---

## [1.0.2] - 2026-10-03

### Compatibility
- Tested and verified on **Paper 26.1** (build 74+), **Paper 26.2** (build 129+) and **Paper / Folia 26.3** (build 143-beta).
- Requires Java 25 LTS+.
- 100% automated test suite passing (75/75 tests including multi-threaded stress tests).

### Fixed & Improved
- **Code Quality & Logging Standardization:**
  - Completely eliminated all raw `printStackTrace()` calls across all DAOs, commands, and modules.
  - Standardized on structured contextual logging via `plugin.getLogger().log(Level.WARNING / SEVERE / FINE, ...)` for production monitoring.
- **Redstone & Territory Performance (`TeamClaimListener`):**
  - Converted piston extension and retraction coordinate calculations to direct bitshifting (`pistonBlock.getX() >> 4`), eliminating `CraftChunk` object allocations during intensive redstone machinery cycles.
- **Concurrency & Thread-Safety (`TeamData`):**
  - Converted internal guild member collection to `CopyOnWriteArrayList` and secured role updates against `ConcurrentModificationException` under heavy multi-threading.
- **Stress & Integration Testing (`ConcurrentTransactionStressTest`):**
  - Implemented 4 multi-threaded stress tests validating guild bank atomicity under 50 concurrent threads, atomic CAS balance competition without overdraft, and single-buyer auction house atomicity.
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
- **Web Applications & Documentation:**
  - `web-panel`: Updated to Vite `8.3.2`, Lucide-React `1.51.0`, ESLint `10.12.0`, `@types/node` `26.6.4`, and rebuilt fresh frontend bundle into `resources/public/`.
  - `docs`: Updated to VitePress `1.6.4` and recompiled client/server bundles.
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
