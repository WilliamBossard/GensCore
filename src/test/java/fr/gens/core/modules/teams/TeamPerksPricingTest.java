package fr.gens.core.modules.teams;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class TeamPerksPricingTest {

    private TeamData team;

    @BeforeEach
    void setUp() {
        team = new TeamData(10, "Titans", UUID.randomUUID());
    }

    @Test
    @DisplayName("Verification des couts en dollars pour chaque niveau de perk")
    void testPerkCostsMoney() {
        // MEMBERS (1..3)
        assertEquals(5000.0, TeamManager.getPerkCostMoney("MEMBERS", 1), 0.001);
        assertEquals(15000.0, TeamManager.getPerkCostMoney("MEMBERS", 2), 0.001);
        assertEquals(35000.0, TeamManager.getPerkCostMoney("MEMBERS", 3), 0.001);
        assertEquals(-1.0, TeamManager.getPerkCostMoney("MEMBERS", 4), 0.001);

        // CLAIMS (1..4)
        assertEquals(4000.0, TeamManager.getPerkCostMoney("CLAIMS", 1), 0.001);
        assertEquals(10000.0, TeamManager.getPerkCostMoney("CLAIMS", 2), 0.001);
        assertEquals(20000.0, TeamManager.getPerkCostMoney("CLAIMS", 3), 0.001);
        assertEquals(40000.0, TeamManager.getPerkCostMoney("CLAIMS", 4), 0.001);
        assertEquals(-1.0, TeamManager.getPerkCostMoney("CLAIMS", 5), 0.001);

        // JOBS (1..3)
        assertEquals(10000.0, TeamManager.getPerkCostMoney("JOBS", 1), 0.001);
        assertEquals(25000.0, TeamManager.getPerkCostMoney("JOBS", 2), 0.001);
        assertEquals(50000.0, TeamManager.getPerkCostMoney("JOBS", 3), 0.001);
        assertEquals(-1.0, TeamManager.getPerkCostMoney("JOBS", 4), 0.001);

        // AH_TAX (1..2)
        assertEquals(8000.0, TeamManager.getPerkCostMoney("AH_TAX", 1), 0.001);
        assertEquals(20000.0, TeamManager.getPerkCostMoney("AH_TAX", 2), 0.001);
        assertEquals(-1.0, TeamManager.getPerkCostMoney("AH_TAX", 3), 0.001);

        // QUESTS (1..2)
        assertEquals(12000.0, TeamManager.getPerkCostMoney("QUESTS", 1), 0.001);
        assertEquals(30000.0, TeamManager.getPerkCostMoney("QUESTS", 2), 0.001);
        assertEquals(-1.0, TeamManager.getPerkCostMoney("QUESTS", 3), 0.001);
    }

    @Test
    @DisplayName("Verification des couts en XP pour chaque niveau de perk (mode Eco OFF)")
    void testPerkCostsXp() {
        // MEMBERS (1..3)
        assertEquals(25, TeamManager.getPerkCostXp("MEMBERS", 1));
        assertEquals(45, TeamManager.getPerkCostXp("MEMBERS", 2));
        assertEquals(70, TeamManager.getPerkCostXp("MEMBERS", 3));
        assertEquals(-1, TeamManager.getPerkCostXp("MEMBERS", 4));

        // CLAIMS (1..4)
        assertEquals(20, TeamManager.getPerkCostXp("CLAIMS", 1));
        assertEquals(35, TeamManager.getPerkCostXp("CLAIMS", 2));
        assertEquals(55, TeamManager.getPerkCostXp("CLAIMS", 3));
        assertEquals(80, TeamManager.getPerkCostXp("CLAIMS", 4));
        assertEquals(-1, TeamManager.getPerkCostXp("CLAIMS", 5));

        // JOBS (1..3)
        assertEquals(30, TeamManager.getPerkCostXp("JOBS", 1));
        assertEquals(50, TeamManager.getPerkCostXp("JOBS", 2));
        assertEquals(80, TeamManager.getPerkCostXp("JOBS", 3));
        assertEquals(-1, TeamManager.getPerkCostXp("JOBS", 4));

        // AH_TAX (1..2)
        assertEquals(30, TeamManager.getPerkCostXp("AH_TAX", 1));
        assertEquals(55, TeamManager.getPerkCostXp("AH_TAX", 2));
        assertEquals(-1, TeamManager.getPerkCostXp("AH_TAX", 3));

        // QUESTS (1..2)
        assertEquals(35, TeamManager.getPerkCostXp("QUESTS", 1));
        assertEquals(60, TeamManager.getPerkCostXp("QUESTS", 2));
        assertEquals(-1, TeamManager.getPerkCostXp("QUESTS", 3));
    }

    @Test
    @DisplayName("Progression des bonus in-game selon le niveau des perks")
    void testPerkEffectsProgression() {
        // Base
        assertEquals(5, team.getMaxMembers());
        assertEquals(4, team.getMaxClaims());
        assertEquals(1.0, team.getJobsXpMultiplier(), 0.001);
        assertEquals(0.0, team.getAhTaxReduction(), 0.001);
        assertEquals(1.0, team.getQuestPointsMultiplier(), 0.001);

        // Augmentation de MEMBERS (niveau 2 -> 5 + 2*3 = 11)
        team.setUpgradeLevel("MEMBERS", 2);
        assertEquals(11, team.getMaxMembers());

        // Augmentation de CLAIMS (niveau 3 -> 4 + 3*4 = 16)
        team.setUpgradeLevel("CLAIMS", 3);
        assertEquals(16, team.getMaxClaims());

        // Augmentation de JOBS (niveau 3 -> 1.0 + 3*0.05 = 1.15)
        team.setUpgradeLevel("JOBS", 3);
        assertEquals(1.15, team.getJobsXpMultiplier(), 0.001);

        // Augmentation de AH_TAX (niveau 2 -> 0.50 soit -50%)
        team.setUpgradeLevel("AH_TAX", 2);
        assertEquals(0.50, team.getAhTaxReduction(), 0.001);

        // Augmentation de QUESTS (niveau 2 -> 1.0 + 2*0.10 = 1.20)
        team.setUpgradeLevel("QUESTS", 2);
        assertEquals(1.20, team.getQuestPointsMultiplier(), 0.001);
    }

    @Test
    @DisplayName("Verification du blocage des membres au-dela de la limite max")
    void testMemberLimitEnforcement() {
        // Initial : leader present (1 membre), max = 5
        assertEquals(1, team.getMembers().size());
        assertEquals(5, team.getMaxMembers());

        // Ajouter 4 membres (total 5)
        for (int i = 0; i < 4; i++) {
            team.addMember(UUID.randomUUID());
        }
        assertEquals(5, team.getMembers().size());
        assertTrue(team.getMembers().size() >= team.getMaxMembers());

        // Ameliorer MEMBERS niveau 1 -> limite passe a 8
        team.setUpgradeLevel("MEMBERS", 1);
        assertEquals(8, team.getMaxMembers());
        assertFalse(team.getMembers().size() >= team.getMaxMembers());

        // Remplir jusqu'a 8
        for (int i = 0; i < 3; i++) {
            team.addMember(UUID.randomUUID());
        }
        assertEquals(8, team.getMembers().size());
        assertTrue(team.getMembers().size() >= team.getMaxMembers());
    }

    @Test
    @DisplayName("Verification du calcul effectif de la taxe HDV avec le perk AH_TAX")
    void testAhTaxCalculationWithPerk() {
        double baseTaxRate = 0.10; // 10%

        // Sans upgrade : réduction 0%
        double effectiveTax0 = baseTaxRate * (1.0 - team.getAhTaxReduction());
        assertEquals(0.10, effectiveTax0, 0.0001);

        // Upgrade lvl 1 : 25% de reduction sur la taxe -> taxe de 7.5%
        team.setUpgradeLevel("AH_TAX", 1);
        double effectiveTax1 = baseTaxRate * (1.0 - team.getAhTaxReduction());
        assertEquals(0.075, effectiveTax1, 0.0001);

        // Upgrade lvl 2 : 50% de reduction sur la taxe -> taxe de 5%
        team.setUpgradeLevel("AH_TAX", 2);
        double effectiveTax2 = baseTaxRate * (1.0 - team.getAhTaxReduction());
        assertEquals(0.050, effectiveTax2, 0.0001);
    }

    @Test
    @DisplayName("Verification du calcul effectif de l'XP de metier avec le perk JOBS")
    void testJobsXpCalculationWithPerk() {
        double baseXp = 100.0;

        // Sans upgrade : multiplicateur 1.0
        assertEquals(100.0, baseXp * team.getJobsXpMultiplier(), 0.001);

        // Upgrade JOBS niveau 1 (+5%) -> 105 XP
        team.setUpgradeLevel("JOBS", 1);
        assertEquals(105.0, baseXp * team.getJobsXpMultiplier(), 0.001);

        // Upgrade JOBS niveau 3 (+15%) -> 115 XP
        team.setUpgradeLevel("JOBS", 3);
        assertEquals(115.0, baseXp * team.getJobsXpMultiplier(), 0.001);
    }

    @Test
    @DisplayName("Verification du calcul des points de quete avec le perk QUESTS")
    void testQuestPointsCalculationWithPerk() {
        int basePoints = 500;

        // Sans upgrade : 500 points
        int awarded0 = (int) Math.round(basePoints * team.getQuestPointsMultiplier());
        assertEquals(500, awarded0);

        // Upgrade QUESTS niveau 1 (+10%) -> 550 points
        team.setUpgradeLevel("QUESTS", 1);
        int awarded1 = (int) Math.round(basePoints * team.getQuestPointsMultiplier());
        assertEquals(550, awarded1);

        // Upgrade QUESTS niveau 2 (+20%) -> 600 points
        team.setUpgradeLevel("QUESTS", 2);
        int awarded2 = (int) Math.round(basePoints * team.getQuestPointsMultiplier());
        assertEquals(600, awarded2);
    }
}
