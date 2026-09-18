package fr.gens.core.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ItemSerializerTest {

    @Test
    @DisplayName("La sérialisation d'un item null doit retourner null")
    void testSerializeNull() {
        assertNull(ItemSerializer.toBase64(null));
    }

    @Test
    @DisplayName("La désérialisation d'une chaîne nulle ou vide doit retourner null sans exception")
    void testDeserializeNullOrEmpty() {
        assertNull(ItemSerializer.fromBase64(null));
        assertNull(ItemSerializer.fromBase64(""));
    }

    @Test
    @DisplayName("La désérialisation d'une chaîne corrompue ou invalide doit retourner null de manière résiliente sans crash")
    void testDeserializeCorruptedData() {
        assertNull(ItemSerializer.fromBase64("corrupted_base64_data_!@#$%^&*()"));
        assertNull(ItemSerializer.fromBase64("rO0ABXNyABFqYXZhLnV0aWwuSGFzaE1hcAUH2sB4RwFN"));
    }
}
