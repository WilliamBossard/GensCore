package fr.gens.core.modules.loot;

import fr.gens.core.CorePlugin;
import fr.gens.core.database.LootDAO;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class LootManager {

    private final CorePlugin plugin;
    private final LootDAO lootDAO;
    private final Map<String, LootChestData> chestsCache = new ConcurrentHashMap<>();
    private final Map<UUID, Set<String>> playerLootedCache = new ConcurrentHashMap<>();

    public LootManager(CorePlugin plugin) {
        this.plugin = plugin;
        this.lootDAO = new LootDAO(plugin);
        this.lootDAO.initDatabase();

        migrateYamlIfPresent();
        loadChests();
    }

    public LootDAO getLootDAO() {
        return lootDAO;
    }

    /**
     * Migration automatique et transparente des anciens fichiers YAML (chests.yml et playerdata/)
     * vers la base de données relationnelle SQLite.
     */
    private void migrateYamlIfPresent() {
        File dataFolder = new File(plugin.getDataFolder(), "lootr");
        File chestsFile = new File(dataFolder, "chests.yml");

        if (chestsFile.exists() && chestsFile.length() > 0) {
            plugin.getLogger().info("[Lootr] Migration des coffres depuis chests.yml vers SQLite en cours...");
            try {
                FileConfiguration chestsConfig = YamlConfiguration.loadConfiguration(chestsFile);
                int migratedCount = 0;
                for (String key : chestsConfig.getKeys(false)) {
                    ConfigurationSection section = chestsConfig.getConfigurationSection(key);
                    if (section != null) {
                        String lootTable = section.getString("lootTable", "minecraft:chests/simple_dungeon");
                        long seed = section.getLong("seed", 0);
                        int size = section.getInt("size", 27);
                        lootDAO.saveChest(key, lootTable, seed, size);
                        migratedCount++;
                    }
                }
                plugin.getLogger().info("[Lootr] Migration de " + migratedCount + " coffres terminée avec succès.");
                File backup = new File(dataFolder, "chests.yml.migrated");
                chestsFile.renameTo(backup);
            } catch (Exception e) {
                plugin.getLogger().warning("[Lootr] Erreur lors de la migration de chests.yml : " + e.getMessage());
            }
        }

        File playerDataFolder = new File(dataFolder, "playerdata");
        if (playerDataFolder.exists() && playerDataFolder.isDirectory()) {
            File[] files = playerDataFolder.listFiles((dir, name) -> name.endsWith(".yml"));
            if (files != null && files.length > 0) {
                plugin.getLogger().info("[Lootr] Migration des inventaires joueurs (" + files.length + " fichiers) vers SQLite...");
                for (File file : files) {
                    try {
                        String name = file.getName().replace(".yml", "");
                        UUID uuid = UUID.fromString(name);
                        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
                        for (String key : config.getKeys(false)) {
                            if (config.contains(key + ".items")) {
                                List<?> list = config.getList(key + ".items");
                                if (list != null) {
                                    ItemStack[] items = new ItemStack[list.size()];
                                    for (int i = 0; i < list.size(); i++) {
                                        Object obj = list.get(i);
                                        items[i] = (obj instanceof ItemStack is) ? is : null;
                                    }
                                    lootDAO.savePlayerLoot(uuid, key, items);
                                }
                            }
                        }
                    } catch (Exception ignored) {}
                }
                File backupDir = new File(dataFolder, "playerdata_migrated");
                playerDataFolder.renameTo(backupDir);
                plugin.getLogger().info("[Lootr] Migration des inventaires joueurs terminée.");
            }
        }
    }

    public void loadChests() {
        chestsCache.clear();
        Map<String, LootChestData> loaded = lootDAO.loadAllChests();
        for (Map.Entry<String, LootChestData> entry : loaded.entrySet()) {
            Location loc = stringToLoc(entry.getKey());
            entry.getValue().setLocation(loc);
            chestsCache.put(entry.getKey(), entry.getValue());
        }
        plugin.getLogger().info("[Lootr] " + chestsCache.size() + " coffres chargés depuis SQLite.");
    }

    public void saveChests() {
        // En mode SQLite, chaque création/modification est persistée directement.
    }

    public String locToString(Location loc) {
        if (loc == null || loc.getWorld() == null) return "";
        return loc.getWorld().getName() + "|" + loc.getBlockX() + "|" + loc.getBlockY() + "|" + loc.getBlockZ();
    }

    public Location stringToLoc(String str) {
        if (str == null) return null;
        String[] split = str.split("\\|");
        if (split.length == 4) {
            World world = Bukkit.getWorld(split[0]);
            if (world != null) {
                return new Location(world, Integer.parseInt(split[1]), Integer.parseInt(split[2]), Integer.parseInt(split[3]));
            }
        }
        return null;
    }

    public boolean isLootChest(Location loc) {
        return chestsCache.containsKey(locToString(loc));
    }

    public LootChestData getLootChestData(Location loc) {
        return chestsCache.get(locToString(loc));
    }

    public void addLootChest(Location loc, String lootTable, long seed, int size) {
        String key = locToString(loc);
        chestsCache.put(key, new LootChestData(lootTable, seed, size, loc));
        plugin.getFoliaLib().getScheduler().runAsync(task -> {
            lootDAO.saveChest(key, lootTable, seed, size);
        });
    }

    public void removeLootChest(Location loc) {
        String key = locToString(loc);
        chestsCache.remove(key);
        for (Set<String> set : playerLootedCache.values()) {
            set.remove(key);
        }
        plugin.getFoliaLib().getScheduler().runAsync(task -> {
            lootDAO.deleteChest(key);
        });
    }

    public Map<String, LootChestData> getChestsCache() {
        return chestsCache;
    }

    // --- PLAYER DATA ---

    public void removePlayerCache(UUID uuid) {
        playerLootedCache.remove(uuid);
    }

    public ItemStack[] getPlayerLoot(UUID uuid, Location loc) {
        return lootDAO.getPlayerLoot(uuid, locToString(loc));
    }

    public void savePlayerLoot(UUID uuid, Location loc, ItemStack[] items) {
        String key = locToString(loc);
        playerLootedCache.computeIfAbsent(uuid, k -> ConcurrentHashMap.newKeySet()).add(key);
        plugin.getFoliaLib().getScheduler().runAsync(task -> {
            lootDAO.savePlayerLoot(uuid, key, items);
        });
    }

    public boolean hasPlayerLooted(UUID uuid, Location loc) {
        String key = locToString(loc);
        Set<String> set = playerLootedCache.get(uuid);
        if (set != null) {
            return set.contains(key);
        }

        // Chargement du cache joueur lors du premier test
        Set<String> loaded = lootDAO.loadLootedLocationsForPlayer(uuid);
        Set<String> concurrentSet = ConcurrentHashMap.newKeySet();
        concurrentSet.addAll(loaded);
        playerLootedCache.put(uuid, concurrentSet);
        return concurrentSet.contains(key);
    }

    public static class LootChestData {
        private final String lootTable;
        private final long seed;
        private final int size;
        private Location location;

        public LootChestData(String lootTable, long seed, int size) {
            this(lootTable, seed, size, null);
        }

        public LootChestData(String lootTable, long seed, int size, Location location) {
            this.lootTable = lootTable;
            this.seed = seed;
            this.size = size;
            this.location = location;
        }

        public Location getLocation() {
            return location;
        }

        public void setLocation(Location location) {
            this.location = location;
        }

        public String getLootTable() {
            return lootTable;
        }

        public long getSeed() {
            return seed;
        }

        public int getSize() {
            return size;
        }
    }
}
