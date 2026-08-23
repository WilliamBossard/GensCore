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
        return "GÃƒÆ’Ã‚Â¨re la carte en ligne (rendu en temps rÃƒÆ’Ã‚Â©el).";
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void enable() {
        enabled = true;
        // Ne rien faire si le serveur s'arrÃƒÆ’Ã‚Âªte
        if (!plugin.isEnabled()) return;
        
        // Relance le rendu de la carte
        plugin.getFoliaLib().getImpl().runLater((t2) -> {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "bluemap start");
            plugin.getLangManager().sendConsoleMessage("bluemapmodule.log_1");
        }, 20L);
    }

    @Override
    public void disable() {
        org.bukkit.event.HandlerList.unregisterAll(this);
        enabled = false;
        // Ne rien faire si le serveur s'arrÃƒÆ’Ã‚Âªte
        if (!plugin.isEnabled()) return;
        
        // Met en pause le rendu pour ÃƒÆ’Ã‚Â©conomiser les performances
        plugin.getFoliaLib().getImpl().runLater((t2) -> {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "bluemap stop");
            plugin.getLangManager().sendConsoleMessage("bluemapmodule.log_2");
        }, 20L);
    }
}


