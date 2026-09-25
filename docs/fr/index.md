---
layout: home

hero:
  name: "GensCore"
  text: "Core Paper & Folia Haute Performance"
  tagline: "Moteur Survie & Faction autonome conçu pour Minecraft 26.3+ sur Java 25 (LTS)"
  image:
    src: /icon.png
    alt: GensCore
  actions:
    - theme: brand
      text: Démarrage Rapide
      link: /fr/guide/getting-started
    - theme: alt
      text: Suivi & Roadmap 26.3
      link: /fr/guide/roadmap-26-3
    - theme: alt
      text: Liste des Commandes
      link: /fr/guide/commands
    - theme: alt
      text: Dépôt GitHub
      link: https://github.com/WilliamBossard/GensCore

features:
  - title: Threading Régional Folia
    details: Schedulers totalement découplés (GlobalRegion, Region, Entity) pour 20 TPS constants sans aucun lag.
  - title: Anti-Exploit & Verrouillage
    details: Immunité contre le grief par pistons, protection anti-casse de shulker et patchs des exploits d'inventaire.
  - title: Cross-Play Bedrock Natif
    details: Formulaires Cumulus natifs pour Geyser/Floodgate, support des skins et rendu des avatars Bedrock.
  - title: Panel Web Intégré
    details: Micro-serveur React 18 + Javalin sur le port 8080 avec console en direct, gestion des joueurs et BlueMap.
  - title: Économie Dynamique & Métiers
    details: Boutique à inflation/déflation automatique, Hôtel des Ventes joueur-à-joueur et 6 métiers rémunérateurs.
  - title: Moteur SQLite Autonome
    details: Base de données embarquée en mode WAL (Write-Ahead Logging). Fonctionne à 100% sans serveur SQL externe.
---

<div style="max-width: 900px; margin: 2rem auto 1rem; padding: 0 1rem;">
  <div class="gens-launcher-card">
    <div class="gens-launcher-badge">Écosystème Gens</div>
    <h3 class="gens-launcher-title">Découvrez Gens Launcher — Le Lanceur Client Officiel</h3>
    <p class="gens-launcher-desc">
      Pour offrir à vos joueurs une connexion optimale et sécurisée sur votre serveur, recommandez <strong>Gens Launcher</strong> : synchronisation cloud déportée Horizon Delta Sync (Google Drive, Dropbox, OneDrive), gestion multi-modloaders (Vanilla, Fabric, Forge, Quilt, NeoForge), catalogue Modrinth/CurseForge intégré et téléchargement automatique de Java (Java 8 à 25).
    </p>
    <div class="gens-launcher-pills">
      <span class="gens-launcher-pill">Horizon Delta Sync</span>
      <span class="gens-launcher-pill">Multi-Modloaders</span>
      <span class="gens-launcher-pill">Dépôt APT Linux</span>
      <span class="gens-launcher-pill">Windows & macOS</span>
    </div>
    <div class="gens-launcher-actions">
      <a href="https://williambossard.github.io/Gens-Launcher/" target="_blank" class="gens-launcher-btn-prim">
        Découvrir Gens Launcher
      </a>
      <a href="https://github.com/WilliamBossard/Gens-Launcher" target="_blank" class="gens-launcher-btn-sec">
        Dépôt GitHub Launcher
      </a>
    </div>
  </div>
</div>
