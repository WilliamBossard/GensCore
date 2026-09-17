package fr.gens.core.database;

import fr.gens.core.CorePlugin;
import fr.gens.core.modules.AuctionHouseModule.AhItem;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class AuctionHouseDAO {

    private final CorePlugin plugin;

    public AuctionHouseDAO(CorePlugin plugin) {
        this.plugin = plugin;
    }

    public void initDatabase() {
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             Statement stmt = conn.createStatement()) {
            
            stmt.execute("CREATE TABLE IF NOT EXISTS auction_house (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "seller_uuid VARCHAR(36) NOT NULL, " +
                    "seller_name VARCHAR(50) NOT NULL, " +
                    "price DOUBLE NOT NULL, " +
                    "item_data TEXT NOT NULL, " +
                    "expire_time BIGINT NOT NULL" +
                    ");");
                    
        } catch (SQLException e) {
            plugin.getLogger().log(java.util.logging.Level.SEVERE, "Erreur lors de la création de la table auction_house", e);
        }
    }

    public void addAuction(String sellerUuid, String sellerName, double price, String itemData, long expireTime) {
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO auction_house (seller_uuid, seller_name, price, item_data, expire_time) VALUES (?, ?, ?, ?, ?)")) {
            ps.setString(1, sellerUuid);
            ps.setString(2, sellerName);
            ps.setDouble(3, price);
            ps.setString(4, itemData);
            ps.setLong(5, expireTime);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<AhItem> getAuctions(int limit, int offset) {
        List<AhItem> items = new ArrayList<>();
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT * FROM auction_house ORDER BY id DESC LIMIT ? OFFSET ?")) {
            ps.setInt(1, limit);
            ps.setInt(2, offset);
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
        return items;
    }

    public boolean deleteAuction(int id) {
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

    public AhItem getAuction(int id) {
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT id, seller_uuid, seller_name, price, item_data, expire_time FROM auction_house WHERE id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new AhItem(
                            rs.getInt("id"),
                            rs.getString("seller_uuid"),
                            rs.getString("seller_name"),
                            rs.getDouble("price"),
                            rs.getString("item_data"),
                            rs.getLong("expire_time")
                    );
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<Map<String, Object>> getAuctionItemsForWeb() {
        List<Map<String, Object>> ahItems = new ArrayList<>();
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT id, seller_uuid, seller_name, price, expire_time, item_data FROM auction_house ORDER BY id DESC LIMIT 100")) {
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> ahItem = new HashMap<>();
                    ahItem.put("id", rs.getInt("id"));
                    ahItem.put("sellerUuid", rs.getString("seller_uuid"));
                    ahItem.put("sellerName", rs.getString("seller_name"));
                    ahItem.put("price", rs.getDouble("price"));
                    ahItem.put("expireTime", rs.getLong("expire_time"));
                    
                    String itemData = rs.getString("item_data");
                    try {
                        org.bukkit.inventory.ItemStack item = fr.gens.core.utils.ItemSerializer.fromBase64(itemData);
                        if (item != null) {
                            ahItem.put("material", item.getType().name());
                            ahItem.put("amount", item.getAmount());
                            if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
                                ahItem.put("displayName", net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(item.getItemMeta().displayName()));
                            }

                            List<String> enchants = new ArrayList<>();
                            if (item.hasItemMeta()) {
                                org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
                                if (meta.hasEnchants()) {
                                    for (Map.Entry<org.bukkit.enchantments.Enchantment, Integer> entry : meta.getEnchants().entrySet()) {
                                        enchants.add(formatEnchantmentName(entry.getKey().getKey().getKey(), entry.getValue()));
                                    }
                                }
                                if (meta instanceof org.bukkit.inventory.meta.EnchantmentStorageMeta) {
                                    org.bukkit.inventory.meta.EnchantmentStorageMeta bookMeta = (org.bukkit.inventory.meta.EnchantmentStorageMeta) meta;
                                    for (Map.Entry<org.bukkit.enchantments.Enchantment, Integer> entry : bookMeta.getStoredEnchants().entrySet()) {
                                        enchants.add(formatEnchantmentName(entry.getKey().getKey().getKey(), entry.getValue()));
                                    }
                                }
                                if (meta.hasLore() && meta.lore() != null) {
                                    List<String> loreLines = new ArrayList<>();
                                    for (net.kyori.adventure.text.Component line : meta.lore()) {
                                        loreLines.add(net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(line));
                                    }
                                    ahItem.put("lore", loreLines);
                                }
                            }
                            ahItem.put("enchantments", enchants);
                            ahItem.put("isEnchanted", !enchants.isEmpty());
                        }
                    } catch (Exception ignored) {
                        ahItem.put("material", "UNKNOWN");
                    }

                    ahItems.add(ahItem);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return ahItems;
    }

    private String formatEnchantmentName(String key, int level) {
        if (key == null || key.isEmpty()) return "Enchantment";
        String name = key.replace('_', ' ');
        name = Character.toUpperCase(name.charAt(0)) + name.substring(1);
        String roman = toRoman(level);
        return roman.isEmpty() ? name : name + " " + roman;
    }

    private String toRoman(int n) {
        switch (n) {
            case 1: return "I";
            case 2: return "II";
            case 3: return "III";
            case 4: return "IV";
            case 5: return "V";
            default: return n > 0 ? String.valueOf(n) : "";
        }
    }
}


