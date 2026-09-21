package fr.gens.core.utils;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Utilitaire de gestion et d'interoperabilite avec ViaVersion, ViaBackwards et ViaRewind.
 * Concu avec isolation reflective pour garantir un fonctionnement sans aucun crash
 * que ViaVersion soit installe ou non sur le serveur.
 */
public class ViaVersionUtil implements Listener {

    private static final Map<UUID, Integer> PROTOCOL_CACHE = new ConcurrentHashMap<>();
    private static Method getApiMethod = null;
    private static Method getPlayerVersionMethod = null;
    private static boolean reflectionInitialized = false;

    public static void init(fr.gens.core.CorePlugin corePlugin) {
        if (corePlugin == null) return;
        Bukkit.getPluginManager().registerEvents(new ViaVersionUtil(), corePlugin);
        if (isViaVersionInstalled()) {
            corePlugin.getLogger().info("[ViaVersionUtil] ViaVersion detecte. Pont d'interoperabilite multi-protocoles actif.");
        } else {
            corePlugin.getLogger().info("[ViaVersionUtil] ViaVersion non detecte. Mode standard natif 26.3 actif.");
        }
    }

    /**
     * Verifie si le plugin ViaVersion est charge et actif sur le serveur.
     */
    public static boolean isViaVersionInstalled() {
        try {
            if (Bukkit.getServer() == null || Bukkit.getPluginManager() == null) {
                return false;
            }
            return Bukkit.getPluginManager().isPluginEnabled("ViaVersion");
        } catch (Throwable t) {
            return false;
        }
    }

    private static synchronized void initReflection() {
        if (reflectionInitialized) return;
        reflectionInitialized = true;
        try {
            Class<?> viaClass = Class.forName("com.viaversion.viaversion.api.Via");
            getApiMethod = viaClass.getMethod("getAPI");
            Object apiInstance = getApiMethod.invoke(null);
            if (apiInstance != null) {
                getPlayerVersionMethod = apiInstance.getClass().getMethod("getPlayerVersion", UUID.class);
            }
        } catch (Throwable ignored) {
            getApiMethod = null;
            getPlayerVersionMethod = null;
        }
    }

    /**
     * Recupere le numero de protocole client d'un joueur.
     * Renvoie -1 si ViaVersion n'est pas installe ou si le joueur n'est pas trouve.
     */
    public static int getPlayerProtocolVersion(UUID uuid) {
        if (uuid == null) return -1;
        Integer cached = PROTOCOL_CACHE.get(uuid);
        if (cached != null) return cached;

        if (!isViaVersionInstalled()) {
            return -1;
        }

        initReflection();
        if (getApiMethod == null || getPlayerVersionMethod == null) {
            return -1;
        }

        try {
            Object apiInstance = getApiMethod.invoke(null);
            if (apiInstance != null) {
                Object result = getPlayerVersionMethod.invoke(apiInstance, uuid);
                if (result instanceof Number) {
                    int proto = ((Number) result).intValue();
                    PROTOCOL_CACHE.put(uuid, proto);
                    return proto;
                }
            }
        } catch (Throwable ignored) {
        }
        return -1;
    }

    /**
     * Renvoie un nom lisible de la version du client du joueur.
     * Exemples : "Bedrock (Geyser)", "26.3+ (Natif)", "26.2 (Via)", "1.21.x (Via)", etc.
     */
    public static String getPlayerVersionName(UUID uuid) {
        if (uuid == null) return "Inconnu";

        // 1. Detection Bedrock prioritaire
        if (FloodgateUtil.isBedrockPlayer(uuid)) {
            return "Bedrock (Geyser)";
        }

        int protocol = getPlayerProtocolVersion(uuid);
        if (protocol == -1) {
            return "26.3+ (Natif)";
        }

        return formatProtocolVersion(protocol);
    }

    /**
     * Traduit un numero de protocole Minecraft en version commerciale lisible.
     */
    public static String formatProtocolVersion(int protocol) {
        if (protocol >= 770) {
            return "26.3+";
        }
        switch (protocol) {
            case 769:
                return "26.2 / 1.21.4 (Via)";
            case 768:
                return "1.21.2-1.21.3 (Via)";
            case 767:
                return "1.21-1.21.1 (Via)";
            case 766:
                return "1.20.5-1.20.6 (Via)";
            case 765:
                return "1.20.3-1.20.4 (Via)";
            case 764:
                return "1.20.2 (Via)";
            case 763:
                return "1.20-1.20.1 (Via)";
            case 762:
                return "1.19.4 (Via)";
            case 761:
                return "1.19.3 (Via)";
            case 760:
                return "1.19.1-1.19.2 (Via)";
            case 759:
                return "1.19 (Via)";
            case 758:
                return "1.18.2 (Via)";
            case 757:
                return "1.18-1.18.1 (Via)";
            case 756:
                return "1.17.1 (Via)";
            case 755:
                return "1.17 (Via)";
            case 754:
                return "1.16.4-1.16.5 (Via)";
            default:
                if (protocol < 754 && protocol > 0) {
                    return "Legacy <1.16 (Via)";
                }
                return "Protocole " + protocol;
        }
    }

    /**
     * Indique si le client du joueur utilise une version anterieure a 26.3.
     */
    public static boolean isLegacyClient(UUID uuid) {
        if (uuid == null) return false;
        int proto = getPlayerProtocolVersion(uuid);
        return (proto > 0 && proto < 770);
    }

    /**
     * Adapte les materiaux recents exclusifs a Minecraft 26.3 pour eviter
     * les problemes de textures manquantes ou items glitch sur les anciens clients.
     */
    public static Material getSafeMenuMaterial(Material material, UUID uuid) {
        if (material == null) return Material.BARRIER;
        if (!isLegacyClient(uuid)) {
            return material;
        }

        String matName = material.name();
        
        // Repli pour les materiaux Pale Oak / Pale Garden de 26.3
        if (matName.contains("PALE_OAK")) {
            if (matName.endsWith("_LOG")) return Material.DARK_OAK_LOG;
            if (matName.endsWith("_PLANKS")) return Material.DARK_OAK_PLANKS;
            if (matName.endsWith("_STAIRS")) return Material.DARK_OAK_STAIRS;
            if (matName.endsWith("_SLAB")) return Material.DARK_OAK_SLAB;
            if (matName.endsWith("_SAPLING")) return Material.DARK_OAK_SAPLING;
            if (matName.endsWith("_DOOR")) return Material.DARK_OAK_DOOR;
            return Material.DARK_OAK_WOOD;
        }

        if (matName.contains("RESIN")) {
            return Material.ORANGE_TERRACOTTA;
        }

        if (matName.contains("CREAKING_HEART")) {
            return Material.DRIED_KELP_BLOCK;
        }

        return material;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerJoinEvent(PlayerJoinEvent event) {
        if (event != null && event.getPlayer() != null) {
            onPlayerJoin(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuitEvent(PlayerQuitEvent event) {
        if (event != null && event.getPlayer() != null) {
            onPlayerQuit(event.getPlayer().getUniqueId());
        }
    }

    /**
     * Enregistre un joueur lors de sa connexion.
     */
    public static void onPlayerJoin(Player player) {
        if (player != null) {
            getPlayerProtocolVersion(player.getUniqueId());
        }
    }

    /**
     * Nettoie le cache d'un joueur lors de sa deconnexion.
     */
    public static void onPlayerQuit(UUID uuid) {
        if (uuid != null) {
            PROTOCOL_CACHE.remove(uuid);
        }
    }
}
