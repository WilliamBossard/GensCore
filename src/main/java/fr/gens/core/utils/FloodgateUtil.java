package fr.gens.core.utils;

import org.bukkit.Bukkit;
import java.util.UUID;

public class FloodgateUtil {
    
    private static boolean isFloodgateEnabled = false;
    private static boolean initialized = false;

    private static void init() {
        if (!initialized) {
            isFloodgateEnabled = Bukkit.getPluginManager().isPluginEnabled("floodgate");
            initialized = true;
        }
    }

    public static boolean isFloodgateInstalled() {
        init();
        return isFloodgateEnabled;
    }

    public static boolean isBedrockPlayer(UUID uuid) {
        init();
        if (isFloodgateEnabled) {
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
