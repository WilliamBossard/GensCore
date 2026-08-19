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
}
