package fr.gens.core.modules;

import fr.gens.core.CorePlugin;
import fr.gens.core.modules.EconomyModule;
import fr.gens.core.modules.GuiModule.GensGuiHolder;
import fr.gens.core.utils.ItemSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AuctionHouseModule implements Module, CommandExecutor {

    private final CorePlugin plugin;
    private boolean enabled = false;

    public AuctionHouseModule(CorePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "auctionhouse";
    }

    @Override
    public String getDescription() {
        return "Hôtel de ventes entre joueurs";
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void enable() {
        enabled = true;
    }

    @Override
    public void registerCommands(fr.gens.core.CorePlugin plugin) {
        org.bukkit.command.PluginCommand ahCmd = plugin.getCommand("ah");
        if (ahCmd != null) ahCmd.setExecutor(this);
    }

    @Override
    public void disable() {
        enabled = false;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!enabled) {
            plugin.getLangManager().sendMessage(sender, "auctionhousemodule.msg_1");
            return true;
        }

        if (!(sender instanceof Player)) return true;
        Player p = (Player) sender;

        if (args.length == 2 && args[0].equalsIgnoreCase("sell")) {
            try {
                double price = Double.parseDouble(args[1]);
                if (price <= 0) {
                    plugin.getLangManager().sendMessage(p, "auctionhousemodule.msg_2");
                    return true;
                }

                ItemStack item = p.getInventory().getItemInMainHand();
                if (item == null || item.getType() == Material.AIR) {
                    plugin.getLangManager().sendMessage(p, "auctionhousemodule.msg_3");
                    return true;
                }

                // Sauvegarde en base de données
                String base64Item = ItemSerializer.toBase64(item);
                long expireTime = System.currentTimeMillis() + (1000L * 60 * 60 * 24 * 7); // 7 jours

                try (Connection conn = plugin.getDatabaseManager().getConnection();
                     PreparedStatement ps = conn.prepareStatement(
                             "INSERT INTO auction_house (seller_uuid, seller_name, price, item_data, expire_time) VALUES (?, ?, ?, ?, ?)")) {
                    ps.setString(1, p.getUniqueId().toString());
                    ps.setString(2, p.getName());
                    ps.setDouble(3, price);
                    ps.setString(4, base64Item);
                    ps.setLong(5, expireTime);
                    ps.executeUpdate();
                }

                p.getInventory().setItemInMainHand(null);
                p.sendMessage("<green>Objet mis en vente pour <yellow>" + price + " $ <green>!");
                Bukkit.broadcastMessage("<yellow>[AH] <white>" + p.getName() + " <green>vient de mettre un objet en vente pour <yellow>" + price + " $ <green>!");

            } catch (NumberFormatException e) {
                plugin.getLangManager().sendMessage(p, "auctionhousemodule.msg_4");
            } catch (SQLException e) {
                plugin.getLangManager().sendMessage(p, "auctionhousemodule.msg_5");
                e.printStackTrace();
            }
            return true;
        } else {
            openAhGui(p, 0);
            return true;
        }
    }

    private void openAhGui(Player p, int page) {
        AhGuiHolder holder = new AhGuiHolder(page);
        Inventory inv = Bukkit.createInventory(holder, 54, net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize("<dark_gray>Hôtel de Ventes (Page " + (page + 1)) + ")");
        holder.setInventory(inv);

        List<AhItem> items = new ArrayList<>();

        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT * FROM auction_house ORDER BY id DESC LIMIT ? OFFSET ?")) {
            ps.setInt(1, 45); // 45 items max par page
            ps.setInt(2, page * 45);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    items.add(new AhItem(
                            rs.getInt("id"),
                            rs.getString("seller_uuid"),
                            rs.getString("seller_name"),
                            rs.getDouble("price"),
                            rs.getString("item_data"),
                            rs.getLong("expire_time")
                    ));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        int slot = 0;
        for (AhItem ahItem : items) {
            ItemStack item = ItemSerializer.fromBase64(ahItem.itemData);
            if (item != null) {
                ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    List<String> lore = meta.getLore();
                    if (lore == null) lore = new ArrayList<>();
                    lore.add("<dark_gray>----------------------");
                    lore.add("<gray>Vendeur : <white>" + ahItem.sellerName);
                    lore.add("<gray>Prix : <yellow>" + String.format("%.2f", ahItem.price) + " $");
                    if (ahItem.sellerUuid.equals(p.getUniqueId().toString())) {
                        lore.add("<red>▶ Cliquez pour annuler la vente");
                    } else {
                        lore.add("<green>▶ Cliquez pour acheter");
                    }
                    meta.lore(java.util.Optional.ofNullable(lore).orElse(java.util.Collections.emptyList()).stream().map(s -> net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize((String)s)).collect(java.util.stream.Collectors.toList()));
                    item.setItemMeta(meta);
                }
                inv.setItem(slot++, item);
                holder.addItemMapping(slot - 1, ahItem);
            }
        }

        // Boutons de pagination (simplifié pour l'instant)
        ItemStack info = new ItemStack(Material.PAPER);
        ItemMeta infoMeta = info.getItemMeta();
        infoMeta.displayName(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize("<yellow>Page " + (page + 1)));
        info.setItemMeta(infoMeta);
        inv.setItem(49, info);

        p.openInventory(inv);
    }

    private class AhGuiHolder implements GensGuiHolder {
        private Inventory inventory;
        private final int page;
        private final java.util.Map<Integer, AhItem> slotMap = new java.util.HashMap<>();

        public AhGuiHolder(int page) {
            this.page = page;
        }

        public void addItemMapping(int slot, AhItem item) {
            slotMap.put(slot, item);
        }

        public void setInventory(Inventory inv) { this.inventory = inv; }
        @Override public Inventory getInventory() { return inventory; }

        @Override
        public void onClick(InventoryClickEvent event) {
            event.setCancelled(true);
            Player p = (Player) event.getWhoClicked();
            int slot = event.getSlot();

            AhItem ahItem = slotMap.get(slot);
            if (ahItem != null) {
                EconomyModule eco = (EconomyModule) plugin.getModuleManager().getModule("economy");
                if (eco == null) return;

                if (ahItem.sellerUuid.equals(p.getUniqueId().toString())) {
                    // Annuler la vente
                    if (deleteFromDb(ahItem.id)) {
                        ItemStack originalItem = ItemSerializer.fromBase64(ahItem.itemData);
                        p.getInventory().addItem(originalItem);
                        plugin.getLangManager().sendMessage(p, "auctionhousemodule.msg_6");
                        openAhGui(p, page); // Refresh
                    }
                } else {
                    // Acheter
                    if (eco.getBalance(p.getUniqueId()) >= ahItem.price) {
                        if (deleteFromDb(ahItem.id)) { // S'assurer qu'il n'a pas déjà été acheté
                            eco.takeMoney(p.getUniqueId(), ahItem.price);
                            
                            double taxRate = plugin.getConfig().getDouble("ah.tax_percentage", 0.0) / 100.0;
                            double taxAmount = ahItem.price * taxRate;
                            double sellerProfit = ahItem.price - taxAmount;
                            eco.giveMoney(UUID.fromString(ahItem.sellerUuid), sellerProfit);
                            
                            ItemStack originalItem = ItemSerializer.fromBase64(ahItem.itemData);
                            p.getInventory().addItem(originalItem);
                            p.sendMessage("<green>Vous avez acheté un objet à <yellow>" + ahItem.sellerName + " <green>pour <yellow>" + ahItem.price + " $ <green>!");
                            
                            Player seller = Bukkit.getPlayer(UUID.fromString(ahItem.sellerUuid));
                            if (seller != null && seller.isOnline()) {
                                if (taxAmount > 0) {
                                    seller.sendMessage("<green>Un joueur a acheté votre objet sur l'Hôtel de Ventes ! Vous gagnez <yellow>" + String.format("%.2f", sellerProfit) + " $ <dark_gray>(Taxe: -" + String.format("%.2f", taxAmount) + " $)");
                                } else {
                                    seller.sendMessage("<green>Un joueur a acheté votre objet sur l'Hôtel de Ventes pour <yellow>" + ahItem.price + " $ <green>!");
                                }
                            }
                            openAhGui(p, page); // Refresh
                        } else {
                            plugin.getLangManager().sendMessage(p, "auctionhousemodule.msg_7");
                            openAhGui(p, page);
                        }
                    } else {
                        plugin.getLangManager().sendMessage(p, "auctionhousemodule.msg_8");
                    }
                }
            }
        }
    }

    private boolean deleteFromDb(int id) {
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM auction_house WHERE id = ?")) {
            ps.setInt(1, id);
            int affected = ps.executeUpdate();
            return affected > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // --- WEB EXTENSION ---
    public java.util.List<java.util.Map<String, Object>> getAuctionItemsForWeb() {
        java.util.List<java.util.Map<String, Object>> ahItems = new java.util.ArrayList<>();
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT id, seller_name, price, expire_time FROM auction_house ORDER BY id DESC LIMIT 100")) {
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    java.util.Map<String, Object> ahItem = new java.util.HashMap<>();
                    ahItem.put("id", rs.getInt("id"));
                    ahItem.put("sellerName", rs.getString("seller_name"));
                    ahItem.put("price", rs.getDouble("price"));
                    ahItem.put("expireTime", rs.getLong("expire_time"));
                    ahItems.add(ahItem);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return ahItems;
    }

    private static class AhItem {
        public int id;
        public String sellerUuid;
        public String sellerName;
        public double price;
        public String itemData;
        public long expireTime;

        public AhItem(int id, String sellerUuid, String sellerName, double price, String itemData, long expireTime) {
            this.id = id;
            this.sellerUuid = sellerUuid;
            this.sellerName = sellerName;
            this.price = price;
            this.itemData = itemData;
            this.expireTime = expireTime;
        }
    }
}
