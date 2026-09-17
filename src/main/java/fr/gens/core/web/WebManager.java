package fr.gens.core.web;

import fr.gens.core.CorePlugin;
import fr.gens.core.modules.Module;
import io.javalin.Javalin;
import io.javalin.http.staticfiles.Location;
import static io.javalin.apibuilder.ApiBuilder.*;
import fr.gens.core.modules.EconomyModule;
import fr.gens.core.modules.shop.ShopCategory;
import fr.gens.core.modules.shop.ShopItem;
import fr.gens.core.modules.shop.ShopModule;
import fr.gens.core.modules.headdrop.HeadDropModule;
import fr.gens.core.modules.discord.DiscordModule;
import org.bukkit.Material;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.ban.ProfileBanList;
import io.papermc.paper.ban.BanListType;
import java.awt.Color;
import fr.gens.core.utils.PlaceholderUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import java.io.File;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;


public class WebManager {

    private final CorePlugin plugin;
    private final int port;
    private Javalin app;
    private final java.util.Map<String, Long> activeSessions = new java.util.concurrent.ConcurrentHashMap<>();
    public final java.util.Map<String, String> activePlayerSessions = new java.util.concurrent.ConcurrentHashMap<>();
    private final java.util.Map<String, Integer> loginRateLimit = new java.util.concurrent.ConcurrentHashMap<>();
    private final java.util.Map<String, Long> rateLimitReset = new java.util.concurrent.ConcurrentHashMap<>();
    public final java.util.Map<String, Integer> playerLoginRateLimit = new java.util.concurrent.ConcurrentHashMap<>();
    public final java.util.Map<String, Long> playerRateLimitReset = new java.util.concurrent.ConcurrentHashMap<>();
    public final java.util.Map<String, Long> playerSessionExpiry = new java.util.concurrent.ConcurrentHashMap<>();
    private com.tcoded.folialib.wrapper.task.WrappedTask sessionCleanupTask;
    private final fr.gens.core.database.WebDAO webDAO;

    public WebManager(CorePlugin plugin, int port) {
        this.plugin = plugin;
        this.port = port;
        
        this.webDAO = new fr.gens.core.database.WebDAO(plugin);
        this.webDAO.initDatabase();
    }

    public String getPlayerUuidFromCtx(io.javalin.http.Context ctx) {
        String sessionUuid = ctx.attribute("playerUuid");
        if (sessionUuid != null) return sessionUuid;
        String auth = ctx.header("Authorization");
        if (auth != null && auth.startsWith("Bearer ")) {
            String token = auth.substring(7);
            if (activePlayerSessions.containsKey(token)) {
                Long exp = playerSessionExpiry.get(token);
                if (exp == null || exp >= System.currentTimeMillis()) {
                    return activePlayerSessions.get(token);
                }
            }
        }
        return null;
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
        // Hachage du mot de passe admin si nécessaire
        org.bukkit.configuration.file.FileConfiguration webConfig = plugin.getConfigManager().getConfig("modules/web.yml");
        String adminPassword = webConfig.getString("admin-password", "gens");
        if (adminPassword != null && !adminPassword.startsWith("$2a$") && !adminPassword.startsWith("$2b$")) {
            String hashed = org.mindrot.jbcrypt.BCrypt.hashpw(adminPassword, org.mindrot.jbcrypt.BCrypt.gensalt());
            webConfig.set("admin-password", hashed);
            plugin.getConfigManager().saveConfigAsync("modules/web.yml");
            plugin.getLogger().info("The default web admin password was in plain text. It has been hashed for security.");
        }

        // Extraction des fichiers web s'ils n'existent pas ou si index.html manque
        File webDir = new File(plugin.getDataFolder(), "web");
        File indexFile = new File(webDir, "index.html");
        
        boolean autoUpdate = webConfig.getBoolean("web.auto_update_panel", true);
        
        if (!webDir.exists() || !indexFile.exists() || autoUpdate) {
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
                        if (autoUpdate) {
                            webConfig.set("web.auto_update_panel", false);
                            plugin.getConfigManager().saveConfigAsync("modules/web.yml");
                            plugin.getLogger().info("auto_update_panel has been set to false to prevent overwriting custom changes on next restart.");
                        }
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
                        String allowedOrigin = plugin.getConfigManager().getConfig("modules/web.yml").getString("web.allowed_origin", "");
                        if (allowedOrigin != null && !allowedOrigin.isEmpty()) {
                            it.allowHost(allowedOrigin);
                        } else {
                            it.anyHost();
                        }
                    });
                });
                
                // Servir le site web depuis le dossier plugins/GensCore/web/
                config.staticFiles.add(webDir.getAbsolutePath(), Location.EXTERNAL);

                // Gérer le routage SPA (Single Page Application)
                config.spaRoot.addFile("/", new File(webDir, "index.html").getAbsolutePath(), Location.EXTERNAL);

                config.routes.apiBuilder(() -> {
                    // Middleware d'authentification pour les routes /api/admin/*
                    before("/api/admin/*", ctx -> {
                        if (ctx.path().equals("/api/admin/login")) return; // skip for login
                        
                        String authHeader = ctx.header("Authorization");
                        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                            ctx.status(401).json(Map.of("error", "Unauthorized", "message", "En-tête d'autorisation manquant ou invalide"));
                            ctx.skipRemainingHandlers();
                            return;
                        }
                        String token = authHeader.substring(7);
                        if (!activeSessions.containsKey(token) || activeSessions.get(token) < System.currentTimeMillis()) {
                            activeSessions.remove(token);
                            webDAO.removeAdminSession(token);
                            ctx.status(401).json(Map.of("error", "Unauthorized", "message", "Session expirée, veuillez vous reconnecter"));
                            ctx.skipRemainingHandlers();
                            return;
                        }
                    });

                    // Middleware d'authentification pour les jeux
                    before("/api/games/*", ctx -> {
                        if (ctx.path().equals("/api/games/config") || ctx.path().equals("/api/games/wheel") || ctx.path().equals("/api/games/casino/inventory")) return; // public routes
                        
                        String authHeader = ctx.header("Authorization");
                        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                            ctx.status(401).json(Map.of("error", "Unauthorized"));
                            ctx.skipRemainingHandlers();
                            return;
                        }
                        String token = authHeader.substring(7);
                        if (!activePlayerSessions.containsKey(token) || (playerSessionExpiry.containsKey(token) && playerSessionExpiry.get(token) < System.currentTimeMillis())) {
                            activePlayerSessions.remove(token);
                            playerSessionExpiry.remove(token);
                            webDAO.removePlayerSession(token);
                            ctx.status(401).json(Map.of("error", "Session expired"));
                            ctx.skipRemainingHandlers();
                            return;
                        }
                        ctx.attribute("playerUuid", activePlayerSessions.get(token));
                    });

                    post("/api/admin/login", ctx -> {
                        String ip = ctx.ip();
                        long nowTime = System.currentTimeMillis();
                        if (rateLimitReset.getOrDefault(ip, 0L) < nowTime) {
                            loginRateLimit.remove(ip);
                            rateLimitReset.put(ip, nowTime + 60000L); // 1 minute
                        }
                        int attempts = loginRateLimit.getOrDefault(ip, 0);
                        if (attempts >= 5) {
                            ctx.status(429).json(Map.of("error", "Rate limit exceeded. Try again later."));
                            return;
                        }

                        LoginRequest req = ctx.bodyAsClass(LoginRequest.class);
                        String storedHash = plugin.getConfigManager().getConfig("modules/web.yml").getString("admin-password", "");
                        if (org.mindrot.jbcrypt.BCrypt.checkpw(req.password, storedHash)) {
                            loginRateLimit.remove(ip); // reset on success
                            String token = java.util.UUID.randomUUID().toString();
                            long expiry = System.currentTimeMillis() + (24L * 60 * 60 * 1000L); // 24 hours
                            activeSessions.put(token, expiry);
                            webDAO.saveAdminSession(token, expiry);
                            ctx.json(new LoginResponse(token));
                        } else {
                            loginRateLimit.put(ip, attempts + 1);
                            ctx.status(401).json(Map.of("error", "Unauthorized", "message", "Mot de passe administrateur incorrect"));
                        }
                    });
                    
                    // Charger les sessions persistantes
                    Map<String, Long> persistedAdmin = webDAO.loadValidAdminSessions();
                    activeSessions.putAll(persistedAdmin);

                    Map<String, Map.Entry<String, Long>> persisted = webDAO.loadValidSessions();
                    for (Map.Entry<String, Map.Entry<String, Long>> entry : persisted.entrySet()) {
                        activePlayerSessions.put(entry.getKey(), entry.getValue().getKey());
                        playerSessionExpiry.put(entry.getKey(), entry.getValue().getValue());
                    }

                    WebPlayerAPI playerAPI = new WebPlayerAPI(plugin, this);
                    playerAPI.registerRoutes();

                    setupRoutes();
                });

            }).start(port);
            
            // Nettoyage périodique des sessions web expirées (Toutes les heures = 72000 ticks)
            this.sessionCleanupTask = plugin.getFoliaLib().getScheduler().runTimerAsync(() -> {
                long now = System.currentTimeMillis();
                activeSessions.entrySet().removeIf(entry -> {
                    if (entry.getValue() < now) {
                        webDAO.removeAdminSession(entry.getKey());
                        return true;
                    }
                    return false;
                });
            }, 72000L, 72000L);
            
            
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
        public boolean minigameCoinflipEnabled;
        public String publicFeaturesText;
        public String bluemapUrl;
        public String serverIp;
        public String tombBlockType;
        public boolean tombStoreXp;
        public long tombExpirationSeconds;
        public String tombExpirationAction;
        public String tombDefaultAccess;

        public ConfigResponse(double inf, double ahTax, String pass, double hDrop, int qRerolls, boolean lootrPreventBreak, boolean lootrPreventHopper, boolean lootrParticles, String motdLine1, String motdLine2, boolean wheel, boolean casino, boolean coinflip, String publicFeaturesText, String bluemapUrl, String serverIp, String tombBlockType, boolean tombStoreXp, long tombExpirationSeconds, String tombExpirationAction, String tombDefaultAccess) {
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
            this.minigameCoinflipEnabled = coinflip;
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
        public boolean minigameCoinflipEnabled;
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
        if (sessionCleanupTask != null) {
            sessionCleanupTask.cancel();
            sessionCleanupTask = null;
        }
        if (app != null) {
            app.stop();
            plugin.getLangManager().sendConsoleMessage("webmanager.log_2");
        }
    }

    private void setupRoutes() {
        // Route API pour récupérer la liste des modules (Public)
        get("/api/modules", ctx -> {
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

        get("/api/public/features", ctx -> {
            ctx.header("Cache-Control", "no-cache, no-store, must-revalidate");
            ctx.result(plugin.getConfigManager().getConfig("modules/web.yml").getString("web.public_features_text", defaultPublicText));
        });

        // API de Langue
        get("/api/public/lang", ctx -> {
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
        post("/api/admin/modules/{name}/toggle", ctx -> {
            String moduleName = ctx.pathParam("name");
            
            // On récupère le body {"state": true/false}
            ToggleRequest request = ctx.bodyAsClass(ToggleRequest.class);
            
            // On exécute l'activation sur le thread principal de Bukkit (Très important !)
            plugin.getFoliaLib().getScheduler().runNextTick((t2) -> {
                boolean success = plugin.getModuleManager().toggleModule(moduleName, request.state);
                if(success) {
                    plugin.getLogger().info("Web panel changed state of module " + moduleName + " to " + request.state);
                }
            });
            
            ctx.status(200).result("OK");
        });

        // Configuration Routes
        get("/api/admin/config", ctx -> {
            ctx.header("Cache-Control", "no-cache, no-store, must-revalidate");
            ctx.json(new ConfigResponse(
                plugin.getConfigManager().getConfig("modules/economy.yml").getDouble("shop.inflation_exponent", 0.5),
                plugin.getConfigManager().getConfig("modules/economy.yml").getDouble("ah.tax_percentage", 0.0),
                "", // Masqué par sécurité pour ne pas exposer le hash BCrypt
                plugin.getConfigManager().getConfig("modules/headdrop.yml").getDouble("headdrop.chance", 10.0),
                plugin.getConfigManager().getConfig("modules/quests.yml").getInt("quests.max_rerolls_per_day", 3),
                plugin.getConfigManager().getConfig("modules/lootr.yml").getBoolean("lootr.prevent-break", false),
                plugin.getConfigManager().getConfig("modules/lootr.yml").getBoolean("lootr.prevent-hopper", true),
                plugin.getConfigManager().getConfig("modules/lootr.yml").getBoolean("lootr.particles-enabled", true),
                plugin.getConfigManager().getConfig("modules/motd.yml").getString("motd.line1", "<dark_aqua><bold>Le Serveur Des Gens Bien"),
                plugin.getConfigManager().getConfig("modules/motd.yml").getString("motd.line2", "<gray><bold>>> <yellow>Saison 4 <gray><bold>- <aqua>discord.gg/gensbien"),
                plugin.getConfigManager().getConfig("modules/minigames.yml").getBoolean("minigames.wheel.enabled", true),
                plugin.getConfigManager().getConfig("modules/minigames.yml").getBoolean("minigames.casino.enabled", true),
                plugin.getConfigManager().getConfig("modules/minigames.yml").getBoolean("minigames.coinflip.enabled", true),
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

        post("/api/admin/config", ctx -> {
            ConfigRequest req = ctx.bodyAsClass(ConfigRequest.class);
            
            plugin.getFoliaLib().getScheduler().runNextTick((t2) -> {
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
                
                plugin.getConfigManager().saveConfigAsync("modules/economy.yml");
                plugin.getConfigManager().saveConfigAsync("modules/headdrop.yml");
                plugin.getConfigManager().saveConfigAsync("modules/quests.yml");
                plugin.getConfigManager().saveConfigAsync("modules/lootr.yml");
                plugin.getConfigManager().saveConfigAsync("modules/motd.yml");
                plugin.getConfigManager().saveConfigAsync("modules/bluemap.yml");
                plugin.getConfigManager().saveConfigAsync("modules/tomb.yml");
                plugin.getConfigManager().saveMainConfigAsync();
                
                if (req.adminPassword != null && !req.adminPassword.trim().isEmpty() && !req.adminPassword.equals("********")) {
                    String hashedPass;
                    if (req.adminPassword.startsWith("$2a$") || req.adminPassword.startsWith("$2b$") || req.adminPassword.startsWith("$2y$")) {
                        hashedPass = req.adminPassword;
                    } else {
                        hashedPass = org.mindrot.jbcrypt.BCrypt.hashpw(req.adminPassword, org.mindrot.jbcrypt.BCrypt.gensalt());
                    }
                    plugin.getConfigManager().getConfig("modules/web.yml").set("admin-password", hashedPass);
                }
                plugin.getConfigManager().getConfig("modules/minigames.yml").set("minigames.wheel.enabled", req.minigameWheelEnabled);
                plugin.getConfigManager().getConfig("modules/minigames.yml").set("minigames.casino.enabled", req.minigameCasinoEnabled);
                plugin.getConfigManager().getConfig("modules/minigames.yml").set("minigames.coinflip.enabled", req.minigameCoinflipEnabled);
                if (req.publicFeaturesText != null) {
                    plugin.getConfigManager().getConfig("modules/web.yml").set("web.public_features_text", req.publicFeaturesText);
                }
                plugin.getConfigManager().saveConfigAsync("modules/web.yml");
                plugin.getConfigManager().saveConfigAsync("modules/minigames.yml");
                
                plugin.getLangManager().sendConsoleMessage("webmanager.log_3");
                
                HeadDropModule hd = (HeadDropModule) plugin.getModuleManager().getModule("headdrop");
                if (hd != null) {
                    hd.setDropChance(req.headDropChance);
                }
                
                fr.gens.core.modules.loot.LootModule loot = (fr.gens.core.modules.loot.LootModule) plugin.getModuleManager().getModule("lootr");
                if (loot != null) {
                    loot.loadConfig();
                }
            });
            
            ctx.status(200).result("OK");
        });

        // Modération Joueurs
        get("/api/admin/players", ctx -> {
            java.util.concurrent.CompletableFuture<List<Map<String, Object>>> future = new java.util.concurrent.CompletableFuture<>();
            
            // Requete SQL asynchrone hors du thread principal
            fr.gens.core.modules.stats.StatsModule statsModule = (fr.gens.core.modules.stats.StatsModule) plugin.getModuleManager().getModule("stats");
            List<Map<String, Object>> knownPlayers = (statsModule != null) ? statsModule.getStatsDAO().getAllKnownPlayers() : new ArrayList<>();
            
            plugin.getFoliaLib().getScheduler().runNextTick((t2) -> {
                List<Map<String, Object>> players = new ArrayList<>();
                fr.gens.core.modules.moderation.ModerationModule mod = (fr.gens.core.modules.moderation.ModerationModule) plugin.getModuleManager().getModule("Moderation");
                
                ProfileBanList banList = plugin.getServer().getBanList(BanListType.PROFILE);
                
                java.util.Set<java.util.UUID> addedUuids = new java.util.HashSet<>();
                for (Map<String, Object> known : knownPlayers) {
                    String uuidStr = (String) known.get("uuid");
                    String name = (String) known.get("name");
                    if (uuidStr == null || name == null) continue;
                    java.util.UUID uuid = java.util.UUID.fromString(uuidStr);
                    addedUuids.add(uuid);
                    
                    Map<String, Object> map = new HashMap<>();
                    map.put("name", name);
                    map.put("uuid", uuidStr);
                    
                    org.bukkit.entity.Player p = plugin.getServer().getPlayer(uuid);
                    if (p != null && p.isOnline()) {
                        map.put("online", true);
                        map.put("ping", p.getPing());
                        try {
                            map.put("health", p.getHealth());
                            org.bukkit.attribute.AttributeInstance attr = p.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH);
                            map.put("maxHealth", attr != null ? attr.getValue() : 20.0);
                        } catch (Throwable ignored) {
                            map.put("health", 20.0);
                            map.put("maxHealth", 20.0);
                        }
                    } else {
                        map.put("online", false);
                        map.put("ping", 0);
                        map.put("health", 0);
                        map.put("maxHealth", 20.0);
                    }
                    
                    com.destroystokyo.paper.profile.PlayerProfile profile = org.bukkit.Bukkit.createProfile(uuid, name);
                    map.put("isBanned", banList.isBanned(profile));
                    map.put("isMuted", mod != null && mod.isMuted(uuid));
                    map.put("playtime", known.getOrDefault("playtime", 0L));
                    
                    players.add(map);
                }

                // Récupération des joueurs bannis dans banned-players.json de Minecraft
                // afin qu'ils apparaissent toujours dans le panel même après un reset de la BDD SQLite
                try {
                    for (org.bukkit.BanEntry<?> entry : banList.getEntries()) {
                        Object targetObj = entry.getBanTarget();
                        com.destroystokyo.paper.profile.PlayerProfile profile = (targetObj instanceof com.destroystokyo.paper.profile.PlayerProfile pp) ? pp : null;
                        if (profile != null) {
                            java.util.UUID bUuid = profile.getId();
                            String bName = profile.getName();
                            if (bUuid == null && bName != null) {
                                org.bukkit.OfflinePlayer offP = plugin.getServer().getOfflinePlayer(bName);
                                if (offP != null) bUuid = offP.getUniqueId();
                            }
                            if (bUuid != null && !addedUuids.contains(bUuid)) {
                                Map<String, Object> map = new HashMap<>();
                                map.put("name", bName != null ? bName : "Inconnu");
                                map.put("uuid", bUuid.toString());
                                map.put("online", false);
                                map.put("ping", 0);
                                map.put("health", 0);
                                map.put("maxHealth", 20.0);
                                map.put("isBanned", true);
                                map.put("isMuted", mod != null && mod.isMuted(bUuid));
                                map.put("playtime", 0L);
                                players.add(map);
                                addedUuids.add(bUuid);
                            }
                        }
                    }
                } catch (Throwable t) {
                    plugin.getLogger().warning("Erreur lors de la récupération des entrées de bannissement: " + t.getMessage());
                }

                future.complete(players);
            });
            ctx.json(future.join());
        });

        post("/api/admin/players/action", ctx -> {
            PlayerActionRequest req = ctx.bodyAsClass(PlayerActionRequest.class);
            
            // Fetch offline player synchronously in the web thread (which is already async/off the main thread)
            org.bukkit.OfflinePlayer targetOffline = plugin.getServer().getOfflinePlayer(req.playerName);
            
            plugin.getFoliaLib().getScheduler().runNextTick((t2) -> {
                DiscordModule discord = (DiscordModule) plugin.getModuleManager().getModule("Discord");
                fr.gens.core.modules.moderation.ModerationModule mod = (fr.gens.core.modules.moderation.ModerationModule) plugin.getModuleManager().getModule("Moderation");
                org.bukkit.entity.Player target = targetOffline.getPlayer();

                if ("kick".equalsIgnoreCase(req.action)) {
                    if (target != null) {
                        plugin.getFoliaLib().getScheduler().runAtEntity(target, tEntity -> {
                            target.kick(PlaceholderUtils.parseToComponent("<red>Vous avez été expulsé par un Administrateur.<br><gray>Raison : " + (req.reason != null ? req.reason : "Aucune raison")));
                        });
                        plugin.getLogger().info("Web panel kicked " + req.playerName);
                        if (discord != null && discord.isEnabled()) discord.sendBotLogEmbed("KICK", "Joueur : " + req.playerName + "\nAdmin : WebAdmin\nRaison : " + req.reason, java.awt.Color.ORANGE);
                    }
                } else if ("ban".equalsIgnoreCase(req.action)) {
                    long durationMs = 0;
                    if (req.durationHours > 0) durationMs = req.durationHours * 3600000L;
                    else if (req.durationDays > 0) durationMs = req.durationDays * 86400000L;
                    
                    java.util.Date expires = durationMs > 0 ? new java.util.Date(System.currentTimeMillis() + durationMs) : null;
                    String reason = req.reason != null && !req.reason.isEmpty() ? req.reason : "Banni par un Administrateur";
                    
                    com.destroystokyo.paper.profile.PlayerProfile profile = org.bukkit.Bukkit.createProfile(targetOffline.getUniqueId(), targetOffline.getName());
                    ProfileBanList banList = plugin.getServer().getBanList(BanListType.PROFILE);
                    banList.addBan(profile, "<red>" + reason, expires, "WebAdmin");
                    if (target != null) {
                        plugin.getFoliaLib().getScheduler().runAtEntity(target, tEntity -> {
                            target.kick(PlaceholderUtils.parseToComponent("<red>Vous avez été banni.<br><gray>Raison : " + reason));
                        });
                    }
                    plugin.getLogger().info("Web panel banned " + req.playerName);
                    if (discord != null && discord.isEnabled()) discord.sendBotLogEmbed("BAN", "Joueur : " + req.playerName + "\nAdmin : WebAdmin\nRaison : " + reason, Color.RED);
                } else if ("unban".equalsIgnoreCase(req.action)) {
                    com.destroystokyo.paper.profile.PlayerProfile profile = org.bukkit.Bukkit.createProfile(targetOffline.getUniqueId(), targetOffline.getName());
                    ProfileBanList banList = plugin.getServer().getBanList(BanListType.PROFILE);
                    banList.pardon(profile);
                    if (discord != null && discord.isEnabled()) discord.sendBotLogEmbed("UNBAN", "Joueur : " + req.playerName + "\nAdmin : WebAdmin", Color.GREEN);
                } else if ("mute".equalsIgnoreCase(req.action)) {
                    if (mod != null && targetOffline != null && targetOffline.getUniqueId() != null) {
                        long durationMs = 0;
                        if (req.durationHours > 0) durationMs = req.durationHours * 3600000L;
                        else if (req.durationDays > 0) durationMs = req.durationDays * 86400000L;
                        String reason = req.reason != null && !req.reason.isEmpty() ? req.reason : "Aucune raison";
                        mod.mutePlayer(targetOffline.getUniqueId(), reason, durationMs);
                        
                        if (target != null) {
                            target.sendMessage(PlaceholderUtils.parseToComponent("<red><bold>Vous avez été rendu muet par le WebAdmin ! Raison : " + reason));
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
                        target.sendMessage(PlaceholderUtils.parseToComponent("<dark_gray>[<red>WebAdmin<dark_gray>] <gray>" + req.reason));
                    }
                }
            });
            ctx.status(200).json("Action effectuée");
        });

        // Édition de fichiers
        get("/api/admin/file", ctx -> {
            String path = ctx.queryParam("path");
            if (path == null || path.contains("..")) {
                ctx.status(400).result("Invalid path");
                return;
            }
            if (!path.endsWith(".yml") && !path.endsWith(".properties") && !path.endsWith(".json") && !path.endsWith(".txt")) {
                ctx.status(403).result("Forbidden file type");
                return;
            }
            try {
                File file = new File(plugin.getDataFolder(), path);
                if (!file.getCanonicalPath().startsWith(plugin.getDataFolder().getCanonicalPath())) {
                    ctx.status(403).result("Forbidden path");
                    return;
                }
                if (!file.exists()) {
                    ctx.status(404).result("File not found");
                    return;
                }
                String content = Files.readString(file.toPath(), StandardCharsets.UTF_8);
                ctx.result(content);
            } catch (java.io.IOException e) {
                ctx.status(500).result("IO Error");
            }
        });

        post("/api/admin/file", ctx -> {
            String path = ctx.queryParam("path");
            if (path == null || path.contains("..")) {
                ctx.status(400).result("Invalid path");
                return;
            }
            if (!path.endsWith(".yml") && !path.endsWith(".properties") && !path.endsWith(".json") && !path.endsWith(".txt")) {
                ctx.status(403).result("Forbidden file type");
                return;
            }
            try {
                File file = new File(plugin.getDataFolder(), path);
                if (!file.getCanonicalPath().startsWith(plugin.getDataFolder().getCanonicalPath())) {
                    ctx.status(403).result("Forbidden path");
                    return;
                }
                FileEditRequest req = ctx.bodyAsClass(FileEditRequest.class);
                Files.writeString(file.toPath(), req.content, StandardCharsets.UTF_8);
                
                // Reload configuration if it's config.yml
                if (path.equals("config.yml")) {
                    plugin.reloadConfig();
                    plugin.getLangManager().sendConsoleMessage("webmanager.log_4");
                }
                
                ctx.status(200).result("OK");
            } catch (java.io.IOException e) {
                ctx.status(500).result("IO Error");
            }
        });

        // ==========================================
        // ROUTES SHOP & ECONOMIE
        // ==========================================

        get("/api/economy/stats", ctx -> {
            EconomyModule eco = (EconomyModule) plugin.getModuleManager().getModule("economy");
            if (eco != null) {
                double totalMoney = eco.getTotalMoney();
                ctx.json(Map.of("status", "ok", "total_money", totalMoney));
            } else {
                ctx.status(404).json(plugin.getLangManager().getRaw("webmanager.module_disabled"));
            }
        });

        get("/api/shop/categories", ctx -> {
            ShopModule shop = (ShopModule) plugin.getModuleManager().getModule("dynamicshop");
            if (shop != null) {
                ctx.json(shop.getCategories());
            } else {
                ctx.status(404).json(plugin.getLangManager().getRaw("webmanager.shop_disabled"));
            }
        });

        get("/api/shop/history/{material}", ctx -> {
            ShopModule shop = (ShopModule) plugin.getModuleManager().getModule("dynamicshop");
            if (shop != null) {
                String material = ctx.pathParam("material").toUpperCase();
                ctx.json(shop.getHistory(material));
            } else {
                ctx.status(404).json(plugin.getLangManager().getRaw("webmanager.shop_disabled"));
            }
        });

        // Objets déposés en jeu par le joueur via /web deposit
        get("/api/shop/deposited", ctx -> {
            String sessionUuid = getPlayerUuidFromCtx(ctx);
            if (sessionUuid == null) {
                ctx.status(401).json(Map.of("error", "Non connecté"));
                return;
            }
            ShopModule shop = (ShopModule) plugin.getModuleManager().getModule("dynamicshop");
            List<Map<String, Object>> deposited = webDAO.getCasinoInventory(sessionUuid);
            List<Map<String, Object>> result = new ArrayList<>();
            for (Map<String, Object> item : deposited) {
                String material = (String) item.get("material");
                int amount = ((Number) item.get("amount")).intValue();
                double sellPrice = 0.0;
                boolean isShopItem = false;
                if (shop != null) {
                    Material mat = Material.matchMaterial(material);
                    if (mat != null) {
                        for (ShopCategory cat : shop.getCategories()) {
                            ShopItem si = cat.getItem(mat);
                            if (si != null && si.isEnabled()) {
                                sellPrice = si.getCurrentSellPrice();
                                isShopItem = true;
                                break;
                            }
                        }
                    }
                }
                Map<String, Object> map = new HashMap<>(item);
                map.put("unitSellPrice", sellPrice);
                map.put("currentSellPrice", sellPrice);
                map.put("baseSellPrice", sellPrice);
                map.put("totalSellPrice", Math.round(sellPrice * amount * 100.0) / 100.0);
                map.put("canSell", isShopItem && sellPrice > 0);
                result.add(map);
            }
            ctx.json(result);
        });

        // Vente en ligne d'un objet déposé
        post("/api/shop/sell-deposited", ctx -> {
            String sessionUuid = getPlayerUuidFromCtx(ctx);
            if (sessionUuid == null) {
                ctx.status(401).json(Map.of("error", "Non connecté"));
                return;
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> body = ctx.bodyAsClass(Map.class);
            if (body == null || !body.containsKey("betId")) {
                ctx.status(400).json(Map.of("error", "Paramètre betId manquant"));
                return;
            }
            int betId = ((Number) body.get("betId")).intValue();
            Map<String, Object> itemData = webDAO.getDepositedItem(betId, sessionUuid);
            if (itemData == null) {
                ctx.status(404).json(Map.of("error", "Objet introuvable ou déjà vendu"));
                return;
            }
            String materialStr = (String) itemData.get("material");
            int totalAmount = ((Number) itemData.get("amount")).intValue();
            int amountToSell = totalAmount;
            if (body.containsKey("quantity")) {
                int reqQty = ((Number) body.get("quantity")).intValue();
                if (reqQty <= 0) {
                    ctx.status(400).json(Map.of("error", "Quantité invalide"));
                    return;
                }
                if (reqQty < totalAmount) {
                    amountToSell = reqQty;
                }
            }

            ShopModule shop = (ShopModule) plugin.getModuleManager().getModule("dynamicshop");
            if (shop == null) {
                ctx.status(400).json(Map.of("error", "La boutique dynamique est désactivée"));
                return;
            }
            Material mat = Material.matchMaterial(materialStr);
            ShopItem targetItem = null;
            if (mat != null) {
                for (ShopCategory cat : shop.getCategories()) {
                    targetItem = cat.getItem(mat);
                    if (targetItem != null) break;
                }
            }
            if (targetItem == null || !targetItem.isEnabled()) {
                ctx.status(400).json(Map.of("error", "Cet objet n'est pas racheté par la boutique"));
                return;
            }

            double unitPrice = targetItem.getCurrentSellPrice();
            double totalEarned = Math.round(unitPrice * amountToSell * 100.0) / 100.0;

            // Retirer ou mettre à jour la quantité de l'objet déposé
            int remaining = totalAmount - amountToSell;
            if (remaining <= 0) {
                webDAO.deleteDepositedItem(betId, sessionUuid);
            } else {
                webDAO.updateDepositedItemAmount(betId, sessionUuid, remaining);
            }

            // Créditer le solde du joueur (supporte joueur en ligne ET hors-ligne)
            EconomyModule eco = (EconomyModule) plugin.getModuleManager().getModule("economy");
            UUID uuid = UUID.fromString(sessionUuid);
            if (eco != null) {
                eco.addMoney(uuid, totalEarned);
            }

            // Réapprovisionner le stock de la boutique
            targetItem.setStock(targetItem.getStock() + amountToSell);
            shop.saveShop();
            shop.logPlayerTransaction(uuid, "SELL", materialStr, amountToSell, totalEarned);

            // Si le joueur est en jeu, notification directe
            Player onlinePlayer = Bukkit.getPlayer(uuid);
            if (onlinePlayer != null && onlinePlayer.isOnline()) {
                onlinePlayer.sendMessage(PlaceholderUtils.parseToComponent(
                    "<green>[Boutique Web] Vous avez vendu en ligne <white>x" + amountToSell + " " + materialStr + "</white> pour <yellow>" + totalEarned + " $</yellow> !"
                ));
            }

            double newBalance = eco != null ? eco.getBalance(uuid) : 0.0;
            ctx.json(Map.of(
                "success", true,
                "earned", totalEarned,
                "newBalance", newBalance,
                "material", materialStr,
                "amount", amountToSell,
                "remainingAmount", Math.max(0, remaining)
            ));
        });

        // Récupération en jeu d'un objet déposé vers l'inventaire Minecraft
        post("/api/shop/withdraw-deposited", ctx -> {
            String sessionUuid = getPlayerUuidFromCtx(ctx);
            if (sessionUuid == null) {
                ctx.status(401).json(Map.of("error", "Non connecté"));
                return;
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> body = ctx.bodyAsClass(Map.class);
            if (body == null || !body.containsKey("betId")) {
                ctx.status(400).json(Map.of("error", "Paramètre betId manquant"));
                return;
            }
            int betId = ((Number) body.get("betId")).intValue();
            Map<String, Object> itemData = webDAO.getDepositedItem(betId, sessionUuid);
            if (itemData == null) {
                ctx.status(404).json(Map.of("error", "Objet introuvable ou déjà récupéré"));
                return;
            }
            String materialStr = (String) itemData.get("material");
            int totalAmount = ((Number) itemData.get("amount")).intValue();
            int amountToWithdraw = totalAmount;
            if (body.containsKey("quantity")) {
                int reqQty = ((Number) body.get("quantity")).intValue();
                if (reqQty <= 0) {
                    ctx.status(400).json(Map.of("error", "Quantité invalide"));
                    return;
                }
                if (reqQty < totalAmount) {
                    amountToWithdraw = reqQty;
                }
            }
            String base64 = (String) itemData.get("base64_data");

            // Retirer ou mettre à jour la quantité de l'objet déposé
            int remaining = totalAmount - amountToWithdraw;
            if (remaining <= 0) {
                webDAO.deleteDepositedItem(betId, sessionUuid);
            } else {
                webDAO.updateDepositedItemAmount(betId, sessionUuid, remaining);
            }

            final int finalWithdrawAmount = amountToWithdraw;
            UUID uuid = UUID.fromString(sessionUuid);
            Player onlinePlayer = Bukkit.getPlayer(uuid);
            if (onlinePlayer != null && onlinePlayer.isOnline()) {
                plugin.getFoliaLib().getScheduler().runAtEntity(onlinePlayer, (task) -> {
                    ItemStack stack = base64 != null && !base64.isEmpty() 
                        ? plugin.getStorageManager().itemStackFromBase64(base64) 
                        : null;
                    if (stack == null) {
                        Material mat = Material.matchMaterial(materialStr);
                        if (mat != null) stack = new ItemStack(mat, finalWithdrawAmount);
                    } else {
                        stack.setAmount(finalWithdrawAmount);
                    }
                    if (stack != null) {
                        for (ItemStack rem : onlinePlayer.getInventory().addItem(stack).values()) {
                            onlinePlayer.getWorld().dropItemNaturally(onlinePlayer.getLocation(), rem);
                        }
                        onlinePlayer.sendMessage(PlaceholderUtils.parseToComponent(
                            "<green>[Web] Vous avez récupéré votre objet déposé : <white>x" + finalWithdrawAmount + " " + materialStr + "</white> !"
                        ));
                    }
                });
            } else {
                // Si hors-ligne, mise en attente sécurisée distribuée à la connexion
                webDAO.addWebReward(sessionUuid, materialStr, finalWithdrawAmount, base64 != null ? base64 : "");
            }

            ctx.json(Map.of(
                "success", true,
                "message", "Objet transféré vers votre inventaire en jeu !",
                "material", materialStr,
                "amount", finalWithdrawAmount,
                "remainingAmount", Math.max(0, remaining)
            ));
        });

        // Achat en ligne avec débit immédiat et livraison différée/immédiate
        post("/api/shop/buy", ctx -> {
            String sessionUuid = getPlayerUuidFromCtx(ctx);
            if (sessionUuid == null) {
                ctx.status(401).json(Map.of("error", "Non connecté"));
                return;
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> body = ctx.bodyAsClass(Map.class);
            if (body == null || !body.containsKey("material")) {
                ctx.status(400).json(Map.of("error", "Objet manquant"));
                return;
            }
            String materialStr = (String) body.get("material");
            int amount = body.containsKey("amount") ? ((Number) body.get("amount")).intValue() : 1;
            if (amount <= 0 || amount > 2304) {
                ctx.status(400).json(Map.of("error", "Quantité invalide (1 à 2304)"));
                return;
            }

            ShopModule shop = (ShopModule) plugin.getModuleManager().getModule("dynamicshop");
            if (shop == null) {
                ctx.status(400).json(Map.of("error", "La boutique dynamique est désactivée"));
                return;
            }
            Material mat = Material.matchMaterial(materialStr);
            ShopItem targetItem = null;
            if (mat != null) {
                for (ShopCategory cat : shop.getCategories()) {
                    targetItem = cat.getItem(mat);
                    if (targetItem != null) break;
                }
            }
            if (targetItem == null || !targetItem.isEnabled()) {
                ctx.status(400).json(Map.of("error", "Objet non disponible à l'achat"));
                return;
            }

            double unitPrice = targetItem.getCurrentBuyPrice();
            double totalCost = Math.round(unitPrice * amount * 100.0) / 100.0;

            EconomyModule eco = (EconomyModule) plugin.getModuleManager().getModule("economy");
            UUID uuid = UUID.fromString(sessionUuid);

            if (eco != null) {
                boolean debited = eco.takeMoneyAtomic(uuid, totalCost);
                if (!debited) {
                    double currentBal = eco.getBalance(uuid);
                    ctx.status(400).json(Map.of("error", "Solde insuffisant (" + currentBal + " $ disponible, " + totalCost + " $ requis)"));
                    return;
                }
            }

            // Décrémenter le stock si applicable
            if (targetItem.getStock() > 0) {
                targetItem.setStock(Math.max(0, targetItem.getStock() - amount));
                shop.saveShop();
            }
            shop.logPlayerTransaction(uuid, "BUY", materialStr, amount, totalCost);

            // Distribution de l'objet
            Player onlinePlayer = Bukkit.getPlayer(uuid);
            if (onlinePlayer != null && onlinePlayer.isOnline()) {
                plugin.getFoliaLib().getScheduler().runAtEntity(onlinePlayer, (task) -> {
                    ItemStack stack = new ItemStack(mat, amount);
                    onlinePlayer.getInventory().addItem(stack).values().forEach(rem -> 
                        onlinePlayer.getWorld().dropItemNaturally(onlinePlayer.getLocation(), rem)
                    );
                    onlinePlayer.sendMessage(PlaceholderUtils.parseToComponent(
                        "<green>[Boutique Web] Vous avez acheté en ligne <white>x" + amount + " " + materialStr + "</white> pour <yellow>" + totalCost + " $</yellow> !"
                    ));
                });
            } else {
                ItemStack stack = new ItemStack(mat, amount);
                String base64 = plugin.getStorageManager().itemStackToBase64(stack);
                webDAO.addWebReward(sessionUuid, materialStr, amount, base64);
            }

            double newBalance = eco != null ? eco.getBalance(uuid) : 0.0;
            ctx.json(Map.of(
                "success", true,
                "totalCost", totalCost,
                "newBalance", newBalance,
                "material", materialStr,
                "amount", amount,
                "deliveredInstantly", onlinePlayer != null && onlinePlayer.isOnline()
            ));
        });

        post("/api/admin/shop/category", ctx -> {
            ShopModule shop = (ShopModule) plugin.getModuleManager().getModule("dynamicshop");
            if (shop == null) { ctx.status(404).json(Map.of("error", "Shop module disabled")); return; }
            
            CategoryRequest request = ctx.bodyAsClass(CategoryRequest.class);
            if (request.id == null || request.id.trim().isEmpty()) {
                ctx.status(400).json(Map.of("error", "ID de catégorie manquant"));
                return;
            }
            String catId = request.id.trim().toLowerCase();
            String displayName = (request.displayName != null && !request.displayName.trim().isEmpty()) ? request.displayName.trim() : catId;
            Material icon = Material.CHEST;
            if (request.icon != null && !request.icon.trim().isEmpty()) {
                try {
                    icon = Material.valueOf(request.icon.trim().toUpperCase());
                } catch (IllegalArgumentException ignored) {}
            }
            
            ShopCategory existing = shop.getCategory(catId);
            if (existing != null) {
                existing.setDisplayName(displayName);
                existing.setIcon(icon);
            } else {
                shop.getCategories().add(new ShopCategory(catId, displayName, icon));
            }
            shop.saveShop();
            ctx.status(200).json(Map.of("success", true));
        });

        post("/api/admin/shop/item", ctx -> {
            ShopModule shop = (ShopModule) plugin.getModuleManager().getModule("dynamicshop");
            if (shop == null) { ctx.status(404).json(Map.of("error", "Shop module disabled")); return; }
            
            ItemRequest req = ctx.bodyAsClass(ItemRequest.class);
            if (req.categoryId == null || req.material == null) {
                ctx.status(400).json(Map.of("error", "Catégorie ou matériau manquant"));
                return;
            }
            ShopCategory cat = shop.getCategory(req.categoryId.trim().toLowerCase());
            if (cat == null) {
                ctx.status(404).json(Map.of("error", "Catégorie introuvable : " + req.categoryId));
                return;
            }
            
            Material mat;
            try {
                mat = Material.valueOf(req.material.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                ctx.status(400).json(Map.of("error", "Matériau Minecraft invalide : " + req.material));
                return;
            }
            
            ShopItem item = cat.getItem(mat);
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
                item = new ShopItem(mat, req.baseBuyPrice, req.baseSellPrice);
                item.setTargetStock(req.targetStock);
                item.setCommand(req.isCommand);
                if (req.isCommand && req.commandToExecute != null) {
                    item.setCommandToExecute(req.commandToExecute);
                }
                item.setEnabled(req.isEnabled);
                cat.addItem(item);
            }
            shop.saveShop();
            ctx.status(200).json(Map.of("success", true));
        });

        delete("/api/admin/shop/item/{category}/{material}", ctx -> {
            ShopModule shop = (ShopModule) plugin.getModuleManager().getModule("dynamicshop");
            if (shop == null) { ctx.status(404).json(Map.of("error", "Shop module disabled")); return; }
            
            String catId = ctx.pathParam("category");
            String matName = ctx.pathParam("material").trim().toUpperCase();
            
            ShopCategory cat = shop.getCategory(catId);
            if (cat != null) {
                try {
                    Material mat = Material.valueOf(matName);
                    cat.removeItem(mat);
                    // Suppression SQL
                    shop.deleteItem(catId, mat.name());
                    shop.saveShop();
                    ctx.status(200).json(Map.of("success", true));
                } catch (IllegalArgumentException e) {
                    ctx.status(400).json(Map.of("error", "Matériel invalide : " + matName));
                }
            } else {
                ctx.status(404).json(Map.of("error", "Catégorie introuvable : " + catId));
            }
        });

        delete("/api/admin/shop/category/{id}", ctx -> {
            ShopModule shop = (ShopModule) plugin.getModuleManager().getModule("dynamicshop");
            if (shop == null) { ctx.status(404).json(Map.of("error", "Shop module disabled")); return; }
            
            String catId = ctx.pathParam("id");
            ShopCategory cat = shop.getCategory(catId);
            
            if (cat != null) {
                shop.getCategories().remove(cat);
                shop.deleteCategory(catId);
                shop.saveShop();
                ctx.status(200).json(Map.of("success", true));
            } else {
                ctx.status(404).json(Map.of("error", "Catégorie introuvable : " + catId));
            }
        });

        post("/api/admin/wipe-server", ctx -> {
            plugin.setWiping(true);
            
            for (org.bukkit.entity.Player p : org.bukkit.Bukkit.getOnlinePlayers()) {
                plugin.getFoliaLib().getScheduler().runAtEntity(p, (task) -> {
                    p.kick(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize("<red>Wipe du serveur en cours... Relancez manuellement après suppression de la carte."));
                });
            }

            plugin.getFoliaLib().getScheduler().runAsync((task) -> {
                plugin.getDatabaseManager().wipeServerData();
                try {
                    java.io.File lootrDir = new java.io.File(plugin.getDataFolder(), "lootr");
                    if (lootrDir.exists()) {
                        java.nio.file.Files.walk(lootrDir.toPath())
                                .sorted(java.util.Comparator.reverseOrder())
                                .map(java.nio.file.Path::toFile)
                                .forEach(java.io.File::delete);
                        plugin.getLogger().info("Wipe success: Lootr folder has been cleared.");
                    }
                } catch (Exception e) {
                    plugin.getLogger().severe("Failed to wipe Lootr folder: " + e.getMessage());
                }
                
                try {
                    java.io.File blueMapMapsDir = new java.io.File(plugin.getDataFolder().getParentFile(), "BlueMap/web/maps");
                    if (blueMapMapsDir.exists()) {
                        java.nio.file.Files.walk(blueMapMapsDir.toPath())
                                .sorted(java.util.Comparator.reverseOrder())
                                .map(java.nio.file.Path::toFile)
                                .forEach(java.io.File::delete);
                        plugin.getLogger().info("Wipe success: BlueMap maps folder has been cleared.");
                    }
                } catch (Exception e) {
                    plugin.getLogger().severe("Failed to wipe BlueMap maps folder: " + e.getMessage());
                }
                plugin.getFoliaLib().getScheduler().runNextTick((t2) -> {
                    org.bukkit.Bukkit.shutdown();
                });
            });
            ctx.status(200).json("Wipe successful. Server is shutting down.");
        });

        // ==============================================
        // Teams Stats API
        // ==============================================
        get("/api/stats/teams/best", ctx -> {
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

        get("/api/stats/teams", ctx -> {
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
        get("/api/ah/items", ctx -> {
            fr.gens.core.modules.AuctionHouseModule ah = (fr.gens.core.modules.AuctionHouseModule) plugin.getModuleManager().getModule("auctionhouse");
            if (ah != null) {
                ctx.json(ah.getAuctionItemsForWeb());
            } else {
                ctx.status(404).result("AH module not found");
            }
        });

        // Achat d'une offre de l'Hôtel des Ventes (AH) depuis le Web
        post("/api/ah/buy", ctx -> {
            String sessionUuid = getPlayerUuidFromCtx(ctx);
            if (sessionUuid == null) {
                ctx.status(401).json(Map.of("error", "Non connecté"));
                return;
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> body = ctx.bodyAsClass(Map.class);
            if (body == null || !body.containsKey("id")) {
                ctx.status(400).json(Map.of("error", "Paramètre id manquant"));
                return;
            }
            int auctionId = ((Number) body.get("id")).intValue();

            fr.gens.core.modules.AuctionHouseModule ah = (fr.gens.core.modules.AuctionHouseModule) plugin.getModuleManager().getModule("auctionhouse");
            if (ah == null || ah.getAhDAO() == null) {
                ctx.status(404).json(Map.of("error", "Module Hôtel des Ventes indisponible"));
                return;
            }

            fr.gens.core.modules.AuctionHouseModule.AhItem ahItem = ah.getAhDAO().getAuction(auctionId);
            if (ahItem == null) {
                ctx.status(404).json(Map.of("error", "Cette offre n'existe plus ou a déjà été achetée."));
                return;
            }

            if (sessionUuid.equals(ahItem.sellerUuid)) {
                ctx.status(400).json(Map.of("error", "Vous ne pouvez pas acheter votre propre offre. Utilisez le bouton Récupérer."));
                return;
            }

            EconomyModule eco = (EconomyModule) plugin.getModuleManager().getModule("economy");
            if (eco == null) {
                ctx.status(500).json(Map.of("error", "Système économique indisponible."));
                return;
            }

            UUID buyerUuid = UUID.fromString(sessionUuid);
            double balance = eco.getBalance(buyerUuid);
            if (balance < ahItem.price) {
                ctx.status(400).json(Map.of("error", "Solde insuffisant (" + String.format("%.2f", balance) + " $ disponible, " + String.format("%.2f", ahItem.price) + " $ requis)."));
                return;
            }

            if (!eco.takeMoneyAtomic(buyerUuid, ahItem.price)) {
                ctx.status(400).json(Map.of("error", "Fonds insuffisants lors du débit."));
                return;
            }

            if (!ah.getAhDAO().deleteAuction(auctionId)) {
                // Déjà acheté ou retiré au même moment : remboursement immédiat !
                eco.giveMoney(buyerUuid, ahItem.price);
                ctx.status(409).json(Map.of("error", "Cette offre a été achetée ou retirée au même moment. Vous avez été remboursé."));
                return;
            }

            // Calcul de la taxe et crédit au vendeur
            double taxRate = plugin.getConfigManager().getConfig("modules/economy.yml").getDouble("ah.tax_percentage", 0.0) / 100.0;
            double taxAmount = ahItem.price * taxRate;
            double sellerProfit = ahItem.price - taxAmount;
            UUID sellerUuid = UUID.fromString(ahItem.sellerUuid);
            eco.giveMoney(sellerUuid, sellerProfit);

            // Notification du vendeur s'il est en ligne
            Player seller = Bukkit.getPlayer(sellerUuid);
            if (seller != null && seller.isOnline()) {
                plugin.getFoliaLib().getScheduler().runAtEntity(seller, (ts) -> {
                    if (taxAmount > 0) {
                        seller.sendMessage(PlaceholderUtils.parseToComponent(
                            "<green>[Hôtel des Ventes] Un joueur a acheté votre offre sur le Web ! Vous gagnez <yellow>" + String.format("%.2f", sellerProfit) + " $ <dark_gray>(Taxe: -" + String.format("%.2f", taxAmount) + " $)"));
                    } else {
                        seller.sendMessage(PlaceholderUtils.parseToComponent(
                            "<green>[Hôtel des Ventes] Un joueur a acheté votre offre sur le Web pour <yellow>" + String.format("%.2f", ahItem.price) + " $ <green>!"));
                    }
                });
            }

            // Remise de l'objet à l'acheteur
            ItemStack itemStack = fr.gens.core.utils.ItemSerializer.fromBase64(ahItem.itemData);
            Player buyer = Bukkit.getPlayer(buyerUuid);
            if (buyer != null && buyer.isOnline()) {
                plugin.getFoliaLib().getScheduler().runAtEntity(buyer, (task) -> {
                    if (itemStack != null) {
                        for (ItemStack rem : buyer.getInventory().addItem(itemStack).values()) {
                            buyer.getWorld().dropItemNaturally(buyer.getLocation(), rem);
                        }
                        buyer.sendMessage(PlaceholderUtils.parseToComponent(
                            "<green>[Hôtel des Ventes] Vous avez acheté un objet sur le Web à <yellow>" + ahItem.sellerName + " <green>pour <yellow>" + String.format("%.2f", ahItem.price) + " $ <green>!"));
                    }
                });
            } else {
                // Hors-ligne : stocké dans player_web_rewards
                webDAO.addWebReward(sessionUuid, itemStack != null ? itemStack.getType().name() : "ITEM", itemStack != null ? itemStack.getAmount() : 1, ahItem.itemData);
            }

            ctx.json(Map.of(
                "success", true,
                "message", "Achat effectué avec succès ! L'objet vous a été livré en jeu (ou placé dans vos récompenses en attente).",
                "price", ahItem.price,
                "newBalance", eco.getBalance(buyerUuid)
            ));
        });

        // Annulation et récupération d'une offre AH par son vendeur depuis le Web
        post("/api/ah/cancel", ctx -> {
            String sessionUuid = getPlayerUuidFromCtx(ctx);
            if (sessionUuid == null) {
                ctx.status(401).json(Map.of("error", "Non connecté"));
                return;
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> body = ctx.bodyAsClass(Map.class);
            if (body == null || !body.containsKey("id")) {
                ctx.status(400).json(Map.of("error", "Paramètre id manquant"));
                return;
            }
            int auctionId = ((Number) body.get("id")).intValue();

            fr.gens.core.modules.AuctionHouseModule ah = (fr.gens.core.modules.AuctionHouseModule) plugin.getModuleManager().getModule("auctionhouse");
            if (ah == null || ah.getAhDAO() == null) {
                ctx.status(404).json(Map.of("error", "Module Hôtel des Ventes indisponible"));
                return;
            }

            fr.gens.core.modules.AuctionHouseModule.AhItem ahItem = ah.getAhDAO().getAuction(auctionId);
            if (ahItem == null) {
                ctx.status(404).json(Map.of("error", "Cette offre n'existe plus ou a déjà été retirée."));
                return;
            }

            if (!sessionUuid.equals(ahItem.sellerUuid)) {
                ctx.status(403).json(Map.of("error", "Vous n'êtes pas le vendeur de cette offre."));
                return;
            }

            if (!ah.getAhDAO().deleteAuction(auctionId)) {
                ctx.status(500).json(Map.of("error", "Impossible de retirer cette offre."));
                return;
            }

            // Restituer l'objet au vendeur
            ItemStack itemStack = fr.gens.core.utils.ItemSerializer.fromBase64(ahItem.itemData);
            UUID sellerUuid = UUID.fromString(sessionUuid);
            Player seller = Bukkit.getPlayer(sellerUuid);
            if (seller != null && seller.isOnline()) {
                plugin.getFoliaLib().getScheduler().runAtEntity(seller, (task) -> {
                    if (itemStack != null) {
                        for (ItemStack rem : seller.getInventory().addItem(itemStack).values()) {
                            seller.getWorld().dropItemNaturally(seller.getLocation(), rem);
                        }
                        seller.sendMessage(PlaceholderUtils.parseToComponent(
                            "<green>[Hôtel des Ventes] Offre retirée depuis le Web. Votre objet vous a été restitué !"));
                    }
                });
            } else {
                webDAO.addWebReward(sessionUuid, itemStack != null ? itemStack.getType().name() : "ITEM", itemStack != null ? itemStack.getAmount() : 1, ahItem.itemData);
            }

            ctx.json(Map.of(
                "success", true,
                "message", "Offre retirée avec succès ! L'objet a été restitué dans votre inventaire (ou dans vos récompenses en attente)."
            ));
        });

          // ==========================================
          // ROUTE PAGE PUBLIQUE
          // ==========================================
          get("/api/public/news", ctx -> {
              ctx.result(plugin.getConfig().getString("publicPageContent", "Bienvenue sur notre serveur !\nNous sommes heureux de vous accueillir."));
          });

          get("/api/public/bluemap", ctx -> {
              ctx.result(plugin.getConfigManager().getConfig("modules/bluemap.yml").getString("bluemap.url", "http://localhost:8100"));
          });

          get("/api/public/server_ip", ctx -> {
              ctx.result(plugin.getConfigManager().getConfig("modules/web.yml").getString("web.server_ip", "gens-core.duckdns.org"));
          });

        // ==========================================
        // ROUTES LEADERBOARD
        // ==========================================
        get("/api/stats/leaderboard", ctx -> {
            fr.gens.core.modules.stats.StatsModule statsModule = (fr.gens.core.modules.stats.StatsModule) plugin.getModuleManager().getModule("stats");
            ctx.json(statsModule != null ? statsModule.getStatsDAO().getGlobalLeaderboard() : java.util.Collections.emptyList());
        });

        get("/api/stats/quests/leaderboard", ctx -> {
            fr.gens.core.modules.quests.QuestModule questModule = (fr.gens.core.modules.quests.QuestModule) plugin.getModuleManager().getModule("quests");
            ctx.json(questModule != null ? questModule.getQuestDAO().getQuestsLeaderboardData() : java.util.Collections.emptyMap());
        });

        get("/api/stats/jobs", ctx -> {
            fr.gens.core.modules.jobs.JobsModule jobsModule = (fr.gens.core.modules.jobs.JobsModule) plugin.getModuleManager().getModule("jobs");
            ctx.json(jobsModule != null ? jobsModule.getJobsDAO().getJobsLeaderboardData() : java.util.Collections.emptyMap());
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
    public static class CategoryRequest {
        public String id;
        public String displayName;
        public String icon;
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

    @com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
    public static class LoginRequest {
        public String password;
    }

    public static class LoginResponse {
        public String token;
        public LoginResponse(String token) { this.token = token; }
    }
}




