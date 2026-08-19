package fr.gens.core.utils;

import fr.gens.core.CorePlugin;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import fr.gens.core.database.AuthDAO;
import fr.gens.core.database.StatsDAO;
import fr.gens.core.database.QuestDAO;
import fr.gens.core.database.RewardDAO;
import fr.gens.core.database.ModerationDAO;
import fr.gens.core.database.EconomyDAO;
import fr.gens.core.database.ShopDAO;
import fr.gens.core.database.TeamDAO;

public class DatabaseManager {

    private final CorePlugin plugin;
    private HikariDataSource dataSource;
    private final AuthDAO authDAO;
    private final StatsDAO statsDAO;
    private final QuestDAO questDAO;
    private final RewardDAO rewardDAO;
    private final ModerationDAO moderationDAO;
    private final EconomyDAO economyDAO;
    private final ShopDAO shopDAO;
    private final TeamDAO teamDAO;

    public DatabaseManager(CorePlugin plugin) {
        this.plugin = plugin;
        connect();
        initTables();
        this.authDAO = new AuthDAO(plugin);
        this.statsDAO = new StatsDAO(plugin);
        this.questDAO = new QuestDAO(plugin);
        this.rewardDAO = new RewardDAO(plugin);
        this.moderationDAO = new ModerationDAO(plugin);
        this.economyDAO = new EconomyDAO(plugin);
        this.shopDAO = new ShopDAO(plugin);
        this.teamDAO = new TeamDAO(plugin);
    }

    public AuthDAO getAuthDAO() {
        return authDAO;
    }

    public StatsDAO getStatsDAO() {
        return statsDAO;
    }

    public QuestDAO getQuestDAO() {
        return questDAO;
    }

    public RewardDAO getRewardDAO() {
        return rewardDAO;
    }

    public ModerationDAO getModerationDAO() {
        return moderationDAO;
    }

    public EconomyDAO getEconomyDAO() {
        return economyDAO;
    }

    public ShopDAO getShopDAO() {
        return shopDAO;
    }

    public TeamDAO getTeamDAO() {
        return teamDAO;
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
            // Note : ancienne table 'pending_rewards' fusionnée ici — seule genscore_pending_rewards est conservée
            statement.execute("CREATE TABLE IF NOT EXISTS genscore_pending_rewards (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "uuid VARCHAR(36) NOT NULL, " +
                    "amount DOUBLE, " +
                    "command TEXT, " +
                    "message TEXT, " +
                    "item_data TEXT" +
                    ");");

            // Migration silencieuse : si l'ancienne table pending_rewards existe, on la supprime
            try { statement.execute("DROP TABLE IF EXISTS pending_rewards;"); } catch (Exception ignored) {}

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

            // --- Index de performance ---
            statement.execute("CREATE INDEX IF NOT EXISTS idx_quests_history_uuid     ON player_quests_history(uuid);");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_quests_history_date     ON player_quests_history(completion_date);");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_global_stats_uuid       ON player_global_stats(uuid);");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_auction_expire          ON auction_house(expire_time);");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_auction_seller          ON auction_house(seller_uuid);");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_economy_balance         ON players_economy(balance DESC);");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_player_homes_uuid       ON player_homes(uuid);");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_transactions_uuid       ON player_transactions_history(uuid);");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_pending_rewards_uuid    ON genscore_pending_rewards(uuid);");

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

    // Methodes de Quetes, Recompenses et Moderation ont été deplacées dans leurs DAO respectifs


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
