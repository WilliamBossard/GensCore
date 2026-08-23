package fr.gens.core.modules;

import fr.gens.core.CorePlugin;
import fr.gens.core.utils.TeleportUtil;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Location;
import cloud.commandframework.annotations.CommandMethod;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.UUID;


public class TeleportBackModule implements Module, Listener {

    private final CorePlugin plugin;
    private boolean enabled = false;
    private final Map<UUID, Location> lastLocations = new ConcurrentHashMap<>();

    public TeleportBackModule(CorePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "back";
    }

    @Override
    public String getDescription() {
        return "Commande /back payante/VIP avec message de mort interactif.";
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void enable() {
        enabled = true;
        loadBacks();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        plugin.getLangManager().sendConsoleMessage("teleportbackmodule.log_1");
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
        HandlerList.unregisterAll(this);
        plugin.getLangManager().sendConsoleMessage("teleportbackmodule.log_2");
    }

    private void loadBacks() {
        lastLocations.clear();
    }

    private void saveBacks() {
        // Plus sauvegardé dans data.yml pour éviter un fichier vide
    }

    @EventHandler
    public void onQuit(org.bukkit.event.player.PlayerQuitEvent event) {
        lastLocations.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        if(!enabled) return;
        Player p = event.getEntity();
        lastLocations.put(p.getUniqueId(), p.getLocation());
        saveBacks();

        if (p.hasPermission("genscore.back")) {
            // Message cliquable via MiniMessage
            p.sendMessage(MiniMessage.miniMessage().deserialize(
                "<gray>Vous ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Âªtes mort. <click:run_command:'/back'><hover:show_text:'<green>Cliquez ici pour vous tÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â©lÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â©porter ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â  votre point de mort !'><gold><b>[Cliquez ici pour utiliser /back]</b></gold></hover></click></gray>"
            ));
        }
    }

    @EventHandler
    public void onTeleport(PlayerTeleportEvent event) {
        if(!enabled) return;
        lastLocations.put(event.getPlayer().getUniqueId(), event.getFrom());
        saveBacks();
    }

    @CommandMethod("back")
    public void executeBack(org.bukkit.command.CommandSender sender) {
        if (!(sender instanceof org.bukkit.entity.Player)) return;
        org.bukkit.entity.Player p = (org.bukkit.entity.Player) sender;
        if (!enabled) {
            plugin.getLangManager().sendMessage(p, "teleportbackmodule.msg_1");
            return;
        }

        if (!p.hasPermission("genscore.back")) {
            plugin.getLangManager().sendMessage(p, "teleportbackmodule.msg_2");
            return;
        }

        Location backLoc = lastLocations.get(p.getUniqueId());
        if (backLoc != null) {
            TeleportUtil.teleportWithCooldown(plugin, p, backLoc, "l'ancienne position", "genscore.bypass.cooldown.back");
        } else {
            plugin.getLangManager().sendMessage(p, "teleportbackmodule.msg_3");
        }
    }
}

