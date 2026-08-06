package fr.gens.core.utils;

import fr.gens.core.CorePlugin;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.UUID;

public class DatabaseManager {

    private final CorePlugin plugin;
    private Connection connection;

    public DatabaseManager(CorePlugin plugin) {
        this.plugin = plugin;
        connect();
        initTables();
    }

    private void connect() {
        try {
            // S'assurer que le dossier existe
            if (!plugin.getDataFolder().exists()) {
                plugin.getDataFolder().mkdirs();
            }

            File dataFile = new File(plugin.getDataFolder(), "genscore.db");
            String url = "jdbc:sqlite:" + dataFile.getAbsolutePath();

            // S'assurer que le driver SQLite est charge
            Class.forName("org.sqlite.JDBC");

            this.connection = DriverManager.getConnection(url);
            plugin.getLogger().info("[DatabaseManager] Connecte a SQLite !");
        } catch (SQLException | ClassNotFoundException e) {
            plugin.getLogger().severe("Erreur lors de la connexion a la base de donnees SQLite !");
            e.printStackTrace();
        }
    }

    private void initTables() {
         try (Connection conn = getConnection();
             Statement statement = conn.createStatement()) {
             
            plugin.getLogger().info("[DatabaseManager] Tables pretes !");

            // Economie pour l'economie
            statement.execute("CREATE TABLE IF NOT EXISTS players_economy (" +
                    "uuid VARCHAR(36) PRIMARY KEY, " +
                    "balance DOUBLE NOT NULL DEFAULT 0.0" +
                    ");");

            // Tables pour le shop
            statement.execute("CREATE TABLE IF NOT EXISTS shop_categories (" +
                    "id VARCHAR(50) PRIMARY KEY, " +
                    "displayName VARCHAR(255) NOT NULL, " +
                    "icon VARCHAR(50) NOT NULL" +
                    ");");

            statement.execute("CREATE TABLE IF NOT EXISTS shop_items (" +
                    "material VARCHAR(50) PRIMARY KEY, " +
                    "category_id VARCHAR(50) NOT NULL, " +
                    "buyPrice DOUBLE NOT NULL, " +
                    "sellPrice DOUBLE NOT NULL, " +
                    "stock INTEGER DEFAULT 0, " +
                    "targetStock INTEGER DEFAULT 1000, " +
                    "isCommand BOOLEAN DEFAULT 0, " +
                    "commandToExecute TEXT, " +
                    "isEnabled BOOLEAN DEFAULT 1, " +
                    "FOREIGN KEY(category_id) REFERENCES shop_categories(id) ON DELETE CASCADE" +
                    ");");
                    
            // Tentative d'ajout des colonnes si la table existait deja
            try { statement.execute("ALTER TABLE shop_items ADD COLUMN isCommand BOOLEAN DEFAULT 0;"); } catch (SQLException ignored) {}
            try { statement.execute("ALTER TABLE shop_items ADD COLUMN commandToExecute VARCHAR(255) DEFAULT '';"); } catch (SQLException ignored) {}
            try { statement.execute("ALTER TABLE shop_items ADD COLUMN isEnabled BOOLEAN DEFAULT 1;"); } catch (SQLException ignored) {}

            // Hotel des Ventes
            statement.execute("CREATE TABLE IF NOT EXISTS auction_house (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "seller_uuid VARCHAR(36) NOT NULL, " +
                    "seller_name VARCHAR(16) NOT NULL, " +
                    "price DOUBLE NOT NULL, " +
                    "item_data TEXT NOT NULL, " +
                    "expire_time BIGINT NOT NULL" +
                    ");");

            // Historique des prix (pour les graphiques Recharts)
            statement.execute("CREATE TABLE IF NOT EXISTS shop_history (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "material VARCHAR(50) NOT NULL, " +
                    "timestamp BIGINT NOT NULL, " +
                    "buyPrice DOUBLE NOT NULL, " +
                    "sellPrice DOUBLE NOT NULL, " +
                    "stock INTEGER NOT NULL, " +
                    "FOREIGN KEY(material) REFERENCES shop_items(material) ON DELETE CASCADE" +
                    ");");

            // Teleportation
            statement.execute("CREATE TABLE IF NOT EXISTS spawn_location (" +
                    "id INTEGER PRIMARY KEY DEFAULT 1, " +
                    "world VARCHAR(255) NOT NULL, " +
                    "x DOUBLE NOT NULL, " +
                    "y DOUBLE NOT NULL, " +
                    "z DOUBLE NOT NULL, " +
                    "yaw FLOAT NOT NULL, " +
                    "pitch FLOAT NOT NULL" +
                    ");");

            statement.execute("CREATE TABLE IF NOT EXISTS player_homes (" +
                    "uuid VARCHAR(36) NOT NULL, " +
                    "name VARCHAR(50) NOT NULL, " +
                    "world VARCHAR(255) NOT NULL, " +
                    "x DOUBLE NOT NULL, " +
                    "y DOUBLE NOT NULL, " +
                    "z DOUBLE NOT NULL, " +
                    "yaw FLOAT NOT NULL, " +
                    "pitch FLOAT NOT NULL, " +
                    "PRIMARY KEY (uuid, name)" +
                    ");");

            // Tables pour les Quetes (QuestModule)
            statement.execute("CREATE TABLE IF NOT EXISTS player_quests_stats (" +
                    "uuid VARCHAR(36) PRIMARY KEY, " +
                    "player_name VARCHAR(16) NOT NULL, " +
                    "quests_completed INTEGER DEFAULT 0, " +
                    "rerolls_done INTEGER DEFAULT 0, " +
                    "last_reroll_date VARCHAR(255) DEFAULT ''" +
                    ");");

            // Migration des anciennes tables
            try { statement.execute("ALTER TABLE player_quests_stats ADD COLUMN rerolls_done INTEGER DEFAULT 0;"); } catch (SQLException ignored) {}
            try { statement.execute("ALTER TABLE player_quests_stats ADD COLUMN last_reroll_date VARCHAR(255) DEFAULT '';"); } catch (SQLException ignored) {}

            statement.execute("CREATE TABLE IF NOT EXISTS player_quests_history (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "uuid VARCHAR(36) NOT NULL, " +
                    "player_name VARCHAR(16) NOT NULL, " +
                    "completion_date BIGINT NOT NULL" +
                    ");");

            statement.execute("CREATE TABLE IF NOT EXISTS weekly_rewards (" +
                    "week_id VARCHAR(20) PRIMARY KEY, " +
                    "reward_description TEXT NOT NULL, " +
                    "winner_uuid VARCHAR(36), " +
                    "is_distributed INTEGER DEFAULT 0" +
                    ");");

            statement.execute("CREATE TABLE IF NOT EXISTS player_active_quests (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "uuid VARCHAR(36) NOT NULL, " +
                    "date_assigned VARCHAR(10) NOT NULL, " +
                    "category VARCHAR(20) NOT NULL, " +
                    "quest_id VARCHAR(50) NOT NULL, " +
                    "progress INTEGER DEFAULT 0, " +
                    "completed BOOLEAN DEFAULT 0" +
                    ");");

            // Table d'authentification native
            statement.execute("CREATE TABLE IF NOT EXISTS genscore_auth (" +
                    "uuid VARCHAR(36) PRIMARY KEY, " +
                    "password_hash VARCHAR(255) NOT NULL, " +
                    "salt VARCHAR(255) NOT NULL, " +
                    "last_ip VARCHAR(50), " +
                    "last_login BIGINT DEFAULT 0" +
                    ");");

            // Table de statistiques (et Discord ID)
            statement.execute("CREATE TABLE IF NOT EXISTS player_stats (" +
                    "uuid VARCHAR(36) PRIMARY KEY, " +
                    "discord_id VARCHAR(50)" +
                    ");");

            // Table des profils (UUID -> Username) pour le panel web
            statement.execute("CREATE TABLE IF NOT EXISTS player_profiles (" +
                    "uuid VARCHAR(36) PRIMARY KEY, " +
                    "username VARCHAR(16) NOT NULL" +
                    ");");

            // Table des spawners (GensModule)
            statement.execute("CREATE TABLE IF NOT EXISTS spawners (" +
                    "id VARCHAR(36) PRIMARY KEY, " +
                    "world VARCHAR(255) NOT NULL, " +
                    "x DOUBLE NOT NULL, " +
                    "y DOUBLE NOT NULL, " +
                    "z DOUBLE NOT NULL, " +
                    "type VARCHAR(50) NOT NULL, " +
                    "stack_count INTEGER NOT NULL DEFAULT 1, " +
                    "stored_exp INTEGER NOT NULL DEFAULT 0, " +
                    "stored_items TEXT NOT NULL, " +
                    "last_interacted VARCHAR(16), " +
                    "storage_level INTEGER DEFAULT 0, " +
                    "exp_level INTEGER DEFAULT 0, " +
                    "speed_level INTEGER DEFAULT 0, " +
                    "is_loot_chest INTEGER DEFAULT 0" +
                    ");");

            // Migration si la table existe déjà sans ces colonnes
            try { statement.execute("ALTER TABLE spawners ADD COLUMN storage_level INTEGER DEFAULT 0;"); } catch (SQLException ignored) {}
            try { statement.execute("ALTER TABLE spawners ADD COLUMN exp_level INTEGER DEFAULT 0;"); } catch (SQLException ignored) {}
            try { statement.execute("ALTER TABLE spawners ADD COLUMN speed_level INTEGER DEFAULT 0;"); } catch (SQLException ignored) {}
            try { statement.execute("ALTER TABLE spawners ADD COLUMN is_loot_chest INTEGER DEFAULT 0;"); } catch (SQLException ignored) {}

            // NOUVELLES TABLES : Web Panel, Mini-jeux, et Stats Globales
              statement.execute("CREATE TABLE IF NOT EXISTS player_global_stats (" +
                      "uuid VARCHAR(36) PRIMARY KEY, " +
                      "blocks_broken INTEGER DEFAULT 0, " +
                      "mobs_killed INTEGER DEFAULT 0, " +
                      "playtime_minutes INTEGER DEFAULT 0, " +
                      "deaths INTEGER DEFAULT 0, " +
                      "player_kills INTEGER DEFAULT 0, " +
                      "last_updated BIGINT DEFAULT 0" +
                      ");");
              
              // Migrations
              try { statement.execute("ALTER TABLE player_global_stats ADD COLUMN deaths INTEGER DEFAULT 0;"); } catch (Exception ignored) {}
              try { statement.execute("ALTER TABLE player_global_stats ADD COLUMN player_kills INTEGER DEFAULT 0;"); } catch (Exception ignored) {}

              // Table des transactions web (Historique perso)
              statement.execute("CREATE TABLE IF NOT EXISTS player_transactions_history (" +
                      "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                      "uuid VARCHAR(36) NOT NULL, " +
                      "type VARCHAR(10) NOT NULL, " +
                      "material VARCHAR(50) NOT NULL, " +
                      "amount INTEGER NOT NULL, " +
                      "price DOUBLE NOT NULL, " +
                      "timestamp BIGINT NOT NULL" +
                      ");");

            statement.execute("CREATE TABLE IF NOT EXISTS player_minigame_cooldowns (" +
                    "uuid VARCHAR(36), " +
                    "game_id VARCHAR(50), " +
                    "last_played BIGINT DEFAULT 0, " +
                    "PRIMARY KEY(uuid, game_id)" +
                    ");");

            // Tables pour l'inventaire Web (Casino)
            statement.execute("CREATE TABLE IF NOT EXISTS player_web_bets (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "uuid VARCHAR(36) NOT NULL, " +
                    "material VARCHAR(50) NOT NULL, " +
                    "amount INTEGER NOT NULL, " +
                    "base64_data TEXT NOT NULL" +
                    ");");

            statement.execute("CREATE TABLE IF NOT EXISTS player_web_rewards (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "uuid VARCHAR(36) NOT NULL, " +
                    "material VARCHAR(50) NOT NULL, " +
                    "amount INTEGER NOT NULL, " +
                    "base64_data TEXT NOT NULL" +
                    ");");

            // Récompenses en attente (Quêtes de guilde par ex)
            statement.execute("CREATE TABLE IF NOT EXISTS genscore_pending_rewards (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "uuid VARCHAR(36) NOT NULL, " +
                    "amount DOUBLE, " +
                    "item_data TEXT" +
                    ");");

            statement.execute("CREATE TABLE IF NOT EXISTS pending_rewards (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "uuid VARCHAR(36), " +
                    "command TEXT, " +
                    "message TEXT" +
                    ")");

            // Table Teams (Guildes)
            statement.execute("CREATE TABLE IF NOT EXISTS genscore_teams (" +
                    "team_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "name VARCHAR(32) UNIQUE, " +
                    "leader_uuid VARCHAR(36)" +
                    ")");

            // Les tables de verrous (locks) sont creees plus bas
                
            statement.execute("CREATE TABLE IF NOT EXISTS genscore_team_stats (" +
                "team_id INTEGER PRIMARY KEY, " +
                "weekly_points INTEGER DEFAULT 0, " +
                "total_points INTEGER DEFAULT 0, " +
                "FOREIGN KEY(team_id) REFERENCES genscore_teams(team_id) ON DELETE CASCADE" +
                ");");
                
            statement.execute("CREATE TABLE IF NOT EXISTS genscore_team_quests (" +
                "team_id INTEGER PRIMARY KEY, " +
                "quest_id TEXT, " +
                "progress INTEGER DEFAULT 0, " +
                "FOREIGN KEY(team_id) REFERENCES genscore_teams(team_id) ON DELETE CASCADE" +
                ");");

            // Table Team Members
            statement.execute("CREATE TABLE IF NOT EXISTS genscore_team_members (" +
                    "team_id INTEGER, " +
                    "player_uuid VARCHAR(36) PRIMARY KEY, " +
                    "FOREIGN KEY(team_id) REFERENCES genscore_teams(team_id)" +
                    ")");

            // Table Verrous (Locks)
            statement.execute("CREATE TABLE IF NOT EXISTS genscore_locks (" +
                    "location VARCHAR(255) PRIMARY KEY, " +
                    "owner_uuid VARCHAR(36), " +
                    "team_id INTEGER" +
                    ")");

            // Table Métiers
            statement.execute("CREATE TABLE IF NOT EXISTS player_jobs (" +
                    "uuid VARCHAR(36) NOT NULL, " +
                    "job_name VARCHAR(50) NOT NULL, " +
                    "level INT DEFAULT 1, " +
                    "xp DOUBLE DEFAULT 0, " +
                    "PRIMARY KEY (uuid, job_name)" +
                    ")");

            plugin.getLogger().info("[DatabaseManager] Tables initialisees avec succes.");
        } catch (SQLException e) {
            plugin.getLogger().severe("Erreur lors de l'initialisation des tables !");
            e.printStackTrace();
        }
    }

    public Connection getConnection() throws SQLException {
        File dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists()) {
            dataFolder.mkdir();
        }
        File databaseFile = new File(dataFolder, "genscore.db");
        return DriverManager.getConnection("jdbc:sqlite:" + databaseFile.getAbsolutePath());
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                plugin.getLogger().info("[DatabaseManager] Connexion SQLite fermee.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // --- NOUVELLES METHODES REROLLS & STATS ---

    public int getQuestsCompletedTotal(UUID uuid) {
        try (Connection conn = getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement("SELECT quests_completed FROM player_quests_stats WHERE uuid = ?")) {
                stmt.setString(1, uuid.toString());
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getInt("quests_completed");
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public int getRerollsDone(UUID uuid, String today) {
        boolean needsReset = false;
        int count = 0;

        try (Connection conn = getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement("SELECT rerolls_done, last_reroll_date FROM player_quests_stats WHERE uuid = ?")) {
                stmt.setString(1, uuid.toString());
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        String lastDate = rs.getString("last_reroll_date");
                        if (today.equals(lastDate)) {
                            count = rs.getInt("rerolls_done");
                        } else {
                            // C'est un nouveau jour, on reset plus tard
                            needsReset = true;
                        }
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        if (needsReset) {
            setRerollsDone(uuid, 0, today);
            return 0;
        }

        return count;
    }

    public void setRerollsDone(UUID uuid, int count, String today) {
        try (Connection conn = getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement(
                    "UPDATE player_quests_stats SET rerolls_done = ?, last_reroll_date = ? WHERE uuid = ?")) {
                stmt.setInt(1, count);
                stmt.setString(2, today);
                stmt.setString(3, uuid.toString());
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // --- AUTHENTICATION & DISCORD EXTENSIONS ---

    public void setDiscordId(UUID uuid, String discordId) {
        String sql = "INSERT INTO player_stats (uuid, discord_id) VALUES (?, ?) " +
                     "ON CONFLICT(uuid) DO UPDATE SET discord_id = excluded.discord_id;";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, uuid.toString());
            pstmt.setString(2, discordId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(java.util.logging.Level.SEVERE, "Erreur lors de la mise a jour du Discord ID", e);
        }
    }

    public UUID getUuidFromDiscord(String discordId) {
        String sql = "SELECT uuid FROM player_stats WHERE discord_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, discordId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                String u = rs.getString("uuid");
                if (u != null && !u.isEmpty()) return UUID.fromString(u);
            }
        } catch (SQLException e) {
            plugin.getLogger().log(java.util.logging.Level.SEVERE, "Erreur lors de la recuperation de l'UUID via Discord", e);
        }
        return null;
    }

    public String getDiscordId(UUID uuid) {
        String sql = "SELECT discord_id FROM player_stats WHERE uuid = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, uuid.toString());
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getString("discord_id");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
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
        try (Connection conn = getConnection();
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
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, uuid.toString());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void registerPlayer(UUID uuid, String hash, String salt, String ip) {
        String sql = "INSERT INTO genscore_auth (uuid, password_hash, salt, last_ip, last_login) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = getConnection();
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
        try (Connection conn = getConnection();
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
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, hash);
            pstmt.setString(2, salt);
            pstmt.setString(3, uuid.toString());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(java.util.logging.Level.SEVERE, "Erreur lors de la mise a jour du mot de passe", e);
        }
    }
    public void addPendingReward(UUID uuid, double amount, String itemData) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement("INSERT INTO genscore_pending_rewards (uuid, amount, item_data) VALUES (?, ?, ?)")) {
            stmt.setString(1, uuid.toString());
            stmt.setDouble(2, amount);
            stmt.setString(3, itemData);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void processPendingRewards(org.bukkit.entity.Player player) {
        try (Connection conn = getConnection()) {
            boolean hasRewards = false;
            try (PreparedStatement stmt = conn.prepareStatement("SELECT * FROM genscore_pending_rewards WHERE uuid = ?")) {
                stmt.setString(1, player.getUniqueId().toString());
                ResultSet rs = stmt.executeQuery();
                while (rs.next()) {
                    hasRewards = true;
                    double amount = rs.getDouble("amount");
                    String itemData = rs.getString("item_data");
                    
                    if (amount > 0) {
                        fr.gens.core.modules.EconomyModule eco = (fr.gens.core.modules.EconomyModule) plugin.getModuleManager().getModule("economy");
                        if (eco != null && eco.isEnabled()) {
                            eco.addMoney(player.getUniqueId(), amount);
                            player.sendMessage("§aVous avez reçu " + amount + "$ comme récompense en attente !");
                        }
                    }
                    if (itemData != null && !itemData.isEmpty()) {
                        String[] parts = itemData.split(":");
                        if (parts.length == 2) {
                            try {
                                org.bukkit.Material mat = org.bukkit.Material.valueOf(parts[0]);
                                int count = Integer.parseInt(parts[1]);
                                org.bukkit.inventory.ItemStack item = new org.bukkit.inventory.ItemStack(mat, count);
                                
                                java.util.HashMap<Integer, org.bukkit.inventory.ItemStack> excess = player.getInventory().addItem(item);
                                for (org.bukkit.inventory.ItemStack drop : excess.values()) {
                                    player.getWorld().dropItemNaturally(player.getLocation(), drop);
                                }
                                player.sendMessage("§aVous avez reçu votre récompense matérielle de guilde !");
                            } catch (Exception e) {
                                player.sendMessage("§cErreur lors de la distribution de votre récompense (objet invalide).");
                            }
                        }
                    }
                }
            }
            if (hasRewards) {
                try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM genscore_pending_rewards WHERE uuid = ?")) {
                    stmt.setString(1, player.getUniqueId().toString());
                    stmt.executeUpdate();
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
