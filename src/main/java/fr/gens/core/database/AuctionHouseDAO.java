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

    public List<Map<String, Object>> getAuctionItemsForWeb() {
        List<Map<String, Object>> ahItems = new ArrayList<>();
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT id, seller_name, price, expire_time, item_data FROM auction_house ORDER BY id DESC LIMIT 100")) {
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> ahItem = new HashMap<>();
                    ahItem.put("id", rs.getInt("id"));
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
}


