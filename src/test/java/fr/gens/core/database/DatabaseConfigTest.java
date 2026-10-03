package fr.gens.core.database;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

public class DatabaseConfigTest {

    @Test
    @DisplayName("Validation que la base par defaut de config.yml est bien SQLite")
    void testDefaultDatabaseIsSqlite() {
        InputStream stream = getClass().getClassLoader().getResourceAsStream("config.yml");
        assertNotNull(stream, "Le fichier config.yml doit etre present dans les ressources");

        YamlConfiguration config = YamlConfiguration.loadConfiguration(new InputStreamReader(stream, StandardCharsets.UTF_8));
        String type = config.getString("database.type", "sqlite");

        assertEquals("sqlite", type, "Par defaut, database.type doit imperativement etre 'sqlite'");
        assertEquals("genscore.db", config.getString("database.sqlite.file"));
    }

    @Test
    @DisplayName("Validation de la presence de la section optionnelle MySQL dans config.yml")
    void testOptionalMysqlSectionExists() {
        InputStream stream = getClass().getClassLoader().getResourceAsStream("config.yml");
        assertNotNull(stream);

        YamlConfiguration config = YamlConfiguration.loadConfiguration(new InputStreamReader(stream, StandardCharsets.UTF_8));
        
        assertTrue(config.contains("database.mysql"), "La section database.mysql optionnelle doit etre presente");
        assertEquals("localhost", config.getString("database.mysql.host"));
        assertEquals(3306, config.getInt("database.mysql.port"));
        assertEquals("genscore", config.getString("database.mysql.database"));
        assertEquals("root", config.getString("database.mysql.username"));
        assertEquals(10, config.getInt("database.mysql.max_pool_size"));
    }
}
