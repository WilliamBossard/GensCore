package fr.gens.core.web;

import fr.gens.core.CorePlugin;
import fr.gens.core.database.AuthDAO;
import fr.gens.core.database.WebDAO;
import fr.gens.core.modules.auth.AuthModule;
import static io.javalin.apibuilder.ApiBuilder.*;
import org.bukkit.Bukkit;

import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.List;
import java.util.ArrayList;
import java.util.Random;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;
import java.sql.Connection;
import java.sql.PreparedStatement;

public class WebPlayerAPI implements Listener {

    private final CorePlugin plugin;
    private final WebManager webManager;
    private final WebDAO webDAO;
    private final List<WheelReward> wheelRewards;

    public WebPlayerAPI(CorePlugin plugin, WebManager webManager) {
        this.plugin = plugin;
        this.webManager = webManager;
        this.webDAO = new WebDAO(plugin);
        this.wheelRewards = new ArrayList<>();
        Bukkit.getPluginManager().registerEvents(this, plugin);
        
        loadWheelConfig();
    }

    private void loadWheelConfig() {
        wheelRewards.clear();
        org.bukkit.configuration.file.FileConfiguration minigamesConfig = plugin.getConfigManager().getConfig("modules/minigames.yml");
        ConfigurationSection wheelSec = minigamesConfig.getConfigurationSection("minigames.wheel.rewards");
        if (wheelSec == null) {
            // Create default configuration
            minigamesConfig.set("minigames.wheel.rewards.1.name", "10 Diamonds");
            minigamesConfig.set("minigames.wheel.rewards.1.command", "give %player% diamond 10");
            minigamesConfig.set("minigames.wheel.rewards.1.chance", 40);
            minigamesConfig.set("minigames.wheel.rewards.1.color", "#10b981");

            minigamesConfig.set("minigames.wheel.rewards.2.name", "2 Netherite Ingots");
            minigamesConfig.set("minigames.wheel.rewards.2.command", "give %player% netherite_ingot 2");
            minigamesConfig.set("minigames.wheel.rewards.2.chance", 30);
            minigamesConfig.set("minigames.wheel.rewards.2.color", "#3b82f6");

            minigamesConfig.set("minigames.wheel.rewards.3.name", "1 Enchanted Golden Apple");
            minigamesConfig.set("minigames.wheel.rewards.3.command", "give %player% enchanted_golden_apple 1");
            minigamesConfig.set("minigames.wheel.rewards.3.chance", 20);
            minigamesConfig.set("minigames.wheel.rewards.3.color", "#f59e0b");

            minigamesConfig.set("minigames.wheel.rewards.4.name", "1 Elytra");
            minigamesConfig.set("minigames.wheel.rewards.4.command", "give %player% elytra 1");
            minigamesConfig.set("minigames.wheel.rewards.4.chance", 9);
            minigamesConfig.set("minigames.wheel.rewards.4.color", "#8b5cf6");

            minigamesConfig.set("minigames.wheel.rewards.5.name", "Cow Spawner");
            minigamesConfig.set("minigames.wheel.rewards.5.command", "give %player% spawner 1 name:<yellow>Cow_Spawner");
            minigamesConfig.set("minigames.wheel.rewards.5.chance", 1);
            minigamesConfig.set("minigames.wheel.rewards.5.color", "#ef4444");
            
            plugin.getConfigManager().saveConfig("modules/minigames.yml");
            wheelSec = minigamesConfig.getConfigurationSection("minigames.wheel.rewards");
        }

        for (String key : wheelSec.getKeys(false)) {
            String name = wheelSec.getString(key + ".name");
            String command = wheelSec.getString(key + ".command");
            int chance = wheelSec.getInt(key + ".chance");
            String color = wheelSec.getString(key + ".color");
            wheelRewards.add(new WheelReward(name, command, chance, color));
        }
    }

    public void registerRoutes() {
        post("/api/player/login", ctx -> {
            String ip = ctx.ip();
            long nowTime = System.currentTimeMillis();
            if (webManager.playerRateLimitReset.getOrDefault(ip, 0L) < nowTime) {
                webManager.playerLoginRateLimit.remove(ip);
                webManager.playerRateLimitReset.put(ip, nowTime + 60000L); // 1 minute
            }
            int attempts = webManager.playerLoginRateLimit.getOrDefault(ip, 0);
            if (attempts >= 5) {
                ctx.status(429).json(Map.of("error", "Trop de tentatives. Reessayez dans une minute."));
                return;
            }

            LoginRequest req = ctx.bodyAsClass(LoginRequest.class);
            
            UUID playerUUID = null;
            boolean isOp = false;

            // Trouver le joueur
            java.util.concurrent.CompletableFuture<Player> futurePlayer = new java.util.concurrent.CompletableFuture<>();
            plugin.getFoliaLib().getScheduler().runNextTick((t2) -> {
                futurePlayer.complete(Bukkit.getPlayer(req.username));
            });
            Player targetOnline = futurePlayer.join();
            if (targetOnline != null) {
                playerUUID = targetOnline.getUniqueId();
                isOp = targetOnline.isOp();
            } else {
                playerUUID = webDAO.getPlayerUuidByUsername(req.username);
                if (playerUUID != null) {
                    org.bukkit.OfflinePlayer offlineTarget = Bukkit.getOfflinePlayer(playerUUID);
                    isOp = offlineTarget.isOp();
                } else {
                    isOp = false;
                }
            }

            if (playerUUID == null) {
                webManager.playerLoginRateLimit.put(ip, attempts + 1);
                ctx.status(401).json(Map.of("error", "Joueur introuvable."));
                return;
            }

            fr.gens.core.modules.auth.AuthModule authModule = (fr.gens.core.modules.auth.AuthModule) plugin.getModuleManager().getModule("auth");
            AuthDAO.AuthData data = authModule != null ? authModule.getAuthDAO().getAuthData(playerUUID) : null;
            if (data == null) {
                webManager.playerLoginRateLimit.put(ip, attempts + 1);
                ctx.status(401).json(Map.of("error", "Aucun compte enregistr\u00e9 (/register en jeu)."));
                return;
            }

            boolean isPasswordCorrect = false;
            if (data.hash.startsWith("$2a$") || data.hash.startsWith("$2b$") || data.hash.startsWith("$2y$")) {
                isPasswordCorrect = org.mindrot.jbcrypt.BCrypt.checkpw(req.password, data.hash);
            } else {
                String hashedInput = AuthModule.hashPassword(req.password, data.salt);
                isPasswordCorrect = hashedInput.equals(data.hash);
            }

            if (!isPasswordCorrect) {
                webManager.playerLoginRateLimit.put(ip, attempts + 1);
                ctx.status(401).json(Map.of("error", "Mot de passe incorrect."));
                return;
            }

            webManager.playerLoginRateLimit.remove(ip);

            // Generer le VRAI token securise
            String token = UUID.randomUUID().toString() + "-" + System.currentTimeMillis();
            
            // On sauvegarde le token en session RAM
            webManager.activePlayerSessions.put(token, playerUUID.toString());
            webManager.playerSessionExpiry.put(token, System.currentTimeMillis() + (24L * 60 * 60 * 1000L)); // Expire dans 24h
            
            Map<String, Object> response = new HashMap<>();
            response.put("token", token);
            response.put("uuid", playerUUID.toString());
            response.put("username", req.username);
            response.put("isOp", isOp);
            
            ctx.json(response);
        });

        get("/api/head/{name}", ctx -> {
            String name = ctx.pathParam("name");
            UUID uuid = webDAO.getPlayerUuidByUsername(name);
            if (uuid != null && fr.gens.core.utils.FloodgateUtil.isBedrockPlayer(uuid)) {
                ctx.redirect("https://mc-heads.net/avatar/MHF_Steve");
            } else {
                ctx.redirect("https://mc-heads.net/avatar/" + name);
            }
        });

        get("/api/head/{name}/{size}", ctx -> {
            String name = ctx.pathParam("name");
            String size = ctx.pathParam("size");
            UUID uuid = webDAO.getPlayerUuidByUsername(name);
            if (uuid != null && fr.gens.core.utils.FloodgateUtil.isBedrockPlayer(uuid)) {
                ctx.redirect("https://mc-heads.net/avatar/MHF_Steve/" + size);
            } else {
                ctx.redirect("https://mc-heads.net/avatar/" + name + "/" + size);
            }
        });

        get("/api/player/stats", ctx -> {
            String uuidStr = ctx.queryParam("uuid");
            if (uuidStr == null) {
                ctx.status(400).json("UUID manquant");
                return;
            }

            Map<String, Object> stats = new HashMap<>();
            
            // Quetes
            fr.gens.core.modules.quests.QuestModule questModule = (fr.gens.core.modules.quests.QuestModule) plugin.getModuleManager().getModule("quests");
            int questsCompleted = questModule != null ? questModule.getQuestDAO().getQuestsCompletedTotal(UUID.fromString(uuidStr)) : 0;
            stats.put("questsCompleted", questsCompleted);

            // Eco
            stats.put("balance", webDAO.getPlayerBalance(uuidStr));

            // Global Stats
            fr.gens.core.modules.stats.StatsModule statsModule = (fr.gens.core.modules.stats.StatsModule) plugin.getModuleManager().getModule("stats");
            fr.gens.core.modules.stats.StatsModule.PlayerStats pStats = null;
            
            if (statsModule != null && statsModule.isEnabled()) {
                pStats = statsModule.getStatsIfCached(UUID.fromString(uuidStr));
                if (pStats == null) {
                    pStats = statsModule.getStatsDAO().loadPlayerStats(UUID.fromString(uuidStr)).join();
                }
            }
            
            if (pStats != null) {
                stats.put("blocksBroken", pStats.blocksBroken);
                stats.put("mobsKilled", pStats.mobsKilled);
                stats.put("playtimeMinutes", pStats.playtimeMinutes);
                stats.put("deaths", pStats.deaths);
                stats.put("playerKills", pStats.playerKills);
            } else {
                stats.put("blocksBroken", 0);
                stats.put("mobsKilled", 0);
                stats.put("playtimeMinutes", 0);
                stats.put("deaths", 0);
                stats.put("playerKills", 0);
            }

            // Jobs Level Total
            int globalJobLevel = 0;
            fr.gens.core.modules.jobs.JobsModule jobsModule = (fr.gens.core.modules.jobs.JobsModule) plugin.getModuleManager().getModule("jobs");
            if (jobsModule != null) {
                globalJobLevel = jobsModule.getJobsDAO().getTotalJobLevel(UUID.fromString(uuidStr));
            }
            stats.put("globalJobLevel", globalJobLevel);

            // Recent Transactions (Limit 5)
            stats.put("recentTransactions", webDAO.getRecentTransactions(uuidStr));

            // Quests Activity Graph (last 7 days)
            long oneDay = 86400000L;
            long todayStart = (System.currentTimeMillis() / oneDay) * oneDay;
            stats.put("questsActivity", webDAO.getQuestsActivity(uuidStr, todayStart, oneDay));

            ctx.json(stats);
        });

        get("/api/player/info", ctx -> {
            String uuidStr = ctx.queryParam("uuid");
            if (uuidStr == null) {
                ctx.status(400).json("UUID manquant");
                return;
            }
            java.util.concurrent.CompletableFuture<Boolean> futureOp = new java.util.concurrent.CompletableFuture<>();
            plugin.getFoliaLib().getScheduler().runNextTick((t2) -> {
                org.bukkit.OfflinePlayer op = Bukkit.getOfflinePlayer(UUID.fromString(uuidStr));
                futureOp.complete(op != null && op.isOp());
            });
            boolean isOp = futureOp.join();
            ctx.json(Map.of("isOp", isOp));
        });

        get("/api/games/config", ctx -> {
            boolean wheelEnabled = plugin.getConfigManager().getConfig("modules/minigames.yml").getBoolean("minigames.wheel.enabled", true);
            boolean casinoEnabled = plugin.getConfigManager().getConfig("modules/minigames.yml").getBoolean("minigames.casino.enabled", true);
            ctx.json(Map.of("wheelEnabled", wheelEnabled, "casinoEnabled", casinoEnabled));
        });

        get("/api/games/casino/inventory", ctx -> {
            String uuidStr = ctx.queryParam("uuid"); // Allow query param, no auth needed to view inventory (only to play)
            if (uuidStr == null) {
                ctx.status(400).json("UUID manquant");
                return;
            }
            ctx.json(webDAO.getCasinoInventory(uuidStr));
        });

        post("/api/games/casino/play", ctx -> {
            CasinoPlayRequest req = ctx.bodyAsClass(CasinoPlayRequest.class);
            // Requete scurisee via l'intercepteur de WebManager
            String sessionUuid = ctx.attribute("playerUuid"); 
            
            if (sessionUuid == null || req.betId <= 0) {
                ctx.status(400).json(Map.of("error", "Requete invalide"));
                return;
            }

            // Check cooldown (1h)
            long lastPlayed = webDAO.getMinigameLastPlayed(sessionUuid, "casino");
            long now = System.currentTimeMillis();
            if (now - lastPlayed < 60 * 60 * 1000L) { // 1 heure
                long timeLeft = (60 * 60 * 1000L) - (now - lastPlayed);
                long minutes = timeLeft / (60 * 1000L);
                long seconds = (timeLeft % (60 * 1000L)) / 1000L;
                ctx.status(400).json(Map.of("error", "La machine surchauffe ! Revenez dans " + minutes + "m " + seconds + "s."));
                return;
            }

            // Update cooldown
            webDAO.updateMinigameLastPlayed(sessionUuid, "casino", now);

            // Casino Logic with Transaction via DAO
            Map<String, Object> result = webDAO.playCasino(sessionUuid, req.betId);
            if (result.containsKey("error")) {
                ctx.status(400).json(result);
            } else {
                ctx.json(result);
            }
        });

        get("/api/games/wheel", ctx -> {
            List<WheelReward> activeRewards = getActiveWheelRewards();
            ctx.json(activeRewards);
        });
        
        post("/api/games/play", ctx -> {
            PlayRequest req = ctx.bodyAsClass(PlayRequest.class);
            String sessionUuidStr = ctx.attribute("playerUuid"); // Secure
            if (sessionUuidStr == null || req.gameId == null) {
                ctx.status(400).json(Map.of("error", "Requete invalide"));
                return;
            }
            
            UUID playerUUID = UUID.fromString(sessionUuidStr);
            
            // Check cooldown (24h)
            long lastPlayed = webDAO.getMinigameLastPlayed(sessionUuidStr, req.gameId);
            long now = System.currentTimeMillis();
            if (now - lastPlayed < 24 * 60 * 60 * 1000L) {
                long timeLeft = (24 * 60 * 60 * 1000L) - (now - lastPlayed);
                long hours = timeLeft / (60 * 60 * 1000L);
                long minutes = (timeLeft % (60 * 60 * 1000L)) / (60 * 1000L);
                ctx.status(400).json(Map.of("error", "Vous avez d\u00e9j\u00e0 jou\u00e9 aujourd'hui ! Revenez dans " + hours + "h " + minutes + "m."));
                return;
            }

            // Update cooldown
            webDAO.updateMinigameLastPlayed(sessionUuidStr, req.gameId, now);

            List<WheelReward> activeRewards = getActiveWheelRewards();
            if (activeRewards.isEmpty()) {
                ctx.status(400).json(Map.of("error", "Aucune r\u00e9compense configur\u00e9e."));
                return;
            }

            // Determine reward with weights
            int totalWeight = activeRewards.stream().mapToInt(r -> r.chance).sum();
            int randomVal = new Random().nextInt(totalWeight);
            int currentSum = 0;
            WheelReward wonReward = activeRewards.get(0);
            int wonIndex = 0;
            
            for (int i = 0; i < activeRewards.size(); i++) {
                currentSum += activeRewards.get(i).chance;
                if (randomVal < currentSum) {
                    wonReward = activeRewards.get(i);
                    wonIndex = i;
                    break;
                }
            }

            String rewardCommand = wonReward.command;
            String rewardMessage = "Vous avez gagn\u00e9 : " + wonReward.name + " !";
            
            // Si le joueur est en ligne, on ex\u00e9cute, sinon on met en attente
            Player target = Bukkit.getPlayer(playerUUID);
            if (target != null && target.isOnline()) {
                plugin.getFoliaLib().getScheduler().runNextTick((t2) -> {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), rewardCommand.replace("%player%", target.getName()));
                    target.sendMessage(fr.gens.core.utils.PlaceholderUtils.parseToComponent("<green>[Web] " + rewardMessage));
                });
            } else {
                fr.gens.core.database.PendingCommandDAO pcd = new fr.gens.core.database.PendingCommandDAO(plugin);
                pcd.initDatabase(); // just to ensure table exists
                pcd.addPendingCommand(playerUUID, rewardCommand, rewardMessage);
            }

            ctx.json(Map.of("success", true, "message", rewardMessage, "prizeIndex", wonIndex));
        });
    }

    private List<WheelReward> getActiveWheelRewards() {
        List<WheelReward> active = new ArrayList<>();
        fr.gens.core.modules.Module eco = plugin.getModuleManager().getModule("economy");
        boolean ecoEnabled = eco != null && eco.isEnabled();
        
        for (WheelReward r : wheelRewards) {
            if (!ecoEnabled && r.command.contains("eco give")) {
                continue;
            }
            active.add(r);
        }
        return active;
    }

    public static class WheelReward {
        public String name;
        public String command;
        public int chance;
        public String color;
        
        public WheelReward(String name, String command, int chance, String color) {
            this.name = name;
            this.command = command;
            this.chance = chance;
            this.color = color;
        }
    }

    public static class CasinoPlayRequest {
        public int betId;
    }

    public static class LoginRequest {
        public String username;
        public String password;
    }

    public static class PlayRequest {
        public String gameId;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        fr.gens.core.database.PendingCommandDAO pcd = new fr.gens.core.database.PendingCommandDAO(plugin);
        pcd.initDatabase(); // just to ensure table exists
        pcd.processPendingCommands(player);
        
        plugin.getFoliaLib().getScheduler().runAsync((wrappedTask) -> {
            try (Connection conn = plugin.getDatabaseManager().getConnection()) {
                try (PreparedStatement profileStmt = conn.prepareStatement("INSERT OR REPLACE INTO player_profiles (uuid, username) VALUES (?, ?)")) {
                    profileStmt.setString(1, player.getUniqueId().toString());
                    profileStmt.setString(2, player.getName());
                    profileStmt.executeUpdate();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}



