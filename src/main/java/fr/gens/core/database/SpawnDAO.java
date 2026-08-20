package fr.gens.core.database;

import fr.gens.core.CorePlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;


public class SpawnDAO {

    private final CorePlugin plugin;

    public SpawnDAO(CorePlugin plugin) {
        this.plugin = plugin;
    }

    public void initDatabase() {
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             Statement stmt = conn.createStatement()) {
            
            stmt.execute("CREATE TABLE IF NOT EXISTS spawn_location (" +
                    "id INTEGER PRIMARY KEY DEFAULT 1, " +
                    "world VARCHAR(255) NOT NULL, " +
                    "x DOUBLE NOT NULL, " +
                    "y DOUBLE NOT NULL, " +
                    "z DOUBLE NOT NULL, " +
                    "yaw FLOAT NOT NULL, " +
                    "pitch FLOAT NOT NULL" +
                    ");");
                    
        } catch (SQLException e) {
            plugin.getLogger().log(java.util.logging.Level.SEVERE, "Erreur lors de la crÃƒÆ’Ã‚Â©ation de la table spawn_location", e);
        }
    }

    public Location loadSpawn() {
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM spawn_location WHERE id = 1");
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                String worldName = rs.getString("world");
                World world = Bukkit.getWorld(worldName);
                if (world != null) {
                    return new Location(
                            world,
                            rs.getDouble("x"),
                            rs.getDouble("y"),
                            rs.getDouble("z"),
                            rs.getFloat("yaw"),
                            rs.getFloat("pitch")
                    );
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void saveSpawn(Location loc) {
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO spawn_location (id, world, x, y, z, yaw, pitch) VALUES (1, ?, ?, ?, ?, ?, ?) " +
                     "ON CONFLICT(id) DO UPDATE SET world=excluded.world, x=excluded.x, y=excluded.y, z=excluded.z, yaw=excluded.yaw, pitch=excluded.pitch")) {
            ps.setString(1, loc.getWorld().getName());
            ps.setDouble(2, loc.getX());
            ps.setDouble(3, loc.getY());
            ps.setDouble(4, loc.getZ());
            ps.setFloat(5, loc.getYaw());
            ps.setFloat(6, loc.getPitch());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
