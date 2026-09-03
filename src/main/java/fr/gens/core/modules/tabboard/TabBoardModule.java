package fr.gens.core.modules.tabboard;
// Refreshing for IDE

import fr.gens.core.CorePlugin;
import fr.gens.core.modules.EconomyModule;
import fr.gens.core.modules.Module;
import fr.gens.core.utils.GensScoreboard;
import fr.gens.core.utils.PlaceholderUtils;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import net.kyori.adventure.text.Component;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;


public class TabBoardModule implements Module, Listener {

    private final CorePlugin plugin;
    private boolean enabled = false;
    private final Map<UUID, GensScoreboard> boards = new ConcurrentHashMap<>();
    private com.tcoded.folialib.wrapper.task.WrappedTask updateTask = null;

    public TabBoardModule(CorePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "TabBoard";
    }

    @Override
    public String getDescription() {
        return "Gère le Scoreboard, la Tablist et les Nametags sans PlaceholderAPI.";
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void enable() {
        enabled = true;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);

        // Initialisation pour les joueurs déjà en ligne (ex: reload)
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p == null) continue;
            setupBoard(p);
        }

        // Tâche de mise à jour toutes les secondes (20 ticks)
        updateTask = plugin.getFoliaLib().getScheduler().runTimer(() -> this.updateAll(), 20L, 20L);
        plugin.getLangManager().sendConsoleMessage("tabboardmodule.log_1");
    }

    @Override
    public void disable() {
        org.bukkit.event.HandlerList.unregisterAll(this);
        enabled = false;
        if (updateTask != null) {
            updateTask.cancel();
            updateTask = null;
        }
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p == null) continue;
            p.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
        }
        boards.clear();
        plugin.getLangManager().sendConsoleMessage("tabboardmodule.log_2");
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (!enabled) return;
        setupBoard(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        if (!enabled) return;
        boards.remove(event.getPlayer().getUniqueId());
    }

    private void setupBoard(Player player) {
        GensScoreboard board = new GensScoreboard(player, "<gold><bold>Serveur");
        boards.put(player.getUniqueId(), board);
    }

    private void updateAll() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p == null) continue;
            GensScoreboard board = boards.get(p.getUniqueId());
            if (board != null) {
                updateScoreboard(p, board);
                updateTabList(p);
                updateNametags(p, board.getScoreboard());
            }
        }
    }

    private void updateScoreboard(Player p, GensScoreboard board) {
        List<String> lines = new ArrayList<>();
        List<String> configLines = plugin.getConfigManager().getConfig("modules/tabboard.yml").getStringList("tabboard.scoreboard.lines");
        if (configLines == null || configLines.isEmpty()) {
            configLines = new ArrayList<>();
            configLines.add("<gray><strikethrough>--------------------");
            configLines.add("<white>Joueur: <yellow>%player%");
            configLines.add("<white>Grade: %prefix%");
            configLines.add("");
            configLines.add("<gold> Économie:");
            configLines.add("<white>Argent: <green>%money%$");
            configLines.add("<white>Bourse: <gray>/shop");
            configLines.add("");
            configLines.add("<aqua> Quêtes:");
            configLines.add("<white>Terminées: <dark_aqua>0");
            configLines.add("");
            configLines.add("<white>En ligne: <aqua>%online%");
            configLines.add("<gray><strikethrough>--------------------");
            configLines.add("<yellow>IP: <white>82.64.129.187");
            plugin.getConfigManager().getConfig("modules/tabboard.yml").set("tabboard.scoreboard.lines", configLines);
            plugin.getConfigManager().getConfig("modules/tabboard.yml").set("tabboard.scoreboard.title", "<gold><bold>Serveur");
            plugin.getConfigManager().saveConfig("modules/tabboard.yml");
        }

        String title = plugin.getConfigManager().getConfig("modules/tabboard.yml").getString("tabboard.scoreboard.title", "<gold><bold>Serveur");
        board.updateTitle(title);

        EconomyModule eco = (EconomyModule) plugin.getModuleManager().getModule("economy");
        boolean ecoEnabled = eco != null && eco.isEnabled();

        for (String line : configLines) {
            if (!ecoEnabled && (line.contains("%money%") || line.contains("Économie") || line.contains("/shop") || line.contains("Bourse"))) {
                if (line.contains("Économie")) {
                    lines.add(net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection().serialize(PlaceholderUtils.setPlaceholdersComponent(plugin, p, "<gold>\u2694 Statistiques:")));
                } else if (line.contains("%money%")) {
                    int playMinutes = p.getStatistic(org.bukkit.Statistic.PLAY_ONE_MINUTE) / 1200;
                    int playHours = playMinutes / 60;
                    int playMins = playMinutes % 60;
                    lines.add(net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection().serialize(PlaceholderUtils.setPlaceholdersComponent(plugin, p, "<white>Temps de jeu: <yellow>" + playHours + "h" + playMins + "m")));
                } else if (line.contains("Bourse")) {
                    int mobKills = p.getStatistic(org.bukkit.Statistic.MOB_KILLS);
                    lines.add(net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection().serialize(PlaceholderUtils.setPlaceholdersComponent(plugin, p, "<white>Mobs tués: <red>" + mobKills)));
                }
                continue;
            }
            
            String parsed = net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection().serialize(PlaceholderUtils.setPlaceholdersComponent(plugin, p, line));
            lines.add(parsed);
            
            // Inject Jobs after Economy if it's the blank line before Quests, or just dynamically
            if (line.equals("&b Quêtes:")) {
                // Insert Jobs before Quests
                int insertIdx = lines.size() - 1;
                lines.add(insertIdx, "<green> Métiers:");
                fr.gens.core.modules.jobs.JobsModule jobsMod = (fr.gens.core.modules.jobs.JobsModule) plugin.getModuleManager().getModule("jobs");
                if (jobsMod != null && jobsMod.isEnabled()) {
                    boolean hasJob = false;
                    for (fr.gens.core.modules.jobs.JobType type : fr.gens.core.modules.jobs.JobType.values()) {
                        if (jobsMod.hasJob(p.getUniqueId(), type)) {
                            hasJob = true;
                            int lvl = jobsMod.getLevel(p.getUniqueId(), type);
                            lines.add(insertIdx + 1, "<white>" + type.getDisplayName() + ": <green>Lvl " + lvl);
                            insertIdx++;
                        }
                    }
                    if (!hasJob) {
                        lines.add(insertIdx + 1, "<gray>Aucun métier");
                        insertIdx++;
                    }
                }
                lines.add(insertIdx + 1, "");
            }
        }

        board.updateLines(lines);
    }

    private void updateTabList(Player p) {
        if (!plugin.getConfigManager().getConfig("modules/tabboard.yml").contains("tabboard.tablist.header")) {
            plugin.getConfigManager().getConfig("modules/tabboard.yml").set("tabboard.tablist.header", "<strikethrough>                                                                <reset>\n<dark_aqua><bold>Le Serveur Des Gens Bien\n<reset><gray><bold>>> <yellow>Bienvenue <dark_aqua><bold>%player% <gray><bold>! <<\n<reset><gray>Joueurs en ligne: <white>%online%\n<gold>Staff en ligne: <yellow>%staff%");
            plugin.getConfigManager().getConfig("modules/tabboard.yml").set("tabboard.tablist.footer", "\n<dark_green>Ping: %ping%ms\n<gray><bold>Mémoire: %mem_used% MB / %mem_max% MB\n<gray>Quêtes terminées: <yellow>0\n\n%discord_status%\n<strikethrough>                                                                ");
            plugin.getConfigManager().getConfig("modules/tabboard.yml").set("tabboard.tablist.discord_not_linked", "<yellow><bold> <red>Discord non lié ! <aqua>/linktuto");
            plugin.getConfigManager().getConfig("modules/tabboard.yml").set("tabboard.tablist.discord_linked", "<reset><gray>Le Discord: <aqua>discord.gg/gensbien");
            plugin.getConfigManager().saveConfig("modules/tabboard.yml");
        }


        String rawHeader = plugin.getConfigManager().getConfig("modules/tabboard.yml").getString("tabboard.tablist.header", "");
        String rawFooter = plugin.getConfigManager().getConfig("modules/tabboard.yml").getString("tabboard.tablist.footer", "");

        Component headerComp = fr.gens.core.utils.PlaceholderUtils.setPlaceholdersComponent(plugin, p, rawHeader);
        Component footerComp = fr.gens.core.utils.PlaceholderUtils.setPlaceholdersComponent(plugin, p, rawFooter);

        p.sendPlayerListHeaderAndFooter(headerComp, footerComp);
    }

    private void updateNametags(Player p, Scoreboard scoreboard) {
        for (Player target : Bukkit.getOnlinePlayers()) {
            if (target == null) continue;
            String teamName = getTeamWeight(target) + "_" + target.getName();
            // Maximum 16 characters for team names in older versions, safe in 1.21+
            if (teamName.length() > 16) teamName = teamName.substring(0, 16);

            Team team = scoreboard.getTeam(teamName);
            if (team == null) {
                team = scoreboard.registerNewTeam(teamName);
            }

            if (!team.hasEntry(target.getName())) {
                team.addEntry(target.getName());
            }

            String prefixStr = getLuckPermsPrefix(target);
            if (prefixStr.length() > 50) prefixStr = prefixStr.substring(0, 50); // Laisse de la place pour la guilde
            
            // Ajout du tag de Guilde (Team)
            fr.gens.core.modules.teams.TeamData tData = plugin.getTeamManager().getPlayerTeam(target.getUniqueId());
            if (tData != null) {
                prefixStr = prefixStr + " <yellow>[" + tData.getName() + "] ";
            } else {
                prefixStr = prefixStr + " ";
            }
            
            String suffixStr = target.hasPermission("genscore.discord.linked") ? " <aqua><bold>" : "";

            Component prefixComp = fr.gens.core.utils.PlaceholderUtils.parseToComponent(prefixStr);
            Component suffixComp = fr.gens.core.utils.PlaceholderUtils.parseToComponent(suffixStr);

            team.prefix(prefixComp);
            team.suffix(suffixComp);
        }
    }

    private String getTeamWeight(Player p) {
        if (p.hasPermission("group.owner")) return "01";
        if (p.hasPermission("group.admin")) return "02";
        if (p.hasPermission("group.mod")) return "03";
        if (p.hasPermission("group.helper")) return "04";
        if (p.hasPermission("group.premium")) return "05";
        return "99";
    }


    private String getLuckPermsPrefix(Player p) {
        try {
            LuckPerms api = LuckPermsProvider.get();
            User user = api.getUserManager().getUser(p.getUniqueId());
            if (user != null) {
                String prefix = user.getCachedData().getMetaData().getPrefix();
                if (prefix != null && !prefix.trim().isEmpty()) {
                    return prefix;
                }
                
                // Fallback to group display name if prefix is not set
                String groupName = user.getPrimaryGroup();
                if (groupName != null) {
                    return "<yellow>" + groupName.substring(0, 1).toUpperCase() + groupName.substring(1);
                }
            }
        } catch (Exception ignored) {} // In case LuckPerms is missing
        return "<gray>Joueur";
    }
}





