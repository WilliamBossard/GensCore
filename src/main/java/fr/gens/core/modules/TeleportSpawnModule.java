package fr.gens.core.modules;

import fr.gens.core.CorePlugin;
import fr.gens.core.utils.TeleportUtil;

import org.bukkit.Location;
import org.incendo.cloud.annotations.CommandMethod;








public class TeleportSpawnModule implements Module {

    private final CorePlugin plugin;
    private boolean enabled = false;
    private Location spawnLocation;
    
    private fr.gens.core.database.SpawnDAO spawnDAO;

    public TeleportSpawnModule(CorePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "spawn";
    }

    @Override
    public String getDescription() {
        return "Commandes /spawn et /setspawn persistantes avec cooldowns.";
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void enable() {
        enabled = true;
        
        this.spawnDAO = new fr.gens.core.database.SpawnDAO(plugin);
        this.spawnDAO.initDatabase();
        
        // Charger le spawn
        spawnLocation = this.spawnDAO.loadSpawn();
        plugin.getLangManager().sendConsoleMessage("teleportspawnmodule.log_1");
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
        plugin.getLangManager().sendConsoleMessage("teleportspawnmodule.log_2");
    }

    @CommandMethod("setspawn")
    public void executeSetSpawn(org.bukkit.command.CommandSender sender) {
        if (!(sender instanceof org.bukkit.entity.Player)) return;
        org.bukkit.entity.Player p = (org.bukkit.entity.Player) sender;
        if (!enabled) {
            plugin.getLangManager().sendMessage(p, "teleportspawnmodule.msg_1");
            return;
        }
        if (!p.hasPermission("genscore.admin")) {
            plugin.getLangManager().sendMessage(p, "teleportspawnmodule.msg_2");
            return;
        }
        spawnLocation = p.getLocation();
        
        this.spawnDAO.saveSpawn(spawnLocation);
        
        plugin.getLangManager().sendMessage(p, "teleportspawnmodule.msg_3");
    }

    @CommandMethod("spawn")
    public void executeSpawn(org.bukkit.command.CommandSender sender) {
        if (!(sender instanceof org.bukkit.entity.Player)) return;
        org.bukkit.entity.Player p = (org.bukkit.entity.Player) sender;
        if (!enabled) {
            plugin.getLangManager().sendMessage(p, "teleportspawnmodule.msg_1");
            return;
        }
        if (!p.hasPermission("genscore.spawn")) {
            plugin.getLangManager().sendMessage(p, "teleportspawnmodule.msg_4");
            return;
        }
        if (spawnLocation != null) {
            TeleportUtil.teleportWithCooldown(plugin, p, spawnLocation, "le spawn", "genscore.bypass.cooldown.spawn");
        } else {
            plugin.getLangManager().sendMessage(p, "teleportspawnmodule.msg_5");
        }
    }
}



