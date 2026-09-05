package fr.gens.core.utils;

import fr.gens.core.CorePlugin;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class UpdateChecker implements Listener {

    private final CorePlugin plugin;
    private final String repoName = "WilliamBossard/GensCore";
    private String latestVersion = null;
    private String updateUrl = null;
    private boolean updateAvailable = false;

    public UpdateChecker(CorePlugin plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void checkForUpdates() {
        plugin.getFoliaLib().getScheduler().runAsync((wrappedTask) -> {
            try {
                    URL url = java.net.URI.create("https://api.github.com/repos/" + repoName + "/releases/latest").toURL();
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    conn.setConnectTimeout(5000);
                    conn.setReadTimeout(5000);
                    conn.setRequestProperty("Accept", "application/vnd.github.v3+json");

                    int responseCode = conn.getResponseCode();
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        InputStreamReader reader = new InputStreamReader(conn.getInputStream());
                        JsonObject json = new JsonParser().parse(reader).getAsJsonObject();
                        reader.close();

                        latestVersion = json.get("tag_name").getAsString();
                        updateUrl = json.get("html_url").getAsString();

                        String currentVersion = plugin.getPluginMeta().getVersion();

                    if (!currentVersion.equalsIgnoreCase(latestVersion)) {
                        updateAvailable = true;
                        
                        plugin.getLangManager().sendConsoleMessage("core.update_available_console_1");
                        plugin.getLogger().warning("========================================");
                        plugin.getLogger().warning("GensCore Update Available!");
                        plugin.getLogger().warning("Current: " + currentVersion + " | Latest: " + latestVersion);
                        plugin.getLogger().warning("Download: " + updateUrl);
                        plugin.getLogger().warning("========================================");
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to check for updates: " + e.getMessage());
            }
        });
    }

    @EventHandler
    public void onAdminJoin(PlayerJoinEvent event) {
        if (updateAvailable && (event.getPlayer().hasPermission("genscore.admin") || event.getPlayer().isOp())) {
            plugin.getLangManager().sendMessage(event.getPlayer(), "core.update_available_chat", 
                net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.parsed("version", latestVersion),
                net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.parsed("url", updateUrl));
        }
    }
}
