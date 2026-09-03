package fr.gens.core.utils;
// Refreshing for IDE

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


public class PlaceholderUtils {

    public static Component parseToComponent(String text) {
        if (text == null) return Component.empty();
        
        String processed = text.replace("§", "&");
        
        // Convertir tous les codes legacy (&a, &l, etc.) en balises MiniMessage
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
            
        return MiniMessage.miniMessage().deserialize("<!italic>" + mmString);
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
        String discordStatus = linked ? "<gray>Le Discord: <aqua>discord.gg/gensbien" : "<yellow><bold>🔗 <red>Discord non lié ! <aqua>/linktuto";
        
        // Wait, if it contains colors, we should use MiniMessage Component
        resolvers.add(Placeholder.component("discord_status", parseToComponent(discordStatus)));
        
        String discordName = linked ? "Compte Lié" : "Non lié";
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
        String mmText = text.replace("§", "&")
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

        if (org.bukkit.Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            mmText = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(p, mmText);
        }

        // Convertir également les codes couleurs legacy introduits par PAPI en MiniMessage
        mmText = mmText
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

        return MiniMessage.miniMessage().deserialize(mmText, resolvers.toArray(new TagResolver[0]));
    }

}
