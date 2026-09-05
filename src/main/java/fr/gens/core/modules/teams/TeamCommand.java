package fr.gens.core.modules.teams;

import fr.gens.core.CorePlugin;
import org.bukkit.entity.Player;
import org.bukkit.Bukkit;
import org.incendo.cloud.annotations.Argument;
import org.incendo.cloud.annotations.Command;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.UUID;


public class TeamCommand {
    private final CorePlugin plugin;
    private final TeamGui teamGui;
    private final TeamModule module;
    private final Map<UUID, UUID> invites = new ConcurrentHashMap<>(); // invited -> teamLeader

    public TeamCommand(CorePlugin plugin, TeamGui teamGui, TeamModule module) {
        this.plugin = plugin;
        this.teamGui = teamGui;
        this.module = module;
    }

    @Command("team")
    public void executeTeamGui(org.bukkit.command.CommandSender sender) {
        if (!(sender instanceof org.bukkit.entity.Player)) return;
        org.bukkit.entity.Player player = (org.bukkit.entity.Player) sender;
        if (!module.isEnabled()) {
            player.sendMessage(fr.gens.core.utils.PlaceholderUtils.parseToComponent("<red>Ce module est actuellement désactivé.</red>"));
            return;
        }
        teamGui.openTeamGui(player);
    }

    @Command("team quest")
    public void executeTeamQuest(org.bukkit.command.CommandSender sender) {
        if (!(sender instanceof org.bukkit.entity.Player)) return;
        org.bukkit.entity.Player player = (org.bukkit.entity.Player) sender;
        if (!module.isEnabled()) {
            player.sendMessage(fr.gens.core.utils.PlaceholderUtils.parseToComponent("<red>Ce module est actuellement désactivé.</red>"));
            return;
        }
        TeamData team = plugin.getTeamManager().getPlayerTeam(player.getUniqueId());
        if (team == null) {
            plugin.getLangManager().sendMessage(player, "teamgui.msg_1"); // or teamcommand.msg_x
            return;
        }
        teamGui.openTeamQuestGui(player, team);
    }

    @Command("team create <name>")
    public void executeTeamCreate(org.bukkit.command.CommandSender sender, @Argument(value = "name", description = "Nom de la team") String name) {
        if (!(sender instanceof org.bukkit.entity.Player)) return;
        org.bukkit.entity.Player player = (org.bukkit.entity.Player) sender;
        if (!module.isEnabled()) {
            player.sendMessage(fr.gens.core.utils.PlaceholderUtils.parseToComponent("<red>Ce module est actuellement désactivé.</red>"));
            return;
        }
        TeamData team = plugin.getTeamManager().getPlayerTeam(player.getUniqueId());
        
        if (team != null) {
            plugin.getLangManager().sendMessage(player, "teamcommand.msg_2");
            return;
        }
        if (name.length() > 16) {
            plugin.getLangManager().sendMessage(player, "teamcommand.msg_3");
            return;
        }
        plugin.getTeamManager().createTeamAsync(name, player.getUniqueId(), newTeam -> {
            if (newTeam == null) {
                plugin.getLangManager().sendMessage(player, "teamcommand.msg_4");
            } else {
                player.sendMessage(fr.gens.core.utils.PlaceholderUtils.parseToComponent("<green>Guilde " + name + " créée avec succès !"));
            }
        });
    }

    @Command("team invite <target>")
    public void executeTeamInvite(org.bukkit.command.CommandSender sender, @Argument(value = "target", suggestions = "onlinePlayers", description = "Le joueur ciblé") String targetName) {
        if (!(sender instanceof org.bukkit.entity.Player)) return;
        org.bukkit.entity.Player player = (org.bukkit.entity.Player) sender;
        if (!module.isEnabled()) return;
        TeamData team = plugin.getTeamManager().getPlayerTeam(player.getUniqueId());
        
        if (team == null || !team.getLeaderUuid().equals(player.getUniqueId())) {
            plugin.getLangManager().sendMessage(player, "teamcommand.msg_5");
            return;
        }
        Player target = Bukkit.getPlayer(targetName);
        if (target == null) {
            plugin.getLangManager().sendMessage(player, "teamcommand.msg_7");
            return;
        }
        if (plugin.getTeamManager().getPlayerTeam(target.getUniqueId()) != null) {
            plugin.getLangManager().sendMessage(player, "teamcommand.msg_8");
            return;
        }
        invites.put(target.getUniqueId(), player.getUniqueId());
        target.sendMessage(fr.gens.core.utils.PlaceholderUtils.parseToComponent("<green>Vous avez reçu une invitation pour rejoindre la guilde <yellow>" + team.getName() + "<green> !"));
        plugin.getLangManager().sendMessage(target, "teamcommand.msg_9");
        player.sendMessage(fr.gens.core.utils.PlaceholderUtils.parseToComponent("<green>Invitation envoyée à " + target.getName()));
    }

    @Command("team accept")
    public void executeTeamAccept(org.bukkit.command.CommandSender sender) {
        if (!(sender instanceof org.bukkit.entity.Player)) return;
        org.bukkit.entity.Player player = (org.bukkit.entity.Player) sender;
        if (!module.isEnabled()) return;
        TeamData team = plugin.getTeamManager().getPlayerTeam(player.getUniqueId());
        
        if (team != null) {
            plugin.getLangManager().sendMessage(player, "teamcommand.msg_10");
            return;
        }
        UUID leaderUuid = invites.get(player.getUniqueId());
        if (leaderUuid == null) {
            plugin.getLangManager().sendMessage(player, "teamcommand.msg_11");
            return;
        }
        TeamData leaderTeam = plugin.getTeamManager().getPlayerTeam(leaderUuid);
        if (leaderTeam != null) {
            plugin.getTeamManager().addMember(leaderTeam, player.getUniqueId());
            leaderTeam.broadcast("<yellow>" + player.getName() + " <green>a rejoint la guilde !");
        } else {
            plugin.getLangManager().sendMessage(player, "teamcommand.msg_12");
        }
        invites.remove(player.getUniqueId());
    }

    public void removeInvite(UUID uuid) {
        invites.remove(uuid); // If they were invited
        invites.values().removeIf(val -> val.equals(uuid)); // If they were the inviter
    }
}



