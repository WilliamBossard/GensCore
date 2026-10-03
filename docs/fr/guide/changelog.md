# Suivi des Versions & Changelog

Statut actuel et historique complet des mises à jour de **GensCore** sur **Minecraft 26.3** et **Java 25 LTS**.

::: info État du Projet
- **Moteur cible :** Minecraft 26.1 - 26.3 (Paper Build 145-beta · Folia 26.1+)
- **Environnement d'exécution :** Java 25 LTS (Classfile 69)
- **Version actuelle :** **Release 1.0.3** — Stable
- **Assurance Qualité :** **81/81 tests automatisés validés** (0 erreurs, 0 échecs)
- **Dépôt GitHub :** [`dev`](https://github.com/WilliamBossard/GensCore/tree/dev) · [`main`](https://github.com/WilliamBossard/GensCore/tree/main)
:::

## Release 1.0.3 — Paper build.145-beta (3 Octobre 2026)

> **Mise à jour majeure de stabilité, base de données et modernisation Paper 26.3.** Les 81 tests automatisés (unitaires et de concurrence) passent avec succès avec 0 erreur. Validé sur Paper 26.1, Paper 26.2 et Paper/Folia 26.3.

- **Modernisation Paper 26.3 & Élimination Totale des Dépréciations :**
  - `UtilsModule.java` : Migration des menus virtuels (`openWorkbench`, `openAnvil`, `openEnchanting`) vers les builders officiels Paper 26.3 `MenuType.CRAFTING.builder()`, `MenuType.ANVIL.builder()`, et `MenuType.ENCHANTMENT.builder()`.
  - `ShopModule.java` : Détection moderne d'objets via les Data Components (`meta.hasCustomModelDataComponent()`, `meta.hasCustomName()`, `meta.hasItemName()`).
  - `TombListener.java` : Profil de skin des têtes de joueurs migré vers Paper 26.3 `skull.setProfile(ResolvableProfile.resolvableProfile(profile))`.
  - `JobsModule.java` : Remplacement de `Metadatable` (`FixedMetadataValue`) et `Block.getBlockKey()` par l'encodage bitwise haute performance `packBlockKey(x, y, z)` avec cache thread-safe (`Map<UUID, Set<Long>>`).
  - Élimination de tous les `@SuppressWarnings("deprecation")` du code actif Bukkit/Paper.
- **Sécurité Mémoire & Prévention des Fuites d'Objets :**
  - `TeleportBackModule.java` : Remplacement du stockage direct d'objets `Location` par le record immuable `record BackPosition(String worldName, double x, double y, double z, float yaw, float pitch)` afin d'éviter la rétention d'instances `World` lors du déchargement de mondes.
  - `StorageManager.java` : Suppression complète du code mort lié à l'ancien format `data.yml`.
  - `ItemSerializer.java` : Isolation et documentation du repli de secours de désérialisation pour les anciens objets Base64 (préfixe `rO0AB`).
- **Support Base de Données Multi-Moteurs (MySQL / MariaDB & SQLite) :**
  - Prise en charge optionnelle de bases externes MySQL et MariaDB via HikariCP et le pilote officiel MariaDB (`org.mariadb.jdbc:mariadb-java-client:3.5.2`, entièrement ombré).
  - Maintien du moteur local SQLite (`genscore.db`) comme stockage par défaut prêt à l'emploi (zéro configuration requise).
  - Traduction dynamique des dialectes SQL (`DatabaseManager.adaptQuery()`) :
    - Définitions d'auto-incrément (`INTEGER PRIMARY KEY AUTOINCREMENT` -> `INT AUTO_INCREMENT PRIMARY KEY`).
    - Valeurs par défaut d'horodatage (`TIMESTAMP DEFAULT CURRENT_TIMESTAMP` -> `DATETIME DEFAULT CURRENT_TIMESTAMP`).
    - Clauses d'upsert (`ON CONFLICT(...) DO UPDATE SET col = excluded.col` -> `ON DUPLICATE KEY UPDATE col = VALUES(col)`).
  - Introspection multi-schémas et migrations automatiques sur `sqlite_master` et `information_schema.tables`.
  - Bascule de secours automatique (failover) : si le serveur MySQL/MariaDB distant est inaccessible, le plugin bascule instantanément sur SQLite local sans interrompre le lancement du serveur.
- **Service de Métriques & Diagnostic Système (`MetricsService.java`) :**
  - Télémétrie en direct : mémoire JVM (utilisée, libre, max), charge processeur, TPS Folia, statistiques du pool de connexions SQL et état des modules.
  - Nouveaux points de terminaison REST : `/api/metrics` et `/api/admin/metrics` exposés via Javalin.
- **Commandes d'Administration & Diagnostic (`GensCommand.java`) :**
  - `/gens status` (permission : `genscore.admin`) : Affiche la plateforme, le TPS Folia, la mémoire Java et le nombre de modules actifs.
  - `/gens db` (permission : `genscore.admin`) : Affiche le moteur actif (SQLite ou MySQL/MariaDB), l'URL de connexion, les connexions actives/inactives et les threads en attente.
- **Assurance Qualité & CI/CD :**
  - Ajout des suites de tests `SqlDialectTest` et `DatabaseConfigTest`.
  - Suite de tests étendue à **81 tests validés avec 0 échec et 0 erreur**.
  - Workflows GitHub Actions mis à niveau avec vérification automatique du packaging ombré.

---

## Release 1.0.2 — Paper build.143-beta (3 Octobre 2026)

> **Mise à jour de maintenance stable.** Les 75 tests automatisés passent avec succès. Testé et certifié sur Paper 26.1, Paper 26.2 et Paper/Folia 26.3.

- **TabBoard & Nametags :**
  - Omission du préfixe pour les joueurs dans le groupe `default` de LuckPerms sans préfixe personnalisé (affichage épuré sans texte `Default`).
  - Synchronisation du rafraîchissement du cache de préfixe toutes les 5 secondes.
  - Protection du préfixe de plateforme : le badge `[Java]` ne s'affiche plus si Floodgate n'est pas présent.
- **Scoreboard & Placeholders :**
  - Résolution native pour `%vault_eco_balance%`, `%vault_eco_balance_fixed%` et `%vault_eco_balance_formatted%` sans nécessiter d'extension externe.
- **Chat & Formatage :**
  - Alignement du formatage des préfixes avec LuckPerms (suppression du fallback `[Joueur]`).
  - Correction des espaces résiduels entre préfixe et pseudonyme.
- **Authentification & Traduction :**
  - Nettoyage des doublons de traduction dans `fr_FR.yml` et `en_US.yml`.
  - Remplacement des entités HTML échappées par des balises MiniMessage valides.

---

## Release 1.0.1 — Paper build.41-alpha (25 Septembre 2026)

- **API Paper :** Mise à niveau vers `26.3.build.41-alpha`.
- **Panel Web Mobile :** Barre supérieure fixée avec zone de défilement dédiée `admin-content-scroll`.
- **Bibliothèques Backend :** `jackson-databind` 2.22.3, `JDA` 6.7.0.
- **Frontend :** Vite 8.3.1, Lucide-React 1.48.0, React-i18next 17.0.15.

---

## Release 1.0.0 — Version Initiale (20 Septembre 2026)

- Lancement officiel de GensCore.
- Architecture modulaire avec modules autonomes.
- Dashboard web réactif React & Javalin.
- Persistance SQLite avec pool de connexions HikariCP.
- Intégration Discord Bot via JDA 6.
- Cross-play Java & Bedrock (Geyser/Floodgate).
