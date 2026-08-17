package fr.gens.core.utils;

import fr.gens.core.CorePlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class LangManager {

    private final CorePlugin plugin;
    private FileConfiguration langConfig;
    private File langFile;
    private String currentLang;

    public LangManager(CorePlugin plugin) {
        this.plugin = plugin;
        loadLang();
    }

    public void loadLang() {
        this.currentLang = plugin.getConfig().getString("lang", "fr_FR");
        File langDir = new File(plugin.getDataFolder(), "lang");
        if (!langDir.exists()) {
            langDir.mkdirs();
        }

        // Créer les fichiers par défaut s'ils n'existent pas
        createDefaultLangFile("fr_FR.yml");
        createDefaultLangFile("en_US.yml");

        langFile = new File(langDir, currentLang + ".yml");
        if (!langFile.exists()) {
            plugin.getLogger().warning("Fichier de langue " + currentLang + ".yml introuvable. Chargement de fr_FR.yml par defaut.");
            langFile = new File(langDir, "fr_FR.yml");
        }

        langConfig = YamlConfiguration.loadConfiguration(langFile);
        plugin.getLogger().info("Langue chargee : " + langFile.getName());
    }

    private void createDefaultLangFile(String fileName) {
        File file = new File(plugin.getDataFolder() + File.separator + "lang", fileName);
        if (!file.exists()) {
            try (InputStream in = plugin.getResource("lang/" + fileName)) {
                if (in != null) {
                    plugin.saveResource("lang/" + fileName, false);
                } else {
                    // Création d'un fichier vide avec quelques traductions basiques si non présent dans le jar
                    FileConfiguration defConfig = YamlConfiguration.loadConfiguration(file);
                    defConfig.set("prefix", "<gray>[<gradient:gold:yellow>GensCore</gradient>] <reset>");
                    defConfig.set("error.no_permission", "<red>Vous n'avez pas la permission.</red>");
                    defConfig.set("economy.balance", "<yellow>Vous avez <gold><amount></gold> $.</yellow>");
                    defConfig.save(file);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Récupère une chaîne de caractère brute depuis la configuration.
     */
    public String getRaw(String path) {
        return langConfig.getString(path, "<red>Missing Translation: " + path + "</red>");
    }

    /**
     * Récupère et parse un message avec MiniMessage sans arguments.
     */
    public Component get(String path) {
        String message = getRaw(path);
        // On remplace les {prefix} automatiquement
        message = message.replace("{prefix}", langConfig.getString("prefix", ""));
        return MiniMessage.miniMessage().deserialize(message);
    }

    /**
     * Récupère et parse un message avec MiniMessage et des arguments de remplacement.
     * Exemple d'usage: get("economy.balance", Placeholder.parsed("amount", "100.0"))
     */
    public Component get(String path, TagResolver... placeholders) {
        String message = getRaw(path);
        message = message.replace("{prefix}", langConfig.getString("prefix", ""));
        return MiniMessage.miniMessage().deserialize(message, placeholders);
    }

    /**
     * Envoie un message au CommandSender avec la traduction correspondante.
     */
    public void sendMessage(org.bukkit.command.CommandSender sender, String path) {
        sender.sendMessage(get(path));
    }

    /**
     * Envoie un message au CommandSender avec la traduction correspondante et des placeholders.
     */
    public void sendMessage(org.bukkit.command.CommandSender sender, String path, TagResolver... placeholders) {
        sender.sendMessage(get(path, placeholders));
    }

    public void sendConsoleMessage(String path, TagResolver... placeholders) {
        Component msg = get(path, placeholders);
        plugin.getServer().getConsoleSender().sendMessage(msg);
    }

    public void sendConsoleWarning(String path, TagResolver... placeholders) {
        Component msg = get(path, placeholders);
        plugin.getServer().getConsoleSender().sendMessage(msg); // Le logger vanilla ne supporte pas MiniMessage, on utilise ConsoleSender
    }

    public void sendConsoleError(String path, TagResolver... placeholders) {
        Component msg = get(path, placeholders);
        plugin.getServer().getConsoleSender().sendMessage(msg);
    }
}
