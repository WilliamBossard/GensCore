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
    private final java.util.Map<UUID, String> skinHashCache = new java.util.concurrent.ConcurrentHashMap<>();
    private final java.util.Map<UUID, String[]> skinPropertyCache = new java.util.concurrent.ConcurrentHashMap<>();

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
        dbManager.executeStatement("CREATE TABLE IF NOT EXISTS player_skins (uuid VARCHAR(36) PRIMARY KEY, hash VARCHAR(64), texture_value TEXT, texture_signature TEXT);");
        dbManager.addColumnIfNotExists("player_skins", "texture_value", "TEXT");
        dbManager.addColumnIfNotExists("player_skins", "texture_signature", "TEXT");
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
        skinHashCache.clear();
        skinPropertyCache.clear();
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
                    int maxTries = 3;
                    int currentTry = 0;
                    boolean success = false;
                    
                    while (currentTry < maxTries && !success) {
                        currentTry++;
                        try {
                            org.geysermc.floodgate.api.player.FloodgatePlayer fgPlayer = org.geysermc.floodgate.api.FloodgateApi.getInstance().getPlayer(uuid);
                            if (fgPlayer == null) return;
                            String xuid = fgPlayer.getXuid();
                            URL url = java.net.URI.create("https://api.geysermc.org/v2/skin/" + xuid).toURL();
                            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                            conn.setRequestMethod("GET");
                            conn.setConnectTimeout(5000);
                            conn.setReadTimeout(5000);
                            
                            int responseCode = conn.getResponseCode();
                            if (responseCode == 200) {
                                success = true;
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
                                            // Save hash and textures to database for Web/Discord/Head use
                                            skinHashCache.put(uuid, hash);
                                            skinPropertyCache.put(uuid, new String[]{value, signature});
                                            try (java.sql.Connection dbConn = plugin.getDatabaseManager().getConnection();
                                                 java.sql.PreparedStatement pstmt = dbConn.prepareStatement("REPLACE INTO player_skins (uuid, hash, texture_value, texture_signature) VALUES (?, ?, ?, ?)")) {
                                                pstmt.setString(1, uuid.toString());
                                                pstmt.setString(2, hash);
                                                pstmt.setString(3, value);
                                                pstmt.setString(4, signature);
                                                pstmt.executeUpdate();
                                            } catch (Exception e) {
                                                plugin.getLogger().warning("Erreur lors de la sauvegarde du skin en base de donnees: " + e.getMessage());
                                            }
                                        } else {
                                            plugin.getLogger().warning("[BedrockSkinModule] Impossible de trouver le hash/texture_id dans la reponse de Geyser pour " + player.getName());
                                        }
                                        
                                        // Apply skin to player in game
                                        plugin.getFoliaLib().getScheduler().runAtEntity(player, (t) -> {
                                            if (!player.isOnline()) return;
                                            PlayerProfile profile = player.getPlayerProfile();
                                            profile.setProperty(new ProfileProperty("textures", value, signature));
                                            player.setPlayerProfile(profile);
                                            plugin.getLogger().info("[BedrockSkinModule] Profil appliqué à " + player.getName() + " en jeu !");

                                            // NOTE ARCHITECTURALE (Rafraîchissement visuel du Skin) :
                                            // Paper applique le PlayerProfile en mémoire, mais les paquets de spawn
                                            // du joueur ont déjà été reçus par les autres clients. Pour forcer le re-rendu
                                            // de la texture sans nécessiter une téléportation de monde, on alterne hidePlayer / showPlayer.
                                            for (Player other : Bukkit.getOnlinePlayers()) {
                                                if (other != null && other.isOnline() && !other.equals(player)) {
                                                    plugin.getFoliaLib().getScheduler().runAtEntity(other, (otherTask) -> {
                                                        if (other.isOnline() && other.canSee(player)) {
                                                            other.hidePlayer(plugin, player);
                                                            other.showPlayer(plugin, player);
                                                        }
                                                    });
                                                }
                                            }
                                        });
                                    } else {
                                        plugin.getLogger().warning("[BedrockSkinModule] Le JSON de Geyser ne contient pas value ou signature pour " + player.getName());
                                    }
                                }
                            } else if (responseCode == 429) {
                                plugin.getLogger().warning("[BedrockSkinModule] Rate limite par Geyser (Essai " + currentTry + "/" + maxTries + ")");
                                if (currentTry < maxTries) {
                                    Thread.sleep(2000L * currentTry); // Backoff lineaire
                                }
                            } else {
                                plugin.getLogger().warning("[BedrockSkinModule] Code de reponse inattendu : " + responseCode);
                                success = true; // Pas la peine de reessayer si c'est une autre erreur 
                            }
                        } catch (Throwable e) {
                            plugin.getLogger().warning("Impossible de recuperer le skin Bedrock pour " + player.getName() + " (Essai " + currentTry + ") : " + e.getMessage());
                            if (currentTry < maxTries) {
                                try { Thread.sleep(2000L * currentTry); } catch(InterruptedException ignored){}
                            }
                        }
                    }
                });
            }, 20L);
        }
    }

    /**
     * Helper pour obtenir l'URL de l'avatar du joueur (pour Discord, Web, etc)
     */
    public String getHeadUrl(UUID uuid, String name) {
        boolean isBedrock = false;
        if (uuid != null) {
            isBedrock = fr.gens.core.utils.FloodgateUtil.isBedrockPlayer(uuid);
        }

        if (uuid != null && isBedrock) {
            String cachedHash = skinHashCache.get(uuid);
            if (cachedHash != null) {
                if (!cachedHash.isEmpty()) {
                    return "https://mc-heads.net/avatar/" + cachedHash + ".png";
                }
            } else {
                // Chercher le hash en BDD seulement pour les joueurs Bedrock
                try (java.sql.Connection conn = plugin.getDatabaseManager().getConnection();
                     java.sql.PreparedStatement pstmt = conn.prepareStatement("SELECT hash FROM player_skins WHERE uuid = ?")) {
                    
                    pstmt.setString(1, uuid.toString());
                    try (java.sql.ResultSet rs = pstmt.executeQuery()) {
                        if (rs != null && rs.next()) {
                            String hash = rs.getString("hash");
                            if (hash != null && !hash.isEmpty()) {
                                skinHashCache.put(uuid, hash);
                                return "https://mc-heads.net/avatar/" + hash + ".png";
                            }
                        }
                    }
                    skinHashCache.put(uuid, ""); // Marquer comme absent pour éviter de répéter la requête
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        
        // Fallback standard pour les joueurs Java ou Bedrock sans hash en cache
        // On privilégie le nom plutôt que l'UUID car sur un serveur crack/offline,
        // l'UUID est un offline UUID qui retourne Steve sur crafthead.net.
        if (name != null && !name.isEmpty()) {
            // Corriger le pseudo si c'est un joueur Bedrock pour ne pas envoyer le prefixe floodgate
            String cleanName = name.startsWith(".") ? name.substring(1) : name;
            return "https://mc-heads.net/avatar/" + cleanName + ".png";
        } else if (uuid != null) {
            return "https://mc-heads.net/avatar/" + uuid.toString() + ".png";
        }
        return "https://mc-heads.net/avatar/Steve.png";
    }

    /**
     * Récupère la texture et signature en cache (RAM ou DB) pour utilisation sur les têtes de joueurs
     */
    public String[] getCachedSkinProperty(UUID uuid) {
        if (uuid == null) return null;
        String[] cached = skinPropertyCache.get(uuid);
        if (cached != null) return cached;

        try (java.sql.Connection conn = plugin.getDatabaseManager().getConnection();
             java.sql.PreparedStatement pstmt = conn.prepareStatement("SELECT texture_value, texture_signature FROM player_skins WHERE uuid = ?")) {
            pstmt.setString(1, uuid.toString());
            try (java.sql.ResultSet rs = pstmt.executeQuery()) {
                if (rs != null && rs.next()) {
                    String val = rs.getString("texture_value");
                    String sig = rs.getString("texture_signature");
                    if (val != null && !val.isEmpty()) {
                        String[] loaded = new String[]{val, sig != null ? sig : ""};
                        skinPropertyCache.put(uuid, loaded);
                        return loaded;
                    }
                }
            }
        } catch (Exception ignored) {}
        return null;
    }
}
