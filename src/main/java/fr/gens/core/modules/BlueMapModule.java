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
        if (!plugin.getConfig().getBoolean("bluemap.enabled", true)) {
            enabled = false;
            return;
        }
        
        enabled = true;
        // Ne rien faire si le serveur s'arrête
        if (!plugin.isEnabled()) return;
        
        // Relance le rendu de la carte
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "bluemap start");
            plugin.getLangManager().sendConsoleMessage("bluemapmodule.log_1");
        }, 20L);
    }

    @Override
    public void disable() {
        enabled = false;
        // Ne rien faire si le serveur s'arrête
        if (!plugin.isEnabled()) return;
        
        // Met en pause le rendu pour économiser les performances
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "bluemap stop");
            plugin.getLangManager().sendConsoleMessage("bluemapmodule.log_2");
        }, 20L);
    }
}
