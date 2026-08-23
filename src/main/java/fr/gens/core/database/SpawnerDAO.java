package fr.gens.core.database;

import fr.gens.core.CorePlugin;
import fr.gens.core.modules.spawners.SpawnerData;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.UUID;


public class SpawnerDAO {

    private final CorePlugin plugin;

    public SpawnerDAO(CorePlugin plugin) {
        this.plugin = plugin;
    }

    public void initDatabase() {
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             Statement stmt = conn.createStatement()) {
            
            stmt.execute("CREATE TABLE IF NOT EXISTS spawners (" +
                    "id VARCHAR(36) PRIMARY KEY, " +
                    "world VARCHAR(50) NOT NULL, " +
                    "x DOUBLE NOT NULL, " +
                    "y DOUBLE NOT NULL, " +
                    "z DOUBLE NOT NULL, " +
                    "type VARCHAR(50) NOT NULL, " +
                    "stack_count INTEGER NOT NULL, " +
                    "stored_exp INTEGER NOT NULL, " +
                    "stored_items TEXT NOT NULL, " +
                    "last_interacted VARCHAR(50) NOT NULL, " +
                    "storage_level INTEGER NOT NULL, " +
                    "exp_level INTEGER NOT NULL, " +
                    "speed_level INTEGER NOT NULL, " +
                    "is_loot_chest INTEGER NOT NULL DEFAULT 0" +
                    ");");
                    
        } catch (SQLException e) {
            plugin.getLogger().log(java.util.logging.Level.SEVERE, "Erreur lors de la crÃƒÆ’Ã‚Â©ation de la table spawners", e);
        }
    }

    public int loadSpawners(Map<Location, SpawnerData> activeSpawners) {
        int count = 0;
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT * FROM spawners");
             ResultSet rs = stmt.executeQuery()) {
             
            while (rs.next()) {
                UUID id = UUID.fromString(rs.getString("id"));
                World world = Bukkit.getWorld(rs.getString("world"));
                if (world == null) continue; // Skip if world not loaded
                
                double x = rs.getDouble("x");
                double y = rs.getDouble("y");
                double z = rs.getDouble("z");
                Location loc = new Location(world, x, y, z);
                
                String type = rs.getString("type");
                int stackCount = rs.getInt("stack_count");
                int exp = rs.getInt("stored_exp");
                String itemsJson = rs.getString("stored_items");
                String lastInteracted = rs.getString("last_interacted");
                int storageLvl = rs.getInt("storage_level");
                int expLvl = rs.getInt("exp_level");
                int speedLvl = rs.getInt("speed_level");
                
                SpawnerData data = new SpawnerData(id, loc, type, stackCount, exp, itemsJson, lastInteracted, storageLvl, expLvl, speedLvl);
                
                boolean isLootChest = false;
                try {
                    isLootChest = rs.getInt("is_loot_chest") == 1;
                } catch (SQLException ignored) {}
                data.setLootChest(isLootChest);
                
                activeSpawners.put(loc, data);
                count++;
            }
        } catch (SQLException e) {
            plugin.getLangManager().sendConsoleError("spawnermodule.log_3");
            e.printStackTrace();
        }
        return count;
    }

    public void saveSpawner(SpawnerData data) {
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement stmt = conn.prepareStatement("INSERT OR REPLACE INTO spawners (id, world, x, y, z, type, stack_count, stored_exp, stored_items, last_interacted, storage_level, exp_level, speed_level, is_loot_chest) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
             
            stmt.setString(1, data.getId().toString());
            stmt.setString(2, data.getLocation().getWorld().getName());
            stmt.setDouble(3, data.getLocation().getX());
            stmt.setDouble(4, data.getLocation().getY());
            stmt.setDouble(5, data.getLocation().getZ());
            stmt.setString(6, data.getType());
            stmt.setInt(7, data.getStackCount());
            stmt.setInt(8, data.getStoredExp());
            stmt.setString(9, data.getItemsJson());
            stmt.setString(10, data.getLastInteractedPlayer());
            stmt.setInt(11, data.getStorageLevel());
            stmt.setInt(12, data.getExpLevel());
            stmt.setInt(13, data.getSpeedLevel());
            stmt.setInt(14, data.isLootChest() ? 1 : 0);
            
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void saveAllSpawners(java.util.Collection<SpawnerData> spawners) {
        if (spawners.isEmpty()) return;
        
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement stmt = conn.prepareStatement("INSERT OR REPLACE INTO spawners (id, world, x, y, z, type, stack_count, stored_exp, stored_items, last_interacted, storage_level, exp_level, speed_level, is_loot_chest) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
            
            conn.setAutoCommit(false);
            
            for (SpawnerData data : spawners) {
                stmt.setString(1, data.getId().toString());
                stmt.setString(2, data.getLocation().getWorld().getName());
                stmt.setDouble(3, data.getLocation().getX());
                stmt.setDouble(4, data.getLocation().getY());
                stmt.setDouble(5, data.getLocation().getZ());
                stmt.setString(6, data.getType());
                stmt.setInt(7, data.getStackCount());
                stmt.setInt(8, data.getStoredExp());
                stmt.setString(9, data.getItemsJson());
                stmt.setString(10, data.getLastInteractedPlayer());
                stmt.setInt(11, data.getStorageLevel());
                stmt.setInt(12, data.getExpLevel());
                stmt.setInt(13, data.getSpeedLevel());
                stmt.setInt(14, data.isLootChest() ? 1 : 0);
                
                stmt.addBatch();
            }
            
            stmt.executeBatch();
            conn.commit();
            conn.setAutoCommit(true);
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void deleteSpawner(UUID id) {
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement stmt = conn.prepareStatement("DELETE FROM spawners WHERE id = ?")) {
            stmt.setString(1, id.toString());
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
