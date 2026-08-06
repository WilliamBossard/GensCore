package fr.gens.core.modules.tabboard;

import fr.gens.core.CorePlugin;
import fr.gens.core.modules.EconomyModule;
import fr.gens.core.modules.Module;
import fr.gens.core.utils.DatabaseManager;
import fr.gens.core.utils.GensScoreboard;
import fr.gens.core.utils.PlaceholderUtils;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import net.kyori.adventure.text.Component;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class TabBoardModule implements Module, Listener {

    private final CorePlugin plugin;
    private boolean enabled = false;
    private final Map<UUID, GensScoreboard> boards = new HashMap<>();
    private int taskId = -1;

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
            setupBoard(p);
        }

        // Tâche de mise à jour toutes les secondes (20 ticks)
        taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, this::updateAll, 20L, 20L);
        plugin.getLogger().info("[TabBoard] Module activé.");
    }

    @Override
    public void disable() {
        enabled = false;
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
            taskId = -1;
        }
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
        }
        boards.clear();
        plugin.getLogger().info("[TabBoard] Module désactivé.");
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
        GensScoreboard board = new GensScoreboard(player, "§6§lGensBienV4");
        boards.put(player.getUniqueId(), board);
    }

    private void updateAll() {
        for (Player p : Bukkit.getOnlinePlayers()) {
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
        List<String> configLines = plugin.getConfig().getStringList("tabboard.scoreboard.lines");
        if (configLines == null || configLines.isEmpty()) {
            configLines = new ArrayList<>();
            configLines.add("&7&m--------------------");
            configLines.add("&fJoueur: &e%player%");
            configLines.add("&fGrade: %prefix%");
            configLines.add("");
            configLines.add("&6💰 Économie:");
            configLines.add("&fArgent: &a%money%$");
            configLines.add("&fBourse: &7/shop");
            configLines.add("");
            configLines.add("&b📜 Quêtes:");
            configLines.add("&fTerminées: &30");
            configLines.add("");
            configLines.add("&fEn ligne: &b%online%");
            configLines.add("&7&m--------------------");
            configLines.add("&eIP: &f82.64.129.187");
            plugin.getConfig().set("tabboard.scoreboard.lines", configLines);
            plugin.getConfig().set("tabboard.scoreboard.title", "&6&lGensBienV4");
            plugin.saveConfig();
        }

        String title = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("tabboard.scoreboard.title", "&6&lGensBienV4"));
        board.updateTitle(title);

        EconomyModule eco = (EconomyModule) plugin.getModuleManager().getModule("economy");
        boolean ecoEnabled = eco != null && eco.isEnabled();

        for (String line : configLines) {
            if (!ecoEnabled && (line.contains("%money%") || line.contains("Économie") || line.contains("/shop") || line.contains("Bourse"))) {
                if (line.contains("Économie")) {
                    lines.add(PlaceholderUtils.setPlaceholders(plugin, p, "&6\u2694 Statistiques:"));
                } else if (line.contains("%money%")) {
                    int playMinutes = p.getStatistic(org.bukkit.Statistic.PLAY_ONE_MINUTE) / 1200;
                    int playHours = playMinutes / 60;
                    int playMins = playMinutes % 60;
                    lines.add(PlaceholderUtils.setPlaceholders(plugin, p, "&fTemps de jeu: &e" + playHours + "h" + playMins + "m"));
                } else if (line.contains("Bourse")) {
                    int mobKills = p.getStatistic(org.bukkit.Statistic.MOB_KILLS);
                    lines.add(PlaceholderUtils.setPlaceholders(plugin, p, "&fMobs tués: &c" + mobKills));
                }
                continue;
            }
            
            String parsed = PlaceholderUtils.setPlaceholders(plugin, p, line);
            lines.add(parsed);
            
            // Inject Jobs after Economy if it's the blank line before Quests, or just dynamically
            if (line.equals("&b📜 Quêtes:")) {
                // Insert Jobs before Quests
                int insertIdx = lines.size() - 1;
                lines.add(insertIdx, "§a🛠 Métiers:");
                fr.gens.core.modules.jobs.JobsModule jobsMod = (fr.gens.core.modules.jobs.JobsModule) plugin.getModuleManager().getModule("jobs");
                if (jobsMod != null && jobsMod.isEnabled()) {
                    boolean hasJob = false;
                    for (fr.gens.core.modules.jobs.JobType type : fr.gens.core.modules.jobs.JobType.values()) {
                        if (jobsMod.hasJob(p.getUniqueId(), type)) {
                            hasJob = true;
                            int lvl = jobsMod.getLevel(p.getUniqueId(), type);
                            lines.add(insertIdx + 1, "§f" + type.getDisplayName() + ": §aLvl " + lvl);
                            insertIdx++;
                        }
                    }
                    if (!hasJob) {
                        lines.add(insertIdx + 1, "§7Aucun métier");
                        insertIdx++;
                    }
                }
                lines.add(insertIdx + 1, "");
            }
        }

        board.updateLines(lines);
    }

    private void updateTabList(Player p) {
        if (!plugin.getConfig().contains("tabboard.tablist.header")) {
            plugin.getConfig().set("tabboard.tablist.header", "&m                                                                &r\n&3&lLe Serveur Des Gens Bien\n&r&7&l>> &eBienvenue &3&l%player% &7&l! <<\n&r&7Joueurs en ligne: &f%online%\n&6Staff en ligne: &e%staff%");
            plugin.getConfig().set("tabboard.tablist.footer", "\n&2Ping: %ping%ms\n&7&lMémoire: %mem_used% MB / %mem_max% MB\n&7Quêtes terminées: &e0\n\n%discord_status%\n&m                                                                ");
            plugin.getConfig().set("tabboard.tablist.discord_not_linked", "&e&l⚠ &cDiscord non lié ! &b/linktuto");
            plugin.getConfig().set("tabboard.tablist.discord_linked", "&r&7Le Discord: &bdiscord.gg/gensbien");
            plugin.saveConfig();
        }

        long maxMemory = Runtime.getRuntime().maxMemory() / 1048576;
        long usedMemory = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1048576;

        String discordStatus = p.hasPermission("genscore.discord.linked") 
                ? plugin.getConfig().getString("tabboard.tablist.discord_linked", "") 
                : plugin.getConfig().getString("tabboard.tablist.discord_not_linked", "");

        String rawHeader = plugin.getConfig().getString("tabboard.tablist.header", "");
        String rawFooter = plugin.getConfig().getString("tabboard.tablist.footer", "");

        String finalHeaderStr = PlaceholderUtils.setPlaceholders(plugin, p, rawHeader);
        String finalFooterStr = PlaceholderUtils.setPlaceholders(plugin, p, rawFooter);

        Component headerComp = fr.gens.core.utils.PlaceholderUtils.parseToComponent(finalHeaderStr);
        Component footerComp = fr.gens.core.utils.PlaceholderUtils.parseToComponent(finalFooterStr);

        p.sendPlayerListHeaderAndFooter(headerComp, footerComp);
    }

    private void updateNametags(Player p, Scoreboard scoreboard) {
        for (Player target : Bukkit.getOnlinePlayers()) {
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
                prefixStr = prefixStr + " §e[" + tData.getName() + "] ";
            } else {
                prefixStr = prefixStr + " ";
            }
            
            String suffixStr = target.hasPermission("genscore.discord.linked") ? " §b§l✔" : "";

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

    private int getStaffCount() {
        int count = 0;
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.hasPermission("group.owner") || p.hasPermission("group.admin") || p.hasPermission("group.mod") || p.hasPermission("group.helper")) {
                count++;
            }
        }
        return count;
    }

    private String getBalance(Player p) {
        EconomyModule eco = (EconomyModule) plugin.getModuleManager().getModule("economy");
        if (eco != null) {
            return String.format("%.0f", eco.getBalance(p.getUniqueId()));
        }
        return "0";
    }

    private String getLuckPermsPrefix(Player p) {
        try {
            LuckPerms api = LuckPermsProvider.get();
            User user = api.getUserManager().getUser(p.getUniqueId());
            if (user != null) {
                String prefix = user.getCachedData().getMetaData().getPrefix();
                if (prefix != null && !prefix.trim().isEmpty()) {
                    return ChatColor.translateAlternateColorCodes('&', prefix);
                }
                
                // Fallback to group display name if prefix is not set
                String groupName = user.getPrimaryGroup();
                if (groupName != null) {
                    return "§e" + groupName.substring(0, 1).toUpperCase() + groupName.substring(1);
                }
            }
        } catch (Exception ignored) {} // In case LuckPerms is missing
        return "§7Joueur";
    }
}
