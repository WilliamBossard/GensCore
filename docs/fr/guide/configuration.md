# Configuration & Déploiement
 
GensCore génère une arborescence de configuration claire, modulaire et structurée dans `plugins/GensCore/`.
 
---
 
## Arborescence des Fichiers
 
```
plugins/GensCore/
├── config.yml              # Paramètres globaux (langue, base de données SQLite/MySQL)
├── modules.yml             # Activation/Désactivation générale des 30 modules
├── genscore.db             # Base de données locale SQLite (mode WAL, pool HikariCP)
├── lang/
│   ├── fr_FR.yml           # Fichier de traduction française (balises MiniMessage)
│   └── en_US.yml           # Fichier de traduction anglaise (balises MiniMessage)
├── menus/
│   └── default.yml         # Menus inventaires virtuels personnalisables en YAML
├── modules/
│   ├── bluemap.yml         # Intégration de la carte BlueMap dans le Web Panel
│   ├── chat.yml            # Format du chat et alertes de connexion/déconnexion
│   ├── discord.yml         # Bot Discord JDA 6, salons et synchronisation
│   ├── economy.yml         # Exposant d'inflation dynamique et taxe de vente HDV
│   ├── headdrop.yml        # Probabilités de drop des têtes de joueurs et monstres
│   ├── lootr.yml           # Coffres de donjon instanciés et protection anti-casse
│   ├── minigames.yml       # Roue de la fortune, Casino Slots (RTP 84%), CoinFlip
│   ├── motd.yml            # Lignes du MOTD affichées dans la liste des serveurs
│   ├── quests.yml          # Quotas de quêtes journalières et relances (rerolls)
│   ├── shop.yml            # Boutique dynamique, prix de base, élasticité des stocks
│   ├── spawners.yml        # Spawners empilables (stacking), cadence et Silk Touch
│   ├── tabboard.yml        # Scoreboard dynamique, Tablist et placeholders
│   ├── teams.yml           # Paramètres de rotation des quêtes de guilde
│   ├── teleport.yml        # Nombre maximal de homes par défaut et délais
│   ├── tomb.yml            # Tombes de mort, conservation de l'XP et accès
│   └── web.yml             # Serveur web Javalin, auth BCrypt, CORS et limites
└── web/                    # Bundle frontend React / Vite prêt pour la production
```
 
---

## Configuration Principale (`config.yml`)

GensCore supporte nativement une architecture multi-moteur de base de données :

```yaml
# Fichier de langue actif (fr_FR ou en_US)
lang: "fr_FR"

# Configuration de la base de donnees
database:
  # Type de base : "sqlite" (autonome, par defaut) ou "mysql" (serveur distant)
  type: "sqlite"

  # Configuration SQLite locale
  sqlite:
    file: "genscore.db"

  # Configuration MySQL / MariaDB externe (optionnelle)
  # En cas d'indisponibilité du serveur distant, GensCore bascule automatiquement sur SQLite
  mysql:
    host: "localhost"
    port: 3306
    database: "genscore"
    username: "root"
    password: "VOTRE_MOT_DE_PASSE"
    ssl: false
    max_pool_size: 10
    minimum_idle: 2
    connection_timeout: 30000
```

> [!TIP]
> **Tolérance aux pannes (Failover automatique) :** Si `type: "mysql"` est configuré mais que le serveur distant est injoignable ou rejette la connexion, GensCore bascule instantanément sur la base SQLite locale sans jamais faire crasher le serveur Minecraft.

---

## Interrupteur des Modules (`modules.yml`)

Chaque fonctionnalité de GensCore peut être activée ou désactivée indépendamment. Vous pouvez également basculer un module en jeu sans redémarrer le serveur via `/module <nom> on|off`.

| Clé | Fonctionnalité | Par défaut | Description & Dépendances |
|---|---|---|---|
| `dynamicshop` | Boutique avec offre et demande (`/shop`) | `true` | Dépend du module `economy` |
| `economy` | Soldes, `/money`, `/pay`, support Vault | `true` | Enregistre `GensVaultEconomy` si Vault est installé |
| `auctionhouse` | Hôtel des ventes entre joueurs (`/ah`) | `true` | Dépend du module `economy` |
| `jobs` | Métiers (Mineur, Bûcheron, Fermier, Chasseur) | `true` | Cache bitwise anti-farm sécurisé multi-thread |
| `stats` | Suivi des blocs cassés, kills, morts, temps de jeu | `true` | Consultable sur le dashboard web joueur |
| `spawners` | Spawners personnalisés empilables et améliorables | `true` | Configuré via `modules/spawners.yml` |
| `quests` | Quêtes journalières avec relances | `true` | Configuré via `modules/quests.yml` |
| `lootr` | Coffres de donjon instanciés par joueur | `true` | Configuré via `modules/lootr.yml` |
| `headdrop` | Drop de têtes de monstres et joueurs | `true` | Configuré via `modules/headdrop.yml` |
| `minigames` | Roue de la fortune, machine à sous, pile ou face | `true` | Configuré via `modules/minigames.yml` |
| `motd` | MOTD dynamique avec tags MiniMessage | `true` | Configuré via `modules/motd.yml` |
| `tabboard` | Scoreboard et Tablist dynamiques | `true` | Configuré via `modules/tabboard.yml` |
| `discord` | Pont Discord Bot (chat et logs staff) | `true` | Nécessite un bot token dans `modules/discord.yml` |
| `gui` | Gestionnaire d'inventaires virtuels | `true` | Détecte nativement les joueurs Bedrock (Geyser/Floodgate) |
| `tomb` | Tombes de mort protégeant stuff et XP | `false` | Activer si vous préférez des tombes aux drops au sol |
| `home` | Points de téléportation personnels (`/home`) | `true` | Quotas configurables par permissions |
| `back` | Retour au point de mort ou téléportation (`/back`) | `true` | Zéro fuite mémoire (`record BackPosition`) |
| `spawn` | Point d'apparition principal (`/spawn`) | `true` | Persistant en base de données |
| `tpa` | Requêtes de téléportation entre joueurs | `true` | Annulation automatique en cas de mouvement |
| `teams` | Guildes, claims de territoire, banque commune | `true` | Intégré à BlueMap et au panel web |
| `locks` | Verrouillage anti-vol des coffres et fours | `true` | Protection contre les pistons et entonnoirs |
| `auth` | Authentification en jeu (`/login`, `/register`) | `true` | Hashage sécurisé des mots de passe en BCrypt |
| `moderation` | Outils de modération (`/ban`, `/mute`, `/freeze`) | `true` | Sanctions persistantes en base de données |
| `chat` | Format du chat et synchronisation des préfixes | `true` | Synchronisation automatique avec LuckPerms |
| `bluemap` | Affichage des territoires sur BlueMap | `false` | Nécessite le plugin BlueMap |
| `fastleafdecay` | Disparition rapide des feuilles d'arbres | `true` | Optimisé pour Paper et Folia |
| `solo_perks` | Avantages et maîtrises de quêtes solo (`/perks`) | `true` | Synchronisé avec les quêtes et l'économie |
| `customgui` | Chargeur de menus virtuels YAML (`/menu`) | `true` | Configuré via `plugins/GensCore/menus/` |
| `utils` | Utilitaires virtuels (`/craft`, `/anvil`, `/ec`, `/feed`) | `true` | API native Paper 26.3 MenuType |
| `bedrockskin` | Proxy et cache des skins & avatars Bedrock | `true` | Cache local Floodgate pour les têtes 3D |

---

## Guide des Fichiers de Modules

### 1. Panel Web & API REST (`modules/web.yml`)
- `web.enabled` (`false`) : Activer le serveur web.
- `web.port` (`8080`) : Port d'écoute Javalin. À ouvrir dans le pare-feu.
- `web.server_ip` (`localhost`) : IP affichée aux visiteurs sur le portail public.
- `web.allowed_origin` (`""`) : Restriction CORS. Laissez vide en dev ou renseignez le domaine (ex: `"https://panel.genscore.com"`).
- `web.deposit_limit` (`27`) : Nombre d'objets maximum déposables dans le coffre du casino web via `/web deposit`.
- `admin-password` (`"gens"`) : Mot de passe d'accès à l'administration web (`/admin`). Au démarrage, GensCore le remplace automatiquement par un hash BCrypt sécurisé.

### 2. Bot Discord (`modules/discord.yml`)
- `discord.bot_token` : Token de votre bot créé sur le Discord Developer Portal.
- `discord.chat_channel_id` : ID du salon textuel synchronisé avec le chat du serveur.
- `discord.linked_role_id` : Rôle Discord attribué aux joueurs liés.
- **Intents requis :** Cochez impérativement *Server Members Intent* et *Message Content Intent* sur Discord.

### 3. Spawners Personnalisés (`modules/spawners.yml`)
- `spawners.delay` (`25`) : Intervalle en ticks entre chaque cycle d'apparition de mobs.
- `spawners.holograms` (`true`) : Affiche le niveau et la quantité empilée au-dessus du spawner.
- `spawners.max-stack` (`100000`) : Limite maximale de spawners fusionnés sur un même bloc.
- `spawners.upgrade-base-cost` (`1000.0`) : Coût financier de l'amélioration au niveau supérieur.
- `spawners.vanilla-require-silktouch` (`true`) : Exige l'enchantement Toucher de Soie (Silk Touch) pour récupérer le spawner.

### 4. Coffres Instanciés Lootr (`modules/lootr.yml`)
- `lootr.prevent-hopper` (`true`) : Empêche les entonnoirs d'aspirer le contenu des coffres instanciés.
- `lootr.prevent-break` (`false`) : Interdit aux joueurs de casser les coffres de donjon.
- `lootr.particles-enabled` (`true`) : Particules indiquant à chaque joueur qu'il n'a pas encore ouvert ce coffre.
- `lootr.break-confirm-time` (`3`) : Délai en secondes pour confirmer la destruction d'un coffre Lootr.

### 5. Économie & HDV (`modules/economy.yml`)
- `shop.inflation_exponent` (`0.5`) : Exposant d'élasticité régissant la variation dynamique des prix de la boutique selon les stocks.
- `ah.tax_percentage` (`0.0`) : Taxe prélevée sur le montant des ventes réalisées à l'hôtel des ventes.

### 6. Tombes de Mort (`modules/tomb.yml`)
- `modules.tomb.block_type` (`CHEST`) : Matériau du bloc (`CHEST`, `BARREL`, `PLAYER_HEAD`).
- `modules.tomb.store_xp` (`true`) : Stocke l'expérience du joueur dans la tombe.
- `modules.tomb.expiration_time_seconds` (`3600`) : Durée avant déclenchement de l'action d'expiration.
- `modules.tomb.expiration_action` (`UNLOCK`) : Comportement à expiration (`UNLOCK`, `DROP` au sol, ou `DESTROY`).
- `modules.tomb.default_access` (`OWNER_ONLY`) : Accès réservé au défunt (`OWNER_ONLY`) ou ouvert à tous (`EVERYONE`).

### 7. Boutique Dynamique & Catalogue d'Objets (`modules/shop.yml`)
- `categories.<nom_categorie>` : Définit une catégorie d'articles (ex: `ores`, `farming`, `drops`, `blocks`, `nether`).
  - `displayName` : Nom formaté de la catégorie affiché dans l'interface `/shop`.
  - `icon` : Matériau vanilla utilisé comme icône du menu de catégorie.
  - `items.<MATERIAU>` :
    - `buyPrice` : Prix d'achat unitaire de base auprès du serveur.
    - `sellPrice` : Prix de vente unitaire de base au serveur (calibré à ~25-35% du prix d'achat pour prévenir tout arbitrage).
    - `stock` : Quantité actuelle en stock dynamique.
    - `targetStock` : Stock cible d'équilibre. Les achats diminuent le stock (font monter le prix) ; les ventes alimentent le stock (font baisser le prix).

### 8. Mini-Jeux & Casino Web (`modules/minigames.yml`)
- **Roue de la Fortune (`minigames.wheel`) :**
  - `wheel.enabled` (`true`) : Active le tirage gratuit quotidien.
  - `wheel.rewards` : 8 parts de récompenses équilibrées avec `name`, `command` (exécutée en console lors du gain), `chance` (poids en pourcentage), et `color` (couleur hexadécimale sur l'interface web).
- **Machine à Sous Web (`minigames.casino`) :**
  - `casino.enabled` (`true`) : Active la machine à sous animée à 3 rouleaux sur le panel web.
  - `jackpot_chance` (`4`) : Probabilité du triple 7 / jackpot (~500x).
  - `medium_win_chance` (`8`) : Probabilité de 3 symboles identiques (~300x).
  - `small_win_chance` (`20`) : Probabilité de 2 symboles identiques (~200x). Assure un taux de redistribution théorique (RTP) équilibré d'environ 84%.
- **CoinFlip (`minigames.coinflip`) :**
  - `coinflip.enabled` (`true`) : Active les duels joueur-contre-joueur à pile ou face.
  - `coinflip.multiplier` (`2`) : Multiplicateur appliqué à la mise du vainqueur.

### 9. Scoreboard Dynamique & Tablist (`modules/tabboard.yml`)
- **Scoreboard (`tabboard.scoreboard`) :**
  - `title` : Titre d'en-tête acceptant les balises MiniMessage.
  - `lines` : Lignes personnalisables acceptant les variables intégrées (`%money%`, `%player%`, `%online%`) et les méta LuckPerms.
- **Tablist (`tabboard.tablist`) :**
  - `header` : Bannière de bienvenue au-dessus de la liste des joueurs.
  - `footer` : Pied de page sous la liste des joueurs.
  - `show_platform_prefix` (`true`) : Affiche le tag `[Bedrock]` pour les joueurs mobiles Floodgate.
  - `discord_linked` / `discord_not_linked` : Badges visuels de vérification Discord.

### 10. Délais de Téléportation & Quota de Homes (`modules/teleport.yml`)
- `modules.home.default_max` (`3`) : Nombre maximum de homes personnels autorisés pour les joueurs sans grade ni perk solo.
- `teleport-cooldown` (`3`) : Délai de préchauffage (warmup) en secondes avant l'exécution de `/spawn`, `/home` ou `/tpa`. Tout déplacement ou coup reçu annule la téléportation, sauf avec une permission de contournement.

### 11. Quêtes Journalières (`modules/quests.yml`)
- `quests.max_rerolls_per_day` (`3`) : Nombre maximum de relances par clic droit autorisées chaque jour dans `/quests`.

### 12. Quêtes Hebdomadaires de Guilde (`modules/teams.yml`)
- `teams.last_rotation` : Horodatage (epoch) de la dernière rotation de quête hebdomadaire de guilde.
- `teams.active_quest` : Identifiant de la quête collective active (ex: `weekly_1`).

### 13. Têtes de Joueurs & Monstres (`modules/headdrop.yml`)
- `headdrop.chance` (`10.0`) : Pourcentage de probabilité (0.0 à 100.0) qu'un monstre ou un joueur vaincu fasse tomber sa tête avec skin lors de sa mort.

### 14. Message Serveur MOTD (`modules/motd.yml`)
- `motd.line1` : Première ligne affichée dans la liste des serveurs multijoueur (balises MiniMessage supportées).
- `motd.line2` : Seconde ligne affichée dans la liste des serveurs.

### 15. Système de Chat (`modules/chat.yml`)
- `chat.custom-join-messages` (`true`) : Active les annonces formatées d'arrivée et de départ synchronisées avec les préfixes LuckPerms.

### 16. Carte 3D BlueMap (`modules/bluemap.yml`)
- `bluemap.url` (`"http://localhost:8100"`) : Adresse du serveur BlueMap intégrée dans l'iframe du panel web d'administration.

---

## Menus Virtuels Personnalisés en YAML (`menus/`)

GensCore permet aux administrateurs de concevoir une infinité de menus virtuels d'inventaire dans le dossier `plugins/GensCore/menus/` (ex : `default.yml`, `tutoriel.yml`).

```yaml
title: "<gradient:#00c6ff:#0072ff><bold>Menu Principal</bold></gradient>"
size: 27
items:
  '11':
    material: "DIAMOND_SWORD"
    name: "<gold><bold>Boutique & Économie"
    lore:
      - "<gray>Cliquez pour ouvrir la boutique du serveur."
    command: "shop"
  '13':
    material: "PLAYER_HEAD"
    name: "<green><bold>Métiers & Professions"
    lore:
      - "<gray>Consultez votre progression de carrière."
    command: "jobs"
  '15':
    material: "BEACON"
    name: "<aqua><bold>Système de Guildes"
    lore:
      - "<gray>Gérez votre clan et vos territoires."
    command: "team"
```

- **Enregistrement Automatique de Commande :** Tout fichier de menu créé dans `menus/<nom>.yml` génère automatiquement la commande `/menu <nom>` en jeu !

---

## Création et Configuration des Quêtes (`quests/`)

En plus du réglage global des relances dans `modules/quests.yml`, GensCore permet de créer une infinité de quêtes personnalisées réparties par catégories dans le dossier `plugins/GensCore/quests/` (ex : `easy.yml`, `medium.yml`, `hard.yml` fournis par défaut, ou vos propres fichiers comme `farming.yml`, `combat.yml`, `mining.yml`).

### Comment fonctionne la distribution ?
- Chaque fichier YAML dans `plugins/GensCore/quests/` définit une **catégorie** de quêtes.
- Chaque jour à minuit (ou lors de la première connexion quotidienne du joueur), GensCore tire au sort automatiquement **jusqu'à 3 quêtes par catégorie**.
- Le joueur ouvre l'interface via `/quests` (ou `/quest`) pour consulter ses objectifs, sa barre de progression en temps réel et ses récompenses.
- Un **clic droit** sur une quête permet de la relancer (reroll) dans la limite autorisée (`quests.max_rerolls_per_day` dans `modules/quests.yml`).
- Chaque quête terminée alimente l'historique du joueur et débloque les avantages permanents du module [Solo Perks](/fr/guide/modules#12-bonus-personnels-de-quetes-soloperkmodule) (`/perks`) ainsi que l'avancement de guilde.

### Structure YAML d'un fichier de quêtes

```yaml
quests:
  mine_diamonds:
    name: "<aqua><bold>Trésor des Abîmes"
    menu_item: "DIAMOND"
    description:
      - "<gray>Mine 16 minerais de diamant brut."
      - "<yellow>Progression : <current>/<total>"
    quest_type: "BREAK"
    required:
      - "DIAMOND_ORE"
      - "DEEPSLATE_DIAMOND_ORE"
    required_amount: 16
    reward:
      commands:
        - "eco give %player% 750"
        - "tell %player% Félicitations pour votre trouvaille !"
```

### Paramètres d'une quête

| Champ | Type | Description |
|---|---|---|
| `name` | Texte | Nom formaté avec le support complet de **MiniMessage** (`<gradient>`, `<bold>`, etc.). |
| `menu_item` | Matériau | Matériau Minecraft vanilla servant d'icône d'affichage dans le menu `/quests`. |
| `description` | Liste | Lignes de texte affichées dans l'infobulle (lore). Les balises `<current>` et `<total>` sont automatiquement remplacées par la progression en direct. |
| `quest_type` | Chaîne | Type d'objectif parmi les 28 types reconnus par le moteur. |
| `required` | Liste / Texte | Matériau vanilla ou entité ciblée. Peut être une valeur unique ou une liste (ex: plusieurs variantes de minerais). |
| `required_amount` | Entier | Quantité cible requise pour valider la quête. |
| `reward.commands` | Liste | Commandes exécutées par la console lors de la complétion. Supporte `%player%` et `{player}` pour cibler le joueur. |

### Les 28 Types de Quêtes Pris en Charge (`quest_type`)

| `quest_type` | Action Déclenchante | Valeur attendue dans `required` | Exemple |
|---|---|---|---|
| `BREAK` | Casser / Miner un bloc | Type de bloc vanilla | `STONE`, `DIAMOND_ORE`, `OAK_LOG` |
| `PLACE` | Poser un bloc dans le monde | Type de bloc vanilla | `OBSIDIAN`, `TORCH`, `STONE_BRICKS` |
| `KILL` | Tuer une créature ou un joueur | Type d'entité vanilla | `ZOMBIE`, `SKELETON`, `CREEPER`, `PLAYER` |
| `CRAFT` | Fabriquer un objet dans l'établi | Type d'objet résultant | `BREAD`, `IRON_CHESTPLATE`, `GOLDEN_APPLE` |
| `FISH` | Pêcher un poisson ou un trésor | Type d'objet repêché | `COD`, `SALMON`, `PUFFERFISH`, `BOW` |
| `SHEAR` | Tondre un animal avec des cisailles | Type d'entité tondue | `SHEEP`, `MOOSHROOM` |
| `COOK` | Cuire un aliment ou fondre un minerai | Type d'objet extrait du four | `COOKED_BEEF`, `IRON_INGOT`, `GLASS` |
| `CONSUME` | Manger ou boire un consommable | Type d'aliment ou potion | `GOLDEN_CARROT`, `POTION`, `BREAD` |
| `BREED` | Faire se reproduire deux animaux | Type d'animal reproduit | `COW`, `SHEEP`, `CHICKEN`, `PIG` |
| `PICKUP` | Ramasser un objet au sol | Type d'objet ramassé | `EMERALD`, `NETHERITE_INGOT` |
| `TAME` | Apprivoiser un animal sauvage | Type d'animal apprivoisé | `WOLF`, `CAT`, `HORSE`, `PARROT` |
| `VILLAGER_TRADE` | Conclure un échange avec un PNJ | Type d'objet ou profession | `VILLAGER` |
| `GET` | Obtenir un objet dans son inventaire | Type d'objet | `ELYTRA`, `TOTEM_OF_UNDYING` |
| `MILKING` | Traire une vache avec un seau | Type d'entité | `COW`, `MOOSHROOM` |
| `EXP_POINTS` | Récolter des orbes d'expérience | Quantité de points | `EXP_POINTS` |
| `EXP_LEVELS` | Atteindre un niveau d'XP requis | Nombre de niveaux | `EXP_LEVELS` |
| `ENCHANT` | Enchanter un objet sur la table | Matériau enchanté | `DIAMOND_SWORD`, `BOW`, `BOOK` |
| `CARVE` | Sculpter une citrouille avec cisailles | Type de bloc | `PUMPKIN` |
| `PLAYER_DEATH` | Mourir au combat ou d'accident | Cause ou n'importe | `PLAYER_DEATH` |
| `LOCATION` | Atteindre une région ou des coordonnées | Nom de zone | Coordonnées / Région |
| `FARMING` | Récolter une culture mature | Type de graine ou légume | `WHEAT`, `CARROTS`, `POTATOES` |
| `LAUNCH` | Tirer une flèche ou lancer un projectile | Type de projectile | `ARROW`, `ENDER_PEARL`, `TRIDENT` |
| *Intégrations* | Plugins externes optionnels | Dépendance | `MYTHIC_MOBS`, `ELITE_MOBS`, `NU_VOTIFIER`, `PYRO_FISH`, `EMF_FISH`, `PLACEHOLDER` |

---

## Fichiers de Langues & Traduction (`lang/`)

Tous les messages, notifications d'erreurs et affichages textuels du plugin sont regroupés dans `plugins/GensCore/lang/` (`fr_FR.yml` et `en_US.yml`) :
- **Support MiniMessage Intégral :** Couleurs hexadécimales, dégradés (`<gradient:#ff0000:#00ff00>texte</gradient>`), tooltips au survol (`<hover:show_text:'Infobulle'>Survolez-moi</hover>`) et actions au clic (`<click:run_command:'/spawn'>Spawn</click>`).
- **Variables Dynamiques :** Les messages injectent automatiquement les variables contextuelles comme `<player>`, `<amount>`, `<target>`, `<balance>`.

---

## Ports Réseau & Redirection (Port Forwarding)

| Port | Protocole | Utilité | Requis |
|---|---|---|---|
| **25565** | TCP/UDP | Serveur de Jeu Minecraft | Oui |
| **8080** | TCP | Panel Web & API REST | Oui (si web activé) |
| **8100** | TCP | Carte BlueMap (Optionnel) | Optionnel |

### Exemple de Reverse Proxy Nginx (SSL HTTPS)
```nginx
server {
    server_name panel.monserveur.fr;

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

