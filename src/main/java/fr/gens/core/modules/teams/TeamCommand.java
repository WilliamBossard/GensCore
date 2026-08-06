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
                player.sendMessage("§cUsage: /team create <nom>");
                return true;
            }
            if (team != null) {
                player.sendMessage("§cVous êtes déjà dans une guilde.");
                return true;
            }
            String name = args[1];
            if (name.length() > 16) {
                player.sendMessage("§cLe nom de la guilde est trop long (16 max).");
                return true;
            }
            TeamData newTeam = plugin.getTeamManager().createTeam(name, player.getUniqueId());
            if (newTeam == null) {
                player.sendMessage("§cCe nom de guilde est déjà pris ou une erreur est survenue.");
            } else {
                player.sendMessage("§aGuilde " + name + " créée avec succès !");
            }
            return true;
        }

        if (sub.equals("invite")) {
            if (team == null || !team.getLeaderUuid().equals(player.getUniqueId())) {
                player.sendMessage("§cVous devez être chef de guilde pour inviter.");
                return true;
            }
            if (args.length < 2) {
                player.sendMessage("§cUsage: /team invite <joueur>");
                return true;
            }
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                player.sendMessage("§cJoueur introuvable.");
                return true;
            }
            if (plugin.getTeamManager().getPlayerTeam(target.getUniqueId()) != null) {
                player.sendMessage("§cCe joueur est déjà dans une guilde.");
                return true;
            }
            invites.put(target.getUniqueId(), player.getUniqueId());
            target.sendMessage("§aVous avez reçu une invitation pour rejoindre la guilde §e" + team.getName() + "§a !");
            target.sendMessage("§aTapez §e/team accept §apour rejoindre.");
            player.sendMessage("§aInvitation envoyée à " + target.getName());
            return true;
        }

        if (sub.equals("accept")) {
            if (team != null) {
                player.sendMessage("§cVous êtes déjà dans une guilde.");
                return true;
            }
            UUID leaderUuid = invites.get(player.getUniqueId());
            if (leaderUuid == null) {
                player.sendMessage("§cAucune invitation en attente.");
                return true;
            }
            TeamData leaderTeam = plugin.getTeamManager().getPlayerTeam(leaderUuid);
            if (leaderTeam != null) {
                plugin.getTeamManager().addMember(leaderTeam, player.getUniqueId());
                leaderTeam.broadcast("§e" + player.getName() + " §aa rejoint la guilde !");
            } else {
                player.sendMessage("§cLa guilde n'existe plus.");
            }
            invites.remove(player.getUniqueId());
            return true;
        }

        player.sendMessage("§cUsage: /team create <nom> | /team invite <joueur> | /team accept");
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
