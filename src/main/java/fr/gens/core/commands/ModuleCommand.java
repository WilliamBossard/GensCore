package fr.gens.core.commands;

import fr.gens.core.CorePlugin;
import fr.gens.core.modules.Module;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class ModuleCommand implements CommandExecutor, TabCompleter {

    private final CorePlugin plugin;

    public ModuleCommand(CorePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.isOp() && !sender.hasPermission("genscore.admin")) {
            sender.sendMessage("<red>" + "Vous n'avez pas la permission d'utiliser cette commande.");
            return true;
        }

        if (args.length != 2) {
            sender.sendMessage("<red>" + "Usage: /module <nom_du_module> <on|off>");
            return true;
        }

        String moduleName = args[0].toLowerCase();
        String action = args[1].toLowerCase();

        Module module = plugin.getModuleManager().getModule(moduleName);
        if (module == null) {
            sender.sendMessage("<red>" + "Module introuvable: " + moduleName);
            return true;
        }

        boolean state;
        if (action.equals("on") || action.equals("enable")) {
            state = true;
        } else if (action.equals("off") || action.equals("disable")) {
            state = false;
        } else {
            sender.sendMessage("<red>" + "L'action doit être 'on' ou 'off'.");
            return true;
        }

        if (module.isEnabled() == state) {
            sender.sendMessage("<yellow>" + "Le module " + module.getName() + " est déjà sur ce statut.");
            return true;
        }

        boolean success = plugin.getModuleManager().toggleModule(moduleName, state);
        if (success) {
            String stateStr = state ? "<green>" + "ACTIVÉ" : "<red>" + "DÉSACTIVÉ";
            sender.sendMessage("<green>" + "Le module " + "<gold>" + module.getName() + "<green>" + " a été " + stateStr + "<green>" + ".");
        } else {
            sender.sendMessage("<red>" + "Une erreur est survenue lors du changement de statut.");
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (!sender.isOp() && !sender.hasPermission("genscore.admin")) {
            return completions;
        }

        if (args.length == 1) {
            for (Module module : plugin.getModuleManager().getModules()) {
                if (module.getName().toLowerCase().startsWith(args[0].toLowerCase())) {
                    completions.add(module.getName());
                }
            }
        } else if (args.length == 2) {
            if ("on".startsWith(args[1].toLowerCase())) completions.add("on");
            if ("off".startsWith(args[1].toLowerCase())) completions.add("off");
        }

        return completions;
    }
}
