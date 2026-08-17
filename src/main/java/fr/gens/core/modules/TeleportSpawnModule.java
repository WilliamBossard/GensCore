package fr.gens.core.modules;

import fr.gens.core.CorePlugin;
import fr.gens.core.utils.TeleportUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class TeleportSpawnModule implements Module, CommandExecutor {

    private final CorePlugin plugin;
    private boolean enabled = false;
    private Location spawnLocation;

    public TeleportSpawnModule(CorePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "CmdSpawn";
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
        // Charger le spawn
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM spawn_location WHERE id = 1");
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                String worldName = rs.getString("world");
                if (Bukkit.getWorld(worldName) != null) {
                    spawnLocation = new Location(
                            Bukkit.getWorld(worldName),
                            rs.getDouble("x"),
                            rs.getDouble("y"),
                            rs.getDouble("z"),
                            rs.getFloat("yaw"),
                            rs.getFloat("pitch")
                    );
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
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
            
            try (Connection conn = plugin.getDatabaseManager().getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                         "INSERT INTO spawn_location (id, world, x, y, z, yaw, pitch) VALUES (1, ?, ?, ?, ?, ?, ?) " +
                         "ON CONFLICT(id) DO UPDATE SET world=excluded.world, x=excluded.x, y=excluded.y, z=excluded.z, yaw=excluded.yaw, pitch=excluded.pitch")) {
                ps.setString(1, spawnLocation.getWorld().getName());
                ps.setDouble(2, spawnLocation.getX());
                ps.setDouble(3, spawnLocation.getY());
                ps.setDouble(4, spawnLocation.getZ());
                ps.setFloat(5, spawnLocation.getYaw());
                ps.setFloat(6, spawnLocation.getPitch());
                ps.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
            
            plugin.getLangManager().sendMessage(p, "teleportspawnmodule.msg_3");
            return true;
        }

        if (command.getName().equalsIgnoreCase("spawn")) {
            if (!p.hasPermission("genscore.spawn")) {
                plugin.getLangManager().sendMessage(p, "teleportspawnmodule.msg_4");
                return true;
            }
            if (spawnLocation != null) {
                TeleportUtil.teleportWithCooldown(p, spawnLocation, "le spawn", "genscore.bypass.cooldown.spawn");
            } else {
                plugin.getLangManager().sendMessage(p, "teleportspawnmodule.msg_5");
            }
            return true;
        }
        
        return false;
    }
}
