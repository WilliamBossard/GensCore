# Panel Web & Mini-jeux

GensCore intègre un serveur web autonome **React 18 + Javalin** fonctionnant directement sur le port `8080`.

---

## Sécurité & Architecture

1. **Micro-Serveur Embarqué :** Javalin s'exécute au sein même du processus Minecraft sans nécessiter Apache ou Nginx en prérequis.
2. **Protection par Hachage BCrypt :** Le mot de passe administrateur par défaut (`gens` dans `modules/web.yml`) est automatiquement converti en hash BCrypt sécurisé dès le premier démarrage.
3. **Jetons de Session :**
   - Les requêtes d'administration utilisent des tokens Bearer valides 24 heures.
   - Les sessions joueurs utilisent des tokens de 7 jours liés aux identifiants du `/register`.
4. **Protection Anti-Brute-Force :** Bloque temporairement toute adresse IP cumulant plus de 5 tentatives de connexion infructueuses par minute.

---

## Tableau de Bord Administrateur

Accessible sur : `http://<ip-de-votre-serveur>:8080/admin`

- **Performances en Direct :** Jauges en temps réel pour le TPS, les joueurs connectés, l'allocation mémoire JVM et la charge CPU.
- **Console Web Interactive :** Streaming en direct des logs du serveur avec envoi sécurisé de commandes via le `GlobalRegionScheduler` de Folia.
- **Gestion des Joueurs :** Recherche de joueurs, inspection des soldes, modification de l'argent et exécution de kicks/bans/mutes depuis le navigateur.
- **Commutateur de Modules en Direct :** Activez ou désactivez n'importe lequel des 28 modules d'un simple clic.
- **Éditeur de Configuration Dynamique :** Ajustez l'inflation de la boutique, les taxes d'enchères, les chances de têtes, le reroll de quêtes et le MOTD en direct.
- **BlueMap Intégré :** Carte 2D/3D interactive intégrée dans l'interface.
- **Purge d'Urgence du Serveur :** Bouton de réinitialisation atomique de la base de données protégé par mot de passe.

---

## Portail Joueur Web

Accessible sur : `http://<ip-de-votre-serveur>:8080`

- **Espace Personnel :** Connexion avec les identifiants définis en jeu via `/register`.
- **Statistiques en Temps Réel :** Solde du porte-monnaie, niveaux de métiers, blocs cassés, monstres tués, morts et ratio K/D.
- **Historique des Quêtes sur 7 Jours :** Graphique vectoriel SVG suivant les quêtes accomplies sur la semaine.
- **Historique Financier :** Consultation des 5 dernières transactions économiques.
- **Moteur de Têtes & Avatars (`/api/head/{nom}/{taille}`) :** Génération d'avatars 3D haute résolution pour les joueurs Java et Bedrock.

---

---

## Boutique Dynamique Web & Dépôts

La boutique dynamique du serveur est entièrement accessible et synchronisée en direct sur le web (`http://<ip-de-votre-serveur>:8080/shop`).

### 1. Achat en Ligne
- Visualisation du cours dynamique en temps réel avec graphiques de tendance et jauges de stock.
- Achat direct par tranche de quantite (x1, x16, x32, x64, Max ou curseur précis).
- **Distribution Intelligente :** Si le joueur est connecté en jeu, ses objets sont livrés immédiatement dans son inventaire (ou déposés au sol si plein). Si le joueur est déconnecté, les récompenses sont stockées de façon sécurisée en base et distribuées automatiquement lors de sa prochaine connexion.

### 2. Réserve & Vente d'Objets Déposés (`/web deposit`)
- **Dépôt en jeu :** Tenez n'importe quel bloc ou objet en main et tapez `/web deposit`. L'objet est retiré et transféré dans votre réserve web.
- **Limite de Stockage (27 Slots) :** Par défaut, un joueur dispose d'un espace de **27 slots** (l'équivalent exact d'un coffre Minecraft), configurable via la clé `web.deposit_limit` dans `modules/web.yml`.
- **Stacking Intelligent :**
  - Les objets identiques (même matériau et NBT/enchantements identiques) s'empilent automatiquement dans un même slot de réserve jusqu'à la limite maximale de pile Minecraft (64 pour les minerais/blocs, 16 pour les perles de l'Ender, etc.).
  - Les armes, outils et équipements (stack max = 1) ne s'empilent jamais.
  - En cas de surplus dépassant le stack max, un nouveau slot est utilisé s'il reste de la place, sinon l'excédent est restitué au joueur.
- **Sélecteur de Quantité :** Dans l'onglet **Mes Objets Déposés** et dans le tiroir du shop, vous pouvez choisir précisément la quantité à vendre ou à récupérer via un curseur et des boutons rapides (x1, x16, x32, x64, Max).
- **Retrait en jeu (`/web withdraw`) :** Cliquez sur le bouton "Récupérer en jeu" sur le web, ou tapez directement `/web withdraw` sur le serveur pour ouvrir le menu d'inventaire 54 slots et récupérer vos objets !

---

## Mini-jeux Web

### 1. Roue de la Fortune
- Un lancer gratuit disponible toutes les 24 heures.
- Table de gains pondérée et entièrement paramétrable :
  - 10 Diamants (40%)
  - 2 Lingots de Netherite (30%)
  - 1 Pomme Dorée Enchantée (20%)
  - 1 Paire d'Élytres (9%)
  - 1 Spawner à Vaches (Jackpot 1%)
- **Distribution Hors-Ligne Sécurisée :** Si un joueur tourne la roue alors qu'il est déconnecté du serveur Minecraft, son lot est placé en file d'attente dans SQLite et lui est remis automatiquement lors de sa prochaine connexion via le scheduler de Folia.

### 2. Casino Web (Machine à Sous)
- Misez vos véritables objets Minecraft sur une machine à sous web animée à 3 rouleaux.
- **Délai d'une heure (cooldown)** entre chaque lancer pour éviter les abus.
- **Dépôt :** Alimenté par vos objets déposés via `/web deposit`.
- **Jeu :** Faites tourner la machine dans votre navigateur avec vos objets misés.
- **Retrait :** Directement via l'interface web ou via `/web withdraw` en jeu.
