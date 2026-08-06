package fr.gens.core.modules.spawners;

import fr.gens.core.modules.EconomyModule;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SpawnerGui implements Listener {

    private static final Map<Player, SpawnerData> openGuis = new ConcurrentHashMap<>();
    private static SpawnerModule moduleInstance;

    public static void setModule(SpawnerModule module) {
        moduleInstance = module;
    }

    public static void openGui(Player player, SpawnerData data) {
        Inventory inv = Bukkit.createInventory(null, 45, "§8Gestion: §6" + data.getType());

        // Vitres de décoration
        ItemStack glass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta glassMeta = glass.getItemMeta();
        if (glassMeta != null) {
            glassMeta.setDisplayName(" ");
            glass.setItemMeta(glassMeta);
        }
        for (int i = 0; i < 45; i++) {
            inv.setItem(i, glass);
        }

        // Info Spawner (Slot 4)
        ItemStack info = new ItemStack(Material.SPAWNER);
        ItemMeta infoMeta = info.getItemMeta();
        if (infoMeta != null) {
            infoMeta.setDisplayName("§6§l" + data.getType());
            List<String> lore = new ArrayList<>();
            lore.add("§7Stack: §f" + data.getStackCount());
            lore.add("§7Dernier accès: §f" + data.getLastInteractedPlayer());
            infoMeta.setLore(lore);
            info.setItemMeta(infoMeta);
        }
        inv.setItem(4, info);

        // Récupérer XP (Slot 20)
        ItemStack xpBtn = new ItemStack(Material.EXPERIENCE_BOTTLE);
        ItemMeta xpMeta = xpBtn.getItemMeta();
        if (xpMeta != null) {
            xpMeta.setDisplayName("§a§lRécupérer l'XP");
            List<String> lore = new ArrayList<>();
            lore.add("§7XP Actuelle: §a" + data.getStoredExp() + " / " + moduleInstance.getSpawnerManager().getMaxStorageExp(data.getExpLevel()));
            lore.add("");
            lore.add("§eCliquez pour récupérer");
            xpMeta.setLore(lore);
            xpBtn.setItemMeta(xpMeta);
        }
        inv.setItem(20, xpBtn);

        // Ouvrir Stockage d'Items (Slot 24)
        ItemStack itemsBtn = new ItemStack(Material.CHEST);
        ItemMeta itemsMeta = itemsBtn.getItemMeta();
        if (itemsMeta != null) {
            itemsMeta.setDisplayName("§6§lStockage d'objets");
            int totalItems = data.getStoredItems().values().stream().mapToInt(Integer::intValue).sum();
            List<String> lore = new ArrayList<>();
            lore.add("§7Items Actuels: §e" + totalItems + " / " + moduleInstance.getSpawnerManager().getMaxStorageItems(data.getStorageLevel()));
            lore.add("");
            lore.add("§eCliquez pour ouvrir le coffre");
            itemsMeta.setLore(lore);
            itemsBtn.setItemMeta(itemsMeta);
        }
        inv.setItem(24, itemsBtn);

        // Upgrades
        EconomyModule eco = moduleInstance != null ? (EconomyModule) moduleInstance.getPlugin().getModuleManager().getModule("economy") : null;
        boolean ecoEnabled = eco != null && eco.isEnabled();
        
        inv.setItem(38, createUpgradeItem("Capacité XP", Material.EMERALD, data.getExpLevel(), ecoEnabled));
        inv.setItem(40, createUpgradeItem("Vitesse", Material.SUGAR, data.getSpeedLevel(), ecoEnabled));
        inv.setItem(42, createUpgradeItem("Capacité Stockage", Material.DIAMOND, data.getStorageLevel(), ecoEnabled));

        openGuis.put(player, data);
        player.openInventory(inv);
    }
    
    private static ItemStack createUpgradeItem(String name, Material mat, int currentLevel, boolean ecoEnabled) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§b§lAméliorer " + name);
            List<String> lore = new ArrayList<>();
            lore.add("§7Niveau actuel: §f" + currentLevel);
            if (currentLevel >= 10) {
                lore.add("§c§lNIVEAU MAX !");
            } else {
                lore.add("§7Niveau suivant: §f" + (currentLevel + 1));
                lore.add("");
                if (ecoEnabled) {
                    lore.add("§7Coût: §6" + getUpgradeCost(currentLevel) + "$");
                } else {
                    lore.add("§7Coût: §a" + getUpgradeXpCost(currentLevel) + " Niveaux");
                }
                lore.add("§eCliquez pour améliorer !");
            }
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }
    
    public static double getUpgradeCost(int currentLevel) {
        if (moduleInstance != null) {
            double base = moduleInstance.getPlugin().getConfig().getDouble("spawners.upgrade-base-cost", 1000.0);
            return base * Math.pow(2.5, currentLevel);
        }
        return 1000.0 * Math.pow(2.5, currentLevel);
    }
    
    public static int getUpgradeXpCost(int currentLevel) {
        return (currentLevel + 1) * 10;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        if (!event.getView().getTitle().startsWith("§8Gestion: ")) return;
        
        // SECURITE: On annule TOUS les clics quand on est dans ce menu
        event.setCancelled(true);
        
        Player player = (Player) event.getWhoClicked();
        if (!openGuis.containsKey(player)) return;
        
        SpawnerData data = openGuis.get(player);
        if (data == null) return;
        
        // Si le clic ne vient pas de l'inventaire custom (par ex: dans l'inventaire du joueur)
        if (event.getClickedInventory() == null || event.getClickedInventory() != event.getView().getTopInventory()) {
            return;
        }

        int slot = event.getRawSlot();
        
        if (slot == 20) {
            // XP
            if (data.getStoredExp() > 0) {
                player.giveExp(data.getStoredExp());
                player.sendMessage("§aVous avez récupéré " + data.getStoredExp() + " points d'expérience !");
                player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
                data.setStoredExp(0);
                moduleInstance.getSpawnerManager().updateHologram(data);
                Bukkit.getScheduler().runTask(moduleInstance.getPlugin(), () -> openGui(player, data)); // refresh différé
            } else {
                player.sendMessage("§cAucune XP stockée !");
            }
        } else if (slot == 24) {
            // Coffre Items
            Bukkit.getScheduler().runTask(moduleInstance.getPlugin(), () -> SpawnerLootGui.openGui(player, data, moduleInstance));
        } else if (slot == 38) {
            handleUpgrade(player, data, "exp");
        } else if (slot == 40) {
            handleUpgrade(player, data, "speed");
        } else if (slot == 42) {
            handleUpgrade(player, data, "storage");
        }
    }
    
    private void handleUpgrade(Player player, SpawnerData data, String type) {
        int currentLevel = 0;
        switch (type) {
            case "exp" -> currentLevel = data.getExpLevel();
            case "speed" -> currentLevel = data.getSpeedLevel();
            case "storage" -> currentLevel = data.getStorageLevel();
        }
        
        if (currentLevel >= 10) {
            player.sendMessage("§cCe spawner est déjà au niveau maximum pour cette amélioration.");
            return;
        }
        
        if (moduleInstance != null && !moduleInstance.isEnabled()) {
            player.sendMessage("§cLe système de spawners est actuellement désactivé. Vous ne pouvez pas acheter d'améliorations.");
            return;
        }
        
        EconomyModule eco = (EconomyModule) moduleInstance.getPlugin().getModuleManager().getModule("economy");
        boolean ecoEnabled = eco != null && eco.isEnabled();
        
        if (ecoEnabled) {
            double cost = getUpgradeCost(currentLevel);
            if (eco.getBalance(player.getUniqueId()) >= cost) {
                eco.takeMoney(player.getUniqueId(), cost);
                applyUpgrade(player, data, type, currentLevel);
                player.sendMessage("§aAmélioration effectuée pour " + cost + "$ !");
            } else {
                player.sendMessage("§cVous n'avez pas assez d'argent ! (" + cost + "$ nécessaires)");
            }
        } else {
            int xpCost = getUpgradeXpCost(currentLevel);
            if (player.getLevel() >= xpCost) {
                player.setLevel(player.getLevel() - xpCost);
                applyUpgrade(player, data, type, currentLevel);
                player.sendMessage("§aAmélioration effectuée pour " + xpCost + " niveaux !");
            } else {
                player.sendMessage("§cVous n'avez pas assez de niveaux ! (" + xpCost + " nécessaires)");
            }
        }
    }
    
    private void applyUpgrade(Player player, SpawnerData data, String type, int currentLevel) {
        switch (type) {
            case "exp" -> data.setExpLevel(currentLevel + 1);
            case "speed" -> data.setSpeedLevel(currentLevel + 1);
            case "storage" -> data.setStorageLevel(currentLevel + 1);
        }
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
        moduleInstance.getSpawnerManager().updateHologram(data);
        Bukkit.getScheduler().runTask(moduleInstance.getPlugin(), () -> openGui(player, data)); // refresh différé
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!event.getView().getTitle().startsWith("§8Gestion: ")) return;
        if (event.getWhoClicked() instanceof Player && openGuis.containsKey((Player) event.getWhoClicked())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!event.getView().getTitle().startsWith("§8Gestion: ")) return;
        if (event.getPlayer() instanceof Player) {
            Player p = (Player) event.getPlayer();
            // Délai d'un tick pour voir si le joueur ouvre un autre inventaire de gestion (ex: refresh)
            Bukkit.getScheduler().runTask(moduleInstance.getPlugin(), () -> {
                if (!p.getOpenInventory().getTitle().startsWith("§8Gestion: ")) {
                    openGuis.remove(p);
                }
            });
        }
    }
}
