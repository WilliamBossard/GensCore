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
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitaires pour la couche SQLite du module Lootr.
 * Exécutés entièrement en mémoire (SQLite :memory:), sans dépendance serveur Paper/Folia.
 */
public class LootDAOTest {

    private Connection conn;

    // DDL repris de LootDAO.initDatabase()
    private static final String DDL_CHESTS =
            "CREATE TABLE IF NOT EXISTS lootr_chests (" +
            "location VARCHAR(100) PRIMARY KEY, " +
            "loot_table VARCHAR(100) NOT NULL, " +
            "seed BIGINT NOT NULL, " +
            "size INT NOT NULL" +
            ");";

    private static final String DDL_PLAYER_CHESTS =
            "CREATE TABLE IF NOT EXISTS lootr_player_chests (" +
            "uuid VARCHAR(36) NOT NULL, " +
            "location VARCHAR(100) NOT NULL, " +
            "items_data TEXT, " +
            "PRIMARY KEY (uuid, location)" +
            ");";

    private static final String DDL_INDEX =
            "CREATE INDEX IF NOT EXISTS idx_lootr_player_chests_uuid ON lootr_player_chests(uuid);";

    @BeforeEach
    void setUp() throws Exception {
        conn = DriverManager.getConnection("jdbc:sqlite::memory:");
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(DDL_CHESTS);
            stmt.execute(DDL_PLAYER_CHESTS);
            stmt.execute(DDL_INDEX);
        }
    }

    @AfterEach
    void tearDown() throws Exception {
        if (conn != null && !conn.isClosed()) {
            conn.close();
        }
    }

    // =========================================================
    // Tests sur lootr_chests
    // =========================================================

    @Test
    @DisplayName("saveChest : insertion d'un nouveau coffre Lootr")
    void testSaveChest() throws Exception {
        String locKey = "world|100|64|-200";
        String lootTable = "minecraft:chests/simple_dungeon";
        long seed = 123456L;
        int size = 27;

        String sql = "INSERT INTO lootr_chests (location, loot_table, seed, size) VALUES (?, ?, ?, ?) " +
                     "ON CONFLICT(location) DO UPDATE SET loot_table=excluded.loot_table, seed=excluded.seed, size=excluded.size";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, locKey);
            ps.setString(2, lootTable);
            ps.setLong(3, seed);
            ps.setInt(4, size);
            ps.executeUpdate();
        }

        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT loot_table, seed, size FROM lootr_chests WHERE location = ?")) {
            ps.setString(1, locKey);
            try (ResultSet rs = ps.executeQuery()) {
                assertTrue(rs.next(), "Le coffre doit exister apres insertion");
                assertEquals(lootTable, rs.getString("loot_table"));
                assertEquals(seed, rs.getLong("seed"));
                assertEquals(size, rs.getInt("size"));
            }
        }
    }

    @Test
    @DisplayName("saveChest : UPSERT - mise a jour d'un coffre existant")
    void testSaveChestUpsert() throws Exception {
        String locKey = "world|0|100|0";
        String sql = "INSERT INTO lootr_chests (location, loot_table, seed, size) VALUES (?, ?, ?, ?) " +
                     "ON CONFLICT(location) DO UPDATE SET loot_table=excluded.loot_table, seed=excluded.seed, size=excluded.size";

        // Insertion initiale
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, locKey);
            ps.setString(2, "minecraft:chests/abandoned_mineshaft");
            ps.setLong(3, 111L);
            ps.setInt(4, 9);
            ps.executeUpdate();
        }

        // Mise a jour via UPSERT
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, locKey);
            ps.setString(2, "minecraft:chests/nether_bridge");
            ps.setLong(3, 999L);
            ps.setInt(4, 27);
            ps.executeUpdate();
        }

        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT loot_table, seed, size FROM lootr_chests WHERE location = ?")) {
            ps.setString(1, locKey);
            try (ResultSet rs = ps.executeQuery()) {
                assertTrue(rs.next());
                assertEquals("minecraft:chests/nether_bridge", rs.getString("loot_table"), "La loot table doit etre mise a jour");
                assertEquals(999L, rs.getLong("seed"), "Le seed doit etre mis a jour");
                assertEquals(27, rs.getInt("size"), "La taille doit etre mise a jour");
            }
        }
    }

    @Test
    @DisplayName("deleteChest : suppression d'un coffre et de ses donnees joueurs associees")
    void testDeleteChest() throws Exception {
        String locKey = "nether|50|64|50";
        UUID playerUUID = UUID.randomUUID();

        // Insertion coffre
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO lootr_chests (location, loot_table, seed, size) VALUES (?, ?, ?, ?)")) {
            ps.setString(1, locKey);
            ps.setString(2, "minecraft:chests/nether_bridge");
            ps.setLong(3, 42L);
            ps.setInt(4, 27);
            ps.executeUpdate();
        }

        // Insertion donnees joueur
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO lootr_player_chests (uuid, location, items_data) VALUES (?, ?, ?)")) {
            ps.setString(1, playerUUID.toString());
            ps.setString(2, locKey);
            ps.setString(3, "base64encodeddata==");
            ps.executeUpdate();
        }

        // Suppression coffre puis donnees joueurs associees (logique de deleteChest)
        try (PreparedStatement ps = conn.prepareStatement("DELETE FROM lootr_chests WHERE location = ?")) {
            ps.setString(1, locKey);
            ps.executeUpdate();
        }
        try (PreparedStatement ps = conn.prepareStatement("DELETE FROM lootr_player_chests WHERE location = ?")) {
            ps.setString(1, locKey);
            ps.executeUpdate();
        }

        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT 1 FROM lootr_chests WHERE location = ?")) {
            ps.setString(1, locKey);
            try (ResultSet rs = ps.executeQuery()) {
                assertFalse(rs.next(), "Le coffre ne doit plus exister apres suppression");
            }
        }

        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT 1 FROM lootr_player_chests WHERE uuid = ? AND location = ?")) {
            ps.setString(1, playerUUID.toString());
            ps.setString(2, locKey);
            try (ResultSet rs = ps.executeQuery()) {
                assertFalse(rs.next(), "Les donnees joueur doivent etre supprimees avec le coffre");
            }
        }
    }

    // =========================================================
    // Tests sur lootr_player_chests
    // =========================================================

    @Test
    @DisplayName("savePlayerLoot : persistance et rechargement des donnees d'inventaire en Base64")
    void testSaveAndLoadPlayerLoot() throws Exception {
        UUID uuid = UUID.randomUUID();
        String locKey = "world|200|70|300";
        String fakeBase64 = java.util.Base64.getEncoder().encodeToString("FAKE_ITEM_DATA".getBytes());

        String sql = "INSERT INTO lootr_player_chests (uuid, location, items_data) VALUES (?, ?, ?) " +
                     "ON CONFLICT(uuid, location) DO UPDATE SET items_data=excluded.items_data";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            ps.setString(2, locKey);
            ps.setString(3, fakeBase64);
            ps.executeUpdate();
        }

        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT items_data FROM lootr_player_chests WHERE uuid = ? AND location = ?")) {
            ps.setString(1, uuid.toString());
            ps.setString(2, locKey);
            try (ResultSet rs = ps.executeQuery()) {
                assertTrue(rs.next(), "Les donnees joueur doivent exister");
                String loaded = rs.getString("items_data");
                assertEquals(fakeBase64, loaded, "Les donnees Base64 doivent etre identiques apres rechargement");
                String decoded = new String(java.util.Base64.getDecoder().decode(loaded));
                assertEquals("FAKE_ITEM_DATA", decoded, "Le contenu decode doit correspondre aux donnees originales");
            }
        }
    }

    @Test
    @DisplayName("hasPlayerLooted : verification d'existence d'un loot joueur")
    void testHasPlayerLooted() throws Exception {
        UUID uuid = UUID.randomUUID();
        String locKey = "world|10|64|10";

        // Avant insertion : le joueur n'a pas pille
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT 1 FROM lootr_player_chests WHERE uuid = ? AND location = ? LIMIT 1")) {
            ps.setString(1, uuid.toString());
            ps.setString(2, locKey);
            try (ResultSet rs = ps.executeQuery()) {
                assertFalse(rs.next(), "Le joueur ne doit pas avoir pille avant insertion");
            }
        }

        // Insertion du loot
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO lootr_player_chests (uuid, location, items_data) VALUES (?, ?, ?)")) {
            ps.setString(1, uuid.toString());
            ps.setString(2, locKey);
            ps.setString(3, null);
            ps.executeUpdate();
        }

        // Apres insertion : le joueur a pille
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT 1 FROM lootr_player_chests WHERE uuid = ? AND location = ? LIMIT 1")) {
            ps.setString(1, uuid.toString());
            ps.setString(2, locKey);
            try (ResultSet rs = ps.executeQuery()) {
                assertTrue(rs.next(), "Le joueur doit avoir pille apres insertion");
            }
        }
    }

    @Test
    @DisplayName("loadLootedLocationsForPlayer : chargement de tous les emplacements pilles d'un joueur")
    void testLoadLootedLocations() throws Exception {
        UUID uuid = UUID.randomUUID();
        String[] locations = {"world|0|64|0", "world|100|64|100", "nether|50|50|50"};

        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO lootr_player_chests (uuid, location, items_data) VALUES (?, ?, ?)")) {
            for (String loc : locations) {
                ps.setString(1, uuid.toString());
                ps.setString(2, loc);
                ps.setString(3, null);
                ps.addBatch();
            }
            ps.executeBatch();
        }

        int count = 0;
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT location FROM lootr_player_chests WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) count++;
            }
        }
        assertEquals(3, count, "Les 3 emplacements pilles doivent etre charges depuis SQLite");
    }

    @Test
    @DisplayName("loadAllChests : chargement de l'integralite des coffres Lootr enregistres")
    void testLoadAllChests() throws Exception {
        String[][] chests = {
            {"world|10|64|10", "minecraft:chests/simple_dungeon", "100", "27"},
            {"world|20|64|20", "minecraft:chests/abandoned_mineshaft", "200", "9"},
            {"nether|0|64|0", "minecraft:chests/nether_bridge", "300", "27"}
        };

        String sql = "INSERT INTO lootr_chests (location, loot_table, seed, size) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (String[] c : chests) {
                ps.setString(1, c[0]);
                ps.setString(2, c[1]);
                ps.setLong(3, Long.parseLong(c[2]));
                ps.setInt(4, Integer.parseInt(c[3]));
                ps.addBatch();
            }
            ps.executeBatch();
        }

        int count = 0;
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) AS cnt FROM lootr_chests")) {
            if (rs.next()) count = rs.getInt("cnt");
        }
        assertEquals(3, count, "Les 3 coffres doivent etre charges depuis SQLite");
    }
}
