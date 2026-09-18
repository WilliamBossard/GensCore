package fr.gens.core.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class BedrockFormTest {

    @Test
    @DisplayName("Un texte null passé à clean() doit retourner null")
    void testNullText() {
        assertNull(BedrockFormManager.clean(null));
    }

    @Test
    @DisplayName("Les emojis courants non supportés par Bedrock doivent être convertis en caractères ASCII")
    void testEmojiReplacement() {
        String input = "Boutique ⭐ Armes ✧ Epées ✦ Go ▶ Suivant » Précédent « Vie ❤";
        String cleaned = BedrockFormManager.clean(input);

        assertTrue(cleaned.contains("*"), "⭐ et ✧ doivent devenir *");
        assertTrue(cleaned.contains(">"), "▶ et » doivent devenir >");
        assertTrue(cleaned.contains("<"), "« doit devenir <");
        assertTrue(cleaned.contains("<3"), "❤ doit devenir <3");
        assertFalse(cleaned.contains("⭐"), "L'emoji ⭐ ne doit plus être présent");
        assertFalse(cleaned.contains("❤"), "L'emoji ❤ ne doit plus être présent");
    }

    @Test
    @DisplayName("Les balises et codes de mise en gras doivent être supprimés")
    void testBoldRemoval() {
        String input = "<bold>Bienvenue</bold> §lJoueur &lVIP";
        String cleaned = BedrockFormManager.clean(input);

        assertFalse(cleaned.contains("<bold>"), "La balise <bold> doit être retirée");
        assertFalse(cleaned.contains("§l"), "Le code §l doit être retiré");
        assertFalse(cleaned.contains("&l"), "Le code &l doit être retiré");
        assertTrue(cleaned.contains("Bienvenue"), "Le texte brut doit subsister");
        assertTrue(cleaned.contains("Joueur"), "Le texte brut doit subsister");
    }

    @Test
    @DisplayName("Les accents français doivent être intégralement préservés pour l'i18n")
    void testFrenchAccentsPreserved() {
        String input = "Épée enchantée à l'orée de la forêt où le héros fêtera Noël";
        String cleaned = BedrockFormManager.clean(input);

        assertEquals(input, cleaned, "Les caractères accentués français doivent être préservés sans altération");
    }

    @Test
    @DisplayName("Les caractères Unicode de la zone d'usage privé (PUA) doivent être éliminés")
    void testPrivateUnicodeStripped() {
        String input = "Texte normal \uE001 icône custom \uF8FF fin";
        String cleaned = BedrockFormManager.clean(input);

        assertFalse(cleaned.contains("\uE001"));
        assertFalse(cleaned.contains("\uF8FF"));
        assertTrue(cleaned.contains("Texte normal  icône custom  fin"));
    }
}
