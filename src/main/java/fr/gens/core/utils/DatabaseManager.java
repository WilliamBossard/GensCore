package fr.gens.core.utils;

import fr.gens.core.CorePlugin;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

public class DatabaseManager {

    private final CorePlugin plugin;
    private HikariDataSource dataSource;
    private String databaseType = "sqlite";

    public DatabaseManager(CorePlugin plugin) {
        this.plugin = plugin;
        connect();
        initTables();
    }

    public String getDatabaseType() {
        return databaseType;
    }

    public boolean isMySQL() {
        return "mysql".equalsIgnoreCase(databaseType);
    }

    private void connect() {
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }

        FileConfiguration cfg = null;
        try {
            if (plugin.getConfigManager() != null) {
                cfg = plugin.getConfigManager().getConfig("config.yml");
            }
        } catch (Exception ignored) {}

        if (cfg == null) {
            cfg = plugin.getConfig();
        }

        String type = (cfg != null) ? cfg.getString("database.type", "sqlite").trim().toLowerCase() : "sqlite";

        if ("mysql".equals(type) || "mariadb".equals(type)) {
            boolean success = connectMySQL(cfg);
            if (!success) {
                plugin.getLogger().warning("[Database] Echec de la connexion MySQL. Bascule automatique vers la base locale SQLite.");
                connectSQLite(cfg);
            }
        } else {
            connectSQLite(cfg);
        }
    }

    private boolean connectMySQL(FileConfiguration cfg) {
        String host = cfg.getString("database.mysql.host", "localhost");
        int port = cfg.getInt("database.mysql.port", 3306);
        String database = cfg.getString("database.mysql.database", "genscore");
        String username = cfg.getString("database.mysql.username", "root");
        String password = cfg.getString("database.mysql.password", "");
        boolean ssl = cfg.getBoolean("database.mysql.ssl", false);
        int maxPool = cfg.getInt("database.mysql.max_pool_size", 10);
        int minIdle = cfg.getInt("database.mysql.minimum_idle", 2);
        int timeout = cfg.getInt("database.mysql.connection_timeout", 30000);

        String jdbcUrl = "jdbc:mariadb://" + host + ":" + port + "/" + database + 
                         "?useSSL=" + ssl + "&autoReconnect=true&characterEncoding=utf8";

        try {
            Class.forName("org.mariadb.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            plugin.getLogger().warning("[Database] Pilote MariaDB/MySQL introuvable dans le classpath.");
            return false;
        }

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(username);
        config.setPassword(password);
        config.setPoolName("GensCore-MySQL-Pool");
        config.setMaximumPoolSize(maxPool);
        config.setMinimumIdle(minIdle);
        config.setConnectionTimeout(timeout);
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

        try {
            HikariDataSource ds = new HikariDataSource(config);
            // Test de connectivite
            try (Connection testConn = ds.getConnection()) {
                if (testConn != null && !testConn.isClosed()) {
                    this.dataSource = ds;
                    this.databaseType = "mysql";
                    plugin.getLogger().info("[Database] Connexion au serveur MySQL/MariaDB etablie (" + host + ":" + port + "/" + database + ").");
                    return true;
                }
            }
            ds.close();
            return false;
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "[Database] Erreur de connexion a la base MySQL distantes: " + e.getMessage());
            return false;
        }
    }

    private void connectSQLite(FileConfiguration cfg) {
        String fileName = "genscore.db";
        if (cfg != null) {
            fileName = cfg.getString("database.sqlite.file", cfg.getString("database.file", "genscore.db"));
        }

        File dataFile = new File(plugin.getDataFolder(), fileName);
        String url = "jdbc:sqlite:" + dataFile.getAbsolutePath();

        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            plugin.getLangManager().sendConsoleError("error.sqlite_driver");
        }

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(url);
        config.setPoolName("GensCore-SQLite-Pool");
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(30000);

        // Parametres SQLite optimaux pour WAL et multithreading
        config.setConnectionInitSql("PRAGMA journal_mode=WAL; PRAGMA synchronous=NORMAL; PRAGMA busy_timeout=5000; PRAGMA cache_size=-20000; PRAGMA temp_store=MEMORY;");
        config.addDataSourceProperty("journal_mode", "WAL");
        config.addDataSourceProperty("synchronous", "NORMAL");
        config.addDataSourceProperty("busy_timeout", "5000");

        try {
            this.dataSource = new HikariDataSource(config);
            this.databaseType = "sqlite";
            plugin.getLangManager().sendConsoleMessage("db.pool_init");
        } catch (Exception e) {
            plugin.getLangManager().sendConsoleError("db.pool_error");
            plugin.getLogger().log(Level.SEVERE, "Erreur lors de l'initialisation du pool HikariCP (SQLite)", e);
        }
    }

    public String adaptQuery(String sql) {
        if (sql == null) return "";
        if (isMySQL()) {
            sql = sql.replace("INTEGER PRIMARY KEY AUTOINCREMENT", "INT AUTO_INCREMENT PRIMARY KEY");
            sql = sql.replace("integer primary key autoincrement", "INT AUTO_INCREMENT PRIMARY KEY");
            sql = sql.replace("TIMESTAMP DEFAULT CURRENT_TIMESTAMP", "DATETIME DEFAULT CURRENT_TIMESTAMP");

            if (sql.contains("ON CONFLICT") || sql.contains("on conflict")) {
                sql = sql.replaceAll("(?i)ON\\s+CONFLICT\\s*\\([^)]*\\)\\s*DO\\s+UPDATE\\s+SET", "ON DUPLICATE KEY UPDATE");
                sql = sql.replaceAll("(?i)excluded\\.([a-zA-Z0-9_]+)", "VALUES($1)");
            }
        }
        return sql;
    }

    private void initTables() {
        try {
            try (Connection conn = getConnection()) {
                if (conn != null && !conn.isClosed()) {
                    plugin.getLangManager().sendConsoleMessage("db.tables_ready");
                    try (Statement stmt = conn.createStatement()) {
                        String createRewards = adaptQuery("CREATE TABLE IF NOT EXISTS player_web_rewards (" +
                                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                                "uuid VARCHAR(36) NOT NULL, " +
                                "material VARCHAR(64) NOT NULL, " +
                                "amount INTEGER NOT NULL, " +
                                "base64_data TEXT, " +
                                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP);");
                        stmt.execute(createRewards);

                        if (isMySQL()) {
                            try {
                                stmt.execute("CREATE INDEX idx_player_web_rewards_uuid ON player_web_rewards(uuid);");
                            } catch (SQLException ignored) {
                                // Index deja existant sur MySQL
                            }
                        } else {
                            stmt.execute("CREATE INDEX IF NOT EXISTS idx_player_web_rewards_uuid ON player_web_rewards(uuid);");
                        }
                    }
                    plugin.getLangManager().sendConsoleMessage("db.tables_init_success");
                }
            }
        } catch (SQLException e) {
            plugin.getLangManager().sendConsoleError("db.tables_init_error");
            plugin.getLogger().log(Level.SEVERE, "Erreur lors de l'initialisation des tables de la base de donnees", e);
        }
    }

    public Connection getConnection() throws SQLException {
        if (dataSource == null) {
            throw new SQLException("Pool HikariCP non initialise.");
        }
        return dataSource.getConnection();
    }

    public PreparedStatement prepareStatement(Connection conn, String sql) throws SQLException {
        return conn.prepareStatement(adaptQuery(sql));
    }

    public PreparedStatement prepareStatement(Connection conn, String sql, int autoGeneratedKeys) throws SQLException {
        return conn.prepareStatement(adaptQuery(sql), autoGeneratedKeys);
    }

    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            plugin.getLangManager().sendConsoleMessage("db.pool_closed");
        }
    }

    public void executeStatement(String sql) {
        String adapted = adaptQuery(sql);
        try (Connection conn = getConnection();
             Statement statement = conn.createStatement()) {
            statement.execute(adapted);
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Erreur SQL lors de l'execution: " + adapted, e);
        }
    }

    public void addColumnIfNotExists(String table, String column, String definition) {
        try (Connection conn = getConnection()) {
            boolean exists = false;
            if (isMySQL()) {
                try (PreparedStatement stmt = conn.prepareStatement(
                        "SELECT COLUMN_NAME FROM information_schema.COLUMNS WHERE TABLE_NAME = ? AND COLUMN_NAME = ?")) {
                    stmt.setString(1, table);
                    stmt.setString(2, column);
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) exists = true;
                    }
                }
            } else {
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery("PRAGMA table_info(" + table + ")")) {
                    while (rs.next()) {
                        if (column.equalsIgnoreCase(rs.getString("name"))) {
                            exists = true;
                            break;
                        }
                    }
                }
            }
            if (!exists) {
                try (Statement stmt = conn.createStatement()) {
                    stmt.execute("ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition);
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Impossible d'ajouter la colonne " + column + " a la table " + table, e);
        }
    }

    public void wipeServerData() {
        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            try (Statement stmt = conn.createStatement()) {
                String query = isMySQL()
                    ? "SELECT table_name FROM information_schema.tables WHERE table_schema = DATABASE()"
                    : "SELECT name FROM sqlite_master WHERE type='table'";

                List<String> tables = new ArrayList<>();
                try (ResultSet rs = stmt.executeQuery(query)) {
                    while (rs.next()) {
                        String tableName = rs.getString(1);
                        if (!tableName.equals("sqlite_sequence") && 
                            !tableName.equalsIgnoreCase("shop_categories") && 
                            !tableName.equalsIgnoreCase("shop_items")) {
                            tables.add(tableName);
                        }
                    }
                }

                for (String table : tables) {
                    stmt.executeUpdate("DELETE FROM " + table);
                }
                conn.commit();
                plugin.getLogger().info("Wipe success: all player tables have been cleared.");
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to wipe database: " + e.getMessage(), e);
        }
    }

    public int getActiveConnections() {
        if (dataSource != null && dataSource.getHikariPoolMXBean() != null) {
            return dataSource.getHikariPoolMXBean().getActiveConnections();
        }
        return 0;
    }

    public int getIdleConnections() {
        if (dataSource != null && dataSource.getHikariPoolMXBean() != null) {
            return dataSource.getHikariPoolMXBean().getIdleConnections();
        }
        return 0;
    }

    public int getTotalConnections() {
        if (dataSource != null && dataSource.getHikariPoolMXBean() != null) {
            return dataSource.getHikariPoolMXBean().getTotalConnections();
        }
        return 0;
    }

    public int getThreadsAwaitingConnection() {
        if (dataSource != null && dataSource.getHikariPoolMXBean() != null) {
            return dataSource.getHikariPoolMXBean().getThreadsAwaitingConnection();
        }
        return 0;
    }

    public HikariDataSource getDataSource() {
        return dataSource;
    }
}
