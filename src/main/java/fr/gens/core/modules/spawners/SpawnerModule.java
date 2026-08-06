package fr.gens.core.modules.spawners;

import fr.gens.core.CorePlugin;
import fr.gens.core.modules.Module;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitTask;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SpawnerModule implements Module {

    private final CorePlugin plugin;
    private boolean enabled;
    private boolean listenersRegistered = false;
    private BukkitTask generationTask;
    private BukkitTask saveTask;
    
    private final Map<Location, SpawnerData> activeSpawners = new ConcurrentHashMap<>();
    private final SpawnerManager spawnerManager;

    public SpawnerModule(CorePlugin plugin) {
        this.plugin = plugin;
        this.spawnerManager = new SpawnerManager(this);
    }

    @Override
    public String getName() {
        return "Spawners";
    }

    @Override
    public String getDescription() {
        return "Générateurs d'objets et d'expérience (remplacement de SmartSpawner)";
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void enable() {
        this.enabled = true;
        
        // Setup config defaults
        if (!plugin.getConfig().contains("spawners.delay")) {
            plugin.getConfig().set("spawners.delay", 25); // base seconds not really used now, handled per level
            plugin.getConfig().set("spawners.holograms", true);
            plugin.getConfig().set("spawners.hoppers", false);
            plugin.getConfig().set("spawners.max-stack", 100000);
            plugin.getConfig().set("spawners.upgrade-base-cost", 1000.0);
            
            // Clean up old config
            plugin.getConfig().set("spawners.types", null);
        }
        
        if (!plugin.getConfig().contains("spawners.vanilla-require-silktouch")) {
            plugin.getConfig().set("spawners.vanilla-require-silktouch", true);
        }
        
        plugin.saveConfig();

        spawnerManager.loadTypes();
        
        // Load from DB
        Bukkit.getScheduler().runTaskAsynchronously(plugin, this::loadSpawnersFromDB);
        
        // Register events
        if (!listenersRegistered) {
            SpawnerGui.setModule(this);
            SpawnerLootGui.setModule(this);
            plugin.getServer().getPluginManager().registerEvents(new SpawnerListener(plugin, this), plugin);
            plugin.getServer().getPluginManager().registerEvents(new SpawnerGui(), plugin);
            plugin.getServer().getPluginManager().registerEvents(new SpawnerLootGui(), plugin);
            listenersRegistered = true;
        }
        
        if (generationTask == null) {
            generationTask = Bukkit.getScheduler().runTaskTimer(plugin, spawnerManager::generateTick, 5L, 5L);
        }
        
        if (saveTask == null) {
            saveTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::saveAllSpawnersToDB, 6000L, 6000L);
        }
        
        // Update holograms if re-enabled
        if (!activeSpawners.isEmpty()) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                for (SpawnerData data : activeSpawners.values()) {
                    spawnerManager.updateHologram(data);
                }
            });
        }
        
        plugin.getCommand("spawner").setExecutor(new SpawnerCommand(plugin, this));
        
        plugin.getLogger().info("[SpawnerModule] Activé.");
    }

    @Override
    public void disable() {
        this.enabled = false;
        if (generationTask != null) {
            generationTask.cancel();
            generationTask = null;
        }
        if (saveTask != null) {
            saveTask.cancel();
            saveTask = null;
        }
        
        // Mettre à jour les holograms pour afficher "Désactivé"
        for (SpawnerData data : activeSpawners.values()) {
            spawnerManager.updateHologram(data);
        }
        
        saveAllSpawnersToDB();
        // On ne clear() pas activeSpawners pour continuer de protéger les blocs
        
        plugin.getLogger().info("[SpawnerModule] Désactivé.");
    }
    
    public CorePlugin getPlugin() {
        return plugin;
    }
    
    public SpawnerManager getSpawnerManager() {
        return spawnerManager;
    }
    
    public Map<Location, SpawnerData> getActiveSpawners() {
        return activeSpawners;
    }
    
    public void addSpawner(SpawnerData data) {
        activeSpawners.put(data.getLocation(), data);
        spawnerManager.updateHologram(data);
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> saveSpawnerToDB(data));
    }
    
    public void removeSpawner(Location loc) {
        SpawnerData data = activeSpawners.remove(loc);
        if (data != null) {
            spawnerManager.removeHologram(loc);
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> deleteSpawnerFromDB(data.getId()));
        }
    }
    
    public SpawnerData getSpawnerAt(Location loc) {
        Location blockLoc = new Location(loc.getWorld(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
        return activeSpawners.get(blockLoc);
    }
    
    private void loadSpawnersFromDB() {
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
                
                // Need to spawn holograms synchronously
                Bukkit.getScheduler().runTask(plugin, () -> spawnerManager.updateHologram(data));
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Erreur lors du chargement des spawners depuis SQLite.");
            e.printStackTrace();
        }
        plugin.getLogger().info("[SpawnerModule] " + count + " spawners chargés.");
    }
    
    public void saveSpawnerToDB(SpawnerData data) {
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
    
    private void saveAllSpawnersToDB() {
        for (SpawnerData data : activeSpawners.values()) {
            saveSpawnerToDB(data);
        }
    }
    
    private void deleteSpawnerFromDB(UUID id) {
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement stmt = conn.prepareStatement("DELETE FROM spawners WHERE id = ?")) {
            stmt.setString(1, id.toString());
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
