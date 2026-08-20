package fr.gens.core.modules;

import fr.gens.core.CorePlugin;
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
        return "Moteur interne pour la crÃƒÆ’Ã‚Â©ation d'inventaires interactifs.";
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

    // Intercepte les clics dans nos menus custom pour empÃƒÆ’Ã‚Âªcher les joueurs de voler les items
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!enabled) return;

        Inventory inv = event.getClickedInventory();
        if (inv != null && inv.getHolder() instanceof GensGuiHolder) {
            event.setCancelled(true); // EmpÃƒÆ’Ã‚Âªche de prendre l'item
            
            // On dÃƒÆ’Ã‚Â©lÃƒÆ’Ã‚Â¨gue l'action au holder spÃƒÆ’Ã‚Â©cifique
            ((GensGuiHolder) inv.getHolder()).onClick(event);
        }
    }

    // Interface pour identifier nos menus personnalisÃƒÆ’Ã‚Â©s
    public interface GensGuiHolder extends InventoryHolder {
        void onClick(InventoryClickEvent event);
    }
}

