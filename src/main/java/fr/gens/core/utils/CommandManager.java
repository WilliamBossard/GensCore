package fr.gens.core.utils;

import fr.gens.core.CorePlugin;
import org.bukkit.command.CommandSender;
import cloud.commandframework.paper.PaperCommandManager;
import cloud.commandframework.execution.CommandExecutionCoordinator;
import cloud.commandframework.annotations.AnnotationParser;
import cloud.commandframework.meta.SimpleCommandMeta;

import java.util.function.Function;

public class CommandManager {

    private PaperCommandManager<CommandSender> paperCommandManager;
    private AnnotationParser<CommandSender> annotationParser;

    public CommandManager(CorePlugin plugin) {
        try {
            this.paperCommandManager = new PaperCommandManager<>(
                plugin,
                CommandExecutionCoordinator.simpleCoordinator(),
                Function.identity(),
                Function.identity()
            );
            
            this.annotationParser = new AnnotationParser<>(this.paperCommandManager, CommandSender.class, parameters -> SimpleCommandMeta.empty());
            
        } catch (Exception e) {
            plugin.getLogger().severe("Impossible d'initialiser Cloud Command Framework");
            e.printStackTrace();
        }
    }

    public AnnotationParser<CommandSender> getAnnotationParser() {
        return annotationParser;
    }
    
    public PaperCommandManager<CommandSender> getPaperCommandManager() {
        return paperCommandManager;
    }
}
