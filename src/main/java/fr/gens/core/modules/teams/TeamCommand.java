package fr.gens.core.modules.teams;

import fr.gens.core.CorePlugin;
import org.bukkit.entity.Player;
import org.bukkit.Bukkit;
import cloud.commandframework.annotations.Argument;
import cloud.commandframework.annotations.CommandMethod;

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

    @CommandMethod("team")
    public void executeTeamGui(Player player) {
        if (!module.isEnabled()) {
            player.sendMessage(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize("<red>Ce module est actuellement désactivé.</red>"));
            return;
        }
        teamGui.openTeamGui(player);
    }

    @CommandMethod("team create <name>")
    public void executeTeamCreate(Player player, @Argument("name") String name) {
        if (!module.isEnabled()) {
            player.sendMessage(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize("<red>Ce module est actuellement désactivé.</red>"));
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
                player.sendMessage(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize("<green>Guilde " + name + " créée avec succès !"));
            }
        });
    }

    @CommandMethod("team invite <target>")
    public void executeTeamInvite(Player player, @Argument("target") String targetName) {
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
        target.sendMessage(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize("<green>Vous avez reçu une invitation pour rejoindre la guilde <yellow>" + team.getName() + "<green> !"));
        plugin.getLangManager().sendMessage(target, "teamcommand.msg_9");
        player.sendMessage(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize("<green>Invitation envoyée à " + target.getName()));
    }

    @CommandMethod("team accept")
    public void executeTeamAccept(Player player) {
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

