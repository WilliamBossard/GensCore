package fr.gens.core.modules;

import fr.gens.core.CorePlugin;
import org.bukkit.Bukkit;
import org.bukkit.event.Listener;


public class BlueMapModule implements Module, Listener {

    private final CorePlugin plugin;
    private boolean enabled = false;

    public BlueMapModule(CorePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "bluemap";
    }

    @Override
    public String getDescription() {
        return "Gère la carte en ligne (rendu en temps réel).";
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void enable() {
        enabled = true;
        if (!plugin.isEnabled()) return;
        
        if (Bukkit.getPluginManager().isPluginEnabled("BlueMap")) {
            plugin.getFoliaLib().getScheduler().runLater((t2) -> {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "bluemap start");
                plugin.getLangManager().sendConsoleMessage("bluemapmodule.log_1");
            }, 20L);
        }
    }

    @Override
    public void disable() {
        org.bukkit.event.HandlerList.unregisterAll(this);
        enabled = false;
        if (!plugin.isEnabled()) return;
        
        if (Bukkit.getPluginManager().isPluginEnabled("BlueMap")) {
            plugin.getFoliaLib().getScheduler().runLater((t2) -> {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "bluemap stop");
                plugin.getLangManager().sendConsoleMessage("bluemapmodule.log_2");
            }, 20L);
        }
    }
}





