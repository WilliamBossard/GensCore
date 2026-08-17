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
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

public class DatabaseManager {

    private final CorePlugin plugin;
    private HikariDataSource dataSource;

    public DatabaseManager(CorePlugin plugin) {
        this.plugin = plugin;
        connect();
        initTables();
    }

    private void connect() {
        // S'assurer que le dossier existe
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }

        File dataFile = new File(plugin.getDataFolder(), "genscore.db");
        String url = "jdbc:sqlite:" + dataFile.getAbsolutePath();

        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            plugin.getLangManager().sendConsoleError("error.sqlite_driver");
        }

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(url);
        config.setPoolName("GensCore-Pool");
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(30000);
        
        // SQLite properties for WAL and concurrency
        config.addDataSourceProperty("journal_mode", "WAL");
        config.addDataSourceProperty("synchronous", "NORMAL");
        config.addDataSourceProperty("busy_timeout", "5000");

        try {
            this.dataSource = new HikariDataSource(config);
            plugin.getLangManager().sendConsoleMessage("db.pool_init");
        } catch (Exception e) {
            plugin.getLangManager().sendConsoleError("db.pool_error");
            e.printStackTrace();
        }
    }

    private void initTables() {
         try (Connection conn = getConnection();
             Statement statement = conn.createStatement()) {
             
            plugin.getLangManager().sendConsoleMessage("db.tables_ready");

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

            // Tombes (Graves)
            statement.execute("CREATE TABLE IF NOT EXISTS tombs (" +
                    "id VARCHAR(36) PRIMARY KEY, " +
                    "owner_id VARCHAR(36) NOT NULL, " +
                    "world VARCHAR(50) NOT NULL, " +
                    "x DOUBLE NOT NULL, " +
                    "y DOUBLE NOT NULL, " +
                    "z DOUBLE NOT NULL, " +
                    "contents TEXT NOT NULL, " +
                    "xp INTEGER NOT NULL, " +
                    "expiration_time BIGINT NOT NULL" +
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

            // Moderation tables
            statement.execute("CREATE TABLE IF NOT EXISTS moderation_mutes (" +
                    "uuid VARCHAR(36) PRIMARY KEY, " +
                    "expiration BIGINT NOT NULL, " +
                    "reason TEXT NOT NULL" +
                    ")");

            statement.execute("CREATE TABLE IF NOT EXISTS moderation_frozen (" +
                    "uuid VARCHAR(36) PRIMARY KEY" +
                    ")");

            plugin.getLangManager().sendConsoleMessage("db.tables_init_success");
        } catch (SQLException e) {
            plugin.getLangManager().sendConsoleError("db.tables_init_error");
            e.printStackTrace();
        }
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            plugin.getLangManager().sendConsoleMessage("db.pool_closed");
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
                            plugin.getLangManager().sendMessage(player, "economy.pending_reward", net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.parsed("amount", String.valueOf(amount)));
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
                                plugin.getLangManager().sendMessage(player, "guild.reward_received");
                            } catch (Exception e) {
                                plugin.getLangManager().sendMessage(player, "error.invalid_reward");
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

    // --- MODERATION ---
    public void saveMutes(java.util.Map<UUID, fr.gens.core.modules.moderation.ModerationModule.MuteData> mutes) {
        String deleteSql = "DELETE FROM moderation_mutes";
        String insertSql = "INSERT INTO moderation_mutes (uuid, expiration, reason) VALUES (?, ?, ?)";
        try (Connection conn = getConnection();
             java.sql.Statement stmt = conn.createStatement();
             PreparedStatement pstmt = conn.prepareStatement(insertSql)) {
            stmt.executeUpdate(deleteSql);
            for (java.util.Map.Entry<UUID, fr.gens.core.modules.moderation.ModerationModule.MuteData> entry : mutes.entrySet()) {
                pstmt.setString(1, entry.getKey().toString());
                pstmt.setLong(2, entry.getValue().expiration);
                pstmt.setString(3, entry.getValue().reason);
                pstmt.addBatch();
            }
            pstmt.executeBatch();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void saveFrozen(java.util.Set<UUID> frozen) {
        String deleteSql = "DELETE FROM moderation_frozen";
        String insertSql = "INSERT INTO moderation_frozen (uuid) VALUES (?)";
        try (Connection conn = getConnection();
             java.sql.Statement stmt = conn.createStatement();
             PreparedStatement pstmt = conn.prepareStatement(insertSql)) {
            stmt.executeUpdate(deleteSql);
            for (UUID uuid : frozen) {
                pstmt.setString(1, uuid.toString());
                pstmt.addBatch();
            }
            pstmt.executeBatch();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public java.util.Map<UUID, fr.gens.core.modules.moderation.ModerationModule.MuteData> loadMutes() {
        java.util.Map<UUID, fr.gens.core.modules.moderation.ModerationModule.MuteData> mutes = new java.util.HashMap<>();
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT * FROM moderation_mutes");
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                mutes.put(UUID.fromString(rs.getString("uuid")), new fr.gens.core.modules.moderation.ModerationModule.MuteData(rs.getString("reason"), rs.getLong("expiration")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return mutes;
    }

    public java.util.Set<UUID> loadFrozen() {
        java.util.Set<UUID> frozen = new java.util.HashSet<>();
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT * FROM moderation_frozen");
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                frozen.add(UUID.fromString(rs.getString("uuid")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return frozen;
    }

    // --- WEB STATS EXTENSIONS ---

    public long getPlaytimeMinutes(UUID uuid) {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT playtime_minutes FROM player_global_stats WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getLong("playtime_minutes");
            }
        } catch (SQLException e) {
            plugin.getLangManager().sendConsoleError("db.query_error");
            e.printStackTrace();
        }
        return 0;
    }

    public java.util.List<java.util.Map<String, Object>> getGlobalLeaderboard() {
        java.util.List<java.util.Map<String, Object>> leaderboard = new java.util.ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT q.player_name, p.username, q.quests_completed, q.uuid, " +
                     "COALESCE(g.blocks_broken, 0) as blocks, " +
                     "COALESCE(g.mobs_killed, 0) as mobs, " +
                     "COALESCE(g.playtime_minutes, 0) as playtime " +
                     "FROM player_quests_stats q " +
                     "LEFT JOIN player_global_stats g ON q.uuid = g.uuid " +
                     "LEFT JOIN player_profiles p ON q.uuid = p.uuid " +
                     "ORDER BY q.quests_completed DESC, g.blocks_broken DESC LIMIT 50")) {
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    java.util.Map<String, Object> playerStat = new java.util.HashMap<>();
                    String name = rs.getString("username");
                    if (name == null) name = rs.getString("player_name");
                    playerStat.put("playerName", name);
                    playerStat.put("questsCompleted", rs.getInt("quests_completed"));
                    playerStat.put("uuid", rs.getString("uuid"));
                    playerStat.put("blocksBroken", rs.getInt("blocks"));
                    playerStat.put("mobsKilled", rs.getInt("mobs"));
                    playerStat.put("playtime", rs.getLong("playtime"));
                    leaderboard.add(playerStat);
                }
            }
        } catch (SQLException e) {
            plugin.getLangManager().sendConsoleError("db.query_error");
            e.printStackTrace();
        }
        return leaderboard;
    }

    public java.util.Map<String, Object> getQuestsLeaderboardData() {
        java.util.Map<String, Object> result = new java.util.HashMap<>();
        try (Connection conn = getConnection()) {
            // Current Reward
            java.util.Calendar cal = java.util.Calendar.getInstance();
            cal.set(java.util.Calendar.DAY_OF_WEEK, java.util.Calendar.MONDAY);
            cal.set(java.util.Calendar.HOUR_OF_DAY, 0);
            cal.set(java.util.Calendar.MINUTE, 0);
            cal.set(java.util.Calendar.SECOND, 0);
            cal.set(java.util.Calendar.MILLISECOND, 0);
            String currentWeek = String.valueOf(cal.getTimeInMillis());
            String reward = "Aucune";
            
            try (PreparedStatement ps = conn.prepareStatement("SELECT reward_description FROM weekly_rewards WHERE week_id = ?")) {
                ps.setString(1, currentWeek);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) reward = rs.getString("reward_description");
                }
            }
            result.put("reward", reward);
            
            long now = System.currentTimeMillis();
            long dayStart = now - (24L * 60L * 60L * 1000L);
            long weekStart = cal.getTimeInMillis();
            long monthStart = now - (30L * 24L * 60L * 60L * 1000L);
            
            long endOfWeek = weekStart + (7L * 24L * 60L * 60L * 1000L);
            long timeRemainingMs = endOfWeek - now;
            if (timeRemainingMs < 0) timeRemainingMs = 0;
            long days = timeRemainingMs / (1000 * 60 * 60 * 24);
            long hours = (timeRemainingMs / (1000 * 60 * 60)) % 24;
            long minutes = (timeRemainingMs / (1000 * 60)) % 60;
            result.put("timeRemaining", days + "j " + hours + "h " + minutes + "m");
            
            // Helper function to query
            java.util.function.BiFunction<Long, Long, java.util.List<java.util.Map<String, Object>>> getLeaderboard = (start, end) -> {
                java.util.List<java.util.Map<String, Object>> list = new java.util.ArrayList<>();
                String sql = "SELECT player_name, COUNT(*) as count FROM player_quests_history WHERE completion_date >= ? AND completion_date <= ? GROUP BY uuid ORDER BY count DESC LIMIT 10";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setLong(1, start);
                    ps.setLong(2, end);
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            java.util.Map<String, Object> map = new java.util.HashMap<>();
                            map.put("playerName", rs.getString("player_name"));
                            map.put("count", rs.getInt("count"));
                            list.add(map);
                        }
                    }
                } catch (SQLException e) { e.printStackTrace(); }
                return list;
            };
            
            result.put("daily", getLeaderboard.apply(dayStart, now));
            result.put("weekly", getLeaderboard.apply(weekStart, now));
            result.put("monthly", getLeaderboard.apply(monthStart, now));
            
            java.util.List<java.util.Map<String, Object>> totalList = new java.util.ArrayList<>();
            try (PreparedStatement ps = conn.prepareStatement(
                "SELECT q.player_name, p.username, q.quests_completed " +
                "FROM player_quests_stats q " +
                "LEFT JOIN player_profiles p ON q.uuid = p.uuid " +
                "ORDER BY quests_completed DESC LIMIT 10")) {
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        java.util.Map<String, Object> map = new java.util.HashMap<>();
                        String name = rs.getString("username");
                        if (name == null) name = rs.getString("player_name");
                        map.put("playerName", name);
                        map.put("count", rs.getInt("quests_completed"));
                        totalList.add(map);
                    }
                }
            }
            result.put("total", totalList);
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return result;
    }

    public java.util.Map<String, java.util.List<java.util.Map<String, Object>>> getJobsLeaderboardData() {
        java.util.Map<String, java.util.List<java.util.Map<String, Object>>> jobsLeaderboard = new java.util.HashMap<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT j.job_name, COALESCE(p.username, q.player_name, 'Inconnu') as player_name, j.level, j.xp " +
                     "FROM player_jobs j " +
                     "LEFT JOIN player_quests_stats q ON j.uuid = q.uuid " +
                     "LEFT JOIN player_profiles p ON j.uuid = p.uuid " +
                     "ORDER BY j.job_name, j.level DESC, j.xp DESC")) {
            
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String jobName = rs.getString("job_name");
                    String playerName = rs.getString("player_name");
                    
                    java.util.Map<String, Object> stat = new java.util.HashMap<>();
                    stat.put("playerName", playerName);
                    stat.put("level", rs.getInt("level"));
                    stat.put("xp", rs.getDouble("xp"));
                    
                    jobsLeaderboard.computeIfAbsent(jobName, k -> new java.util.ArrayList<>()).add(stat);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return jobsLeaderboard;
    }
}
