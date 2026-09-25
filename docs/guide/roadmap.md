# Roadmap & Changelog

Current status and complete patch history for **GensCore** on **Minecraft 26.3** and **Java 25 LTS**.

::: info Project Status
- **Target Engine:** Minecraft 26.3 (Paper Build 41-alpha)
- **Runtime Environment:** Java 25 LTS
- **Current Version:** **Release 1.0.1** — Stable
- **Repository:** [`dev`](https://github.com/WilliamBossard/GensCore/tree/dev) · [`main`](https://github.com/WilliamBossard/GensCore/tree/main)
:::

---

## Progress Overview

| Component | Status |
| :--- | :---: |
| **Java 25 LTS Support** | <span style="color: #22c55e; font-weight: 700;">✓ Done</span> |
| **Paper 26.3 API (build.41-alpha)** | <span style="color: #22c55e; font-weight: 700;">✓ Done</span> |
| **Dependencies Upgrades** | <span style="color: #22c55e; font-weight: 700;">✓ Done</span> |
| **Mobile Web Optimization** | <span style="color: #22c55e; font-weight: 700;">✓ Done</span> |
| **Geyser Standalone Architecture** | <span style="color: #22c55e; font-weight: 700;">✓ Done</span> |
| **Craft Quests (Torches & Bulk Crafting)** | <span style="color: #22c55e; font-weight: 700;">✓ Done</span> |
| **Survival Shop (319 Items & Alchemy)** | <span style="color: #22c55e; font-weight: 700;">✓ Done</span> |
| **Web Minigames Remaster & CoinFlip** | <span style="color: #22c55e; font-weight: 700;">✓ Done</span> |
| **Internationalization (FR & EN)** | <span style="color: #22c55e; font-weight: 700;">✓ Done</span> |
| **Native Paper Brigadier** | <span style="color: #22c55e; font-weight: 700;">✓ Done</span> |
| **Cloud Reflection Patch** | <span style="color: #22c55e; font-weight: 700;">✓ Done</span> |
| **Shop Anti-Double Click** | <span style="color: #22c55e; font-weight: 700;">✓ Done</span> |
| **Banlist Synchronization** | <span style="color: #22c55e; font-weight: 700;">✓ Done</span> |
| **Bedrock Cross-Play (Geyser/Floodgate)** | <span style="color: #22c55e; font-weight: 700;">✓ Hardened</span> |
| **Folia Regional Threading** | <span style="color: #22c55e; font-weight: 700;">✓ Done</span> |
| **Guilds & Territory Claims 2.0** | <span style="color: #22c55e; font-weight: 700;">✓ Done</span> |
| **Guild Upgrades Phase 2** | <span style="color: #22c55e; font-weight: 700;">✓ Done</span> |
| **Solo Quest Perks Module** | <span style="color: #22c55e; font-weight: 700;">✓ Done</span> |
| **Lootr SQLite Migration** | <span style="color: #22c55e; font-weight: 700;">✓ Done</span> |
| **Unit Test Suite (69/69)** | <span style="color: #22c55e; font-weight: 700;">✓ Done</span> |

---

## Changelog

> Patches are listed in **reverse chronological order** — latest first.

---

### Release 1.0.1 — Paper build.41-alpha (September 25, 2026)

> **Stable release.** All 69 automated unit tests passing. Production-ready.

- **Paper API:** Upgraded to `26.3.build.41-alpha`.
- **Mobile Admin Panel:** Fixed admin top bar on mobile — now `position: fixed` with a dedicated `admin-content-scroll` scrollable zone, eliminating the black line / overlap artifact on all tabs.
- **Backend Libraries:** `jackson-databind` → `2.22.3`, `JDA` → `6.7.0`.
- **Frontend:** `vite` 8.3.1, `lucide-react` 1.48.0, `react-i18next` 17.0.15, `typescript-eslint` 8.70.1.
- **CI/CD:** GitHub Actions workflows updated to Node.js 24 and Paper `build.41-alpha`.

---

### Patch 26.3-alpha.20 (September 19, 2026)

- **Solo Quest Perks Module (`SoloPerkModule`):** 29th autonomous GensCore module (`solo_perks`), runtime toggleable in-game (`/module solo_perks <on|off>`) or via the admin web dashboard.
- **Two-Tier Progression:**
  - *Free Milestone Perks (6):* Unlocked automatically by reaching lifetime completed quest thresholds (5, 15, 30, 50, 75, 100): Free Reroll, Extra Home, Celestial Stride, Jobs Wisdom, Instant Teleport, Endless Feast.
  - *Major Solo Masteries (5):* Require a quest milestone AND payment in dollars or XP: Item Magnet, Double Harvest, Portable Workbench, Auto-Smelt, Soul Preservation (50% XP saved on death).
- **Instant On/Off Toggles:** Magnet (`/magnet`) and Auto-Smelt (`/autosmelt`) switchable at any time via commands, `/perks` GUI, or the Web Portal.
- **Player Web Portal (`/dashboard/perks`):** Global quest progress bar, counters, mastery purchase buttons, and live toggle switches backed by real-time SQLite sync.
- **Paper API:** Upgraded to `26.3.build.19-alpha` (shipped with alpha.19 Paper build).

---

### Patch 26.3-alpha.19 (September 19, 2026)

- **Guild Perks Phase 2:** All 5 remaining permanent upgrades deployed (`GUILD_HOME`, `TERRITORY_BUFF`, `BANK_INTEREST`, `SPAWNER_EFFICIENCY`, `GUILD_VAULT`).
- **Territory Anti-Abuse Shield:** 5-minute chunk stabilization anchor before radiating buffs (ActionBar countdown), 15-second presence synchronization on entry, instant buff removal upon leaving.
- **Direct Action Buttons & Bedrock Cross-Play:** 54-slot `/team` GUI with buttons at slot 38 (Home), slot 40 (Vault), slot 42 (Bank). Native Bedrock Cumulus dialog buttons and right-click shortcuts in `/team upgrades`.
- **New Commands:** `/team sethome`, `/team home`, `/team vault` (aliases: `/team coffre`, `/team chest`).
- **HeadUtil:** Native player head resolution with persistent SQLite/RAM skin caching (`player_skins`), supporting Java, Bedrock (Floodgate/Geyser), and SkinsRestorer.
- **Test Suite:** Extended to 58 automated unit tests (100% pass rate).

---

### Patch 26.3-alpha.18 (September 18, 2026)

- **Guild Roles & Hierarchy:** Full `ADMIN` role in SQLite (`genscore_team_members.role`). Commands: `/team promote`, `/team demote`, `/team kick`, `/team leave`, `/team disband`. In-game GUI: left-click promote/demote, right-click kick.
- **Bulletproof Claim Anti-Grief:** Full chunk protection against block break/place, container access (chests, barrels, furnaces, shulkers, hoppers), redstone interactions, entity/livestock damage, and piston crossing.
- **Boundary Screen Titles:** Title + sound effect on entering claimed territory.
- **Expanded Guild Web Portal:** Member cards with role badges, promotion/demotion, kick with confirmation modal, treasury management ($/XP), BlueMap color picker, upgrade purchases.
- **Web Asset Auto-Sync:** Compares JAR web assets on startup and auto-extracts updates into `plugins/GensCore/web/`.
- **Paper API:** Upgraded to `26.3.build.16-alpha`.

---

### Patch 26.3-alpha.17 (September 18, 2026)

- **Shop Anti-Arbitrage Guard:** Harmonized dynamic inflation exponent with `GLOBAL_INFLATION_EXPONENT` and enforced 75% max sell-to-buy price cap, eliminating circular currency arbitrage.
- **Float Exploit Hardening:** `Double.isFinite()` validation on all economy and marketplace inputs (`/pay`, `/eco`, `/ah sell`) to block `NaN` and `Infinity` corruption exploits.
- **Economy Write-Behind Batching:** Replaced isolated async SQLite writes in `EconomyModule` with a batched *Write-Behind Cache* (`flushDirtyBalances()` every 10 seconds).
- **Thread-Safe Loot Handling:** Synchronized `YamlConfiguration` instances in `LootManager` to eliminate race conditions during concurrent async saves on Folia.
- **Non-blocking Discord Shutdown:** Replaced `Thread.sleep(1500)` with `jda.awaitShutdown(Duration.ofMillis(1500))` and graceful fallback to `shutdownNow()`.

---

### Patch 26.3-alpha.16 (September 18, 2026)

- **Player Balance Wallet:** Live balance card in the sidebar with instant debit/credit animations and on-click sync.
- **Shop Balance Security:** Real-time post-purchase balance check in shop drawer with warning alert and automatic blocking when funds are insufficient.
- **100% 26.3 Texture Coverage:** Full asset mapping for all 1,815 Minecraft 26.3 materials (vanilla spears, Poplar wood, cushions, shelf mushroom, straw bed, copper blocks) with resilient fallback chains.
- **Paper API:** Upgraded to `26.3.build.16-alpha`.

---

### Patch 26.3-alpha.8 (September 17, 2026)

- **Craft Quests:** Accurate Shift-Click batch size tracking and vanilla yield multiplier (e.g. 32 torches correctly counted).
- **Survival Shop:** Complete 319-item catalog across 7 categories (ores, farming, wood with Pale Oak, building, mob drops, potions, utilities), SQLite auto-seeding, 45-slot pagination GUI, `/shop reload`.
- **Web Minigames:** Rebalanced slot machine to 84% RTP, normalized Wheel of Fortune slices, added 3D CoinFlip with admin toggle.
- **Localization:** 100% bilingual EN/FR coverage in-game and on the web portal.
- **Paper API:** Upgraded to `26.3.build.8-alpha`.

---

### Patch 26.3-alpha.7 (September 16, 2026)

- **Folia Thread Safety:** Resolved crash caused by `Bukkit.isPrimaryThread()` in `EconomyModule` — economy transactions now fully safe on Folia regional schedulers.
- **Geyser / Floodgate Resilience:** Hardened `BedrockSkinModule` and `BedrockFormManager` with `Throwable` handlers catching JVM `LinkageError` and `NoClassDefFoundError`.
- **Database & Modules:** Fixed `ModuleManager` dynamic toggling to call `initDatabase()` when a module is activated at runtime.
- **Teleportation:** `CompletableFuture<Boolean>` confirmation handling in `TeleportUtil` for asynchronous regional teleports.

---

### Patch 26.3-alpha.6 (September 16, 2026)

- **Paper API:** Upgraded to `26.3.build.6-alpha`.
- **Web Panel:** Mini-Jeux nav item now dynamically hidden when the module or all games are disabled.
- **Web Panel:** Automatic redirection to dashboard when navigating to a disabled mini-games route.
- **Translations:** Added missing BedrockSkin module descriptions in EN/FR with proper fallback keys.

---

### Patch 26.3-beta.1 — Paper build.28-alpha (September 16, 2026)

> **First milestone release.** GensCore reaches Beta stability. All core modules production-ready and fully audited.

- **Lootr Module — Full SQLite Migration:** Per-player instanced loot chests fully persisted in SQLite (`lootr_chests` & `lootr_player_chests`) via `LootDAO`. Automatic transparent migration of legacy `chests.yml` on first startup.
- **Folia Threading Hardening:** Console commands in `QuestModule`, `CustomGuiModule`, and `BlueMapModule` now guaranteed to execute on `GlobalRegionScheduler` via `runNextTick`.
- **AuctionHouseModule Inventory Safety:** Item retrieval and hand-slot clearing execute inside `runAtEntity` to prevent async inventory manipulation.
- **TeamCommand Chunk Access Fix:** `/team claim` and `/team unclaim` now compute chunk coordinates via pure arithmetic, eliminating `IllegalStateException: Asynchronous chunk access` on Folia.
- **JobsModule Race Condition Fix:** Replaced `dirtyPlayers.clear()` with `dirtyPlayers.removeAll(toSave)`, preventing XP gain loss during concurrent saves.
- **Test Suite:** 69 automated unit tests (up from 58) — 11 new tests covering `LootDAO` SQLite CRUD, UPSERT, cascade delete, and Base64 inventory round-trips.
- **Paper API:** `26.3.build.28-alpha`.
