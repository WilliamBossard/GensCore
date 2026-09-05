package fr.gens.core.modules.shop;

import fr.gens.core.CorePlugin;
import fr.gens.core.modules.EconomyModule;
import fr.gens.core.modules.GuiModule.GensGuiHolder;
import fr.gens.core.modules.Module;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.incendo.cloud.annotations.Command;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


public class ShopModule implements Module {

    private final CorePlugin plugin;
    private boolean enabled = false;
    private List<ShopCategory> categories;
    
    private fr.gens.core.database.ShopDAO shopDAO;

    public static double GLOBAL_INFLATION_EXPONENT = 0.5;

    public ShopModule(CorePlugin plugin) {
        this.plugin = plugin;
        this.categories = new ArrayList<>();
    }

    @Override
    public String getName() {
        return "DynamicShop";
    }

    @Override
    public String getDescription() {
        return "Boutique en jeu avec inflation dynamique gérée par l'offre et la demande.";
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    public fr.gens.core.database.ShopDAO getShopDAO() {
        return shopDAO;
    }

    @Override
    public void initDatabase(fr.gens.core.utils.DatabaseManager dbManager) {
        dbManager.executeStatement("CREATE TABLE IF NOT EXISTS shop_categories (id VARCHAR(50) PRIMARY KEY, displayName VARCHAR(255) NOT NULL, icon VARCHAR(50) NOT NULL);");
        dbManager.executeStatement("CREATE TABLE IF NOT EXISTS shop_items (material VARCHAR(50) PRIMARY KEY, category_id VARCHAR(50) NOT NULL, buyPrice DOUBLE NOT NULL, sellPrice DOUBLE NOT NULL, stock INTEGER DEFAULT 0, targetStock INTEGER DEFAULT 1000, isCommand BOOLEAN DEFAULT 0, commandToExecute TEXT, isEnabled BOOLEAN DEFAULT 1, FOREIGN KEY(category_id) REFERENCES shop_categories(id) ON DELETE CASCADE);");
        dbManager.executeStatement("CREATE TABLE IF NOT EXISTS shop_history (id INTEGER PRIMARY KEY AUTOINCREMENT, material VARCHAR(50) NOT NULL, timestamp BIGINT NOT NULL, buyPrice DOUBLE NOT NULL, sellPrice DOUBLE NOT NULL, stock INTEGER NOT NULL, FOREIGN KEY(material) REFERENCES shop_items(material) ON DELETE CASCADE);");
    }

    @Override
    public void enable() {
        enabled = true;
        GLOBAL_INFLATION_EXPONENT = plugin.getConfig().getDouble("shop.inflation_exponent", 0.5);

        // Créer ou vérifier la table
        this.shopDAO = new fr.gens.core.database.ShopDAO(plugin);
        this.shopDAO.initDatabase();
        
        loadShop();
        plugin.getLangManager().sendConsoleMessage("shopmodule.log_3");
    }

    @Override
    public void registerCommands(fr.gens.core.CorePlugin plugin) {
        if (plugin.getCommandManager() != null && plugin.getCommandManager().getAnnotationParser() != null) {
            plugin.getCommandManager().getAnnotationParser().parse(this);
        }
    }



    @Override
    public void disable() {
        enabled = false;
        saveShop();
        plugin.getLangManager().sendConsoleMessage("shopmodule.log_4");
    }

    public List<ShopCategory> getCategories() {
        return categories;
    }

    public ShopCategory getCategory(String id) {
        for (ShopCategory cat : categories) {
            if (cat.getId().equalsIgnoreCase(id)) return cat;
        }
        return null;
    }

    public void loadShop() {
        categories.clear();
        categories.addAll(this.shopDAO.loadShopCategories());
        this.shopDAO.loadShopItems(categories);
    }

    public void saveShop() {
        this.shopDAO.saveShop(categories);
    }

    public void logTransaction(ShopItem item) {
        this.shopDAO.logTransaction(item);
    }

    public void logPlayerTransaction(UUID uuid, String type, String material, int amount, double price) {
        this.shopDAO.logPlayerTransaction(uuid, type, material, amount, price);
    }

    // --- WEB EXTENSION ---
    public java.util.List<java.util.Map<String, Object>> getHistory(String material) {
        return this.shopDAO.getHistory(material);
    }

    public boolean deleteItem(String categoryId, String materialName) {
        return this.shopDAO.deleteItem(categoryId, materialName);
    }

    public boolean deleteCategory(String categoryId) {
        return this.shopDAO.deleteCategory(categoryId);
    }

    @Command("shop")
    public void executeShop(org.bukkit.command.CommandSender sender) {
        if (!(sender instanceof org.bukkit.entity.Player)) return;
        org.bukkit.entity.Player p = (org.bukkit.entity.Player) sender;
        if (!enabled) {
            plugin.getLangManager().sendMessage(p, "shopmodule.msg_1");
            return;
        }
        openCategoryGui(p);
    }

    public void openCategoryGui(Player player) {
        if (fr.gens.core.utils.FloodgateUtil.isBedrockPlayer(player.getUniqueId())) {
            java.util.List<fr.gens.core.utils.BedrockFormManager.BedrockButton> buttons = new java.util.ArrayList<>();
            for (ShopCategory cat : categories) {
                buttons.add(new fr.gens.core.utils.BedrockFormManager.BedrockButton(
                    cat.getDisplayName() + "\n§8" + cat.getItems().size() + " objets",
                    cat.getIcon(),
                    p -> openItemsGui(p, cat)
                ));
            }
            fr.gens.core.utils.BedrockFormManager.openSimpleForm(player, "Boutique", "Sélectionnez une catégorie :", buttons);
            return;
        }

        ShopCategoryGuiHolder holder = new ShopCategoryGuiHolder();
        int size = Math.max(9, (int) (Math.ceil(categories.size() / 9.0) * 9));
        Inventory inv = Bukkit.createInventory(holder, size, fr.gens.core.utils.PlaceholderUtils.parseToComponent("<dark_gray>Boutique - Catégories"));
        holder.setInventory(inv);

        for (int i = 0; i < categories.size(); i++) {
            ShopCategory cat = categories.get(i);
            ItemStack item = new ItemStack(cat.getIcon());
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.displayName(fr.gens.core.utils.PlaceholderUtils.parseToComponent("<green><bold>" + cat.getDisplayName()));
                List<String> lore = new ArrayList<>();
                lore.add("<gray>" + cat.getItems().size() + " objets disponibles.");
                lore.add("<yellow>Cliquez pour ouvrir !");
                meta.lore(java.util.Optional.ofNullable(lore).orElse(java.util.Collections.emptyList()).stream().map(s -> fr.gens.core.utils.PlaceholderUtils.parseToComponent((String)s)).collect(java.util.stream.Collectors.toList()));
                item.setItemMeta(meta);
            }
            inv.setItem(i, item);
        }

        player.openInventory(inv);
    }

    public void openItemsGui(Player player, ShopCategory category) {
        if (fr.gens.core.utils.FloodgateUtil.isBedrockPlayer(player.getUniqueId())) {
            java.util.List<fr.gens.core.utils.BedrockFormManager.BedrockButton> buttons = new java.util.ArrayList<>();
            buttons.add(new fr.gens.core.utils.BedrockFormManager.BedrockButton("§c§lRetour\n§r§8Menu Principal", org.bukkit.Material.BARRIER, p -> openCategoryGui(p)));

            for (ShopItem item : category.getItems()) {
                if (!item.isEnabled()) continue;
                String btnText = item.getMaterial().name() + "\n";
                if (item.isCommand()) {
                    btnText += "§aPrix: " + String.format("%.2f", item.getCurrentBuyPrice()) + "$";
                } else {
                    btnText += "§aAchat: " + String.format("%.2f", item.getCurrentBuyPrice()) + "$";
                    if (item.getBaseSellPrice() > 0) {
                        btnText += " | §cVente: " + String.format("%.2f", item.getCurrentSellPrice()) + "$";
                    }
                }
                buttons.add(new fr.gens.core.utils.BedrockFormManager.BedrockButton(btnText, item.getMaterial(), p -> {
                    openBedrockItemAction(p, category, item);
                }));
            }
            fr.gens.core.utils.BedrockFormManager.openSimpleForm(player, "Shop - " + category.getDisplayName(), "Sélectionnez un objet :", buttons);
            return;
        }

        ShopItemsGuiHolder holder = new ShopItemsGuiHolder(category);
        Inventory inv = Bukkit.createInventory(holder, 54, fr.gens.core.utils.PlaceholderUtils.parseToComponent("<dark_gray>Shop - " + category.getDisplayName()));
        holder.setInventory(inv);

        int slot = 0;
        for (ShopItem item : category.getItems()) {
            if (!item.isEnabled()) continue;
            if (slot >= 45) break; // Pagination plus tard
            ItemStack i = new ItemStack(item.getMaterial());
            ItemMeta meta = i.getItemMeta();
            if (meta != null) {
                meta.displayName(fr.gens.core.utils.PlaceholderUtils.parseToComponent("<white><bold>" + item.getMaterial().name()));
                List<String> lore = new ArrayList<>();
                lore.add("<dark_gray>Prix Dynamique (Inflation)");
                lore.add("");
                if (item.isCommand()) {
                lore.add("<green>➔ Achat Unique : <yellow>" + String.format("%.2f", item.getCurrentBuyPrice()) + " $");
                lore.add("<dark_gray>(Exécute une commande sur votre compte)");
                lore.add("");
                lore.add("<yellow>Clic Gauche pour Acheter");
            } else {
                lore.add("<green>➔ Achat (x1) : <yellow>" + String.format("%.2f", item.getCurrentBuyPrice()) + " $");
                lore.add("<red>➔ Vente (x1) : <yellow>" + String.format("%.2f", item.getCurrentSellPrice()) + " $");
                lore.add("");
                lore.add("<gray>Stock du Serveur: " + item.getStock() + " (Cible: " + item.getTargetStock() + ")");
                lore.add("");
                lore.add("<yellow>Clic Gauche pour Acheter");
                lore.add("<yellow>Clic Droit pour Vendre");
                lore.add("<dark_gray>(Shift pour x64)");
            }
                meta.lore(java.util.Optional.ofNullable(lore).orElse(java.util.Collections.emptyList()).stream().map(s -> fr.gens.core.utils.PlaceholderUtils.parseToComponent((String)s)).collect(java.util.stream.Collectors.toList()));
                i.setItemMeta(meta);
            }
            inv.setItem(slot++, i);
        }

        // Bouton retour
        ItemStack back = new ItemStack(Material.BARRIER);
        ItemMeta backMeta = back.getItemMeta();
        backMeta.displayName(fr.gens.core.utils.PlaceholderUtils.parseToComponent("<red><bold>Retour aux catégories"));
        back.setItemMeta(backMeta);
        inv.setItem(49, back);

        player.openInventory(inv);
    }

    public void openBedrockItemAction(Player player, ShopCategory category, ShopItem item) {
        java.util.List<fr.gens.core.utils.BedrockFormManager.BedrockButton> buttons = new java.util.ArrayList<>();
        
        if (item.isCommand()) {
            buttons.add(new fr.gens.core.utils.BedrockFormManager.BedrockButton("§aAcheter (x1)\n§r§8" + String.format("%.2f", item.getCurrentBuyPrice()) + "$", org.bukkit.Material.EMERALD, p -> {
                buyItem(p, item, 1);
                openItemsGui(p, category);
            }));
        } else {
            buttons.add(new fr.gens.core.utils.BedrockFormManager.BedrockButton("§aAcheter (x1)\n§r§8" + String.format("%.2f", item.getCurrentBuyPrice()) + "$", org.bukkit.Material.EMERALD, p -> {
                buyItem(p, item, 1);
                openItemsGui(p, category);
            }));
            buttons.add(new fr.gens.core.utils.BedrockFormManager.BedrockButton("§aAcheter (x64)\n§r§8" + String.format("%.2f", item.getCurrentBuyPrice() * 64) + "$", org.bukkit.Material.EMERALD_BLOCK, p -> {
                buyItem(p, item, 64);
                openItemsGui(p, category);
            }));
            if (item.getBaseSellPrice() > 0) {
                buttons.add(new fr.gens.core.utils.BedrockFormManager.BedrockButton("§cVendre (x1)\n§r§8" + String.format("%.2f", item.getCurrentSellPrice()) + "$", org.bukkit.Material.REDSTONE, p -> {
                    sellItem(p, item, 1);
                    openItemsGui(p, category);
                }));
                buttons.add(new fr.gens.core.utils.BedrockFormManager.BedrockButton("§cVendre Tout\n§r§8Inventaire", org.bukkit.Material.REDSTONE_BLOCK, p -> {
                    sellAll(p, item);
                    openItemsGui(p, category);
                }));
            }
        }
        
        buttons.add(new fr.gens.core.utils.BedrockFormManager.BedrockButton("§cRetour\n§r§8Objets", org.bukkit.Material.BARRIER, p -> openItemsGui(p, category)));

        fr.gens.core.utils.BedrockFormManager.openSimpleForm(player, "Action: " + item.getMaterial().name(), "Que voulez-vous faire ?", buttons);
    }

    private void sellAll(Player p, ShopItem item) {
        int count = 0;
        for (ItemStack invItem : p.getInventory().getContents()) {
            if (invItem != null && invItem.getType() == item.getMaterial()) {
                count += invItem.getAmount();
            }
        }
        if (count > 0) sellItem(p, item, count);
        else plugin.getLangManager().sendMessage(p, "shopmodule.msg_4");
    }

    private class ShopCategoryGuiHolder implements GensGuiHolder {
        private Inventory inventory;

        public void setInventory(Inventory inv) { this.inventory = inv; }
        @Override public Inventory getInventory() { return inventory; }

        @Override
        public void onClick(InventoryClickEvent event) {
            event.setCancelled(true);
            Player p = (Player) event.getWhoClicked();
            int slot = event.getSlot();
            if (slot >= 0 && slot < categories.size()) {
                openItemsGui(p, categories.get(slot));
            }
        }
    }

    private class ShopItemsGuiHolder implements GensGuiHolder {
        private Inventory inventory;
        private final ShopCategory category;

        public ShopItemsGuiHolder(ShopCategory category) {
            this.category = category;
        }

        public void setInventory(Inventory inv) { this.inventory = inv; }
        @Override public Inventory getInventory() { return inventory; }

        @Override
        public void onClick(InventoryClickEvent event) {
            event.setCancelled(true);
            Player p = (Player) event.getWhoClicked();
            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || clicked.getType() == Material.AIR) return;

            if (event.getSlot() == 49 && clicked.getType() == Material.BARRIER) {
                openCategoryGui(p);
                return;
            }

            ShopItem shopItem = category.getItem(clicked.getType());
            if (shopItem != null) {
                int amount = (event.getClick() == org.bukkit.event.inventory.ClickType.SHIFT_LEFT || event.getClick() == org.bukkit.event.inventory.ClickType.SHIFT_RIGHT) ? 64 : 1;
                if (event.getClick() == org.bukkit.event.inventory.ClickType.LEFT || event.getClick() == org.bukkit.event.inventory.ClickType.SHIFT_LEFT) {
                    buyItem(p, shopItem, amount);
                    openItemsGui(p, category); // Refresh
                } else if (event.getClick() == org.bukkit.event.inventory.ClickType.RIGHT || event.getClick() == org.bukkit.event.inventory.ClickType.SHIFT_RIGHT) {
                    if (shopItem.isCommand()) {
                        plugin.getLangManager().sendMessage(p, "shopmodule.msg_3");
                        return;
                    }
                    sellItem(p, shopItem, amount);
                    openItemsGui(p, category); // Refresh
                }
            }
        }
    }

    public void buyItem(Player p, ShopItem shopItem, int amount) {
        EconomyModule eco = (EconomyModule) plugin.getModuleManager().getModule("economy");
        if (eco == null) return;
        double totalCost = shopItem.getCurrentBuyPrice() * amount;
        if (eco.takeMoneyAtomic(p.getUniqueId(), totalCost)) {
            shopItem.setStock(Math.max(0, shopItem.getStock() - amount));
            
            if (shopItem.isCommand()) {
                String cmd = shopItem.getCommandToExecute().replace("%player%", p.getName());
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
                p.sendMessage(fr.gens.core.utils.PlaceholderUtils.parseToComponent("<green>Achat validé ! Vous avez obtenu le contenu de <yellow>" + shopItem.getMaterial().name()));
            } else {
                p.getInventory().addItem(new ItemStack(shopItem.getMaterial(), amount));
                p.sendMessage(fr.gens.core.utils.PlaceholderUtils.parseToComponent("<green>Achat de " + amount + "x " + shopItem.getMaterial().name() + " pour <yellow>" + String.format("%.2f", totalCost) + " $"));
            }
            
            saveShop();
            logTransaction(shopItem);
            logPlayerTransaction(p.getUniqueId(), "ACHAT", shopItem.getMaterial().name(), amount, totalCost);
        } else {
            plugin.getLangManager().sendMessage(p, "shopmodule.msg_2");
        }
    }

    public void sellItem(Player p, ShopItem shopItem, int amount) {
        EconomyModule eco = (EconomyModule) plugin.getModuleManager().getModule("economy");
        if (eco == null) return;
        
        int playerHas = 0;
        for (ItemStack i : p.getInventory().getContents()) {
            if (i != null && i.getType() == shopItem.getMaterial()) {
                playerHas += i.getAmount();
            }
        }

        if (playerHas >= amount) {
            double totalEarn = shopItem.getCurrentSellPrice() * amount;
            
            // Retirer l'item de l'inventaire
            int toRemove = amount;
            for (ItemStack i : p.getInventory().getContents()) {
                if (i != null && i.getType() == shopItem.getMaterial()) {
                    if (i.getAmount() <= toRemove) {
                        toRemove -= i.getAmount();
                        i.setAmount(0);
                    } else {
                        i.setAmount(i.getAmount() - toRemove);
                        toRemove = 0;
                    }
                    if (toRemove <= 0) break;
                }
            }

            eco.giveMoney(p.getUniqueId(), totalEarn);
            shopItem.setStock(shopItem.getStock() + amount);
            p.sendMessage(fr.gens.core.utils.PlaceholderUtils.parseToComponent("<green>Vente de " + amount + "x " + shopItem.getMaterial().name() + " pour <yellow>" + String.format("%.2f", totalEarn) + " $"));
            saveShop();
            logTransaction(shopItem);
            logPlayerTransaction(p.getUniqueId(), "VENTE", shopItem.getMaterial().name(), amount, totalEarn);
        } else {
            plugin.getLangManager().sendMessage(p, "shopmodule.msg_4");
        }
    }
}
