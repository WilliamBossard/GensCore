# In-Game Modules (28 Modules)

GensCore is built around a modular architecture managed by `ModuleManager`. Every module can be enabled or disabled dynamically without server restarts.

---

## 1. Death Graves (Tomb Module)
When a player dies, their items are preserved inside a physical grave block at the exact death coordinate.
- **Hologram Protection:** A floating display shows the owner name and protection countdown (e.g. 10 minutes).
- **Owner-Exclusive:** Only the victim can right-click the grave to reclaim all inventory and experience.
- **Admin Bypass:** Admins with `genscore.tomb.admin` can loot or unlock any grave.
- **Expiry:** Once the timer reaches 0, the grave can either be looted publicly or dropped into the world.

---

## 2. Lootr Instanced Chests
Every natural dungeon, fortress, and structure chest is instanced per-player:
- Each player sees their own fresh loot upon first opening.
- Prevents players from racing to empty dungeons on a newly opened world.
- Built-in hopper protection prevents draining items via automation.
- Chest break protection prevents destruction before loot is collected.

---

## 3. Dynamic Shop Algorithm
The `/shop` module dynamically calculates buying and selling prices based on market volume:

$$\text{Current Price} = \text{Base Price} \times \left(1 + \frac{\text{Purchases} - \text{Sales}}{\text{Threshold}}\right)^{\text{exponent}}$$

- High demand increases item prices; oversupply lowers prices.
- The `inflationExponent` can be configured via the Web Panel to balance your server economy.

---

## 4. Custom Spawners & Stacking
To eliminate entity lag on high-population servers:
- **Stacking:** Spawners merge into single blocks (e.g., `x15 Skeleton Spawner`).
- **Internal Storage:** Mob drops can be accumulated directly inside the spawner block; players open the GUI to collect loot in bulk.
- **Speed & XP Upgrades:** Increase spawn frequency and multiply experience drops.

---

## 5. Container Locks & Piston Griefing Shield
- Locks protect **Chests, Trapped Chests, Barrels, and Shulker Boxes**.
- **Anti-Piston Shield:** Pistons cannot move, retract, or crush locked containers.
- **Anti-Shulker Break:** Cancels piston shulker breaking contraptions, eliminating duplication exploits.

---

## 6. Bedrock & Floodgate Cross-Play
- Automatic detection of Bedrock players joining through Floodgate/Geyser.
- **Cumulus Forms:** In-game GUIs open as native Bedrock dialogs for mobile and console players, avoiding touch-screen desyncs.
- **Skin & Head API:** Renders authentic Bedrock player heads via the web API (`/api/head/{name}/{size}`).
- **Prefix Isolation:** Handles Bedrock prefixes (`.` or `*`) smoothly across all commands and LuckPerms lookups.

---

## 7. Jobs & Professions
6 diverse professions:
1. **Miner:** Mining ores, stone, deepslate.
2. **Lumberjack:** Chopping trees and logs.
3. **Hunter:** Killing monsters and hostile bosses.
4. **Farmer:** Harvesting wheat, potatoes, carrots, pumpkins.
5. **Fisherman:** Catching fish and treasure items.
6. **Builder:** Placing construction blocks.

Progress is saved asynchronously to SQLite every 60 seconds with 0 main-thread lag.

---

## 8. Fast Leaf Decay
Automatically detects broken tree trunks and triggers rapid, cascading decay of leaves without leaving floating treetops.

---

## 9. Head Drops
Configurable chance for mobs and defeated players to drop their heads with authentic skin textures upon death.

---

## 10. Modern Chat & MiniMessage
- Pure **Kyori Adventure / MiniMessage** formatting (no legacy `§` codes).
- Automatic LuckPerms prefix and suffix resolution.
- Discord synchronization and channel separation.

---

## 11. Guilds, Claims & Shared Treasury
A complete clan ecosystem supporting Folia multi-threading and Bedrock crossplay:
- **Hierarchy & Roles (Leader, Admin, Member):**
  - **Leader (`LEADER`):** Full ownership, admin promotions/demotions, disbanding the guild, treasury withdrawals, and territory claims.
  - **Administrator (`ADMIN`):** Co-management powers: invitations, kicking regular members, treasury withdrawals, land claims/unclaims, BlueMap color styling, and perk purchases.
  - **Member (`MEMBER`):** Depositing funds & XP, participating in weekly co-op quests, and accessing guild-locked storage containers.
  - *In-Game & Web Management:* In-game `/team` interface (left-click to promote/demote, right-click to kick) and Player Web Portal with confirmation modals.
- **Shared Treasury (Guild Bank):** Guild members deposit dollars (`/team deposit <amount>`) or XP levels (`/team depositxp <levels>`). When the Economy module is disabled, all transactions and perk upgrades automatically switch to player XP levels. Leaders and Admins can withdraw funds via `/team withdraw` or `/team withdrawxp`.
- **Territory Claims ($16 \times 16$ Chunks):** Guilds claim chunks via `/team claim`. Claims are strictly purchased using the guild bank balance ($1500 or 10 XP levels per chunk).
  - *Comprehensive Anti-Grief:* Total protection against block breaking/placing, opening containers (chests, barrels, furnaces, hoppers, shulkers), redstone interactions (doors, trapdoors, buttons, levers), entity/livestock damage, and piston griefing across chunk borders.
  - *On-Screen Boundary Notifications:* Animated Title and Subtitle with an immersive chime sound displayed when a player enters claimed guild territory.
- **Team Perks (Active Guild Upgrades):** 5 tiered perks purchased strictly from guild treasury:
  1. *Max Members (Tiers 1 to 3):* 8, 11, and 14 max members (base 5).
  2. *Extended Territory (Tiers 1 to 4):* 8, 12, 16, and 20 max claimed chunks (base 4).
  3. *Jobs Boost (Tiers 1 to 3):* +5%, +10%, and +15% jobs XP bonus for online members.
  4. *Auction House Tax Reduction (Tiers 1 to 2):* -25% and -50% tax cut on sales commission.
  5. *Co-op Quest Boost (Tiers 1 to 2):* +10% and +20% guild quest progress points.
- **Upcoming Perks (In Development):** 5 additional upgrade trees are currently being added: *Guild Home Warp* (`/team home`), *Territory Buffs* (AoE effects in claims), *Daily Bank Interest*, *Spawner Overclocking*, and *Virtual Shared Vault* (`/team vault`).
- **BlueMap Live Integration:** All guild claims render seamlessly on 3D BlueMap with customized hex colors configured via `/team color <#hex>` or the Web Panel.

---

## Complete Module List (28)
`UtilsModule`, `TombModule`, `TeleportTpaModule`, `TeleportSpawnModule`, `TeleportHomeModule`, `TeleportBackModule`, `TeamModule`, `TabBoardModule`, `StatsModule`, `SpawnerModule`, `ShopModule`, `QuestModule`, `MotdModule`, `ModerationModule`, `LootModule`, `LockModule`, `HeadDropModule`, `CustomGuiModule`, `GuiModule`, `JobsModule`, `FastLeafDecayModule`, `EconomyModule`, `ChatModule`, `BlueMapModule`, `DiscordModule`, `AuctionHouseModule`, `AuthModule`, `BedrockSkinModule`.
