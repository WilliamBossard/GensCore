package fr.gens.core.utils;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class ViaVersionUtilTest {

    @Test
    public void testFormatProtocolVersion() {
        assertEquals("26.3+", ViaVersionUtil.formatProtocolVersion(770));
        assertEquals("26.3+", ViaVersionUtil.formatProtocolVersion(775));
        assertEquals("26.2 / 1.21.4 (Via)", ViaVersionUtil.formatProtocolVersion(769));
        assertEquals("1.21.2-1.21.3 (Via)", ViaVersionUtil.formatProtocolVersion(768));
        assertEquals("1.21-1.21.1 (Via)", ViaVersionUtil.formatProtocolVersion(767));
        assertEquals("1.20.5-1.20.6 (Via)", ViaVersionUtil.formatProtocolVersion(766));
        assertEquals("1.20.3-1.20.4 (Via)", ViaVersionUtil.formatProtocolVersion(765));
        assertEquals("1.20.2 (Via)", ViaVersionUtil.formatProtocolVersion(764));
        assertEquals("1.20-1.20.1 (Via)", ViaVersionUtil.formatProtocolVersion(763));
        assertEquals("1.19.4 (Via)", ViaVersionUtil.formatProtocolVersion(762));
        assertEquals("1.18.2 (Via)", ViaVersionUtil.formatProtocolVersion(758));
        assertEquals("1.17.1 (Via)", ViaVersionUtil.formatProtocolVersion(756));
        assertEquals("1.16.4-1.16.5 (Via)", ViaVersionUtil.formatProtocolVersion(754));
        assertEquals("Legacy <1.16 (Via)", ViaVersionUtil.formatProtocolVersion(340));
    }

    @Test
    public void testGetPlayerVersionNameNullSafety() {
        assertEquals("Inconnu", ViaVersionUtil.getPlayerVersionName(null));
        UUID testUuid = UUID.randomUUID();
        String version = ViaVersionUtil.getPlayerVersionName(testUuid);
        assertNotNull(version);
        assertFalse(version.isEmpty());
    }

    @Test
    public void testGetSafeMenuMaterial() {
        UUID testUuid = UUID.randomUUID();
        Material oak = ViaVersionUtil.getSafeMenuMaterial(Material.OAK_LOG, testUuid);
        assertEquals(Material.OAK_LOG, oak);
        Material nullResult = ViaVersionUtil.getSafeMenuMaterial(null, testUuid);
        assertEquals(Material.BARRIER, nullResult);
    }

    @Test
    public void testFormatProtocolEdgeCases() {
        assertEquals("Protocole 0", ViaVersionUtil.formatProtocolVersion(0));
        assertEquals("Protocole -1", ViaVersionUtil.formatProtocolVersion(-1));
    }
}
