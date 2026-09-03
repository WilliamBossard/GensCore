package fr.gens.core.modules.spawners;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.UUID;

public class SpawnerLootGui implements Listener {

    private static final Map<UUID, SpawnerData> openGuis = new ConcurrentHashMap<>();
    private static final Map<UUID, Map<String, Integer>> guiSnapshots = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> playerPages = new ConcurrentHashMap<>();
    private static SpawnerModule moduleInstance;

    public static void setModule(SpawnerModule module) {
        moduleInstance = module;
    }

    public static void openGui(Player player, SpawnerData data, SpawnerModule module) {
        openGui(player, data, module, 0);
    }

    public static void openGui(Player player, SpawnerData data, SpawnerModule module, int page) {
        setModule(module);
        Inventory inv = Bukkit.createInventory(null, 54, fr.gens.core.utils.PlaceholderUtils.parseToComponent("<dark_gray>Loot: <gold>" + data.getType() + " <gray>(Page " + (page + 1) + ")"));

        int slot = 0;
        int startIndex = page * 45;
        int currentIndex = 0;
        boolean hasNextPage = false;
        
        Map<String, Integer> snapshot = new HashMap<>();
        
        for (Map.Entry<String, Integer> entry : data.getStoredItems().entrySet()) {
            Material mat = Material.getMaterial(entry.getKey());
            if (mat != null) {
                int amount = entry.getValue();
                while (amount > 0) {
                    int toAdd = Math.min(amount, mat.getMaxStackSize());
                    
                    if (currentIndex >= startIndex && slot < 45) {
                        inv.setItem(slot, new ItemStack(mat, toAdd));
                        snapshot.put(entry.getKey(), snapshot.getOrDefault(entry.getKey(), 0) + toAdd);
                        slot++;
                    } else if (currentIndex >= startIndex + 45) {
                        hasNextPage = true;
                    }
                    
                    amount -= toAdd;
                    currentIndex++;
                }
            }
        }

        // Navigation Bar
        for (int i = 45; i < 54; i++) {
            inv.setItem(i, createGuiItem(Material.BLACK_STAINED_GLASS_PANE, " "));
        }
        
        if (page > 0) {
            inv.setItem(45, createGuiItem(Material.ARROW, "<red>Page Précédente"));
        }
        
        if (hasNextPage) {
            inv.setItem(53, createGuiItem(Material.ARROW, "<green>Page Suivante"));
        }
        
        inv.setItem(49, createGuiItem(Material.BARRIER, "<red>Retour au Menu Principal"));

        guiSnapshots.put(player.getUniqueId(), snapshot);
        openGuis.put(player.getUniqueId(), data);
        playerPages.put(player.getUniqueId(), page);
        
        player.openInventory(inv);
    }
    
    private static ItemStack createGuiItem(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(fr.gens.core.utils.PlaceholderUtils.parseToComponent(name));
            item.setItemMeta(meta);
        }
        return item;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(event.getView().title()).startsWith("Loot: ")) return;
        
        if (!openGuis.containsKey(player.getUniqueId())) return;
        SpawnerData data = openGuis.get(player.getUniqueId());
        if (data == null) return;

        if (event.getClickedInventory() == null) return;
        
        if (event.getClickedInventory().equals(event.getView().getTopInventory())) {
            int slot = event.getSlot();
            
            // Si clic dans la barre de navigation
            if (slot >= 45 && slot < 54) {
                event.setCancelled(true);
                int page = playerPages.getOrDefault(player.getUniqueId(), 0);
                
                if (slot == 45 && page > 0) {
                    syncItems(player, event.getView().getTopInventory(), data);
                    moduleInstance.getPlugin().getFoliaLib().getScheduler().runAtEntity(player, (wrappedTask) -> openGui(player, data, moduleInstance, page - 1));
                } else if (slot == 53 && event.getCurrentItem() != null && event.getCurrentItem().getType() == Material.ARROW) {
                    syncItems(player, event.getView().getTopInventory(), data);
                    moduleInstance.getPlugin().getFoliaLib().getScheduler().runAtEntity(player, (wrappedTask) -> openGui(player, data, moduleInstance, page + 1));
                } else if (slot == 49) {
                    player.closeInventory();
                }
                return;
            }
            
            // Autoriser à prendre, mais interdire de poser
            if (event.getAction() == InventoryAction.PLACE_ALL || 
                event.getAction() == InventoryAction.PLACE_ONE || 
                event.getAction() == InventoryAction.PLACE_SOME ||
                event.getAction() == InventoryAction.SWAP_WITH_CURSOR) {
                event.setCancelled(true);
                return;
            }
            
            moduleInstance.getPlugin().getFoliaLib().getScheduler().runAtEntity(player, (wrappedTask) -> syncItems(player, event.getView().getTopInventory(), data));
        } else {
            if (event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(event.getView().title()).startsWith("Loot: ")) return;
        if (event.getWhoClicked() instanceof Player && openGuis.containsKey(((Player) event.getWhoClicked()).getUniqueId())) {
            for (int slot : event.getRawSlots()) {
                if (slot < event.getView().getTopInventory().getSize()) {
                    event.setCancelled(true);
                    return;
                }
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(event.getView().title()).startsWith("Loot: ")) return;
        if (event.getPlayer() instanceof Player player) {
            if (openGuis.containsKey(player.getUniqueId())) {
                SpawnerData data = openGuis.get(player.getUniqueId());
                syncItems(player, event.getInventory(), data);
                
                // Si on ferme complètement (pas juste un changement de page)
                moduleInstance.getPlugin().getFoliaLib().getScheduler().runLater((wrappedTask) -> {
                    if (!net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(player.getOpenInventory().title()).startsWith("Loot: ")) {
                        openGuis.remove(player.getUniqueId());
                        guiSnapshots.remove(player.getUniqueId());
                        playerPages.remove(player.getUniqueId());
                        
                        if (data.isLootChest()) {
                            // Check if empty
                            int totalItems = data.getStoredItems().values().stream().mapToInt(Integer::intValue).sum();
                            if (totalItems == 0 && data.getStoredExp() == 0) {
                                // Delete the chest
                                data.getLocation().getBlock().setType(Material.AIR);
                                moduleInstance.removeSpawner(data.getLocation());
                                moduleInstance.getPlugin().getLangManager().sendMessage(player, "spawnerlootgui.msg_1");
                            }
                        } else {
                            SpawnerGui.openGui(player, data);
                        }
                    }
                }, 1L);
            }
        }
    }
    
    private void syncItems(Player player, Inventory inv, SpawnerData data) {
        if (!guiSnapshots.containsKey(player.getUniqueId())) return;
        Map<String, Integer> previousSnapshot = guiSnapshots.get(player.getUniqueId());
        
        Map<String, Integer> currentSnapshot = new HashMap<>();
        for (int i = 0; i < 45; i++) {
            ItemStack item = inv.getItem(i);
            if (item != null && item.getType() != Material.AIR) {
                String matStr = item.getType().name();
                currentSnapshot.put(matStr, currentSnapshot.getOrDefault(matStr, 0) + item.getAmount());
            }
        }
        
        for (Map.Entry<String, Integer> prev : previousSnapshot.entrySet()) {
            String mat = prev.getKey();
            int prevAmount = prev.getValue();
            int currAmount = currentSnapshot.getOrDefault(mat, 0);
            
            if (prevAmount > currAmount) {
                int taken = prevAmount - currAmount;
                data.removeItem(mat, taken);
            }
        }
        
        guiSnapshots.put(player.getUniqueId(), currentSnapshot);
        moduleInstance.getSpawnerManager().updateHologram(data);
    }
}




