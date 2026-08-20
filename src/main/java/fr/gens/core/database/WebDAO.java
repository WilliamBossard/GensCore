package fr.gens.core.database;

import fr.gens.core.CorePlugin;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;


public class WebDAO {

    private final CorePlugin plugin;

    public WebDAO(CorePlugin plugin) {
        this.plugin = plugin;
    }

    public void initDatabase() {
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             Statement stmt = conn.createStatement()) {
            
            stmt.execute("CREATE TABLE IF NOT EXISTS player_web_bets (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "uuid VARCHAR(36), " +
                    "material VARCHAR(50), " +
                    "amount INTEGER, " +
                    "base64_data TEXT" +
                    ");");

            stmt.execute("CREATE TABLE IF NOT EXISTS player_web_rewards (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "uuid VARCHAR(36), " +
                    "material VARCHAR(50), " +
                    "amount INTEGER, " +
                    "base64_data TEXT" +
                    ");");
                    
        } catch (SQLException e) {
            plugin.getLogger().log(java.util.logging.Level.SEVERE, "Erreur lors de la crÃƒÆ’Ã‚Â©ation des tables web", e);
        }
    }
}
