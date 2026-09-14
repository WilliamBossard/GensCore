package fr.gens.core.utils;

import fr.gens.core.CorePlugin;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

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

        // NOTE ARCHITECTURALE (Performance Pool & SQLite WAL) :
        // Grâce au mode WAL ('PRAGMA journal_mode=WAL'), SQLite supporte sans blocage de multiples
        // lecteurs simultanés. Augmenter le pool à 10 connexions permet aux requêtes HTTP du serveur
        // web Javalin (classements, profils, casino) et aux lectures asynchrones en jeu de s'exécuter
        // en parallèle sans jamais bloquer le thread principal ni les transactions économiques.
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(30000);
        
        // SQLite properties for WAL and concurrency
        config.setConnectionInitSql("PRAGMA journal_mode=WAL; PRAGMA synchronous=NORMAL; PRAGMA busy_timeout=5000; PRAGMA cache_size=-20000; PRAGMA temp_store=MEMORY;");
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
        try {
            try (Connection conn = getConnection()) {
                if (conn != null && !conn.isClosed()) {
                    plugin.getLangManager().sendConsoleMessage("db.tables_ready");
                    // Les tables sont désormais créées dynamiquement par chaque module (initDatabase)
                    plugin.getLangManager().sendConsoleMessage("db.tables_init_success");
                }
            }
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

    public void executeStatement(String sql) {
        try (Connection conn = getConnection();
             Statement statement = conn.createStatement()) {
            statement.execute(sql);
        } catch (SQLException e) {
            plugin.getLangManager().sendConsoleError("Erreur SQL: " + sql);
            e.printStackTrace();
        }
    }

    public void wipeServerData() {
        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            try (Statement stmt = conn.createStatement();
                 java.sql.ResultSet rs = stmt.executeQuery("SELECT name FROM sqlite_master WHERE type='table'")) {
                
                java.util.List<String> tables = new java.util.ArrayList<>();
                while (rs.next()) {
                    String tableName = rs.getString(1);
                    if (!tableName.equals("sqlite_sequence") && 
                        !tableName.equals("shop_categories") && 
                        !tableName.equals("shop_items")) {
                        tables.add(tableName);
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
            plugin.getLogger().severe("Failed to wipe database: " + e.getMessage());
            e.printStackTrace();
        }
    }

}



