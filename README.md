# GensCore

![GensCore Banner](https://img.shields.io/badge/GensCore-Paper_Plugin-green.svg) ![Java Version](https://img.shields.io/badge/Java-25+-blue.svg) ![Minecraft Version](https://img.shields.io/badge/Minecraft-26.1+-red.svg)

**GensCore** est un plugin de base ultra-complet developpe specifiquement pour le serveur Survie/Faction *GensBien*. Il regroupe toutes les mecaniques essentielles du serveur en un seul plugin optimise, offrant ainsi d'excellentes performances tout en evitant d'avoir a gerer des dizaines de petits plugins separes.

## Fonctionnalites Incluses

Ce plugin est modulable et gere les aspects suivants du serveur :
* **Economie & Metiers (Jobs) :** Gestion de l'argent (`/money`, `/pay`) et des metiers.
* **Guildes (Teams) :** Creation, invitation et gestion des guildes (`/team`).
* **Quetes & Statistiques :** Quetes journalieres et statistiques globales des joueurs.
* **Web Panel & BlueMap :** Un panel d'administration Web reactif avec integration de BlueMap, tournant via Javalin.
* **Discord Bot :** Synchronisation complete avec Discord via l'API JDA.
* **Moderation :** Commandes de base (`/mute`, `/ban`, `/freeze`, `/openinv`).
* **Utilitaires de Survie :** Commandes essentielles comme `/spawn`, `/sethome`, `/back`, `/tpa`, `/ec`.
* **Loot & Spawners :** Systeme de loot personnalise et gestion des spawners.

---

## Configuration & Reseaux (Ports)

Afin d'eviter d'avoir des elements confidentiels ou des adresses codees en dur, GensCore repose entierement sur le fichier de configuration `plugins/GensCore/config.yml`. Celui-ci se genere automatiquement au premier lancement et inclut les parametres cruciaux.

### Web Panel
Le panel d'administration Web s'execute localement sur votre serveur Minecraft, en parallele du jeu, en utilisant **Javalin**. 
- Le port par defaut est **8080**.
- Il peut etre modifie via l'option `web.port: 8080` dans le `config.yml`.
- Si votre serveur Minecraft tourne sur un hebergeur (Pterodactyl), vous devrez ouvrir un second port de libre et indiquer ce port dans la configuration pour que le Panel Web soit accessible.

### Discord Bot
Le module Discord necessite un Token de bot. Pour des raisons de securite, ce Token ne doit **jamais** etre partage ou hardcode dans le code source Java.
- Indiquez-le via l'option `discord.bot_token: "VOTRE_TOKEN"` dans le `config.yml`.

### BlueMap
Le Panel Web de GensCore integre une frame iFrame de BlueMap pour afficher la carte directement aux moderateurs.
- Il recupere l'URL locale via `bluemap.url: "http://localhost:8100"` modifiable dans le `config.yml`.

---

## Architecture Interne du Code

Pour les developpeurs qui souhaitent modifier ou comprendre le plugin, voici les pilliers de l'architecture interne :

1. **Le `ModuleManager`** : C'est le systeme central. Au lieu d'avoir un plugin monolithique, GensCore est divise en de dizaines de "Modules" (comme `EconomyModule`, `TeamsModule`, `DiscordModule`). Ils heritent tous de l'interface `Module` et peuvent etre actives ou desactives dynamiquement.
2. **La base de donnees locale (`DatabaseManager.java`)** : Pour rester autonome, GensCore n'utilise pas MySQL mais une base de donnees **SQLite locale** (`genscore.db`), qui sauvegarde tout : les soldes des joueurs, les inventaires des guildes, la progression des quetes, les historiques de connexion.
3. **L'API Web REST (`WebManager.java` & `WebPlayerAPI.java`)** : Javalin est utilise en arriere-plan pour lever un micro-serveur HTTP sur le port 8080. Le code React du dossier `/web-panel` est compile et mis dans `/src/main/resources/public/`. A chaque fois qu'un admin va sur la page web, Javalin sert ces fichiers HTML/JS, et communique par API JSON avec React.

---

## Compilation & Installation

Si vous souhaitez compiler GensCore, l'environnement ideal est **Java 21**, gere par **Maven**.

### Compilation Manuelle (Locale)
```bash
git clone https://github.com/WilliamBossard/GensCore.git
cd GensCore
mvn clean package
```
Le fichier final se trouvera dans `target/GensCore-1.0-SNAPSHOT-shaded.jar`. (Le plugin utilise le Maven Shade Plugin pour empaqueter les librairies externes comme Javalin et JDA directement dans l'archive).

### Compilation via GitHub Actions
GensCore possede un Workflow configure (dans `.github/workflows/release.yml`). 
1. Allez sur votre Depot GitHub, dans l'onglet **"Actions"**.
2. Selectionnez **"Build and Release GensCore"** a gauche.
3. Cliquez sur **"Run workflow"**.
4. GitHub va compiler le projet sur ses serveurs (gratuitement) en quelques secondes, puis creer un **Brouillon de Release (Draft Release)**. 
5. Il ne vous reste plus qu'a telecharger le fichier `.jar` depuis la Release GitHub sans avoir besoin d'installer Java ou Maven sur votre ordinateur !

---
*Plugin developpe avec passion pour GensBien.*
