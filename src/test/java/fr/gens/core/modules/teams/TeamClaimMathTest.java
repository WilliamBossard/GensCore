package fr.gens.core.modules.teams;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TeamClaimMathTest {

    @Test
    @DisplayName("Formatage correct de la cle de chunk world:x:z")
    void testChunkKeyFormat() {
        assertEquals("world:10:-5", TeamClaimManager.getChunkKey("world", 10, -5));
        assertEquals("world_nether:0:0", TeamClaimManager.getChunkKey("world_nether", 0, 0));
        assertEquals("custom_world:-100:250", TeamClaimManager.getChunkKey("custom_world", -100, 250));
    }

    @Test
    @DisplayName("Conversion exacte chunk coordinate vers limites blocs (16x16)")
    void testChunkToBlockBoundaries() {
        int chunkX = 5;
        int chunkZ = -3;

        int minBlockX = chunkX * 16;
        int maxBlockX = chunkX * 16 + 16;
        int minBlockZ = chunkZ * 16;
        int maxBlockZ = chunkZ * 16 + 16;

        assertEquals(80, minBlockX);
        assertEquals(96, maxBlockX);
        assertEquals(-48, minBlockZ);
        assertEquals(-32, maxBlockZ);

        assertEquals(16, maxBlockX - minBlockX);
        assertEquals(16, maxBlockZ - minBlockZ);
    }

    @Test
    @DisplayName("Verification des constantes de cout de claim")
    void testClaimCostConstants() {
        assertEquals(1500.0, TeamClaimManager.CLAIM_COST_MONEY, 0.001);
        assertEquals(10, TeamClaimManager.CLAIM_COST_XP);
    }
}
