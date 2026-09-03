package fr.gens.core.database;

import fr.gens.core.CorePlugin;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Random;

public class WebDAO {

    private final CorePlugin plugin;

    public WebDAO(CorePlugin plugin) {
        this.plugin = plugin;
    }

    public void initDatabase() {
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             Statement stmt = conn.createStatement()) {
            
            stmt.execute("CREATE TABLE IF NOT EXISTS player_web_bets (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "uuid VARCHAR(36), " +
                    "material VARCHAR(50), " +
                    "amount INTEGER, " +
                    "base64_data TEXT" +
                    ");");

            stmt.execute("CREATE TABLE IF NOT EXISTS player_web_rewards (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "uuid VARCHAR(36), " +
                    "material VARCHAR(50), " +
                    "amount INTEGER, " +
                    "base64_data TEXT" +
                    ");");
            
            // Index for optimization
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_player_profiles_username ON player_profiles(username);");
                    
        } catch (SQLException e) {
            plugin.getLogger().log(java.util.logging.Level.SEVERE, "Erreur lors de la cr\u00e9ation des tables web", e);
        }
    }

    public UUID getPlayerUuidByUsername(String username) {
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement pstmt = conn.prepareStatement("SELECT uuid FROM player_profiles WHERE username = ? COLLATE NOCASE")) {
            pstmt.setString(1, username);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return UUID.fromString(rs.getString("uuid"));
                }
            }
        } catch (Exception e) {}
        return null;
    }

    public double getPlayerBalance(String uuidStr) {
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement pstmt = conn.prepareStatement("SELECT balance FROM players_economy WHERE uuid = ?")) {
            pstmt.setString(1, uuidStr);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble("balance");
                }
            }
        } catch (Exception e) {}
        return 0.0;
    }

    public List<Map<String, Object>> getRecentTransactions(String uuidStr) {
        List<Map<String, Object>> recentTransactions = new ArrayList<>();
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
        return recentTransactions;
    }

    public int[] getQuestsActivity(String uuidStr, long todayStart, long oneDay) {
        int[] questsActivity = new int[7];
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
        return questsActivity;
    }

    public List<Map<String, Object>> getCasinoInventory(String uuidStr) {
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
        return items;
    }

    public long getMinigameLastPlayed(String uuidStr, String gameId) {
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement pstmt = conn.prepareStatement("SELECT last_played FROM player_minigame_cooldowns WHERE uuid = ? AND game_id = ?")) {
            pstmt.setString(1, uuidStr);
            pstmt.setString(2, gameId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getLong("last_played");
            }
        } catch (Exception e) {}
        return 0L;
    }

    public void updateMinigameLastPlayed(String uuidStr, String gameId, long time) {
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement pstmt = conn.prepareStatement("INSERT OR REPLACE INTO player_minigame_cooldowns (uuid, game_id, last_played) VALUES (?, ?, ?)")) {
            pstmt.setString(1, uuidStr);
            pstmt.setString(2, gameId);
            pstmt.setLong(3, time);
            pstmt.executeUpdate();
        } catch (Exception e) {}
    }

    public Map<String, Object> playCasino(String uuid, int betId) {
        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            conn.setAutoCommit(false);
            
            try {
                // 1. Check if bet exists
                String base64 = null;
                String material = null;
                int amount = 0;
                
                try (PreparedStatement pstmt = conn.prepareStatement("SELECT material, amount, base64_data FROM player_web_bets WHERE id = ? AND uuid = ?")) {
                    pstmt.setInt(1, betId);
                    pstmt.setString(2, uuid);
                    try (ResultSet rs = pstmt.executeQuery()) {
                        if (rs.next()) {
                            material = rs.getString("material");
                            amount = rs.getInt("amount");
                            base64 = rs.getString("base64_data");
                        }
                    }
                }
                
                if (base64 == null) {
                    conn.rollback();
                    return Map.of("error", "Mise introuvable");
                }

                // 2. Remove bet
                try (PreparedStatement pstmt = conn.prepareStatement("DELETE FROM player_web_bets WHERE id = ?")) {
                    pstmt.setInt(1, betId);
                    pstmt.executeUpdate();
                }

                // 3. Roll Casino Logic
                int roll = new Random().nextInt(100);
                int multiplier = 0;
                String resultType = "LOSS";
                if (roll < 5) {
                    multiplier = 5;
                    resultType = "JACKPOT";
                }
                else if (roll < 15) {
                    multiplier = 3;
                    resultType = "WIN_MEDIUM";
                }
                else if (roll < 50) {
                    multiplier = 2;
                    resultType = "WIN_SMALL";
                }

                if (multiplier > 0) {
                    try (PreparedStatement pstmt = conn.prepareStatement("INSERT INTO player_web_rewards (uuid, material, amount, base64_data) VALUES (?, ?, ?, ?)")) {
                        for (int i = 0; i < multiplier; i++) {
                            pstmt.setString(1, uuid);
                            pstmt.setString(2, material);
                            pstmt.setInt(3, amount);
                            pstmt.setString(4, base64);
                            pstmt.addBatch();
                        }
                        pstmt.executeBatch();
                    }
                }
                
                conn.commit();
                return Map.of("success", true, "multiplier", multiplier, "result", resultType);
            } catch (Exception e) {
                conn.rollback();
                e.printStackTrace();
                return Map.of("error", "Erreur serveur lors de la transaction");
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (Exception e) {
            return Map.of("error", "Erreur de connexion a la base");
        }
    }
}


