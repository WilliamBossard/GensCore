package fr.gens.core.modules.spawners;

import org.bukkit.Bukkit;

import fr.gens.core.CorePlugin;
import fr.gens.core.modules.Module;

import org.bukkit.Location;

import org.bukkit.scheduler.BukkitTask;





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
    private fr.gens.core.database.SpawnerDAO spawnerDAO;

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
        return "GÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â©nÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â©rateurs d'objets et d'expÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â©rience (remplacement de SmartSpawner)";
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void initDatabase(fr.gens.core.utils.DatabaseManager dbManager) {
        dbManager.executeStatement("CREATE TABLE IF NOT EXISTS spawners (id VARCHAR(36) PRIMARY KEY, world VARCHAR(255) NOT NULL, x DOUBLE NOT NULL, y DOUBLE NOT NULL, z DOUBLE NOT NULL, type VARCHAR(50) NOT NULL, stack_count INTEGER NOT NULL DEFAULT 1, stored_exp INTEGER NOT NULL DEFAULT 0, stored_items TEXT NOT NULL, last_interacted VARCHAR(16), storage_level INTEGER DEFAULT 0, exp_level INTEGER DEFAULT 0, speed_level INTEGER DEFAULT 0, is_loot_chest INTEGER DEFAULT 0);");
    }

    @Override
    public void enable() {
        this.enabled = true;
        
        // Setup config defaults
        if (!plugin.getConfigManager().getConfig("modules/spawners.yml").contains("spawners.delay")) {
            plugin.getConfigManager().getConfig("modules/spawners.yml").set("spawners.delay", 25); // base seconds not really used now, handled per level
            plugin.getConfigManager().getConfig("modules/spawners.yml").set("spawners.holograms", true);
            plugin.getConfigManager().getConfig("modules/spawners.yml").set("spawners.hoppers", false);
            plugin.getConfigManager().getConfig("modules/spawners.yml").set("spawners.max-stack", 100000);
            plugin.getConfigManager().getConfig("modules/spawners.yml").set("spawners.upgrade-base-cost", 1000.0);
            
            // Clean up old config
            plugin.getConfigManager().getConfig("modules/spawners.yml").set("spawners.types", null);
        }
        
        if (!plugin.getConfigManager().getConfig("modules/spawners.yml").contains("spawners.vanilla-require-silktouch")) {
            plugin.getConfigManager().getConfig("modules/spawners.yml").set("spawners.vanilla-require-silktouch", true);
        }
        
        plugin.getConfigManager().saveConfig("modules/spawners.yml");

        spawnerManager.loadTypes();
        
        this.spawnerDAO = new fr.gens.core.database.SpawnerDAO(plugin);
        this.spawnerDAO.initDatabase();
        
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
        
        org.bukkit.command.PluginCommand cmd = plugin.getCommand("spawner");
        if (cmd != null) {
            cmd.setExecutor(new SpawnerCommand(plugin, this));
        }
        
        plugin.getLangManager().sendConsoleMessage("spawnermodule.log_1");
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
        
        // Mettre ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â  jour les holograms pour afficher "DÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â©sactivÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â©"
        for (SpawnerData data : activeSpawners.values()) {
            spawnerManager.updateHologram(data);
        }
        
        saveAllSpawnersToDB();
        // On ne clear() pas activeSpawners pour continuer de protÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â©ger les blocs
        
        plugin.getLangManager().sendConsoleMessage("spawnermodule.log_2");
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
        int count = this.spawnerDAO.loadSpawners(activeSpawners);
        
        // Need to spawn holograms synchronously for the loaded spawners
        Bukkit.getScheduler().runTask(plugin, () -> {
            for (SpawnerData data : activeSpawners.values()) {
                spawnerManager.updateHologram(data);
            }
        });
        
        plugin.getLogger().info("[SpawnerModule] " + count + " spawners loaded.");
    }
    
    public void saveSpawnerToDB(SpawnerData data) {
        this.spawnerDAO.saveSpawner(data);
    }
    
    private void saveAllSpawnersToDB() {
        for (SpawnerData data : activeSpawners.values()) {
            saveSpawnerToDB(data);
        }
    }
    
    private void deleteSpawnerFromDB(UUID id) {
        this.spawnerDAO.deleteSpawner(id);
    }
}

