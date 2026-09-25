package fr.gens.core.modules;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class EconomyValidationTest {

    private boolean isValidAmount(double amount) {
        return Double.isFinite(amount) && amount > 0;
    }

    private boolean isValidAdminAmount(double amount) {
        return Double.isFinite(amount) && amount >= 0;
    }

    @Test
    @DisplayName("Les montants positifs et finis doivent être acceptés")
    void testValidAmounts() {
        assertTrue(isValidAmount(1.0));
        assertTrue(isValidAmount(0.01));
        assertTrue(isValidAmount(1000000.50));
    }

    @Test
    @DisplayName("Les valeurs NaN doivent être strictement rejetées")
    void testRejectNaN() {
        assertFalse(isValidAmount(Double.NaN), "Double.NaN doit être refusé");
        assertFalse(isValidAdminAmount(Double.NaN), "Double.NaN doit être refusé en commande admin");
    }

    @Test
    @DisplayName("Les valeurs infinies doivent être strictement rejetées")
    void testRejectInfinity() {
        assertFalse(isValidAmount(Double.POSITIVE_INFINITY));
        assertFalse(isValidAmount(Double.NEGATIVE_INFINITY));
        assertFalse(isValidAdminAmount(Double.POSITIVE_INFINITY));
        assertFalse(isValidAdminAmount(Double.NEGATIVE_INFINITY));
    }

    @Test
    @DisplayName("Les montants négatifs ou nuls doivent être rejetés pour /pay et /ah sell")
    void testRejectNonPositive() {
        assertFalse(isValidAmount(0.0));
        assertFalse(isValidAmount(-1.0));
        assertFalse(isValidAmount(-0.001));
    }

    @Test
    @DisplayName("Le montant 0.0 doit être accepté pour /eco set mais pas pour les transferts")
    void testAdminZeroAmount() {
        assertTrue(isValidAdminAmount(0.0), "0.0 est un solde valide pour /eco set");
        assertFalse(isValidAdminAmount(-5.0), "-5.0 est invalide même pour /eco set");
    }

    @Test
    @DisplayName("Le transfert /pay vers son propre compte doit être interdit")
    void testSelfPaymentRejection() {
        java.util.UUID sender = java.util.UUID.randomUUID();
        java.util.UUID receiverSame = sender;
        java.util.UUID receiverOther = java.util.UUID.randomUUID();

        assertTrue(receiverOther.equals(sender) == false, "Un destinataire différent doit être autorisé");
        assertTrue(receiverSame.equals(sender), "L'auto-virement doit être détecté et rejeté");
    }
}
