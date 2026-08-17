package fr.gens.core.modules;

import fr.gens.core.CorePlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
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
    public void onChat(AsyncPlayerChatEvent event) {
        if (!enabled) return;
        
        // Exige la permission pour parler
        if (!event.getPlayer().hasPermission("genscore.chat")) {
            event.getPlayer().sendMessage("§cVous n'avez pas la permission de parler dans le chat (genscore.chat).");
            event.setCancelled(true);
            return;
        }

        // Récupérer le préfixe depuis LuckPerms
        String prefix = "§7[Joueur] ";
        try {
            net.luckperms.api.LuckPerms api = net.luckperms.api.LuckPermsProvider.get();
            net.luckperms.api.model.user.User user = api.getUserManager().getUser(event.getPlayer().getUniqueId());
            if (user != null) {
                String lpPrefix = user.getCachedData().getMetaData().getPrefix();
                if (lpPrefix != null) {
                    net.kyori.adventure.text.Component prefixComp = fr.gens.core.utils.PlaceholderUtils.parseToComponent(lpPrefix);
                    String legacyPrefix = net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.builder()
                        .character('§')
                        .hexColors()
                        .build()
                        .serialize(prefixComp);
                    prefix = legacyPrefix + " ";
                }
            }
        } catch (Exception e) {
            if (event.getPlayer().hasPermission("genscore.admin")) {
                prefix = "§c[Admin] ";
            }
        }
        
        // Ajout du tag de guilde si le joueur en a une
        String guildTag = "";
        fr.gens.core.modules.teams.TeamData team = plugin.getTeamManager().getPlayerTeam(event.getPlayer().getUniqueId());
        if (team != null) {
            guildTag = "§e[" + team.getName() + "] ";
        }
        
        event.setFormat(prefix + guildTag + "§f%1$s §8» §7%2$s");
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (!enabled) return;
        
        if (!event.getPlayer().hasPlayedBefore()) {
            event.setJoinMessage(
                " \n" +
                "§e§l⭐ §6§lBIENVENUE §e§l⭐\n" +
                "§fBienvenue à §a" + event.getPlayer().getName() + " §fsur le serveur Gens !\n" +
                "§7C'est sa toute première connexion !\n" +
                " "
            );
        } else {
            event.setJoinMessage("§8[§a+§8] §7" + event.getPlayer().getName() + " §aa rejoint le serveur.");
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        if (!enabled) return;
        event.setQuitMessage("§8[§c-§8] §7" + event.getPlayer().getName() + " §ca quitté le serveur.");
    }
}
