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
import fr.gens.core.utils.PlaceholderUtils;

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
        new fr.gens.core.database.PendingCommandDAO(plugin).initDatabase();
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
            
            plugin.getConfigManager().saveConfigAsync("modules/minigames.yml");
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
            String remoteIp = ctx.ip();
            boolean isLocalProxy = "127.0.0.1".equals(remoteIp) || "0:0:0:0:0:0:0:1".equals(remoteIp) || "::1".equals(remoteIp);
            String forwarded = ctx.header("X-Forwarded-For");
            String ip = (isLocalProxy && forwarded != null && !forwarded.isEmpty()) ? forwarded.split(",")[0].trim() : remoteIp;
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

            // Trouver le joueur de manière non-bloquante (support des préfixes Bedrock Floodgate '.' ou '*')
            Player targetOnline = Bukkit.getPlayerExact(req.username);
            if (targetOnline == null && !req.username.startsWith(".")) {
                targetOnline = Bukkit.getPlayerExact("." + req.username);
            }

            if (targetOnline != null) {
                playerUUID = targetOnline.getUniqueId();
                isOp = targetOnline.isOp();
            } else {
                // NOTE ARCHITECTURALE (Support Bedrock hors-ligne) :
                // Les joueurs Bedrock via Floodgate possèdent un UUID dérivé de leur XUID et ont souvent un préfixe '.'
                // On recherche d'abord dans player_profiles avec le pseudo tel quel, puis avec/sans le préfixe Bedrock.
                playerUUID = webDAO.getPlayerUuidByUsername(req.username);
                if (playerUUID == null) {
                    if (req.username.startsWith(".")) {
                        playerUUID = webDAO.getPlayerUuidByUsername(req.username.substring(1));
                    } else {
                        playerUUID = webDAO.getPlayerUuidByUsername("." + req.username);
                    }
                }

                if (playerUUID != null) {
                    org.bukkit.OfflinePlayer offlineTarget = Bukkit.getOfflinePlayer(playerUUID);
                    isOp = offlineTarget.isOp();
                } else {
                    // Fallback offline UUID standard pour les comptes Java hors-ligne
                    playerUUID = UUID.nameUUIDFromBytes(("OfflinePlayer:" + req.username).getBytes(java.nio.charset.StandardCharsets.UTF_8));
                    org.bukkit.OfflinePlayer offlineTarget = Bukkit.getOfflinePlayer(playerUUID);
                    isOp = (offlineTarget != null && offlineTarget.isOp());
                }
            }

            if (playerUUID == null) {
                webManager.playerLoginRateLimit.put(ip, attempts + 1);
                ctx.status(401).json(Map.of("error", "Joueur introuvable."));
                return;
            }

            AuthModule authModule = (AuthModule) plugin.getModuleManager().getModule("auth");
            AuthDAO.AuthData data = authModule != null ? authModule.getAuthDAO().getAuthData(playerUUID) : null;
            if (data == null) {
                webManager.playerLoginRateLimit.put(ip, attempts + 1);
                ctx.status(401).json(Map.of("error", "Aucun compte enregistré (/register en jeu)."));
                return;
            }

            boolean isPasswordCorrect = false;
            boolean isLegacy = false;
            if (data.hash.startsWith("$2a$") || data.hash.startsWith("$2b$") || data.hash.startsWith("$2y$")) {
                isPasswordCorrect = org.mindrot.jbcrypt.BCrypt.checkpw(req.password, data.hash);
            } else {
                String hashedInput = AuthModule.oldHashPassword(req.password, data.salt);
                isPasswordCorrect = hashedInput.equals(data.hash);
                isLegacy = true;
            }

            if (!isPasswordCorrect) {
                webManager.playerLoginRateLimit.put(ip, attempts + 1);
                ctx.status(401).json(Map.of("error", "Mot de passe incorrect."));
                return;
            }

            // Migration transparente du mot de passe legacy vers BCrypt
            if (isLegacy && authModule != null) {
                String newBcryptHash = org.mindrot.jbcrypt.BCrypt.hashpw(req.password, org.mindrot.jbcrypt.BCrypt.gensalt());
                authModule.getAuthDAO().updatePassword(playerUUID, newBcryptHash, "");
            }

            webManager.playerLoginRateLimit.remove(ip);

            // Generer le VRAI token securise
            String token = UUID.randomUUID().toString() + "-" + System.currentTimeMillis();
            long expiry = System.currentTimeMillis() + (7L * 24 * 60 * 60 * 1000L); // 7 jours
            
            // Sauvegarde RAM + Persistance SQLite
            webManager.activePlayerSessions.put(token, playerUUID.toString());
            webManager.playerSessionExpiry.put(token, expiry);
            final String finalToken = token;
            final String finalUuid = playerUUID.toString();
            plugin.getFoliaLib().getScheduler().runAsync((t) -> webDAO.savePlayerSession(finalToken, finalUuid, expiry));
            
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
            
            fr.gens.core.modules.BedrockSkinModule skinModule = (fr.gens.core.modules.BedrockSkinModule) plugin.getModuleManager().getModule("bedrockskin");
            if (skinModule != null) {
                ctx.redirect(skinModule.getHeadUrl(uuid, name));
                return;
            }
            
            String cleanName = name.startsWith(".") ? name.substring(1) : name;
            ctx.redirect("https://crafthead.net/avatar/" + cleanName);
        });

        get("/api/head/{name}/{size}", ctx -> {
            String name = ctx.pathParam("name");
            String size = ctx.pathParam("size");
            UUID uuid = webDAO.getPlayerUuidByUsername(name);
            
            fr.gens.core.modules.BedrockSkinModule skinModule = (fr.gens.core.modules.BedrockSkinModule) plugin.getModuleManager().getModule("bedrockskin");
            String cleanName = name.startsWith(".") ? name.substring(1) : name;
            if (skinModule != null) {
                String baseUrl = skinModule.getHeadUrl(uuid, name);
                // Si l'URL contient mc-heads.net, on gère la taille avec le paramètre de taille
                if (baseUrl.contains("mc-heads.net")) {
                    ctx.redirect(baseUrl.replace(".png", "") + "/" + size);
                } else {
                    // Pour crafthead, il faut insérer la taille et utiliser le pseudo car c'est un serveur crack
                    ctx.redirect("https://crafthead.net/avatar/" + cleanName + "/" + size);
                }
                return;
            }
            
            ctx.redirect("https://crafthead.net/avatar/" + cleanName + "/" + size);
        });

        get("/api/player/balance", ctx -> {
            String uuidStr = ctx.queryParam("uuid");
            if (uuidStr == null || uuidStr.trim().isEmpty()) {
                uuidStr = webManager.getPlayerUuidFromCtx(ctx);
            }
            if (uuidStr == null || uuidStr.trim().isEmpty()) {
                uuidStr = ctx.header("X-Player-UUID");
            }
            if (uuidStr == null || uuidStr.trim().isEmpty()) {
                ctx.status(400).json(Map.of("error", "UUID manquant"));
                return;
            }

            double balance = getLiveBalance(uuidStr);
            ctx.json(Map.of(
                "success", true,
                "uuid", uuidStr,
                "balance", balance
            ));
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
            stats.put("balance", getLiveBalance(uuidStr));

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
            org.bukkit.OfflinePlayer op = Bukkit.getOfflinePlayer(UUID.fromString(uuidStr));
            boolean isOp = (op != null && op.isOp());
            ctx.json(Map.of("isOp", isOp));
        });

        get("/api/games/config", ctx -> {
            fr.gens.core.modules.Module minigamesModule = plugin.getModuleManager().getModule("minigames");
            boolean moduleEnabled = minigamesModule != null 
                    ? minigamesModule.isEnabled() 
                    : plugin.getConfigManager().getConfig("modules.yml").getBoolean("modules.minigame", true);
            boolean wheelConfig = plugin.getConfigManager().getConfig("modules/minigames.yml").getBoolean("minigames.wheel.enabled", true);
            boolean casinoConfig = plugin.getConfigManager().getConfig("modules/minigames.yml").getBoolean("minigames.casino.enabled", true);
            boolean coinflipConfig = plugin.getConfigManager().getConfig("modules/minigames.yml").getBoolean("minigames.coinflip.enabled", true);
            boolean wheelEnabled = moduleEnabled && wheelConfig;
            boolean casinoEnabled = moduleEnabled && casinoConfig;
            boolean coinflipEnabled = moduleEnabled && coinflipConfig;
            boolean totalEnabled = moduleEnabled && (wheelConfig || casinoConfig || coinflipConfig);

            Map<String, Object> resp = new HashMap<>();
            resp.put("wheelEnabled", wheelEnabled);
            resp.put("casinoEnabled", casinoEnabled);
            resp.put("coinflipEnabled", coinflipEnabled);
            resp.put("enabled", totalEnabled);
            ctx.json(resp);
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
            fr.gens.core.modules.Module minigamesModule = plugin.getModuleManager().getModule("minigames");
            if (minigamesModule != null && !minigamesModule.isEnabled()) {
                ctx.json(Map.of("error", "Les mini-jeux sont actuellement désactivés par l'administration."));
                return;
            }
            boolean casinoEnabled = plugin.getConfigManager().getConfig("modules/minigames.yml").getBoolean("minigames.casino.enabled", true);
            if (!casinoEnabled) {
                ctx.json(Map.of("error", "La machine à sous est actuellement désactivée."));
                return;
            }
            CasinoPlayRequest req = ctx.bodyAsClass(CasinoPlayRequest.class);
            // Requete securisee via l'intercepteur de WebManager
            String sessionUuid = ctx.attribute("playerUuid"); 
            
            if (sessionUuid == null || req.betId <= 0) {
                ctx.json(Map.of("error", "Requete invalide"));
                return;
            }

            // Check cooldown (1h)
            long lastPlayed = webDAO.getMinigameLastPlayed(sessionUuid, "casino");
            long now = System.currentTimeMillis();
            if (now - lastPlayed < 60 * 60 * 1000L) { // 1 heure
                long timeLeft = (60 * 60 * 1000L) - (now - lastPlayed);
                long minutes = timeLeft / (60 * 1000L);
                long seconds = (timeLeft % (60 * 1000L)) / 1000L;
                ctx.json(Map.of("error", "La machine surchauffe ! Revenez dans " + minutes + "m " + seconds + "s."));
                return;
            }

            // Update cooldown
            webDAO.updateMinigameLastPlayed(sessionUuid, "casino", now);

            // Casino Logic with Transaction via DAO
            Map<String, Object> result = webDAO.playCasino(sessionUuid, req.betId);
            ctx.json(result);
        });

        post("/api/games/coinflip/play", ctx -> {
            fr.gens.core.modules.Module minigamesModule = plugin.getModuleManager().getModule("minigames");
            if (minigamesModule != null && !minigamesModule.isEnabled()) {
                ctx.json(Map.of("error", "Les mini-jeux sont actuellement désactivés par l'administration."));
                return;
            }
            boolean coinflipEnabled = plugin.getConfigManager().getConfig("modules/minigames.yml").getBoolean("minigames.coinflip.enabled", true);
            if (!coinflipEnabled) {
                ctx.json(Map.of("error", "Le Pile ou Face est actuellement désactivé."));
                return;
            }
            CoinFlipPlayRequest req = ctx.bodyAsClass(CoinFlipPlayRequest.class);
            String sessionUuid = ctx.attribute("playerUuid");
            if (sessionUuid == null || req.betId <= 0 || req.choice == null) {
                ctx.json(Map.of("error", "Requête invalide"));
                return;
            }

            // Cooldown de 5s pour coinflip
            long lastPlayed = webDAO.getMinigameLastPlayed(sessionUuid, "coinflip");
            long now = System.currentTimeMillis();
            if (now - lastPlayed < 5 * 1000L) {
                long timeLeft = (5 * 1000L) - (now - lastPlayed);
                ctx.json(Map.of("error", "Patientez " + (timeLeft / 1000L + 1) + "s avant de relancer la pièce."));
                return;
            }
            webDAO.updateMinigameLastPlayed(sessionUuid, "coinflip", now);

            Map<String, Object> result = webDAO.playCoinFlip(sessionUuid, req.betId, req.choice);
            ctx.json(result);
        });

        get("/api/games/wheel", ctx -> {
            fr.gens.core.modules.Module minigamesModule = plugin.getModuleManager().getModule("minigames");
            if (minigamesModule != null && !minigamesModule.isEnabled()) {
                ctx.json(java.util.Collections.emptyList());
                return;
            }
            List<WheelReward> activeRewards = getActiveWheelRewards();
            ctx.json(activeRewards);
        });
        
        post("/api/games/play", ctx -> {
            fr.gens.core.modules.Module minigamesModule = plugin.getModuleManager().getModule("minigames");
            if (minigamesModule != null && !minigamesModule.isEnabled()) {
                ctx.json(Map.of("error", "Les mini-jeux sont actuellement désactivés par l'administration."));
                return;
            }
            boolean wheelEnabled = plugin.getConfigManager().getConfig("modules/minigames.yml").getBoolean("minigames.wheel.enabled", true);
            if (!wheelEnabled) {
                ctx.json(Map.of("error", "La roue de la fortune est actuellement désactivée."));
                return;
            }
            PlayRequest req = ctx.bodyAsClass(PlayRequest.class);
            String sessionUuidStr = ctx.attribute("playerUuid"); // Secure
            if (sessionUuidStr == null || req.gameId == null) {
                ctx.json(Map.of("error", "Requete invalide"));
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
                ctx.json(Map.of("error", "Vous avez d\u00e9j\u00e0 jou\u00e9 aujourd'hui ! Revenez dans " + hours + "h " + minutes + "m."));
                return;
            }

            // Update cooldown
            webDAO.updateMinigameLastPlayed(sessionUuidStr, req.gameId, now);

            List<WheelReward> activeRewards = getActiveWheelRewards();
            if (activeRewards.isEmpty()) {
                ctx.json(Map.of("error", "Aucune r\u00e9compense configur\u00e9e."));
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
                String finalCmd = rewardCommand.replace("%player%", target.getName());
                // Sur Folia, dispatchCommand avec la console doit s'exécuter sur le GlobalRegionScheduler
                plugin.getFoliaLib().getScheduler().runNextTick((gt) -> {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), finalCmd);
                });
                plugin.getFoliaLib().getScheduler().runAtEntity(target, (t2) -> {
                    if (target.isOnline()) {
                        target.sendMessage(PlaceholderUtils.parseToComponent("<green>[Web] " + rewardMessage));
                    }
                });
            } else {
                fr.gens.core.database.PendingCommandDAO pcd = new fr.gens.core.database.PendingCommandDAO(plugin);
                pcd.initDatabase(); // just to ensure table exists
                pcd.addPendingCommand(playerUUID, rewardCommand, rewardMessage);
            }

            ctx.json(Map.of("success", true, "message", rewardMessage, "prizeIndex", wonIndex));
        });

        // --- GUILD / TEAM REST ENDPOINTS ---

        get("/api/player/team", ctx -> {
            String uuidStr = webManager.getPlayerUuidFromCtx(ctx);
            if (uuidStr == null) {
                ctx.status(401).json(Map.of("error", "Non authentifie."));
                return;
            }

            UUID playerUuid = UUID.fromString(uuidStr);
            fr.gens.core.modules.teams.TeamData team = plugin.getTeamManager().getPlayerTeam(playerUuid);
            if (team == null) {
                ctx.json(Map.of("hasTeam", false));
                return;
            }

            fr.gens.core.modules.EconomyModule ecoMod = (fr.gens.core.modules.EconomyModule) plugin.getModuleManager().getModule("economy");
            boolean ecoEnabled = (ecoMod != null && ecoMod.isEnabled());

            fr.gens.core.modules.teams.TeamClaimManager claimMgr = plugin.getTeamManager().getClaimManager();
            int currentClaims = claimMgr != null ? claimMgr.getClaimsCount(team.getTeamId()) : 0;

            List<Map<String, Object>> membersList = new ArrayList<>();
            for (UUID mUuid : team.getMembers()) {
                org.bukkit.OfflinePlayer op = Bukkit.getOfflinePlayer(mUuid);
                String name = op.getName() != null ? op.getName() : webDAO.getPlayerUsernameByUuid(mUuid);
                if (name == null) name = "Inconnu";
                boolean isLeader = team.getLeaderUuid().equals(mUuid);
                boolean isAdm = team.isAdmin(mUuid);
                membersList.add(Map.of(
                    "uuid", mUuid.toString(),
                    "username", name,
                    "isLeader", isLeader,
                    "isAdmin", isAdm,
                    "role", team.getRoleName(mUuid)
                ));
            }

            Map<String, Object> teamInfo = new HashMap<>();
            teamInfo.put("hasTeam", true);
            teamInfo.put("teamId", team.getTeamId());
            teamInfo.put("name", team.getName());
            teamInfo.put("leaderUuid", team.getLeaderUuid().toString());
            teamInfo.put("isLeader", team.getLeaderUuid().equals(playerUuid));
            teamInfo.put("isAdmin", team.isAdmin(playerUuid));
            teamInfo.put("canManage", team.isAdminOrLeader(playerUuid));
            teamInfo.put("bankBalance", team.getBankBalance());
            teamInfo.put("bankXp", team.getBankXp());
            teamInfo.put("color", team.getColor());
            teamInfo.put("isEconomyEnabled", ecoEnabled);
            teamInfo.put("currentClaims", currentClaims);
            teamInfo.put("maxClaims", team.getMaxClaims());
            teamInfo.put("maxMembers", team.getMaxMembers());
            teamInfo.put("jobsXpMultiplier", team.getJobsXpMultiplier());
            teamInfo.put("ahTaxReduction", team.getAhTaxReduction());
            teamInfo.put("questPointsMultiplier", team.getQuestPointsMultiplier());
            teamInfo.put("upgrades", team.getUpgrades());
            teamInfo.put("members", membersList);

            ctx.json(teamInfo);
        });

        post("/api/player/team/deposit", ctx -> {
            String uuidStr = webManager.getPlayerUuidFromCtx(ctx);
            if (uuidStr == null) {
                ctx.status(401).json(Map.of("error", "Non authentifie."));
                return;
            }

            UUID playerUuid = UUID.fromString(uuidStr);
            fr.gens.core.modules.teams.TeamData team = plugin.getTeamManager().getPlayerTeam(playerUuid);
            if (team == null) {
                ctx.status(400).json(Map.of("error", "Vous n'appartenez a aucune guilde."));
                return;
            }

            TeamDepositRequest req = ctx.bodyAsClass(TeamDepositRequest.class);
            fr.gens.core.modules.EconomyModule ecoMod = (fr.gens.core.modules.EconomyModule) plugin.getModuleManager().getModule("economy");
            boolean ecoEnabled = (ecoMod != null && ecoMod.isEnabled());

            if ("xp".equalsIgnoreCase(req.type)) {
                int levels = (int) Math.floor(req.amount);
                if (levels <= 0) {
                    ctx.status(400).json(Map.of("error", "Montant d'XP invalide."));
                    return;
                }
                Player onlineP = Bukkit.getPlayer(playerUuid);
                if (onlineP == null || !onlineP.isOnline()) {
                    ctx.status(400).json(Map.of("error", "Vous devez etre connecte en jeu pour deposer vos niveaux d'XP."));
                    return;
                }
                if (onlineP.getLevel() < levels) {
                    ctx.status(400).json(Map.of("error", "Niveau d'XP insuffisant (actuel : " + onlineP.getLevel() + ")."));
                    return;
                }
                plugin.getFoliaLib().getScheduler().runAtEntity(onlineP, task -> {
                    if (onlineP.getLevel() >= levels) {
                        onlineP.setLevel(onlineP.getLevel() - levels);
                        team.addBankXp(levels);
                        plugin.getTeamManager().saveBankAsync(team);
                        onlineP.sendMessage(fr.gens.core.utils.PlaceholderUtils.parseToComponent("<green>[Guilde] Vous avez depose <yellow>" + levels + " niveaux d'XP<green> dans la banque de guilde."));
                    }
                });
                ctx.json(Map.of("success", true, "bankXp", team.getBankXp() + levels));
                return;
            } else {
                if (!ecoEnabled) {
                    ctx.status(400).json(Map.of("error", "L'economie est desactivee sur ce serveur. Utilisez l'XP."));
                    return;
                }
                double amount = req.amount;
                if (amount <= 0 || Double.isNaN(amount) || Double.isInfinite(amount)) {
                    ctx.status(400).json(Map.of("error", "Montant d'argent invalide."));
                    return;
                }
                if (ecoMod.getBalance(playerUuid) < amount) {
                    ctx.status(400).json(Map.of("error", "Solde personnel insuffisant."));
                    return;
                }
                ecoMod.takeMoney(playerUuid, amount);
                team.addBankBalance(amount);
                plugin.getTeamManager().saveBankAsync(team);

                Player onlineP = Bukkit.getPlayer(playerUuid);
                if (onlineP != null && onlineP.isOnline()) {
                    onlineP.sendMessage(fr.gens.core.utils.PlaceholderUtils.parseToComponent("<green>[Guilde] Vous avez depose <gold>" + String.format("%.2f", amount) + " $<green> dans la banque de guilde."));
                }
                ctx.json(Map.of("success", true, "bankBalance", team.getBankBalance()));
                return;
            }
        });

        post("/api/player/team/withdraw", ctx -> {
            String uuidStr = webManager.getPlayerUuidFromCtx(ctx);
            if (uuidStr == null) {
                ctx.status(401).json(Map.of("error", "Non authentifie."));
                return;
            }

            UUID playerUuid = UUID.fromString(uuidStr);
            fr.gens.core.modules.teams.TeamData team = plugin.getTeamManager().getPlayerTeam(playerUuid);
            if (team == null) {
                ctx.status(400).json(Map.of("error", "Vous n'appartenez a aucune guilde."));
                return;
            }

            if (!team.isAdminOrLeader(playerUuid)) {
                ctx.status(403).json(Map.of("error", "Seuls le chef et les administrateurs de guilde peuvent retirer des fonds de la banque."));
                return;
            }

            TeamDepositRequest req = ctx.bodyAsClass(TeamDepositRequest.class);
            fr.gens.core.modules.EconomyModule ecoMod = (fr.gens.core.modules.EconomyModule) plugin.getModuleManager().getModule("economy");
            boolean ecoEnabled = (ecoMod != null && ecoMod.isEnabled());

            if ("xp".equalsIgnoreCase(req.type)) {
                int levels = (int) Math.floor(req.amount);
                if (levels <= 0) {
                    ctx.status(400).json(Map.of("error", "Montant d'XP invalide."));
                    return;
                }
                if (team.getBankXp() < levels) {
                    ctx.status(400).json(Map.of("error", "Solde XP de la banque insuffisant."));
                    return;
                }
                Player onlineP = Bukkit.getPlayer(playerUuid);
                if (onlineP == null || !onlineP.isOnline()) {
                    ctx.status(400).json(Map.of("error", "Vous devez etre connecte en jeu pour recevoir vos niveaux d'XP."));
                    return;
                }
                boolean withdrawn = team.withdrawBankXp(levels);
                if (!withdrawn) {
                    ctx.status(400).json(Map.of("error", "Echec du retrait d'XP."));
                    return;
                }
                plugin.getFoliaLib().getScheduler().runAtEntity(onlineP, task -> {
                    onlineP.setLevel(onlineP.getLevel() + levels);
                    onlineP.sendMessage(fr.gens.core.utils.PlaceholderUtils.parseToComponent("<green>[Guilde] Vous avez retire <yellow>" + levels + " niveaux d'XP<green> de la banque de guilde."));
                });
                plugin.getTeamManager().saveBankAsync(team);
                ctx.json(Map.of("success", true, "bankXp", team.getBankXp()));
                return;
            } else {
                if (!ecoEnabled) {
                    ctx.status(400).json(Map.of("error", "L'economie est desactivee sur ce serveur. Utilisez l'XP."));
                    return;
                }
                double amount = req.amount;
                if (amount <= 0 || Double.isNaN(amount) || Double.isInfinite(amount)) {
                    ctx.status(400).json(Map.of("error", "Montant d'argent invalide."));
                    return;
                }
                if (team.getBankBalance() < amount) {
                    ctx.status(400).json(Map.of("error", "Solde d'argent de la banque insuffisant."));
                    return;
                }
                boolean withdrawn = team.withdrawBankBalance(amount);
                if (!withdrawn) {
                    ctx.status(400).json(Map.of("error", "Echec du retrait."));
                    return;
                }
                ecoMod.addMoney(playerUuid, amount);
                plugin.getTeamManager().saveBankAsync(team);

                Player onlineP = Bukkit.getPlayer(playerUuid);
                if (onlineP != null && onlineP.isOnline()) {
                    onlineP.sendMessage(fr.gens.core.utils.PlaceholderUtils.parseToComponent("<green>[Guilde] Vous avez retire <gold>" + String.format("%.2f", amount) + " $<green> de la banque de guilde."));
                }
                ctx.json(Map.of("success", true, "bankBalance", team.getBankBalance()));
                return;
            }
        });

        post("/api/player/team/upgrade", ctx -> {
            String uuidStr = webManager.getPlayerUuidFromCtx(ctx);
            if (uuidStr == null) {
                ctx.status(401).json(Map.of("error", "Non authentifie."));
                return;
            }

            UUID playerUuid = UUID.fromString(uuidStr);
            fr.gens.core.modules.teams.TeamData team = plugin.getTeamManager().getPlayerTeam(playerUuid);
            if (team == null) {
                ctx.status(400).json(Map.of("error", "Vous n'appartenez a aucune guilde."));
                return;
            }

            if (!team.isAdminOrLeader(playerUuid)) {
                ctx.status(403).json(Map.of("error", "Seuls le chef et les administrateurs de guilde peuvent acheter des ameliorations."));
                return;
            }

            TeamUpgradeRequest req = ctx.bodyAsClass(TeamUpgradeRequest.class);
            if (req.perkId == null || req.perkId.trim().isEmpty()) {
                ctx.status(400).json(Map.of("error", "Identifiant d'amelioration manquant."));
                return;
            }

            boolean success = plugin.getTeamManager().buyPerk(team, req.perkId);
            if (success) {
                ctx.json(Map.of(
                    "success", true,
                    "perkId", req.perkId.toUpperCase(),
                    "newLevel", team.getUpgradeLevel(req.perkId),
                    "bankBalance", team.getBankBalance(),
                    "bankXp", team.getBankXp()
                ));
            } else {
                ctx.status(400).json(Map.of("error", "Fonds insuffisants dans la banque de guilde ou niveau maximum deja atteint."));
            }
        });

        post("/api/player/team/color", ctx -> {
            String uuidStr = webManager.getPlayerUuidFromCtx(ctx);
            if (uuidStr == null) {
                ctx.status(401).json(Map.of("error", "Non authentifie."));
                return;
            }

            UUID playerUuid = UUID.fromString(uuidStr);
            fr.gens.core.modules.teams.TeamData team = plugin.getTeamManager().getPlayerTeam(playerUuid);
            if (team == null) {
                ctx.status(400).json(Map.of("error", "Vous n'appartenez a aucune guilde."));
                return;
            }

            if (!team.isAdminOrLeader(playerUuid)) {
                ctx.status(403).json(Map.of("error", "Seuls le chef et les administrateurs de guilde peuvent modifier la couleur de la guilde."));
                return;
            }

            TeamColorRequest req = ctx.bodyAsClass(TeamColorRequest.class);
            if (req.color == null || !req.color.matches("^#([A-Fa-f0-9]{6})$")) {
                ctx.status(400).json(Map.of("error", "Format de couleur invalide (attendu : #RRGGBB)."));
                return;
            }

            team.setColor(req.color);
            plugin.getTeamManager().saveColorAsync(team);

            fr.gens.core.modules.BlueMapModule bm = (fr.gens.core.modules.BlueMapModule) plugin.getModuleManager().getModule("bluemap");
            if (bm != null) {
                bm.updateAllTeamTerritories();
            }

            ctx.json(Map.of("success", true, "color", team.getColor()));
        });

        post("/api/player/team/promote", ctx -> {
            String uuidStr = webManager.getPlayerUuidFromCtx(ctx);
            if (uuidStr == null) {
                ctx.status(401).json(Map.of("error", "Non authentifie."));
                return;
            }

            UUID playerUuid = UUID.fromString(uuidStr);
            fr.gens.core.modules.teams.TeamData team = plugin.getTeamManager().getPlayerTeam(playerUuid);
            if (team == null) {
                ctx.status(400).json(Map.of("error", "Vous n'appartenez a aucune guilde."));
                return;
            }

            if (!team.isLeader(playerUuid)) {
                ctx.status(403).json(Map.of("error", "Seul le chef de guilde peut nommer des administrateurs."));
                return;
            }

            TeamMemberActionRequest req = ctx.bodyAsClass(TeamMemberActionRequest.class);
            if (req.uuid == null) {
                ctx.status(400).json(Map.of("error", "UUID du joueur manquant."));
                return;
            }

            UUID targetUuid;
            try {
                targetUuid = UUID.fromString(req.uuid);
            } catch (Exception e) {
                ctx.status(400).json(Map.of("error", "UUID invalide."));
                return;
            }

            if (!team.getMembers().contains(targetUuid)) {
                ctx.status(400).json(Map.of("error", "Ce joueur ne fait pas partie de votre guilde."));
                return;
            }

            if (team.isLeader(targetUuid)) {
                ctx.status(400).json(Map.of("error", "Le chef de guilde ne peut pas etre nomme administrateur."));
                return;
            }

            if (team.isAdmin(targetUuid)) {
                ctx.status(400).json(Map.of("error", "Ce membre est deja administrateur de la guilde."));
                return;
            }

            plugin.getTeamManager().promoteAdmin(team, targetUuid);

            org.bukkit.OfflinePlayer targetOp = Bukkit.getOfflinePlayer(targetUuid);
            String targetName = targetOp.getName() != null ? targetOp.getName() : "Un membre";
            org.bukkit.OfflinePlayer leaderOp = Bukkit.getOfflinePlayer(playerUuid);
            String leaderName = leaderOp.getName() != null ? leaderOp.getName() : "Le chef";
            team.broadcast("<gold><bold>" + targetName + "</bold> a ete promu <aqua>Administrateur</aqua> de la guilde par <yellow>" + leaderName + "</yellow> !");

            Player targetPlayer = Bukkit.getPlayer(targetUuid);
            if (targetPlayer != null && targetPlayer.isOnline()) {
                targetPlayer.sendMessage(fr.gens.core.utils.PlaceholderUtils.parseToComponent("<green><bold>Felicitation !</bold> Vous etes desormais Administrateur de la guilde.</green>"));
            }

            ctx.json(Map.of("success", true));
        });

        post("/api/player/team/demote", ctx -> {
            String uuidStr = webManager.getPlayerUuidFromCtx(ctx);
            if (uuidStr == null) {
                ctx.status(401).json(Map.of("error", "Non authentifie."));
                return;
            }

            UUID playerUuid = UUID.fromString(uuidStr);
            fr.gens.core.modules.teams.TeamData team = plugin.getTeamManager().getPlayerTeam(playerUuid);
            if (team == null) {
                ctx.status(400).json(Map.of("error", "Vous n'appartenez a aucune guilde."));
                return;
            }

            if (!team.isLeader(playerUuid)) {
                ctx.status(403).json(Map.of("error", "Seul le chef de guilde peut retrograder des administrateurs."));
                return;
            }

            TeamMemberActionRequest req = ctx.bodyAsClass(TeamMemberActionRequest.class);
            if (req.uuid == null) {
                ctx.status(400).json(Map.of("error", "UUID du joueur manquant."));
                return;
            }

            UUID targetUuid;
            try {
                targetUuid = UUID.fromString(req.uuid);
            } catch (Exception e) {
                ctx.status(400).json(Map.of("error", "UUID invalide."));
                return;
            }

            if (!team.getMembers().contains(targetUuid)) {
                ctx.status(400).json(Map.of("error", "Ce joueur ne fait pas partie de votre guilde."));
                return;
            }

            if (!team.isAdmin(targetUuid)) {
                ctx.status(400).json(Map.of("error", "Ce joueur n'est pas administrateur de la guilde."));
                return;
            }

            plugin.getTeamManager().demoteAdmin(team, targetUuid);

            org.bukkit.OfflinePlayer targetOp = Bukkit.getOfflinePlayer(targetUuid);
            String targetName = targetOp.getName() != null ? targetOp.getName() : "Un membre";
            org.bukkit.OfflinePlayer leaderOp = Bukkit.getOfflinePlayer(playerUuid);
            String leaderName = leaderOp.getName() != null ? leaderOp.getName() : "Le chef";
            team.broadcast("<yellow><bold>" + targetName + "</bold> a ete retrograde au rang de <white>Membre</white> par <gold>" + leaderName + "</gold>.");

            Player targetPlayer = Bukkit.getPlayer(targetUuid);
            if (targetPlayer != null && targetPlayer.isOnline()) {
                targetPlayer.sendMessage(fr.gens.core.utils.PlaceholderUtils.parseToComponent("<yellow>Vous n'etes plus administrateur de la guilde.</yellow>"));
            }

            ctx.json(Map.of("success", true));
        });

        post("/api/player/team/kick", ctx -> {
            String uuidStr = webManager.getPlayerUuidFromCtx(ctx);
            if (uuidStr == null) {
                ctx.status(401).json(Map.of("error", "Non authentifie."));
                return;
            }

            UUID playerUuid = UUID.fromString(uuidStr);
            fr.gens.core.modules.teams.TeamData team = plugin.getTeamManager().getPlayerTeam(playerUuid);
            if (team == null) {
                ctx.status(400).json(Map.of("error", "Vous n'appartenez a aucune guilde."));
                return;
            }

            TeamMemberActionRequest req = ctx.bodyAsClass(TeamMemberActionRequest.class);
            if (req.uuid == null) {
                ctx.status(400).json(Map.of("error", "UUID du joueur manquant."));
                return;
            }

            UUID targetUuid;
            try {
                targetUuid = UUID.fromString(req.uuid);
            } catch (Exception e) {
                ctx.status(400).json(Map.of("error", "UUID invalide."));
                return;
            }

            if (!team.getMembers().contains(targetUuid)) {
                ctx.status(400).json(Map.of("error", "Ce joueur ne fait pas partie de votre guilde."));
                return;
            }

            if (team.isLeader(targetUuid)) {
                ctx.status(403).json(Map.of("error", "Vous ne pouvez pas exclure le chef de guilde."));
                return;
            }

            if (!team.canManageMembers(playerUuid, targetUuid)) {
                ctx.status(403).json(Map.of("error", "Vous n'avez pas la permission d'exclure ce membre."));
                return;
            }

            org.bukkit.OfflinePlayer targetOp = Bukkit.getOfflinePlayer(targetUuid);
            String targetName = targetOp.getName() != null ? targetOp.getName() : "Un membre";
            org.bukkit.OfflinePlayer kickerOp = Bukkit.getOfflinePlayer(playerUuid);
            String kickerName = kickerOp.getName() != null ? kickerOp.getName() : "Un responsable";

            plugin.getTeamManager().removeMember(team, targetUuid);

            team.broadcast("<red><bold>" + targetName + "</bold> a ete exclu de la guilde par <yellow>" + kickerName + "</yellow>.");

            Player targetPlayer = Bukkit.getPlayer(targetUuid);
            if (targetPlayer != null && targetPlayer.isOnline()) {
                targetPlayer.sendMessage(fr.gens.core.utils.PlaceholderUtils.parseToComponent("<red>Vous avez ete exclu de la guilde " + team.getName() + ".</red>"));
            }

            ctx.json(Map.of("success", true));
        });

        // --- SOLO PERKS REST ENDPOINTS ---

        get("/api/player/perks", ctx -> {
            String uuidStr = webManager.getPlayerUuidFromCtx(ctx);
            if (uuidStr == null) {
                ctx.status(401).json(Map.of("error", "Non authentifie."));
                return;
            }

            fr.gens.core.modules.perks.SoloPerkModule mod = (fr.gens.core.modules.perks.SoloPerkModule) plugin.getModuleManager().getModule("solo_perks");
            if (mod == null || !mod.isEnabled()) {
                ctx.json(Map.of("enabled", false));
                return;
            }

            UUID uuid = UUID.fromString(uuidStr);
            Map<String, Object> data = mod.getManager().getPlayerDataForWeb(uuid);
            data.put("enabled", true);
            ctx.json(data);
        });

        post("/api/player/perks/claim", ctx -> {
            String uuidStr = webManager.getPlayerUuidFromCtx(ctx);
            if (uuidStr == null) {
                ctx.status(401).json(Map.of("error", "Non authentifie."));
                return;
            }

            fr.gens.core.modules.perks.SoloPerkModule mod = (fr.gens.core.modules.perks.SoloPerkModule) plugin.getModuleManager().getModule("solo_perks");
            if (mod == null || !mod.isEnabled()) {
                ctx.status(400).json(Map.of("error", "Le module de bonus individuels est desactive."));
                return;
            }

            PerkClaimRequest req = ctx.bodyAsClass(PerkClaimRequest.class);
            if (req.perkId == null || req.perkId.trim().isEmpty()) {
                ctx.status(400).json(Map.of("error", "Identifiant de bonus manquant."));
                return;
            }

            fr.gens.core.modules.perks.SoloPerkType perk = fr.gens.core.modules.perks.SoloPerkType.fromId(req.perkId);
            if (perk == null) {
                ctx.status(400).json(Map.of("error", "Bonus introuvable."));
                return;
            }

            UUID uuid = UUID.fromString(uuidStr);
            fr.gens.core.modules.perks.SoloPerkManager.UnlockResult res = mod.getManager().unlockPerk(uuid, perk);
            switch (res) {
                case SUCCESS -> ctx.json(Map.of("success", true, "perkId", perk.getId()));
                case ALREADY_UNLOCKED -> ctx.status(400).json(Map.of("error", "Vous possedez deja ce bonus."));
                case NOT_ENOUGH_QUESTS -> ctx.status(400).json(Map.of("error", "Nombre de quetes insuffisant (" + perk.getRequiredQuests() + " requises)."));
                case NOT_ENOUGH_FUNDS -> ctx.status(400).json(Map.of("error", "Fonds insuffisants."));
                case NOT_ENOUGH_XP -> ctx.status(400).json(Map.of("error", "Niveaux d'experience insuffisants."));
                default -> ctx.status(400).json(Map.of("error", "Une erreur est survenue lors du deblocage."));
            }
        });

        post("/api/player/perks/toggle", ctx -> {
            String uuidStr = webManager.getPlayerUuidFromCtx(ctx);
            if (uuidStr == null) {
                ctx.status(401).json(Map.of("error", "Non authentifie."));
                return;
            }

            fr.gens.core.modules.perks.SoloPerkModule mod = (fr.gens.core.modules.perks.SoloPerkModule) plugin.getModuleManager().getModule("solo_perks");
            if (mod == null || !mod.isEnabled()) {
                ctx.status(400).json(Map.of("error", "Le module de bonus individuels est desactive."));
                return;
            }

            PerkClaimRequest req = ctx.bodyAsClass(PerkClaimRequest.class);
            if (req.perkId == null || req.perkId.trim().isEmpty()) {
                ctx.status(400).json(Map.of("error", "Identifiant de bonus manquant."));
                return;
            }

            fr.gens.core.modules.perks.SoloPerkType perk = fr.gens.core.modules.perks.SoloPerkType.fromId(req.perkId);
            if (perk == null || !perk.isToggleable()) {
                ctx.status(400).json(Map.of("error", "Ce bonus n'est pas activable/desactivable."));
                return;
            }

            UUID uuid = UUID.fromString(uuidStr);
            if (!mod.getManager().hasPerk(uuid, perk)) {
                ctx.status(400).json(Map.of("error", "Vous ne possedez pas ce bonus."));
                return;
            }

            boolean newState = mod.getManager().togglePerk(uuid, perk);
            ctx.json(Map.of("success", true, "perkId", perk.getId(), "isEnabled", newState));
        });

        get("/api/public/claims", ctx -> {
            fr.gens.core.modules.BlueMapModule bm = (fr.gens.core.modules.BlueMapModule) plugin.getModuleManager().getModule("bluemap");
            if (bm != null) {
                ctx.json(bm.getClaimsMapData());
            } else {
                ctx.json(List.of());
            }
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

    public double getLiveBalance(String uuidStr) {
        if (uuidStr == null || uuidStr.trim().isEmpty()) return 0.0;
        try {
            UUID uuid = UUID.fromString(uuidStr);
            fr.gens.core.modules.EconomyModule eco = (fr.gens.core.modules.EconomyModule) plugin.getModuleManager().getModule("economy");
            if (eco != null && eco.isEnabled()) {
                return eco.getBalance(uuid);
            }
        } catch (Exception ignored) {}
        return webDAO.getPlayerBalance(uuidStr);
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

    @com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
    public static class CasinoPlayRequest {
        public int betId;
    }

    @com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
    public static class CoinFlipPlayRequest {
        public int betId;
        public String choice;
    }

    public static class LoginRequest {
        public String username;
        public String password;
    }

    @com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
    public static class PlayRequest {
        public String gameId;
        public String uuid; // Accepté (envoyé par le panel web) mais non utilisé côté serveur
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        fr.gens.core.database.PendingCommandDAO pcd = new fr.gens.core.database.PendingCommandDAO(plugin);
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

    @com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
    public static class TeamDepositRequest {
        public double amount;
        public String type; // "money" or "xp"
    }

    @com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
    public static class TeamUpgradeRequest {
        public String perkId;
    }

    @com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
    public static class TeamColorRequest {
        public String color;
    }

    @com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
    public static class TeamMemberActionRequest {
        public String uuid;
    }

    @com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
    public static class PerkClaimRequest {
        public String perkId;
    }
}



