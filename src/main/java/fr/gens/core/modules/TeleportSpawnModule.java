package fr.gens.core.modules;

import fr.gens.core.CorePlugin;
import fr.gens.core.utils.TeleportUtil;

import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;







public class TeleportSpawnModule implements Module, CommandExecutor {

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
        org.bukkit.command.PluginCommand setspawnCmd = plugin.getCommand("setspawn");
        if (setspawnCmd != null) setspawnCmd.setExecutor(this);
        
        org.bukkit.command.PluginCommand spawnCmd = plugin.getCommand("spawn");
        if (spawnCmd != null) spawnCmd.setExecutor(this);
    }

    @Override
    public void disable() {
        enabled = false;
        plugin.getLangManager().sendConsoleMessage("teleportspawnmodule.log_2");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!enabled) {
            plugin.getLangManager().sendMessage(sender, "teleportspawnmodule.msg_1");
            return true;
        }

        if (!(sender instanceof Player)) return true;
        Player p = (Player) sender;

        if (command.getName().equalsIgnoreCase("setspawn")) {
            if (!p.hasPermission("genscore.admin")) {
                plugin.getLangManager().sendMessage(p, "teleportspawnmodule.msg_2");
                return true;
            }
            spawnLocation = p.getLocation();
            
            this.spawnDAO.saveSpawn(spawnLocation);
            
            plugin.getLangManager().sendMessage(p, "teleportspawnmodule.msg_3");
            return true;
        }

        if (command.getName().equalsIgnoreCase("spawn")) {
            if (!p.hasPermission("genscore.spawn")) {
                plugin.getLangManager().sendMessage(p, "teleportspawnmodule.msg_4");
                return true;
            }
            if (spawnLocation != null) {
                TeleportUtil.teleportWithCooldown(plugin, p, spawnLocation, "le spawn", "genscore.bypass.cooldown.spawn");
            } else {
                plugin.getLangManager().sendMessage(p, "teleportspawnmodule.msg_5");
            }
            return true;
        }
        
        return false;
    }
}

