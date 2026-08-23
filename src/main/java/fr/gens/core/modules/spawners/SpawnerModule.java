package fr.gens.core.modules.spawners;


import fr.gens.core.CorePlugin;
import fr.gens.core.modules.Module;

import org.bukkit.Location;

import com.tcoded.folialib.wrapper.task.WrappedTask;




import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;


public class SpawnerModule implements Module {

    private final CorePlugin plugin;
    private boolean enabled;
    private boolean listenersRegistered = false;
    private SpawnerListener spawnerListener;
    private SpawnerGui spawnerGui;
    private SpawnerLootGui spawnerLootGui;
    private WrappedTask generationTask;
    private WrappedTask saveTask;
    
    private final Map<Location, SpawnerData> activeSpawners = new ConcurrentHashMap<>();
    private final Map<Long, java.util.List<SpawnerData>> spawnersByChunk = new ConcurrentHashMap<>();
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
        plugin.getFoliaLib().getImpl().runAsync((wrappedTask) -> loadSpawnersFromDB());
        
        // Register events
        if (!listenersRegistered) {
            SpawnerGui.setModule(this);
            SpawnerLootGui.setModule(this);
            this.spawnerListener = new SpawnerListener(plugin, this);
            this.spawnerGui = new SpawnerGui();
            this.spawnerLootGui = new SpawnerLootGui();
            plugin.getServer().getPluginManager().registerEvents(this.spawnerListener, plugin);
            plugin.getServer().getPluginManager().registerEvents(this.spawnerGui, plugin);
            plugin.getServer().getPluginManager().registerEvents(this.spawnerLootGui, plugin);
            listenersRegistered = true;
        }
        
        if (generationTask == null) {
            plugin.getFoliaLib().getImpl().runTimer((wrappedTask) -> {
            generationTask = wrappedTask;
            spawnerManager.generateTick();
        }, 5L, 5L);
        }
        
        if (saveTask == null) {
            plugin.getFoliaLib().getImpl().runTimerAsync((wrappedTask) -> {
            saveTask = wrappedTask;
            saveAllSpawnersToDB();
        }, 6000L, 6000L);
        }
        
        // Update holograms if re-enabled
        if (!activeSpawners.isEmpty()) {
            for (SpawnerData data : activeSpawners.values()) {
                plugin.getFoliaLib().getImpl().runAtLocation(data.getLocation(), (t2) -> {
                    spawnerManager.updateHologram(data);
                });
            }
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
        
        if (this.spawnerListener != null) {
            org.bukkit.event.HandlerList.unregisterAll(this.spawnerListener);
            this.spawnerListener = null;
        }
        if (this.spawnerGui != null) {
            org.bukkit.event.HandlerList.unregisterAll(this.spawnerGui);
            this.spawnerGui = null;
        }
        if (this.spawnerLootGui != null) {
            org.bukkit.event.HandlerList.unregisterAll(this.spawnerLootGui);
            this.spawnerLootGui = null;
        }
        listenersRegistered = false;
        
        // Mettre ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â  jour les holograms pour afficher "DÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â©sactivÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â©"
        for (SpawnerData data : activeSpawners.values()) {
            spawnerManager.updateHologram(data);
        }
        
        saveAllSpawnersToDB();
        // On ne clear() pas activeSpawners pour continuer de protÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â©ger les blocs
        
        plugin.getLangManager().sendConsoleMessage("spawnermodule.log_2");
    }

    @Override
    public void registerCommands(CorePlugin plugin) {
        if (plugin.getCommandManager() != null && plugin.getCommandManager().getAnnotationParser() != null) {
            plugin.getCommandManager().getAnnotationParser().parse(new SpawnerCommand(plugin, this));
        }
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
    
    public java.util.List<SpawnerData> getSpawnersInChunk(long chunkKey) {
        return spawnersByChunk.getOrDefault(chunkKey, java.util.Collections.emptyList());
    }
    
    public void addSpawner(SpawnerData data) {
        activeSpawners.put(data.getLocation(), data);
        long chunkKey = org.bukkit.Chunk.getChunkKey(data.getLocation().getBlockX() >> 4, data.getLocation().getBlockZ() >> 4);
        spawnersByChunk.computeIfAbsent(chunkKey, k -> new java.util.concurrent.CopyOnWriteArrayList<>()).add(data);
        spawnerManager.updateHologram(data);
        plugin.getFoliaLib().getImpl().runAsync((wrappedTask) -> saveSpawnerToDB(data));
    }
    
    public void removeSpawner(Location loc) {
        SpawnerData data = activeSpawners.remove(loc);
        if (data != null) {
            long chunkKey = org.bukkit.Chunk.getChunkKey(loc.getBlockX() >> 4, loc.getBlockZ() >> 4);
            java.util.List<SpawnerData> chunkList = spawnersByChunk.get(chunkKey);
            if (chunkList != null) {
                chunkList.remove(data);
                if (chunkList.isEmpty()) spawnersByChunk.remove(chunkKey);
            }
            spawnerManager.removeHologram(loc);
            plugin.getFoliaLib().getImpl().runAsync((wrappedTask) -> deleteSpawnerFromDB(data.getId()));
        }
    }
    
    public SpawnerData getSpawnerAt(Location loc) {
        Location blockLoc = new Location(loc.getWorld(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
        return activeSpawners.get(blockLoc);
    }
    
    private void loadSpawnersFromDB() {
        int count = this.spawnerDAO.loadSpawners(activeSpawners);
        
        // Populate spawnersByChunk
        spawnersByChunk.clear();
        for (SpawnerData data : activeSpawners.values()) {
            long chunkKey = org.bukkit.Chunk.getChunkKey(data.getLocation().getBlockX() >> 4, data.getLocation().getBlockZ() >> 4);
            spawnersByChunk.computeIfAbsent(chunkKey, k -> new java.util.concurrent.CopyOnWriteArrayList<>()).add(data);
        }
        
        // Need to spawn holograms synchronously for the loaded spawners
        for (SpawnerData data : activeSpawners.values()) {
            plugin.getFoliaLib().getImpl().runAtLocation(data.getLocation(), (t2) -> {
                spawnerManager.updateHologram(data);
            });
        }
        
        plugin.getLogger().info("[SpawnerModule] " + count + " spawners loaded.");
    }
    
    public void saveSpawnerToDB(SpawnerData data) {
        this.spawnerDAO.saveSpawner(data);
    }
    
    private void saveAllSpawnersToDB() {
        this.spawnerDAO.saveAllSpawners(activeSpawners.values());
    }
    
    private void deleteSpawnerFromDB(UUID id) {
        this.spawnerDAO.deleteSpawner(id);
    }
}

