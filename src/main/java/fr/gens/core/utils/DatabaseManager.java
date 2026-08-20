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
         try (Connection conn = getConnection()) {
             
            plugin.getLangManager().sendConsoleMessage("db.tables_ready");
            // Les tables sont désormais créées dynamiquement par chaque module (initDatabase)
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

    public void executeStatement(String sql) {
        try (Connection conn = getConnection();
             Statement statement = conn.createStatement()) {
            statement.execute(sql);
        } catch (SQLException e) {
            plugin.getLangManager().sendConsoleError("Erreur SQL: " + sql);
            e.printStackTrace();
        }
    }

}

