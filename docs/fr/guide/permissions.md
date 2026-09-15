# Guide des Permissions & LuckPerms

GensCore utilise une nomenclature Bukkit moderne qui s'intègre harmonieusement avec **LuckPerms** ou tout gestionnaire de permissions standard.

---

## Matrice Complète des Permissions

| Nœud de Permission | Description | Défaut | Groupe Recommandé |
|---|---|---|---|
| `genscore.chat` | Autorisation de parler dans le chat public | `true` | Default (Joueur) |
| `genscore.home` | Autorise `/home`, `/sethome`, `/delhome` | `true` | Default (Joueur) |
| `genscore.spawn` | Autorise `/spawn` | `true` | Default (Joueur) |
| `genscore.tpa` | Autorise `/tpa`, `/tpaccept`, `/tpadeny`, `/tpacancel` | `true` | Default (Joueur) |
| `genscore.back` | Permet de retourner au lieu de mort ou TP | `false` | VIP / Donateur |
| `genscore.ec` | Ender Chest virtuel (`/ec`) | `op` | VIP |
| `genscore.craft` | Établi virtuel (`/craft`) | `op` | VIP |
| `genscore.anvil` | Enclume virtuelle (`/anvil`) | `op` | VIP+ |
| `genscore.enchant` | Table d'enchantement virtuelle (`/enchant`) | `op` | VIP+ |
| `genscore.feed` | Remplir sa nourriture (`/feed`) | `op` | VIP+ / Modérateur |
| `genscore.quests.reroll` | Relancer une quête journalière par clic droit | `op` | VIP / Donateur |
| `genscore.quests.admin` | Forcer la relance de toutes les quêtes | `op` | Administrateur |
| `genscore.discord.linked` | Badge joueur vérifié Discord | `false` | Joueurs synchronisés |
| `genscore.freeze` | Geler les suspects (`/freeze`) | `op` | Assistant / Modérateur |
| `genscore.openinv` | Inspecter l'inventaire en direct (`/openinv`) | `op` | Modérateur |
| `genscore.mute` | Muter / démuter un joueur dans le chat | `op` | Modérateur |
| `genscore.kick` | Expulser un joueur (`/kick`) | `op` | Modérateur |
| `genscore.ban` | Bannir / débannir un joueur (`/ban`) | `op` | Administrateur |
| `genscore.admin.spawner` | Créer des spawners empilables sur mesure | `op` | Administrateur |
| `genscore.tomb.admin` | Accéder et gérer n'importe quelle tombe de mort | `op` | Administrateur |
| `genscore.admin` | Permission maîtresse de contournement admin | `op` | Fondateur / Admin |

---

## Permissions de Contournement des Délais (Cooldown Bypass)

| Nœud de Permission | Description |
|---|---|
| `genscore.bypass.cooldown.all` | Contourne tous les temps de chauffe (warmup) et délais d'attente (cooldown). |
| `genscore.bypass.cooldown.home` | Contourne le délai du `/home`. |
| `genscore.bypass.cooldown.spawn` | Contourne le délai du `/spawn`. |
| `genscore.bypass.cooldown.tpa` | Contourne le délai du `/tpa`. |
| `genscore.bypass.cooldown.back` | Contourne le délai du `/back`. |

---

## Modèles de Configuration LuckPerms

Copiez-collez ces blocs de commandes dans votre console serveur pour configurer vos grades instantanément :

### 1. Groupe Joueur par Défaut (`default`)
```bash
lp group default permission set genscore.chat true
lp group default permission set genscore.home true
lp group default permission set genscore.spawn true
lp group default permission set genscore.tpa true
```

### 2. Groupe VIP / Donateur (`vip`)
```bash
lp group vip parent add default
lp group vip permission set genscore.back true
lp group vip permission set genscore.ec true
lp group vip permission set genscore.craft true
lp group vip permission set genscore.quests.reroll true
lp group vip permission set genscore.bypass.cooldown.home true
```

### 3. Groupe Modérateur (`moderator`)
```bash
lp group moderator parent add vip
lp group moderator permission set genscore.freeze true
lp group moderator permission set genscore.openinv true
lp group moderator permission set genscore.mute true
lp group moderator permission set genscore.kick true
lp group moderator permission set genscore.feed true
```

### 4. Groupe Administrateur (`admin`)
```bash
lp group admin parent add moderator
lp group admin permission set genscore.ban true
lp group admin permission set genscore.admin true
lp group admin permission set genscore.admin.spawner true
lp group admin permission set genscore.tomb.admin true
lp group admin permission set genscore.quests.admin true
lp group admin permission set genscore.bypass.cooldown.all true
```
