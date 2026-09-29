package fr.gens.core.utils;

import org.bukkit.Bukkit;
import java.util.UUID;

public class FloodgateUtil {
    
    public static boolean isFloodgateInstalled() {
        try {
            if (Bukkit.getServer() == null || Bukkit.getPluginManager() == null) {
                return false;
            }
            return Bukkit.getPluginManager().isPluginEnabled("floodgate");
        } catch (Throwable t) {
            return false;
        }
    }

    public static boolean isBedrockPlayer(UUID uuid) {
        if (uuid == null) return false;
        // All Floodgate Bedrock players have UUIDs with most significant bits == 0L
        if (uuid.getMostSignificantBits() == 0L) {
            return true;
        }
        if (isFloodgateInstalled()) {
            return isBedrockPlayerInternal(uuid);
        }
        return false;
    }

    // We keep this in a separate private method to avoid NoClassDefFoundError 
    // when the class is loaded and floodgate is missing.
    private static boolean isBedrockPlayerInternal(UUID uuid) {
        try {
            org.geysermc.floodgate.api.FloodgateApi api = org.geysermc.floodgate.api.FloodgateApi.getInstance();
            if (api == null) return false;
            return api.isFloodgatePlayer(uuid) || api.isFloodgateId(uuid);
        } catch (Throwable t) {
            return false;
        }
    }

    public static boolean isBedrockPlayer(String name) {
        if (name == null || name.isEmpty()) return false;
        return name.startsWith(".") || name.startsWith("*");
    }

    public static boolean isBedrockPlayer(UUID uuid, String name) {
        if (isBedrockPlayer(uuid)) return true;
        return isBedrockPlayer(name);
    }

    public static String getCleanGamertag(String name) {
        if (name == null) return "";
        if (name.startsWith(".") || name.startsWith("*")) {
            return name.substring(1);
        }
        return name;
    }

    public static String getBedrockPrefix() {
        return "<dark_gray>[<aqua>Bedrock<dark_gray>] <reset>";
    }
    
    public static String getBedrockDiscordPrefix() {
        return "[Bedrock] ";
    }
    
    public static String getJavaPrefix() {
        return "<dark_gray>[<gold>Java<dark_gray>] <reset>";
    }
    
    public static String getJavaDiscordPrefix() {
        return "[Java] ";
    }
}
