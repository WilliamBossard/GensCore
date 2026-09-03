package fr.gens.core.database;

import fr.gens.core.CorePlugin;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.LinkedHashMap;


public class EconomyDAO {

    private final CorePlugin plugin;

    public EconomyDAO(CorePlugin plugin) {
        this.plugin = plugin;
    }

    public void initDatabase() {
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             java.sql.Statement stmt = conn.createStatement()) {
            
            stmt.execute("CREATE TABLE IF NOT EXISTS players_economy (" +
                    "uuid VARCHAR(36) PRIMARY KEY, " +
                    "balance DOUBLE NOT NULL DEFAULT 0.0" +
                    ");");
                    
        } catch (SQLException e) {
            plugin.getLogger().log(java.util.logging.Level.SEVERE, "Erreur lors de la création de la table economy", e);
        }
    }

    public Map<UUID, Double> loadBalances() {
        Map<UUID, Double> balances = new HashMap<>();
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT uuid, balance FROM players_economy");
             ResultSet rs = ps.executeQuery()) {
            
            while (rs.next()) {
                balances.put(UUID.fromString(rs.getString("uuid")), rs.getDouble("balance"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return balances;
    }

    public double getBalance(UUID uuid) {
        double balance = 0.0;
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT balance FROM players_economy WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    balance = rs.getDouble("balance");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return balance;
    }

    public void savePlayerBalance(UUID uuid, double balance) {
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO players_economy (uuid, balance) VALUES (?, ?) " +
                     "ON CONFLICT(uuid) DO UPDATE SET balance=excluded.balance")) {
            ps.setString(1, uuid.toString());
            ps.setDouble(2, balance);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public Map<UUID, Double> getTopBalances(int limit) {
        Map<UUID, Double> top = new LinkedHashMap<>();
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT uuid, balance FROM players_economy ORDER BY balance DESC LIMIT ?")) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    top.put(UUID.fromString(rs.getString("uuid")), rs.getDouble("balance"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return top;
    }
    
    public double getTotalMoney() {
        double total = 0.0;
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT SUM(balance) AS total FROM players_economy");
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                total = rs.getDouble("total");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return total;
    }
}



