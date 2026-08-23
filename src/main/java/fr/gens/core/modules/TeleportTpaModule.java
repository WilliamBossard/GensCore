package fr.gens.core.modules;

import fr.gens.core.CorePlugin;
import fr.gens.core.utils.TeleportUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import cloud.commandframework.annotations.Argument;
import cloud.commandframework.annotations.CommandMethod;
import org.bukkit.entity.Player;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class TeleportTpaModule implements Module, Listener {

    private final CorePlugin plugin;
    private boolean enabled = false;
    
    // Key = Target (Receiver), Value = Sender (Requester)
    private final Map<UUID, UUID> tpaRequests = new ConcurrentHashMap<>();

    public TeleportTpaModule(CorePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "tpa";
    }

    @Override
    public String getDescription() {
        return "Commandes de demande de tÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â©lÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â©portation (/tpa, /tpaccept, /tpdeny, /tpacancel).";
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void enable() {
        enabled = true;
        Bukkit.getPluginManager().registerEvents(this, plugin);
        plugin.getLangManager().sendConsoleMessage("teleporttpamodule.log_1");
    }

    @Override
    public void registerCommands(fr.gens.core.CorePlugin plugin) {
        if (plugin.getCommandManager() != null && plugin.getCommandManager().getAnnotationParser() != null) {
            plugin.getCommandManager().getAnnotationParser().parse(this);
        }
    }

    @Override
    public void disable() {
        enabled = false;
        tpaRequests.clear();
        org.bukkit.event.HandlerList.unregisterAll(this);
        plugin.getLangManager().sendConsoleMessage("teleporttpamodule.log_2");
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        tpaRequests.remove(event.getPlayer().getUniqueId());
        tpaRequests.entrySet().removeIf(entry -> entry.getValue().equals(event.getPlayer().getUniqueId()));
    }

    @CommandMethod("tpa <target>")
    public void executeTpa(Player p, @Argument("target") String targetName) {
        if (!enabled) {
            plugin.getLangManager().sendMessage(p, "teleporttpamodule.msg_1");
            return;
        }
        if (!p.hasPermission("genscore.tpa")) {
            plugin.getLangManager().sendMessage(p, "teleporttpamodule.msg_2");
            return;
        }
        Player target = Bukkit.getPlayer(targetName);
        if (target == null || !target.isOnline()) {
            plugin.getLangManager().sendMessage(p, "teleporttpamodule.msg_4");
            return;
        }
        if (target.getUniqueId().equals(p.getUniqueId())) {
            plugin.getLangManager().sendMessage(p, "teleporttpamodule.msg_5");
            return;
        }

        tpaRequests.put(target.getUniqueId(), p.getUniqueId());
        
        p.sendMessage(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize("<green>Demande de tÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â©lÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â©portation envoyÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â©e ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â  <yellow>" + target.getName() + "<green>."));
        
        Component acceptBtn = Component.text("[Accepter] ")
                .color(NamedTextColor.GREEN).decorate(TextDecoration.BOLD)
                .clickEvent(ClickEvent.runCommand("/tpaccept"));
                
        Component denyBtn = Component.text("[Refuser]")
                .color(NamedTextColor.RED).decorate(TextDecoration.BOLD)
                .clickEvent(ClickEvent.runCommand("/tpdeny"));

        target.sendMessage(Component.text("<yellow>" + p.getName() + " <green>souhaite se tÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â©lÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â©porter ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â  vous."));
        target.sendMessage(acceptBtn.append(denyBtn));

        // Expiration aprÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¨s 60 secondes
        plugin.getFoliaLib().getImpl().runLater((t2) -> {
            if (tpaRequests.containsKey(target.getUniqueId()) && tpaRequests.get(target.getUniqueId()).equals(p.getUniqueId())) {
                tpaRequests.remove(target.getUniqueId());
                p.sendMessage(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize("<red>Votre demande de tÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â©lÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â©portation vers <yellow>" + target.getName() + " <red>a expirÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â©."));
                target.sendMessage(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize("<red>La demande de tÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â©lÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â©portation de <yellow>" + p.getName() + " <red>a expirÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â©."));
            }
        }, 20 * 60L);
    }

    @CommandMethod("tpaccept")
    public void executeTpaccept(Player p) {
        if (!enabled) return;
        if (!tpaRequests.containsKey(p.getUniqueId())) {
            plugin.getLangManager().sendMessage(p, "teleporttpamodule.msg_6");
            return;
        }
        
        UUID requesterId = tpaRequests.remove(p.getUniqueId());
        Player requester = Bukkit.getPlayer(requesterId);
        if (requester != null && requester.isOnline()) {
            plugin.getLangManager().sendMessage(p, "teleporttpamodule.msg_7");
            requester.sendMessage(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize("<green>Demande acceptÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â©e par <yellow>" + p.getName() + "<green>. TÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â©lÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â©portation..."));
            TeleportUtil.teleportWithCooldown(plugin, requester, p.getLocation(), p.getName(), "genscore.bypass.cooldown.tpa");
        } else {
            plugin.getLangManager().sendMessage(p, "teleporttpamodule.msg_8");
        }
    }

    @CommandMethod("tpdeny|tpadeny")
    public void executeTpdeny(Player p) {
        if (!enabled) return;
        if (!tpaRequests.containsKey(p.getUniqueId())) {
            plugin.getLangManager().sendMessage(p, "teleporttpamodule.msg_9");
            return;
        }
        
        UUID requesterId = tpaRequests.remove(p.getUniqueId());
        Player requester = Bukkit.getPlayer(requesterId);
        plugin.getLangManager().sendMessage(p, "teleporttpamodule.msg_10");
        if (requester != null && requester.isOnline()) {
            requester.sendMessage(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize("<red>Demande refusÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â©e par <yellow>" + p.getName() + "<red>."));
        }
    }

    @CommandMethod("tpacancel")
    public void executeTpacancel(Player p) {
        if (!enabled) return;
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
    }
}

