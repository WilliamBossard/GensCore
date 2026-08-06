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
        plugin.getLogger().info("[CmdTpa] Activé.");
    }

    @Override
    public void disable() {
        enabled = false;
        tpaRequests.clear();
        plugin.getLogger().info("[CmdTpa] Désactivé.");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!enabled) {
            sender.sendMessage("§cCe module est désactivé.");
            return true;
        }
        if (!(sender instanceof Player)) return true;
        Player p = (Player) sender;

        if (command.getName().equalsIgnoreCase("tpa")) {
            if (!p.hasPermission("genscore.tpa")) {
                p.sendMessage("§cPermission refusée (genscore.tpa).");
                return true;
            }
            if (args.length == 0) {
                p.sendMessage("§cUsage: /tpa <joueur>");
                return true;
            }
            
            Player target = Bukkit.getPlayer(args[0]);
            if (target == null || !target.isOnline()) {
                p.sendMessage("§cCe joueur n'est pas en ligne.");
                return true;
            }
            if (target.getUniqueId().equals(p.getUniqueId())) {
                p.sendMessage("§cVous ne pouvez pas vous téléporter sur vous-même.");
                return true;
            }

            tpaRequests.put(target.getUniqueId(), p.getUniqueId());
            
            p.sendMessage("§aDemande de téléportation envoyée à §e" + target.getName() + "§a.");
            
            Component acceptBtn = Component.text("[Accepter] ")
                    .color(NamedTextColor.GREEN).decorate(TextDecoration.BOLD)
                    .clickEvent(ClickEvent.runCommand("/tpaccept"));
                    
            Component denyBtn = Component.text("[Refuser]")
                    .color(NamedTextColor.RED).decorate(TextDecoration.BOLD)
                    .clickEvent(ClickEvent.runCommand("/tpdeny"));

            target.sendMessage(Component.text("§e" + p.getName() + " §asouhaite se téléporter à vous."));
            target.sendMessage(acceptBtn.append(denyBtn));

            // Expiration après 60 secondes
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (tpaRequests.containsKey(target.getUniqueId()) && tpaRequests.get(target.getUniqueId()).equals(p.getUniqueId())) {
                    tpaRequests.remove(target.getUniqueId());
                    p.sendMessage("§cVotre demande de téléportation vers §e" + target.getName() + " §ca expiré.");
                    target.sendMessage("§cLa demande de téléportation de §e" + p.getName() + " §ca expiré.");
                }
            }, 20 * 60L);
            return true;
        }

        if (command.getName().equalsIgnoreCase("tpaccept")) {
            if (!tpaRequests.containsKey(p.getUniqueId())) {
                p.sendMessage("§cAucune demande de téléportation en attente.");
                return true;
            }
            
            UUID requesterId = tpaRequests.remove(p.getUniqueId());
            Player requester = Bukkit.getPlayer(requesterId);
            if (requester != null && requester.isOnline()) {
                p.sendMessage("§aDemande acceptée.");
                requester.sendMessage("§aDemande acceptée par §e" + p.getName() + "§a. Téléportation...");
                TeleportUtil.teleportWithCooldown(requester, p.getLocation(), p.getName(), "genscore.bypass.cooldown.tpa");
            } else {
                p.sendMessage("§cLe joueur n'est plus en ligne.");
            }
            return true;
        }

        if (command.getName().equalsIgnoreCase("tpdeny")) {
            if (!tpaRequests.containsKey(p.getUniqueId())) {
                p.sendMessage("§cAucune demande de téléportation en attente.");
                return true;
            }
            
            UUID requesterId = tpaRequests.remove(p.getUniqueId());
            Player requester = Bukkit.getPlayer(requesterId);
            p.sendMessage("§cDemande refusée.");
            if (requester != null && requester.isOnline()) {
                requester.sendMessage("§cDemande refusée par §e" + p.getName() + "§c.");
            }
            return true;
        }

        if (command.getName().equalsIgnoreCase("tpacancel")) {
            boolean removed = false;
            for (Map.Entry<UUID, UUID> entry : tpaRequests.entrySet()) {
                if (entry.getValue().equals(p.getUniqueId())) {
                    tpaRequests.remove(entry.getKey());
                    p.sendMessage("§cDemande annulée.");
                    removed = true;
                    break;
                }
            }
            if (!removed) {
                p.sendMessage("§cVous n'avez envoyé aucune demande en attente.");
            }
            return true;
        }

        return false;
    }
}
