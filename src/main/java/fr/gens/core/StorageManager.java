package fr.gens.core;

import org.bukkit.configuration.file.FileConfiguration;

import java.io.File;
import java.io.IOException;
import org.bukkit.inventory.ItemStack;

public class StorageManager {

    private final CorePlugin plugin;
    private FileConfiguration dataConfig;
    private File dataFile;

    public StorageManager(CorePlugin plugin) {
        this.plugin = plugin;
        plugin.saveDefaultConfig(); // Sauvegarde config.yml initiale si elle n'existe pas
        
        plugin.getConfigManager().getConfig("modules/tomb.yml").addDefault("modules.tomb.store_xp", true);
        plugin.getConfigManager().getConfig("modules/tomb.yml").addDefault("modules.tomb.xp_keep_percentage", 100);
        plugin.getConfigManager().getConfig("modules/tomb.yml").addDefault("modules.tomb.expiration_time_seconds", 3600);
        
        plugin.getConfig().options().copyDefaults(true);
        plugin.saveConfig();
        
        initDataFile(); // Initialise data.yml
    }

    private void initDataFile() {
        // Désactivé car SQLite est maintenant utilisé pour les données principales.
        // Les modules comme TeleportBack qui l'utilisent temporairement le créeront si besoin
        // via leur propre logique ou utiliseront la configuration par défaut.
    }

    public FileConfiguration getData() {
        return dataConfig;
    }

    public void saveData() {
        if (dataConfig != null && dataFile != null) {
            try {
                dataConfig.save(dataFile);
            } catch (IOException e) {
                plugin.getLangManager().sendConsoleError("storagemanager.log_1");
            }
        }
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
