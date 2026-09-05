package fr.gens.core.modules;

import fr.gens.core.CorePlugin;
import fr.gens.core.utils.DatabaseManager;
import fr.gens.core.utils.FloodgateUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.UUID;

public class BedrockSkinModule implements Module, Listener {

    private final CorePlugin plugin;
    private boolean enabled = false;

    public BedrockSkinModule(CorePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "bedrockskin";
    }

    @Override
    public String getDescription() {
        return "Gère les skins natifs pour les joueurs Bedrock et leurs avatars Web/Discord";
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void initDatabase(DatabaseManager dbManager) {
        dbManager.executeStatement("CREATE TABLE IF NOT EXISTS player_skins (uuid VARCHAR(36) PRIMARY KEY, hash VARCHAR(64));");
    }

    @Override
    public void enable() {
        enabled = true;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public void registerCommands(CorePlugin plugin) {
        // Pas de commandes pour ce module
    }

    @Override
    public void disable() {
        enabled = false;
        HandlerList.unregisterAll(this);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!enabled) return;
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        
        if (FloodgateUtil.isBedrockPlayer(uuid)) {
            // Delay 20 ticks (1s) to allow Geyser/Floodgate to initialize the session properly
            plugin.getFoliaLib().getScheduler().runLater((task) -> {
                if (!player.isOnline()) return;
                
                plugin.getFoliaLib().getScheduler().runAsync((asyncTask) -> {
                    try {
                        String xuid = org.geysermc.floodgate.api.FloodgateApi.getInstance().getPlayer(uuid).getXuid();
                        URL url = java.net.URI.create("https://api.geysermc.org/v2/skin/" + xuid).toURL();
                        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                        conn.setRequestMethod("GET");
                        conn.setConnectTimeout(5000);
                        conn.setReadTimeout(5000);
                        
                        if (conn.getResponseCode() == 200) {
                            try (InputStreamReader reader = new InputStreamReader(conn.getInputStream())) {
                                JsonObject json = new JsonParser().parse(reader).getAsJsonObject();
                                if (json.has("value") && json.has("signature")) {
                                    String value = json.get("value").getAsString();
                                    String signature = json.get("signature").getAsString();
                                    String hash = "";
                                    
                                    try {
                                        String decoded = new String(java.util.Base64.getDecoder().decode(value), java.nio.charset.StandardCharsets.UTF_8);
                                        JsonObject decodedJson = new JsonParser().parse(decoded).getAsJsonObject();
                                        if (decodedJson.has("textures")) {
                                            JsonObject textures = decodedJson.getAsJsonObject("textures");
                                            if (textures.has("SKIN")) {
                                                JsonObject skin = textures.getAsJsonObject("SKIN");
                                                if (skin.has("url")) {
                                                    String skinUrl = skin.get("url").getAsString();
                                                    hash = skinUrl.substring(skinUrl.lastIndexOf('/') + 1);
                                                }
                                            }
                                        }
                                    } catch (Exception ex) {
                                        plugin.getLogger().warning("Erreur lors du decodage de la texture Bedrock: " + ex.getMessage());
                                    }
                                    
                                    if (json.has("hash") && hash.isEmpty()) {
                                        hash = json.get("hash").getAsString();
                                    } else if (json.has("texture_id") && hash.isEmpty()) {
                                        hash = json.get("texture_id").getAsString();
                                    }
                                    
                                    if (!hash.isEmpty()) {
                                        plugin.getLogger().info("[BedrockSkinModule] Hash trouve pour " + player.getName() + " : " + hash);
                                        // Save hash to database for Web/Discord use
                                        plugin.getDatabaseManager().executeStatement(
                                            "REPLACE INTO player_skins (uuid, hash) VALUES ('" + uuid.toString() + "', '" + hash + "');"
                                        );
                                    } else {
                                        plugin.getLogger().warning("[BedrockSkinModule] Impossible de trouver le hash/texture_id dans la reponse de Geyser pour " + player.getName());
                                    }
                                    
                                    // Apply skin to player in game
                                    plugin.getFoliaLib().getScheduler().runAtEntity(player, (t) -> {
                                        PlayerProfile profile = player.getPlayerProfile();
                                        profile.setProperty(new ProfileProperty("textures", value, signature));
                                        player.setPlayerProfile(profile);
                                        plugin.getLogger().info("[BedrockSkinModule] Profil applique a " + player.getName() + " en jeu !");
                                    });
                                } else {
                                    plugin.getLogger().warning("[BedrockSkinModule] Le JSON de Geyser ne contient pas value ou signature pour " + player.getName());
                                }
                            }
                        }
                    } catch (Exception e) {
                        plugin.getLogger().warning("Impossible de recuperer le skin Bedrock pour " + player.getName() + ": " + e.getMessage());
                    }
                });
            }, 20L);
        }
    }

    /**
     * Helper pour obtenir l'URL de l'avatar du joueur (pour Discord, Web, etc)
     */
    public String getHeadUrl(UUID uuid, String name) {
        if (uuid != null) {
            // Chercher le hash en BDD
            try (java.sql.Connection conn = plugin.getDatabaseManager().getConnection();
                 java.sql.Statement stmt = conn.createStatement();
                 java.sql.ResultSet rs = stmt.executeQuery("SELECT hash FROM player_skins WHERE uuid = '" + uuid.toString() + "';")) {
                
                if (rs != null && rs.next()) {
                    String hash = rs.getString("hash");
                    if (hash != null && !hash.isEmpty()) {
                        return "https://mc-heads.net/avatar/" + hash;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        // Fallback standard pour les joueurs Java ou Bedrock sans hash en cache
        // On privilégie le nom plutôt que l'UUID car sur un serveur crack/offline,
        // l'UUID est un offline UUID qui retourne Steve sur crafthead.net.
        if (name != null && !name.isEmpty()) {
            return "https://crafthead.net/helm/" + name + ".png";
        } else if (uuid != null) {
            return "https://crafthead.net/helm/" + uuid.toString() + ".png";
        }
        return "https://crafthead.net/helm/Steve.png";
    }
}
