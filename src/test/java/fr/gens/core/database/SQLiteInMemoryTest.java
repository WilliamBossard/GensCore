package fr.gens.core.database;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class SQLiteInMemoryTest {

    private Connection conn;

    @BeforeEach
    void setUp() throws Exception {
        conn = DriverManager.getConnection("jdbc:sqlite::memory:");
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS players_economy (" +
                    "uuid VARCHAR(36) PRIMARY KEY, " +
                    "balance DOUBLE NOT NULL DEFAULT 0.0" +
                    ");");
        }
    }

    @AfterEach
    void tearDown() throws Exception {
        if (conn != null && !conn.isClosed()) {
            conn.close();
        }
    }

    @Test
    @DisplayName("Sauvegarde par lots (Write-Behind Cache) de plusieurs soldes dans SQLite")
    void testBatchSaveBalances() throws Exception {
        Map<UUID, Double> balances = new HashMap<>();
        UUID u1 = UUID.randomUUID();
        UUID u2 = UUID.randomUUID();
        UUID u3 = UUID.randomUUID();

        balances.put(u1, 1500.75);
        balances.put(u2, 250.00);
        balances.put(u3, 99999.99);

        String sql = "INSERT INTO players_economy (uuid, balance) VALUES (?, ?) " +
                     "ON CONFLICT(uuid) DO UPDATE SET balance = excluded.balance";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (Map.Entry<UUID, Double> entry : balances.entrySet()) {
                ps.setString(1, entry.getKey().toString());
                ps.setDouble(2, entry.getValue());
                ps.addBatch();
            }
            int[] results = ps.executeBatch();
            assertEquals(3, results.length, "3 requêtes par lot doivent être exécutées");
        }

        // Vérification de la persistance exacte des soldes
        try (PreparedStatement ps = conn.prepareStatement("SELECT balance FROM players_economy WHERE uuid = ?")) {
            ps.setString(1, u1.toString());
            try (ResultSet rs = ps.executeQuery()) {
                assertTrue(rs.next());
                assertEquals(1500.75, rs.getDouble("balance"), 0.001);
            }

            ps.setString(1, u2.toString());
            try (ResultSet rs = ps.executeQuery()) {
                assertTrue(rs.next());
                assertEquals(250.00, rs.getDouble("balance"), 0.001);
            }
        }
    }

    @Test
    @DisplayName("Mise à jour idempotente d'un solde existant (ON CONFLICT DO UPDATE)")
    void testUpdateBalanceOnConflict() throws Exception {
        UUID uuid = UUID.randomUUID();
        String sql = "INSERT INTO players_economy (uuid, balance) VALUES (?, ?) " +
                     "ON CONFLICT(uuid) DO UPDATE SET balance = excluded.balance";

        // Premier insert
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            ps.setDouble(2, 100.0);
            ps.executeUpdate();
        }

        // Mise à jour ultérieure
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            ps.setDouble(2, 350.50);
            ps.executeUpdate();
        }

        // Vérification que le solde est bien mis à jour
        try (PreparedStatement ps = conn.prepareStatement("SELECT balance FROM players_economy WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                assertTrue(rs.next());
                assertEquals(350.50, rs.getDouble("balance"), 0.001);
            }
        }
    }
}
