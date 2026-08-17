package fr.gens.core.modules.teams;

import fr.gens.core.CorePlugin;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class TeamCommand implements CommandExecutor, TabCompleter {
    private final CorePlugin plugin;
    private final TeamGui teamGui;
    private final Map<UUID, UUID> invites = new HashMap<>(); // invited -> teamLeader

    public TeamCommand(CorePlugin plugin, TeamGui teamGui) {
        this.plugin = plugin;
        this.teamGui = teamGui;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) return true;
        Player player = (Player) sender;

        if (args.length == 0) {
            teamGui.openTeamGui(player);
            return true;
        }

        String sub = args[0].toLowerCase();
        TeamData team = plugin.getTeamManager().getPlayerTeam(player.getUniqueId());

        if (sub.equals("create")) {
            if (args.length < 2) {
                plugin.getLangManager().sendMessage(player, "teamcommand.msg_1");
                return true;
            }
            if (team != null) {
                plugin.getLangManager().sendMessage(player, "teamcommand.msg_2");
                return true;
            }
            String name = args[1];
            if (name.length() > 16) {
                plugin.getLangManager().sendMessage(player, "teamcommand.msg_3");
                return true;
            }
            TeamData newTeam = plugin.getTeamManager().createTeam(name, player.getUniqueId());
            if (newTeam == null) {
                plugin.getLangManager().sendMessage(player, "teamcommand.msg_4");
            } else {
                player.sendMessage("<green>Guilde " + name + " créée avec succès !");
            }
            return true;
        }

        if (sub.equals("invite")) {
            if (team == null || !team.getLeaderUuid().equals(player.getUniqueId())) {
                plugin.getLangManager().sendMessage(player, "teamcommand.msg_5");
                return true;
            }
            if (args.length < 2) {
                plugin.getLangManager().sendMessage(player, "teamcommand.msg_6");
                return true;
            }
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                plugin.getLangManager().sendMessage(player, "teamcommand.msg_7");
                return true;
            }
            if (plugin.getTeamManager().getPlayerTeam(target.getUniqueId()) != null) {
                plugin.getLangManager().sendMessage(player, "teamcommand.msg_8");
                return true;
            }
            invites.put(target.getUniqueId(), player.getUniqueId());
            target.sendMessage("<green>Vous avez reçu une invitation pour rejoindre la guilde <yellow>" + team.getName() + "<green> !");
            plugin.getLangManager().sendMessage(target, "teamcommand.msg_9");
            player.sendMessage("<green>Invitation envoyée à " + target.getName());
            return true;
        }

        if (sub.equals("accept")) {
            if (team != null) {
                plugin.getLangManager().sendMessage(player, "teamcommand.msg_10");
                return true;
            }
            UUID leaderUuid = invites.get(player.getUniqueId());
            if (leaderUuid == null) {
                plugin.getLangManager().sendMessage(player, "teamcommand.msg_11");
                return true;
            }
            TeamData leaderTeam = plugin.getTeamManager().getPlayerTeam(leaderUuid);
            if (leaderTeam != null) {
                plugin.getTeamManager().addMember(leaderTeam, player.getUniqueId());
                leaderTeam.broadcast("<yellow>" + player.getName() + " <green>a rejoint la guilde !");
            } else {
                plugin.getLangManager().sendMessage(player, "teamcommand.msg_12");
            }
            invites.remove(player.getUniqueId());
            return true;
        }

        plugin.getLangManager().sendMessage(player, "teamcommand.msg_13");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            if ("create".startsWith(args[0].toLowerCase())) completions.add("create");
            if ("invite".startsWith(args[0].toLowerCase())) completions.add("invite");
            if ("accept".startsWith(args[0].toLowerCase())) completions.add("accept");
        } else if (args.length == 2 && args[0].equalsIgnoreCase("invite")) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().toLowerCase().startsWith(args[1].toLowerCase())) {
                    completions.add(p.getName());
                }
            }
        }
        return completions;
    }
}
