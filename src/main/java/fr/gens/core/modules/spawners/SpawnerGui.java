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
        Inventory inv = Bukkit.createInventory(null, 45, net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize("<dark_gray>Gestion: <gold>" + data.getType()));

        // Vitres de dÃƒÆ’Ã‚Â©coration
        ItemStack glass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta glassMeta = glass.getItemMeta();
        if (glassMeta != null) {
            glassMeta.displayName(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize(" "));
            glass.setItemMeta(glassMeta);
        }
        for (int i = 0; i < 45; i++) {
            inv.setItem(i, glass);
        }

        // Info Spawner (Slot 4)
        ItemStack info = new ItemStack(Material.SPAWNER);
        ItemMeta infoMeta = info.getItemMeta();
        if (infoMeta != null) {
            infoMeta.displayName(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize("<gold><bold>" + data.getType()));
            List<String> lore = new ArrayList<>();
            lore.add("<gray>Stack: <white>" + data.getStackCount());
            lore.add("<gray>Dernier accÃƒÆ’Ã‚Â¨s: <white>" + data.getLastInteractedPlayer());
            infoMeta.lore(java.util.Optional.ofNullable(lore).orElse(java.util.Collections.emptyList()).stream().map(s -> net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize((String)s)).collect(java.util.stream.Collectors.toList()));
            info.setItemMeta(infoMeta);
        }
        inv.setItem(4, info);

        // RÃƒÆ’Ã‚Â©cupÃƒÆ’Ã‚Â©rer XP (Slot 20)
        ItemStack xpBtn = new ItemStack(Material.EXPERIENCE_BOTTLE);
        ItemMeta xpMeta = xpBtn.getItemMeta();
        if (xpMeta != null) {
            xpMeta.displayName(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize("<green><bold>RÃƒÆ’Ã‚Â©cupÃƒÆ’Ã‚Â©rer l'XP"));
            List<String> lore = new ArrayList<>();
            lore.add("<gray>XP Actuelle: <green>" + data.getStoredExp() + " / " + moduleInstance.getSpawnerManager().getMaxStorageExp(data.getExpLevel()));
            lore.add("");
            lore.add("<yellow>Cliquez pour rÃƒÆ’Ã‚Â©cupÃƒÆ’Ã‚Â©rer");
            xpMeta.lore(java.util.Optional.ofNullable(lore).orElse(java.util.Collections.emptyList()).stream().map(s -> net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize((String)s)).collect(java.util.stream.Collectors.toList()));
            xpBtn.setItemMeta(xpMeta);
        }
        inv.setItem(20, xpBtn);

        // Ouvrir Stockage d'Items (Slot 24)
        ItemStack itemsBtn = new ItemStack(Material.CHEST);
        ItemMeta itemsMeta = itemsBtn.getItemMeta();
        if (itemsMeta != null) {
            itemsMeta.displayName(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize("<gold><bold>Stockage d'objets"));
            int totalItems = data.getStoredItems().values().stream().mapToInt(Integer::intValue).sum();
            List<String> lore = new ArrayList<>();
            lore.add("<gray>Items Actuels: <yellow>" + totalItems + " / " + moduleInstance.getSpawnerManager().getMaxStorageItems(data.getStorageLevel()));
            lore.add("");
            lore.add("<yellow>Cliquez pour ouvrir le coffre");
            itemsMeta.lore(java.util.Optional.ofNullable(lore).orElse(java.util.Collections.emptyList()).stream().map(s -> net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize((String)s)).collect(java.util.stream.Collectors.toList()));
            itemsBtn.setItemMeta(itemsMeta);
        }
        inv.setItem(24, itemsBtn);

        // Upgrades
        EconomyModule eco = moduleInstance != null ? (EconomyModule) moduleInstance.getPlugin().getModuleManager().getModule("economy") : null;
        boolean ecoEnabled = eco != null && eco.isEnabled();
        
        inv.setItem(38, createUpgradeItem("CapacitÃƒÆ’Ã‚Â© XP", Material.EMERALD, data.getExpLevel(), ecoEnabled));
        inv.setItem(40, createUpgradeItem("Vitesse", Material.SUGAR, data.getSpeedLevel(), ecoEnabled));
        inv.setItem(42, createUpgradeItem("CapacitÃƒÆ’Ã‚Â© Stockage", Material.DIAMOND, data.getStorageLevel(), ecoEnabled));

        openGuis.put(player, data);
        player.openInventory(inv);
    }
    
    private static ItemStack createUpgradeItem(String name, Material mat, int currentLevel, boolean ecoEnabled) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize("<aqua><bold>AmÃƒÆ’Ã‚Â©liorer " + name));
            List<String> lore = new ArrayList<>();
            lore.add("<gray>Niveau actuel: <white>" + currentLevel);
            if (currentLevel >= 10) {
                lore.add("<red><bold>NIVEAU MAX !");
            } else {
                lore.add("<gray>Niveau suivant: <white>" + (currentLevel + 1));
                lore.add("");
                if (ecoEnabled) {
                    lore.add("<gray>CoÃƒÆ’Ã‚Â»t: <gold>" + getUpgradeCost(currentLevel) + "$");
                } else {
                    lore.add("<gray>CoÃƒÆ’Ã‚Â»t: <green>" + getUpgradeXpCost(currentLevel) + " Niveaux");
                }
                lore.add("<yellow>Cliquez pour amÃƒÆ’Ã‚Â©liorer !");
            }
            meta.lore(java.util.Optional.ofNullable(lore).orElse(java.util.Collections.emptyList()).stream().map(s -> net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize((String)s)).collect(java.util.stream.Collectors.toList()));
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
        if (!net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(event.getView().title()).startsWith("Gestion: ")) return;
        
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
                player.sendMessage("<green>Vous avez rÃƒÆ’Ã‚Â©cupÃƒÆ’Ã‚Â©rÃƒÆ’Ã‚Â© " + data.getStoredExp() + " points d'expÃƒÆ’Ã‚Â©rience !");
                player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
                data.setStoredExp(0);
                moduleInstance.getSpawnerManager().updateHologram(data);
                Bukkit.getScheduler().runTask(moduleInstance.getPlugin(), () -> openGui(player, data)); // refresh diffÃƒÆ’Ã‚Â©rÃƒÆ’Ã‚Â©
            } else {
                moduleInstance.getPlugin().getLangManager().sendMessage(player, "spawnergui.msg_1");
            }
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
            moduleInstance.getPlugin().getLangManager().sendMessage(player, "spawnergui.msg_2");
            return;
        }
        
        if (moduleInstance != null && !moduleInstance.isEnabled()) {
            moduleInstance.getPlugin().getLangManager().sendMessage(player, "spawnergui.msg_3");
            return;
        }
        
        EconomyModule eco = (EconomyModule) moduleInstance.getPlugin().getModuleManager().getModule("economy");
        boolean ecoEnabled = eco != null && eco.isEnabled();
        
        if (ecoEnabled) {
            double cost = getUpgradeCost(currentLevel);
            if (eco != null && eco.getBalance(player.getUniqueId()) >= cost) {
                eco.takeMoney(player.getUniqueId(), cost);
                applyUpgrade(player, data, type, currentLevel);
                player.sendMessage("<green>AmÃƒÆ’Ã‚Â©lioration effectuÃƒÆ’Ã‚Â©e pour " + cost + "$ !");
            } else {
                player.sendMessage("<red>Vous n'avez pas assez d'argent ! (" + cost + "$ nÃƒÆ’Ã‚Â©cessaires)");
            }
        } else {
            int xpCost = getUpgradeXpCost(currentLevel);
            if (player.getLevel() >= xpCost) {
                player.setLevel(player.getLevel() - xpCost);
                applyUpgrade(player, data, type, currentLevel);
                player.sendMessage("<green>AmÃƒÆ’Ã‚Â©lioration effectuÃƒÆ’Ã‚Â©e pour " + xpCost + " niveaux !");
            } else {
                player.sendMessage("<red>Vous n'avez pas assez de niveaux ! (" + xpCost + " nÃƒÆ’Ã‚Â©cessaires)");
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
        Bukkit.getScheduler().runTask(moduleInstance.getPlugin(), () -> openGui(player, data)); // refresh diffÃƒÆ’Ã‚Â©rÃƒÆ’Ã‚Â©
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(event.getView().title()).startsWith("Gestion: ")) return;
        if (event.getWhoClicked() instanceof Player && openGuis.containsKey((Player) event.getWhoClicked())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(event.getView().title()).startsWith("Gestion: ")) return;
        if (event.getPlayer() instanceof Player) {
            Player p = (Player) event.getPlayer();
            // DÃƒÆ’Ã‚Â©lai d'un tick pour voir si le joueur ouvre un autre inventaire de gestion (ex: refresh)
            Bukkit.getScheduler().runTask(moduleInstance.getPlugin(), () -> {
                if (!net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(p.getOpenInventory().title()).startsWith("Gestion: ")) {
                    openGuis.remove(p);
                }
            });
        }
    }
}

