package fr.gens.core.modules.teams;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class TeamBankTest {

    private TeamData team;
    private final UUID leaderUuid = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        team = new TeamData(1, "AlphaGuild", leaderUuid);
    }

    @Test
    @DisplayName("Solde initial de la banque de guilde a 0.0$ et 0 XP")
    void testInitialBankBalance() {
        assertEquals(0.0, team.getBankBalance(), 0.001);
        assertEquals(0, team.getBankXp());
        assertEquals("#2ecc71", team.getColor());
    }

    @Test
    @DisplayName("Depot d'argent valide et rejet des valeurs invalides")
    void testDepositMoney() {
        team.addBankBalance(1500.0);
        assertEquals(1500.0, team.getBankBalance(), 0.001);

        // Valeurs invalides ignorees
        team.addBankBalance(-500.0);
        assertEquals(1500.0, team.getBankBalance(), 0.001);

        team.addBankBalance(Double.NaN);
        assertEquals(1500.0, team.getBankBalance(), 0.001);

        team.addBankBalance(Double.POSITIVE_INFINITY);
        assertEquals(1500.0, team.getBankBalance(), 0.001);
    }

    @Test
    @DisplayName("Retrait d'argent avec verification de solvabilite")
    void testWithdrawMoney() {
        team.setBankBalance(2500.0);

        assertTrue(team.withdrawBankBalance(1000.0));
        assertEquals(1500.0, team.getBankBalance(), 0.001);

        // Retrait superieur au solde refuse
        assertFalse(team.withdrawBankBalance(2000.0));
        assertEquals(1500.0, team.getBankBalance(), 0.001);

        // Retrait negatif refuse
        assertFalse(team.withdrawBankBalance(-100.0));
        assertEquals(1500.0, team.getBankBalance(), 0.001);

        // Retrait exact autorise
        assertTrue(team.withdrawBankBalance(1500.0));
        assertEquals(0.0, team.getBankBalance(), 0.001);
    }

    @Test
    @DisplayName("Depot et retrait de niveaux d'XP")
    void testDepositAndWithdrawXp() {
        team.addBankXp(35);
        assertEquals(35, team.getBankXp());

        // Depot negatif ignore
        team.addBankXp(-10);
        assertEquals(35, team.getBankXp());

        // Retrait partiel
        assertTrue(team.withdrawBankXp(15));
        assertEquals(20, team.getBankXp());

        // Retrait superieur au solde refuse
        assertFalse(team.withdrawBankXp(25));
        assertEquals(20, team.getBankXp());

        // Retrait negatif refuse
        assertFalse(team.withdrawBankXp(-5));
        assertEquals(20, team.getBankXp());
    }

    @Test
    @DisplayName("Validation du code couleur hexadécimal de guilde")
    void testTeamColorValidation() {
        team.setColor("#e74c3c");
        assertEquals("#e74c3c", team.getColor());

        // Format invalide ignore
        team.setColor("invalid_color");
        assertEquals("#e74c3c", team.getColor());

        team.setColor("#12345"); // 5 caracteres
        assertEquals("#e74c3c", team.getColor());

        team.setColor("#1234567"); // 7 caracteres
        assertEquals("#e74c3c", team.getColor());

        team.setColor("#3498DB"); // Majuscules valides
        assertEquals("#3498DB", team.getColor());
    }
}
