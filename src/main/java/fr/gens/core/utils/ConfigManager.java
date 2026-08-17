package fr.gens.core.utils;

import fr.gens.core.CorePlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class ConfigManager {

    private final CorePlugin plugin;
    private final Map<String, FileConfiguration> configs = new HashMap<>();
    private final Map<String, File> files = new HashMap<>();

    public ConfigManager(CorePlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Get a FileConfiguration. Loads it if not already loaded.
     * @param fileName The path relative to the plugin's data folder (e.g. "modules.yml" or "modules/tabboard.yml").
     */
    public FileConfiguration getConfig(String fileName) {
        if (!configs.containsKey(fileName)) {
            loadConfig(fileName);
        }
        return configs.get(fileName);
    }

    /**
     * Loads or reloads a configuration file.
     * If the file doesn't exist, it will copy the default from the jar if it exists,
     * otherwise it will create a new empty file.
     */
    public void loadConfig(String fileName) {
        File file = new File(plugin.getDataFolder(), fileName);
        if (!file.exists()) {
            file.getParentFile().mkdirs();
            // Essayons de le copier depuis le jar
            try {
                plugin.saveResource(fileName, false);
            } catch (IllegalArgumentException e) {
                // Le fichier n'existe pas dans le jar, on le crée vide
                try {
                    file.createNewFile();
                } catch (IOException ex) {
                    plugin.getLogger().severe("Impossible de créer le fichier de configuration: " + fileName);
                }
            }
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(file);

        // Si on a des defaults dans le jar, on les ajoute
        InputStream defConfigStream = plugin.getResource(fileName);
        if (defConfigStream != null) {
            YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(defConfigStream, StandardCharsets.UTF_8));
            config.setDefaults(defConfig);
            config.options().copyDefaults(true);
            try {
                config.save(file);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        configs.put(fileName, config);
        files.put(fileName, file);
    }

    /**
     * Save a configuration to disk.
     */
    public void saveConfig(String fileName) {
        if (configs.containsKey(fileName) && files.containsKey(fileName)) {
            try {
                configs.get(fileName).save(files.get(fileName));
            } catch (IOException e) {
                plugin.getLogger().severe("Impossible de sauvegarder la configuration: " + fileName);
            }
        }
    }

    /**
     * Reload all loaded configurations.
     */
    public void reloadAll() {
        for (String fileName : configs.keySet()) {
            loadConfig(fileName);
        }
    }
}
