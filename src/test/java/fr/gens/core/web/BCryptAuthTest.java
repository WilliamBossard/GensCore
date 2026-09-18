package fr.gens.core.web;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mindrot.jbcrypt.BCrypt;

import static org.junit.jupiter.api.Assertions.*;

public class BCryptAuthTest {

    @Test
    @DisplayName("Le hachage BCrypt et la vérification du mot de passe doivent correspondre")
    void testValidPasswordCheck() {
        String plainPassword = "SuperSecretAdminPassword2026!";
        String salt = BCrypt.gensalt(10);
        String hash = BCrypt.hashpw(plainPassword, salt);

        assertNotNull(hash);
        assertTrue(hash.startsWith("$2a$") || hash.startsWith("$2b$") || hash.startsWith("$2y$"), "Le hash doit avoir un préfixe BCrypt standard");
        assertTrue(BCrypt.checkpw(plainPassword, hash), "Le mot de passe valide doit être accepté");
    }

    @Test
    @DisplayName("Un mot de passe erroné doit être systématiquement rejeté")
    void testWrongPasswordCheck() {
        String plainPassword = "CorrectPassword";
        String hash = BCrypt.hashpw(plainPassword, BCrypt.gensalt());

        assertFalse(BCrypt.checkpw("IncorrectPassword", hash), "Un mauvais mot de passe doit être refusé");
        assertFalse(BCrypt.checkpw("correctpassword", hash), "La casse doit être strictement respectée");
        assertFalse(BCrypt.checkpw("", hash), "Une chaîne vide doit être refusée");
    }

    @Test
    @DisplayName("Chaque appel à gensalt() doit produire un sel et un condensat distincts (Anti Rainbow-Table)")
    void testSaltUniqueness() {
        String password = "IdenticalPassword";
        String hash1 = BCrypt.hashpw(password, BCrypt.gensalt());
        String hash2 = BCrypt.hashpw(password, BCrypt.gensalt());

        assertNotEquals(hash1, hash2, "Deux hachages du même mot de passe avec deux sels doivent être différents");
        assertTrue(BCrypt.checkpw(password, hash1));
        assertTrue(BCrypt.checkpw(password, hash2));
    }
}
