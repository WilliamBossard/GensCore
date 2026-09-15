# Démarrer avec GensCore

<div style="display: flex; gap: 8px; margin: 1rem 0; flex-wrap: wrap;">
  <img src="https://img.shields.io/badge/GensCore-Paper%20%26%20Folia-green.svg" alt="Paper et Folia" />
  <img src="https://img.shields.io/badge/Java-25+-blue.svg" alt="Java 25" />
  <img src="https://img.shields.io/badge/Minecraft-26.2+-red.svg" alt="Minecraft 26.2" />
</div>

**GensCore** est un plugin core tout-en-un pour serveurs Survie et Faction, développé spécifiquement pour **PaperMC & Folia (Minecraft 26.2+)** sous **Java 25 (LTS)**.

---

## Points Clés

- **Serveurs Supportés :** **PaperMC**, **Folia** et **Purpur** (Minecraft 26.2+).
- **Prérequis Runtime :** **Java 25+** (exploite les fonctionnalités JVM modernes, le pattern matching et ASM 9.10.1).
- **Zéro Dépendance Obligatoire :** GensCore intègre son propre moteur d'économie, sa base SQLite locale, son système de verrouillage de coffres et son fallback de permissions.
- **Cross-Play Prêt à l'Emploi :** Support natif des joueurs Bedrock dès que Geyser et Floodgate sont installés.

---

## Étapes d'Installation

1. **Vérifiez votre version de Java :**
   Assurez-vous que votre serveur Minecraft s'exécute bien sous **Java 25 ou supérieur** :
   ```bash
   java -version
   ```
2. **Téléchargez le dernier `.jar` :**
   Récupérez `GensCore-vX.Y.Z.jar` depuis les [Releases GitHub](https://github.com/WilliamBossard/GensCore/releases) ou sur [Modrinth](https://modrinth.com/).
3. **Placez-le dans le dossier `plugins/` :**
   Déposez simplement le fichier `.jar` dans le répertoire `plugins/` de votre serveur.
4. **Démarrez le serveur :**
   Démarrez ou redémarrez votre serveur. GensCore va automatiquement :
   - Créer le dossier de configuration `plugins/GensCore/`.
   - Initialiser la base SQLite locale (`genscore.db`) en mode haute performance WAL.
   - Extraire les fichiers du panel web dans `plugins/GensCore/web/`.
   - Lancer le micro-serveur web Javalin sur le port `8080`.

---

## Écosystème & Intégrations

GensCore fonctionne de façon 100% autonome, mais étend automatiquement ses capacités si les plugins suivants sont détectés :

| Plugin | Type d'intégration | Description |
|---|---|---|
| **Vault** | Dépendance douce | Enregistre automatiquement `GensVaultEconomy`. Permet aux plugins tiers (ChestShop, Towny, Lands) d'accéder aux soldes de GensCore. |
| **LuckPerms** | Dépendance douce | Lit automatiquement les grades, préfixes, suffixes et poids pour le Chat et la Tablist/Scoreboard. |
| **Floodgate / Geyser** | Optionnel | Active les fenêtres UI natives Bedrock (API Cumulus), gère les UUIDs Bedrock et résout les têtes de skin personnalisées. |
| **BlueMap** | Optionnel | Intègre automatiquement la carte interactive 2D/3D directement dans le Panel Web Administrateur. |
| **PlaceholderAPI** | Optionnel | Expose toutes les variables GensCore (`%genscore_money%`, etc.) aux autres plugins. |

---

::: tip Lanceur Client Recommandé : Gens Launcher
Pour offrir la meilleure expérience à vos joueurs (ressource packs, mods, synchronisation transparente), recommandez **[Gens Launcher](https://williambossard.github.io/Gens-Launcher/)**. Il intègre la configuration Java automatique, la synchronisation cloud (Horizon Delta Sync) et le support complet de Fabric, NeoForge et Forge.
:::

## Prochaines Étapes

- Consultez la [Liste complète des Commandes](/fr/guide/commands).
- Configurez les grades joueurs et staff avec le guide [Permissions & LuckPerms](/fr/guide/permissions).
- Découvrez les [28 Modules en jeu](/fr/guide/modules).
- Connectez-vous au [Panel Web & Mini-jeux](/fr/guide/web-panel).
