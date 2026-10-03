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
| `minigame` | Fortune Wheel, Casino Slots & CoinFlip | `true` | Configured via `modules/minigames.yml` & Web Panel |
| `motd` | Dynamic server list ping message | `true` | Configured via `modules/motd.yml` |
| `tabboard` | Custom Scoreboard and Tablist layout | `true` | Configured via `modules/tabboard.yml` |
| `discord` | Discord Bot bridge (chat sync, staff logs) | `true` | Requires valid bot token in `modules/discord.yml` |
| `gui` | In-game menu provider | `true` | Auto-detects Bedrock clients (Cumulus Forms) |
| `web` | Embedded Javalin REST API & React Dashboard | `true` | Configured via `modules/web.yml` |
| `tomb` | Death graves preserving items & XP upon player death | `false` | Enable if you want graves instead of standard drops |
| `home` | Player home waypoints (`/sethome`, `/home`) | `true` | Limits set via permissions and `modules/teleport.yml` |
| `back` | Return to last death or teleport location (`/back`) | `true` | Zero-leak `BackPosition` memory safety |
| `spawn` | Server spawn location (`/setspawn`, `/spawn`) | `true` | Persistent spawn coordinates in database |
| `tpa` | Teleport request system (`/tpa`, `/tpaccept`, etc.) | `true` | Configurable warmup and movement cancellation |
| `teams` | Guilds, territory claims, shared bank & weekly quests | `true` | Integrated with BlueMap and Web Panel |
| `lock` | Container protection for chests, barrels & furnaces | `true` | Anti-hopper and anti-piston theft protection |
| `auth` | In-game authentication for offline servers (`/login`) | `true` | BCrypt password hashing |
| `moderation` | Staff toolset (`/ban`, `/mute`, `/kick`, `/freeze`) | `true` | Persistent punishment records in database |
| `chat` | Chat formatting and join/quit messages | `true` | LuckPerms prefix/suffix synchronization |
| `bluemap` | BlueMap guild territory claim visualization | `false` | Requires BlueMap installed on the server |
| `fastleafdecay` | Instant natural decay for orphaned tree leaves | `true` | High-performance leaves cleanup |

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

