package fr.gens.core.modules.motd;

import fr.gens.core.CorePlugin;
import fr.gens.core.modules.Module;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerListPingEvent;

public class MotdModule implements Module, Listener {

    private final CorePlugin plugin;
    private boolean enabled = false;

    public MotdModule(CorePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "MOTD";
    }

    @Override
    public String getDescription() {
        return "Gère le message de bienvenue (MOTD) affiché dans la liste des serveurs.";
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void enable() {
        enabled = true;
        if (!plugin.getConfig().contains("motd.line1")) {
            plugin.getConfig().set("motd.line1", "<dark_aqua><bold>Le Serveur Des Gens Bien");
            plugin.getConfig().set("motd.line2", "<gray><bold>>> <yellow>Saison 4 <gray><bold>- <aqua>discord.gg/gensbien");
            plugin.saveConfig();
        }
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        plugin.getLangManager().sendConsoleMessage("motdmodule.log_1");
    }

    @Override
    public void disable() {
        enabled = false;
        plugin.getLangManager().sendConsoleMessage("motdmodule.log_2");
    }

    @EventHandler
    public void onServerPing(ServerListPingEvent event) {
        if (!enabled) return;

        String line1 = plugin.getConfig().getString("motd.line1", "<dark_aqua><bold>Le Serveur Des Gens Bien");
        String line2 = plugin.getConfig().getString("motd.line2", "");

        String fullMotdStr = line1 + "\n" + line2;

        try {
            // Paper environment (1.16+) supports Component MOTD via PaperServerListPingEvent
            if (event instanceof com.destroystokyo.paper.event.server.PaperServerListPingEvent) {
                net.kyori.adventure.text.Component motdComp = fr.gens.core.utils.PlaceholderUtils.parseToComponent(fullMotdStr);
                ((com.destroystokyo.paper.event.server.PaperServerListPingEvent) event).motd(motdComp);
                return;
            }
        } catch (Throwable ignored) {}

        // Fallback for non-Paper environments or older versions
        String fullMotd = fullMotdStr;
        event.setMotd(fullMotd);
    }
}
