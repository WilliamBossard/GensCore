package fr.gens.core;

import org.bukkit.configuration.file.FileConfiguration;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Base64;
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
        if (item == null) return null;
        return Base64.getEncoder().encodeToString(item.serializeAsBytes());
    }

    @SuppressWarnings("deprecation")
    public ItemStack itemStackFromBase64(String data) {
        if (data == null || data.isEmpty()) return null;
        
        if (data.startsWith("rO0AB")) {
            try {
                ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64.getDecoder().decode(data));
                org.bukkit.util.io.BukkitObjectInputStream dataInput = new org.bukkit.util.io.BukkitObjectInputStream(inputStream);
                ItemStack item = (ItemStack) dataInput.readObject();
                dataInput.close();
                return item;
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }
        
        try {
            return ItemStack.deserializeBytes(Base64.getDecoder().decode(data));
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public String itemStackArrayToBase64(ItemStack[] items) {
        if (items == null) return null;
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            java.io.DataOutputStream dataOutput = new java.io.DataOutputStream(outputStream);
            dataOutput.writeInt(items.length);
            for (ItemStack item : items) {
                if (item != null) {
                    byte[] bytes = item.serializeAsBytes();
                    dataOutput.writeInt(bytes.length);
                    dataOutput.write(bytes);
                } else {
                    dataOutput.writeInt(0);
                }
            }
            dataOutput.close();
            return Base64.getEncoder().encodeToString(outputStream.toByteArray());
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @SuppressWarnings("deprecation")
    public ItemStack[] itemStackArrayFromBase64(String data) {
        if (data == null || data.isEmpty()) return null;
        
        if (data.startsWith("rO0AB")) {
            try {
                ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64.getDecoder().decode(data));
                org.bukkit.util.io.BukkitObjectInputStream dataInput = new org.bukkit.util.io.BukkitObjectInputStream(inputStream);
                int size = dataInput.readInt();
                ItemStack[] items = new ItemStack[size];
                for (int i = 0; i < size; i++) {
                    items[i] = (ItemStack) dataInput.readObject();
                }
                dataInput.close();
                return items;
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }
        
        try {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64.getDecoder().decode(data));
            java.io.DataInputStream dataInput = new java.io.DataInputStream(inputStream);
            int size = dataInput.readInt();
            ItemStack[] items = new ItemStack[size];
            for (int i = 0; i < size; i++) {
                int len = dataInput.readInt();
                if (len > 0) {
                    byte[] bytes = new byte[len];
                    dataInput.readFully(bytes);
                    items[i] = ItemStack.deserializeBytes(bytes);
                } else {
                    items[i] = null;
                }
            }
            dataInput.close();
            return items;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
