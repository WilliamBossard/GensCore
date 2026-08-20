package fr.gens.core.database;

import fr.gens.core.CorePlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;


public class PendingCommandDAO {

    private final CorePlugin plugin;

    public PendingCommandDAO(CorePlugin plugin) {
        this.plugin = plugin;
    }

    public void initDatabase() {
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             Statement stmt = conn.createStatement()) {
            
            stmt.execute("CREATE TABLE IF NOT EXISTS pending_rewards (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "uuid VARCHAR(36) NOT NULL, " +
                    "command TEXT, " +
                    "message TEXT" +
                    ");");
                    
        } catch (SQLException e) {
            plugin.getLogger().log(java.util.logging.Level.SEVERE, "Erreur lors de la crÃƒÆ’Ã‚Â©ation de la table pending_rewards", e);
        }
    }

    public void addPendingCommand(UUID uuid, String command, String message) {
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement ps = conn.prepareStatement("INSERT INTO pending_rewards (uuid, command, message) VALUES (?, ?, ?)")) {
            ps.setString(1, uuid.toString());
            ps.setString(2, command);
            ps.setString(3, message);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void processPendingCommands(Player p) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection conn = plugin.getDatabaseManager().getConnection();
                 PreparedStatement ps = conn.prepareStatement("SELECT * FROM pending_rewards WHERE uuid = ?")) {
                ps.setString(1, p.getUniqueId().toString());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        int id = rs.getInt("id");
                        String cmd = rs.getString("command");
                        String msg = rs.getString("message");
                        
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            if (cmd != null && !cmd.isEmpty()) {
                                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd.replace("%player%", p.getName()));
                            }
                            if (msg != null && !msg.isEmpty()) {
                                p.sendMessage(msg);
                            }
                        });
                        
                        try (PreparedStatement del = conn.prepareStatement("DELETE FROM pending_rewards WHERE id = ?")) {
                            del.setInt(1, id);
                            del.executeUpdate();
                        }
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }
}
