package fr.gens.core.database;

import fr.gens.core.CorePlugin;
import fr.gens.core.modules.tomb.TombData;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.inventory.ItemStack;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.UUID;


public class TombDAO {

    private final CorePlugin plugin;

    public TombDAO(CorePlugin plugin) {
        this.plugin = plugin;
    }

    public void initDatabase() {
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             Statement stmt = conn.createStatement()) {
            
            stmt.execute("CREATE TABLE IF NOT EXISTS tombs (" +
                    "id VARCHAR(36) PRIMARY KEY, " +
                    "owner_id VARCHAR(36) NOT NULL, " +
                    "world VARCHAR(50) NOT NULL, " +
                    "x DOUBLE NOT NULL, " +
                    "y DOUBLE NOT NULL, " +
                    "z DOUBLE NOT NULL, " +
                    "contents TEXT NOT NULL, " +
                    "xp INTEGER NOT NULL, " +
                    "expiration_time BIGINT NOT NULL" +
                    ");");
                    
        } catch (SQLException e) {
            plugin.getLogger().log(java.util.logging.Level.SEVERE, "Erreur lors de la création de la table tombs", e);
        }
    }

    public void loadTombs(Map<UUID, TombData> activeTombs, Map<Location, UUID> tombsByLocation) {
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT * FROM tombs");
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                UUID id = UUID.fromString(rs.getString("id"));
                UUID ownerId = UUID.fromString(rs.getString("owner_id"));
                World world = Bukkit.getWorld(rs.getString("world"));
                double x = rs.getDouble("x");
                double y = rs.getDouble("y");
                double z = rs.getDouble("z");
                
                if (world == null) {
                    plugin.getLogger().warning("Tomb world not found: " + rs.getString("world"));
                    continue;
                }
                
                Location loc = new Location(world, x, y, z);
                String base64Contents = rs.getString("contents");
                ItemStack[] contents = plugin.getStorageManager().itemStackArrayFromBase64(base64Contents);
                int xp = rs.getInt("xp");
                long expirationTime = rs.getLong("expiration_time");

                TombData tomb = new TombData(id, ownerId, loc, contents, xp, expirationTime);
                activeTombs.put(id, tomb);
                tombsByLocation.put(loc.getBlock().getLocation(), id);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void createTomb(UUID id, UUID ownerId, Location location, ItemStack[] contents, int xp, long expirationTime) {
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement stmt = conn.prepareStatement("INSERT INTO tombs (id, owner_id, world, x, y, z, contents, xp, expiration_time) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
            stmt.setString(1, id.toString());
            stmt.setString(2, ownerId.toString());
            stmt.setString(3, location.getWorld().getName());
            stmt.setDouble(4, location.getX());
            stmt.setDouble(5, location.getY());
            stmt.setDouble(6, location.getZ());
            stmt.setString(7, plugin.getStorageManager().itemStackArrayToBase64(contents));
            stmt.setInt(8, xp);
            stmt.setLong(9, expirationTime);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void deleteTomb(UUID id) {
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement stmt = conn.prepareStatement("DELETE FROM tombs WHERE id = ?")) {
            stmt.setString(1, id.toString());
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}


