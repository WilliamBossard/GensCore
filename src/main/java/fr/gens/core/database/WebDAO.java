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

            stmt.execute("CREATE INDEX IF NOT EXISTS idx_player_web_bets_uuid ON player_web_bets(uuid);");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_player_web_rewards_uuid ON player_web_rewards(uuid);");
            
            stmt.execute("CREATE TABLE IF NOT EXISTS web_player_sessions (" +
                    "token VARCHAR(100) PRIMARY KEY, " +
                    "uuid VARCHAR(36) NOT NULL, " +
                    "expiry BIGINT NOT NULL" +
                    ");");

            stmt.execute("CREATE TABLE IF NOT EXISTS web_admin_sessions (" +
                    "token VARCHAR(100) PRIMARY KEY, " +
                    "expiry BIGINT NOT NULL" +
                    ");");
            
            // Index for optimization
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_player_profiles_username ON player_profiles(username);");
                    
        } catch (SQLException e) {
            plugin.getLogger().log(java.util.logging.Level.SEVERE, "Erreur lors de la cr\u00e9ation des tables web", e);
        }
    }

    public void saveAdminSession(String token, long expiry) {
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement ps = conn.prepareStatement("INSERT OR REPLACE INTO web_admin_sessions (token, expiry) VALUES (?, ?)")) {
            ps.setString(1, token);
            ps.setLong(2, expiry);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void removeAdminSession(String token) {
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM web_admin_sessions WHERE token = ?")) {
            ps.setString(1, token);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public Map<String, Long> loadValidAdminSessions() {
        Map<String, Long> sessions = new HashMap<>();
        long now = System.currentTimeMillis();
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT token, expiry FROM web_admin_sessions WHERE expiry > ?")) {
            ps.setLong(1, now);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    sessions.put(rs.getString("token"), rs.getLong("expiry"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return sessions;
    }

    public void savePlayerSession(String token, String uuid, long expiry) {
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement ps = conn.prepareStatement("INSERT OR REPLACE INTO web_player_sessions (token, uuid, expiry) VALUES (?, ?, ?)")) {
            ps.setString(1, token);
            ps.setString(2, uuid);
            ps.setLong(3, expiry);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void removePlayerSession(String token) {
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM web_player_sessions WHERE token = ?")) {
            ps.setString(1, token);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public Map<String, Map.Entry<String, Long>> loadValidSessions() {
        Map<String, Map.Entry<String, Long>> sessions = new HashMap<>();
        long now = System.currentTimeMillis();
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT token, uuid, expiry FROM web_player_sessions WHERE expiry > ?")) {
            ps.setLong(1, now);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    sessions.put(rs.getString("token"), new java.util.AbstractMap.SimpleEntry<>(rs.getString("uuid"), rs.getLong("expiry")));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return sessions;
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

    public String getPlayerUsernameByUuid(UUID uuid) {
        if (uuid == null) return null;
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement pstmt = conn.prepareStatement("SELECT username FROM player_profiles WHERE uuid = ?")) {
            pstmt.setString(1, uuid.toString());
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("username");
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
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> tr = new HashMap<>();
                    tr.put("type", rs.getString("type"));
                    tr.put("material", rs.getString("material"));
                    tr.put("amount", rs.getInt("amount"));
                    tr.put("price", rs.getDouble("price"));
                    tr.put("timestamp", rs.getLong("timestamp"));
                    recentTransactions.add(tr);
                }
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
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    long completedAt = rs.getLong("completed_at");
                    int dayDiff = (int) ((todayStart - (completedAt / oneDay * oneDay)) / oneDay);
                    if (dayDiff >= 0 && dayDiff < 7) {
                        questsActivity[6 - dayDiff]++;
                    }
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
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    items.add(Map.of(
                        "id", rs.getInt("id"),
                        "material", rs.getString("material"),
                        "amount", rs.getInt("amount")
                    ));
                }
            }
        } catch (Exception e) {}
        return items;
    }

    public long getMinigameLastPlayed(String uuidStr, String gameId) {
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement pstmt = conn.prepareStatement("SELECT last_played FROM player_minigame_cooldowns WHERE uuid = ? AND game_id = ?")) {
            pstmt.setString(1, uuidStr);
            pstmt.setString(2, gameId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong("last_played");
                }
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

                // 3. Roll Casino Logic - Balanced 84% RTP
                // 4% Jackpot (x5), 8% Medium Win (x3), 20% Small Win (x2), 68% Loss (x0)
                int roll = new Random().nextInt(100);
                int multiplier = 0;
                String resultType = "LOSS";
                if (roll < 4) {
                    multiplier = 5;
                    resultType = "JACKPOT";
                }
                else if (roll < 12) {
                    multiplier = 3;
                    resultType = "WIN_MEDIUM";
                }
                else if (roll < 32) {
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

    public Map<String, Object> playCoinFlip(String uuid, int betId, String choice) {
        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            conn.setAutoCommit(false);
            try {
                // 1. Get bet info
                String material = null;
                int amount = 0;
                String base64 = null;
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

                if (material == null) {
                    conn.rollback();
                    return Map.of("error", "Mise introuvable ou n'appartient pas au joueur");
                }

                // 2. Remove bet
                try (PreparedStatement pstmt = conn.prepareStatement("DELETE FROM player_web_bets WHERE id = ?")) {
                    pstmt.setInt(1, betId);
                    pstmt.executeUpdate();
                }

                // 3. CoinFlip Logic (50/50 chance)
                boolean userChoseHeads = choice != null && (choice.equalsIgnoreCase("HEADS") || choice.equalsIgnoreCase("PILE"));
                boolean outcomeHeads = new Random().nextBoolean();
                String outcomeSide = outcomeHeads ? "HEADS" : "TAILS";
                boolean won = (userChoseHeads == outcomeHeads);

                int multiplier = won ? 2 : 0;
                if (won) {
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
                return Map.of(
                    "success", true,
                    "won", won,
                    "choice", userChoseHeads ? "HEADS" : "TAILS",
                    "outcome", outcomeSide,
                    "multiplier", multiplier
                );
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

    public Map<String, Object> getDepositedItem(int id, String uuid) {
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement pstmt = conn.prepareStatement("SELECT id, material, amount, base64_data FROM player_web_bets WHERE id = ? AND uuid = ?")) {
            pstmt.setInt(1, id);
            pstmt.setString(2, uuid);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    Map<String, Object> res = new HashMap<>();
                    res.put("id", rs.getInt("id"));
                    res.put("material", rs.getString("material"));
                    res.put("amount", rs.getInt("amount"));
                    res.put("base64_data", rs.getString("base64_data"));
                    return res;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean deleteDepositedItem(int id, String uuid) {
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement pstmt = conn.prepareStatement("DELETE FROM player_web_bets WHERE id = ? AND uuid = ?")) {
            pstmt.setInt(1, id);
            pstmt.setString(2, uuid);
            return pstmt.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public void addWebReward(String uuid, String material, int amount, String base64) {
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement pstmt = conn.prepareStatement("INSERT INTO player_web_rewards (uuid, material, amount, base64_data) VALUES (?, ?, ?, ?)")) {
            pstmt.setString(1, uuid);
            pstmt.setString(2, material);
            pstmt.setInt(3, amount);
            pstmt.setString(4, base64 != null ? base64 : "");
            pstmt.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Retourne le nombre de slots de depot utilises par un joueur (lignes dans player_web_bets).
     */
    public int countDepositedSlots(String uuid) {
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement pstmt = conn.prepareStatement("SELECT COUNT(*) FROM player_web_bets WHERE uuid = ?")) {
            pstmt.setString(1, uuid);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    /**
     * Cherche un depot existant stackable pour un meme materiau + meme base64_data (NBT identique),
     * ayant de la place disponible (amount < maxStack).
     * Retourne l'id et l'amount actuels si trouve, sinon null.
     */
    public Map<String, Object> findStackableDeposit(String uuid, String material, String base64, int maxStack) {
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                 "SELECT id, amount FROM player_web_bets WHERE uuid = ? AND material = ? AND base64_data = ? AND amount < ? ORDER BY id ASC LIMIT 1")) {
            pstmt.setString(1, uuid);
            pstmt.setString(2, material);
            pstmt.setString(3, base64 != null ? base64 : "");
            pstmt.setInt(4, maxStack);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    Map<String, Object> row = new HashMap<>();
                    row.put("id", rs.getInt("id"));
                    row.put("amount", rs.getInt("amount"));
                    return row;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Met a jour la quantite d'un depot existant (pour vente/recuperation partielle ou stacking).
     * Si newAmount <= 0, supprime la ligne.
     */
    public boolean updateDepositedItemAmount(int id, String uuid, int newAmount) {
        if (newAmount <= 0) {
            return deleteDepositedItem(id, uuid);
        }
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                 "UPDATE player_web_bets SET amount = ? WHERE id = ? AND uuid = ?")) {
            pstmt.setInt(1, newAmount);
            pstmt.setInt(2, id);
            pstmt.setString(3, uuid);
            return pstmt.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Incrémente la quantite d'un depot existant (stacking).
     */
    public boolean incrementDepositedItemAmount(int id, String uuid, int delta) {
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                 "UPDATE player_web_bets SET amount = amount + ? WHERE id = ? AND uuid = ?")) {
            pstmt.setInt(1, delta);
            pstmt.setInt(2, id);
            pstmt.setString(3, uuid);
            return pstmt.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}
