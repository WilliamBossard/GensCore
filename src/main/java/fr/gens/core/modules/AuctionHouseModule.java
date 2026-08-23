package fr.gens.core.modules;

import fr.gens.core.CorePlugin;
import fr.gens.core.modules.GuiModule.GensGuiHolder;
import fr.gens.core.utils.ItemSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import cloud.commandframework.annotations.Argument;
import cloud.commandframework.annotations.CommandMethod;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;





import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AuctionHouseModule implements Module {

    private final CorePlugin plugin;
    private boolean enabled = false;
    private fr.gens.core.database.AuctionHouseDAO ahDAO;

    public AuctionHouseModule(CorePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "auctionhouse";
    }

    @Override
    public String getDescription() {
        return "HÃƒÂ´tel de ventes entre joueurs";
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void initDatabase(fr.gens.core.utils.DatabaseManager dbManager) {
        dbManager.executeStatement("CREATE TABLE IF NOT EXISTS auction_house (id INTEGER PRIMARY KEY AUTOINCREMENT, seller_uuid VARCHAR(36) NOT NULL, seller_name VARCHAR(16) NOT NULL, price DOUBLE NOT NULL, item_data TEXT NOT NULL, expire_time BIGINT NOT NULL);");
        dbManager.executeStatement("CREATE INDEX IF NOT EXISTS idx_auction_expire ON auction_house(expire_time);");
        dbManager.executeStatement("CREATE INDEX IF NOT EXISTS idx_auction_seller ON auction_house(seller_uuid);");
    }

    @Override
    public void enable() {
        enabled = true;
        this.ahDAO = new fr.gens.core.database.AuctionHouseDAO(plugin);
        this.ahDAO.initDatabase();
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
    }

    @CommandMethod("ah")
    public void executeAh(org.bukkit.command.CommandSender sender) {
        if (!(sender instanceof org.bukkit.entity.Player)) return;
        org.bukkit.entity.Player p = (org.bukkit.entity.Player) sender;
        if (!enabled) {
            plugin.getLangManager().sendMessage(p, "auctionhousemodule.msg_1");
            return;
        }
        openAhGui(p, 0);
    }

    @CommandMethod("ah sell <price>")
    public void executeAhSell(org.bukkit.command.CommandSender sender, @Argument("price") double price) {
        if (!(sender instanceof org.bukkit.entity.Player)) return;
        org.bukkit.entity.Player p = (org.bukkit.entity.Player) sender;
        if (!enabled) {
            plugin.getLangManager().sendMessage(p, "auctionhousemodule.msg_1");
            return;
        }

        if (price <= 0) {
            plugin.getLangManager().sendMessage(p, "auctionhousemodule.msg_2");
            return;
        }

        ItemStack item = p.getInventory().getItemInMainHand();
        if (item == null || item.getType() == Material.AIR) {
            plugin.getLangManager().sendMessage(p, "auctionhousemodule.msg_3");
            return;
        }

        // Sauvegarde en base de données
        String base64Item = ItemSerializer.toBase64(item);
        long expireTime = System.currentTimeMillis() + (1000L * 60 * 60 * 24 * 7); // 7 jours

        plugin.getFoliaLib().getImpl().runAsync((wrappedTask) -> {
            try {
                ahDAO.addAuction(p.getUniqueId().toString(), p.getName(), price, base64Item, expireTime);
                plugin.getFoliaLib().getImpl().runAtEntity(p, (t2) -> {
                    p.getInventory().setItemInMainHand(null);
                    p.sendMessage(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize("<green>Objet mis en vente pour <yellow>" + price + " $ <green>!"));
                    Bukkit.broadcast(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize("<yellow>[AH] <white>" + p.getName() + " <green>vient de mettre un objet en vente pour <yellow>" + price + " $ <green>!"));
                });
            } catch (Exception e) {
                plugin.getFoliaLib().getImpl().runAtEntity(p, (t2) -> plugin.getLangManager().sendMessage(p, "auctionhousemodule.msg_5"));
                e.printStackTrace();
            }
        });
    }

    private void openAhGui(Player p, int page) {
        plugin.getFoliaLib().getImpl().runAsync((wrappedTask) -> {
            List<AhItem> items = ahDAO.getAuctions(45, page * 45);

            plugin.getFoliaLib().getImpl().runAtEntity(p, (t2) -> {
                AhGuiHolder holder = new AhGuiHolder(page);
                Inventory inv = Bukkit.createInventory(holder, 54, net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize("<dark_gray>HÃƒÂ´tel de Ventes (Page " + (page + 1) + ")"));
                holder.setInventory(inv);

                int slot = 0;
                for (AhItem ahItem : items) {
                    ItemStack item = ItemSerializer.fromBase64(ahItem.itemData);
                    if (item != null) {
                        ItemMeta meta = item.getItemMeta();
                        if (meta != null) {
                            List<net.kyori.adventure.text.Component> lore = new ArrayList<>();
                            if (meta.hasLore()) {
                                lore = new ArrayList<>(java.util.Objects.requireNonNull(meta.lore()));
                            }
                            lore.add(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize("<dark_gray>----------------------"));
                            lore.add(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize("<gray>Vendeur : <white>" + ahItem.sellerName));
                            lore.add(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize("<gray>Prix : <yellow>" + String.format("%.2f", ahItem.price) + " $"));
                            if (ahItem.sellerUuid.equals(p.getUniqueId().toString())) {
                                lore.add(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize("<red>Ã¢â€“Â¶ Cliquez pour annuler la vente"));
                            } else {
                                lore.add(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize("<green>Ã¢â€“Â¶ Cliquez pour acheter"));
                            }
                            meta.lore(lore);
                            item.setItemMeta(meta);
                        }
                        inv.setItem(slot++, item);
                        holder.addItemMapping(slot - 1, ahItem);
                    }
                }

                // Boutons de pagination (simplifiÃƒÂ© pour l'instant)
                ItemStack info = new ItemStack(Material.PAPER);
                ItemMeta infoMeta = info.getItemMeta();
                infoMeta.displayName(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize("<yellow>Page " + (page + 1)));
                info.setItemMeta(infoMeta);
                inv.setItem(49, info);

                p.openInventory(inv);
            });
        });
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
                    plugin.getFoliaLib().getImpl().runAsync((wrappedTask) -> {
                        if (deleteFromDb(ahItem.id)) {
                            plugin.getFoliaLib().getImpl().runAtEntity(p, (t2) -> {
                                ItemStack originalItem = ItemSerializer.fromBase64(ahItem.itemData);
                                p.getInventory().addItem(originalItem);
                                plugin.getLangManager().sendMessage(p, "auctionhousemodule.msg_6");
                                openAhGui(p, page); // Refresh
                            });
                        }
                    });
                } else {
                    // Acheter
                    plugin.getFoliaLib().getImpl().runAtEntity(p, (t1) -> {
                        if (eco.takeMoneyAtomic(p.getUniqueId(), ahItem.price)) {
                            // On a réservé l'argent atomiquement !
                            
                            // Ensuite on supprime asynchroniquement de la BDD
                            plugin.getFoliaLib().getImpl().runAsync((t2) -> {
                                if (deleteFromDb(ahItem.id)) { // Achat confirmé
                                    plugin.getFoliaLib().getImpl().runAtEntity(p, (t3) -> {
                                        double taxRate = plugin.getConfigManager().getConfig("modules/economy.yml").getDouble("ah.tax_percentage", 0.0) / 100.0;
                                        double taxAmount = ahItem.price * taxRate;
                                        double sellerProfit = ahItem.price - taxAmount;
                                        eco.giveMoney(UUID.fromString(ahItem.sellerUuid), sellerProfit);
                                        
                                        ItemStack originalItem = ItemSerializer.fromBase64(ahItem.itemData);
                                        p.getInventory().addItem(originalItem);
                                        p.sendMessage(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize("<green>Vous avez achetÃ© un objet Ã  <yellow>" + ahItem.sellerName + " <green>pour <yellow>" + ahItem.price + " $ <green>!"));
                                        
                                        Player seller = Bukkit.getPlayer(UUID.fromString(ahItem.sellerUuid));
                                        if (seller != null && seller.isOnline()) {
                                            if (taxAmount > 0) {
                                                seller.sendMessage(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize("<green>Un joueur a achetÃ© votre objet sur l'HÃ´tel de Ventes ! Vous gagnez <yellow>" + String.format("%.2f", sellerProfit) + " $ <dark_gray>(Taxe: -" + String.format("%.2f", taxAmount) + " $)"));
                                            } else {
                                                seller.sendMessage(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize("<green>Un joueur a achetÃ© votre objet sur l'HÃ´tel de Ventes pour <yellow>" + ahItem.price + " $ <green>!"));
                                            }
                                        }
                                        openAhGui(p, page); // Refresh
                                    });
                                } else {
                                    // Quelqu'un d'autre l'a achetÃ© juste avant ! On rembourse
                                    plugin.getFoliaLib().getImpl().runAtEntity(p, (t4) -> {
                                        eco.giveMoney(p.getUniqueId(), ahItem.price);
                                        plugin.getLangManager().sendMessage(p, "auctionhousemodule.msg_7");
                                        openAhGui(p, page);
                                    });
                                }
                            });
                        } else {
                            plugin.getLangManager().sendMessage(p, "auctionhousemodule.msg_8");
                        }
                    });
                }
            }
        }
    }

    private boolean deleteFromDb(int id) {
        return ahDAO.deleteAuction(id);
    }

    // --- WEB EXTENSION ---
    public java.util.List<java.util.Map<String, Object>> getAuctionItemsForWeb() {
        return ahDAO.getAuctionItemsForWeb();
    }

    public static class AhItem {
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
