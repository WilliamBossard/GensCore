# Référence des Commandes

Toutes les commandes de GensCore sont enregistrées de façon asynchrone via le **Framework Cloud Command**, avec auto-complétion native Brigadier et compatibilité multi-thread Folia.

---

## Authentification & Comptes

| Commande | Arguments | Permission | Défaut | Description |
|---|---|---|---|---|
| `/register` | `<motDePasse> <confirmation>` | *Aucune* | Tout le monde | Crée un compte et hache le mot de passe avec BCrypt. |
| `/login` | `<motDePasse>` | *Aucune* | Tout le monde | Connexion au compte. Les interactions sont figées tant qu'on n'est pas identifié. |
| `/changemdp` | `<ancienMDP> <nouveauMDP>` | *Aucune* | Tout le monde | Modifie le mot de passe. *(Alias : `/changepassword`)* |
| `/resetmdp` | `<joueur>` | `genscore.admin` | OP | Réinitialise le mot de passe d'un joueur, le forçant à se réinscrire. |

---

## Économie & Boutique Dynamique

| Commande | Arguments | Permission | Défaut | Description |
|---|---|---|---|---|
| `/money` | *Aucun* | *Aucune* | Tout le monde | Affiche votre solde actuel. *(Alias : `/balance`)* |
| `/money` | `<joueur>` | `genscore.admin` | OP | Consulte le solde d'un autre joueur. |
| `/baltop` | *Aucun* | *Aucune* | Tout le monde | Affiche le classement des joueurs les plus fortunés. |
| `/pay` | `<joueur> <montant>` | *Aucune* | Tout le monde | Transfère de l'argent à un autre joueur. |
| `/eco set` | `<joueur> <montant>` | `genscore.admin` | OP | Définit le solde d'un joueur. |
| `/eco give` | `<joueur> <montant>` | `genscore.admin` | OP | Ajoute de l'argent au compte d'un joueur. |
| `/eco take` | `<joueur> <montant>` | `genscore.admin` | OP | Retire de l'argent du compte d'un joueur. |
| `/eco reset` | `<joueur>` | `genscore.admin` | OP | Réinitialise le solde d'un joueur au montant par défaut. |
| `/shop` | *Aucun* | *Aucune* | Tout le monde | Ouvre l'interface GUI de la boutique serveur dynamique. |

---

## Hôtel des Ventes (Auction House)

| Commande | Arguments | Permission | Défaut | Description |
|---|---|---|---|---|
| `/ah` | *Aucun* | *Aucune* | Tout le monde | Ouvre l'interface de l'Hôtel des Ventes. *(Aliases : `/auctionhouse`, `/hdv`)* |
| `/ah sell` | `<prix>` | *Aucune* | Tout le monde | Met en vente l'objet tenu dans votre main. |

---

## Guildes & Équipes (Teams)
Système complet de clans avec trésorerie partagée ($ ou XP), revendication territoriale anti-grief par chunks ($16 \times 16$), notifications à l'écran, synchronisation BlueMap en temps réel, hiérarchie de rôles (Chef, Admin, Membre) et améliorations permanentes.

| Commande | Arguments | Permission | Défaut | Description |
|---|---|---|---|---|
| `/team` | *Aucun* | *Aucune* | Tout le monde | Ouvre le menu interactif de gestion de guilde. *(Aliases : `/guild`, `/guilde`, `/teams`)* |
| `/team create` | `<nom>` | *Aucune* | Tout le monde | Fonde une nouvelle guilde (16 caractères max). |
| `/team invite` | `<joueur>` | *Aucune* | Admin / Chef | Invite un joueur à rejoindre votre guilde. |
| `/team accept` | *Aucun* | *Aucune* | Tout le monde | Accepte une invitation en attente. |
| `/team kick` | `<joueur>` | *Aucune* | Admin / Chef | Expulse un membre de la guilde (les Admins ne peuvent pas expulser le Chef ni d'autres Admins). |
| `/team promote` | `<joueur>` | *Aucune* | Chef | Promeut un membre au rang d'Administrateur (`ADMIN`). |
| `/team demote` | `<joueur>` | *Aucune* | Chef | Rétrograde un Administrateur au rang de Membre classique. |
| `/team leave` | *Aucun* | *Aucune* | Membre / Admin | Quitte la guilde actuelle (le Chef doit dissoudre la guilde). |
| `/team disband` | *Aucun* | *Aucune* | Chef | Dissout définitivement la guilde et libère tous ses territoires. |
| `/team quest` | *Aucun* | *Aucune* | Tout le monde | Ouvre le menu des quêtes communautaires de guilde. |
| `/team upgrades` | *Aucun* | *Aucune* | Tout le monde | Ouvre la boutique d'améliorations de guilde. |
| `/team deposit` | `<montant>` | *Aucune* | Tout le monde | Dépose de l'argent dans la banque de guilde (si Économie active). |
| `/team depositxp` | `<niveaux>` | *Aucune* | Tout le monde | Dépose des niveaux d'XP dans la banque (si Économie désactivée). |
| `/team withdraw` | `<montant>` | *Aucune* | Admin / Chef | Retire de l'argent de la banque de guilde. |
| `/team withdrawxp` | `<niveaux>` | *Aucune* | Admin / Chef | Retire des niveaux d'XP de la banque de guilde. |
| `/team claim` | *Aucun* | *Aucune* | Admin / Chef | Revendique le chunk actuel ($16 \times 16$). Financé strictement par la banque de guilde. |
| `/team unclaim` | *Aucun* | *Aucune* | Admin / Chef | Libère le chunk revendiqué et le rend au monde sauvage. |
| `/team color` | `<hex>` | *Aucune* | Admin / Chef | Définit la couleur du territoire de guilde sur BlueMap (ex: `#3498db`). |

---

## Métiers (Jobs)

| Commande | Arguments | Permission | Défaut | Description |
|---|---|---|---|---|
| `/jobs` | *Aucun* | *Aucune* | Tout le monde | Ouvre le menu des métiers. *(Aliases : `/job`, `/metier`, `/metiers`)* |

Métiers disponibles : **Mineur**, **Bûcheron**, **Chasseur**, **Fermier**, **Pêcheur** et **Bâtisseur**.

---

## Quêtes Journalières

| Commande | Arguments | Permission | Défaut | Description |
|---|---|---|---|---|
| `/quests` | *Aucun* | *Aucune* | Tout le monde | Ouvre le menu des quêtes du jour. *(Aliases : `/quest`, `/quete`, `/quetes`)* |

- Relance (reroll) via clic droit nécessite la permission `genscore.quests.reroll`.
- Relance globale forcée (admin) nécessite `genscore.quests.admin`.

---

## Téléportation & Points de Repère

| Commande | Arguments | Permission | Défaut | Description |
|---|---|---|---|---|
| `/spawn` | *Aucun* | `genscore.spawn` | `true` | Téléporte au point d'apparition du monde. |
| `/setspawn` | *Aucun* | `genscore.admin` | OP | Définit les coordonnées du spawn global. |
| `/sethome` | `[nom]` | `genscore.home` | `true` | Crée un point de téléportation personnel (home). |
| `/home` | `[nom]` | `genscore.home` | `true` | Téléporte à l'un de vos homes. |
| `/delhome` | `[nom]` | `genscore.home` | `true` | Supprime un home existant. |
| `/back` | *Aucun* | `genscore.back` | `false` | Téléporte à la dernière position de mort ou de téléportation. |
| `/tpa` | `<joueur>` | `genscore.tpa` | `true` | Envoie une demande de téléportation à un joueur. |
| `/tpaccept` | *Aucun* | `genscore.tpa` | `true` | Accepte une demande de téléportation reçue. |
| `/tpadeny` | *Aucun* | `genscore.tpa` | `true` | Refuse une demande de téléportation. |
| `/tpacancel` | *Aucun* | `genscore.tpa` | `true` | Annule votre propre demande de téléportation envoyée. |

---

## Verrouillage & Protection des Conteneurs

| Commande | Arguments | Permission | Défaut | Description |
|---|---|---|---|---|
| `/lock` | `[private]` | *Aucune* | Tout le monde | Clic droit sur un coffre/tonneau/shulker pour le verrouiller. *(Alias : `/cprivate`)* |
| `/lock unlock` | *Aucun* | *Aucune* | Tout le monde | Clic droit sur votre conteneur pour lever le verrouillage. |
| `/lock guild` | *Aucun* | *Aucune* | Tout le monde | Clic droit pour partager l'accès au conteneur avec votre guilde. |

---

## Spawners Personnalisés

| Commande | Arguments | Permission | Défaut | Description |
|---|---|---|---|---|
| `/spawner give` | `<joueur> <type> [nombre]` | `genscore.admin.spawner` | OP | Donne un bloc de spawner empilable à un joueur. |

---

## Utilitaires Survie

| Commande | Arguments | Permission | Défaut | Description |
|---|---|---|---|---|
| `/ec` | *Aucun* | `genscore.ec` | OP | Ouvre votre coffre de l'Ender virtuel. *(Alias : `/enderchest`)* |
| `/craft` | *Aucun* | `genscore.craft` | OP | Ouvre un établi virtuel 3x3. *(Aliases : `/craftingtable`, `/workbench`)* |
| `/anvil` | *Aucun* | `genscore.anvil` | OP | Ouvre une enclume virtuelle. |
| `/enchant` | *Aucun* | `genscore.enchant` | OP | Ouvre une table d'enchantement virtuelle. *(Alias : `/enchanttable`)* |
| `/feed` | *Aucun* | `genscore.feed` | OP | Restaure entièrement la barre de nourriture et de saturation. |

---

## Modération & Administration

| Commande | Arguments | Permission | Défaut | Description |
|---|---|---|---|---|
| `/ban` | `<joueur> [durée] [motif]` | `genscore.ban` | OP | Bannit un joueur. |
| `/unban` | `<joueur>` | `genscore.ban` | OP | Débannit un joueur. |
| `/mute` | `<joueur> [durée] [motif]` | `genscore.mute` | OP | Rend muet un joueur dans le chat public. |
| `/unmute` | `<joueur>` | `genscore.mute` | OP | Rend la parole à un joueur muet. |
| `/kick` | `<joueur> [motif]` | `genscore.kick` | OP | Expulse un joueur du serveur. |
| `/freeze` | `<joueur>` | `genscore.freeze` | OP | Gèle un joueur sur place pour vérification screenshare. |
| `/openinv` | `<joueur>` | `genscore.openinv` | OP | Inspecte l'inventaire et l'armure d'un joueur en temps réel. *(Alias : `/invsee`)* |

---

## Passerelle Casino Web

| Commande | Arguments | Permission | Défaut | Description |
|---|---|---|---|---|
| `/web` | *Aucun* | *Aucune* | Tout le monde | Message d'aide pour l'inventaire du casino web. |
| `/web deposit` | *Aucun* | *Aucune* | Tout le monde | Dépose l'item en main dans l'inventaire de jeu du casino web. |
| `/web withdraw` | *Aucun* | *Aucune* | Tout le monde | Ouvre un menu pour récupérer les items gagnés sur la machine à sous web. |

---

## Gestion du Noyau

| Commande | Arguments | Permission | Défaut | Description |
|---|---|---|---|---|
| `/module` | `<nomModule> <on\|off>` | `genscore.admin` | OP | Active ou désactive à chaud l'un des 28 modules. |
| `/menu` | `[nom]` | *Aucune* | Tout le monde | Ouvre un menu inventaire YAML configuré dans `plugins/GensCore/menus/`. |
