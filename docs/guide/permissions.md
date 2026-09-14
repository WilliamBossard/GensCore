# Permissions & LuckPerms Guide

GensCore uses modern Bukkit permission nodes that seamlessly integrate with **LuckPerms** or any standard permission manager.

---

## Full Permissions Matrix

| Permission Node | Description | Default | Recommended Group |
|---|---|---|---|
| `genscore.chat` | Ability to speak in public chat | `true` | Default |
| `genscore.home` | Allows `/home`, `/sethome`, `/delhome` | `true` | Default |
| `genscore.spawn` | Allows `/spawn` | `true` | Default |
| `genscore.tpa` | Allows `/tpa`, `/tpaccept`, `/tpadeny`, `/tpacancel` | `true` | Default |
| `genscore.back` | Allows returning to death/teleport location | `false` | VIP / Donator |
| `genscore.ec` | Virtual Ender Chest (`/ec`) | `op` | VIP |
| `genscore.craft` | Virtual Workbench (`/craft`) | `op` | VIP |
| `genscore.anvil` | Virtual Anvil (`/anvil`) | `op` | VIP+ |
| `genscore.enchant` | Virtual Enchant Table (`/enchant`) | `op` | VIP+ |
| `genscore.feed` | Satisfy hunger (`/feed`) | `op` | VIP+ / Mod |
| `genscore.quests.reroll` | Right-click reroll daily quests in GUI | `op` | VIP / Donator |
| `genscore.quests.admin` | Force-reroll all quests | `op` | Admin |
| `genscore.discord.linked` | Verified Discord badge | `false` | Linked players |
| `genscore.freeze` | Freeze suspected players (`/freeze`) | `op` | Helper / Mod |
| `genscore.openinv` | Live inspect inventories (`/openinv`) | `op` | Moderator |
| `genscore.mute` | Mute/unmute players in chat | `op` | Moderator |
| `genscore.kick` | Kick players (`/kick`) | `op` | Moderator |
| `genscore.ban` | Ban/unban players (`/ban`) | `op` | Admin |
| `genscore.admin.spawner` | Spawn custom stacked spawners | `op` | Admin |
| `genscore.tomb.admin` | Access & manage any death grave | `op` | Admin |
| `genscore.admin` | Master administrator bypass | `op` | Owner / Admin |

---

## Cooldown Bypass Permissions

| Permission Node | Description |
|---|---|
| `genscore.bypass.cooldown.all` | Bypasses all teleportation warmup delays and cooldowns. |
| `genscore.bypass.cooldown.home` | Bypasses `/home` delay. |
| `genscore.bypass.cooldown.spawn` | Bypasses `/spawn` delay. |
| `genscore.bypass.cooldown.tpa` | Bypasses `/tpa` delay. |
| `genscore.bypass.cooldown.back` | Bypasses `/back` delay. |

---

## LuckPerms Setup Templates

Copy and paste these command blocks into your server console to configure your ranks instantly:

### 1. Default Player Group
```bash
lp group default permission set genscore.chat true
lp group default permission set genscore.home true
lp group default permission set genscore.spawn true
lp group default permission set genscore.tpa true
```

### 2. VIP / Donator Group
```bash
lp group vip parent add default
lp group vip permission set genscore.back true
lp group vip permission set genscore.ec true
lp group vip permission set genscore.craft true
lp group vip permission set genscore.quests.reroll true
lp group vip permission set genscore.bypass.cooldown.home true
```

### 3. Moderator Group
```bash
lp group moderator parent add vip
lp group moderator permission set genscore.freeze true
lp group moderator permission set genscore.openinv true
lp group moderator permission set genscore.mute true
lp group moderator permission set genscore.kick true
lp group moderator permission set genscore.feed true
```

### 4. Administrator Group
```bash
lp group admin parent add moderator
lp group admin permission set genscore.ban true
lp group admin permission set genscore.admin true
lp group admin permission set genscore.admin.spawner true
lp group admin permission set genscore.tomb.admin true
lp group admin permission set genscore.quests.admin true
lp group admin permission set genscore.bypass.cooldown.all true
```
