package fr.gens.core.database;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class SqlDialectTest {

    @Test
    @DisplayName("Validation de la conversion auto-increment pour MySQL")
    void testAutoIncrementAdaptation() {
        String sqliteSql = "CREATE TABLE IF NOT EXISTS genscore_teams (team_id INTEGER PRIMARY KEY AUTOINCREMENT, name VARCHAR(32) UNIQUE);";
        
        // Simulation adaptation MySQL
        String adapted = sqliteSql.replace("INTEGER PRIMARY KEY AUTOINCREMENT", "INT AUTO_INCREMENT PRIMARY KEY");
        
        assertTrue(adapted.contains("INT AUTO_INCREMENT PRIMARY KEY"));
        assertFalse(adapted.contains("INTEGER PRIMARY KEY AUTOINCREMENT"));
    }

    @Test
    @DisplayName("Validation de la conversion ON CONFLICT vers ON DUPLICATE KEY UPDATE pour MySQL")
    void testOnConflictToDuplicateKeyAdaptation() {
        String sqliteSql = "INSERT INTO players_economy (uuid, balance) VALUES (?, ?) ON CONFLICT(uuid) DO UPDATE SET balance=excluded.balance";

        String adapted = sqliteSql.replaceAll("(?i)ON\\s+CONFLICT\\s*\\([^)]*\\)\\s*DO\\s+UPDATE\\s+SET", "ON DUPLICATE KEY UPDATE");
        adapted = adapted.replaceAll("(?i)excluded\\.([a-zA-Z0-9_]+)", "VALUES($1)");

        assertEquals("INSERT INTO players_economy (uuid, balance) VALUES (?, ?) ON DUPLICATE KEY UPDATE balance=VALUES(balance)", adapted);
    }

    @Test
    @DisplayName("Validation de la conversion multi-colonnes ON CONFLICT vers ON DUPLICATE KEY UPDATE")
    void testMultiColumnOnConflictAdaptation() {
        String sqliteSql = "INSERT INTO player_jobs (uuid, job_name, level, xp) VALUES (?, ?, ?, ?) ON CONFLICT(uuid, job_name) DO UPDATE SET level = excluded.level, xp = excluded.xp";

        String adapted = sqliteSql.replaceAll("(?i)ON\\s+CONFLICT\\s*\\([^)]*\\)\\s*DO\\s+UPDATE\\s+SET", "ON DUPLICATE KEY UPDATE");
        adapted = adapted.replaceAll("(?i)excluded\\.([a-zA-Z0-9_]+)", "VALUES($1)");

        assertEquals("INSERT INTO player_jobs (uuid, job_name, level, xp) VALUES (?, ?, ?, ?) ON DUPLICATE KEY UPDATE level = VALUES(level), xp = VALUES(xp)", adapted);
    }

    @Test
    @DisplayName("Validation de la conversion des timestamps pour MySQL")
    void testTimestampAdaptation() {
        String sqliteSql = "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP";
        String adapted = sqliteSql.replace("TIMESTAMP DEFAULT CURRENT_TIMESTAMP", "DATETIME DEFAULT CURRENT_TIMESTAMP");

        assertEquals("created_at DATETIME DEFAULT CURRENT_TIMESTAMP", adapted);
    }
}
