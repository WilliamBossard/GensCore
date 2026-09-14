# Web Panel & Minigames

GensCore includes an integrated **React 18 + Javalin** web server running on port `8080`.

---

## Security & Architecture

1. **Embedded Micro-Server:** Javalin runs inside the Minecraft server process with zero external web server configuration required.
2. **BCrypt Password Protection:** The default admin password (`gens` in `modules/web.yml`) is automatically converted to a secure BCrypt hash on first startup.
3. **Session Tokens:**
   - Admin routes use 24-hour Bearer tokens.
   - Player sessions use 7-day tokens tied to `/register` credentials.
4. **Brute Force Rate Limiting:** Locks out any IP address attempting more than 5 failed logins per minute.

---

## Admin Control Dashboard

Access at: `http://<your-server-ip>:8080/admin`

- **Live Performance Metrics:** Real-time gauges for TPS, active players, JVM memory allocation, and CPU load.
- **Interactive Web Console:** Real-time server log streaming with command dispatching safely routed to Folia's `GlobalRegionScheduler`.
- **Player Management:** Search players, inspect balances, adjust money, and execute kicks/bans/mutes from the browser.
- **Live Module Switchboard:** Toggle any of the 28 modules on or off with a single click.
- **Dynamic Config Editor:** Modify shop inflation, auction house fees, head drop rates, quest reroll limits, and MOTD lines live.
- **Integrated BlueMap:** Embedded interactive 3D/2D territory map.
- **Emergency Server Wipe:** Password-guarded atomic database reset button.

---

## Player Web Portal

Access at: `http://<your-server-ip>:8080`

- **Player Dashboard:** Login with in-game `/register` credentials.
- **Live Stats:** Wallet balance, global job levels, blocks broken, mobs killed, deaths, and K/D ratio.
- **7-Day Quest Activity:** Visual SVG graph tracking daily quest completion history.
- **Transaction History:** Review your last 5 financial transactions.
- **Head & Avatar Engine (`/api/head/{name}/{size}`):** Renders high-resolution 3D avatars for both Java and Bedrock players.

---

## Web Minigames

### 1. Wheel of Fortune (Roue de la Fortune)
- Daily spin available once every 24 hours.
- Configurable weighted loot table:
  - 10 Diamonds (40%)
  - 2 Netherite Ingots (30%)
  - 1 Enchanted Golden Apple (20%)
  - 1 Elytra (9%)
  - 1 Cow Spawner (1% Jackpot)
- **Offline Safe:** If a player spins while disconnected, the prize is queued in SQLite and automatically delivered upon their next login via Folia's scheduler.

### 2. Web Casino (Slot Machine)
- Bet real in-game items on an animated 3-reel web slot machine.
- **1-Hour Cooldown** between spins to prevent spamming.
- **Deposit (`/web deposit`):** Hold any item in your main hand in-game and type `/web deposit`. The item is serialized to Base64 and stored in your web casino inventory.
- **Play:** Spin the machine in your web browser with your deposited items.
- **Withdraw (`/web withdraw`):** Run `/web withdraw` in Minecraft to open a 54-slot GUI and collect all your winnings back into your inventory!
