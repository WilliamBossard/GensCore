package fr.gens.core.modules.lock;

import fr.gens.core.CorePlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class LockCommand implements CommandExecutor, TabCompleter {
    private final CorePlugin plugin;
    private final LockModule lockModule;
    
    // Map pour savoir qui a tapé quelle commande avant de cliquer sur un bloc
    public static final Map<UUID, String> pendingActions = new HashMap<>();

    public LockCommand(CorePlugin plugin, LockModule lockModule) {
        this.plugin = plugin;
        this.lockModule = lockModule;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) return true;
        Player player = (Player) sender;

        if (args.length == 0) {
            plugin.getLangManager().sendMessage(player, "lockcommand.msg_1");
            pendingActions.put(player.getUniqueId(), "private");
            return true;
        }

        String sub = args[0].toLowerCase();
        
        if (sub.equals("private")) {
            plugin.getLangManager().sendMessage(player, "lockcommand.msg_2");
            pendingActions.put(player.getUniqueId(), "private");
            return true;
        }

        if (sub.equals("unlock")) {
            plugin.getLangManager().sendMessage(player, "lockcommand.msg_3");
            pendingActions.put(player.getUniqueId(), "unlock");
            return true;
        }

        if (sub.equals("guild")) {
            plugin.getLangManager().sendMessage(player, "lockcommand.msg_4");
            pendingActions.put(player.getUniqueId(), "guild");
            return true;
        }

        plugin.getLangManager().sendMessage(player, "lockcommand.msg_5");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            if ("private".startsWith(args[0].toLowerCase())) completions.add("private");
            if ("unlock".startsWith(args[0].toLowerCase())) completions.add("unlock");
            if ("guild".startsWith(args[0].toLowerCase())) completions.add("guild");
        }
        return completions;
    }
}
