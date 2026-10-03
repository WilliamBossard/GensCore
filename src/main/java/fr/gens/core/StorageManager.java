package fr.gens.core;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;

public class StorageManager {

    private final CorePlugin plugin;

    public StorageManager(CorePlugin plugin) {
        this.plugin = plugin;
        plugin.saveDefaultConfig(); // Sauvegarde config.yml initiale si elle n'existe pas
        
        plugin.getConfigManager().getConfig("modules/tomb.yml").addDefault("modules.tomb.store_xp", true);
        plugin.getConfigManager().getConfig("modules/tomb.yml").addDefault("modules.tomb.xp_keep_percentage", 100);
        plugin.getConfigManager().getConfig("modules/tomb.yml").addDefault("modules.tomb.expiration_time_seconds", 3600);
        
        plugin.getConfig().options().copyDefaults(true);
        plugin.saveConfig();
    }

    public FileConfiguration getConfig() {
        return plugin.getConfig();
    }
    
    public void saveConfig() {
        plugin.saveConfig();
    }

    public String itemStackToBase64(ItemStack item) {
        return fr.gens.core.utils.ItemSerializer.toBase64(item);
    }

    public ItemStack itemStackFromBase64(String data) {
        return fr.gens.core.utils.ItemSerializer.fromBase64(data);
    }

    public String itemStackArrayToBase64(ItemStack[] items) {
        return fr.gens.core.utils.ItemSerializer.itemStackArrayToBase64(items);
    }

    public ItemStack[] itemStackArrayFromBase64(String data) {
        return fr.gens.core.utils.ItemSerializer.itemStackArrayFromBase64(data);
    }
}
