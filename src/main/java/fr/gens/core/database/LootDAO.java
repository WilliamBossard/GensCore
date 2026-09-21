package fr.gens.core.database;

import fr.gens.core.CorePlugin;
import fr.gens.core.modules.loot.LootManager.LootChestData;
import fr.gens.core.utils.ItemSerializer;
import org.bukkit.inventory.ItemStack;

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
import java.util.logging.Level;

public class LootDAO {

    private final CorePlugin plugin;

    public LootDAO(CorePlugin plugin) {
        this.plugin = plugin;
    }

    public void initDatabase() {
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.execute("CREATE TABLE IF NOT EXISTS lootr_chests (" +
                    "location VARCHAR(100) PRIMARY KEY, " +
                    "loot_table VARCHAR(100) NOT NULL, " +
                    "seed BIGINT NOT NULL, " +
                    "size INT NOT NULL" +
                    ");");

            stmt.execute("CREATE TABLE IF NOT EXISTS lootr_player_chests (" +
                    "uuid VARCHAR(36) NOT NULL, " +
                    "location VARCHAR(100) NOT NULL, " +
                    "items_data TEXT, " +
                    "PRIMARY KEY (uuid, location)" +
                    ");");

            stmt.execute("CREATE INDEX IF NOT EXISTS idx_lootr_player_chests_uuid ON lootr_player_chests(uuid);");

        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Erreur lors de l'initialisation des tables Lootr SQLite", e);
        }
    }

    public Map<String, LootChestData> loadAllChests() {
        Map<String, LootChestData> chests = new HashMap<>();
        String sql = "SELECT location, loot_table, seed, size FROM lootr_chests";
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                String locKey = rs.getString("location");
                String lootTable = rs.getString("loot_table");
                long seed = rs.getLong("seed");
                int size = rs.getInt("size");
                chests.put(locKey, new LootChestData(lootTable, seed, size));
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Erreur lors du chargement des coffres Lootr depuis SQLite", e);
        }
        return chests;
    }

    public void saveChest(String locKey, String lootTable, long seed, int size) {
        String sql = "INSERT INTO lootr_chests (location, loot_table, seed, size) VALUES (?, ?, ?, ?) " +
                "ON CONFLICT(location) DO UPDATE SET loot_table=excluded.loot_table, seed=excluded.seed, size=excluded.size";
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, locKey);
            ps.setString(2, lootTable);
            ps.setLong(3, seed);
            ps.setInt(4, size);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Erreur lors de la sauvegarde du coffre Lootr " + locKey, e);
        }
    }

    public void deleteChest(String locKey) {
        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM lootr_chests WHERE location = ?")) {
                ps.setString(1, locKey);
                ps.executeUpdate();
            }
            try (PreparedStatement ps2 = conn.prepareStatement("DELETE FROM lootr_player_chests WHERE location = ?")) {
                ps2.setString(1, locKey);
                ps2.executeUpdate();
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Erreur lors de la suppression du coffre Lootr " + locKey, e);
        }
    }

    public ItemStack[] getPlayerLoot(UUID uuid, String locKey) {
        String sql = "SELECT items_data FROM lootr_player_chests WHERE uuid = ? AND location = ?";
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            ps.setString(2, locKey);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String base64 = rs.getString("items_data");
                    if (base64 != null && !base64.isEmpty()) {
                        return ItemSerializer.itemStackArrayFromBase64(base64);
                    }
                    return new ItemStack[0];
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Erreur lors de la récupération du loot joueur pour " + uuid + " à " + locKey, e);
        }
        return null;
    }

    public void savePlayerLoot(UUID uuid, String locKey, ItemStack[] items) {
        String base64 = items != null ? ItemSerializer.itemStackArrayToBase64(items) : null;
        String sql = "INSERT INTO lootr_player_chests (uuid, location, items_data) VALUES (?, ?, ?) " +
                "ON CONFLICT(uuid, location) DO UPDATE SET items_data=excluded.items_data";
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            ps.setString(2, locKey);
            ps.setString(3, base64);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Erreur lors de la sauvegarde du loot joueur pour " + uuid + " à " + locKey, e);
        }
    }

    public boolean hasPlayerLooted(UUID uuid, String locKey) {
        String sql = "SELECT 1 FROM lootr_player_chests WHERE uuid = ? AND location = ? LIMIT 1";
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            ps.setString(2, locKey);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Erreur lors de la vérification de coffre pillé pour " + uuid + " à " + locKey, e);
            return false;
        }
    }

    public Set<String> loadLootedLocationsForPlayer(UUID uuid) {
        Set<String> locations = new HashSet<>();
        String sql = "SELECT location FROM lootr_player_chests WHERE uuid = ?";
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    locations.add(rs.getString("location"));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Erreur lors du chargement des coffres pillés pour " + uuid, e);
        }
        return locations;
    }
}
