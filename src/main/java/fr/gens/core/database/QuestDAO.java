package fr.gens.core.database;

import fr.gens.core.CorePlugin;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiFunction;


public class QuestDAO {

    private final CorePlugin plugin;

    public QuestDAO(CorePlugin plugin) {
        this.plugin = plugin;
    }

    public void initDatabase() {
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             java.sql.Statement stmt = conn.createStatement()) {
            
            stmt.execute("CREATE TABLE IF NOT EXISTS player_quests_stats (" +
                    "uuid VARCHAR(36) PRIMARY KEY, " +
                    "player_name VARCHAR(16) NOT NULL, " +
                    "quests_completed INTEGER DEFAULT 0, " +
                    "rerolls_done INTEGER DEFAULT 0, " +
                    "last_reroll_date VARCHAR(255) DEFAULT ''" +
                    ");");

            try { stmt.execute("ALTER TABLE player_quests_stats ADD COLUMN rerolls_done INTEGER DEFAULT 0;"); } catch (SQLException ignored) {}
            try { stmt.execute("ALTER TABLE player_quests_stats ADD COLUMN last_reroll_date VARCHAR(255) DEFAULT '';"); } catch (SQLException ignored) {}

            stmt.execute("CREATE TABLE IF NOT EXISTS player_quests_history (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "uuid VARCHAR(36) NOT NULL, " +
                    "player_name VARCHAR(16) NOT NULL, " +
                    "completion_date BIGINT NOT NULL" +
                    ");");

            stmt.execute("CREATE TABLE IF NOT EXISTS weekly_rewards (" +
                    "week_id VARCHAR(20) PRIMARY KEY, " +
                    "reward_description TEXT NOT NULL, " +
                    "winner_uuid VARCHAR(36), " +
                    "is_distributed INTEGER DEFAULT 0" +
                    ");");

            stmt.execute("CREATE TABLE IF NOT EXISTS player_active_quests (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "uuid VARCHAR(36) NOT NULL, " +
                    "date_assigned VARCHAR(10) NOT NULL, " +
                    "category VARCHAR(20) NOT NULL, " +
                    "quest_id VARCHAR(50) NOT NULL, " +
                    "progress INTEGER DEFAULT 0, " +
                    "completed BOOLEAN DEFAULT 0" +
                    ");");
            
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_quests_history_uuid     ON player_quests_history(uuid);");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_quests_history_date     ON player_quests_history(completion_date);");

        } catch (SQLException e) {
            plugin.getLogger().log(java.util.logging.Level.SEVERE, "Erreur lors de la crÃƒÆ’Ã‚Â©ation des tables de quÃƒÆ’Ã‚Âªtes", e);
        }
    }

    public int getQuestsCompletedTotal(UUID uuid) {
        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement("SELECT quests_completed FROM player_quests_stats WHERE uuid = ?")) {
                stmt.setString(1, uuid.toString());
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getInt("quests_completed");
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public int getRerollsDone(UUID uuid, String today) {
        boolean needsReset = false;
        int count = 0;

        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement("SELECT rerolls_done, last_reroll_date FROM player_quests_stats WHERE uuid = ?")) {
                stmt.setString(1, uuid.toString());
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        String lastDate = rs.getString("last_reroll_date");
                        if (today.equals(lastDate)) {
                            count = rs.getInt("rerolls_done");
                        } else {
                            // C'est un nouveau jour, on reset plus tard
                            needsReset = true;
                        }
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        if (needsReset) {
            setRerollsDone(uuid, 0, today);
            return 0;
        }

        return count;
    }

    public void setRerollsDone(UUID uuid, int count, String today) {
        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement(
                    "UPDATE player_quests_stats SET rerolls_done = ?, last_reroll_date = ? WHERE uuid = ?")) {
                stmt.setInt(1, count);
                stmt.setString(2, today);
                stmt.setString(3, uuid.toString());
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public Map<String, Object> getQuestsLeaderboardData() {
        Map<String, Object> result = new HashMap<>();
        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            // Current Reward
            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            String currentWeek = String.valueOf(cal.getTimeInMillis());
            String reward = "Aucune";
            
            try (PreparedStatement ps = conn.prepareStatement("SELECT reward_description FROM weekly_rewards WHERE week_id = ?")) {
                ps.setString(1, currentWeek);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) reward = rs.getString("reward_description");
                }
            }
            result.put("reward", reward);
            
            long now = System.currentTimeMillis();
            long dayStart = now - (24L * 60L * 60L * 1000L);
            long weekStart = cal.getTimeInMillis();
            long monthStart = now - (30L * 24L * 60L * 60L * 1000L);
            
            long endOfWeek = weekStart + (7L * 24L * 60L * 60L * 1000L);
            long timeRemainingMs = endOfWeek - now;
            if (timeRemainingMs < 0) timeRemainingMs = 0;
            long days = timeRemainingMs / (1000 * 60 * 60 * 24);
            long hours = (timeRemainingMs / (1000 * 60 * 60)) % 24;
            long minutes = (timeRemainingMs / (1000 * 60)) % 60;
            result.put("timeRemaining", days + "j " + hours + "h " + minutes + "m");
            
            BiFunction<Long, Long, List<Map<String, Object>>> getLeaderboard = (start, end) -> {
                List<Map<String, Object>> list = new ArrayList<>();
                String sql = "SELECT player_name, COUNT(*) as count FROM player_quests_history WHERE completion_date >= ? AND completion_date <= ? GROUP BY uuid ORDER BY count DESC LIMIT 10";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setLong(1, start);
                    ps.setLong(2, end);
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            Map<String, Object> map = new HashMap<>();
                            map.put("playerName", rs.getString("player_name"));
                            map.put("questsCompleted", rs.getInt("count"));
                            list.add(map);
                        }
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                return list;
            };
            
            result.put("daily", getLeaderboard.apply(dayStart, now));
            result.put("weekly", getLeaderboard.apply(weekStart, now));
            result.put("monthly", getLeaderboard.apply(monthStart, now));
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return result;
    }
}

