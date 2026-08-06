package fr.gens.core.utils;

import fr.gens.core.CorePlugin;
import fr.gens.core.modules.EconomyModule;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Statistic;
import org.bukkit.entity.Player;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public class PlaceholderUtils {

    public static Component parseToComponent(String text) {
        if (text == null) return Component.empty();
        
        String processed = text.replace("§", "&");
        
        if (processed.contains("<") && processed.contains(">")) {
            // Convert legacy ampersand codes to MiniMessage tags
            String mmString = processed
                .replace("&0", "<black>")
                .replace("&1", "<dark_blue>")
                .replace("&2", "<dark_green>")
                .replace("&3", "<dark_aqua>")
                .replace("&4", "<dark_red>")
                .replace("&5", "<dark_purple>")
                .replace("&6", "<gold>")
                .replace("&7", "<gray>")
                .replace("&8", "<dark_gray>")
                .replace("&9", "<blue>")
                .replace("&a", "<green>")
                .replace("&b", "<aqua>")
                .replace("&c", "<red>")
                .replace("&d", "<light_purple>")
                .replace("&e", "<yellow>")
                .replace("&f", "<white>")
                .replace("&k", "<obfuscated>")
                .replace("&l", "<bold>")
                .replace("&m", "<strikethrough>")
                .replace("&n", "<underlined>")
                .replace("&o", "<italic>")
                .replace("&r", "<reset>");
            return MiniMessage.miniMessage().deserialize(mmString);
        } else {
            return LegacyComponentSerializer.legacyAmpersand().deserialize(processed);
        }
    }

    public static String setPlaceholders(CorePlugin plugin, Player p, String text) {
        if (text == null || p == null) return text;

        String result = text;

        // Base Player
        result = result.replace("%player%", p.getName());
        result = result.replace("%player_name%", p.getName());

        // Quests
        int completedQuests = plugin.getDatabaseManager().getQuestsCompletedTotal(p.getUniqueId());
        result = result.replace("%quests%", String.valueOf(completedQuests));
        result = result.replace("%quests_completed%", String.valueOf(completedQuests));

        // Economy
        EconomyModule eco = (EconomyModule) plugin.getModuleManager().getModule("economy");
        if (eco != null && eco.isEnabled()) {
            String balance = String.format("%.0f", eco.getBalance(p.getUniqueId()));
            result = result.replace("%balance%", balance);
            result = result.replace("%money%", balance);
        } else {
            result = result.replace("%balance%", "REMOVE_LINE");
            result = result.replace("%money%", "REMOVE_LINE");
        }

        // Server
        result = result.replace("%online%", String.valueOf(Bukkit.getOnlinePlayers().size()));
        int staffCount = 0;
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.hasPermission("group.owner") || online.hasPermission("group.admin") || online.hasPermission("group.mod") || online.hasPermission("group.helper")) {
                staffCount++;
            }
        }
        result = result.replace("%staff%", String.valueOf(staffCount));
        
        long maxMemory = Runtime.getRuntime().maxMemory() / 1048576;
        long usedMemory = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1048576;
        result = result.replace("%mem_used%", String.valueOf(usedMemory));
        result = result.replace("%mem_max%", String.valueOf(maxMemory));
        result = result.replace("%ping%", String.valueOf(p.getPing()));

        // Discord
        boolean linked = p.hasPermission("genscore.discord.linked");
        String discordStatus = linked ? "&r&7Le Discord: &bdiscord.gg/gensbien" : "&e&l⚠ &cDiscord non lié ! &b/linktuto";
        result = result.replace("%discord_status%", discordStatus);
        
        String discordName = linked ? "Compte Lié" : "Non lié";
        result = result.replace("%discord_name%", discordName);

        // Statistics
        try {
            int ticks = p.getStatistic(Statistic.PLAY_ONE_MINUTE);
            int totalMinutes = ticks / 1200;
            int hours = totalMinutes / 60;
            int minutes = totalMinutes % 60;
            result = result.replace("%hours_played%", String.valueOf(ticks / 72000));
            result = result.replace("%playtime%", hours + "h" + String.format("%02d", minutes) + "m");
            result = result.replace("%player_kills%", String.valueOf(p.getStatistic(Statistic.PLAYER_KILLS)));
            result = result.replace("%mob_kills%", String.valueOf(p.getStatistic(Statistic.MOB_KILLS)));
            result = result.replace("%deaths%", String.valueOf(p.getStatistic(Statistic.DEATHS)));
        } catch (Exception ignored) {}

        // First join date
        try {
            long firstPlayed = p.getFirstPlayed();
            if (firstPlayed > 0) {
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
                sdf.setTimeZone(TimeZone.getTimeZone("Europe/Paris"));
                result = result.replace("%first_join%", sdf.format(new Date(firstPlayed)));
            } else {
                result = result.replace("%first_join%", "Inconnu");
            }
        } catch (Exception ignored) {}

        // LuckPerms
        try {
            LuckPerms api = LuckPermsProvider.get();
            User user = api.getUserManager().getUser(p.getUniqueId());
            if (user != null) {
                String groupName = user.getPrimaryGroup();
                if (groupName != null) {
                    String formattedGroup = groupName.substring(0, 1).toUpperCase() + groupName.substring(1);
                    result = result.replace("%group%", formattedGroup);
                } else {
                    result = result.replace("%group%", "Joueur");
                }

                String prefix = user.getCachedData().getMetaData().getPrefix();
                if (prefix != null && !prefix.trim().isEmpty()) {
                    result = result.replace("%prefix%", prefix);
                } else if (groupName != null) {
                    result = result.replace("%prefix%", "&e" + groupName.substring(0, 1).toUpperCase() + groupName.substring(1));
                } else {
                    result = result.replace("%prefix%", "&7Joueur");
                }
            } else {
                result = result.replace("%group%", "Joueur");
                result = result.replace("%prefix%", "&7Joueur");
            }
        } catch (Exception ignored) {
            result = result.replace("%group%", "Joueur");
            result = result.replace("%prefix%", "&7Joueur");
        }

        return ChatColor.translateAlternateColorCodes('&', result);
    }
}
