package fr.gens.core.modules.shop;

import fr.gens.core.CorePlugin;
import fr.gens.core.modules.EconomyModule;
import fr.gens.core.modules.GuiModule.GensGuiHolder;
import fr.gens.core.modules.Module;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ShopModule implements Module, CommandExecutor {

    private final CorePlugin plugin;
    private boolean enabled = false;
    private List<ShopCategory> categories;

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

    @Override
    public void enable() {
        enabled = true;
        loadShop();
        plugin.getLogger().info("[DynamicShop] Activé.");
    }



    @Override
    public void disable() {
        enabled = false;
        saveShop();
        plugin.getLogger().info("[DynamicShop] Désactivé.");
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
        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            // Charger les catégories
            try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM shop_categories");
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ShopCategory cat = new ShopCategory(
                            rs.getString("id"),
                            rs.getString("displayName"),
                            Material.valueOf(rs.getString("icon"))
                    );
                    categories.add(cat);
                }
            }

            // Charger les items
            try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM shop_items");
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ShopCategory cat = getCategory(rs.getString("category_id"));
                    if (cat != null) {
                        ShopItem item = new ShopItem(
                                Material.valueOf(rs.getString("material")),
                                rs.getDouble("buyPrice"),
                                rs.getDouble("sellPrice")
                        );
                        item.setStock(rs.getInt("stock"));
                        item.setTargetStock(rs.getInt("targetStock"));
                        item.setCommand(rs.getBoolean("isCommand"));
                        item.setCommandToExecute(rs.getString("commandToExecute"));
                        item.setEnabled(rs.getBoolean("isEnabled"));
                        cat.addItem(item);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void saveShop() {
        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            for (ShopCategory cat : categories) {
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO shop_categories (id, displayName, icon) VALUES (?, ?, ?) " +
                        "ON CONFLICT(id) DO UPDATE SET displayName=excluded.displayName, icon=excluded.icon")) {
                    ps.setString(1, cat.getId());
                    ps.setString(2, cat.getDisplayName());
                    ps.setString(3, cat.getIcon().name());
                    ps.executeUpdate();
                }

                for (ShopItem item : cat.getItems()) {
                    try (PreparedStatement ps = conn.prepareStatement(
                            "INSERT INTO shop_items (material, category_id, buyPrice, sellPrice, stock, targetStock, isCommand, commandToExecute, isEnabled) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                            "ON CONFLICT(material) DO UPDATE SET category_id=excluded.category_id, buyPrice=excluded.buyPrice, " +
                            "sellPrice=excluded.sellPrice, stock=excluded.stock, targetStock=excluded.targetStock, " +
                            "isCommand=excluded.isCommand, commandToExecute=excluded.commandToExecute, isEnabled=excluded.isEnabled")) {
                        ps.setString(1, item.getMaterial().name());
                        ps.setString(2, cat.getId());
                        ps.setDouble(3, item.getBaseBuyPrice());
                        ps.setDouble(4, item.getBaseSellPrice());
                        ps.setInt(5, item.getStock());
                        ps.setInt(6, item.getTargetStock());
                        ps.setBoolean(7, item.isCommand());
                        ps.setString(8, item.getCommandToExecute());
                        ps.setBoolean(9, item.isEnabled());
                        ps.executeUpdate();
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void logTransaction(ShopItem item) {
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO shop_history (material, timestamp, buyPrice, sellPrice, stock) VALUES (?, ?, ?, ?, ?)")) {
            ps.setString(1, item.getMaterial().name());
            ps.setLong(2, System.currentTimeMillis());
            ps.setDouble(3, item.getCurrentBuyPrice());
            ps.setDouble(4, item.getCurrentSellPrice());
            ps.setInt(5, item.getStock());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void logPlayerTransaction(UUID uuid, String type, String material, int amount, double price) {
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO player_transactions_history (uuid, type, material, amount, price, timestamp) VALUES (?, ?, ?, ?, ?, ?)"
             )) {
            ps.setString(1, uuid.toString());
            ps.setString(2, type);
            ps.setString(3, material);
            ps.setInt(4, amount);
            ps.setDouble(5, price);
            ps.setLong(6, System.currentTimeMillis());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!enabled) {
            sender.sendMessage("§cLe shop est désactivé.");
            return true;
        }
        if (!(sender instanceof Player)) return true;
        Player p = (Player) sender;

        if (command.getName().equalsIgnoreCase("shop")) {
            openCategoryGui(p);
            return true;
        }
        return false;
    }

    public void openCategoryGui(Player player) {
        ShopCategoryGuiHolder holder = new ShopCategoryGuiHolder();
        int size = Math.max(9, (int) (Math.ceil(categories.size() / 9.0) * 9));
        Inventory inv = Bukkit.createInventory(holder, size, "§8Boutique - Catégories");
        holder.setInventory(inv);

        for (int i = 0; i < categories.size(); i++) {
            ShopCategory cat = categories.get(i);
            ItemStack item = new ItemStack(cat.getIcon());
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName("§a§l" + cat.getDisplayName());
                List<String> lore = new ArrayList<>();
                lore.add("§7" + cat.getItems().size() + " objets disponibles.");
                lore.add("§eCliquez pour ouvrir !");
                meta.setLore(lore);
                item.setItemMeta(meta);
            }
            inv.setItem(i, item);
        }

        player.openInventory(inv);
    }

    public void openItemsGui(Player player, ShopCategory category) {
        ShopItemsGuiHolder holder = new ShopItemsGuiHolder(category);
        Inventory inv = Bukkit.createInventory(holder, 54, "§8Shop - " + category.getDisplayName());
        holder.setInventory(inv);

        int slot = 0;
        for (ShopItem item : category.getItems()) {
            if (!item.isEnabled()) continue;
            if (slot >= 45) break; // Pagination plus tard
            ItemStack i = new ItemStack(item.getMaterial());
            ItemMeta meta = i.getItemMeta();
            if (meta != null) {
                meta.setDisplayName("§f§l" + item.getMaterial().name());
                List<String> lore = new ArrayList<>();
                lore.add("§8Prix Dynamique (Inflation)");
                lore.add("");
                if (item.isCommand()) {
                lore.add("§a▶ Achat Unique : §e" + String.format("%.2f", item.getCurrentBuyPrice()) + " $");
                lore.add("§8(Exécute une commande sur votre compte)");
                lore.add("");
                lore.add("§eClic Gauche pour Acheter");
            } else {
                lore.add("§a▶ Achat (x1) : §e" + String.format("%.2f", item.getCurrentBuyPrice()) + " $");
                lore.add("§c◀ Vente (x1) : §e" + String.format("%.2f", item.getCurrentSellPrice()) + " $");
                lore.add("");
                lore.add("§7Stock du Serveur: " + item.getStock() + " (Cible: " + item.getTargetStock() + ")");
                lore.add("");
                lore.add("§eClic Gauche pour Acheter");
                lore.add("§eClic Droit pour Vendre");
                lore.add("§8(Shift pour x64)");
            }
                meta.setLore(lore);
                i.setItemMeta(meta);
            }
            inv.setItem(slot++, i);
        }

        // Bouton retour
        ItemStack back = new ItemStack(Material.BARRIER);
        ItemMeta backMeta = back.getItemMeta();
        backMeta.setDisplayName("§c§lRetour aux catégories");
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
                    if (eco.getBalance(p.getUniqueId()) >= totalCost) {
                        eco.takeMoney(p.getUniqueId(), totalCost);
                        shopItem.setStock(Math.max(0, shopItem.getStock() - amount));
                        
                        if (shopItem.isCommand()) {
                            String cmd = shopItem.getCommandToExecute().replace("%player%", p.getName());
                            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
                            p.sendMessage("§aAchat validé ! Vous avez obtenu le contenu de §e" + shopItem.getMaterial().name());
                        } else {
                            p.getInventory().addItem(new ItemStack(shopItem.getMaterial(), amount));
                            p.sendMessage("§aAchat de " + amount + "x " + shopItem.getMaterial().name() + " pour §e" + String.format("%.2f", totalCost) + " $");
                        }
                        
                        openItemsGui(p, category); // Refresh
                        saveShop();
                        logTransaction(shopItem);
                        logPlayerTransaction(p.getUniqueId(), "ACHAT", shopItem.getMaterial().name(), amount, totalCost);
                    } else {
                        p.sendMessage("§cVous n'avez pas assez d'argent.");
                    }
                } else {
                    if (shopItem.isCommand()) {
                        p.sendMessage("§cVous ne pouvez pas vendre cet objet.");
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
                        
                        p.sendMessage("§aVente de " + amount + "x " + shopItem.getMaterial().name() + " pour §e" + String.format("%.2f", totalEarn) + " $");
                        openItemsGui(p, category); // Refresh
                        saveShop();
                        logTransaction(shopItem);
                        logPlayerTransaction(p.getUniqueId(), "VENTE", shopItem.getMaterial().name(), amount, totalEarn);
                    } else {
                        p.sendMessage("§cVous n'avez pas assez d'objets à vendre.");
                    }
                }
            }
        }
    }
}
