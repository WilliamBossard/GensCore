package fr.gens.core.utils;

import fr.gens.core.CorePlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;


public class ConfigManager {

    private final CorePlugin plugin;
    private final Map<String, FileConfiguration> configs = new ConcurrentHashMap<>();
    private final Map<String, File> files = new ConcurrentHashMap<>();

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

        // Autonettoyeur : on sauvegarde immédiatement la configuration pour que SnakeYAML efface les clés dupliquées
        try {
            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }

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
     * Save a configuration to disk (Synchronous).
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
     * Save a configuration to disk asynchronously to prevent blocking the main thread.
     */
    public void saveConfigAsync(String fileName) {
        if (configs.containsKey(fileName) && files.containsKey(fileName)) {
            // Sérialisation synchrone en mémoire
            String data = configs.get(fileName).saveToString();
            File file = files.get(fileName);
            
            Runnable writeTask = () -> {
                try {
                    java.nio.file.Files.writeString(file.toPath(), data, StandardCharsets.UTF_8);
                } catch (IOException e) {
                    plugin.getLogger().severe("Impossible de sauvegarder la configuration: " + fileName);
                    e.printStackTrace();
                }
            };

            if (plugin.isEnabled()) {
                plugin.getFoliaLib().getScheduler().runAsync((wrappedTask) -> writeTask.run());
            } else {
                writeTask.run();
            }
        }
    }

    /**
     * Save the main plugin configuration (config.yml) asynchronously.
     */
    public void saveMainConfigAsync() {
        String data = plugin.getConfig().saveToString();
        File file = new File(plugin.getDataFolder(), "config.yml");
        
        Runnable writeTask = () -> {
            try {
                java.nio.file.Files.writeString(file.toPath(), data, StandardCharsets.UTF_8);
            } catch (IOException e) {
                plugin.getLogger().severe("Impossible de sauvegarder la configuration principale (config.yml)");
                e.printStackTrace();
            }
        };

        if (plugin.isEnabled()) {
            plugin.getFoliaLib().getScheduler().runAsync((wrappedTask) -> writeTask.run());
        } else {
            writeTask.run();
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



