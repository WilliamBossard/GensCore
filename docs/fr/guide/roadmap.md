# Roadmap & Changelog

Suivi en temps réel et historique complet des patches de **GensCore** pour **Minecraft 26.3** et **Java 25 LTS**.

::: info Statut du Projet
- **Version cible :** Minecraft 26.3 (Paper Build 41-alpha)
- **Environnement d'exécution :** Java 25 LTS
- **Version actuelle :** **Release 1.0.1** — Stable
- **Dépôt :** [`dev`](https://github.com/WilliamBossard/GensCore/tree/dev) · [`main`](https://github.com/WilliamBossard/GensCore/tree/main)
:::

---

## Tableau d'avancement

| Composant | Statut |
| :--- | :---: |
| **Support Java 25 LTS** | <span style="color: #22c55e; font-weight: 700;">✓ Terminé</span> |
| **API Paper 26.3 (build.41-alpha)** | <span style="color: #22c55e; font-weight: 700;">✓ Terminé</span> |
| **Mises à jour des dépendances** | <span style="color: #22c55e; font-weight: 700;">✓ Terminé</span> |
| **Optimisation Mobile Web** | <span style="color: #22c55e; font-weight: 700;">✓ Terminé</span> |
| **Architecture Geyser Standalone** | <span style="color: #22c55e; font-weight: 700;">✓ Terminé</span> |
| **Quêtes de Craft (Shift-Click & Torches)** | <span style="color: #22c55e; font-weight: 700;">✓ Terminé</span> |
| **Boutique Survie (319 Objets & Alchimie)** | <span style="color: #22c55e; font-weight: 700;">✓ Terminé</span> |
| **Remaster Mini-Jeux Web & CoinFlip** | <span style="color: #22c55e; font-weight: 700;">✓ Terminé</span> |
| **Internationalisation (FR & EN)** | <span style="color: #22c55e; font-weight: 700;">✓ Terminé</span> |
| **Moteur Brigadier Paper Natif** | <span style="color: #22c55e; font-weight: 700;">✓ Terminé</span> |
| **Cloud Reflection Patch** | <span style="color: #22c55e; font-weight: 700;">✓ Terminé</span> |
| **Anti Double-Clic Shop** | <span style="color: #22c55e; font-weight: 700;">✓ Terminé</span> |
| **Synchronisation des Bannissements** | <span style="color: #22c55e; font-weight: 700;">✓ Terminé</span> |
| **Crossplay Bedrock (Geyser/Floodgate)** | <span style="color: #22c55e; font-weight: 700;">✓ Blindé</span> |
| **Validation Folia Régionale** | <span style="color: #22c55e; font-weight: 700;">✓ Terminé</span> |
| **Guildes & Claims 2.0** | <span style="color: #22c55e; font-weight: 700;">✓ Terminé</span> |
| **Améliorations de Guilde Phase 2** | <span style="color: #22c55e; font-weight: 700;">✓ Terminé</span> |
| **Module Bonus Quêtes Solo** | <span style="color: #22c55e; font-weight: 700;">✓ Terminé</span> |
| **Migration SQLite Lootr** | <span style="color: #22c55e; font-weight: 700;">✓ Terminé</span> |
| **Suite de Tests (69/69)** | <span style="color: #22c55e; font-weight: 700;">✓ Terminé</span> |

---

## Changelog

> Les patches sont listés dans l'**ordre chronologique décroissant** — le plus récent en premier.

---

### Release 1.0.1 — Paper build.41-alpha (25 septembre 2026)

> **Release stable.** 69 tests automatisés validés à 100%. Prêt pour la production.

- **API Paper :** Mise à jour vers `26.3.build.41-alpha`.
- **Panel Admin Mobile :** Correction de la barre du haut en mode mobile — désormais `position: fixed` avec une zone scrollable dédiée `admin-content-scroll`, éliminant le trait noir / chevauchement sur tous les onglets.
- **Bibliothèques backend :** `jackson-databind` → `2.22.3`, `JDA` → `6.7.0`.
- **Frontend :** `vite` 8.3.1, `lucide-react` 1.48.0, `react-i18next` 17.0.15, `typescript-eslint` 8.70.1.
- **CI/CD :** Workflows GitHub Actions mis à jour vers Node.js 24 et Paper `build.41-alpha`.

---

### Patch 26.3-alpha.20 (19 septembre 2026)

- **Module Bonus Quêtes Solo (`SoloPerkModule`) :** 29ème module autonome GensCore (`solo_perks`), activable en jeu (`/module solo_perks <on|off>`) ou via le tableau de bord admin.
- **Progression à deux niveaux :**
  - *Bonus Gratuits (6) :* Débloqués automatiquement par paliers de quêtes complétées à vie (5, 15, 30, 50, 75, 100) : Reroll Gratuit, Foyer Supplémentaire, Foulée Céleste, Sagesse des Métiers, Téléportation Instantanée, Festin sans Fin.
  - *Maîtrises Majeures Solo (5) :* Requièrent un palier de quêtes ET un paiement en dollars ou niveaux XP : Aimant à Objets, Double Récolte, Établi Portable, Auto-Fonte, Préservation d'Âme (50% XP conservé à la mort).
- **Bascules On/Off instantanées :** Aimant (`/magnet`) et Auto-Fonte (`/autosmelt`) commutables à tout moment via commandes, GUI `/perks` ou le Portail Web.
- **Portail Web Joueur (`/dashboard/perks`) :** Barre de progression globale des quêtes, compteurs, boutons d'achat de maîtrises, bascules en direct synchronisées en temps réel avec SQLite.
- **API Paper :** Déployé avec Paper `26.3.build.19-alpha`.

---

### Patch 26.3-alpha.19 (19 septembre 2026)

- **Améliorations de Guilde Phase 2 :** 5 améliorations permanentes déployées (`GUILD_HOME`, `TERRITORY_BUFF`, `BANK_INTEREST`, `SPAWNER_EFFICIENCY`, `GUILD_VAULT`).
- **Bouclier Anti-Abus Territorial :** Ancre de stabilisation de 5 minutes avant radiation des buffs (compte à rebours ActionBar), synchronisation de présence de 15 secondes à l'entrée, suppression instantanée des buffs en quittant le territoire.
- **Boutons d'Action Directs & Crossplay Bedrock :** GUI `/team` en 54 slots avec boutons aux slots 38 (Foyer), 40 (Coffre), 42 (Banque). Boutons natifs Bedrock Cumulus et raccourcis clic-droit dans `/team upgrades`.
- **Nouvelles commandes :** `/team sethome`, `/team home`, `/team vault` (alias : `/team coffre`, `/team chest`).
- **HeadUtil :** Résolution native de têtes de joueurs avec cache SQLite/RAM persistant (`player_skins`), support Java, Bedrock (Floodgate/Geyser) et SkinsRestorer.
- **Suite de tests :** Étendue à 58 tests automatisés (100% de réussite).

---

### Patch 26.3-alpha.18 (18 septembre 2026)

- **Rôles & Hiérarchie de Guilde :** Rôle `ADMIN` complet en SQLite (`genscore_team_members.role`). Commandes : `/team promote`, `/team demote`, `/team kick`, `/team leave`, `/team disband`. GUI en jeu : clic-gauche pour promouvoir/rétrograder, clic-droit pour exclure.
- **Anti-Grief Total :** Protection complète des chunks contre : casser/poser des blocs, accès aux contenants (coffres, tonneaux, fourneaux, shulkers, entonnoirs), interactions redstone, dégâts aux entités/animaux et pistons franchissant les frontières.
- **Titres de Frontière :** Titre écran + effet sonore à l'entrée dans un territoire revendiqué.
- **Portail Web Guilde enrichi :** Cartes membres avec badges de rôle, promotion/rétrogradation, exclusion avec modal de confirmation, gestion de trésorerie ($/XP), sélecteur de couleur BlueMap, achats d'améliorations.
- **Auto-Sync Ressources Web :** Comparaison des ressources web du JAR au démarrage et extraction automatique dans `plugins/GensCore/web/`.
- **API Paper :** Mise à jour vers `26.3.build.16-alpha`.

---

### Patch 26.3-alpha.17 (18 septembre 2026)

- **Guard Anti-Arbitrage Boutique :** Harmonisation de l'exposant d'inflation avec `GLOBAL_INFLATION_EXPONENT` et plafond strict à 75% du prix d'achat pour le prix de vente, éliminant l'arbitrage de monnaie circulaire infinie.
- **Durcissement Exploit Float :** Validation `Double.isFinite()` sur toutes les entrées économiques et de marché (`/pay`, `/eco`, `/ah sell`) pour bloquer les corruptions `NaN` et `Infinity`.
- **Batching Write-Behind Économie :** Remplacement des écritures SQLite isolées dans `EconomyModule` par un cache Write-Behind groupé (`flushDirtyBalances()` toutes les 10 secondes).
- **Gestion des Loots Thread-Safe :** Synchronisation des instances `YamlConfiguration` dans `LootManager` pour éliminer les conditions de course lors des sauvegardes async concurrentes sur Folia.
- **Arrêt Discord Non-Bloquant :** Remplacement de `Thread.sleep(1500)` par `jda.awaitShutdown(Duration.ofMillis(1500))` avec fallback gracieux vers `shutdownNow()`.

---

### Patch 26.3-alpha.16 (18 septembre 2026)

- **Portefeuille de Solde Joueur :** Carte de solde en direct dans la barre latérale avec animations débit/crédit instantanées (`+`/`-`) et sync au clic.
- **Sécurité Solde Boutique :** Calcul du solde post-achat en temps réel dans le tiroir boutique avec alerte d'avertissement et blocage automatique si les fonds sont insuffisants.
- **Couverture Texture 26.3 à 100% :** Cartographie complète des 1 815 matériaux Minecraft 26.3 (lances vanilla, bois Peuplier, coussins, champignon étagère, lit de paille, blocs de cuivre) avec chaînes de fallback résilientes.
- **API Paper :** Mise à jour vers `26.3.build.16-alpha`.

---

### Patch 26.3-alpha.8 (17 septembre 2026)

- **Quêtes de Craft :** Comptabilisation précise du Shift-Click et multiplicateur de rendement vanilla (ex. 32 torches correctement comptées).
- **Boutique Survie :** Catalogue complet de 319 objets en 7 catégories (ores, farming, bois avec Pale Oak, construction, drops de mob, potions, utilitaires), auto-seeding SQLite, GUI 45 slots avec pagination, `/shop reload`.
- **Mini-Jeux Web :** Casino rééquilibré à 84% de RTP, tranches de la Roue normalisées, nouveau jeu CoinFlip 3D avec switch admin.
- **Localisation :** Couverture bilingue EN/FR à 100% en jeu et sur le portail web.
- **API Paper :** Mise à jour vers `26.3.build.8-alpha`.

---

### Patch 26.3-alpha.7 (16 septembre 2026)

- **Thread Safety Folia :** Résolution du crash causé par `Bukkit.isPrimaryThread()` dans `EconomyModule` — transactions économiques désormais entièrement sûres sur les schedulers régionaux Folia.
- **Résilience Geyser / Floodgate :** Durcissement de `BedrockSkinModule` et `BedrockFormManager` avec handlers `Throwable` capturant les `LinkageError` et `NoClassDefFoundError` JVM.
- **Base de Données & Modules :** Correction du toggle dynamique dans `ModuleManager` pour appeler `initDatabase()` lors de l'activation d'un module à l'exécution.
- **Téléportation :** Gestion de confirmation `CompletableFuture<Boolean>` dans `TeleportUtil` pour les téléportations régionales asynchrones.

---

### Patch 26.3-alpha.6 (16 septembre 2026)

- **API Paper :** Mise à jour vers `26.3.build.6-alpha`.
- **Portail Web :** Élément de nav « Mini-Jeux » masqué dynamiquement quand le module ou tous les jeux sont désactivés.
- **Portail Web :** Redirection automatique vers le tableau de bord si un joueur tente de naviguer vers une route de mini-jeux désactivée.
- **Traductions :** Ajout des descriptions manquantes du module BedrockSkin en EN/FR avec les clés de fallback appropriées.

---

### Patch 26.3-beta.1 — Paper build.28-alpha (16 septembre 2026)

> **Première milestone.** GensCore atteint la stabilité Beta. Tous les modules sont prêts pour la production et entièrement audités.

- **Module Lootr — Migration SQLite complète :** Coffres instanciés par joueur entièrement persistés en SQLite (`lootr_chests` & `lootr_player_chests`) via `LootDAO`. Migration transparente automatique des anciens fichiers `chests.yml` au premier démarrage.
- **Durcissement Folia :** Commandes console dans `QuestModule`, `CustomGuiModule` et `BlueMapModule` garanties d'exécuter sur le `GlobalRegionScheduler` via `runNextTick`.
- **Sécurité Inventaire AuctionHouse :** Récupération d'objets et effacement de la main exécutés dans `runAtEntity` pour éviter les manipulations d'inventaire asynchrones.
- **Fix TeamCommand Chunk Access :** `/team claim` et `/team unclaim` calculent les coordonnées de chunk via arithmétique pure, éliminant `IllegalStateException: Asynchronous chunk access` sur Folia.
- **Fix Race Condition JobsModule :** Remplacement de `dirtyPlayers.clear()` par `dirtyPlayers.removeAll(toSave)` pour éviter les pertes d'XP pendant les sauvegardes concurrentes.
- **Suite de tests :** 69 tests automatisés (contre 58) — 11 nouveaux tests couvrant le CRUD SQLite `LootDAO`, UPSERT, suppression en cascade et round-trips Base64.
- **API Paper :** `26.3.build.28-alpha`.
