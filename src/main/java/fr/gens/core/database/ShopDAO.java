package fr.gens.core.database;

import fr.gens.core.CorePlugin;
import fr.gens.core.modules.shop.ShopCategory;
import fr.gens.core.modules.shop.ShopItem;
import org.bukkit.Material;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;


public class ShopDAO {

    private final CorePlugin plugin;

    public ShopDAO(CorePlugin plugin) {
        this.plugin = plugin;
    }

    public void initDatabase() {
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             java.sql.Statement stmt = conn.createStatement()) {
            
            stmt.execute("CREATE TABLE IF NOT EXISTS shop_categories (" +
                    "id VARCHAR(50) PRIMARY KEY, " +
                    "displayName VARCHAR(255), " +
                    "icon VARCHAR(50)" +
                    ");");

            stmt.execute("CREATE TABLE IF NOT EXISTS shop_items (" +
                    "material VARCHAR(50) PRIMARY KEY, " +
                    "category_id VARCHAR(50), " +
                    "buyPrice DOUBLE, " +
                    "sellPrice DOUBLE, " +
                    "stock INTEGER, " +
                    "targetStock INTEGER, " +
                    "isCommand BOOLEAN, " +
                    "commandToExecute VARCHAR(255), " +
                    "isEnabled BOOLEAN" +
                    ");");

            stmt.execute("CREATE TABLE IF NOT EXISTS shop_history (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "material VARCHAR(50), " +
                    "timestamp BIGINT, " +
                    "buyPrice DOUBLE, " +
                    "sellPrice DOUBLE, " +
                    "stock INTEGER" +
                    ");");
            
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_shop_history_material  ON shop_history(material);");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_shop_history_timestamp ON shop_history(timestamp);");

        } catch (SQLException e) {
            plugin.getLogger().log(java.util.logging.Level.SEVERE, "Erreur lors de la crÃƒÆ’Ã‚Â©ation des tables du shop", e);
        }
    }

    public List<ShopCategory> loadShopCategories() {
        List<ShopCategory> categories = new ArrayList<>();
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM shop_categories");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                categories.add(new ShopCategory(
                        rs.getString("id"),
                        rs.getString("displayName"),
                        Material.valueOf(rs.getString("icon"))
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return categories;
    }

    public void loadShopItems(List<ShopCategory> categories) {
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM shop_items");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String catId = rs.getString("category_id");
                ShopCategory targetCat = null;
                for (ShopCategory cat : categories) {
                    if (cat.getId().equalsIgnoreCase(catId)) {
                        targetCat = cat;
                        break;
                    }
                }
                if (targetCat != null) {
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
                    targetCat.addItem(item);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void saveShop(List<ShopCategory> categories) {
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

    public List<Map<String, Object>> getHistory(String material) {
        List<Map<String, Object>> history = new ArrayList<>();
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT timestamp, buyPrice, sellPrice, stock FROM shop_history WHERE material = ? ORDER BY timestamp ASC LIMIT 50")) {
            ps.setString(1, material);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> point = new HashMap<>();
                    point.put("timestamp", rs.getLong("timestamp"));
                    point.put("buyPrice", rs.getDouble("buyPrice"));
                    point.put("sellPrice", rs.getDouble("sellPrice"));
                    point.put("stock", rs.getInt("stock"));
                    history.add(point);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return history;
    }

    public boolean deleteItem(String categoryId, String materialName) {
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM shop_items WHERE material = ? AND category_id = ?")) {
            ps.setString(1, materialName);
            ps.setString(2, categoryId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean deleteCategory(String categoryId) {
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM shop_categories WHERE id = ?")) {
            ps.setString(1, categoryId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
}

