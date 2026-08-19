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
}
