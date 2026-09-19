package fr.gens.core.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class HeadUtilTest {

    @Test
    @DisplayName("Extraction du hash de texture Mojang a partir du Base64")
    void testExtractHashFromValidBase64() {
        String json = "{\"textures\":{\"SKIN\":{\"url\":\"http://textures.minecraft.net/texture/4e5e40889279a7cd645391d4e08dd1d0fae6293ba17fa6b78e22858b92b60459\"}}}";
        String b64 = Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));

        String hash = HeadUtil.extractHash(b64);
        assertNotNull(hash);
        assertEquals("4e5e40889279a7cd645391d4e08dd1d0fae6293ba17fa6b78e22858b92b60459", hash);
    }

    @Test
    @DisplayName("Extraction du hash avec entrees nulles ou corrompues")
    void testExtractHashInvalid() {
        assertNull(HeadUtil.extractHash(null));
        assertNull(HeadUtil.extractHash(""));
        assertNull(HeadUtil.extractHash("invalid_base64_string"));
        String emptyJsonB64 = Base64.getEncoder().encodeToString("{}".getBytes(StandardCharsets.UTF_8));
        assertNull(HeadUtil.extractHash(emptyJsonB64));
    }

    @Test
    @DisplayName("Mise en cache et resolution des donnees de skin")
    void testCacheAndRetrieval() {
        UUID uuid = UUID.randomUUID();
        String username = "TestPlayer123";
        String val = "fakeBase64Val";
        String sig = "fakeSig";
        String hash = "abc123hash";

        HeadUtil.SkinData data = new HeadUtil.SkinData(val, sig, hash, username);
        HeadUtil.saveToCache(uuid, username, data, false);

        assertEquals(hash, HeadUtil.getSkinHashByUsername(username));
        assertEquals(hash, HeadUtil.getSkinHashByUsername(username.toLowerCase()));
        assertEquals(username, HeadUtil.getUsername(uuid));
    }
}
