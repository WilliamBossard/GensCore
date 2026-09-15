# Configuration & Déploiement

GensCore génère une arborescence de configuration claire, modulaire et structurée dans `plugins/GensCore/`.

---

## Arborescence des Fichiers

```
plugins/GensCore/
├── config.yml              # Paramètres globaux (langue, base de données)
├── modules.yml             # Activation/Désactivation générale des 28 modules
├── genscore.db             # Base de données locale SQLite (mode WAL)
├── lang/
│   ├── fr_FR.yml           # Fichier de traduction française
│   └── en_US.yml           # Fichier de traduction anglaise
├── menus/
│   └── default.yml         # Menus inventaires personnalisables en YAML
├── modules/
│   ├── web.yml             # Port du panel web, mot de passe admin, paramètres CORS
│   ├── discord.yml         # Token du bot Discord, salons, options d'embeds
│   ├── minigames.yml       # Récompenses et probabilités de la Roue de la Fortune
│   ├── lootr.yml           # Coffres instanciés et effets de particules
│   ├── shop.yml            # Boutique dynamique, prix de base, catégories
│   └── ...
└── web/                    # Fichiers frontend React / Vite extraits (index.html, JS, CSS)
```

---

## Ports Réseau & Redirection (Port Forwarding)

Si vous hébergez sur un VPS ou serveur dédié :

| Port | Protocole | Utilité | Requis |
|---|---|---|---|
| **25565** | TCP/UDP | Serveur Minecraft | Oui |
| **8080** | TCP | Panel Web & API REST | Oui (si le panel est activé) |
| **8100** | TCP | BlueMap (Optionnel) | Optionnel |

> [!TIP]
> **HTTPS / SSL :** Vous pouvez placer un reverse proxy Nginx, Caddy ou Cloudflare devant le port `8080` pour associer un nom de domaine avec certificat SSL. Pensez à ajuster `web.allowed_origin` dans `modules/web.yml`.

---

## Configuration du Bot Discord

1. Rendez-vous sur le [Portail Développeur Discord](https://discord.com/developers/applications) et créez une application.
2. Dans l'onglet **Bot**, générez un token.
3. Cochez impérativement **Server Members Intent** et **Message Content Intent**.
4. Ouvrez `plugins/GensCore/modules/discord.yml` et insérez vos identifiants :
   ```yaml
   bot_token: "VOTRE_TOKEN_BOT"
   channel_id: "ID_DU_SALON"
   ```
5. Redémarrez le serveur ou exécutez la commande `/module discord on`.
