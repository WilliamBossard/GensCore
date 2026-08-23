package fr.gens.core.utils;

import fr.gens.core.CorePlugin;
import org.bukkit.command.CommandSender;
import cloud.commandframework.paper.PaperCommandManager;
import cloud.commandframework.execution.CommandExecutionCoordinator;
import cloud.commandframework.annotations.AnnotationParser;
import cloud.commandframework.meta.SimpleCommandMeta;
import cloud.commandframework.minecraft.extras.MinecraftExceptionHandler;
import cloud.commandframework.minecraft.extras.MinecraftHelp;
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
                .apply(this.paperCommandManager, CommandSender::sendMessage);
            
            this.paperCommandManager.captionVariableReplacementHandler(new cloud.commandframework.captions.CaptionVariableReplacementHandler() {
                @Override
                public String replaceVariables(String string, cloud.commandframework.captions.CaptionVariable... variables) {
                    for (cloud.commandframework.captions.CaptionVariable variable : variables) {
                        string = string.replace("<" + variable.getKey() + ">", variable.getValue());
                    }
                    return string;
                }
            });
            
            cloud.commandframework.captions.CaptionRegistry<CommandSender> registry = this.paperCommandManager.captionRegistry();
            registry.registerProvider(cloud.commandframework.captions.CaptionProvider.constantProvider(cloud.commandframework.captions.StandardCaptionKeys.ARGUMENT_PARSE_FAILURE_BOOLEAN, "Impossible d'analyser le booléen depuis '<input>'"));
            registry.registerProvider(cloud.commandframework.captions.CaptionProvider.constantProvider(cloud.commandframework.captions.StandardCaptionKeys.ARGUMENT_PARSE_FAILURE_NUMBER, "'<input>' n'est pas un nombre valide"));
            registry.registerProvider(cloud.commandframework.captions.CaptionProvider.constantProvider(cloud.commandframework.captions.StandardCaptionKeys.ARGUMENT_PARSE_FAILURE_STRING, "'<input>' n'est pas une chaîne de caractères valide"));
            registry.registerProvider(cloud.commandframework.captions.CaptionProvider.constantProvider(cloud.commandframework.captions.StandardCaptionKeys.ARGUMENT_PARSE_FAILURE_ENUM, "'<input>' n'est pas une valeur autorisée. Les valeurs sont : <acceptableValues>"));
            registry.registerProvider(cloud.commandframework.captions.CaptionProvider.constantProvider(cloud.commandframework.captions.StandardCaptionKeys.ARGUMENT_PARSE_FAILURE_COLOR, "'<input>' n'est pas une couleur valide"));
            registry.registerProvider(cloud.commandframework.captions.CaptionProvider.constantProvider(cloud.commandframework.captions.StandardCaptionKeys.EXCEPTION_INVALID_ARGUMENT, "Argument invalide : <cause>"));
            registry.registerProvider(cloud.commandframework.captions.CaptionProvider.constantProvider(cloud.commandframework.captions.StandardCaptionKeys.EXCEPTION_INVALID_SYNTAX, "Syntaxe invalide. Essayez : <syntax>"));
            registry.registerProvider(cloud.commandframework.captions.CaptionProvider.constantProvider(cloud.commandframework.captions.StandardCaptionKeys.EXCEPTION_INVALID_SENDER, "Vous ne pouvez pas exécuter cette commande."));
            registry.registerProvider(cloud.commandframework.captions.CaptionProvider.constantProvider(cloud.commandframework.captions.StandardCaptionKeys.EXCEPTION_NO_PERMISSION, "Vous n'avez pas la permission de faire cela."));

            
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
