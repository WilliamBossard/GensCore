package fr.gens.core.utils;

import fr.gens.core.CorePlugin;
import org.bukkit.command.CommandSender;
import org.incendo.cloud.paper.PaperCommandManager;
import org.incendo.cloud.execution.ExecutionCoordinator;
import org.incendo.cloud.annotations.AnnotationParser;
import org.incendo.cloud.minecraft.extras.MinecraftExceptionHandler;
import org.incendo.cloud.SenderMapper;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.stream.Collectors;
import java.util.Arrays;

public class CommandManager {

    private PaperCommandManager<CommandSender> paperCommandManager;
    private AnnotationParser<CommandSender> annotationParser;

    public CommandManager(CorePlugin plugin) {
        try {
            this.paperCommandManager = new PaperCommandManager<>(
                plugin,
                ExecutionCoordinator.simpleCoordinator(),
                SenderMapper.identity()
            );
            
            // Cloud v2 gère automatiquement Brigadier sur Paper 1.20.6+ via LifecycleEventManager !
            try {
                this.paperCommandManager.registerAsynchronousCompletions();
            } catch (Exception ignored) {}
            
            this.paperCommandManager.parserRegistry().registerSuggestionProvider("onlinePlayers", 
                (context, input) -> org.bukkit.Bukkit.getOnlinePlayers().stream()
                    .map(org.bukkit.entity.Player::getName)
                    .collect(Collectors.toList())
            );
            
            this.paperCommandManager.parserRegistry().registerSuggestionProvider("spawnerTypes", 
                (context, input) -> Arrays.stream(org.bukkit.entity.EntityType.values())
                    .filter(org.bukkit.entity.EntityType::isAlive)
                    .map(Enum::name)
                    .collect(Collectors.toList())
            );
            
            this.annotationParser = new AnnotationParser<>(this.paperCommandManager, CommandSender.class);
            
            MinecraftExceptionHandler.createNative()
                .defaultInvalidSyntaxHandler()
                .defaultInvalidSenderHandler()
                .defaultNoPermissionHandler()
                .defaultArgumentParsingHandler()
                .defaultCommandExecutionHandler()
                .decorator(
                    component -> Component.text("[", NamedTextColor.DARK_GRAY)
                        .append(Component.text("Système", NamedTextColor.GOLD))
                        .append(Component.text("] ", NamedTextColor.DARK_GRAY))
                        .append(component)
                )
                .registerTo(this.paperCommandManager);
            
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
