package fr.gens.core.modules.loot;

import fr.gens.core.CorePlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class LootManager {

    private final CorePlugin plugin;
    private final File dataFolder;
    private final File chestsFile;
    private FileConfiguration chestsConfig;
    private final File playerDataFolder;

    private final Map<String, LootChestData> chestsCache = new HashMap<>();

    public LootManager(CorePlugin plugin) {
        this.plugin = plugin;
        this.dataFolder = new File(plugin.getDataFolder(), "lootr");
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        
        this.chestsFile = new File(dataFolder, "chests.yml");
        this.playerDataFolder = new File(dataFolder, "playerdata");
        if (!playerDataFolder.exists()) {
            playerDataFolder.mkdirs();
        }

        loadChests();
    }

    public void loadChests() {
        if (!chestsFile.exists()) {
            try {
                chestsFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        chestsConfig = YamlConfiguration.loadConfiguration(chestsFile);
        chestsCache.clear();

        for (String key : chestsConfig.getKeys(false)) {
            ConfigurationSection section = chestsConfig.getConfigurationSection(key);
            if (section != null) {
                String lootTable = section.getString("lootTable");
                long seed = section.getLong("seed", 0);
                int size = section.getInt("size", 27);
                chestsCache.put(key, new LootChestData(lootTable, seed, size));
            }
        }
        plugin.getLogger().info("[Lootr] " + chestsCache.size() + " chests loaded.");
    }

    public void saveChests() {
        for (Map.Entry<String, LootChestData> entry : chestsCache.entrySet()) {
            chestsConfig.set(entry.getKey() + ".lootTable", entry.getValue().getLootTable());
            chestsConfig.set(entry.getKey() + ".seed", entry.getValue().getSeed());
            chestsConfig.set(entry.getKey() + ".size", entry.getValue().getSize());
        }
        try {
            chestsConfig.save(chestsFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String locToString(Location loc) {
        return loc.getWorld().getName() + "|" + loc.getBlockX() + "|" + loc.getBlockY() + "|" + loc.getBlockZ();
    }

    public Location stringToLoc(String str) {
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
        chestsCache.put(key, new LootChestData(lootTable, seed, size));
        chestsConfig.set(key + ".lootTable", lootTable);
        chestsConfig.set(key + ".seed", seed);
        chestsConfig.set(key + ".size", size);
        try {
            chestsConfig.save(chestsFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void removeLootChest(Location loc) {
        String key = locToString(loc);
        chestsCache.remove(key);
        chestsConfig.set(key, null);
        try {
            chestsConfig.save(chestsFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public Map<String, LootChestData> getChestsCache() {
        return chestsCache;
    }

    // --- PLAYER DATA ---

    private File getPlayerFile(UUID uuid) {
        return new File(playerDataFolder, uuid.toString() + ".yml");
    }

    public ItemStack[] getPlayerLoot(UUID uuid, Location loc) {
        File file = getPlayerFile(uuid);
        if (!file.exists()) return null;

        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        String key = locToString(loc);
        
        if (config.contains(key + ".items")) {
            List<?> list = config.getList(key + ".items");
            if (list != null) {
                ItemStack[] items = new ItemStack[list.size()];
                for (int i = 0; i < list.size(); i++) {
                    Object obj = list.get(i);
                    if (obj instanceof ItemStack) {
                        items[i] = (ItemStack) obj;
                    } else {
                        items[i] = null;
                    }
                }
                return items;
            }
        }
        return null;
    }

    public void savePlayerLoot(UUID uuid, Location loc, ItemStack[] items) {
        File file = getPlayerFile(uuid);
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        String key = locToString(loc);
        
        // Vérifier si l'inventaire est complètement vide, on peut nettoyer pour alléger
        boolean isEmpty = true;
        for (ItemStack item : items) {
            if (item != null && !item.getType().isAir()) {
                isEmpty = false;
                break;
            }
        }

        if (isEmpty) {
            // Optionnel : on peut garder un tableau vide ou juste null
            // Dans LootrPlugin, ils sauvegardent des tableaux remplis de null
        }

        config.set(key + ".items", items);

        try {
            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean hasPlayerLooted(UUID uuid, Location loc) {
        File file = getPlayerFile(uuid);
        if (!file.exists()) return false;
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        return config.contains(locToString(loc));
    }

    public static class LootChestData {
        private final String lootTable;
        private final long seed;
        private final int size;

        public LootChestData(String lootTable, long seed, int size) {
            this.lootTable = lootTable;
            this.seed = seed;
            this.size = size;
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
