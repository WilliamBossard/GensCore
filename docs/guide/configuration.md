# Configuration & Deployment
 
GensCore creates a clean and organized configuration structure in `plugins/GensCore/`.
 
---
 
## File Structure
 
```
plugins/GensCore/
├── config.yml              # Core settings (language, database options)
├── modules.yml             # Global enable/disable switches for all 30 modules
├── genscore.db             # Local SQLite database (WAL mode, connection pooled)
├── lang/
│   ├── fr_FR.yml           # French localization strings (MiniMessage tags)
│   └── en_US.yml           # English localization strings (MiniMessage tags)
├── menus/
│   └── default.yml         # Custom virtual inventory menus in YAML
├── modules/
│   ├── bluemap.yml         # BlueMap web integration URL
│   ├── chat.yml            # Custom chat format and join/quit alerts
│   ├── discord.yml         # Discord bot token, channels, and synchronization
│   ├── economy.yml         # Shop dynamic inflation exponent and AH sales tax
│   ├── headdrop.yml        # Mob and player skull drop probabilities
│   ├── lootr.yml           # Instanced dungeon chests and anti-break security
│   ├── minigames.yml       # Fortune Wheel rewards, Casino Slot RTP, CoinFlip
│   ├── motd.yml            # Server list MOTD lines with MiniMessage tags
│   ├── quests.yml          # Daily quest limits and reroll quotas
│   ├── shop.yml            # Dynamic shop items, base prices, price elasticity
│   ├── spawners.yml        # Custom spawner stacking, tick rate, and Silk Touch
│   ├── tabboard.yml        # Dynamic Scoreboard and Tablist layout & placeholders
│   ├── teams.yml           # Guild weekly quest rotation parameters
│   ├── teleport.yml        # Default max homes and global teleportation delays
│   ├── tomb.yml            # Death graves, XP retention, and access policies
│   └── web.yml             # Embedded Javalin web server, BCrypt auth, CORS, limits
└── web/                    # Extracted React / Vite production frontend bundle
```
 
---

## Core Configuration (`config.yml`)

GensCore supports dual-engine database persistence configured via `config.yml`:

```yaml
# Main language file (fr_FR or en_US)
lang: "en_US"

# Database Configuration
database:
  # Storage engine type: "sqlite" (default, autonomous) or "mysql" (remote database)
  type: "sqlite"

  # Local SQLite configuration (active by default if type: "sqlite")
  sqlite:
    file: "genscore.db"

  # Remote MySQL / MariaDB configuration (active only if type: "mysql")
  # If remote connection fails at startup, GensCore automatically falls back to SQLite
  mysql:
    host: "localhost"
    port: 3306
    database: "genscore"
    username: "root"
    password: "YOUR_SECURE_PASSWORD"
    ssl: false
    max_pool_size: 10
    minimum_idle: 2
    connection_timeout: 30000
```

> [!TIP]
> **Resilient Automatic Fallback:** If `type: "mysql"` is specified but the remote server is unreachable, timed out, or rejects authentication, GensCore outputs an actionable warning to the server console and instantly switches to the local SQLite engine. The server will never crash or fail to boot due to a database connection failure.

---

## Global Module Switchboard (`modules.yml`)

Each feature in GensCore can be enabled or disabled independently. You can also toggle modules at runtime via `/module <name> <on|off>`.

| Key | In-Game Feature | Default | Description & Dependencies |
|---|---|---|---|
| `dynamicshop` | Supply & demand server shop (`/shop`) | `true` | Requires `economy` module |
| `economy` | Player balances, `/money`, `/pay`, Vault provider | `true` | Registers Vault `GensVaultEconomy` if Vault is present |
| `auctionhouse` | Player-to-player marketplace (`/ah`) | `true` | Requires `economy` module |
| `jobs` | Mining, woodcutting, hunting, farming professions | `true` | Folia thread-safe anti-farm block key cache |
| `stats` | Block break, kill, death & playtime tracking | `true` | Visible in Player Web Dashboard |
| `spawners` | Smart stacked spawners with GUI upgrades & Silk Touch | `true` | Configured via `modules/spawners.yml` |
| `quests` | Daily rotating quests with reroll limits | `true` | Configured via `modules/quests.yml` |
| `lootr` | Per-player instanced dungeon & structure chests | `true` | Configured via `modules/lootr.yml` |
| `headdrop` | Mob and player skull drop probabilities | `true` | Configured via `modules/headdrop.yml` |
| `minigames` | Fortune Wheel, Casino Slots & CoinFlip | `true` | Configured via `modules/minigames.yml` & Web Panel |
| `motd` | Dynamic server list ping message | `true` | Configured via `modules/motd.yml` |
| `tabboard` | Custom Scoreboard and Tablist layout | `true` | Configured via `modules/tabboard.yml` |
| `discord` | Discord Bot bridge (chat sync, staff logs) | `true` | Requires valid bot token in `modules/discord.yml` |
| `gui` | In-game menu provider | `true` | Auto-detects Bedrock clients (Cumulus Forms) |
| `tomb` | Death graves preserving items & XP upon player death | `false` | Enable if you want graves instead of standard drops |
| `home` | Player home waypoints (`/sethome`, `/home`) | `true` | Limits set via permissions and `modules/teleport.yml` |
| `back` | Return to last death or teleport location (`/back`) | `true` | Zero-leak `BackPosition` memory safety |
| `spawn` | Server spawn location (`/setspawn`, `/spawn`) | `true` | Persistent spawn coordinates in database |
| `tpa` | Teleport request system (`/tpa`, `/tpaccept`, etc.) | `true` | Configurable warmup and movement cancellation |
| `teams` | Guilds, territory claims, shared bank & weekly quests | `true` | Integrated with BlueMap and Web Panel |
| `locks` | Container protection for chests, barrels & furnaces | `true` | Anti-hopper and anti-piston theft protection |
| `auth` | In-game authentication for offline servers (`/login`) | `true` | BCrypt password hashing |
| `moderation` | Staff toolset (`/ban`, `/mute`, `/kick`, `/freeze`) | `true` | Persistent punishment records in database |
| `chat` | Chat formatting and join/quit messages | `true` | LuckPerms prefix/suffix synchronization |
| `bluemap` | BlueMap guild territory claim visualization | `false` | Requires BlueMap installed on the server |
| `fastleafdecay` | Instant natural decay for orphaned tree leaves | `true` | High-performance leaves cleanup |
| `solo_perks` | Solo quest progression perks & masteries (`/perks`) | `true` | Integrated with daily quests & economy |
| `customgui` | Custom YAML inventory menus loader (`/menu`) | `true` | Configured via `plugins/GensCore/menus/` |
| `utils` | Virtual utilities (`/craft`, `/anvil`, `/ec`, `/feed`) | `true` | Native Paper 26.3 MenuType API |
| `bedrockskin` | Bedrock player skin & avatar proxying | `true` | Auto-caches Floodgate skins for 3D avatars |

---

## Module Configuration Reference

### 1. Web Application & API (`modules/web.yml`)
- `web.enabled` (`false`): Enable the embedded web server.
- `web.port` (`8080`): Javalin listening port. Ensure TCP port is open in firewall.
- `web.server_ip` (`localhost`): Server address shown on landing page.
- `web.allowed_origin` (`""`): CORS origin (e.g., `"https://panel.yourserver.com"`). Leave empty in dev.
- `web.deposit_limit` (`27`): Maximum item slots in web casino deposit box.
- `admin-password` (`"gens"`): Admin password for `/admin`. Hashed with BCrypt on startup.

### 2. Discord Bot (`modules/discord.yml`)
- `discord.bot_token`: Token from Discord Developer Portal.
- `discord.chat_channel_id`: Channel ID for in-game chat bridge.
- `discord.linked_role_id`: Role given to linked players.
- **Required Intents:** Server Members Intent and Message Content Intent must be enabled.

### 3. Custom Spawners (`modules/spawners.yml`)
- `spawners.delay` (`25`): Tick delay between spawn cycles (20 ticks = 1s).
- `spawners.holograms` (`true`): Floating text above spawner displaying tier and quantity.
- `spawners.max-stack` (`100000`): Maximum merged spawners per block.
- `spawners.upgrade-base-cost` (`1000.0`): Cost to upgrade to tier 2.
- `spawners.vanilla-require-silktouch` (`true`): Silk Touch required to pick up custom spawners.

### 4. Instanced Chests / Lootr (`modules/lootr.yml`)
- `lootr.prevent-hopper` (`true`): Blocks hoppers from taking chest contents.
- `lootr.prevent-break` (`false`): Prevents players from destroying dungeon chests.
- `lootr.particles-enabled` (`true`): Particles indicate unopened chests for each player.
- `lootr.break-confirm-time` (`3`): Double-hit confirmation timer in seconds.

### 5. Economy & Auction House (`modules/economy.yml`)
- `shop.inflation_exponent` (`0.5`): Price elasticity curve based on supply/demand.
- `ah.tax_percentage` (`0.0`): Commission tax percentage on Auction House sales.

### 6. Death Graves (`modules/tomb.yml`)
- `modules.tomb.block_type` (`CHEST`): Grave block (`CHEST`, `BARREL`, `PLAYER_HEAD`).
- `modules.tomb.store_xp` (`true`): Store player XP in grave.
- `modules.tomb.expiration_time_seconds` (`3600`): Despawn countdown in seconds.
- `modules.tomb.expiration_action` (`UNLOCK`): Action on expiry (`UNLOCK`, `DROP`, or `DESTROY`).
- `modules.tomb.default_access` (`OWNER_ONLY`): Access policy (`OWNER_ONLY` or `EVERYONE`).

### 7. Dynamic Shop & Items Catalog (`modules/shop.yml`)
- `categories.<category_name>`: Defines a shop category (e.g. `ores`, `farming`, `drops`, `blocks`, `nether`).
  - `displayName`: Formatted title displayed in `/shop`.
  - `icon`: Vanilla material identifier used as the category icon.
  - `items.<MATERIAL>`:
    - `buyPrice`: Base unit purchase price from the server shop.
    - `sellPrice`: Base unit sale price to the server shop (balanced at ~25% to 35% of purchase price).
    - `stock`: Current dynamic stock units.
    - `targetStock`: Equilibrium target capacity. Purchasing decreases stock (increasing prices); selling adds stock (decreasing prices).

### 8. Minigames & Casino (`modules/minigames.yml`)
- **Wheel of Fortune (`minigames.wheel`):**
  - `wheel.enabled` (`true`): Toggles the daily free wheel spin.
  - `wheel.rewards`: 8 balanced reward slices with `name`, `command` (executed on win), `chance` (percentage weight), and `color` (hex color on web UI).
- **Casino Slot Machine (`minigames.casino`):**
  - `casino.enabled` (`true`): Toggles the 3-reel web slot machine.
  - `jackpot_chance` (`4`): Triple 7s / jackpot chance (~500x).
  - `medium_win_chance` (`8`): 3 matching items chance (~300x).
  - `small_win_chance` (`20`): 2 matching items chance (~200x). Yields a balanced aggregate RTP (Return to Player) of ~84%.
- **CoinFlip (`minigames.coinflip`):**
  - `coinflip.enabled` (`true`): Toggles player duel coinflips.
  - `coinflip.multiplier` (`2`): Payout multiplier on winning bets.

### 9. Dynamic Scoreboard & Tablist (`modules/tabboard.yml`)
- **Scoreboard (`tabboard.scoreboard`):**
  - `title`: Header title component supporting MiniMessage formatting tags.
  - `lines`: Ordered list of scoreboard lines supporting internal placeholders (`%money%`, `%player%`, `%online%`) and LuckPerms prefixes.
- **Tablist (`tabboard.tablist`):**
  - `header`: Multi-line text banner above player usernames.
  - `footer`: Multi-line text banner below player usernames.
  - `show_platform_prefix` (`true`): Displays `[Bedrock]` tag for Floodgate mobile players.
  - `discord_linked` / `discord_not_linked`: Discord account verification badge indicators.

### 10. Teleportation Delays & Homes Quota (`modules/teleport.yml`)
- `modules.home.default_max` (`3`): Maximum personal homes permitted for default players without rank permissions or solo perks.
- `teleport-cooldown` (`3`): Warmup delay in seconds before executing `/spawn`, `/home`, or `/tpa`. Any movement or damage cancels the teleportation unless the player holds a bypass permission.

### 11. Daily Quests (`modules/quests.yml`)
- `quests.max_rerolls_per_day` (`3`): Maximum times a player can right-click an objective in `/quests` to reroll it each day.

### 12. Guild Weekly Quests (`modules/teams.yml`)
- `teams.last_rotation`: Epoch timestamp of the last weekly quest rotation.
- `teams.active_quest`: Currently assigned communal guild objective identifier (e.g. `weekly_1`).

### 13. Player & Mob Head Drops (`modules/headdrop.yml`)
- `headdrop.chance` (`10.0`): Percentage probability (0.0 to 100.0) for a defeated player or mob to drop their textured head upon death.

### 14. Server List Ping MOTD (`modules/motd.yml`)
- `motd.line1`: Top text line in the Minecraft multiplayer server list (supports MiniMessage color tags).
- `motd.line2`: Bottom text line in the multiplayer server list.

### 15. Chat System (`modules/chat.yml`)
- `chat.custom-join-messages` (`true`): Enables formatted welcome and departure announcements synchronized with LuckPerms ranks.

### 16. BlueMap 3D Visualizer (`modules/bluemap.yml`)
- `bluemap.url` (`"http://localhost:8100"`): Local or public URL of the BlueMap web server embedded into the Web Admin Panel territory iframe.

---

## Custom Virtual Inventory Menus (`menus/`)

GensCore allows administrators to create unlimited custom GUI menus in `plugins/GensCore/menus/` (e.g. `default.yml`, `tutorial.yml`).

```yaml
title: "<gradient:#00c6ff:#0072ff><bold>Server Hub</bold></gradient>"
size: 27
items:
  '11':
    material: "DIAMOND_SWORD"
    name: "<gold><bold>Shop & Economy"
    lore:
      - "<gray>Click to open the dynamic server shop."
    command: "shop"
  '13':
    material: "PLAYER_HEAD"
    name: "<green><bold>Jobs & Professions"
    lore:
      - "<gray>Click to view your career progress."
    command: "jobs"
  '15':
    material: "BEACON"
    name: "<aqua><bold>Guild System"
    lore:
      - "<gray>Click to manage your clan and territory."
    command: "team"
```

- **Dynamic Command Binding:** Any menu file created in `menus/<name>.yml` automatically registers the `/menu <name>` command!

---

## Creating & Configuring Custom Quests (`quests/`)

Beyond global daily reroll limits configured in `modules/quests.yml`, GensCore allows server administrators to create an unlimited collection of custom quests organized by categories inside `plugins/GensCore/quests/` (e.g. `easy.yml`, `medium.yml`, `hard.yml` provided by default, or your own files like `farming.yml`, `combat.yml`, `mining.yml`).

### How Quests are Distributed
- Each YAML file inside `plugins/GensCore/quests/` defines an active quest **category**.
- Every day at midnight (or upon a player's first daily login), GensCore randomly selects **up to 3 quests per category** for that player.
- Players open `/quests` (or `/quest`) to view their assigned objectives, live progress bars, and rewards.
- **Right-clicking** a quest in the GUI rerolls it (subject to `quests.max_rerolls_per_day` in `modules/quests.yml`).
- Every completed quest feeds the player's lifetime completion stats, unlocking perks in [Solo Perks](/guide/modules#12-solo-quest-perks-soloperkmodule) (`/perks`) and team weekly progress.

### YAML Quest File Structure

```yaml
quests:
  mine_diamonds:
    name: "<aqua><bold>Abyssal Diamond Hunter"
    menu_item: "DIAMOND"
    description:
      - "<gray>Mine 16 raw diamond ores deep underground."
      - "<yellow>Progress: <current>/<total>"
    quest_type: "BREAK"
    required:
      - "DIAMOND_ORE"
      - "DEEPSLATE_DIAMOND_ORE"
    required_amount: 16
    reward:
      commands:
        - "eco give %player% 750"
        - "tell %player% Congratulations on your find!"
```

### Quest Configuration Parameters

| Field | Type | Description |
|---|---|---|
| `name` | String | Formatted quest display title supporting modern **MiniMessage** tags (`<gradient>`, `<bold>`, etc.). |
| `menu_item` | Material | Vanilla Minecraft material used as the display icon inside `/quests` menu. |
| `description` | List | Lore lines displayed on hover. `<current>` and `<total>` tags are automatically replaced with live player progress. |
| `quest_type` | String | Objective action type among the 28 engine-supported types. |
| `required` | List / String | Targeted vanilla material or entity type. Can be a single item or a list of acceptable items. |
| `required_amount` | Integer | Goal count required to complete the quest. |
| `reward.commands` | List | Commands executed via console upon completion. Supports `%player%` and `{player}` variables. |

### All 28 Supported Quest Types (`quest_type`)

| `quest_type` | Trigger Action | Expected `required` Target | Example |
|---|---|---|---|
| `BREAK` | Mine or break a world block | Vanilla block material | `STONE`, `DIAMOND_ORE`, `OAK_LOG` |
| `PLACE` | Place a block into the world | Vanilla block material | `OBSIDIAN`, `TORCH`, `STONE_BRICKS` |
| `KILL` | Defeat a mob or player | Vanilla entity type | `ZOMBIE`, `SKELETON`, `CREEPER`, `PLAYER` |
| `CRAFT` | Craft an item in workbench | Resulting item material | `BREAD`, `IRON_CHESTPLATE`, `GOLDEN_APPLE` |
| `FISH` | Catch a fish or treasure | Caught item material | `COD`, `SALMON`, `PUFFERFISH`, `BOW` |
| `SHEAR` | Shear a woolly mob | Sheared entity type | `SHEEP`, `MOOSHROOM` |
| `COOK` | Smelt ores or cook food | Extracted item material | `COOKED_BEEF`, `IRON_INGOT`, `GLASS` |
| `CONSUME` | Eat food or drink potions | Consumed item material | `GOLDEN_CARROT`, `POTION`, `BREAD` |
| `BREED` | Breed two animals together | Bred entity type | `COW`, `SHEEP`, `CHICKEN`, `PIG` |
| `PICKUP` | Pick up dropped ground item | Picked item material | `EMERALD`, `NETHERITE_INGOT` |
| `TAME` | Tame a wild animal | Tamed entity type | `WOLF`, `CAT`, `HORSE`, `PARROT` |
| `VILLAGER_TRADE` | Complete trade with a villager | Villager type or item | `VILLAGER` |
| `GET` | Obtain an item in inventory | Material identifier | `ELYTRA`, `TOTEM_OF_UNDYING` |
| `MILKING` | Milk a cow using a bucket | Entity type | `COW`, `MOOSHROOM` |
| `EXP_POINTS` | Collect experience points | Point quota | `EXP_POINTS` |
| `EXP_LEVELS` | Reach an XP level quota | Level quota | `EXP_LEVELS` |
| `ENCHANT` | Enchant an item on enchanting table | Enchanted item material | `DIAMOND_SWORD`, `BOW`, `BOOK` |
| `CARVE` | Carve a pumpkin with shears | Block material | `PUMPKIN` |
| `PLAYER_DEATH` | Die in combat or by accident | Death reason / any | `PLAYER_DEATH` |
| `LOCATION` | Reach a region or coordinates | Region identifier | World Coordinates / Region |
| `FARMING` | Harvest mature crops | Crop item material | `WHEAT`, `CARROTS`, `POTATOES` |
| `LAUNCH` | Shoot an arrow or projectile | Projectile type | `ARROW`, `ENDER_PEARL`, `TRIDENT` |
| *Integrations* | Optional third-party plugins | Plugin dependency | `MYTHIC_MOBS`, `ELITE_MOBS`, `NU_VOTIFIER`, `PYRO_FISH`, `EMF_FISH`, `PLACEHOLDER` |

---

## Languages & Localization (`lang/`)

All player-facing messages, error prompts, and chat notifications are located in `plugins/GensCore/lang/` (`fr_FR.yml` and `en_US.yml`):
- **Full MiniMessage Support:** Rich modern text tags (`<gradient:#ff0000:#00ff00>text</gradient>`, `<hover:show_text:'Tooltip'>Click here</hover>`, `<click:run_command:'/spawn'>Spawn</click>`).
- **Dynamic Placeholders:** Messages automatically inject context variables like `<player>`, `<amount>`, `<target>`, `<balance>`.

---

## Network Ports & Port Forwarding

| Port | Protocol | Purpose | Required |
|---|---|---|---|
| **25565** | TCP/UDP | Minecraft Server | Yes |
| **8080** | TCP | Web Panel & REST API | Yes (if Web Panel enabled) |
| **8100** | TCP | BlueMap (Optional) | Optional |

### Nginx SSL Reverse Proxy Example
```nginx
server {
    server_name panel.yourserver.com;

    location / {
        proxy_pass http://127.0.0.1:8080;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

