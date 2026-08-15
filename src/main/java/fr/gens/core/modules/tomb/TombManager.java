package fr.gens.core.modules.tomb;

import fr.gens.core.CorePlugin;
import fr.gens.core.utils.DatabaseManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class TombManager {

    private final CorePlugin plugin;
    private final TombModule module;
    private final Map<UUID, TombData> activeTombs = new ConcurrentHashMap<>();
    private final Map<Location, UUID> tombsByLocation = new ConcurrentHashMap<>();

    public TombManager(CorePlugin plugin, TombModule module) {
        this.plugin = plugin;
        this.module = module;
    }

    public void loadTombs() {
        activeTombs.clear();
        tombsByLocation.clear();
        
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

    public TombData createTomb(UUID ownerId, Location location, ItemStack[] contents, int xp, long expirationMs) {
        UUID id = UUID.randomUUID();
        long expirationTime = System.currentTimeMillis() + expirationMs;
        TombData tomb = new TombData(id, ownerId, location, contents, xp, expirationTime);
        
        activeTombs.put(id, tomb);
        tombsByLocation.put(location.getBlock().getLocation(), id);

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
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
        });

        return tomb;
    }

    public void removeTomb(UUID id) {
        TombData tomb = activeTombs.remove(id);
        if (tomb != null) {
            tombsByLocation.remove(tomb.getLocation().getBlock().getLocation());
            
            // Remove block
            Bukkit.getScheduler().runTask(plugin, () -> {
                Block block = tomb.getLocation().getBlock();
                block.setType(Material.AIR);
            });

            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                try (Connection conn = plugin.getDatabaseManager().getConnection();
                     PreparedStatement stmt = conn.prepareStatement("DELETE FROM tombs WHERE id = ?")) {
                    stmt.setString(1, id.toString());
                    stmt.executeUpdate();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            });
        }
    }

    public TombData getTombAt(Location location) {
        UUID id = tombsByLocation.get(location.getBlock().getLocation());
        if (id != null) {
            return activeTombs.get(id);
        }
        return null;
    }

    public void checkExpirations() {
        long now = System.currentTimeMillis();
        String action = plugin.getConfig().getString("modules.tomb.expiration_action", "UNLOCK").toUpperCase();
        
        for (TombData tomb : activeTombs.values()) {
            if (now > tomb.getExpirationTime()) {
                if (action.equals("DESTROY")) {
                    removeTomb(tomb.getId());
                } else if (action.equals("DROP")) {
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        for (ItemStack item : tomb.getContents()) {
                            if (item != null && item.getType() != Material.AIR) {
                                tomb.getLocation().getWorld().dropItemNaturally(tomb.getLocation(), item);
                            }
                        }
                    });
                    removeTomb(tomb.getId());
                }
                // Si c'est UNLOCK, on le laisse mais il sera marqué comme expiré
            }
        }
    }
}
