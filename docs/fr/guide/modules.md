# Modules en Jeu (29 Modules)

GensCore repose sur une architecture entièrement modulaire pilotée par `ModuleManager`. Chaque module peut être activé ou désactivé dynamiquement sans redémarrer le serveur.

---

## 1. Tombes de Mort (Module Tomb)
Lorsqu'un joueur meurt, son inventaire est sauvegardé dans un bloc physique de tombe aux coordonnées exactes du décès.
- **Protection par Hologramme :** Un affichage flottant indique le nom du propriétaire et le temps de protection restant (ex. 10 minutes).
- **Exclusivité du Propriétaire :** Seule la victime peut faire un clic droit sur sa tombe pour récupérer l'intégralité de ses objets et son expérience.
- **Accès Administrateur :** Les modérateurs/admins disposant de `genscore.tomb.admin` peuvent ouvrir ou déverrouiller n'importe quelle tombe.
- **Expiration :** Une fois le compte à rebours terminé, la tombe devient pillable publiquement ou dépose ses items au sol selon la configuration.

---

## 2. Coffres Instanciés Lootr
Chaque coffre de donjon, forteresse ou structure naturelle est rendu unique par joueur :
- Chaque joueur découvre son propre contenu lors de sa première ouverture.
- Empêche les premiers joueurs arrivés de piller l'ensemble des structures d'un monde fraîchement généré.
- Protection anti-entonnoirs (hopper) intégrée pour bloquer le siphonnage automatique.
- Protection contre la destruction du coffre avant récupération du butin.

---

## 3. Algorithme de Boutique Dynamique
Le module `/shop` calcule les prix d'achat et de vente en fonction de l'offre et de la demande :

$$\text{Prix Actuel} = \text{Prix de Base} \times \left(1 + \frac{\text{Achats} - \text{Ventes}}{\text{Seuil}}\right)^{\text{exposant}}$$

- Une forte demande fait grimper les prix ; une surabondance d'offres les fait chuter.
- Le paramètre `inflationExponent` est réglable en direct via le Panel Web pour équilibrer l'économie de votre serveur.

---

## 4. Spawners Personnalisés & Empilement
Pour éliminer les chutes de TPS causées par les entités sur les serveurs à forte affluence :
- **Empilement (Stacking) :** Les spawners fusionnent en un bloc unique (ex. `x15 Spawner à Squelettes`).
- **Stockage Interne :** Les loots de monstres s'accumulent directement dans le stockage interne du spawner ; les joueurs ouvrent le GUI pour collecter les récompenses d'un coup.
- **Améliorations de Vitesse & XP :** Augmentez la fréquence d'apparition et multipliez les gains d'expérience.

---

## 5. Verrouillage de Coffres & Bouclier Anti-Piston
- Protège les **Coffres, Coffres Piégés, Tonneaux et Boîtes de Shulker**.
- **Bouclier Anti-Piston :** Les pistons ne peuvent ni pousser, ni tirer, ni détruire les conteneurs verrouillés.
- **Anti-Casse de Shulker :** Annule les systèmes de pistons conçus pour casser les boîtes de shulker, neutralisant ainsi les failles de duplication.

---

## 6. Cross-Play Bedrock & Floodgate
- Détection automatique des joueurs Bedrock se connectant via Floodgate / Geyser.
- **Formulaires Cumulus :** Les menus s'affichent sous forme de formulaires Bedrock natifs sur mobile et console pour éviter les désynchronisations tactiles.
- **API Skins & Têtes :** Génère les têtes de joueurs Bedrock authentiques via l'API web (`/api/head/{nom}/{taille}`).
- **Gestion des Préfixes :** Traitement transparent des préfixes Bedrock (`.` ou `*`) dans les commandes et LuckPerms.

---

## 7. Métiers & Professions
6 métiers rémunérateurs et progressifs :
1. **Mineur :** Extraction de minerais, pierres, deepslate.
2. **Bûcheron :** Coupe de troncs d'arbres et de bois.
3. **Chasseur :** Élimination de monstres et boss hostiles.
4. **Fermier :** Récolte de blé, pommes de terre, carottes, citrouilles.
5. **Pêcheur :** Capture de poissons et trésors marins.
6. **Bâtisseur :** Pose de blocs de construction.

La progression est enregistrée de façon asynchrone dans SQLite toutes les 60 secondes sans impacter le thread principal.

---

## 8. Chute Rapide des Feuilles (Fast Leaf Decay)
Détecte l'abattage d'un tronc et déclenche la décomposition accélérée et en cascade des feuilles sans laisser d'arbres volants.

---

## 9. Butin de Têtes (Head Drops)
Probabilité configurable pour les monstres et les joueurs vaincus de laisser tomber leur tête avec leur texture exacte de skin.

---

## 10. Chat Moderne & MiniMessage
- Formatage pur **Kyori Adventure / MiniMessage** (aucun code obsolète `§`).
- Résolution dynamique des préfixes et suffixes LuckPerms.
- Synchronisation Discord bidirectionnelle avec séparation des salons.

---

## 11. Guildes, Claims & Trésorerie Partagée
Un écosystème de guilde complet, compatible Folia multi-thread et crossplay Bedrock :
- **Hiérarchie & Rôles (Chef, Admin, Membre) :**
  - **Chef (`LEADER`) :** Contrôle absolu, promotion/rétrogradation d'admins, dissolution de la guilde, retraits bancaires et claims.
  - **Administrateur (`ADMIN`) :** Cogestion complète : invitations, expulsion de membres réguliers, retraits bancaires, claims/unclaims de territoire, personnalisation BlueMap et achat d'améliorations.
  - **Membre (`MEMBER`) :** Dépôt d'argent et d'XP, participation aux quêtes coopératives et accès aux coffres de guilde verrouillés.
  - *Gestion In-Game & Web :* Gestion via l'interface `/team` (clic gauche pour promouvoir/rétrograder, clic droit pour expulser) et depuis le portail web joueur avec fenêtres de confirmation.
- **Trésorerie Partagée (Banque de Guilde) :** Les membres déposent des dollars (`/team deposit <montant>`) ou des niveaux d'expérience (`/team depositxp <niveaux>`). Lorsque le module d'économie est désactivé, toutes les opérations financières basculent automatiquement et exclusivement sur les niveaux d'expérience. Les chefs et administrateurs effectuent les retraits via `/team withdraw` ou `/team withdrawxp`.
- **Claims de Territoire ($16 \times 16$ Chunks) :** Les guildes revendiquent des parcelles via `/team claim`. Tout claim est strictement financé par le solde de la banque de guilde (1 500 $ ou 10 niveaux d'XP par chunk).
  - *Anti-Grief Intégral :* Protection absolue contre la casse et pose de blocs, ouverture de coffres/barils/fours/shulkers, interactions redstone (portes, trappes, boutons, leviers), dégâts aux entités/animaux/porte-armures, et grief par pistons depuis l'extérieur.
  - *Notifications Frontalières :* Titre et sous-titre animés avec effet sonore immersif affichés sur l'écran du joueur lorsqu'il pénètre sur le territoire d'une guilde.
- **Améliorations de Guilde Actives (10 Arbres de Perks) :** Améliorations permanentes financées exclusivement par la banque de guilde :
  1. *Membres Max (`MAX_MEMBERS`, Niveaux 1 à 3) :* De 5 à 8, 11 et 14 membres maximum.
  2. *Territoire Étendu (`EXTENDED_TERRITORY`, Niveaux 1 à 4) :* De 4 à 8, 12, 16 et 20 chunks revendiquables.
  3. *Boost Métiers (`JOBS_BOOST`, Niveaux 1 à 3) :* +5%, +10% et +15% de gains d'expérience de métiers pour tous les membres en ligne.
  4. *Réduction Taxe HDV (`AH_TAX_REDUCTION`, Niveaux 1 à 2) :* -25% et -50% sur la commission de vente à l'Hôtel des Ventes.
  5. *Boost Quêtes Coop (`COOP_QUESTS_BOOST`, Niveaux 1 à 2) :* +10% et +20% de points de quêtes de guilde.
  6. *Foyer de Guilde (`GUILD_HOME`, Niveaux 1 à 3) :* Point de téléportation partagé (`/team home` et `/team sethome`). Délais réduits : 5s de charge et 15m de cooldown au niveau 1, 3s et 5m au niveau 2, téléportation instantanée dans les claims et 1m de cooldown au niveau 3.
  7. *Aura Territoriale (`TERRITORY_BUFF`, Niveaux 1 à 3) :* Effets passifs diffusés aux membres présents sur le territoire revendiqué (Niveau 1 : Régénération I et Saturation lente, Niveau 2 : Vitesse I, Niveau 3 : Célérité I).
     - *Sécurité Anti-Abus :* Afin d'interdire les failles de minage nomade ou de fuite en combat, tout nouveau claim requiert un temps d'ancrage territorial de 5 minutes (`CLAIM_ANCHOR_WARMUP_MS = 300_000L`) avant de pouvoir diffuser l'aura (décompte affiché dans l'ActionBar). De plus, un délai de présence de 15 secondes est exigé lors de l'entrée sur le territoire avant l'octroi des buffs. Quitter le territoire dissipe immédiatement tous les effets.
  8. *Intérêts Bancaires (`BANK_INTEREST`, Niveaux 1 à 2) :* Versements passifs d'intérêts dans la trésorerie de guilde toutes les 24 heures réelles (+1% par jour plafonné à 10 000 $ ou 50 XP au niveau 1, +2% par jour plafonné à 25 000 $ ou 100 XP au niveau 2).
  9. *Efficacité des Spawners (`SPAWNER_EFFICIENCY`, Niveaux 1 à 2) :* Surcadence les générateurs personnalisés (`SpawnerManager.generateTick()`) posés dans les claims de la guilde (+15% de vitesse de génération au niveau 1, +30% au niveau 2).
  10. *Coffre-fort Virtuel (`GUILD_VAULT`, Niveaux 1 à 3) :* Stockage virtuel commun accessible in-game via `/team vault` (ou `/team coffre`, `/team chest`), depuis le menu `/team` ou via le portail web (18 slots / 2 lignes au niveau 1, 36 slots / 4 lignes au niveau 2, 54 slots / 6 lignes au niveau 3).
- **Boutons d'Action Rapides In-Game & Bedrock :**
  - Dans l'interface `/team` (54 slots) : Slot 38 (Lit : Clic gauche pour `/team home`, Clic droit pour `/team sethome`), Slot 40 (Coffre/Tonneau : Clic gauche pour `/team vault`), Slot 42 (Statut Banque).
  - Raccourcis clic droit intégrés directement dans `/team upgrades` (Slot 19 pour le Home, Slot 23 pour le Coffre).
  - Formulaires Bedrock Cumulus proposant des boutons natifs dédiés au Home, à la définition du Home et au Coffre de guilde.
- **Intégration Temps Réel BlueMap :** Tous les claims de guilde sont dessinés sur la carte 3D BlueMap avec une couleur hexadécimale personnalisable via `/team color <#hex>` ou le Panel Web.

---

## 12. Bonus Personnels de Quêtes (SoloPerkModule)
Système complet de progression et de récompenses pour les joueurs solo, reliant l'accomplissement des quêtes journalières à des avantages permanents et des maîtrises majeures :
- **Autonomie & Contrôle :** 29ème module autonome de GensCore (`solo_perks`), activable et désactivable dynamiquement en direct (`/module solo_perks <on|off>`) ou via l'interface web admin sans redémarrage.
- **Paliers Gratuits de Quêtes :** Déblocage automatique ou par clic dès que le quota de quêtes réalisées à vie est atteint :
  1. *Relance Gratuite (5 quêtes) :* 1 reroll quotidien gratuit de quête (`/quest reroll`).
  2. *Foyer Additionnel (15 quêtes) :* +1 home supplémentaire utilisable avec `/sethome`.
  3. *Foulée Céleste (30 quêtes) :* Effet Speed I permanent hors combat.
  4. *Savoir des Métiers (50 quêtes) :* Multiplicateur permanent de +5% sur tous les gains d'XP de métiers.
  5. *Téléportation Instantanée (75 quêtes) :* Temps de charge des téléportations divisé par deux.
  6. *Festin Infini (100 quêtes) :* Accès débloqué à la commande `/feed` avec un temps de recharge de 15 minutes sans nécessité d'un grade VIP.
- **Maîtrises Majeures Personnelles :** Déblocage conditionné par un palier de quêtes ET un coût d'acquisition en dollars ($) ou en niveaux d'expérience (XP) lorsque l'économie est coupée :
  1. *Aimant de Collecte (25 quêtes, 20 000 $ / 40 XP) :* Aspire automatiquement les objets au sol dans un rayon de 5 blocs vers le joueur. Commutable en direct (`/magnet`, GUI `/perks` ou Web).
  2. *Bénédiction Minérale (40 quêtes, 35 000 $ / 60 XP) :* 5% de chance de doubler les drops de minerais bruts et de bois récoltés.
  3. *Atelier Portatif (60 quêtes, 50 000 $ / 80 XP) :* Débloque les commandes `/craft` et `/workbench` n'importe où.
  4. *Fonte Instantanée (80 quêtes, 75 000 $ / 100 XP) :* Transforme instantanément les minerais bruts minés en lingots cuits. Commutable en direct (`/autosmelt`, GUI `/perks` ou Web).
  5. *Âme Préservée (100 quêtes, 100 000 $ / 120 XP) :* Conserve 50% de vos niveaux d'expérience lors d'une mort.
- **Synchronisation Instantanée In-Game & Web :** Toute acquisition ou commutation On/Off est synchronisée en temps réel entre la base SQLite, le GUI joueur en jeu (`/perks`) et l'onglet dédié du portail web (`/dashboard/perks`).

---

## Liste Complète des Modules (29)
`UtilsModule`, `TombModule`, `TeleportTpaModule`, `TeleportSpawnModule`, `TeleportHomeModule`, `TeleportBackModule`, `TeamModule`, `TabBoardModule`, `StatsModule`, `SpawnerModule`, `ShopModule`, `SoloPerkModule`, `QuestModule`, `MotdModule`, `ModerationModule`, `LootModule`, `LockModule`, `HeadDropModule`, `CustomGuiModule`, `GuiModule`, `JobsModule`, `FastLeafDecayModule`, `EconomyModule`, `ChatModule`, `BlueMapModule`, `DiscordModule`, `AuctionHouseModule`, `AuthModule`, `BedrockSkinModule`.
