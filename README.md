# GensCore

![GensCore Banner](https://img.shields.io/badge/GensCore-Paper_Plugin-green.svg) ![Java Version](https://img.shields.io/badge/Java-21+-blue.svg) ![Minecraft Version](https://img.shields.io/badge/Minecraft-26.1+-red.svg)

**GensCore** est un plugin de base ultra-complet developpe specifiquement pour le serveur Survie/Faction *GensBien*. Il regroupe toutes les mecaniques essentielles du serveur en un seul plugin optimise, offrant ainsi d'excellentes performances tout en evitant d'avoir a gerer des dizaines de petits plugins separes.

## Fonctionnalites Incluses

Ce plugin est modulable et gere les aspects suivants du serveur :
* **Economie & Metiers (Jobs) :** Gestion de l'argent (`/money`, `/pay`) et des metiers.
* **Guildes (Teams) :** Creation, invitation et gestion des guildes (`/team`).
* **Quetes & Statistiques :** Quetes journalieres et statistiques globales des joueurs.
* **Web Panel & BlueMap :** Un panel d'administration Web reactif developpe en React (port 8080) avec integration de BlueMap.
* **Discord Bot :** Synchronisation complete avec Discord (JDA). Recompenses, badges de liaison en jeu et logs.
* **Moderation :** Commandes de base (`/mute`, `/ban`, `/freeze`, `/openinv`).
* **Utilitaires de Survie :** Commandes essentielles comme `/spawn`, `/sethome`, `/home`, `/back`, `/tpa`, `/ec`, et `/feed`.
* **Loot & Spawners :** Systeme de loot personnalise et gestion des spawners.
* **Custom GUIs :** Interfaces et menus interactifs en jeu (`/menu`).

---

## Compilation & Installation (Pour les Developpeurs / Joueurs Curieux)

Si vous souhaitez contribuer, tester le plugin en local, ou simplement voir comment il est fait, voici un tutoriel complet pour le compiler vous-meme !

### Prerequis

Avant de commencer, assurez-vous d'avoir les elements suivants installes sur votre ordinateur :
1. **[Java 21 ou superieur](https://adoptium.net/fr/)** (JDK)
2. **[Maven](https://maven.apache.org/download.cgi)** (Pour gerer les dependances et compiler le plugin)
3. **[Git](https://git-scm.com/)** (Pour telecharger le code source)

### Etapes de Compilation

**1. Cloner le depot GitHub**
Ouvrez un terminal (ou l'invite de commande) et tapez :
```bash
git clone https://github.com/WilliamBossard/GensCore.git
cd GensCore
```

**2. (Optionnel) Construire le Panel Web React**
Si vous souhaitez recompiler le panel d'administration web integre au plugin, vous devez avoir [Node.js](https://nodejs.org/) installe.
```bash
cd web-panel
npm install
npm run build
cd ..
```
*Note : Si vous ne voulez pas modifier le panel web, vous pouvez sauter cette etape, le plugin utilisera la version deja construite.*

**3. Compiler le plugin avec Maven**
A la racine du dossier `GensCore`, lancez la commande suivante :
```bash
mvn clean package
```
*Maven va telecharger toutes les dependances (l'API de Bukkit/Paper, JDA, etc.) et compiler le projet.*

**4. Recuperer le fichier `.jar`**
Si la compilation a reussi, vous verrez un message `BUILD SUCCESS`.
Allez dans le dossier `target/`. Vous y trouverez le fichier `GensCore-1.0-SNAPSHOT-shaded.jar`. 
C'est votre plugin pret a etre utilise !

### Lancer sur votre serveur local
1. Prenez le fichier `GensCore-1.0-SNAPSHOT-shaded.jar` (que vous pouvez renommer en `GensCore.jar`).
2. Glissez-le dans le dossier `plugins/` de votre serveur de test local Paper/Spigot.
3. Demarrez votre serveur. Le dossier de configuration `GensCore` se creera automatiquement avec tous les fichiers necessaires !

---
*Plugin developpe avec passion pour GensBien.*
