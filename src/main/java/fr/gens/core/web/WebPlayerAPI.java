package fr.gens.core.web;

import fr.gens.core.CorePlugin;
import fr.gens.core.database.AuthDAO;
import fr.gens.core.modules.auth.AuthModule;
import static io.javalin.apibuilder.ApiBuilder.*;
import org.bukkit.Bukkit;

import org.bukkit.entity.Player;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
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


public class WebPlayerAPI implements Listener {

    private final CorePlugin plugin;
    private final List<WheelReward> wheelRewards;

    public WebPlayerAPI(CorePlugin plugin) {
        this.plugin = plugin;
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
            LoginRequest req = ctx.bodyAsClass(LoginRequest.class);
            
            UUID playerUUID = null;
            boolean isOp = false;

            // Trouver le joueur
            Player targetOnline = Bukkit.getPlayer(req.username);
            if (targetOnline != null) {
                playerUUID = targetOnline.getUniqueId();
                isOp = targetOnline.isOp();
            } else {
                try (Connection conn = plugin.getDatabaseManager().getConnection();
                     PreparedStatement pstmt = conn.prepareStatement("SELECT uuid FROM player_profiles WHERE username = ? COLLATE NOCASE")) {
                    pstmt.setString(1, req.username);
                    try (ResultSet rs = pstmt.executeQuery()) {
                        if (rs.next()) {
                            playerUUID = UUID.fromString(rs.getString("uuid"));
                            // On the web, if offline, we assume they are not OP unless we want to query ops.json.
                            // For security, ops should login while online or we use a separate table for roles.
                            isOp = false; 
                        }
                    }
                } catch (Exception e) {}
            }

            if (playerUUID == null) {
                ctx.status(401).json(Map.of("error", "Joueur introuvable."));
                return;
            }

            fr.gens.core.modules.auth.AuthModule authModule = (fr.gens.core.modules.auth.AuthModule) plugin.getModuleManager().getModule("auth");
            AuthDAO.AuthData data = authModule != null ? authModule.getAuthDAO().getAuthData(playerUUID) : null;
            if (data == null) {
                ctx.status(401).json(Map.of("error", "Aucun compte enregistrÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â© (/register en jeu)."));
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
                ctx.status(401).json(Map.of("error", "Mot de passe incorrect."));
                return;
            }

            // GÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â©nÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â©rer un faux "token" simple pour l'instant (on pourrait utiliser JWT)
            String token = UUID.randomUUID().toString() + "-" + playerUUID.toString();
            
            // On sauvegarde le token en session ou on fait confiance au format uuid pour ce test
            
            Map<String, Object> response = new HashMap<>();
            response.put("token", token);
            response.put("uuid", playerUUID.toString());
            response.put("username", req.username);
            response.put("isOp", isOp);
            
            ctx.json(response);
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
            double balance = 0.0;
            try (Connection conn = plugin.getDatabaseManager().getConnection();
                 PreparedStatement pstmt = conn.prepareStatement("SELECT balance FROM players_economy WHERE uuid = ?")) {
                pstmt.setString(1, uuidStr);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        balance = rs.getDouble("balance");
                    }
                }
            } catch (Exception e) {}
            stats.put("balance", balance);

              // Global Stats (Lecture en direct depuis la RAM via le module Stats)
              fr.gens.core.modules.stats.StatsModule statsModule = (fr.gens.core.modules.stats.StatsModule) plugin.getModuleManager().getModule("stats");
              if (statsModule != null && statsModule.isEnabled()) {
                  fr.gens.core.modules.stats.StatsModule.PlayerStats pStats = statsModule.getStats(UUID.fromString(uuidStr));
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
              java.util.List<Map<String, Object>> recentTransactions = new java.util.ArrayList<>();
              try (Connection conn = plugin.getDatabaseManager().getConnection();
                   PreparedStatement pstmt = conn.prepareStatement("SELECT type, material, amount, price, timestamp FROM player_transactions_history WHERE uuid = ? ORDER BY timestamp DESC LIMIT 5")) {
                  pstmt.setString(1, uuidStr);
                  ResultSet rs = pstmt.executeQuery();
                  while (rs.next()) {
                      Map<String, Object> tr = new HashMap<>();
                      tr.put("type", rs.getString("type"));
                      tr.put("material", rs.getString("material"));
                      tr.put("amount", rs.getInt("amount"));
                      tr.put("price", rs.getDouble("price"));
                      tr.put("timestamp", rs.getLong("timestamp"));
                      recentTransactions.add(tr);
                  }
              } catch (Exception e) {}
              stats.put("recentTransactions", recentTransactions);

              // Quests Activity Graph (last 7 days)
              int[] questsActivity = new int[7];
              long oneDay = 86400000L;
              long todayStart = (System.currentTimeMillis() / oneDay) * oneDay;
              try (Connection conn = plugin.getDatabaseManager().getConnection();
                   PreparedStatement pstmt = conn.prepareStatement("SELECT completed_at FROM player_quests_history WHERE uuid = ? AND completed_at >= ?")) {
                  pstmt.setString(1, uuidStr);
                  pstmt.setLong(2, todayStart - (6 * oneDay));
                  ResultSet rs = pstmt.executeQuery();
                  while (rs.next()) {
                      long completedAt = rs.getLong("completed_at");
                      int dayDiff = (int) ((todayStart - (completedAt / oneDay * oneDay)) / oneDay);
                      if (dayDiff >= 0 && dayDiff < 7) {
                          questsActivity[6 - dayDiff]++;
                      }
                  }
              } catch (Exception e) {}
              stats.put("questsActivity", questsActivity);

              ctx.json(stats);
        });

        get("/api/player/info", ctx -> {
            String uuidStr = ctx.queryParam("uuid");
            if (uuidStr == null) {
                ctx.status(400).json("UUID manquant");
                return;
            }
            boolean isOp = false;
            Player p = Bukkit.getPlayer(UUID.fromString(uuidStr));
            if (p != null) {
                isOp = p.isOp();
            }
            ctx.json(Map.of("isOp", isOp));
        });

        get("/api/games/config", ctx -> {
            boolean wheelEnabled = plugin.getConfigManager().getConfig("modules/minigames.yml").getBoolean("minigames.wheel.enabled", true);
            boolean casinoEnabled = plugin.getConfigManager().getConfig("modules/minigames.yml").getBoolean("minigames.casino.enabled", true);
            ctx.json(Map.of("wheelEnabled", wheelEnabled, "casinoEnabled", casinoEnabled));
        });

        get("/api/games/casino/inventory", ctx -> {
            String uuidStr = ctx.queryParam("uuid");
            if (uuidStr == null) {
                ctx.status(400).json("UUID manquant");
                return;
            }
            List<Map<String, Object>> items = new ArrayList<>();
            try (Connection conn = plugin.getDatabaseManager().getConnection();
                 PreparedStatement pstmt = conn.prepareStatement("SELECT id, material, amount FROM player_web_bets WHERE uuid = ?")) {
                pstmt.setString(1, uuidStr);
                ResultSet rs = pstmt.executeQuery();
                while (rs.next()) {
                    items.add(Map.of(
                        "id", rs.getInt("id"),
                        "material", rs.getString("material"),
                        "amount", rs.getInt("amount")
                    ));
                }
            } catch (Exception e) {}
            ctx.json(items);
        });

        post("/api/games/casino/play", ctx -> {
            CasinoPlayRequest req = ctx.bodyAsClass(CasinoPlayRequest.class);
            if (req.uuid == null || req.betId <= 0) {
                ctx.status(400).json(Map.of("error", "RequÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Âªte invalide"));
                return;
            }

            // Check cooldown (1h)
            long lastPlayed = 0;
            try (Connection conn = plugin.getDatabaseManager().getConnection();
                 PreparedStatement pstmt = conn.prepareStatement("SELECT last_played FROM player_minigame_cooldowns WHERE uuid = ? AND game_id = ?")) {
                pstmt.setString(1, req.uuid);
                pstmt.setString(2, "casino");
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    lastPlayed = rs.getLong("last_played");
                }
            } catch (Exception e) {}

            long now = System.currentTimeMillis();
            if (now - lastPlayed < 60 * 60 * 1000L) { // 1 heure
                long timeLeft = (60 * 60 * 1000L) - (now - lastPlayed);
                long minutes = timeLeft / (60 * 1000L);
                long seconds = (timeLeft % (60 * 1000L)) / 1000L;
                ctx.status(400).json(Map.of("error", "La machine surchauffe ! Revenez dans " + minutes + "m " + seconds + "s."));
                return;
            }

            // Update cooldown
            try (Connection conn = plugin.getDatabaseManager().getConnection();
                 PreparedStatement pstmt = conn.prepareStatement("INSERT OR REPLACE INTO player_minigame_cooldowns (uuid, game_id, last_played) VALUES (?, ?, ?)")) {
                pstmt.setString(1, req.uuid);
                pstmt.setString(2, "casino");
                pstmt.setLong(3, now);
                pstmt.executeUpdate();
            } catch (Exception e) {}

            // Casino Logic with Transaction
            try (Connection conn = plugin.getDatabaseManager().getConnection()) {
                conn.setAutoCommit(false);
                
                try {
                    // 1. Check if bet exists
                    String base64 = null;
                    String material = null;
                    int amount = 0;
                    
                    try (PreparedStatement pstmt = conn.prepareStatement("SELECT material, amount, base64_data FROM player_web_bets WHERE id = ? AND uuid = ?")) {
                        pstmt.setInt(1, req.betId);
                        pstmt.setString(2, req.uuid);
                        try (ResultSet rs = pstmt.executeQuery()) {
                            if (rs.next()) {
                                material = rs.getString("material");
                                amount = rs.getInt("amount");
                                base64 = rs.getString("base64_data");
                            }
                        }
                    }
                    
                    if (base64 == null) {
                        ctx.status(400).json(Map.of("error", "Mise introuvable"));
                        conn.rollback();
                        return;
                    }

                    // 2. Remove bet
                    try (PreparedStatement pstmt = conn.prepareStatement("DELETE FROM player_web_bets WHERE id = ?")) {
                        pstmt.setInt(1, req.betId);
                        pstmt.executeUpdate();
                    }

                    // 3. Roll Casino Logic
                    int roll = new Random().nextInt(100);
                    int multiplier = 0;
                    if (roll < 5) multiplier = 5;
                    else if (roll < 15) multiplier = 3;
                    else if (roll < 50) multiplier = 2;
                    else multiplier = 0;

                    if (multiplier > 0) {
                        try (PreparedStatement pstmt = conn.prepareStatement("INSERT INTO player_web_rewards (uuid, material, amount, base64_data) VALUES (?, ?, ?, ?)")) {
                            for (int i = 0; i < multiplier; i++) {
                                pstmt.setString(1, req.uuid);
                                pstmt.setString(2, material);
                                pstmt.setInt(3, amount);
                                pstmt.setString(4, base64);
                                pstmt.addBatch();
                            }
                            pstmt.executeBatch();
                        }
                    }
                    
                    conn.commit();
                    ctx.json(Map.of("success", true, "multiplier", multiplier));
                } catch (Exception e) {
                    conn.rollback();
                    e.printStackTrace();
                    ctx.status(500).json(Map.of("error", "Erreur serveur lors de la transaction"));
                } finally {
                    conn.setAutoCommit(true);
                }
            } catch (Exception e) {}
        });

        get("/api/games/wheel", ctx -> {
            List<WheelReward> activeRewards = getActiveWheelRewards();
            ctx.json(activeRewards);
        });
        
        post("/api/games/play", ctx -> {
            PlayRequest req = ctx.bodyAsClass(PlayRequest.class);
            UUID playerUUID = UUID.fromString(req.uuid);
            
            // Check cooldown (24h)
            long lastPlayed = 0;
            try (Connection conn = plugin.getDatabaseManager().getConnection();
                 PreparedStatement pstmt = conn.prepareStatement("SELECT last_played FROM player_minigame_cooldowns WHERE uuid = ? AND game_id = ?")) {
                pstmt.setString(1, req.uuid);
                pstmt.setString(2, req.gameId);
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    lastPlayed = rs.getLong("last_played");
                }
            } catch (Exception e) {}

            long now = System.currentTimeMillis();
            if (now - lastPlayed < 24 * 60 * 60 * 1000L) {
                long timeLeft = (24 * 60 * 60 * 1000L) - (now - lastPlayed);
                long hours = timeLeft / (60 * 60 * 1000L);
                long minutes = (timeLeft % (60 * 60 * 1000L)) / (60 * 1000L);
                ctx.status(400).json(Map.of("error", "Vous avez dÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â©jÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â  jouÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â© aujourd'hui ! Revenez dans " + hours + "h " + minutes + "m."));
                return;
            }

            // Update cooldown
            try (Connection conn = plugin.getDatabaseManager().getConnection();
                 PreparedStatement pstmt = conn.prepareStatement("INSERT OR REPLACE INTO player_minigame_cooldowns (uuid, game_id, last_played) VALUES (?, ?, ?)")) {
                pstmt.setString(1, req.uuid);
                pstmt.setString(2, req.gameId);
                pstmt.setLong(3, now);
                pstmt.executeUpdate();
            } catch (Exception e) {}

            List<WheelReward> activeRewards = getActiveWheelRewards();
            if (activeRewards.isEmpty()) {
                ctx.status(400).json(Map.of("error", "Aucune rÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â©compense configurÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â©e."));
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
            String rewardMessage = "Vous avez gagnÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â© : " + wonReward.name + " !";
            
            // Si le joueur est en ligne, on exÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â©cute, sinon on met en attente
            Player target = Bukkit.getPlayer(playerUUID);
            if (target != null && target.isOnline()) {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), rewardCommand.replace("%player%", target.getName()));
                    target.sendMessage("<green>[Web] " + rewardMessage);
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
        public String uuid;
        public int betId;
    }

    public static class LoginRequest {
        public String username;
        public String password;
    }

    public static class PlayRequest {
        public String uuid;
        public String gameId;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        fr.gens.core.database.PendingCommandDAO pcd = new fr.gens.core.database.PendingCommandDAO(plugin);
        pcd.initDatabase(); // just to ensure table exists
        pcd.processPendingCommands(player);
        
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection conn = plugin.getDatabaseManager().getConnection()) {
                // Mettre ÃƒÆ’Ã‚Â  jour le profil (UUID -> Name)
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

