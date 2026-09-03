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

        Inventory topInv = event.getView().getTopInventory();
        if (topInv.getHolder() instanceof GensGuiHolder) {
            Inventory clickedInv = event.getClickedInventory();
            if (clickedInv == null) return;

            if (!clickedInv.equals(topInv)) {
                if (event.getAction() == org.bukkit.event.inventory.InventoryAction.MOVE_TO_OTHER_INVENTORY) {
                    event.setCancelled(true);
                }
                return;
            }

            event.setCancelled(true); // Empêche de prendre l'item
            
            // On délègue l'action au holder spécifique
            ((GensGuiHolder) topInv.getHolder()).onClick(event);
        }
    }

    // Intercepte les glissements (drag) d'items pour empêcher le remplacement des objets du menu
    @EventHandler
    public void onInventoryDrag(org.bukkit.event.inventory.InventoryDragEvent event) {
        if (!enabled) return;

        Inventory topInv = event.getView().getTopInventory();
        if (topInv.getHolder() instanceof GensGuiHolder) {
            for (int rawSlot : event.getRawSlots()) {
                if (rawSlot < topInv.getSize()) {
                    event.setCancelled(true);
                    return;
                }
            }
        }
    }

    // Interface pour identifier nos menus personnalisés
    public interface GensGuiHolder extends InventoryHolder {
        void onClick(InventoryClickEvent event);
    }
}



