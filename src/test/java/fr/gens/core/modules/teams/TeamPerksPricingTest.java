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
}
