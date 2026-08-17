package fr.gens.core.modules;

import fr.gens.core.CorePlugin;
import fr.gens.core.utils.TeleportUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TeleportTpaModule implements Module, CommandExecutor {

    private final CorePlugin plugin;
    private boolean enabled = false;
    
    // Key = Target (Receiver), Value = Sender (Requester)
    private final Map<UUID, UUID> tpaRequests = new HashMap<>();

    public TeleportTpaModule(CorePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "CmdTpa";
    }

    @Override
    public String getDescription() {
        return "Commandes de demande de téléportation (/tpa, /tpaccept, /tpdeny, /tpacancel).";
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void enable() {
        enabled = true;
        plugin.getLangManager().sendConsoleMessage("teleporttpamodule.log_1");
    }

    @Override
    public void registerCommands(fr.gens.core.CorePlugin plugin) {
        String[] cmds = {"tpa", "tpaccept", "tpdeny", "tpadeny", "tpacancel"};
        for (String c : cmds) {
            org.bukkit.command.PluginCommand cmd = plugin.getCommand(c);
            if (cmd != null) cmd.setExecutor(this);
        }
    }

    @Override
    public void disable() {
        enabled = false;
        tpaRequests.clear();
        plugin.getLangManager().sendConsoleMessage("teleporttpamodule.log_2");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!enabled) {
            plugin.getLangManager().sendMessage(sender, "teleporttpamodule.msg_1");
            return true;
        }
        if (!(sender instanceof Player)) return true;
        Player p = (Player) sender;

        if (command.getName().equalsIgnoreCase("tpa")) {
            if (!p.hasPermission("genscore.tpa")) {
                plugin.getLangManager().sendMessage(p, "teleporttpamodule.msg_2");
                return true;
            }
            if (args.length == 0) {
                plugin.getLangManager().sendMessage(p, "teleporttpamodule.msg_3");
                return true;
            }
            
            Player target = Bukkit.getPlayer(args[0]);
            if (target == null || !target.isOnline()) {
                plugin.getLangManager().sendMessage(p, "teleporttpamodule.msg_4");
                return true;
            }
            if (target.getUniqueId().equals(p.getUniqueId())) {
                plugin.getLangManager().sendMessage(p, "teleporttpamodule.msg_5");
                return true;
            }

            tpaRequests.put(target.getUniqueId(), p.getUniqueId());
            
            p.sendMessage("<green>Demande de téléportation envoyée à <yellow>" + target.getName() + "<green>.");
            
            Component acceptBtn = Component.text("[Accepter] ")
                    .color(NamedTextColor.GREEN).decorate(TextDecoration.BOLD)
                    .clickEvent(ClickEvent.runCommand("/tpaccept"));
                    
            Component denyBtn = Component.text("[Refuser]")
                    .color(NamedTextColor.RED).decorate(TextDecoration.BOLD)
                    .clickEvent(ClickEvent.runCommand("/tpdeny"));

            target.sendMessage(Component.text("<yellow>" + p.getName() + " <green>souhaite se téléporter à vous."));
            target.sendMessage(acceptBtn.append(denyBtn));

            // Expiration après 60 secondes
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (tpaRequests.containsKey(target.getUniqueId()) && tpaRequests.get(target.getUniqueId()).equals(p.getUniqueId())) {
                    tpaRequests.remove(target.getUniqueId());
                    p.sendMessage("<red>Votre demande de téléportation vers <yellow>" + target.getName() + " <red>a expiré.");
                    target.sendMessage("<red>La demande de téléportation de <yellow>" + p.getName() + " <red>a expiré.");
                }
            }, 20 * 60L);
            return true;
        }

        if (command.getName().equalsIgnoreCase("tpaccept")) {
            if (!tpaRequests.containsKey(p.getUniqueId())) {
                plugin.getLangManager().sendMessage(p, "teleporttpamodule.msg_6");
                return true;
            }
            
            UUID requesterId = tpaRequests.remove(p.getUniqueId());
            Player requester = Bukkit.getPlayer(requesterId);
            if (requester != null && requester.isOnline()) {
                plugin.getLangManager().sendMessage(p, "teleporttpamodule.msg_7");
                requester.sendMessage("<green>Demande acceptée par <yellow>" + p.getName() + "<green>. Téléportation...");
                TeleportUtil.teleportWithCooldown(requester, p.getLocation(), p.getName(), "genscore.bypass.cooldown.tpa");
            } else {
                plugin.getLangManager().sendMessage(p, "teleporttpamodule.msg_8");
            }
            return true;
        }

        if (command.getName().equalsIgnoreCase("tpdeny")) {
            if (!tpaRequests.containsKey(p.getUniqueId())) {
                plugin.getLangManager().sendMessage(p, "teleporttpamodule.msg_9");
                return true;
            }
            
            UUID requesterId = tpaRequests.remove(p.getUniqueId());
            Player requester = Bukkit.getPlayer(requesterId);
            plugin.getLangManager().sendMessage(p, "teleporttpamodule.msg_10");
            if (requester != null && requester.isOnline()) {
                requester.sendMessage("<red>Demande refusée par <yellow>" + p.getName() + "<red>.");
            }
            return true;
        }

        if (command.getName().equalsIgnoreCase("tpacancel")) {
            boolean removed = false;
            for (Map.Entry<UUID, UUID> entry : tpaRequests.entrySet()) {
                if (entry.getValue().equals(p.getUniqueId())) {
                    tpaRequests.remove(entry.getKey());
                    plugin.getLangManager().sendMessage(p, "teleporttpamodule.msg_11");
                    removed = true;
                    break;
                }
            }
            if (!removed) {
                plugin.getLangManager().sendMessage(p, "teleporttpamodule.msg_12");
            }
            return true;
        }

        return false;
    }
}
