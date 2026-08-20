package fr.gens.core.modules.utils;

import fr.gens.core.CorePlugin;
import fr.gens.core.modules.Module;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import java.util.Collections;
import java.util.List;

import org.bukkit.command.CommandExecutor;
import org.bukkit.command.TabCompleter;


public class UtilsModule implements Module, Listener, CommandExecutor, TabCompleter {

    private CorePlugin plugin;
    private boolean enabled;

    public UtilsModule(CorePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "utils";
    }

    @Override
    public String getDescription() {
        return "Commandes utilitaires du serveur";
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void initDatabase(fr.gens.core.utils.DatabaseManager dbManager) {
        dbManager.executeStatement("CREATE TABLE IF NOT EXISTS player_transactions_history (id INTEGER PRIMARY KEY AUTOINCREMENT, uuid VARCHAR(36) NOT NULL, type VARCHAR(10) NOT NULL, material VARCHAR(50) NOT NULL, amount INTEGER NOT NULL, price DOUBLE NOT NULL, timestamp BIGINT NOT NULL);");
        dbManager.executeStatement("CREATE TABLE IF NOT EXISTS player_minigame_cooldowns (uuid VARCHAR(36), game_id VARCHAR(50), last_played BIGINT DEFAULT 0, PRIMARY KEY(uuid, game_id));");
        dbManager.executeStatement("CREATE TABLE IF NOT EXISTS player_profiles (uuid VARCHAR(36) PRIMARY KEY, username VARCHAR(16) NOT NULL);");
        dbManager.executeStatement("CREATE INDEX IF NOT EXISTS idx_transactions_uuid ON player_transactions_history(uuid);");
    }

    @Override
    public void enable() {
        this.enabled = true;
        plugin.getLangManager().sendConsoleMessage("utilsmodule.log_1");
    }

    @Override
    public void registerCommands(CorePlugin plugin) {
        String[] utilsCmds = {"anvil", "craftingtable", "enchanttable", "ec", "feed"};
        for (String cmd : utilsCmds) {
            org.bukkit.command.PluginCommand c = plugin.getCommand(cmd);
            if (c != null) c.setExecutor(this);
        }
    }

    @Override
    public void disable() {
        org.bukkit.event.HandlerList.unregisterAll(this);
        this.enabled = false;
        plugin.getLangManager().sendConsoleMessage("utilsmodule.log_2");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!enabled || !(sender instanceof Player)) return true;
        Player p = (Player) sender;

        if (label.equalsIgnoreCase("anvil")) {
            if (!p.hasPermission("genscore.anvil")) {
                plugin.getLangManager().sendMessage(p, "utilsmodule.msg_1");
                return true;
            }
            p.openInventory(org.bukkit.Bukkit.createInventory(p, org.bukkit.event.inventory.InventoryType.ANVIL));
            return true;
        }

        if (label.equalsIgnoreCase("craftingtable") || label.equalsIgnoreCase("craft") || label.equalsIgnoreCase("workbench")) {
            if (!p.hasPermission("genscore.craft")) {
                plugin.getLangManager().sendMessage(p, "utilsmodule.msg_2");
                return true;
            }
            p.openInventory(org.bukkit.Bukkit.createInventory(p, org.bukkit.event.inventory.InventoryType.WORKBENCH));
            return true;
        }

        if (label.equalsIgnoreCase("enchanttable") || label.equalsIgnoreCase("enchanting")) {
            if (!p.hasPermission("genscore.enchant")) {
                plugin.getLangManager().sendMessage(p, "utilsmodule.msg_3");
                return true;
            }
            p.openInventory(org.bukkit.Bukkit.createInventory(p, org.bukkit.event.inventory.InventoryType.ENCHANTING));
            return true;
        }

        if (label.equalsIgnoreCase("ec") || label.equalsIgnoreCase("enderchest")) {
            if (!p.hasPermission("genscore.ec")) {
                plugin.getLangManager().sendMessage(p, "utilsmodule.msg_4");
                return true;
            }
            if (args.length > 0) {
                if (!p.hasPermission("genscore.admin")) {
                    plugin.getLangManager().sendMessage(p, "utilsmodule.msg_5");
                    return true;
                }
                Player target = org.bukkit.Bukkit.getPlayer(args[0]);
                if (target == null) {
                    plugin.getLangManager().sendMessage(p, "utilsmodule.msg_6");
                    return true;
                }
                p.openInventory(target.getEnderChest());
                p.sendMessage("<green>Vous regardez l'enderchest de <yellow>" + target.getName() + "<green>.");
            } else {
                p.openInventory(p.getEnderChest());
            }
            return true;
        }

        if (label.equalsIgnoreCase("feed")) {
            if (!p.hasPermission("genscore.feed")) {
                plugin.getLangManager().sendMessage(p, "utilsmodule.msg_7");
                return true;
            }
            p.setFoodLevel(20);
            p.setSaturation(20.0f);
            plugin.getLangManager().sendMessage(p, "utilsmodule.msg_8");
            return true;
        }

        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if ((label.equalsIgnoreCase("ec") || label.equalsIgnoreCase("enderchest")) && args.length == 1 && sender.hasPermission("genscore.admin")) {
            List<String> names = new java.util.ArrayList<>();
            for (Player player : org.bukkit.Bukkit.getOnlinePlayers()) {
            if (player == null) continue;
                if (player.getName().toLowerCase().startsWith(args[0].toLowerCase())) {
                    names.add(player.getName());
                }
            }
            return names;
        }
        return Collections.emptyList();
    }
}


