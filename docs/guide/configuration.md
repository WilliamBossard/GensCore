# Configuration & Deployment

GensCore creates a clean and organized configuration structure in `plugins/GensCore/`.

---

## 📁 File Structure

```
plugins/GensCore/
├── config.yml              # Core settings (language, database options)
├── modules.yml             # Global enable/disable flags for all 28 modules
├── genscore.db             # Local SQLite database (WAL mode)
├── lang/
│   ├── fr_FR.yml           # French localization messages
│   └── en_US.yml           # English localization messages
├── menus/
│   └── default.yml         # Custom YAML GUI menus
├── modules/
│   ├── web.yml             # Web panel port, admin password, CORS settings
│   ├── discord.yml         # Discord bot token, channels, embed options
│   ├── minigames.yml       # Wheel of fortune rewards and chances
│   ├── lootr.yml           # Lootr chest behavior and particle settings
│   ├── shop.yml            # Dynamic shop items, base prices, categories
│   └── ...
└── web/                    # Extracted React / Vite frontend files (index.html, assets)
```

---

## 🌐 Network Ports & Port Forwarding

If running on a VPS or dedicated host:

| Port | Protocol | Purpose | Required |
|---|---|---|---|
| **25565** | TCP/UDP | Minecraft Server | Yes |
| **8080** | TCP | Web Panel & REST API | Yes (if Web Panel enabled) |
| **8100** | TCP | BlueMap (Optional) | Optional |

> [!TIP]
> **HTTPS / SSL:** You can place an Nginx, Caddy, or Cloudflare reverse proxy in front of port `8080` for custom domain SSL encryption. Set `web.allowed_origin` in `modules/web.yml` accordingly.

---

## 🤖 Discord Bot Setup

1. Create an application on the [Discord Developer Portal](https://discord.com/developers/applications).
2. Under the **Bot** tab, generate a token.
3. Enable **Server Members Intent** and **Message Content Intent**.
4. Open `plugins/GensCore/modules/discord.yml` and paste your token:
   ```yaml
   bot_token: "YOUR_BOT_TOKEN"
   channel_id: "YOUR_CHANNEL_ID"
   ```
5. Restart your server or run `/module discord on`.
