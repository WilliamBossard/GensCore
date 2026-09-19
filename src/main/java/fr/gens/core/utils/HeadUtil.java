package fr.gens.core.utils;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import fr.gens.core.CorePlugin;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Utilitaire centralise pour la resolution, la mise en cache et l'application des tetes et skins joueurs.
 * Gere les comptes Java payants, Bedrock (Floodgate/Geyser), SkinsRestorer et les serveurs en online-mode=false.
 */
public final class HeadUtil implements Listener {

    private static CorePlugin plugin;
    private static final Map<UUID, SkinData> UUID_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, SkinData> NAME_CACHE = new ConcurrentHashMap<>();
    private static final Set<String> PENDING_FETCHES = ConcurrentHashMap.newKeySet();

    private HeadUtil() {}

    public static class SkinData {
        public final String value;
        public final String signature;
        public final String hash;
        public final String username;

        public SkinData(String value, String signature, String hash, String username) {
            this.value = value;
            this.signature = signature != null ? signature : "";
            this.hash = hash != null ? hash : "";
            this.username = username != null ? username : "";
        }
    }

    public static void init(CorePlugin corePlugin) {
        plugin = corePlugin;
        try {
            plugin.getDatabaseManager().executeStatement(
                    "CREATE TABLE IF NOT EXISTS player_skins (" +
                            "uuid VARCHAR(36) PRIMARY KEY, " +
                            "hash VARCHAR(64), " +
                            "texture_value TEXT, " +
                            "texture_signature TEXT);"
            );
            plugin.getDatabaseManager().addColumnIfNotExists("player_skins", "username", "VARCHAR(32)");

            // Prechargement du cache en memoire vive
            try (Connection conn = plugin.getDatabaseManager().getConnection();
                 PreparedStatement pstmt = conn.prepareStatement("SELECT uuid, hash, texture_value, texture_signature, username FROM player_skins")) {
                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        String uuidStr = rs.getString("uuid");
                        String hash = rs.getString("hash");
                        String val = rs.getString("texture_value");
                        String sig = rs.getString("texture_signature");
                        String user = rs.getString("username");
                        if (val != null && !val.isEmpty()) {
                            SkinData data = new SkinData(val, sig, hash, user);
                            if (uuidStr != null) {
                                try {
                                    UUID u = UUID.fromString(uuidStr);
                                    UUID_CACHE.put(u, data);
                                } catch (Exception ignored) {}
                            }
                            if (user != null && !user.isEmpty()) {
                                NAME_CACHE.put(user.toLowerCase(), data);
                            }
                        }
                    }
                }
            }
            plugin.getLogger().info("[HeadUtil] Cache initialise avec " + UUID_CACHE.size() + " skins de joueurs.");
        } catch (Exception e) {
            plugin.getLogger().warning("[HeadUtil] Erreur lors de l'initialisation du cache skins: " + e.getMessage());
        }

        Bukkit.getPluginManager().registerEvents(new HeadUtil(), plugin);
    }

    public static void shutdown() {
        UUID_CACHE.clear();
        NAME_CACHE.clear();
        PENDING_FETCHES.clear();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // 1. Capture immediate du profil s'il contient deja les textures
        capturePlayerSkin(player);

        // 2. Capture differee (20 ticks / 1 seconde) pour laisser SkinsRestorer / Floodgate appliquer les textures
        if (plugin != null && plugin.getFoliaLib() != null) {
            plugin.getFoliaLib().getScheduler().runLater(task -> {
                if (player.isOnline()) {
                    capturePlayerSkin(player);
                }
            }, 20L);
        }
    }

    private static void capturePlayerSkin(Player player) {
        if (player == null || !player.isOnline()) return;
        UUID uuid = player.getUniqueId();
        String name = player.getName();

        try {
            PlayerProfile profile = player.getPlayerProfile();
            for (ProfileProperty prop : profile.getProperties()) {
                if ("textures".equalsIgnoreCase(prop.getName())) {
                    String val = prop.getValue();
                    String sig = prop.getSignature();
                    if (val != null && !val.isEmpty()) {
                        String hash = extractHash(val);
                        SkinData data = new SkinData(val, sig, hash, name);
                        saveToCache(uuid, name, data, true);
                        return;
                    }
                }
            }

            // Si le profil n'a pas encore de textures, tenter SkinsRestorer
            SkinData srData = querySkinsRestorer(uuid, name);
            if (srData != null) {
                saveToCache(uuid, name, srData, true);
            }
        } catch (Throwable ignored) {}
    }

    /**
     * Applique le profil de tete et les textures d'un joueur en ligne sur un ItemStack PLAYER_HEAD.
     */
    public static void applyHeadProfile(ItemStack head, Player player) {
        if (player == null) return;
        applyHeadProfile(head, player.getUniqueId(), player.getName());
    }

    /**
     * Applique le profil de tete et les textures d'un joueur hors-ligne sur un ItemStack PLAYER_HEAD.
     */
    public static void applyHeadProfile(ItemStack head, OfflinePlayer op) {
        if (op == null) return;
        applyHeadProfile(head, op.getUniqueId(), op.getName());
    }

    /**
     * Applique le profil de tete et les textures pour un UUID et pseudo donnes.
     */
    public static void applyHeadProfile(ItemStack head, UUID uuid, String name) {
        if (head == null || head.getType() != Material.PLAYER_HEAD) return;
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        if (meta == null) return;

        // 1. Joueur actuellement connecte
        if (uuid != null) {
            Player online = Bukkit.getPlayer(uuid);
            if (online != null && online.isOnline()) {
                PlayerProfile onlineProfile = online.getPlayerProfile();
                boolean hasTextures = onlineProfile.getProperties().stream()
                        .anyMatch(p -> "textures".equalsIgnoreCase(p.getName()));
                if (hasTextures) {
                    meta.setPlayerProfile(onlineProfile);
                    meta.setOwningPlayer(online);
                    head.setItemMeta(meta);

                    for (ProfileProperty prop : onlineProfile.getProperties()) {
                        if ("textures".equalsIgnoreCase(prop.getName())) {
                            SkinData data = new SkinData(prop.getValue(), prop.getSignature(), extractHash(prop.getValue()), online.getName());
                            saveToCache(uuid, online.getName(), data, false);
                            break;
                        }
                    }
                    return;
                }
            }
        }

        // 2. Recherche dans le cache memoire
        SkinData cached = null;
        if (uuid != null) cached = UUID_CACHE.get(uuid);
        if (cached == null && name != null) cached = NAME_CACHE.get(name.toLowerCase());

        // 3. Recherche via SkinsRestorer (si present sur le serveur)
        if (cached == null && (uuid != null || name != null)) {
            cached = querySkinsRestorer(uuid, name);
        }

        // 4. Si texture trouvee, creation d'un PlayerProfile Paper avec la propriete signee
        if (cached != null && cached.value != null && !cached.value.isEmpty()) {
            UUID profUuid = uuid != null ? uuid : UUID.randomUUID();
            String profName = name != null ? name : (cached.username != null ? cached.username : "Steve");
            PlayerProfile profile = Bukkit.createProfile(profUuid, profName);
            profile.setProperty(new ProfileProperty("textures", cached.value, cached.signature));
            meta.setPlayerProfile(profile);
            if (uuid != null) meta.setOwningPlayer(Bukkit.getOfflinePlayer(uuid));
            head.setItemMeta(meta);
            return;
        }

        // 5. Fallback par defaut et recuperation asynchrone pour les ouvertures futures
        if (uuid != null) {
            meta.setOwningPlayer(Bukkit.getOfflinePlayer(uuid));
        }
        if (name != null && !name.isEmpty()) {
            PlayerProfile stub = Bukkit.createProfile(name);
            meta.setPlayerProfile(stub);
            fetchMojangSkinAsync(uuid, name);
        }
        head.setItemMeta(meta);
    }

    /**
     * Interroge SkinsRestorer par reflexion sans dependance directe au bytecode.
     */
    public static SkinData querySkinsRestorer(UUID uuid, String name) {
        try {
            Class<?> providerClass = Class.forName("net.skinsrestorer.api.SkinsRestorerProvider");
            Object sr = providerClass.getMethod("get").invoke(null);
            if (sr == null) return null;
            Object playerStorage = sr.getClass().getMethod("getPlayerStorage").invoke(sr);
            if (playerStorage == null) return null;

            Method targetMethod = null;
            for (Method m : playerStorage.getClass().getMethods()) {
                if (m.getName().equals("getSkinForPlayer") && m.getParameterCount() == 2) {
                    targetMethod = m;
                    break;
                }
            }
            if (targetMethod != null) {
                Object res = targetMethod.invoke(playerStorage, uuid, name);
                if (res instanceof Optional<?> opt && opt.isPresent()) {
                    Object skinProp = opt.get();
                    String val = (String) skinProp.getClass().getMethod("getValue").invoke(skinProp);
                    String sig = (String) skinProp.getClass().getMethod("getSignature").invoke(skinProp);
                    if (val != null && !val.isEmpty()) {
                        String hash = extractHash(val);
                        SkinData data = new SkinData(val, sig, hash, name);
                        saveToCache(uuid, name, data, true);
                        return data;
                    }
                }
            }
        } catch (Throwable ignored) {}
        return null;
    }

    /**
     * Recupere les textures Mojang de maniere asynchrone pour les comptes officiels Java payants.
     */
    public static void fetchMojangSkinAsync(UUID uuid, String name) {
        if (name == null || name.isEmpty() || name.startsWith(".") || plugin == null) return;
        String key = name.toLowerCase();
        if (!PENDING_FETCHES.add(key)) return;

        plugin.getFoliaLib().getScheduler().runAsync(task -> {
            try {
                PlayerProfile profile = Bukkit.createProfile(name);
                profile.complete(true);
                for (ProfileProperty prop : profile.getProperties()) {
                    if ("textures".equalsIgnoreCase(prop.getName())) {
                        String val = prop.getValue();
                        String sig = prop.getSignature();
                        if (val != null && !val.isEmpty()) {
                            String hash = extractHash(val);
                            SkinData data = new SkinData(val, sig, hash, name);
                            saveToCache(uuid != null ? uuid : profile.getId(), name, data, true);
                            break;
                        }
                    }
                }
            } catch (Throwable ignored) {
            } finally {
                PENDING_FETCHES.remove(key);
            }
        });
    }

    public static void saveToCache(UUID uuid, String username, SkinData data, boolean saveToDb) {
        if (data == null) return;
        if (uuid != null) UUID_CACHE.put(uuid, data);
        if (username != null && !username.isEmpty()) NAME_CACHE.put(username.toLowerCase(), data);

        if (saveToDb && plugin != null && uuid != null) {
            plugin.getFoliaLib().getScheduler().runAsync(task -> {
                try (Connection conn = plugin.getDatabaseManager().getConnection();
                     PreparedStatement pstmt = conn.prepareStatement(
                             "REPLACE INTO player_skins (uuid, hash, texture_value, texture_signature, username) VALUES (?, ?, ?, ?, ?)")) {
                    pstmt.setString(1, uuid.toString());
                    pstmt.setString(2, data.hash != null ? data.hash : "");
                    pstmt.setString(3, data.value);
                    pstmt.setString(4, data.signature != null ? data.signature : "");
                    pstmt.setString(5, username != null ? username : "");
                    pstmt.executeUpdate();
                } catch (Exception ignored) {}
            });
        }
    }

    public static String extractHash(String base64Value) {
        if (base64Value == null || base64Value.isEmpty()) return null;
        try {
            String decoded = new String(Base64.getDecoder().decode(base64Value), StandardCharsets.UTF_8);
            JsonObject json = new JsonParser().parse(decoded).getAsJsonObject();
            if (json.has("textures")) {
                JsonObject textures = json.getAsJsonObject("textures");
                if (textures.has("SKIN")) {
                    JsonObject skin = textures.getAsJsonObject("SKIN");
                    if (skin.has("url")) {
                        String url = skin.get("url").getAsString();
                        return url.substring(url.lastIndexOf('/') + 1);
                    }
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    public static String getSkinHashByUsername(String username) {
        if (username == null || username.isEmpty()) return null;
        SkinData data = NAME_CACHE.get(username.toLowerCase());
        if (data != null && data.hash != null && !data.hash.isEmpty()) {
            return data.hash;
        }
        return null;
    }

    public static String getUsername(UUID uuid) {
        if (uuid == null) return null;
        SkinData data = UUID_CACHE.get(uuid);
        if (data != null && data.username != null && !data.username.isEmpty()) {
            return data.username;
        }
        return null;
    }
}
