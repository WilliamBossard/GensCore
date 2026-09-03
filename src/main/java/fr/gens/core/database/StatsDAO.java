package fr.gens.core.database;

import fr.gens.core.CorePlugin;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;


public class StatsDAO {

    private final CorePlugin plugin;

    public StatsDAO(CorePlugin plugin) {
        this.plugin = plugin;
    }

    public void initDatabase() {
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             java.sql.Statement stmt = conn.createStatement()) {
            
            stmt.execute("CREATE TABLE IF NOT EXISTS player_stats (" +
                    "uuid VARCHAR(36) PRIMARY KEY, " +
                    "discord_id VARCHAR(50)" +
                    ");");

            stmt.execute("CREATE TABLE IF NOT EXISTS player_profiles (" +
                    "uuid VARCHAR(36) PRIMARY KEY, " +
                    "username VARCHAR(16) NOT NULL" +
                    ");");
            
            stmt.execute("CREATE TABLE IF NOT EXISTS player_global_stats (" +
                    "uuid VARCHAR(36) PRIMARY KEY, " +
                    "blocks_broken INTEGER DEFAULT 0, " +
                    "mobs_killed INTEGER DEFAULT 0, " +
                    "playtime_minutes INTEGER DEFAULT 0, " +
                    "deaths INTEGER DEFAULT 0, " +
                    "player_kills INTEGER DEFAULT 0, " +
                    "last_updated BIGINT DEFAULT 0" +
                    ");");
            
            try { stmt.execute("ALTER TABLE player_global_stats ADD COLUMN deaths INTEGER DEFAULT 0;"); } catch (Exception ignored) {}
            try { stmt.execute("ALTER TABLE player_global_stats ADD COLUMN player_kills INTEGER DEFAULT 0;"); } catch (Exception ignored) {}

            stmt.execute("CREATE TABLE IF NOT EXISTS player_transactions_history (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "uuid VARCHAR(36) NOT NULL, " +
                    "type VARCHAR(10) NOT NULL, " +
                    "material VARCHAR(50) NOT NULL, " +
                    "amount INTEGER NOT NULL, " +
                    "price DOUBLE NOT NULL, " +
                    "timestamp BIGINT NOT NULL" +
                    ");");
            
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_global_stats_uuid       ON player_global_stats(uuid);");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_transactions_uuid       ON player_transactions_history(uuid);");

        } catch (SQLException e) {
            plugin.getLogger().log(java.util.logging.Level.SEVERE, "Erreur lors de la création des tables stats", e);
        }
    }

    public void setDiscordId(UUID uuid, String discordId) {
        String sql = "INSERT INTO player_stats (uuid, discord_id) VALUES (?, ?) " +
                     "ON CONFLICT(uuid) DO UPDATE SET discord_id = excluded.discord_id;";
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, uuid.toString());
            pstmt.setString(2, discordId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(java.util.logging.Level.SEVERE, "Erreur lors de la mise a jour du Discord ID", e);
        }
    }

    public UUID getUuidFromDiscord(String discordId) {
        String sql = "SELECT uuid FROM player_stats WHERE discord_id = ?";
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, discordId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    String u = rs.getString("uuid");
                    if (u != null && !u.isEmpty()) return UUID.fromString(u);
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(java.util.logging.Level.SEVERE, "Erreur lors de la recuperation de l'UUID via Discord", e);
        }
        return null;
    }

    public String getDiscordId(UUID uuid) {
        String sql = "SELECT discord_id FROM player_stats WHERE uuid = ?";
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, uuid.toString());
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("discord_id");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<Map<String, Object>> getAllKnownPlayers() {
        List<Map<String, Object>> players = new ArrayList<>();
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT uuid, username FROM player_profiles")) {
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> p = new HashMap<>();
                    p.put("uuid", rs.getString("uuid"));
                    p.put("name", rs.getString("username"));
                    players.add(p);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return players;
    }

    public long getPlaytimeMinutes(UUID uuid) {
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT playtime_minutes FROM player_global_stats WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getLong("playtime_minutes");
            }
        } catch (SQLException e) {
            plugin.getLangManager().sendConsoleError("db.query_error");
            e.printStackTrace();
        }
        return 0;
    }

    public List<Map<String, Object>> getGlobalLeaderboard() {
        List<Map<String, Object>> leaderboard = new ArrayList<>();
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT q.player_name, p.username, q.quests_completed, q.uuid, " +
                     "COALESCE(g.blocks_broken, 0) as blocks, " +
                     "COALESCE(g.mobs_killed, 0) as mobs, " +
                     "COALESCE(g.playtime_minutes, 0) as playtime " +
                     "FROM player_quests_stats q " +
                     "LEFT JOIN player_global_stats g ON q.uuid = g.uuid " +
                     "LEFT JOIN player_profiles p ON q.uuid = p.uuid " +
                     "ORDER BY q.quests_completed DESC, g.blocks_broken DESC LIMIT 50")) {
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> playerStat = new HashMap<>();
                    String name = rs.getString("username");
                    if (name == null) name = rs.getString("player_name");
                    playerStat.put("playerName", name);
                    playerStat.put("questsCompleted", rs.getInt("quests_completed"));
                    playerStat.put("uuid", rs.getString("uuid"));
                    playerStat.put("blocksBroken", rs.getInt("blocks"));
                    playerStat.put("mobsKilled", rs.getInt("mobs"));
                    playerStat.put("playtime", rs.getLong("playtime"));
                    leaderboard.add(playerStat);
                }
            }
        } catch (SQLException e) {
            plugin.getLangManager().sendConsoleError("db.query_error");
            e.printStackTrace();
        }
        return leaderboard;
    }

    public java.util.concurrent.CompletableFuture<fr.gens.core.modules.stats.StatsModule.PlayerStats> loadPlayerStats(UUID uuid) {
        java.util.concurrent.CompletableFuture<fr.gens.core.modules.stats.StatsModule.PlayerStats> future = new java.util.concurrent.CompletableFuture<>();
        plugin.getFoliaLib().getScheduler().runAsync((task) -> {
            fr.gens.core.modules.stats.StatsModule.PlayerStats loadedStats = new fr.gens.core.modules.stats.StatsModule.PlayerStats();
            try (Connection conn = plugin.getDatabaseManager().getConnection();
                 PreparedStatement pstmt = conn.prepareStatement("SELECT blocks_broken, mobs_killed, playtime_minutes, deaths, player_kills FROM player_global_stats WHERE uuid = ?")) {
                pstmt.setString(1, uuid.toString());
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        loadedStats.blocksBroken = rs.getInt("blocks_broken");
                        loadedStats.mobsKilled = rs.getInt("mobs_killed");
                        loadedStats.playtimeMinutes = rs.getInt("playtime_minutes");
                        loadedStats.deaths = rs.getInt("deaths");
                        loadedStats.playerKills = rs.getInt("player_kills");
                    } else {
                        try (PreparedStatement insert = conn.prepareStatement("INSERT INTO player_global_stats (uuid, blocks_broken, mobs_killed, playtime_minutes, deaths, player_kills) VALUES (?, 0, 0, 0, 0, 0)")) {
                            insert.setString(1, uuid.toString());
                            insert.executeUpdate();
                        }
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            future.complete(loadedStats);
        });
        return future;
    }

    public java.util.concurrent.CompletableFuture<Void> savePlayerStats(UUID uuid, fr.gens.core.modules.stats.StatsModule.PlayerStats stats) {
        java.util.concurrent.CompletableFuture<Void> future = new java.util.concurrent.CompletableFuture<>();
        plugin.getFoliaLib().getScheduler().runAsync((task) -> {
            try (Connection conn = plugin.getDatabaseManager().getConnection();
                 PreparedStatement pstmt = conn.prepareStatement("UPDATE player_global_stats SET blocks_broken = ?, mobs_killed = ?, playtime_minutes = ?, deaths = ?, player_kills = ?, last_updated = ? WHERE uuid = ?")) {
                pstmt.setInt(1, stats.blocksBroken);
                pstmt.setInt(2, stats.mobsKilled);
                pstmt.setInt(3, stats.playtimeMinutes);
                pstmt.setInt(4, stats.deaths);
                pstmt.setInt(5, stats.playerKills);
                pstmt.setLong(6, System.currentTimeMillis());
                pstmt.setString(7, uuid.toString());
                pstmt.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
            future.complete(null);
        });
        return future;
    }

    public java.util.concurrent.CompletableFuture<Void> saveAllStats(Map<UUID, fr.gens.core.modules.stats.StatsModule.PlayerStats> statsMap) {
        java.util.concurrent.CompletableFuture<Void> future = new java.util.concurrent.CompletableFuture<>();
        if (statsMap.isEmpty()) {
            future.complete(null);
            return future;
        }
        
        Runnable task = () -> {
            try (Connection conn = plugin.getDatabaseManager().getConnection();
                 PreparedStatement pstmt = conn.prepareStatement("UPDATE player_global_stats SET blocks_broken = ?, mobs_killed = ?, playtime_minutes = ?, deaths = ?, player_kills = ?, last_updated = ? WHERE uuid = ?")) {
                
                conn.setAutoCommit(false);
                long now = System.currentTimeMillis();
                for (Map.Entry<UUID, fr.gens.core.modules.stats.StatsModule.PlayerStats> entry : statsMap.entrySet()) {
                    pstmt.setInt(1, entry.getValue().blocksBroken);
                    pstmt.setInt(2, entry.getValue().mobsKilled);
                    pstmt.setInt(3, entry.getValue().playtimeMinutes);
                    pstmt.setInt(4, entry.getValue().deaths);
                    pstmt.setInt(5, entry.getValue().playerKills);
                    pstmt.setLong(6, now);
                    pstmt.setString(7, entry.getKey().toString());
                    pstmt.addBatch();
                }
                pstmt.executeBatch();
                conn.commit();
            } catch (SQLException e) {
                e.printStackTrace();
            }
            future.complete(null);
        };

        if (plugin.isEnabled()) {
            plugin.getFoliaLib().getScheduler().runAsync((t) -> task.run());
        } else {
            task.run();
        }
        
        return future;
    }
}




