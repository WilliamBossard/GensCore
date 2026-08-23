package fr.gens.core.utils;

import fr.gens.core.CorePlugin;
import org.bukkit.command.CommandSender;
import cloud.commandframework.paper.PaperCommandManager;
import cloud.commandframework.execution.CommandExecutionCoordinator;
import cloud.commandframework.annotations.AnnotationParser;
import cloud.commandframework.meta.SimpleCommandMeta;
import cloud.commandframework.minecraft.extras.MinecraftExceptionHandler;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

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
            
            try {
                this.paperCommandManager.registerBrigadier();
            } catch (Exception ignored) {}
            
            try {
                this.paperCommandManager.registerAsynchronousCompletions();
            } catch (Exception ignored) {}
            
            this.annotationParser = new AnnotationParser<>(this.paperCommandManager, CommandSender.class, parameters -> SimpleCommandMeta.empty());
            
            new MinecraftExceptionHandler<CommandSender>()
                .withInvalidSyntaxHandler()
                .withInvalidSenderHandler()
                .withNoPermissionHandler()
                .withArgumentParsingHandler()
                .withCommandExecutionHandler()
                .withDecorator(
                    component -> Component.text("[", NamedTextColor.DARK_GRAY)
                        .append(Component.text("Système", NamedTextColor.GOLD))
                        .append(Component.text("] ", NamedTextColor.DARK_GRAY))
                        .append(component)
                )
                .apply(this.paperCommandManager, sender -> sender);
            
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
