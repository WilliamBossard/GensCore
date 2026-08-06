package fr.gens.core.modules;

import fr.gens.core.CorePlugin;
import fr.gens.core.utils.TeleportUtil;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TeleportBackModule implements Module, CommandExecutor, Listener {

    private final CorePlugin plugin;
    private boolean enabled = false;
    private final Map<UUID, Location> lastLocations = new HashMap<>();

    public TeleportBackModule(CorePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "CmdBack";
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
        plugin.getLogger().info("[CmdBack] Activé.");
    }

    @Override
    public void disable() {
        enabled = false;
        HandlerList.unregisterAll(this);
        plugin.getLogger().info("[CmdBack] Désactivé.");
    }

    private void loadBacks() {
        lastLocations.clear();
    }

    private void saveBacks() {
        // Plus sauvegardé dans data.yml pour éviter un fichier vide
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
                "<gray>Vous êtes mort. <click:run_command:'/back'><hover:show_text:'<green>Cliquez ici pour vous téléporter à votre point de mort !'><gold><b>[Cliquez ici pour utiliser /back]</b></gold></hover></click></gray>"
            ));
        }
    }

    @EventHandler
    public void onTeleport(PlayerTeleportEvent event) {
        if(!enabled) return;
        lastLocations.put(event.getPlayer().getUniqueId(), event.getFrom());
        saveBacks();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!enabled) {
            sender.sendMessage("§cCe module est désactivé.");
            return true;
        }

        if (!(sender instanceof Player)) return true;
        Player p = (Player) sender;

        if (!p.hasPermission("genscore.back")) {
            p.sendMessage("§cVous n'avez pas la permission ou le grade requis (genscore.back).");
            return true;
        }

        Location backLoc = lastLocations.get(p.getUniqueId());
        if (backLoc != null) {
            TeleportUtil.teleportWithCooldown(p, backLoc, "l'ancienne position", "genscore.bypass.cooldown.back");
        } else {
            p.sendMessage("§cAucune position précédente trouvée.");
        }
        return true;
    }
}
