# Commands Reference

All commands in GensCore are parsed asynchronously through the **Cloud Command Framework** with native Brigadier completions and Folia thread safety.

---

## Authentication & Accounts

| Command | Arguments | Permission | Default | Description |
|---|---|---|---|---|
| `/register` | `<password> <confirmPassword>` | *None* | Everyone | Registers an account and hashes password with BCrypt. |
| `/login` | `<password>` | *None* | Everyone | Logs in to your account. Interactions are frozen until authenticated. |
| `/changemdp` | `<oldPassword> <newPassword>` | *None* | Everyone | Changes your account password. *(Alias: `/changepassword`)* |
| `/resetmdp` | `<player>` | `genscore.admin` | OP | Resets a player's password, forcing them to re-register. |

---

## Economy & Dynamic Shop

| Command | Arguments | Permission | Default | Description |
|---|---|---|---|---|
| `/money` | *None* | *None* | Everyone | Displays your current wallet balance. *(Alias: `/balance`)* |
| `/money` | `<player>` | `genscore.admin` | OP | Views another player's balance. |
| `/baltop` | *None* | *None* | Everyone | Displays the top wealthiest players leaderboard. |
| `/pay` | `<player> <amount>` | *None* | Everyone | Transfers funds to another player. |
| `/eco set` | `<player> <amount>` | `genscore.admin` | OP | Sets a player's balance. |
| `/eco give` | `<player> <amount>` | `genscore.admin` | OP | Adds funds to a player's balance. |
| `/eco take` | `<player> <amount>` | `genscore.admin` | OP | Withdraws funds from a player's balance. |
| `/eco reset` | `<player>` | `genscore.admin` | OP | Resets a player's balance to default. |
| `/shop` | *None* | *None* | Everyone | Opens the dynamic server shop GUI. |

---

## Auction House

| Command | Arguments | Permission | Default | Description |
|---|---|---|---|---|
| `/ah` | *None* | *None* | Everyone | Opens the Auction House browsing menu. *(Aliases: `/auctionhouse`, `/hdv`)* |
| `/ah sell` | `<price>` | *None* | Everyone | Lists the item in your hand for sale. |

---

## Guilds & Teams
Comprehensive clan system with shared treasury ($ or XP), territorial chunk claims ($16 \times 16$), on-screen boundary notifications, real-time BlueMap rendering, 3-tier role hierarchy (Leader, Admin, Member), and permanent team perks.

| Command | Arguments | Permission | Default | Description |
|---|---|---|---|---|
| `/team` | *None* | *None* | Everyone | Opens the Guild management GUI. *(Aliases: `/guild`, `/guilde`, `/teams`)* |
| `/team create` | `<name>` | *None* | Everyone | Creates a new guild (max 16 characters). |
| `/team invite` | `<player>` | *None* | Admin / Leader | Invites a player to join your guild. |
| `/team accept` | *None* | *None* | Everyone | Accepts a pending guild invite. |
| `/team kick` | `<player>` | *None* | Admin / Leader | Kicks a member from the guild (Admins cannot kick the Leader or other Admins). |
| `/team promote` | `<player>` | *None* | Leader | Promotes a member to Administrator (`ADMIN`). |
| `/team demote` | `<player>` | *None* | Leader | Demotes an Administrator back to regular Member. |
| `/team leave` | *None* | *None* | Member / Admin | Leaves your current guild (the Leader must disband the guild). |
| `/team disband` | *None* | *None* | Leader | Permanently disbands the guild and releases all claimed chunks. |
| `/team quest` | *None* | *None* | Everyone | Opens the shared guild quest menu. |
| `/team upgrades` | *None* | *None* | Everyone | Opens the guild upgrades shop menu. |
| `/team deposit` | `<amount>` | *None* | Everyone | Deposits dollars into the guild bank (when Economy is active). |
| `/team depositxp` | `<levels>` | *None* | Everyone | Deposits XP levels into the guild bank (when Economy is disabled). |
| `/team withdraw` | `<amount>` | *None* | Admin / Leader | Withdraws dollars from the guild bank. |
| `/team withdrawxp` | `<levels>` | *None* | Admin / Leader | Withdraws XP levels from the guild bank. |
| `/team claim` | *None* | *None* | Admin / Leader | Claims the current chunk ($16 \times 16$). Strictly funded from the guild bank. |
| `/team unclaim` | *None* | *None* | Admin / Leader | Releases the current chunk claim back to the wild. |
| `/team color` | `<hex>` | *None* | Admin / Leader | Sets guild territory color on BlueMap (e.g. `#3498db`). |

---

## Jobs & Professions

| Command | Arguments | Permission | Default | Description |
|---|---|---|---|---|
| `/jobs` | *None* | *None* | Everyone | Opens the Jobs menu. *(Aliases: `/job`, `/metier`, `/metiers`)* |

Available professions: **Miner**, **Lumberjack**, **Hunter**, **Farmer**, **Fisherman**, and **Builder**.

---

## Daily Quests

| Command | Arguments | Permission | Default | Description |
|---|---|---|---|---|
| `/quests` | *None* | *None* | Everyone | Opens daily quests menu. *(Aliases: `/quest`, `/quete`, `/quetes`)* |

- Right-click reroll requires `genscore.quests.reroll`.
- Admin forced reroll requires `genscore.quests.admin`.

---

## Teleportation & Navigation

| Command | Arguments | Permission | Default | Description |
|---|---|---|---|---|
| `/spawn` | *None* | `genscore.spawn` | `true` | Teleports to the world spawn. |
| `/setspawn` | *None* | `genscore.admin` | OP | Sets the global spawn point. |
| `/sethome` | `[name]` | `genscore.home` | `true` | Sets a home waypoint. |
| `/home` | `[name]` | `genscore.home` | `true` | Teleports to a home waypoint. |
| `/delhome` | `[name]` | `genscore.home` | `true` | Deletes a home waypoint. |
| `/back` | *None* | `genscore.back` | `false` | Returns to your last death or teleport location. |
| `/tpa` | `<player>` | `genscore.tpa` | `true` | Sends a teleport request. |
| `/tpaccept` | *None* | `genscore.tpa` | `true` | Accepts an incoming teleport request. |
| `/tpadeny` | *None* | `genscore.tpa` | `true` | Refuses an incoming teleport request. |
| `/tpacancel` | *None* | `genscore.tpa` | `true` | Cancels your sent teleport request. |

---

## Container Security & Locks

| Command | Arguments | Permission | Default | Description |
|---|---|---|---|---|
| `/lock` | `[private]` | *None* | Everyone | Right-click a container to lock it for yourself. *(Alias: `/cprivate`)* |
| `/lock unlock` | *None* | *None* | Everyone | Right-click your container to remove the lock. |
| `/lock guild` | *None* | *None* | Everyone | Right-click a container to share access with your guild. |

---

## Custom Spawners

| Command | Arguments | Permission | Default | Description |
|---|---|---|---|---|
| `/spawner give` | `<player> <type> [stack]` | `genscore.admin.spawner` | OP | Gives a custom stacked spawner block to a player. |

---

## Survival Utilities

| Command | Arguments | Permission | Default | Description |
|---|---|---|---|---|
| `/ec` | *None* | `genscore.ec` | OP | Opens your virtual Ender Chest. *(Alias: `/enderchest`)* |
| `/craft` | *None* | `genscore.craft` | OP | Opens a virtual 3x3 workbench. *(Aliases: `/craftingtable`, `/workbench`)* |
| `/anvil` | *None* | `genscore.anvil` | OP | Opens a virtual Anvil. |
| `/enchant` | *None* | `genscore.enchant` | OP | Opens a virtual Enchanting Table. *(Alias: `/enchanttable`)* |
| `/feed` | *None* | `genscore.feed` | OP | Fills your hunger and saturation bar. |

---

## Staff & Moderation

| Command | Arguments | Permission | Default | Description |
|---|---|---|---|---|
| `/ban` | `<player> [duration] [reason]` | `genscore.ban` | OP | Bans a player. |
| `/unban` | `<player>` | `genscore.ban` | OP | Unbans a player. |
| `/mute` | `<player> [duration] [reason]` | `genscore.mute` | OP | Mutes a player in chat. |
| `/unmute` | `<player>` | `genscore.mute` | OP | Unmutes a player. |
| `/kick` | `<player> [reason]` | `genscore.kick` | OP | Kicks a player from the server. |
| `/freeze` | `<player>` | `genscore.freeze` | OP | Freezes a player in place for screenshare checks. |
| `/openinv` | `<player>` | `genscore.openinv` | OP | Live views player inventory & armor. *(Alias: `/invsee`)* |

---

## Web Casino Bridge

| Command | Arguments | Permission | Default | Description |
|---|---|---|---|---|
| `/web` | *None* | *None* | Everyone | Help message for web casino inventory bridge. |
| `/web deposit` | *None* | *None* | Everyone | Deposits held item into your web casino betting inventory. |
| `/web withdraw` | *None* | *None* | Everyone | Opens GUI to retrieve won items from the web slot machine. |

---

## Core Management

| Command | Arguments | Permission | Default | Description |
|---|---|---|---|---|
| `/module` | `<moduleName> <on\|off>` | `genscore.admin` | OP | Toggles any of the 28 modules live. |
| `/menu` | `[name]` | *None* | Everyone | Opens a custom YAML menu from `plugins/GensCore/menus/`. |
