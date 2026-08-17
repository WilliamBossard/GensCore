package fr.gens.core.modules;

import fr.gens.core.CorePlugin;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class GuiModule implements Module, Listener {

    private final CorePlugin plugin;
    private boolean enabled = false;

    public GuiModule(CorePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "GUI API";
    }

    @Override
    public String getDescription() {
        return "Moteur interne pour la création d'inventaires interactifs.";
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void enable() {
        enabled = true;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        plugin.getLangManager().sendConsoleMessage("guimodule.log_1");
    }

    @Override
    public void disable() {
        enabled = false;
        HandlerList.unregisterAll(this);
        plugin.getLangManager().sendConsoleMessage("guimodule.log_2");
    }

    // Intercepte les clics dans nos menus custom pour empêcher les joueurs de voler les items
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!enabled) return;

        Inventory inv = event.getClickedInventory();
        if (inv != null && inv.getHolder() instanceof GensGuiHolder) {
            event.setCancelled(true); // Empêche de prendre l'item
            
            // On délègue l'action au holder spécifique
            ((GensGuiHolder) inv.getHolder()).onClick(event);
        }
    }

    // Interface pour identifier nos menus personnalisés
    public interface GensGuiHolder extends InventoryHolder {
        void onClick(InventoryClickEvent event);
    }
}
