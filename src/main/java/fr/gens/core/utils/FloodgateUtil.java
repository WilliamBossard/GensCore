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
        if (isFloodgateInstalled()) {
            return isBedrockPlayerInternal(uuid);
        }
        return false;
    }

    // We keep this in a separate private method to avoid NoClassDefFoundError 
    // when the class is loaded and floodgate is missing.
    private static boolean isBedrockPlayerInternal(UUID uuid) {
        try {
            return org.geysermc.floodgate.api.FloodgateApi.getInstance().isFloodgatePlayer(uuid);
        } catch (Throwable t) {
            return false;
        }
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
