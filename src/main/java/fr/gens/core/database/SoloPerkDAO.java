package fr.gens.core.database;

import fr.gens.core.CorePlugin;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

public class SoloPerkDAO {

    private final CorePlugin plugin;

    public SoloPerkDAO(CorePlugin plugin) {
        this.plugin = plugin;
    }

    public void initDatabase() {
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             java.sql.Statement stmt = conn.createStatement()) {

            stmt.execute("CREATE TABLE IF NOT EXISTS genscore_player_perks (" +
                    "uuid VARCHAR(36) NOT NULL, " +
                    "perk_id VARCHAR(50) NOT NULL, " +
                    "unlocked_at BIGINT NOT NULL, " +
                    "PRIMARY KEY (uuid, perk_id)" +
                    ");");

            stmt.execute("CREATE TABLE IF NOT EXISTS genscore_perk_settings (" +
                    "uuid VARCHAR(36) NOT NULL, " +
                    "perk_id VARCHAR(50) NOT NULL, " +
                    "is_enabled BOOLEAN NOT NULL DEFAULT 1, " +
                    "PRIMARY KEY (uuid, perk_id)" +
                    ");");

            stmt.execute("CREATE INDEX IF NOT EXISTS idx_player_perks_uuid ON genscore_player_perks(uuid);");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_perk_settings_uuid ON genscore_perk_settings(uuid);");

        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Erreur lors de l'initialisation des tables de bonus individuels", e);
        }
    }

    public Set<String> getUnlockedPerks(UUID uuid) {
        Set<String> perks = new HashSet<>();
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT perk_id FROM genscore_player_perks WHERE uuid = ?")) {
            stmt.setString(1, uuid.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    perks.add(rs.getString("perk_id"));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Erreur lors du chargement des bonus pour " + uuid, e);
        }
        return perks;
    }

    public Map<String, Boolean> getPerkSettings(UUID uuid) {
        Map<String, Boolean> settings = new HashMap<>();
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT perk_id, is_enabled FROM genscore_perk_settings WHERE uuid = ?")) {
            stmt.setString(1, uuid.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    settings.put(rs.getString("perk_id"), rs.getBoolean("is_enabled"));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Erreur lors du chargement des reglages de bonus pour " + uuid, e);
        }
        return settings;
    }

    public void saveUnlockedPerk(UUID uuid, String perkId, long timestamp) {
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "INSERT OR IGNORE INTO genscore_player_perks (uuid, perk_id, unlocked_at) VALUES (?, ?, ?)")) {
            stmt.setString(1, uuid.toString());
            stmt.setString(2, perkId);
            stmt.setLong(3, timestamp);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Erreur lors de la sauvegarde du bonus debloque pour " + uuid, e);
        }
    }

    public void savePerkSetting(UUID uuid, String perkId, boolean isEnabled) {
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "INSERT INTO genscore_perk_settings (uuid, perk_id, is_enabled) VALUES (?, ?, ?) " +
                             "ON CONFLICT(uuid, perk_id) DO UPDATE SET is_enabled = excluded.is_enabled")) {
            stmt.setString(1, uuid.toString());
            stmt.setString(2, perkId);
            stmt.setBoolean(3, isEnabled);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Erreur lors de la sauvegarde du reglage de bonus pour " + uuid, e);
        }
    }
}
