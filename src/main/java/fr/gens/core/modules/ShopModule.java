package fr.gens.core.modules;

import fr.gens.core.CorePlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class ShopModule implements Module, Listener {

    private final CorePlugin plugin;
    private boolean enabled = false;

    public ShopModule(CorePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "Shop";
    }

    @Override
    public String getDescription() {
        return "Gère les boutiques des joueurs et l'économie interne.";
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void enable() {
        enabled = true;
        // Enregistrer les événements de ce module
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        plugin.getLangManager().sendConsoleMessage("shopmodule.log_1");
    }

    @Override
    public void disable() {
        enabled = false;
        // Désenregistrer les événements de ce module pour qu'il s'arrête instantanément
        HandlerList.unregisterAll(this);
        plugin.getLangManager().sendConsoleMessage("shopmodule.log_2");
    }

    // Exemple d'événement qui ne fonctionne que si le module est actif
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!enabled) return; // Sécurité supplémentaire
        event.getPlayer().sendMessage("<green>Le Shop est actuellement ouvert !");
    }
}
