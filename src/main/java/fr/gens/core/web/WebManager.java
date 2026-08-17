package fr.gens.core.web;

import fr.gens.core.CorePlugin;
import fr.gens.core.modules.Module;
import io.javalin.Javalin;
import io.javalin.http.staticfiles.Location;
import fr.gens.core.modules.EconomyModule;
import fr.gens.core.modules.shop.ShopCategory;
import fr.gens.core.modules.shop.ShopItem;
import fr.gens.core.modules.shop.ShopModule;
import fr.gens.core.modules.headdrop.HeadDropModule;
import fr.gens.core.modules.discord.DiscordModule;
import org.bukkit.Material;
import java.awt.Color;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Calendar;
import java.io.File;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;

public class WebManager {

    private final CorePlugin plugin;
    private final int port;
    private Javalin app;
    private String[] corsOrigins;

    public WebManager(CorePlugin plugin, int port) {
        this.plugin = plugin;
        this.port = port;
    }

    public void setCorsOrigins(String[] origins) {
        this.corsOrigins = origins;
    }

    private java.util.Map<String, Object> convertToMap(org.bukkit.configuration.ConfigurationSection section) {
        java.util.Map<String, Object> map = new java.util.LinkedHashMap<>();
        for (String key : section.getKeys(false)) {
            Object value = section.get(key);
            if (value instanceof org.bukkit.configuration.ConfigurationSection) {
                map.put(key, convertToMap((org.bukkit.configuration.ConfigurationSection) value));
            } else {
                map.put(key, value);
            }
        }
        return map;
    }

    public void start() {
        // Extraction des fichiers web s'ils n'existent pas ou si index.html manque
        File webDir = new File(plugin.getDataFolder(), "web");
        File indexFile = new File(webDir, "index.html");
        
        if (!webDir.exists() || !indexFile.exists()) {
            webDir.mkdirs();
            try {
                java.net.URL url = getClass().getProtectionDomain().getCodeSource().getLocation();
                if (url != null) {
                    File jarFile = new File(url.toURI());
                    if (jarFile.isFile()) {
                        try (java.util.jar.JarFile jar = new java.util.jar.JarFile(jarFile)) {
                            java.util.Enumeration<java.util.jar.JarEntry> entries = jar.entries();
                            while (entries.hasMoreElements()) {
                                java.util.jar.JarEntry entry = entries.nextElement();
                                if (entry.getName().startsWith("public/") && !entry.isDirectory()) {
                                    String destPath = entry.getName().substring("public/".length());
                                    File destFile = new File(webDir, destPath);
                                    destFile.getParentFile().mkdirs();
                                    try (java.io.InputStream in = jar.getInputStream(entry)) {
                                        java.nio.file.Files.copy(in, destFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                                    }
                                }
                            }
                        }
                        plugin.getLogger().info("Web files extracted in " + webDir.getPath());
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().severe("Error extracting web files: " + e.getMessage());
            }
        }

        // Sauvegarder le ClassLoader de Bukkit
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        
        try {
            // Forcer le ClassLoader de Javalin pour éviter les conflits dans un plugin
            Thread.currentThread().setContextClassLoader(Javalin.class.getClassLoader());

            app = Javalin.create(config -> {
                // Activer le CORS
                config.bundledPlugins.enableCors(cors -> {
                    cors.addRule(it -> {
                        it.anyHost();
                    });
                });
                
                // Servir le site web depuis le dossier plugins/GensCore/web/
                config.staticFiles.add(webDir.getAbsolutePath(), Location.EXTERNAL);

                // Gérer le routage SPA (Single Page Application)
                config.spaRoot.addFile("/", new File(webDir, "index.html").getAbsolutePath(), Location.EXTERNAL);

            }).start(port);

            // Middleware d'authentification pour les routes /api/admin/*
            app.before("/api/admin/*", ctx -> {
                String authHeader = ctx.header("Authorization");
                String expectedPassword = plugin.getConfigManager().getConfig("modules/web.yml").getString("admin-password", "gens");
                
                if (authHeader == null || !authHeader.equals("Bearer " + expectedPassword)) {
                    ctx.status(401).json("Non autorisé: Mot de passe incorrect");
                }
            });
            WebPlayerAPI playerAPI = new WebPlayerAPI(plugin, app);
            playerAPI.registerRoutes();

            setupRoutes();
            
        } catch (Exception e) {
            plugin.getLangManager().sendConsoleError("webmanager.log_1");
            e.printStackTrace();
        } finally {
            // Remettre le ClassLoader original de Bukkit
            Thread.currentThread().setContextClassLoader(classLoader);
        }
    }

    public static class ConfigResponse {
        public double inflationExponent;
        public double ahTaxPercentage;
        public String adminPassword;
        public double headDropChance;
        public int maxQuestsRerolls;
        public boolean lootrPreventBreak;
        public boolean lootrPreventHopper;
        public boolean lootrParticles;
        public String motdLine1;
        public String motdLine2;
        public boolean minigameWheelEnabled;
        public boolean minigameCasinoEnabled;
        public String publicFeaturesText;
        public String bluemapUrl;
        public String serverIp;
        public String tombBlockType;
        public boolean tombStoreXp;
        public long tombExpirationSeconds;
        public String tombExpirationAction;
        public String tombDefaultAccess;

        public ConfigResponse(double inf, double ahTax, String pass, double hDrop, int qRerolls, boolean lootrPreventBreak, boolean lootrPreventHopper, boolean lootrParticles, String motdLine1, String motdLine2, boolean wheel, boolean casino, String publicFeaturesText, String bluemapUrl, String serverIp, String tombBlockType, boolean tombStoreXp, long tombExpirationSeconds, String tombExpirationAction, String tombDefaultAccess) {
            this.inflationExponent = inf;
            this.ahTaxPercentage = ahTax;
            this.adminPassword = pass;
            this.headDropChance = hDrop;
            this.maxQuestsRerolls = qRerolls;
            this.lootrPreventBreak = lootrPreventBreak;
            this.lootrPreventHopper = lootrPreventHopper;
            this.lootrParticles = lootrParticles;
            this.motdLine1 = motdLine1;
            this.motdLine2 = motdLine2;
            this.minigameWheelEnabled = wheel;
            this.minigameCasinoEnabled = casino;
            this.publicFeaturesText = publicFeaturesText;
            this.bluemapUrl = bluemapUrl;
            this.serverIp = serverIp;
            this.tombBlockType = tombBlockType;
            this.tombStoreXp = tombStoreXp;
            this.tombExpirationSeconds = tombExpirationSeconds;
            this.tombExpirationAction = tombExpirationAction;
            this.tombDefaultAccess = tombDefaultAccess;
        }
    }

    public static class ConfigRequest {
        public double inflationExponent;
        public double ahTaxPercentage;
        public String adminPassword;
        public double headDropChance;
        public int maxQuestsRerolls;
        public boolean lootrPreventBreak;
        public boolean lootrPreventHopper;
        public boolean lootrParticles;
        public String motdLine1;
        public String motdLine2;
        public boolean minigameWheelEnabled;
        public boolean minigameCasinoEnabled;
        public String publicFeaturesText;
        public String bluemapUrl;
        public String serverIp;
        public String tombBlockType;
        public boolean tombStoreXp;
        public long tombExpirationSeconds;
        public String tombExpirationAction;
        public String tombDefaultAccess;
    }

    public static class FileEditRequest {
        public String content;
    }

    public void stop() {
        if (app != null) {
            app.stop();
            plugin.getLangManager().sendConsoleMessage("webmanager.log_2");
        }
    }

    private void setupRoutes() {
        // Route API pour récupérer la liste des modules (Public)
        app.get("/api/modules", ctx -> {
            List<Map<String, Object>> modulesList = new ArrayList<>();
            for (Module m : plugin.getModuleManager().getModules()) {
                Map<String, Object> moduleData = new HashMap<>();
                moduleData.put("name", m.getName());
                moduleData.put("description", m.getDescription());
                moduleData.put("enabled", m.isEnabled());
                modulesList.add(moduleData);
            }
            ctx.json(modulesList);
        });

        // Texte public de la page d'accueil
        String defaultPublicText = "Welcome to our brand new Minecraft server!\n\n" +
                "Explore a unique world filled with custom features and an evolving economy. " +
                "Join a guild, complete daily quests, and level up your jobs to become the wealthiest player on the server.\n\n" +
                "Our custom web panel allows you to view your stats, check the global leaderboard, and even interact with the in-game economy directly from your browser!\n\n" +
                "Read the complete documentation below to learn more about our custom features.";

        app.get("/api/public/features", ctx -> {
            ctx.header("Cache-Control", "no-cache, no-store, must-revalidate");
            ctx.result(plugin.getConfigManager().getConfig("modules/web.yml").getString("web.public_features_text", defaultPublicText));
        });

        // API de Langue
        app.get("/api/public/lang", ctx -> {
            ctx.header("Cache-Control", "no-cache, no-store, must-revalidate");
            
            String forceLang = plugin.getConfigManager().getConfig("modules/web.yml").getString("web.force_lang", "");
            String lang = (forceLang != null && !forceLang.isEmpty()) ? forceLang : ctx.queryParam("lang");
            if (lang == null || lang.isEmpty() || lang.equals("dev")) lang = plugin.getConfig().getString("lang", "fr_FR");
            if (lang.contains("-")) lang = lang.replace("-", "_"); // i18next envoie fr-FR parfois
            
            java.io.File langFile = new java.io.File(plugin.getDataFolder() + java.io.File.separator + "lang", "web_" + lang + ".yml");
            if (!langFile.exists()) {
                langFile = new java.io.File(plugin.getDataFolder() + java.io.File.separator + "lang", "web_fr_FR.yml");
            }
            if (langFile.exists()) {
                org.bukkit.configuration.file.FileConfiguration langConfig = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(langFile);
                ctx.json(convertToMap(langConfig));
            } else {
                ctx.status(404).json(new java.util.HashMap<>()); // Retourne objet vide au lieu d'une erreur
            }
        });

        // Route API pour activer/désactiver un module (Protégée Admin)
        app.post("/api/admin/modules/{name}/toggle", ctx -> {
            String moduleName = ctx.pathParam("name");
            
            // On récupère le body {"state": true/false}
            ToggleRequest request = ctx.bodyAsClass(ToggleRequest.class);
            
            // On exécute l'activation sur le thread principal de Bukkit (Très important !)
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                boolean success = plugin.getModuleManager().toggleModule(moduleName, request.state);
                if(success) {
                    plugin.getLogger().info("Web panel changed state of module " + moduleName + " to " + request.state);
                }
            });
            
            ctx.status(200).result("OK");
        });

        // Configuration Routes
        app.get("/api/admin/config", ctx -> {
            ctx.header("Cache-Control", "no-cache, no-store, must-revalidate");
            ctx.json(new ConfigResponse(
                plugin.getConfigManager().getConfig("modules/economy.yml").getDouble("shop.inflation_exponent", 0.5),
                plugin.getConfigManager().getConfig("modules/economy.yml").getDouble("ah.tax_percentage", 0.0),
                plugin.getConfigManager().getConfig("modules/web.yml").getString("admin-password", "gens"),
                plugin.getConfigManager().getConfig("modules/headdrop.yml").getDouble("headdrop.chance", 10.0),
                plugin.getConfigManager().getConfig("modules/quests.yml").getInt("quests.max_rerolls_per_day", 3),
                plugin.getConfigManager().getConfig("modules/lootr.yml").getBoolean("lootr.prevent-break", false),
                plugin.getConfigManager().getConfig("modules/lootr.yml").getBoolean("lootr.prevent-hopper", true),
                plugin.getConfigManager().getConfig("modules/lootr.yml").getBoolean("lootr.particles-enabled", true),
                plugin.getConfigManager().getConfig("modules/motd.yml").getString("motd.line1", "<dark_aqua><bold>Le Serveur Des Gens Bien"),
                plugin.getConfigManager().getConfig("modules/motd.yml").getString("motd.line2", "<gray><bold>>> <yellow>Saison 4 <gray><bold>- <aqua>discord.gg/gensbien"),
                plugin.getConfigManager().getConfig("modules/minigames.yml").getBoolean("minigames.wheel.enabled", true),
                plugin.getConfigManager().getConfig("modules/minigames.yml").getBoolean("minigames.casino.enabled", true),
                plugin.getConfigManager().getConfig("modules/web.yml").getString("web.public_features_text", defaultPublicText),
                plugin.getConfigManager().getConfig("modules/bluemap.yml").getString("bluemap.url", "http://localhost:8100"),
                plugin.getConfigManager().getConfig("modules/web.yml").getString("web.server_ip", "gens-core.duckdns.org"),
                plugin.getConfigManager().getConfig("modules/tomb.yml").getString("modules.tomb.block_type", "CHEST"),
                plugin.getConfigManager().getConfig("modules/tomb.yml").getBoolean("modules.tomb.store_xp", true),
                plugin.getConfigManager().getConfig("modules/tomb.yml").getLong("modules.tomb.expiration_time_seconds", 3600),
                plugin.getConfigManager().getConfig("modules/tomb.yml").getString("modules.tomb.expiration_action", "UNLOCK"),
                plugin.getConfigManager().getConfig("modules/tomb.yml").getString("modules.tomb.default_access", "OWNER_ONLY")
            ));
        });

        app.post("/api/admin/config", ctx -> {
            ConfigRequest req = ctx.bodyAsClass(ConfigRequest.class);
            plugin.getConfigManager().getConfig("modules/economy.yml").set("shop.inflation_exponent", req.inflationExponent);
            plugin.getConfigManager().getConfig("modules/economy.yml").set("ah.tax_percentage", req.ahTaxPercentage);
            plugin.getConfigManager().getConfig("modules/headdrop.yml").set("headdrop.chance", req.headDropChance);
            plugin.getConfigManager().getConfig("modules/quests.yml").set("quests.max_rerolls_per_day", req.maxQuestsRerolls);
            plugin.getConfigManager().getConfig("modules/lootr.yml").set("lootr.prevent-break", req.lootrPreventBreak);
            plugin.getConfigManager().getConfig("modules/lootr.yml").set("lootr.prevent-hopper", req.lootrPreventHopper);
            plugin.getConfigManager().getConfig("modules/lootr.yml").set("lootr.particles-enabled", req.lootrParticles);
            plugin.getConfigManager().getConfig("modules/motd.yml").set("motd.line1", req.motdLine1);
            plugin.getConfigManager().getConfig("modules/motd.yml").set("motd.line2", req.motdLine2);
            if (req.bluemapUrl != null) plugin.getConfigManager().getConfig("modules/bluemap.yml").set("bluemap.url", req.bluemapUrl);
            if (req.serverIp != null) plugin.getConfigManager().getConfig("modules/web.yml").set("web.server_ip", req.serverIp);
            
            if (req.tombBlockType != null) plugin.getConfigManager().getConfig("modules/tomb.yml").set("modules.tomb.block_type", req.tombBlockType);
            plugin.getConfigManager().getConfig("modules/tomb.yml").set("modules.tomb.store_xp", req.tombStoreXp);
            plugin.getConfigManager().getConfig("modules/tomb.yml").set("modules.tomb.expiration_time_seconds", req.tombExpirationSeconds);
            if (req.tombExpirationAction != null) plugin.getConfigManager().getConfig("modules/tomb.yml").set("modules.tomb.expiration_action", req.tombExpirationAction);
            if (req.tombDefaultAccess != null) plugin.getConfigManager().getConfig("modules/tomb.yml").set("modules.tomb.default_access", req.tombDefaultAccess);
            
            plugin.getConfigManager().saveConfig("modules/economy.yml");
            plugin.getConfigManager().saveConfig("modules/headdrop.yml");
            plugin.getConfigManager().saveConfig("modules/quests.yml");
            plugin.getConfigManager().saveConfig("modules/lootr.yml");
            plugin.getConfigManager().saveConfig("modules/motd.yml");
            plugin.getConfigManager().saveConfig("modules/bluemap.yml");
            plugin.getConfigManager().saveConfig("modules/tomb.yml");
            plugin.saveConfig();
            
            plugin.getConfigManager().getConfig("modules/web.yml").set("admin-password", req.adminPassword);
            plugin.getConfigManager().getConfig("modules/minigames.yml").set("minigames.wheel.enabled", req.minigameWheelEnabled);
            plugin.getConfigManager().getConfig("modules/minigames.yml").set("minigames.casino.enabled", req.minigameCasinoEnabled);
            if (req.publicFeaturesText != null) {
                plugin.getConfigManager().getConfig("modules/web.yml").set("web.public_features_text", req.publicFeaturesText);
            }
            plugin.getConfigManager().saveConfig("modules/web.yml");
            plugin.getConfigManager().saveConfig("modules/minigames.yml");
            
            plugin.getLangManager().sendConsoleMessage("webmanager.log_3");
            
            HeadDropModule hd = (HeadDropModule) plugin.getModuleManager().getModule("headdrop");
            if (hd != null) {
                hd.setDropChance(req.headDropChance);
            }
            
            fr.gens.core.modules.loot.LootModule loot = (fr.gens.core.modules.loot.LootModule) plugin.getModuleManager().getModule("lootr");
            if (loot != null) {
                loot.loadConfig();
            }
            
            ctx.status(200).result("OK");
        });

        // Modération Joueurs
        app.get("/api/admin/players", ctx -> {
            List<Map<String, Object>> players = new ArrayList<>();
            fr.gens.core.modules.moderation.ModerationModule mod = (fr.gens.core.modules.moderation.ModerationModule) plugin.getModuleManager().getModule("Moderation");
            for (org.bukkit.OfflinePlayer op : plugin.getServer().getOfflinePlayers()) {
                if (op.getName() == null) continue; // Skip invalid
                Map<String, Object> map = new HashMap<>();
                map.put("name", op.getName());
                map.put("uuid", op.getUniqueId().toString());
                
                org.bukkit.entity.Player p = op.getPlayer();
                if (p != null && p.isOnline()) {
                    map.put("online", true);
                    map.put("ping", p.getPing());
                    map.put("health", p.getHealth());
                    map.put("maxHealth", p.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue());
                } else {
                    map.put("online", false);
                    map.put("ping", 0);
                    map.put("health", 0);
                    map.put("maxHealth", 20.0);
                }
                
                map.put("isBanned", plugin.getServer().getBanList(org.bukkit.BanList.Type.NAME).isBanned(op.getName()));
                map.put("isMuted", mod != null && mod.isMuted(op.getUniqueId()));

                long playtime = plugin.getDatabaseManager().getPlaytimeMinutes(op.getUniqueId());
                map.put("playtime", playtime);
                players.add(map);
            }
            ctx.json(players);
        });

        app.post("/api/admin/players/action", ctx -> {
            PlayerActionRequest req = ctx.bodyAsClass(PlayerActionRequest.class);
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                DiscordModule discord = (DiscordModule) plugin.getModuleManager().getModule("Discord");
                fr.gens.core.modules.moderation.ModerationModule mod = (fr.gens.core.modules.moderation.ModerationModule) plugin.getModuleManager().getModule("Moderation");
                org.bukkit.OfflinePlayer targetOffline = plugin.getServer().getOfflinePlayer(req.playerName);
                org.bukkit.entity.Player target = targetOffline.getPlayer();

                if ("kick".equalsIgnoreCase(req.action)) {
                    if (target != null) {
                        target.kickPlayer("<red>Vous avez été expulsé par un Administrateur.\n<gray>Raison : " + (req.reason != null ? req.reason : "Aucune raison"));
                        plugin.getLogger().info("Web panel kicked " + req.playerName);
                        if (discord != null && discord.isEnabled()) discord.sendBotLogEmbed("KICK", "Joueur : " + req.playerName + "\nAdmin : WebAdmin\nRaison : " + req.reason, Color.ORANGE);
                    }
                } else if ("ban".equalsIgnoreCase(req.action)) {
                    long durationMs = 0;
                    if (req.durationHours > 0) durationMs = req.durationHours * 3600000L;
                    else if (req.durationDays > 0) durationMs = req.durationDays * 86400000L;
                    
                    java.util.Date expires = durationMs > 0 ? new java.util.Date(System.currentTimeMillis() + durationMs) : null;
                    String reason = req.reason != null && !req.reason.isEmpty() ? req.reason : "Banni par un Administrateur";
                    
                    plugin.getServer().getBanList(org.bukkit.BanList.Type.NAME).addBan(req.playerName, "<red>" + reason, expires, "WebAdmin");
                    if (target != null) {
                        target.kickPlayer("<red>Vous avez été banni.\n<gray>Raison : " + reason);
                    }
                    plugin.getLogger().info("Web panel banned " + req.playerName);
                    if (discord != null && discord.isEnabled()) discord.sendBotLogEmbed("BAN", "Joueur : " + req.playerName + "\nAdmin : WebAdmin\nRaison : " + reason, Color.RED);
                } else if ("unban".equalsIgnoreCase(req.action)) {
                    plugin.getServer().getBanList(org.bukkit.BanList.Type.NAME).pardon(req.playerName);
                    if (discord != null && discord.isEnabled()) discord.sendBotLogEmbed("UNBAN", "Joueur : " + req.playerName + "\nAdmin : WebAdmin", Color.GREEN);
                } else if ("mute".equalsIgnoreCase(req.action)) {
                    if (mod != null && targetOffline != null && targetOffline.getUniqueId() != null) {
                        long durationMs = 0;
                        if (req.durationHours > 0) durationMs = req.durationHours * 3600000L;
                        else if (req.durationDays > 0) durationMs = req.durationDays * 86400000L;
                        String reason = req.reason != null && !req.reason.isEmpty() ? req.reason : "Aucune raison";
                        mod.mutePlayer(targetOffline.getUniqueId(), reason, durationMs);
                        
                        if (target != null) {
                            target.sendMessage("<red><bold>Vous avez été rendu muet par le WebAdmin ! Raison : " + reason);
                        }
                        if (discord != null && discord.isEnabled()) discord.sendBotLogEmbed("MUTE", "Joueur : " + req.playerName + "\nAdmin : WebAdmin\nRaison : " + reason, Color.YELLOW);
                    }
                } else if ("unmute".equalsIgnoreCase(req.action)) {
                    if (mod != null && targetOffline != null && targetOffline.getUniqueId() != null) {
                        mod.unmutePlayer(targetOffline.getUniqueId());
                        if (target != null) {
                            plugin.getLangManager().sendMessage(target, "webmanager.msg_1");
                        }
                        if (discord != null && discord.isEnabled()) discord.sendBotLogEmbed("UNMUTE", "Joueur : " + req.playerName + "\nAdmin : WebAdmin", Color.GREEN);
                    }
                } else if ("message".equalsIgnoreCase(req.action)) {
                    if (target != null) {
                        target.sendMessage("<dark_gray>[<red>WebAdmin<dark_gray>] <gray>" + req.reason);
                    }
                }
            });
            ctx.status(200).json("Action effectuée");
        });

        // Édition de fichiers
        app.get("/api/admin/file", ctx -> {
            String path = ctx.queryParam("path");
            if (path == null || path.contains("..")) {
                ctx.status(400).result("Invalid path");
                return;
            }
            File file = new File(plugin.getDataFolder(), path);
            if (!file.exists()) {
                ctx.status(404).result("File not found");
                return;
            }
            String content = Files.readString(file.toPath(), StandardCharsets.UTF_8);
            ctx.result(content);
        });

        app.post("/api/admin/file", ctx -> {
            String path = ctx.queryParam("path");
            if (path == null || path.contains("..")) {
                ctx.status(400).result("Invalid path");
                return;
            }
            File file = new File(plugin.getDataFolder(), path);
            FileEditRequest req = ctx.bodyAsClass(FileEditRequest.class);
            Files.writeString(file.toPath(), req.content, StandardCharsets.UTF_8);
            
            // Reload configuration if it's config.yml
            if (path.equals("config.yml")) {
                plugin.reloadConfig();
                plugin.getLangManager().sendConsoleMessage("webmanager.log_4");
            }
            
            ctx.status(200).result("OK");
        });

        // ==========================================
        // ROUTES SHOP & ECONOMIE
        // ==========================================

        app.get("/api/economy/stats", ctx -> {
            EconomyModule eco = (EconomyModule) plugin.getModuleManager().getModule("economy");
            if (eco != null) {
                // Pour simplifier, on ne peut pas iterer directement sur les UUIDs sans reflection si balances est privé
                // Je vais utiliser un raccourci ou demander au module
                // TODO: Ajouter une methode getTotalMoney dans EconomyModule
                ctx.json(Map.of("status", "ok", "message", "Endpoint economie a implementer"));
            } else {
                ctx.status(404).json("Module economie desactive");
            }
        });

        app.get("/api/shop/categories", ctx -> {
            ShopModule shop = (ShopModule) plugin.getModuleManager().getModule("dynamicshop");
            if (shop != null) {
                ctx.json(shop.getCategories());
            } else {
                ctx.status(404).json("Shop desactive");
            }
        });

        app.get("/api/shop/history/{material}", ctx -> {
            ShopModule shop = (ShopModule) plugin.getModuleManager().getModule("dynamicshop");
            if (shop != null) {
                String material = ctx.pathParam("material").toUpperCase();
                ctx.json(shop.getHistory(material));
            } else {
                ctx.status(404).json("Shop desactive");
            }
        });

        app.post("/api/admin/shop/category", ctx -> {
            ShopModule shop = (ShopModule) plugin.getModuleManager().getModule("dynamicshop");
            if (shop == null) { ctx.status(404).json("Shop desactive"); return; }
            
            ShopCategory request = ctx.bodyAsClass(ShopCategory.class);
            ShopCategory existing = shop.getCategory(request.getId());
            if (existing != null) {
                existing.setDisplayName(request.getDisplayName());
                existing.setIcon(request.getIcon());
            } else {
                shop.getCategories().add(request);
            }
            shop.saveShop();
            ctx.status(200).result("OK");
        });

        app.post("/api/admin/shop/item", ctx -> {
            ShopModule shop = (ShopModule) plugin.getModuleManager().getModule("dynamicshop");
            if (shop == null) { ctx.status(404).json("Shop desactive"); return; }
            
            ItemRequest req = ctx.bodyAsClass(ItemRequest.class);
            ShopCategory cat = shop.getCategory(req.categoryId);
            if (cat != null) {
                ShopItem item = cat.getItem(Material.valueOf(req.material));
                if (item != null) {
                    item.setBaseBuyPrice(req.baseBuyPrice);
                    item.setBaseSellPrice(req.baseSellPrice);
                    item.setTargetStock(req.targetStock);
                    item.setCommand(req.isCommand);
                    if (req.isCommand && req.commandToExecute != null) {
                        item.setCommandToExecute(req.commandToExecute);
                    }
                    item.setEnabled(req.isEnabled);
                } else {
                    item = new ShopItem(Material.valueOf(req.material), req.baseBuyPrice, req.baseSellPrice);
                    item.setTargetStock(req.targetStock);
                    item.setCommand(req.isCommand);
                    if (req.isCommand && req.commandToExecute != null) {
                        item.setCommandToExecute(req.commandToExecute);
                    }
                    item.setEnabled(req.isEnabled);
                    cat.addItem(item);
                }
                shop.saveShop();
                ctx.status(200).result("OK");
            } else {
                ctx.status(404).json("Categorie introuvable");
            }
        });

        app.delete("/api/admin/shop/item/{category}/{material}", ctx -> {
            ShopModule shop = (ShopModule) plugin.getModuleManager().getModule("dynamicshop");
            if (shop == null) { ctx.status(404).json("Shop desactive"); return; }
            
            String catId = ctx.pathParam("category");
            String matName = ctx.pathParam("material").toUpperCase();
            
            ShopCategory cat = shop.getCategory(catId);
            if (cat != null) {
                try {
                    Material mat = Material.valueOf(matName);
                    cat.removeItem(mat);
                    // Suppression SQL
                    shop.deleteItem(catId, mat.name());
                    shop.saveShop();
                    ctx.status(200).result("OK");
                } catch (IllegalArgumentException e) {
                    ctx.status(400).json("Materiel invalide");
                }
            } else {
                ctx.status(404).json("Categorie introuvable");
            }
        });

        app.delete("/api/admin/shop/category/{id}", ctx -> {
            ShopModule shop = (ShopModule) plugin.getModuleManager().getModule("dynamicshop");
            if (shop == null) { ctx.status(404).json("Shop desactive"); return; }
            
            String catId = ctx.pathParam("id");
            ShopCategory cat = shop.getCategory(catId);
            
            if (cat != null) {
                shop.getCategories().remove(cat);
                shop.deleteCategory(catId);
                shop.saveShop();
                ctx.status(200).result("OK");
            } else {
                ctx.status(404).json("Categorie introuvable");
            }
        });

        app.delete("/api/admin/homes", ctx -> {
            fr.gens.core.modules.TeleportHomeModule homeModule = (fr.gens.core.modules.TeleportHomeModule) plugin.getModuleManager().getModule("CmdHome");
            if (homeModule != null) {
                homeModule.clearAllHomes();
                ctx.status(200).result("OK");
            } else {
                ctx.status(404).json("CmdHome module disabled");
            }
        });

        // ==============================================
        // Teams Stats API
        // ==============================================
        app.get("/api/stats/teams/best", ctx -> {
            fr.gens.core.modules.teams.TeamManager tm = plugin.getTeamManager();
            if (tm != null) {
                java.util.Map<String, Object> bestTeam = tm.getBestTeamStats();
                if (bestTeam != null) {
                    ctx.json(bestTeam);
                } else {
                    ctx.status(404).result("Aucune guilde trouvée.");
                }
            } else {
                ctx.status(404).result("Team manager non trouvé.");
            }
        });

        app.get("/api/stats/teams", ctx -> {
            fr.gens.core.modules.teams.TeamManager tm = plugin.getTeamManager();
            if (tm != null) {
                ctx.json(tm.getAllTeamStats());
            } else {
                ctx.status(404).result("Team manager non trouvé.");
            }
        });

        // ==========================================
        // ROUTES AUCTION HOUSE (AH)
        // ==========================================
        app.get("/api/ah/items", ctx -> {
            fr.gens.core.modules.AuctionHouseModule ah = (fr.gens.core.modules.AuctionHouseModule) plugin.getModuleManager().getModule("auctionhouse");
            if (ah != null) {
                ctx.json(ah.getAuctionItemsForWeb());
            } else {
                ctx.status(404).result("AH module not found");
            }
        });

          // ==========================================
          // ROUTE PAGE PUBLIQUE
          // ==========================================
          app.get("/api/public/news", ctx -> {
              ctx.result(plugin.getConfig().getString("publicPageContent", "Bienvenue sur notre serveur !\nNous sommes heureux de vous accueillir."));
          });

          app.get("/api/public/bluemap", ctx -> {
              ctx.result(plugin.getConfigManager().getConfig("modules/bluemap.yml").getString("bluemap.url", "http://localhost:8100"));
          });

          app.get("/api/public/server_ip", ctx -> {
              ctx.result(plugin.getConfigManager().getConfig("modules/web.yml").getString("web.server_ip", "gens-core.duckdns.org"));
          });

        // ==========================================
        // ROUTES LEADERBOARD
        // ==========================================
        app.get("/api/stats/leaderboard", ctx -> {
            ctx.json(plugin.getDatabaseManager().getGlobalLeaderboard());
        });

        app.get("/api/stats/quests/leaderboard", ctx -> {
            ctx.json(plugin.getDatabaseManager().getQuestsLeaderboardData());
        });

        app.get("/api/stats/jobs", ctx -> {
            ctx.json(plugin.getDatabaseManager().getJobsLeaderboardData());
        });
    }
    
    // Classe DTO pour parser le JSON envoyé par React
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
    public static class PlayerActionRequest {
        public String action; // kick, ban, mute, message
        public String playerName;
        public String reason;
        public int durationHours;
        public int durationDays;
    }

    @com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
    public static class ToggleRequest {
        public boolean state;
    }

    @com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
    public static class ItemRequest {
        public String categoryId;
        public String material;
        public double baseBuyPrice;
        public double baseSellPrice;
        public int targetStock;
        public boolean isCommand;
        public String commandToExecute;
        public boolean isEnabled = true;
    }
}
