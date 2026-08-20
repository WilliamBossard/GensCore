package fr.gens.core.database;

import fr.gens.core.CorePlugin;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;


public class AuthDAO {

    private final CorePlugin plugin;

    public AuthDAO(CorePlugin plugin) {
        this.plugin = plugin;
    }

    public void initDatabase() {
        String sql = "CREATE TABLE IF NOT EXISTS genscore_auth (" +
                "uuid VARCHAR(36) PRIMARY KEY, " +
                "password_hash VARCHAR(255) NOT NULL, " +
                "salt VARCHAR(255) NOT NULL, " +
                "last_ip VARCHAR(50), " +
                "last_login BIGINT DEFAULT 0" +
                ");";
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             java.sql.Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            plugin.getLogger().log(java.util.logging.Level.SEVERE, "Erreur lors de la crÃƒÆ’Ã‚Â©ation de la table genscore_auth", e);
        }
    }

    public static class AuthData {
        public String hash;
        public String salt;
        public String lastIp;
        public long lastLogin;

        public AuthData(String hash, String salt, String lastIp, long lastLogin) {
            this.hash = hash;
            this.salt = salt;
            this.lastIp = lastIp;
            this.lastLogin = lastLogin;
        }
    }

    public AuthData getAuthData(UUID uuid) {
        String sql = "SELECT password_hash, salt, last_ip, last_login FROM genscore_auth WHERE uuid = ?";
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, uuid.toString());
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return new AuthData(
                        rs.getString("password_hash"),
                        rs.getString("salt"),
                        rs.getString("last_ip"),
                        rs.getLong("last_login")
                );
            }
        } catch (SQLException e) {
            plugin.getLogger().log(java.util.logging.Level.SEVERE, "Erreur lors de la lecture des donnees d'auth", e);
        }
        return null;
    }

    public void removeAuthData(UUID uuid) {
        String sql = "DELETE FROM genscore_auth WHERE uuid = ?";
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, uuid.toString());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void registerPlayer(UUID uuid, String hash, String salt, String ip) {
        String sql = "INSERT INTO genscore_auth (uuid, password_hash, salt, last_ip, last_login) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, uuid.toString());
            pstmt.setString(2, hash);
            pstmt.setString(3, salt);
            pstmt.setString(4, ip);
            pstmt.setLong(5, System.currentTimeMillis());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(java.util.logging.Level.SEVERE, "Erreur lors de l'enregistrement (register)", e);
        }
    }

    public void updateLogin(UUID uuid, String ip) {
        String sql = "UPDATE genscore_auth SET last_ip = ?, last_login = ? WHERE uuid = ?";
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, ip);
            pstmt.setLong(2, System.currentTimeMillis());
            pstmt.setString(3, uuid.toString());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(java.util.logging.Level.SEVERE, "Erreur lors de la mise a jour de la connexion (login)", e);
        }
    }

    public void updatePassword(UUID uuid, String hash, String salt) {
        String sql = "UPDATE genscore_auth SET password_hash = ?, salt = ?, last_ip = NULL WHERE uuid = ?";
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, hash);
            pstmt.setString(2, salt);
            pstmt.setString(3, uuid.toString());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(java.util.logging.Level.SEVERE, "Erreur lors de la mise a jour du mot de passe", e);
        }
    }
}

