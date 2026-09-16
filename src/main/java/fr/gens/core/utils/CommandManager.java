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

import org.incendo.cloud.paper.PaperCommandManager;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import java.lang.reflect.Proxy;

public class CommandManager {

    private org.incendo.cloud.CommandManager<CommandSender> paperCommandManager;
    private AnnotationParser<CommandSender> annotationParser;

    public CommandManager(CorePlugin plugin) {
        try {
            try {
                SenderMapper<CommandSourceStack, CommandSender> mapper = SenderMapper.create(
                    CommandSourceStack::getSender,
                    sender -> (CommandSourceStack) Proxy.newProxyInstance(
                        CommandSourceStack.class.getClassLoader(),
                        new Class<?>[]{CommandSourceStack.class},
                        (proxy, method, args) -> {
                            if ("getSender".equals(method.getName())) return sender;
                            if ("getLocation".equals(method.getName()) && sender instanceof org.bukkit.entity.Entity e) return e.getLocation();
                            return null;
                        }
                    )
                );
                this.paperCommandManager = PaperCommandManager.builder(mapper)
                    .executionCoordinator(ExecutionCoordinator.asyncCoordinator())
                    .buildOnEnable(plugin);
                plugin.getLogger().info("Cloud Command Framework initialisé avec succès via PaperCommandManager moderne (Brigadier).");
            } catch (Throwable t) {
                plugin.getLogger().warning("Modern PaperCommandManager non disponible, tentative avec LegacyPaperCommandManager: " + t.getMessage());
                LegacyPaperCommandManager<CommandSender> legacyMgr = new LegacyPaperCommandManager<>(
                    plugin,
                    ExecutionCoordinator.asyncCoordinator(),
                    SenderMapper.identity()
                );
                if (legacyMgr.hasCapability(org.incendo.cloud.bukkit.CloudBukkitCapabilities.NATIVE_BRIGADIER)) {
                    legacyMgr.registerBrigadier();
                } else if (legacyMgr.hasCapability(org.incendo.cloud.bukkit.CloudBukkitCapabilities.ASYNCHRONOUS_COMPLETION)) {
                    legacyMgr.registerAsynchronousCompletions();
                }
                this.paperCommandManager = legacyMgr;
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

            this.paperCommandManager.parserRegistry().registerSuggestionProvider("prices", 
                (context, input) -> java.util.concurrent.CompletableFuture.completedFuture(Arrays.asList(
                    org.incendo.cloud.suggestion.Suggestion.suggestion("100"),
                    org.incendo.cloud.suggestion.Suggestion.suggestion("500"),
                    org.incendo.cloud.suggestion.Suggestion.suggestion("1000"),
                    org.incendo.cloud.suggestion.Suggestion.suggestion("5000"),
                    org.incendo.cloud.suggestion.Suggestion.suggestion("10000")
                ))
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
    
    public org.incendo.cloud.CommandManager<CommandSender> getPaperCommandManager() {
        return paperCommandManager;
    }
}
