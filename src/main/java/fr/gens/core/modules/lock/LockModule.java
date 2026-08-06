package fr.gens.core.modules.lock;

import fr.gens.core.CorePlugin;
import fr.gens.core.modules.Module;
import org.bukkit.Bukkit;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Location;

public class LockModule implements Module {
    private final CorePlugin plugin;
    private LockListener lockListener;
    private LockCommand lockCommand;
    private final Map<String, LockData> locks = new HashMap<>();
    private boolean enabled;

    public LockModule(CorePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() { return "Locks"; }

    @Override
    public String getDescription() { return "Système de verrouillage de conteneurs avec support de guildes."; }

    @Override
    public boolean isEnabled() { return enabled; }

    @Override
    public void enable() {
        enabled = true;
        loadLocks();
        lockCommand = new LockCommand(plugin, this);
        lockListener = new LockListener(plugin, this);

        plugin.getCommand("lock").setExecutor(lockCommand);
        plugin.getCommand("lock").setTabCompleter(lockCommand);
        Bukkit.getPluginManager().registerEvents(lockListener, plugin);
        plugin.getLogger().info("[Locks] Module activé ! (" + locks.size() + " verrous chargés)");
    }

    @Override
    public void disable() {
        enabled = false;
        plugin.getLogger().info("[Locks] Module désactivé !");
    }

    private void loadLocks() {
        locks.clear();
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM genscore_locks")) {
            while (rs.next()) {
                String locStr = rs.getString("location");
                String ownerStr = rs.getString("owner_uuid");
                int teamId = rs.getInt("team_id");
                
                UUID ownerUuid = ownerStr != null && !ownerStr.isEmpty() ? UUID.fromString(ownerStr) : null;
                locks.put(locStr, new LockData(locStr, ownerUuid, teamId > 0 ? teamId : -1));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public LockData getLock(Location loc) {
        return locks.get(serializeLocation(loc));
    }

    public void createLock(Location loc, UUID ownerUuid, int teamId) {
        String locStr = serializeLocation(loc);
        LockData lock = new LockData(locStr, ownerUuid, teamId);
        locks.put(locStr, lock);

        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "INSERT OR REPLACE INTO genscore_locks (location, owner_uuid, team_id) VALUES (?, ?, ?)")) {
            stmt.setString(1, locStr);
            stmt.setString(2, ownerUuid != null ? ownerUuid.toString() : "");
            stmt.setInt(3, teamId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void removeLock(Location loc) {
        String locStr = serializeLocation(loc);
        locks.remove(locStr);

        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement stmt = conn.prepareStatement("DELETE FROM genscore_locks WHERE location = ?")) {
            stmt.setString(1, locStr);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static String serializeLocation(Location loc) {
        return loc.getWorld().getName() + "," + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
    }
}
