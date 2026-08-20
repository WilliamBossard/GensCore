package fr.gens.core.utils;

import fr.gens.core.CorePlugin;
import fr.gens.core.modules.EconomyModule;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import org.bukkit.Bukkit;
import org.bukkit.Statistic;
import org.bukkit.entity.Player;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;


public class PlaceholderUtils {

    public static Component parseToComponent(String text) {
        if (text == null) return Component.empty();
        
        String processed = text.replace("ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â§", "&");
        
        if (processed.contains("<") && processed.contains(">")) {
            // Convert legacy ampersand codes to MiniMessage tags
            String mmString = processed
                .replace("<black>", "<black>")
                .replace("<dark_blue>", "<dark_blue>")
                .replace("<dark_green>", "<dark_green>")
                .replace("<dark_aqua>", "<dark_aqua>")
                .replace("<dark_red>", "<dark_red>")
                .replace("<dark_purple>", "<dark_purple>")
                .replace("<gold>", "<gold>")
                .replace("<gray>", "<gray>")
                .replace("<dark_gray>", "<dark_gray>")
                .replace("<blue>", "<blue>")
                .replace("<green>", "<green>")
                .replace("<aqua>", "<aqua>")
                .replace("<red>", "<red>")
                .replace("<light_purple>", "<light_purple>")
                .replace("<yellow>", "<yellow>")
                .replace("<white>", "<white>")
                .replace("<obfuscated>", "<obfuscated>")
                .replace("<bold>", "<bold>")
                .replace("<strikethrough>", "<strikethrough>")
                .replace("<underlined>", "<underlined>")
                .replace("<italic>", "<italic>")
                .replace("<reset>", "<reset>");
            return MiniMessage.miniMessage().deserialize(mmString);
        } else {
            return LegacyComponentSerializer.legacyAmpersand().deserialize(processed);
        }
    }

    /**
     * Parse une string (contenant potentiellement des codes legacy ou MiniMessage) et applique les placeholders.
     */
    public static Component setPlaceholdersComponent(CorePlugin plugin, Player p, String text) {
        if (text == null || p == null) return Component.empty();

        List<TagResolver> resolvers = new ArrayList<>();

        // Base Player
        resolvers.add(Placeholder.parsed("player", p.getName()));
        resolvers.add(Placeholder.parsed("player_name", p.getName()));

        // Quests
        fr.gens.core.modules.quests.QuestModule questModule = (fr.gens.core.modules.quests.QuestModule) plugin.getModuleManager().getModule("quests");
        int completedQuests = questModule != null ? questModule.getQuestDAO().getQuestsCompletedTotal(p.getUniqueId()) : 0;
        resolvers.add(Placeholder.parsed("quests", String.valueOf(completedQuests)));
        resolvers.add(Placeholder.parsed("quests_completed", String.valueOf(completedQuests)));

        // Economy
        EconomyModule eco = (EconomyModule) plugin.getModuleManager().getModule("economy");
        if (eco != null && eco.isEnabled()) {
            String balance = String.format("%.0f", eco.getBalance(p.getUniqueId()));
            resolvers.add(Placeholder.parsed("balance", balance));
            resolvers.add(Placeholder.parsed("money", balance));
        } else {
            resolvers.add(Placeholder.parsed("balance", "0"));
            resolvers.add(Placeholder.parsed("money", "0"));
        }

        // Server
        resolvers.add(Placeholder.parsed("online", String.valueOf(Bukkit.getOnlinePlayers().size())));
        int staffCount = 0;
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online == null) continue;
            if (online.hasPermission("group.owner") || online.hasPermission("group.admin") || online.hasPermission("group.mod") || online.hasPermission("group.helper")) {
                staffCount++;
            }
        }
        resolvers.add(Placeholder.parsed("staff", String.valueOf(staffCount)));
        
        long maxMemory = Runtime.getRuntime().maxMemory() / 1048576;
        long usedMemory = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1048576;
        resolvers.add(Placeholder.parsed("mem_used", String.valueOf(usedMemory)));
        resolvers.add(Placeholder.parsed("mem_max", String.valueOf(maxMemory)));
        resolvers.add(Placeholder.parsed("ping", String.valueOf(p.getPing())));

        // Discord
        boolean linked = p.hasPermission("genscore.discord.linked");
        String discordStatus = linked ? "<gray>Le Discord: <aqua>discord.gg/gensbien" : "<yellow><bold> <red>Discord non liÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â© ! <aqua>/linktuto";
        resolvers.add(Placeholder.parsed("discord_status", discordStatus)); // We parse it as a placeholder string, wait, no, if we parse it, it won't resolve colors.
        
        // Wait, if it contains colors, we should use MiniMessage Component
        resolvers.add(Placeholder.component("discord_status", parseToComponent(discordStatus)));
        
        String discordName = linked ? "Compte LiÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â©" : "Non liÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â©";
        resolvers.add(Placeholder.parsed("discord_name", discordName));

        // Statistics
        try {
            int ticks = p.getStatistic(Statistic.PLAY_ONE_MINUTE);
            int totalMinutes = ticks / 1200;
            int hours = totalMinutes / 60;
            int minutes = totalMinutes % 60;
            resolvers.add(Placeholder.parsed("hours_played", String.valueOf(ticks / 72000)));
            resolvers.add(Placeholder.parsed("playtime", hours + "h" + String.format("%02d", minutes) + "m"));
            resolvers.add(Placeholder.parsed("player_kills", String.valueOf(p.getStatistic(Statistic.PLAYER_KILLS))));
            resolvers.add(Placeholder.parsed("mob_kills", String.valueOf(p.getStatistic(Statistic.MOB_KILLS))));
            resolvers.add(Placeholder.parsed("deaths", String.valueOf(p.getStatistic(Statistic.DEATHS))));
        } catch (Exception ignored) {}

        // First join date
        try {
            long firstPlayed = p.getFirstPlayed();
            if (firstPlayed > 0) {
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
                sdf.setTimeZone(TimeZone.getTimeZone("Europe/Paris"));
                resolvers.add(Placeholder.parsed("first_join", sdf.format(new Date(firstPlayed))));
            } else {
                resolvers.add(Placeholder.parsed("first_join", "Inconnu"));
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
                    resolvers.add(Placeholder.parsed("group", formattedGroup));
                } else {
                    resolvers.add(Placeholder.parsed("group", "Joueur"));
                }

                String prefix = user.getCachedData().getMetaData().getPrefix();
                if (prefix != null && !prefix.trim().isEmpty()) {
                    resolvers.add(Placeholder.component("prefix", parseToComponent(prefix)));
                } else if (groupName != null) {
                    resolvers.add(Placeholder.component("prefix", parseToComponent("<yellow>" + groupName.substring(0, 1).toUpperCase() + groupName.substring(1))));
                } else {
                    resolvers.add(Placeholder.component("prefix", parseToComponent("<gray>Joueur")));
                }
            } else {
                resolvers.add(Placeholder.parsed("group", "Joueur"));
                resolvers.add(Placeholder.component("prefix", parseToComponent("<gray>Joueur")));
            }
        } catch (Exception ignored) {
            resolvers.add(Placeholder.parsed("group", "Joueur"));
            resolvers.add(Placeholder.component("prefix", parseToComponent("<gray>Joueur")));
        }

        // Before passing to MiniMessage, let's pre-convert Legacy variables (%) to MiniMessage Tags (<>)
        String mmText = text.replace("ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â§", "&")
                            .replace("%player%", "<player>")
                            .replace("%player_name%", "<player_name>")
                            .replace("%quests%", "<quests>")
                            .replace("%quests_completed%", "<quests_completed>")
                            .replace("%balance%", "<balance>")
                            .replace("%money%", "<money>")
                            .replace("%online%", "<online>")
                            .replace("%staff%", "<staff>")
                            .replace("%mem_used%", "<mem_used>")
                            .replace("%mem_max%", "<mem_max>")
                            .replace("%ping%", "<ping>")
                            .replace("%discord_status%", "<discord_status>")
                            .replace("%discord_name%", "<discord_name>")
                            .replace("%hours_played%", "<hours_played>")
                            .replace("%playtime%", "<playtime>")
                            .replace("%player_kills%", "<player_kills>")
                            .replace("%mob_kills%", "<mob_kills>")
                            .replace("%deaths%", "<deaths>")
                            .replace("%first_join%", "<first_join>")
                            .replace("%group%", "<group>")
                            .replace("%prefix%", "<prefix>");

        // Also convert legacy colors in the base string to MiniMessage tags so everything is handled cleanly
        mmText = mmText
                .replace("<black>", "<black>")
                .replace("<dark_blue>", "<dark_blue>")
                .replace("<dark_green>", "<dark_green>")
                .replace("<dark_aqua>", "<dark_aqua>")
                .replace("<dark_red>", "<dark_red>")
                .replace("<dark_purple>", "<dark_purple>")
                .replace("<gold>", "<gold>")
                .replace("<gray>", "<gray>")
                .replace("<dark_gray>", "<dark_gray>")
                .replace("<blue>", "<blue>")
                .replace("<green>", "<green>")
                .replace("<aqua>", "<aqua>")
                .replace("<red>", "<red>")
                .replace("<light_purple>", "<light_purple>")
                .replace("<yellow>", "<yellow>")
                .replace("<white>", "<white>")
                .replace("<obfuscated>", "<obfuscated>")
                .replace("<bold>", "<bold>")
                .replace("<strikethrough>", "<strikethrough>")
                .replace("<underlined>", "<underlined>")
                .replace("<italic>", "<italic>")
                .replace("<reset>", "<reset>");

        return MiniMessage.miniMessage().deserialize(mmText, TagResolver.resolver(resolvers));
    }

}
