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
import net.kyori.adventure.text.Component;
import fr.gens.core.utils.BedrockFormManager;
import fr.gens.core.utils.BedrockFormManager.BedrockButton;
import fr.gens.core.utils.FloodgateUtil;
import fr.gens.core.utils.PlaceholderUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.inventory.meta.Damageable;


public class ShopModule implements Module {

    private final CorePlugin plugin;
    private boolean enabled = false;
    private List<ShopCategory> categories;
    
    private fr.gens.core.database.ShopDAO shopDAO;

    public static double GLOBAL_INFLATION_EXPONENT = 0.5;
    private final java.util.Map<UUID, Long> clickDebounce = new java.util.concurrent.ConcurrentHashMap<>();

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
        if (categories.isEmpty()) {
            loadShopFromConfig();
            saveShop();
            plugin.getLogger().info("[Shop] Boutique initialisée avec succès depuis modules/shop.yml !");
        }
    }

    public void loadShopFromConfig() {
        categories.clear();
        org.bukkit.configuration.file.FileConfiguration config = plugin.getConfigManager().getConfig("modules/shop.yml");
        org.bukkit.configuration.ConfigurationSection catSection = config.getConfigurationSection("categories");
        if (catSection != null) {
            for (String catKey : catSection.getKeys(false)) {
                org.bukkit.configuration.ConfigurationSection cs = catSection.getConfigurationSection(catKey);
                if (cs == null) continue;
                String displayName = cs.getString("displayName", catKey);
                String iconName = cs.getString("icon", "CHEST");
                Material icon = Material.matchMaterial(iconName);
                if (icon == null) icon = Material.CHEST;
                ShopCategory cat = new ShopCategory(catKey, displayName, icon);

                org.bukkit.configuration.ConfigurationSection itemsSec = cs.getConfigurationSection("items");
                if (itemsSec != null) {
                    for (String matKey : itemsSec.getKeys(false)) {
                        org.bukkit.configuration.ConfigurationSection is = itemsSec.getConfigurationSection(matKey);
                        if (is == null) continue;
                        Material mat = Material.matchMaterial(matKey);
                        if (mat == null) continue;
                        double buyPrice = is.getDouble("buyPrice", 10.0);
                        double sellPrice = is.getDouble("sellPrice", 0.0);
                        int stock = is.getInt("stock", 500);
                        int targetStock = is.getInt("targetStock", 500);
                        boolean isCommand = is.getBoolean("isCommand", false);
                        String commandToExecute = is.getString("commandToExecute", "");
                        boolean isEnabled = is.getBoolean("isEnabled", true);

                        ShopItem item = new ShopItem(mat, buyPrice, sellPrice);
                        item.setStock(stock);
                        item.setTargetStock(targetStock);
                        item.setCommand(isCommand);
                        item.setCommandToExecute(commandToExecute);
                        item.setEnabled(isEnabled);
                        cat.addItem(item);
                    }
                }
                categories.add(cat);
            }
        }
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
        plugin.getFoliaLib().getScheduler().runAtEntity(p, task -> openCategoryGui(p));
    }

    @Command("shop reload")
    @org.incendo.cloud.annotations.Permission("genscore.admin")
    public void executeShopReload(org.bukkit.command.CommandSender sender) {
        plugin.getConfigManager().loadConfig("modules/shop.yml");
        loadShopFromConfig();
        saveShop();
        plugin.getLangManager().sendMessage(sender, "shopmodule.reload_success");
    }

    public void openCategoryGui(Player player) {
        if (FloodgateUtil.isBedrockPlayer(player.getUniqueId())) {
            List<BedrockButton> buttons = new ArrayList<>();
            for (ShopCategory cat : categories) {
                buttons.add(new BedrockButton(
                    cat.getDisplayName() + "\n§8" + cat.getItems().size() + " objets",
                    cat.getIcon(),
                    p -> openItemsGui(p, cat)
                ));
            }
            BedrockFormManager.openSimpleForm(player, "Boutique", "Sélectionnez une catégorie :", buttons);
            return;
        }

        ShopCategoryGuiHolder holder = new ShopCategoryGuiHolder();
        int size = Math.max(9, (int) (Math.ceil(categories.size() / 9.0) * 9));
        Inventory inv = Bukkit.createInventory(holder, size, plugin.getLangManager().get("shopmodule.gui_categories_title"));
        holder.setInventory(inv);

        for (int i = 0; i < categories.size(); i++) {
            ShopCategory cat = categories.get(i);
            ItemStack item = new ItemStack(cat.getIcon());
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.displayName(PlaceholderUtils.parseToComponent("<green><bold>" + cat.getDisplayName()));
                List<Component> componentLore = new ArrayList<>();
                componentLore.add(PlaceholderUtils.parseToComponent("<gray>" + cat.getItems().size() + " objets disponibles."));
                componentLore.add(PlaceholderUtils.parseToComponent("<yellow>Cliquez pour ouvrir !"));
                meta.lore(componentLore);
                item.setItemMeta(meta);
            }
            inv.setItem(i, item);
        }

        plugin.getFoliaLib().getScheduler().runAtEntity(player, task -> player.openInventory(inv));
    }

    public void openItemsGui(Player player, ShopCategory category) {
        openItemsGui(player, category, 0);
    }

    public void openItemsGui(Player player, ShopCategory category, int page) {
        if (FloodgateUtil.isBedrockPlayer(player.getUniqueId())) {
            List<BedrockButton> buttons = new ArrayList<>();
            buttons.add(new BedrockButton("§c§lRetour\n§r§8Menu Principal", org.bukkit.Material.BARRIER, p -> openCategoryGui(p)));

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
                buttons.add(new BedrockButton(btnText, item.getMaterial(), p -> {
                    openBedrockItemAction(p, category, item);
                }));
            }
            BedrockFormManager.openSimpleForm(player, "Shop - " + category.getDisplayName(), "Sélectionnez un objet :", buttons);
            return;
        }

        List<ShopItem> enabledItems = category.getItems().stream().filter(ShopItem::isEnabled).toList();
        int totalPages = Math.max(1, (int) Math.ceil(enabledItems.size() / 45.0));
        int currentPage = Math.max(0, Math.min(page, totalPages - 1));

        ShopItemsGuiHolder holder = new ShopItemsGuiHolder(category, currentPage);
        String pageSuffix = totalPages > 1 ? " (" + (currentPage + 1) + "/" + totalPages + ")" : "";
        Component titleComp = plugin.getLangManager().get("shopmodule.gui_items_title",
                net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.parsed("category", category.getDisplayName()),
                net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.parsed("page", pageSuffix));
        Inventory inv = Bukkit.createInventory(holder, 54, titleComp);
        holder.setInventory(inv);

        int startIndex = currentPage * 45;
        int endIndex = Math.min(enabledItems.size(), startIndex + 45);

        int slot = 0;
        for (int i = startIndex; i < endIndex; i++) {
            ShopItem item = enabledItems.get(i);
            ItemStack is = new ItemStack(item.getMaterial());
            ItemMeta meta = is.getItemMeta();
            if (meta != null) {
                meta.displayName(PlaceholderUtils.parseToComponent("<white><bold>" + item.getMaterial().name()));
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
                    if (item.getBaseSellPrice() > 0) {
                        lore.add("<red>➔ Vente (x1) : <yellow>" + String.format("%.2f", item.getCurrentSellPrice()) + " $");
                    }
                    lore.add("");
                    lore.add("<gray>Stock du Serveur: " + item.getStock() + " (Cible: " + item.getTargetStock() + ")");
                    lore.add("");
                    lore.add("<yellow>Clic Gauche pour Acheter");
                    if (item.getBaseSellPrice() > 0) {
                        lore.add("<yellow>Clic Droit pour Vendre");
                    }
                    lore.add("<dark_gray>(Shift pour x64)");
                }
                List<Component> componentLore = new ArrayList<>();
                for (String l : lore) {
                    componentLore.add(PlaceholderUtils.parseToComponent(l));
                }
                meta.lore(componentLore);
                is.setItemMeta(meta);
            }
            inv.setItem(slot++, is);
        }

        // Bouton page précédente
        if (currentPage > 0) {
            ItemStack prev = new ItemStack(Material.ARROW);
            ItemMeta prevMeta = prev.getItemMeta();
            if (prevMeta != null) {
                prevMeta.displayName(plugin.getLangManager().get("shopmodule.gui_prev_page",
                        net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.parsed("page", String.valueOf(currentPage)),
                        net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.parsed("total", String.valueOf(totalPages))));
                prev.setItemMeta(prevMeta);
            }
            inv.setItem(45, prev);
        }

        // Bouton retour
        ItemStack back = new ItemStack(Material.BARRIER);
        ItemMeta backMeta = back.getItemMeta();
        if (backMeta != null) {
            backMeta.displayName(plugin.getLangManager().get("shopmodule.gui_back"));
            back.setItemMeta(backMeta);
        }
        inv.setItem(49, back);

        // Bouton page suivante
        if (currentPage < totalPages - 1) {
            ItemStack next = new ItemStack(Material.ARROW);
            ItemMeta nextMeta = next.getItemMeta();
            if (nextMeta != null) {
                nextMeta.displayName(plugin.getLangManager().get("shopmodule.gui_next_page",
                        net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.parsed("page", String.valueOf(currentPage + 2)),
                        net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.parsed("total", String.valueOf(totalPages))));
                next.setItemMeta(nextMeta);
            }
            inv.setItem(53, next);
        }

        plugin.getFoliaLib().getScheduler().runAtEntity(player, task -> player.openInventory(inv));
    }

    public void openBedrockItemAction(Player player, ShopCategory category, ShopItem item) {
        List<BedrockButton> buttons = new ArrayList<>();
        
        if (item.isCommand()) {
            buttons.add(new BedrockButton("§aAcheter (x1)\n§r§8" + String.format("%.2f", item.getCurrentBuyPrice()) + "$", org.bukkit.Material.EMERALD, p -> {
                buyItem(p, item, 1);
                openItemsGui(p, category);
            }));
        } else {
            buttons.add(new BedrockButton("§aAcheter (x1)\n§r§8" + String.format("%.2f", item.getCurrentBuyPrice()) + "$", org.bukkit.Material.EMERALD, p -> {
                buyItem(p, item, 1);
                openItemsGui(p, category);
            }));
            buttons.add(new BedrockButton("§aAcheter (x64)\n§r§8" + String.format("%.2f", item.getCurrentBuyPrice() * 64) + "$", org.bukkit.Material.EMERALD_BLOCK, p -> {
                buyItem(p, item, 64);
                openItemsGui(p, category);
            }));
            if (item.getBaseSellPrice() > 0) {
                buttons.add(new BedrockButton("§cVendre (x1)\n§r§8" + String.format("%.2f", item.getCurrentSellPrice()) + "$", org.bukkit.Material.REDSTONE, p -> {
                    sellItem(p, item, 1);
                    openItemsGui(p, category);
                }));
                buttons.add(new BedrockButton("§cVendre Tout\n§r§8Inventaire", org.bukkit.Material.REDSTONE_BLOCK, p -> {
                    sellAll(p, item);
                    openItemsGui(p, category);
                }));
            }
        }
        
        buttons.add(new BedrockButton("§cRetour\n§r§8Objets", org.bukkit.Material.BARRIER, p -> openItemsGui(p, category)));

        BedrockFormManager.openSimpleForm(player, "Action: " + item.getMaterial().name(), "Que voulez-vous faire ?", buttons);
    }

    private void sellAll(Player p, ShopItem item) {
        int count = 0;
        for (ItemStack invItem : p.getInventory().getContents()) {
            if (isSellableItem(invItem, item.getMaterial())) {
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
            long now = System.currentTimeMillis();
            Long last = clickDebounce.get(p.getUniqueId());
            if (last != null && now - last < 250) return;
            clickDebounce.put(p.getUniqueId(), now);

            int slot = event.getSlot();
            if (slot >= 0 && slot < categories.size()) {
                openItemsGui(p, categories.get(slot));
            }
        }
    }

    private class ShopItemsGuiHolder implements GensGuiHolder {
        private Inventory inventory;
        private final ShopCategory category;
        private final int page;

        public ShopItemsGuiHolder(ShopCategory category, int page) {
            this.category = category;
            this.page = page;
        }

        public void setInventory(Inventory inv) { this.inventory = inv; }
        @Override public Inventory getInventory() { return inventory; }

        @Override
        public void onClick(InventoryClickEvent event) {
            event.setCancelled(true);
            Player p = (Player) event.getWhoClicked();
            long now = System.currentTimeMillis();
            Long last = clickDebounce.get(p.getUniqueId());
            if (last != null && now - last < 250) return;
            clickDebounce.put(p.getUniqueId(), now);

            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || clicked.getType() == Material.AIR) return;

            if (event.getSlot() == 45 && clicked.getType() == Material.ARROW) {
                openItemsGui(p, category, page - 1);
                return;
            }

            if (event.getSlot() == 49 && clicked.getType() == Material.BARRIER) {
                openCategoryGui(p);
                return;
            }

            if (event.getSlot() == 53 && clicked.getType() == Material.ARROW) {
                openItemsGui(p, category, page + 1);
                return;
            }

            ShopItem shopItem = category.getItem(clicked.getType());
            if (shopItem != null) {
                int amount = (event.getClick() == org.bukkit.event.inventory.ClickType.SHIFT_LEFT || event.getClick() == org.bukkit.event.inventory.ClickType.SHIFT_RIGHT) ? 64 : 1;
                if (event.getClick() == org.bukkit.event.inventory.ClickType.LEFT || event.getClick() == org.bukkit.event.inventory.ClickType.SHIFT_LEFT) {
                    buyItem(p, shopItem, amount);
                    openItemsGui(p, category, page); // Refresh
                } else if (event.getClick() == org.bukkit.event.inventory.ClickType.RIGHT || event.getClick() == org.bukkit.event.inventory.ClickType.SHIFT_RIGHT) {
                    if (shopItem.isCommand()) {
                        plugin.getLangManager().sendMessage(p, "shopmodule.msg_3");
                        return;
                    }
                    sellItem(p, shopItem, amount);
                    openItemsGui(p, category, page); // Refresh
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
                // Sur Folia, dispatchCommand avec la console doit s'exécuter sur le GlobalRegionScheduler
                plugin.getFoliaLib().getScheduler().runNextTick((gt) -> {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
                });
                plugin.getLangManager().sendMessage(p, "shopmodule.buy_cmd_success",
                        net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.parsed("item", shopItem.getMaterial().name()));
            } else {
                Map<Integer, ItemStack> leftover = p.getInventory().addItem(new ItemStack(shopItem.getMaterial(), amount));
                if (!leftover.isEmpty()) {
                    for (ItemStack drop : leftover.values()) {
                        p.getWorld().dropItemNaturally(p.getLocation(), drop);
                    }
                    plugin.getLangManager().sendMessage(p, "shopmodule.inventory_full");
                }
                plugin.getLangManager().sendMessage(p, "shopmodule.buy_success",
                        net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.parsed("amount", String.valueOf(amount)),
                        net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.parsed("item", shopItem.getMaterial().name()),
                        net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.parsed("price", String.format("%.2f", totalCost)));
            }
            
            this.shopDAO.updateItemStockAsync(shopItem);
            logTransaction(shopItem);
            logPlayerTransaction(p.getUniqueId(), "ACHAT", shopItem.getMaterial().name(), amount, totalCost);
        } else {
            plugin.getLangManager().sendMessage(p, "shopmodule.msg_2");
        }
    }

    @SuppressWarnings("deprecation")
    private boolean isSellableItem(ItemStack item, Material requiredMaterial) {
        if (item == null || item.getType() != requiredMaterial) {
            return false;
        }
        if (item.hasItemMeta()) {
            ItemMeta meta = item.getItemMeta();
            if (meta.hasDisplayName() || meta.hasEnchants() || meta.hasLore() || meta.hasCustomModelData()) {
                return false;
            }
            if (meta instanceof Damageable dmg && dmg.hasDamage()) {
                return false;
            }
        }
        return true;
    }

    public void sellItem(Player p, ShopItem shopItem, int amount) {
        EconomyModule eco = (EconomyModule) plugin.getModuleManager().getModule("economy");
        if (eco == null) return;
        
        int playerHas = 0;
        for (ItemStack i : p.getInventory().getContents()) {
            if (isSellableItem(i, shopItem.getMaterial())) {
                playerHas += i.getAmount();
            }
        }

        if (playerHas >= amount) {
            double totalEarn = shopItem.getCurrentSellPrice() * amount;
            
            // Retirer l'item de l'inventaire
            int toRemove = amount;
            for (ItemStack i : p.getInventory().getContents()) {
                if (isSellableItem(i, shopItem.getMaterial())) {
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
            plugin.getLangManager().sendMessage(p, "shopmodule.sell_success",
                    net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.parsed("amount", String.valueOf(amount)),
                    net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.parsed("item", shopItem.getMaterial().name()),
                    net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.parsed("price", String.format("%.2f", totalEarn)));
            this.shopDAO.updateItemStockAsync(shopItem);
            logTransaction(shopItem);
            logPlayerTransaction(p.getUniqueId(), "VENTE", shopItem.getMaterial().name(), amount, totalEarn);
        } else {
            plugin.getLangManager().sendMessage(p, "shopmodule.msg_4");
        }
    }
}
