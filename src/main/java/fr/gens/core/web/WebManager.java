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

    public WebManager(CorePlugin plugin, int port) {
        this.plugin = plugin;
        this.port = port;
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
                        plugin.getLogger().info("Fichiers web extraits dans " + webDir.getPath());
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().severe("Erreur lors de l'extraction des fichiers web : " + e.getMessage());
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
                String expectedPassword = plugin.getStorageManager().getConfig().getString("admin-password", "gens");
                
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
        String defaultPublicText = "Bienvenue aventurier sur la toute nouvelle interface du serveur Gens !\n\n" +
                "- Système de Métiers (/jobs) pour bosser comme un forcené.\n" +
                "- Hôtel des Ventes (/ah) pour vos transactions.\n" +
                "- Map en direct via BlueMap (attention on te voit).\n" +
                "- Création de Guildes (/team) avec des Quêtes Hebdomadaires en coop !\n" +
                "- Verrous de coffres (/lock) pour sécuriser vos items ou les partager avec votre guilde.\n" +
                "- Serveur Discord relié en direct au chat du jeu (avec affichage des Grades et Guildes).\n" +
                "- Quêtes solos et classements en ligne pour flexer.\n\n" +
                "NOUVEAU SYSTÈME DE GESTION\n" +
                "Le plugin est continuellement mis à jour avec de nouvelles fonctionnalités. Rejoignez-nous pour découvrir la suite !";

        app.get("/api/public/features", ctx -> {
            ctx.header("Cache-Control", "no-cache, no-store, must-revalidate");
            ctx.result(plugin.getStorageManager().getConfig().getString("web.public_features_text", defaultPublicText));
        });

        // API de Langue
        app.get("/api/public/lang", ctx -> {
            ctx.header("Cache-Control", "no-cache, no-store, must-revalidate");
            String lang = ctx.queryParam("lang");
            if (lang == null) lang = plugin.getConfig().getString("lang", "fr_FR");
            
            java.io.File langFile = new java.io.File(plugin.getDataFolder() + java.io.File.separator + "lang", lang + ".yml");
            if (!langFile.exists()) {
                langFile = new java.io.File(plugin.getDataFolder() + java.io.File.separator + "lang", "fr_FR.yml");
            }
            if (langFile.exists()) {
                org.bukkit.configuration.file.FileConfiguration langConfig = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(langFile);
                ctx.json(langConfig.getValues(true)); // Transforme les clés YAML en JSON "a.b": "value"
            } else {
                ctx.status(404).json("Language file not found");
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
                    plugin.getLogger().info("Le web panel a changé l'état du module " + moduleName + " vers " + request.state);
                }
            });
            
            ctx.status(200).result("OK");
        });

        // Configuration Routes
        app.get("/api/admin/config", ctx -> {
            ctx.header("Cache-Control", "no-cache, no-store, must-revalidate");
            ctx.json(new ConfigResponse(
                plugin.getConfig().getDouble("shop.inflation_exponent", 0.5),
                plugin.getConfig().getDouble("ah.tax_percentage", 0.0),
                plugin.getStorageManager().getConfig().getString("admin-password", "gens"),
                plugin.getConfig().getDouble("headdrop.chance", 10.0),
                plugin.getConfig().getInt("quests.max_rerolls_per_day", 3),
                plugin.getConfig().getBoolean("lootr.prevent-break", false),
                plugin.getConfig().getBoolean("lootr.prevent-hopper", true),
                plugin.getConfig().getBoolean("lootr.particles-enabled", true),
                plugin.getConfig().getString("motd.line1", "&3&lLe Serveur Des Gens Bien"),
                plugin.getConfig().getString("motd.line2", "&7&l>> &eSaison 4 &7&l- &bdiscord.gg/gensbien"),
                plugin.getStorageManager().getConfig().getBoolean("minigames.wheel.enabled", true),
                plugin.getStorageManager().getConfig().getBoolean("minigames.casino.enabled", true),
                plugin.getStorageManager().getConfig().getString("web.public_features_text", defaultPublicText),
                plugin.getConfig().getString("bluemap.url", "http://localhost:8100"),
                plugin.getConfig().getString("web.server_ip", "gens-core.duckdns.org"),
                plugin.getConfig().getString("modules.tomb.block_type", "CHEST"),
                plugin.getConfig().getBoolean("modules.tomb.store_xp", true),
                plugin.getConfig().getLong("modules.tomb.expiration_time_seconds", 3600),
                plugin.getConfig().getString("modules.tomb.expiration_action", "UNLOCK"),
                plugin.getConfig().getString("modules.tomb.default_access", "OWNER_ONLY")
            ));
        });

        app.post("/api/admin/config", ctx -> {
            ConfigRequest req = ctx.bodyAsClass(ConfigRequest.class);
            plugin.getConfig().set("shop.inflation_exponent", req.inflationExponent);
            plugin.getConfig().set("ah.tax_percentage", req.ahTaxPercentage);
            plugin.getConfig().set("headdrop.chance", req.headDropChance);
            plugin.getConfig().set("quests.max_rerolls_per_day", req.maxQuestsRerolls);
            plugin.getConfig().set("lootr.prevent-break", req.lootrPreventBreak);
            plugin.getConfig().set("lootr.prevent-hopper", req.lootrPreventHopper);
            plugin.getConfig().set("lootr.particles-enabled", req.lootrParticles);
            plugin.getConfig().set("motd.line1", req.motdLine1);
            plugin.getConfig().set("motd.line2", req.motdLine2);
            if (req.bluemapUrl != null) plugin.getConfig().set("bluemap.url", req.bluemapUrl);
            if (req.serverIp != null) plugin.getConfig().set("web.server_ip", req.serverIp);
            
            if (req.tombBlockType != null) plugin.getConfig().set("modules.tomb.block_type", req.tombBlockType);
            plugin.getConfig().set("modules.tomb.store_xp", req.tombStoreXp);
            plugin.getConfig().set("modules.tomb.expiration_time_seconds", req.tombExpirationSeconds);
            if (req.tombExpirationAction != null) plugin.getConfig().set("modules.tomb.expiration_action", req.tombExpirationAction);
            if (req.tombDefaultAccess != null) plugin.getConfig().set("modules.tomb.default_access", req.tombDefaultAccess);
            
            plugin.saveConfig();
            
            plugin.getStorageManager().getConfig().set("admin-password", req.adminPassword);
            plugin.getStorageManager().getConfig().set("minigames.wheel.enabled", req.minigameWheelEnabled);
            plugin.getStorageManager().getConfig().set("minigames.casino.enabled", req.minigameCasinoEnabled);
            if (req.publicFeaturesText != null) {
                plugin.getStorageManager().getConfig().set("web.public_features_text", req.publicFeaturesText);
            }
            plugin.getStorageManager().saveConfig();
            
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
                        target.kickPlayer("§cVous avez été expulsé par un Administrateur.\n§7Raison : " + (req.reason != null ? req.reason : "Aucune raison"));
                        plugin.getLogger().info("Le web panel a kick " + req.playerName);
                        if (discord != null && discord.isEnabled()) discord.sendBotLogEmbed("KICK", "Joueur : " + req.playerName + "\nAdmin : WebAdmin\nRaison : " + req.reason, Color.ORANGE);
                    }
                } else if ("ban".equalsIgnoreCase(req.action)) {
                    long durationMs = 0;
                    if (req.durationHours > 0) durationMs = req.durationHours * 3600000L;
                    else if (req.durationDays > 0) durationMs = req.durationDays * 86400000L;
                    
                    java.util.Date expires = durationMs > 0 ? new java.util.Date(System.currentTimeMillis() + durationMs) : null;
                    String reason = req.reason != null && !req.reason.isEmpty() ? req.reason : "Banni par un Administrateur";
                    
                    plugin.getServer().getBanList(org.bukkit.BanList.Type.NAME).addBan(req.playerName, "§c" + reason, expires, "WebAdmin");
                    if (target != null) {
                        target.kickPlayer("§cVous avez été banni.\n§7Raison : " + reason);
                    }
                    plugin.getLogger().info("Le web panel a banni " + req.playerName);
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
                            target.sendMessage("§c§lVous avez été rendu muet par le WebAdmin ! Raison : " + reason);
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
                        target.sendMessage("§8[§cWebAdmin§8] §7" + req.reason);
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
                ctx.status(404).json("Module CmdHome désactivé");
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
              ctx.result(plugin.getConfig().getString("bluemap.url", "http://localhost:8100"));
          });

          app.get("/api/public/server_ip", ctx -> {
              ctx.result(plugin.getConfig().getString("web.server_ip", "gens-core.duckdns.org"));
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
