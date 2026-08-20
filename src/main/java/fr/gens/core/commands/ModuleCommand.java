package fr.gens.core.commands;

import cloud.commandframework.annotations.Argument;
import cloud.commandframework.annotations.CommandMethod;
import cloud.commandframework.annotations.CommandPermission;
import fr.gens.core.CorePlugin;
import fr.gens.core.modules.Module;
import org.bukkit.command.CommandSender;
import net.kyori.adventure.text.minimessage.MiniMessage;

public class ModuleCommand {

    private final CorePlugin plugin;

    public ModuleCommand(CorePlugin plugin) {
        this.plugin = plugin;
    }

    @CommandMethod("module <moduleName> <state>")
    @CommandPermission("genscore.admin")
    public void execute(CommandSender sender, @Argument("moduleName") String moduleName, @Argument("state") String stateStr) {
        Module module = plugin.getModuleManager().getModule(moduleName);
        if (module == null) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Module introuvable: " + moduleName));
            return;
        }

        boolean state;
        if (stateStr.equalsIgnoreCase("on") || stateStr.equalsIgnoreCase("enable")) {
            state = true;
        } else if (stateStr.equalsIgnoreCase("off") || stateStr.equalsIgnoreCase("disable")) {
            state = false;
        } else {
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>L'action doit \u00eatre 'on' ou 'off'."));
            return;
        }

        if (module.isEnabled() == state) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<yellow>Le module " + module.getName() + " est d\u00e9j\u00e0 sur ce statut."));
            return;
        }

        boolean success = plugin.getModuleManager().toggleModule(moduleName, state);
        if (success) {
            String str = state ? "<green>ACTIV\u00c9" : "<red>D\u00c9SACTIV\u00c9";
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<green>Le module <gold>" + module.getName() + "<green> a \u00e9t\u00e9 " + str + "<green>."));
        } else {
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Une erreur est survenue lors du changement de statut."));
        }
    }
}

