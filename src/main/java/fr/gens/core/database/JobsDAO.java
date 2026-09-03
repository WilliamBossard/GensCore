package fr.gens.core.database;

import fr.gens.core.CorePlugin;
import fr.gens.core.modules.jobs.JobType;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import java.util.Map;
import java.util.UUID;


public class JobsDAO {

    private final CorePlugin plugin;

    public JobsDAO(CorePlugin plugin) {
        this.plugin = plugin;
    }

    public void initDatabase() {
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             Statement stmt = conn.createStatement()) {
            
            stmt.execute("CREATE TABLE IF NOT EXISTS player_jobs (" +
                    "uuid VARCHAR(36) NOT NULL, " +
                    "job_name VARCHAR(50) NOT NULL, " +
                    "level INT DEFAULT 1, " +
                    "xp DOUBLE DEFAULT 0, " +
                    "PRIMARY KEY (uuid, job_name)" +
                    ");");
                    
        } catch (SQLException e) {
            plugin.getLogger().log(java.util.logging.Level.SEVERE, "Erreur lors de la création de la table player_jobs", e);
        }
    }

    public void loadPlayerJobs(UUID uuid, Map<JobType, Double> playerXp, Map<JobType, Integer> playerLevel, Map<JobType, Boolean> activeJobs) {
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM player_jobs WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String jobName = rs.getString("job_name");
                    try {
                        JobType type = JobType.valueOf(jobName);
                        double xp = rs.getDouble("xp");
                        int level = rs.getInt("level");
                        
                        playerXp.put(type, xp);
                        playerLevel.put(type, level);
                        activeJobs.put(type, true);
                    } catch (IllegalArgumentException ignored) {}
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void savePlayerJobs(UUID uuid, Map<JobType, Double> playerXp, Map<JobType, Integer> playerLevel, Map<JobType, Boolean> activeJobs) {
        if (playerXp == null || playerXp.isEmpty()) return;
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO player_jobs (uuid, job_name, level, xp) VALUES (?, ?, ?, ?) " +
                     "ON CONFLICT(uuid, job_name) DO UPDATE SET level = excluded.level, xp = excluded.xp")) {
            for (Map.Entry<JobType, Double> entry : playerXp.entrySet()) {
                JobType type = entry.getKey();
                if (!activeJobs.getOrDefault(type, false)) continue;
                ps.setString(1, uuid.toString());
                ps.setString(2, type.name());
                ps.setInt(3, playerLevel.getOrDefault(type, 1));
                ps.setDouble(4, entry.getValue());
                ps.addBatch();
            }
            ps.executeBatch();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void removePlayerJob(UUID uuid, JobType type) {
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM player_jobs WHERE uuid = ? AND job_name = ?")) {
            ps.setString(1, uuid.toString());
            ps.setString(2, type.name());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public int getTotalJobLevel(UUID uuid) {
        int globalJobLevel = 0;
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement pstmt = conn.prepareStatement("SELECT SUM(level) as total FROM player_jobs WHERE uuid = ?")) {
            pstmt.setString(1, uuid.toString());
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                globalJobLevel = rs.getInt("total");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return globalJobLevel;
    }

    public java.util.Map<String, java.util.List<java.util.Map<String, Object>>> getJobsLeaderboardData() {
        java.util.Map<String, java.util.List<java.util.Map<String, Object>>> jobsLeaderboard = new java.util.HashMap<>();
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT j.job_name, COALESCE(p.username, q.player_name, 'Inconnu') as player_name, j.level, j.xp " +
                     "FROM player_jobs j " +
                     "LEFT JOIN player_quests_stats q ON j.uuid = q.uuid " +
                     "LEFT JOIN player_profiles p ON j.uuid = p.uuid " +
                     "ORDER BY j.job_name, j.level DESC, j.xp DESC")) {
            
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String jobName = rs.getString("job_name");
                    String playerName = rs.getString("player_name");
                    
                    java.util.Map<String, Object> stat = new java.util.HashMap<>();
                    stat.put("playerName", playerName);
                    stat.put("level", rs.getInt("level"));
                    stat.put("xp", rs.getDouble("xp"));
                    
                    jobsLeaderboard.computeIfAbsent(jobName, k -> new java.util.ArrayList<>()).add(stat);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return jobsLeaderboard;
    }
}


