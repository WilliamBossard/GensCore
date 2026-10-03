package fr.gens.core.modules;

import fr.gens.core.CorePlugin;
import fr.gens.core.utils.TeleportUtil;

import org.bukkit.Location;
import org.incendo.cloud.annotations.Command;
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

    public record BackPosition(String worldName, double x, double y, double z, float yaw, float pitch) {
        public static BackPosition fromLocation(Location loc) {
            if (loc == null || loc.getWorld() == null) return null;
            return new BackPosition(loc.getWorld().getName(), loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch());
        }

        public Location toLocation() {
            org.bukkit.World w = org.bukkit.Bukkit.getWorld(worldName);
            if (w == null) return null;
            return new Location(w, x, y, z, yaw, pitch);
        }
    }

    private final CorePlugin plugin;
    private boolean enabled = false;
    private final Map<UUID, BackPosition> lastLocations = new ConcurrentHashMap<>();

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
        lastLocations.clear();
        plugin.getLangManager().sendConsoleMessage("teleportbackmodule.log_2");
    }

    private void loadBacks() {
        lastLocations.clear();
    }

    @EventHandler
    public void onQuit(org.bukkit.event.player.PlayerQuitEvent event) {
        lastLocations.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        if(!enabled) return;
        Player p = event.getEntity();
        BackPosition pos = BackPosition.fromLocation(p.getLocation());
        if (pos != null) {
            lastLocations.put(p.getUniqueId(), pos);
        }

        if (p.hasPermission("genscore.back")) {
            // Message cliquable via MiniMessage
            p.sendMessage(fr.gens.core.utils.PlaceholderUtils.parseToComponent(
                "<gray>Vous êtes mort. <click:run_command:'/back'><hover:show_text:'<green>Cliquez ici pour vous téléporter à votre point de mort !'><gold><b>[Cliquez ici pour utiliser /back]</b></gold></hover></click></gray>"
            ));
        }
    }

    @EventHandler
    public void onTeleport(PlayerTeleportEvent event) {
        if(!enabled) return;
        BackPosition pos = BackPosition.fromLocation(event.getFrom());
        if (pos != null) {
            lastLocations.put(event.getPlayer().getUniqueId(), pos);
        }
    }

    @Command("back")
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

        BackPosition pos = lastLocations.get(p.getUniqueId());
        if (pos != null) {
            Location backLoc = pos.toLocation();
            if (backLoc != null) {
                TeleportUtil.teleportWithCooldown(plugin, p, backLoc, "l'ancienne position", "genscore.bypass.cooldown.back");
                return;
            }
        }
        plugin.getLangManager().sendMessage(p, "teleportbackmodule.msg_3");
    }
}



