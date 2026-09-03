package fr.gens.core.modules.shop;

import fr.gens.core.CorePlugin;
import fr.gens.core.modules.EconomyModule;
import fr.gens.core.modules.GuiModule.GensGuiHolder;
import fr.gens.core.modules.Module;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import cloud.commandframework.annotations.CommandMethod;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
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

    @CommandMethod("shop")
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
                EconomyModule eco = (EconomyModule) plugin.getModuleManager().getModule("economy");
                if (eco == null) return;

                int amount = (event.getClick() == ClickType.SHIFT_LEFT || event.getClick() == ClickType.SHIFT_RIGHT) ? 64 : 1;
                boolean isBuy = (event.getClick() == ClickType.LEFT || event.getClick() == ClickType.SHIFT_LEFT);

                if (isBuy) {
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
                        
                        openItemsGui(p, category); // Refresh
                        saveShop();
                        logTransaction(shopItem);
                        logPlayerTransaction(p.getUniqueId(), "ACHAT", shopItem.getMaterial().name(), amount, totalCost);
                    } else {
                        plugin.getLangManager().sendMessage(p, "shopmodule.msg_2");
                    }
                } else {
                    if (shopItem.isCommand()) {
                        plugin.getLangManager().sendMessage(p, "shopmodule.msg_3");
                        return;
                    }
                    
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
                        
                        // Modifier le stock (augmente car les joueurs vendent au serveur)
                        shopItem.setStock(shopItem.getStock() + amount);
                        
                        p.sendMessage(fr.gens.core.utils.PlaceholderUtils.parseToComponent("<green>Vente de " + amount + "x " + shopItem.getMaterial().name() + " pour <yellow>" + String.format("%.2f", totalEarn) + " $"));
                        openItemsGui(p, category); // Refresh
                        saveShop();
                        logTransaction(shopItem);
                        logPlayerTransaction(p.getUniqueId(), "VENTE", shopItem.getMaterial().name(), amount, totalEarn);
                    } else {
                        plugin.getLangManager().sendMessage(p, "shopmodule.msg_4");
                    }
                }
            }
        }
    }
}



