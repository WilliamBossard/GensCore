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
import java.util.UUID;


public class SpawnerGui implements Listener {

    private static final Map<UUID, SpawnerData> openGuis = new ConcurrentHashMap<>();
    private static SpawnerModule moduleInstance;

    public static void setModule(SpawnerModule module) {
        moduleInstance = module;
    }

    public static void openGui(Player player, SpawnerData data) {
        if (fr.gens.core.utils.FloodgateUtil.isBedrockPlayer(player.getUniqueId())) {
            openBedrockGui(player, data);
            return;
        }

        Inventory inv = Bukkit.createInventory(null, 45, fr.gens.core.utils.PlaceholderUtils.parseToComponent("<dark_gray>Gestion: <gold>" + data.getType()));

        // Vitres de décoration
        ItemStack glass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta glassMeta = glass.getItemMeta();
        if (glassMeta != null) {
            glassMeta.displayName(fr.gens.core.utils.PlaceholderUtils.parseToComponent(" "));
            glass.setItemMeta(glassMeta);
        }
        for (int i = 0; i < 45; i++) {
            inv.setItem(i, glass);
        }

        // Info Spawner (Slot 4)
        ItemStack info = new ItemStack(Material.SPAWNER);
        ItemMeta infoMeta = info.getItemMeta();
        if (infoMeta != null) {
            infoMeta.displayName(fr.gens.core.utils.PlaceholderUtils.parseToComponent("<gold><bold>" + data.getType()));
            List<String> lore = new ArrayList<>();
            lore.add("<gray>Stack: <white>" + data.getStackCount());
            lore.add("<gray>Dernier accès: <white>" + data.getLastInteractedPlayer());
            infoMeta.lore(java.util.Optional.ofNullable(lore).orElse(java.util.Collections.emptyList()).stream().map(s -> fr.gens.core.utils.PlaceholderUtils.parseToComponent((String)s)).collect(java.util.stream.Collectors.toList()));
            info.setItemMeta(infoMeta);
        }
        inv.setItem(4, info);

        // Récupérer XP (Slot 20)
        ItemStack xpBtn = new ItemStack(Material.EXPERIENCE_BOTTLE);
        ItemMeta xpMeta = xpBtn.getItemMeta();
        if (xpMeta != null) {
            xpMeta.displayName(fr.gens.core.utils.PlaceholderUtils.parseToComponent("<green><bold>Récupérer l'XP"));
            List<String> lore = new ArrayList<>();
            lore.add("<gray>XP Actuelle: <green>" + data.getStoredExp() + " / " + moduleInstance.getSpawnerManager().getMaxStorageExp(data.getExpLevel()));
            lore.add("");
            lore.add("<yellow>Cliquez pour récupérer");
            xpMeta.lore(java.util.Optional.ofNullable(lore).orElse(java.util.Collections.emptyList()).stream().map(s -> fr.gens.core.utils.PlaceholderUtils.parseToComponent((String)s)).collect(java.util.stream.Collectors.toList()));
            xpBtn.setItemMeta(xpMeta);
        }
        inv.setItem(20, xpBtn);

        // Ouvrir Stockage d'Items (Slot 24)
        ItemStack itemsBtn = new ItemStack(Material.CHEST);
        ItemMeta itemsMeta = itemsBtn.getItemMeta();
        if (itemsMeta != null) {
            itemsMeta.displayName(fr.gens.core.utils.PlaceholderUtils.parseToComponent("<gold><bold>Stockage d'objets"));
            int totalItems = data.getStoredItems().values().stream().mapToInt(Integer::intValue).sum();
            List<String> lore = new ArrayList<>();
            lore.add("<gray>Items Actuels: <yellow>" + totalItems + " / " + moduleInstance.getSpawnerManager().getMaxStorageItems(data.getStorageLevel()));
            lore.add("");
            lore.add("<yellow>Cliquez pour ouvrir le coffre");
            itemsMeta.lore(java.util.Optional.ofNullable(lore).orElse(java.util.Collections.emptyList()).stream().map(s -> fr.gens.core.utils.PlaceholderUtils.parseToComponent((String)s)).collect(java.util.stream.Collectors.toList()));
            itemsBtn.setItemMeta(itemsMeta);
        }
        inv.setItem(24, itemsBtn);

        // Upgrades
        EconomyModule eco = moduleInstance != null ? (EconomyModule) moduleInstance.getPlugin().getModuleManager().getModule("economy") : null;
        boolean ecoEnabled = eco != null && eco.isEnabled();
        
        inv.setItem(38, createUpgradeItem("Capacité XP", Material.EMERALD, data.getExpLevel(), ecoEnabled));
        inv.setItem(40, createUpgradeItem("Vitesse", Material.SUGAR, data.getSpeedLevel(), ecoEnabled));
        inv.setItem(42, createUpgradeItem("Capacité Stockage", Material.DIAMOND, data.getStorageLevel(), ecoEnabled));

        openGuis.put(player.getUniqueId(), data);
        player.openInventory(inv);
    }
    
    private static void openBedrockGui(Player player, SpawnerData data) {
        java.util.List<fr.gens.core.utils.BedrockFormManager.BedrockButton> buttons = new java.util.ArrayList<>();
        
        // XP Button
        String xpText = "§aRécupérer l'XP\n§8" + data.getStoredExp() + " / " + moduleInstance.getSpawnerManager().getMaxStorageExp(data.getExpLevel());
        buttons.add(new fr.gens.core.utils.BedrockFormManager.BedrockButton(xpText, Material.EXPERIENCE_BOTTLE, p -> {
            if (data.getStoredExp() > 0) {
                p.giveExp(data.getStoredExp());
                p.sendMessage(fr.gens.core.utils.PlaceholderUtils.parseToComponent("<green>Vous avez récupéré " + data.getStoredExp() + " points d'expérience !"));
                p.playSound(p.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
                data.setStoredExp(0);
                moduleInstance.getSpawnerManager().updateHologram(data);
                moduleInstance.getPlugin().getFoliaLib().getScheduler().runAtEntity(p, (wrappedTask) -> openGui(p, data));
            } else {
                moduleInstance.getPlugin().getLangManager().sendMessage(p, "spawnergui.msg_1");
            }
        }));

        // Items Button
        int totalItems = data.getStoredItems().values().stream().mapToInt(Integer::intValue).sum();
        String itemsText = "§6Stockage d'objets\n§8" + totalItems + " / " + moduleInstance.getSpawnerManager().getMaxStorageItems(data.getStorageLevel());
        buttons.add(new fr.gens.core.utils.BedrockFormManager.BedrockButton(itemsText, Material.CHEST, p -> {
            moduleInstance.getPlugin().getFoliaLib().getScheduler().runAtEntity(p, (wrappedTask) -> SpawnerLootGui.openGui(p, data, moduleInstance));
        }));

        EconomyModule eco = moduleInstance != null ? (EconomyModule) moduleInstance.getPlugin().getModuleManager().getModule("economy") : null;
        boolean ecoEnabled = eco != null && eco.isEnabled();

        // Upgrades
        buttons.add(createBedrockUpgradeBtn("Capacité XP", Material.EMERALD, "exp", data, data.getExpLevel(), ecoEnabled));
        buttons.add(createBedrockUpgradeBtn("Vitesse", Material.SUGAR, "speed", data, data.getSpeedLevel(), ecoEnabled));
        buttons.add(createBedrockUpgradeBtn("Capacité Stockage", Material.DIAMOND, "storage", data, data.getStorageLevel(), ecoEnabled));

        String content = "Type: " + data.getType() + "\nStack: " + data.getStackCount() + "\nDernier accès: " + data.getLastInteractedPlayer();
        fr.gens.core.utils.BedrockFormManager.openSimpleForm(player, "Gestion: " + data.getType(), content, buttons);
    }

    private static fr.gens.core.utils.BedrockFormManager.BedrockButton createBedrockUpgradeBtn(String name, Material mat, String type, SpawnerData data, int currentLevel, boolean ecoEnabled) {
        String text = "§bAméliorer " + name + "\n§8Niv: " + currentLevel;
        if (currentLevel >= 10) {
            text += " (MAX)";
        } else {
            if (ecoEnabled) {
                text += " | Cout: " + getUpgradeCost(currentLevel) + "$";
            } else {
                text += " | Cout: " + getUpgradeXpCost(currentLevel) + " Niv";
            }
        }
        return new fr.gens.core.utils.BedrockFormManager.BedrockButton(text, mat, p -> handleUpgrade(p, data, type));
    }
    
    private static ItemStack createUpgradeItem(String name, Material mat, int currentLevel, boolean ecoEnabled) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(fr.gens.core.utils.PlaceholderUtils.parseToComponent("<aqua><bold>Améliorer " + name));
            List<String> lore = new ArrayList<>();
            lore.add("<gray>Niveau actuel: <white>" + currentLevel);
            if (currentLevel >= 10) {
                lore.add("<red><bold>NIVEAU MAX !");
            } else {
                lore.add("<gray>Niveau suivant: <white>" + (currentLevel + 1));
                lore.add("");
                if (ecoEnabled) {
                    lore.add("<gray>Cout: <gold>" + getUpgradeCost(currentLevel) + "$");
                } else {
                    lore.add("<gray>Cout: <green>" + getUpgradeXpCost(currentLevel) + " Niveaux");
                }
                lore.add("<yellow>Cliquez pour améliorer !");
            }
            meta.lore(java.util.Optional.ofNullable(lore).orElse(java.util.Collections.emptyList()).stream().map(s -> fr.gens.core.utils.PlaceholderUtils.parseToComponent((String)s)).collect(java.util.stream.Collectors.toList()));
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
        if (!openGuis.containsKey(player.getUniqueId())) return;
        
        SpawnerData data = openGuis.get(player.getUniqueId());
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
                player.sendMessage(fr.gens.core.utils.PlaceholderUtils.parseToComponent("<green>Vous avez récupéré " + data.getStoredExp() + " points d'expérience !"));
                player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
                data.setStoredExp(0);
                moduleInstance.getSpawnerManager().updateHologram(data);
                moduleInstance.getPlugin().getFoliaLib().getScheduler().runAtEntity(player, (wrappedTask) -> openGui(player, data)); // refresh différé
            } else {
                moduleInstance.getPlugin().getLangManager().sendMessage(player, "spawnergui.msg_1");
            }
        } else if (slot == 24) {
            // Coffre Items
            moduleInstance.getPlugin().getFoliaLib().getScheduler().runAtEntity(player, (wrappedTask) -> SpawnerLootGui.openGui(player, data, moduleInstance));
        } else if (slot == 38) {
            handleUpgrade(player, data, "exp");
        } else if (slot == 40) {
            handleUpgrade(player, data, "speed");
        } else if (slot == 42) {
            handleUpgrade(player, data, "storage");
        }
    }
    
    public static void handleUpgrade(Player player, SpawnerData data, String type) {
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
                player.sendMessage(fr.gens.core.utils.PlaceholderUtils.parseToComponent("<green>Amélioration effectuée pour " + cost + "$ !"));
            } else {
                player.sendMessage(fr.gens.core.utils.PlaceholderUtils.parseToComponent("<red>Vous n'avez pas assez d'argent ! (" + cost + "$ nécessaires)"));
            }
        } else {
            int xpCost = getUpgradeXpCost(currentLevel);
            if (player.getLevel() >= xpCost) {
                player.setLevel(player.getLevel() - xpCost);
                applyUpgrade(player, data, type, currentLevel);
                player.sendMessage(fr.gens.core.utils.PlaceholderUtils.parseToComponent("<green>Amélioration effectuée pour " + xpCost + " niveaux !"));
            } else {
                player.sendMessage(fr.gens.core.utils.PlaceholderUtils.parseToComponent("<red>Vous n'avez pas assez de niveaux ! (" + xpCost + " nécessaires)"));
            }
        }
    }
    
    public static void applyUpgrade(Player player, SpawnerData data, String type, int currentLevel) {
        switch (type) {
            case "exp" -> data.setExpLevel(currentLevel + 1);
            case "speed" -> data.setSpeedLevel(currentLevel + 1);
            case "storage" -> data.setStorageLevel(currentLevel + 1);
        }
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
        moduleInstance.getSpawnerManager().updateHologram(data);
        moduleInstance.getPlugin().getFoliaLib().getScheduler().runAtEntity(player, (wrappedTask) -> openGui(player, data)); // refresh différééré
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(event.getView().title()).startsWith("Gestion: ")) return;
        if (event.getWhoClicked() instanceof Player && openGuis.containsKey(((Player) event.getWhoClicked()).getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(event.getView().title()).startsWith("Gestion: ")) return;
        if (event.getPlayer() instanceof Player) {
            Player p = (Player) event.getPlayer();
            // Délai d'un tick pour voir si le joueur ouvre un autre inventaire de gestion (ex: refresh)
            moduleInstance.getPlugin().getFoliaLib().getScheduler().runAtEntity(p, (wrappedTask) -> {
                if (!net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(p.getOpenInventory().title()).startsWith("Gestion: ")) {
                    openGuis.remove(p.getUniqueId());
                }
            });
        }
    }
}




