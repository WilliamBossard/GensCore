package fr.gens.core.modules.teams;

import org.bukkit.Material;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class TeamColorPaletteTest {

    @Test
    @DisplayName("Verification des couleurs de la palette BlueMap")
    void testPaletteEntries() {
        TeamGui.ColorEntry[] palette = TeamGui.getColorPalette();
        assertNotNull(palette);
        assertEquals(8, palette.length);

        for (TeamGui.ColorEntry entry : palette) {
            assertNotNull(entry.material);
            assertTrue(entry.material.name().endsWith("_WOOL"));
            assertNotNull(entry.hex);
            assertTrue(entry.hex.matches("^#[0-9a-fA-F]{6}$"), "Code hex invalide: " + entry.hex);
            assertNotNull(entry.colorName);
            assertNotNull(entry.bedrockName);
        }
    }

    @Test
    @DisplayName("Verification de la couleur par defaut d'une guilde")
    void testDefaultTeamColor() {
        TeamData team = new TeamData(1, "TestTeam", UUID.randomUUID());
        assertEquals("#2ecc71", team.getColor());

        team.setColor("#3498db");
        assertEquals("#3498db", team.getColor());
    }
}
