package fr.gens.core.database;

import fr.gens.core.CorePlugin;
import fr.gens.core.modules.moderation.ModerationModule.MuteData;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;


public class ModerationDAO {

    private final CorePlugin plugin;

    public ModerationDAO(CorePlugin plugin) {
        this.plugin = plugin;
    }

    public void initDatabase() {
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             Statement stmt = conn.createStatement()) {
            
            stmt.execute("CREATE TABLE IF NOT EXISTS moderation_mutes (" +
                    "uuid VARCHAR(36) PRIMARY KEY, " +
                    "expiration BIGINT, " +
                    "reason VARCHAR(255)" +
                    ");");

            stmt.execute("CREATE TABLE IF NOT EXISTS moderation_frozen (" +
                    "uuid VARCHAR(36) PRIMARY KEY" +
                    ");");
                    
        } catch (SQLException e) {
            plugin.getLogger().log(java.util.logging.Level.SEVERE, "Erreur lors de la création des tables de modération", e);
        }
    }

    public void saveMutes(Map<UUID, MuteData> mutes) {
        String deleteSql = "DELETE FROM moderation_mutes";
        String insertSql = "INSERT INTO moderation_mutes (uuid, expiration, reason) VALUES (?, ?, ?)";
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             Statement stmt = conn.createStatement();
             PreparedStatement pstmt = conn.prepareStatement(insertSql)) {
            stmt.executeUpdate(deleteSql);
            for (Map.Entry<UUID, MuteData> entry : mutes.entrySet()) {
                pstmt.setString(1, entry.getKey().toString());
                pstmt.setLong(2, entry.getValue().expiration);
                pstmt.setString(3, entry.getValue().reason);
                pstmt.addBatch();
            }
            pstmt.executeBatch();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void saveFrozen(Set<UUID> frozen) {
        String deleteSql = "DELETE FROM moderation_frozen";
        String insertSql = "INSERT INTO moderation_frozen (uuid) VALUES (?)";
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             Statement stmt = conn.createStatement();
             PreparedStatement pstmt = conn.prepareStatement(insertSql)) {
            stmt.executeUpdate(deleteSql);
            for (UUID uuid : frozen) {
                pstmt.setString(1, uuid.toString());
                pstmt.addBatch();
            }
            pstmt.executeBatch();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public Map<UUID, MuteData> loadMutes() {
        Map<UUID, MuteData> mutes = new HashMap<>();
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT * FROM moderation_mutes");
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                mutes.put(UUID.fromString(rs.getString("uuid")), new MuteData(rs.getString("reason"), rs.getLong("expiration")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return mutes;
    }

    public Set<UUID> loadFrozen() {
        Set<UUID> frozen = new HashSet<>();
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT * FROM moderation_frozen");
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                frozen.add(UUID.fromString(rs.getString("uuid")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return frozen;
    }
}

