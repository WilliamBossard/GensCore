package fr.gens.core.modules;

import fr.gens.core.CorePlugin;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class ChatModule implements Module, Listener {

    private final CorePlugin plugin;
    private boolean enabled = false;

    public ChatModule(CorePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "Chat";
    }

    @Override
    public String getDescription() {
        return "Gère le formatage du chat. Permission: genscore.chat";
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void enable() {
        enabled = true;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        plugin.getLangManager().sendConsoleMessage("chatmodule.log_1");
    }

    @Override
    public void disable() {
        enabled = false;
        HandlerList.unregisterAll(this);
        plugin.getLangManager().sendConsoleMessage("chatmodule.log_2");
    }

    @EventHandler
    public void onChat(AsyncChatEvent event) {
        if (!enabled) return;
        
        // Exige la permission pour parler
        if (!event.getPlayer().hasPermission("genscore.chat")) {
            event.getPlayer().sendMessage("<red>Vous n'avez pas la permission de parler dans le chat (genscore.chat).");
            event.setCancelled(true);
            return;
        }

        // Récupérer le préfixe depuis LuckPerms
        String resolvedPrefix;
        try {
            net.luckperms.api.LuckPerms api = net.luckperms.api.LuckPermsProvider.get();
            net.luckperms.api.model.user.User user = api.getUserManager().getUser(event.getPlayer().getUniqueId());
            if (user != null) {
                String lpPrefix = user.getCachedData().getMetaData().getPrefix();
                if (lpPrefix != null) {
                    net.kyori.adventure.text.Component prefixComp = fr.gens.core.utils.PlaceholderUtils.parseToComponent(lpPrefix);
                    resolvedPrefix = net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.builder()
                        .character('§')
                        .hexColors()
                        .build()
                        .serialize(prefixComp) + " ";
                } else {
                    resolvedPrefix = "<gray>[Joueur] ";
                }
            } else {
                resolvedPrefix = "<gray>[Joueur] ";
            }
        } catch (Exception e) {
            resolvedPrefix = event.getPlayer().hasPermission("genscore.admin") ? "<red>[Admin] " : "<gray>[Joueur] ";
        }
        
        // Ajout du tag de guilde si le joueur en a une
        fr.gens.core.modules.teams.TeamData team = plugin.getTeamManager().getPlayerTeam(event.getPlayer().getUniqueId());
        final String guildTag = (team != null) ? "<yellow>[" + team.getName() + "] " : "";
        
        // Variables effectively final pour le lambda
        final String finalPrefix = resolvedPrefix;
        final String messageText = PlainTextComponentSerializer.plainText().serialize(event.message());

        // AsyncChatEvent utilise un ChatRenderer pour formater le message
        event.renderer((source, sourceDisplayName, message, viewer) ->
            net.kyori.adventure.text.minimessage.MiniMessage.miniMessage()
                .deserialize(finalPrefix + guildTag + "<white>" + source.getName() + " <dark_gray>» <gray>" + messageText)
        );
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (!enabled) return;
        if (!plugin.getConfigManager().getConfig("modules/chat.yml").getBoolean("chat.custom-join-messages", true)) return;
        
        if (!event.getPlayer().hasPlayedBefore()) {
            event.joinMessage(plugin.getLangManager().get("chat.first_join", net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.parsed("player", event.getPlayer().getName())));
        } else {
            event.joinMessage(plugin.getLangManager().get("chat.join", net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.parsed("player", event.getPlayer().getName())));
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        if (!enabled) return;
        if (!plugin.getConfigManager().getConfig("modules/chat.yml").getBoolean("chat.custom-join-messages", true)) return;
        
        event.quitMessage(plugin.getLangManager().get("chat.quit", net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.parsed("player", event.getPlayer().getName())));
    }
}
