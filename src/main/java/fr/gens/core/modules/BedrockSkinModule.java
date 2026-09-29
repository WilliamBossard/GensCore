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
        dbManager.addColumnIfNotExists("player_skins", "username", "VARCHAR(32)");
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
                            org.geysermc.floodgate.api.player.FloodgatePlayer fgPlayer = null;
                            try {
                                fgPlayer = org.geysermc.floodgate.api.FloodgateApi.getInstance().getPlayer(uuid);
                            } catch (Throwable ignored) {}
                            String xuid = (fgPlayer != null) ? fgPlayer.getXuid() : (uuid.getMostSignificantBits() == 0L ? Long.toUnsignedString(uuid.getLeastSignificantBits()) : null);
                            if (xuid == null) return;

                            SkinFetchResult res = fetchSkinFromGeyser(xuid);
                            if (res != null) {
                                success = true;
                                String value = res.value;
                                String signature = res.signature;
                                String hash = res.hash;

                                plugin.getLogger().info("[BedrockSkinModule] Hash trouve pour " + player.getName() + " : " + hash);
                                // Save hash and textures to database for Web/Discord/Head use
                                skinHashCache.put(uuid, hash);
                                skinPropertyCache.put(uuid, new String[]{value, signature});
                                try (java.sql.Connection dbConn = plugin.getDatabaseManager().getConnection();
                                     java.sql.PreparedStatement pstmt = dbConn.prepareStatement("REPLACE INTO player_skins (uuid, hash, texture_value, texture_signature, username) VALUES (?, ?, ?, ?, ?)")) {
                                    pstmt.setString(1, uuid.toString());
                                    pstmt.setString(2, hash);
                                    pstmt.setString(3, value);
                                    pstmt.setString(4, signature);
                                    pstmt.setString(5, player.getName());
                                    pstmt.executeUpdate();
                                } catch (Exception e) {
                                    plugin.getLogger().warning("Erreur lors de la sauvegarde du skin en base de donnees: " + e.getMessage());
                                }

                                fr.gens.core.utils.HeadUtil.SkinData skinData = new fr.gens.core.utils.HeadUtil.SkinData(value, signature, hash, player.getName());
                                fr.gens.core.utils.HeadUtil.saveToCache(uuid, player.getName(), skinData, false);
                                String clean = FloodgateUtil.getCleanGamertag(player.getName());
                                fr.gens.core.utils.HeadUtil.saveToCache(uuid, clean, skinData, false);

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
                                if (currentTry < maxTries) {
                                    try { Thread.sleep(2000L * currentTry); } catch(InterruptedException ignored){}
                                }
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

    public static class SkinFetchResult {
        public final String value;
        public final String signature;
        public final String hash;

        public SkinFetchResult(String value, String signature, String hash) {
            this.value = value;
            this.signature = signature;
            this.hash = hash;
        }
    }

    public SkinFetchResult fetchSkinFromGeyser(String xuid) {
        if (xuid == null || xuid.isEmpty()) return null;
        try {
            URL url = java.net.URI.create("https://api.geysermc.org/v2/skin/" + xuid).toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
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
                        } catch (Exception ignored) {}

                        if (json.has("hash") && hash.isEmpty()) {
                            hash = json.get("hash").getAsString();
                        } else if (json.has("texture_id") && hash.isEmpty()) {
                            hash = json.get("texture_id").getAsString();
                        }

                        if (!hash.isEmpty()) {
                            return new SkinFetchResult(value, signature, hash);
                        }
                    }
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    public String fetchXuidFromGeyser(String gamertag) {
        if (gamertag == null || gamertag.isEmpty()) return null;
        try {
            URL url = java.net.URI.create("https://api.geysermc.org/v2/xbox/xuid/" + java.net.URLEncoder.encode(gamertag, java.nio.charset.StandardCharsets.UTF_8)).toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(3000);
            if (conn.getResponseCode() == 200) {
                try (InputStreamReader reader = new InputStreamReader(conn.getInputStream())) {
                    JsonObject json = new JsonParser().parse(reader).getAsJsonObject();
                    if (json.has("xuid")) {
                        return json.get("xuid").getAsString();
                    }
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    public String fetchAndCacheBedrockHash(UUID uuid, String name) {
        String xuid = null;
        if (uuid != null && uuid.getMostSignificantBits() == 0L) {
            xuid = Long.toUnsignedString(uuid.getLeastSignificantBits());
        }
        if (xuid == null && uuid != null && FloodgateUtil.isFloodgateInstalled()) {
            try {
                org.geysermc.floodgate.api.player.FloodgatePlayer fgPlayer = org.geysermc.floodgate.api.FloodgateApi.getInstance().getPlayer(uuid);
                if (fgPlayer != null) {
                    xuid = fgPlayer.getXuid();
                }
            } catch (Throwable ignored) {}
        }
        if (xuid == null && name != null) {
            String cleanName = FloodgateUtil.getCleanGamertag(name);
            xuid = fetchXuidFromGeyser(cleanName);
        }
        if (xuid == null || xuid.isEmpty()) {
            return null;
        }

        SkinFetchResult res = fetchSkinFromGeyser(xuid);
        if (res != null && res.hash != null && !res.hash.isEmpty()) {
            if (uuid != null) {
                skinHashCache.put(uuid, res.hash);
                skinPropertyCache.put(uuid, new String[]{res.value, res.signature});
            }
            UUID targetUuid = uuid;
            if (targetUuid == null) {
                try {
                    long xuidLong = Long.parseUnsignedLong(xuid);
                    targetUuid = new UUID(0L, xuidLong);
                } catch (Exception ignored) {}
            }
            if (targetUuid != null) {
                final UUID finalUuid = targetUuid;
                try (java.sql.Connection dbConn = plugin.getDatabaseManager().getConnection();
                     java.sql.PreparedStatement pstmt = dbConn.prepareStatement(
                             "REPLACE INTO player_skins (uuid, hash, texture_value, texture_signature, username) VALUES (?, ?, ?, ?, ?)")) {
                    pstmt.setString(1, finalUuid.toString());
                    pstmt.setString(2, res.hash);
                    pstmt.setString(3, res.value);
                    pstmt.setString(4, res.signature);
                    pstmt.setString(5, name != null ? name : "");
                    pstmt.executeUpdate();
                } catch (Exception ignored) {}

                fr.gens.core.utils.HeadUtil.SkinData skinData = new fr.gens.core.utils.HeadUtil.SkinData(res.value, res.signature, res.hash, name);
                fr.gens.core.utils.HeadUtil.saveToCache(finalUuid, name, skinData, false);
                if (name != null) {
                    String clean = FloodgateUtil.getCleanGamertag(name);
                    fr.gens.core.utils.HeadUtil.saveToCache(finalUuid, clean, skinData, false);
                }
            }
            return res.hash;
        }
        return null;
    }

    /**
     * Helper pour obtenir l'URL de l'avatar du joueur (pour Discord, Web, etc)
     */
    public String getHeadUrl(UUID uuid, String name) {
        boolean isBedrock = FloodgateUtil.isBedrockPlayer(uuid, name);
        String cleanName = FloodgateUtil.getCleanGamertag(name);

        if (uuid == null && name != null) {
            try {
                if (plugin != null) {
                    fr.gens.core.database.WebDAO webDAO = new fr.gens.core.database.WebDAO(plugin);
                    uuid = webDAO.getPlayerUuidByUsername(name);
                    if (uuid != null && FloodgateUtil.isBedrockPlayer(uuid)) {
                        isBedrock = true;
                    }
                }
            } catch (Throwable ignored) {}
        }

        // 1. Verifier le cache RAM memoire (BedrockSkinModule + HeadUtil)
        if (uuid != null) {
            String cached = skinHashCache.get(uuid);
            if (cached != null && !cached.isEmpty()) {
                return "https://mc-heads.net/avatar/" + cached + ".png";
            }
        }
        if (name != null && !name.isEmpty()) {
            String hash = fr.gens.core.utils.HeadUtil.getSkinHashByUsername(name);
            if (hash == null || hash.isEmpty()) {
                hash = fr.gens.core.utils.HeadUtil.getSkinHashByUsername(cleanName);
            }
            if (hash != null && !hash.isEmpty()) {
                if (uuid != null) skinHashCache.put(uuid, hash);
                return "https://mc-heads.net/avatar/" + hash + ".png";
            }
        }

        // 2. Recherche en Base de Données player_skins (pour Bedrock comme pour Java)
        if (plugin != null && plugin.getDatabaseManager() != null) {
            try (java.sql.Connection conn = plugin.getDatabaseManager().getConnection();
                 java.sql.PreparedStatement pstmt = conn.prepareStatement(
                         "SELECT uuid, hash, texture_value, texture_signature, username FROM player_skins WHERE uuid = ? OR username = ? COLLATE NOCASE OR username = ? COLLATE NOCASE LIMIT 1")) {
                pstmt.setString(1, uuid != null ? uuid.toString() : "");
                pstmt.setString(2, name != null ? name : "");
                pstmt.setString(3, cleanName != null ? cleanName : "");
                try (java.sql.ResultSet rs = pstmt.executeQuery()) {
                    if (rs != null && rs.next()) {
                        String hash = rs.getString("hash");
                        String val = rs.getString("texture_value");
                        String sig = rs.getString("texture_signature");
                        String uuidStr = rs.getString("uuid");
                        if ((hash == null || hash.isEmpty()) && val != null && !val.isEmpty()) {
                            hash = fr.gens.core.utils.HeadUtil.extractHash(val);
                        }
                        if (hash != null && !hash.isEmpty()) {
                            if (uuid != null) skinHashCache.put(uuid, hash);
                            if (uuidStr != null && val != null) {
                                try {
                                    UUID loadedUuid = UUID.fromString(uuidStr);
                                    skinPropertyCache.put(loadedUuid, new String[]{val, sig != null ? sig : ""});
                                } catch (Exception ignored) {}
                            }
                            return "https://mc-heads.net/avatar/" + hash + ".png";
                        }
                    }
                }
            } catch (Exception ignored) {}
        }

        // 3. Si c'est un compte Bedrock sans hash en DB, tenter la resolution directe via Geyser API
        if (isBedrock) {
            if (!Bukkit.isPrimaryThread()) {
                String fetchedHash = fetchAndCacheBedrockHash(uuid, name);
                if (fetchedHash != null && !fetchedHash.isEmpty()) {
                    return "https://mc-heads.net/avatar/" + fetchedHash + ".png";
                }
            } else if (plugin != null && plugin.getFoliaLib() != null) {
                final UUID finalUuid = uuid;
                final String finalName = name;
                plugin.getFoliaLib().getScheduler().runAsync(task -> fetchAndCacheBedrockHash(finalUuid, finalName));
            }
        }

        // 4. Fallback standard pour les joueurs Java ou Bedrock sans hash en cache
        if (cleanName != null && !cleanName.isEmpty()) {
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
