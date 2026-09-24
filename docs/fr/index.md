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

<div style="max-width: 900px; margin: 2.5rem auto 1rem; padding: 0 1rem;">
  <div style="background: linear-gradient(135deg, rgba(30, 27, 75, 0.75) 0%, rgba(15, 23, 42, 0.9) 100%); border: 1px solid rgba(168, 85, 247, 0.4); border-radius: 12px; padding: 1.8rem 2rem; box-shadow: 0 8px 32px rgba(147, 51, 234, 0.15); margin-bottom: 2rem;">
    <div style="display: flex; align-items: center; gap: 0.6rem; margin-bottom: 0.8rem; flex-wrap: wrap;">
      <span style="font-size: 0.75rem; font-weight: 700; text-transform: uppercase; letter-spacing: 0.5px; background: linear-gradient(90deg, #9333ea, #6366f1); color: #ffffff; padding: 0.25rem 0.7rem; border-radius: 9999px; box-shadow: 0 2px 8px rgba(147, 51, 234, 0.4);">
        NOUVELLE VERSION
      </span>
      <span style="font-size: 0.8rem; font-weight: 600; color: #cbd5e1;">GensCore Release 1.0.1</span>
    </div>
    <h3 style="font-size: 1.4rem; font-weight: 800; color: #fff; margin-bottom: 0.6rem; letter-spacing: -0.3px;">
      GensCore 1.0.1 est prêt pour Minecraft 26.3 & Paper Alpha Build 40 !
    </h3>
    <p style="color: var(--vp-c-text-2); font-size: 0.95rem; line-height: 1.6; margin-bottom: 1.2rem;">
      Cette version majeure apporte la prise en charge officielle de <strong>Minecraft 26.3</strong> sous <strong>Java 25 LTS</strong>, la mise à niveau vers <strong>Paper 26.3.build.40-alpha</strong>, le passage à Jackson Databind 2.22.3 & JDA 6.7.0, une interface web totalement optimisée pour mobiles & smartphones, ainsi que le support architectural de <strong>Geyser Standalone</strong> sur Pterodactyl.
    </p>
    <div style="display: flex; gap: 0.5rem; flex-wrap: wrap; margin-bottom: 1.4rem;">
      <span style="font-size: 0.75rem; font-weight: 600; color: #e9d5ff; background: rgba(168, 85, 247, 0.15); padding: 0.25rem 0.6rem; border-radius: 4px; border: 1px solid rgba(168, 85, 247, 0.3);">Minecraft 26.3</span>
      <span style="font-size: 0.75rem; font-weight: 600; color: #e9d5ff; background: rgba(168, 85, 247, 0.15); padding: 0.25rem 0.6rem; border-radius: 4px; border: 1px solid rgba(168, 85, 247, 0.3);">Paper Build 40-alpha</span>
      <span style="font-size: 0.75rem; font-weight: 600; color: #e9d5ff; background: rgba(168, 85, 247, 0.15); padding: 0.25rem 0.6rem; border-radius: 4px; border: 1px solid rgba(168, 85, 247, 0.3);">Java 25 LTS</span>
      <span style="font-size: 0.75rem; font-weight: 600; color: #e9d5ff; background: rgba(168, 85, 247, 0.15); padding: 0.25rem 0.6rem; border-radius: 4px; border: 1px solid rgba(168, 85, 247, 0.3);">Mobile Responsive</span>
      <span style="font-size: 0.75rem; font-weight: 600; color: #e9d5ff; background: rgba(168, 85, 247, 0.15); padding: 0.25rem 0.6rem; border-radius: 4px; border: 1px solid rgba(168, 85, 247, 0.3);">Geyser Standalone</span>
    </div>
    <div style="display: flex; gap: 0.8rem; flex-wrap: wrap;">
      <a href="/fr/guide/roadmap-26-3" style="display: inline-flex; align-items: center; gap: 0.5rem; padding: 0.6rem 1.3rem; border-radius: 6px; font-size: 0.85rem; font-weight: 600; text-decoration: none; background: linear-gradient(90deg, #9333ea, #6366f1); color: #fff; box-shadow: 0 4px 14px rgba(147, 51, 234, 0.3);">
        Consulter la Feuille de Route 26.3
      </a>
      <a href="/fr/guide/getting-started" style="display: inline-flex; align-items: center; gap: 0.5rem; padding: 0.6rem 1.2rem; border-radius: 6px; font-size: 0.85rem; font-weight: 600; text-decoration: none; background: rgba(255, 255, 255, 0.08); color: #e2e8f0; border: 1px solid rgba(255, 255, 255, 0.15);">
        Guide de Démarrage
      </a>
    </div>
  </div>

  <div style="background: linear-gradient(135deg, rgba(30, 41, 59, 0.7) 0%, rgba(15, 23, 42, 0.8) 100%); border: 1px solid rgba(56, 189, 248, 0.3); border-radius: 12px; padding: 2rem; box-shadow: 0 8px 32px rgba(0, 0, 0, 0.2);">
    <div style="display: inline-block; font-size: 0.75rem; font-weight: 700; text-transform: uppercase; letter-spacing: 0.5px; color: #38bdf8; background: rgba(56, 189, 248, 0.15); padding: 0.25rem 0.6rem; border-radius: 4px; border: 1px solid rgba(56, 189, 248, 0.3); margin-bottom: 0.8rem;">Écosystème Gens</div>
    <h3 style="font-size: 1.35rem; font-weight: 700; color: #fff; margin-bottom: 0.6rem;">Découvrez Gens Launcher — Le Lanceur Client Officiel</h3>
    <p style="color: var(--vp-c-text-2); font-size: 0.95rem; line-height: 1.6; margin-bottom: 1.2rem;">
      Pour offrir à vos joueurs une connexion optimale et sécurisée sur votre serveur, recommandez <strong>Gens Launcher</strong> : synchronisation cloud déportée Horizon Delta Sync (Google Drive, Dropbox, OneDrive), gestion multi-modloaders (Vanilla, Fabric, Forge, Quilt, NeoForge), catalogue Modrinth/CurseForge intégré et téléchargement automatique de Java (Java 8 à 25).
    </p>
    <div style="display: flex; gap: 0.5rem; flex-wrap: wrap; margin-bottom: 1.5rem;">
      <span style="font-size: 0.75rem; font-weight: 600; color: #cbd5e1; background: rgba(255, 255, 255, 0.08); padding: 0.25rem 0.6rem; border-radius: 4px; border: 1px solid rgba(255, 255, 255, 0.1); font-family: var(--vp-font-family-mono);">Horizon Delta Sync</span>
      <span style="font-size: 0.75rem; font-weight: 600; color: #cbd5e1; background: rgba(255, 255, 255, 0.08); padding: 0.25rem 0.6rem; border-radius: 4px; border: 1px solid rgba(255, 255, 255, 0.1); font-family: var(--vp-font-family-mono);">Multi-Modloaders</span>
      <span style="font-size: 0.75rem; font-weight: 600; color: #cbd5e1; background: rgba(255, 255, 255, 0.08); padding: 0.25rem 0.6rem; border-radius: 4px; border: 1px solid rgba(255, 255, 255, 0.1); font-family: var(--vp-font-family-mono);">Dépôt APT Linux</span>
      <span style="font-size: 0.75rem; font-weight: 600; color: #cbd5e1; background: rgba(255, 255, 255, 0.08); padding: 0.25rem 0.6rem; border-radius: 4px; border: 1px solid rgba(255, 255, 255, 0.1); font-family: var(--vp-font-family-mono);">Windows & macOS</span>
    </div>
    <div style="display: flex; gap: 0.8rem; flex-wrap: wrap;">
      <a href="https://williambossard.github.io/Gens-Launcher/" target="_blank" style="display: inline-flex; align-items: center; gap: 0.5rem; padding: 0.6rem 1.2rem; border-radius: 6px; font-size: 0.85rem; font-weight: 600; text-decoration: none; background: #0284c7; color: #fff;">
        Découvrir Gens Launcher
      </a>
      <a href="https://github.com/WilliamBossard/Gens-Launcher" target="_blank" style="display: inline-flex; align-items: center; gap: 0.5rem; padding: 0.6rem 1.2rem; border-radius: 6px; font-size: 0.85rem; font-weight: 600; text-decoration: none; background: rgba(255, 255, 255, 0.08); color: #e2e8f0; border: 1px solid rgba(255, 255, 255, 0.15);">
        Dépôt GitHub Launcher
      </a>
    </div>
  </div>
</div>
