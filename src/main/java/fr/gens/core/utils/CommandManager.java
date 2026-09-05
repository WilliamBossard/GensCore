package fr.gens.core.utils;

import fr.gens.core.CorePlugin;
import org.bukkit.command.CommandSender;
import org.incendo.cloud.paper.LegacyPaperCommandManager;
import org.incendo.cloud.execution.ExecutionCoordinator;
import org.incendo.cloud.annotations.AnnotationParser;
import org.incendo.cloud.minecraft.extras.MinecraftExceptionHandler;
import org.incendo.cloud.SenderMapper;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.stream.Collectors;
import java.util.Arrays;

public class CommandManager {

    private LegacyPaperCommandManager<CommandSender> paperCommandManager;
    private AnnotationParser<CommandSender> annotationParser;

    public CommandManager(CorePlugin plugin) {
        try {
            this.paperCommandManager = new LegacyPaperCommandManager<>(
                plugin,
                ExecutionCoordinator.simpleCoordinator(),
                SenderMapper.identity()
            );
            
            // Enregistrement manuel de Brigadier (requis pour LegacyPaperCommandManager dans Cloud V2)
            if (this.paperCommandManager.hasCapability(org.incendo.cloud.bukkit.CloudBukkitCapabilities.NATIVE_BRIGADIER)) {
                this.paperCommandManager.registerBrigadier();
            } else if (this.paperCommandManager.hasCapability(org.incendo.cloud.bukkit.CloudBukkitCapabilities.ASYNCHRONOUS_COMPLETION)) {
                this.paperCommandManager.registerAsynchronousCompletions();
            }
            
            this.paperCommandManager.parserRegistry().registerSuggestionProvider("onlinePlayers", 
                (context, input) -> java.util.concurrent.CompletableFuture.completedFuture(org.bukkit.Bukkit.getOnlinePlayers().stream()
                    .map(org.bukkit.entity.Player::getName)
                    .map(org.incendo.cloud.suggestion.Suggestion::suggestion)
                    .collect(Collectors.toList()))
            );
            
            this.paperCommandManager.parserRegistry().registerSuggestionProvider("spawnerTypes", 
                (context, input) -> java.util.concurrent.CompletableFuture.completedFuture(Arrays.stream(org.bukkit.entity.EntityType.values())
                    .filter(org.bukkit.entity.EntityType::isAlive)
                    .map(Enum::name)
                    .map(org.incendo.cloud.suggestion.Suggestion::suggestion)
                    .collect(Collectors.toList()))
            );
            
            this.annotationParser = new AnnotationParser<>(this.paperCommandManager, CommandSender.class);
            
            MinecraftExceptionHandler.<CommandSender>createNative()
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
    
    public LegacyPaperCommandManager<CommandSender> getPaperCommandManager() {
        return paperCommandManager;
    }
}
