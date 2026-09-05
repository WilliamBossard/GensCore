package fr.gens.core.commands;

import org.incendo.cloud.annotations.Argument;
import org.incendo.cloud.annotations.Command;
import org.incendo.cloud.annotations.Permission;
import fr.gens.core.CorePlugin;
import fr.gens.core.modules.Module;
import org.bukkit.command.CommandSender;


public class ModuleCommand {

    private final CorePlugin plugin;

    public ModuleCommand(CorePlugin plugin) {
        this.plugin = plugin;
    }

    @Command("module <moduleName> <state>")
    @Permission("genscore.admin")
    public void execute(CommandSender sender, @Argument(value = "moduleName", suggestions = "moduleNames", description = "Le nom du module") String moduleName, @Argument(value = "state", suggestions = "moduleStates", description = "enable ou disable") String stateStr) {
        Module module = plugin.getModuleManager().getModule(moduleName);
        if (module == null) {
            sender.sendMessage(fr.gens.core.utils.PlaceholderUtils.parseToComponent("<red>Module introuvable: " + moduleName));
            return;
        }

        boolean state;
        if (stateStr.equalsIgnoreCase("on") || stateStr.equalsIgnoreCase("enable")) {
            state = true;
        } else if (stateStr.equalsIgnoreCase("off") || stateStr.equalsIgnoreCase("disable")) {
            state = false;
        } else {
            sender.sendMessage(fr.gens.core.utils.PlaceholderUtils.parseToComponent("<red>L'action doit \u00eatre 'on' ou 'off'."));
            return;
        }

        if (module.isEnabled() == state) {
            sender.sendMessage(fr.gens.core.utils.PlaceholderUtils.parseToComponent("<yellow>Le module " + module.getName() + " est d\u00e9j\u00e0 sur ce statut."));
            return;
        }

        boolean success = plugin.getModuleManager().toggleModule(moduleName, state);
        if (success) {
            String str = state ? "<green>ACTIV\u00c9" : "<red>D\u00c9SACTIV\u00c9";
            sender.sendMessage(fr.gens.core.utils.PlaceholderUtils.parseToComponent("<green>Le module <gold>" + module.getName() + "<green> a \u00e9t\u00e9 " + str + "<green>."));
        } else {
            sender.sendMessage(fr.gens.core.utils.PlaceholderUtils.parseToComponent("<red>Une erreur est survenue lors du changement de statut."));
        }
    }
    @org.incendo.cloud.annotations.suggestion.Suggestions("moduleNames")
    public java.util.List<String> suggestModules(org.incendo.cloud.context.CommandContext<CommandSender> context, String input) {
        return plugin.getModuleManager().getModules().stream().map(fr.gens.core.modules.Module::getName).filter(name -> name.toLowerCase().startsWith(input.toLowerCase())).collect(java.util.stream.Collectors.toList());
    }

    @org.incendo.cloud.annotations.suggestion.Suggestions("moduleStates")
    public java.util.List<String> suggestStates(org.incendo.cloud.context.CommandContext<CommandSender> context, String input) {
        return java.util.Arrays.asList("on", "off").stream().filter(name -> name.startsWith(input.toLowerCase())).collect(java.util.stream.Collectors.toList());
    }
}



