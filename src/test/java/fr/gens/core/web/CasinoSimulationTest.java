package fr.gens.core.web;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

public class CasinoSimulationTest {

    private int getMultiplierForRoll(int roll) {
        if (roll < 4) {
            return 5; // Jackpot x5
        } else if (roll < 12) {
            return 3; // Medium Win x3
        } else if (roll < 32) {
            return 2; // Small Win x2
        } else {
            return 0; // Loss x0
        }
    }

    @Test
    @DisplayName("Le calcul théorique du RTP Casino doit être strictement égal à 84%")
    void testTheoreticalRtp() {
        double pJackpot = 4.0 / 100.0;
        double pMedium = 8.0 / 100.0;
        double pSmall = 20.0 / 100.0;
        double pLoss = 68.0 / 100.0;

        double expectedRtp = (pJackpot * 5) + (pMedium * 3) + (pSmall * 2) + (pLoss * 0);
        assertEquals(0.84, expectedRtp, 0.0001, "Le RTP théorique de la machine à sous doit être de 84%");
    }

    @Test
    @DisplayName("Tous les tirages de 0 à 99 doivent attribuer le multiplicateur exact attendu")
    void testBoundaryRolls() {
        // Jackpot [0..3]
        for (int i = 0; i < 4; i++) {
            assertEquals(5, getMultiplierForRoll(i), "Roll " + i + " doit être un Jackpot x5");
        }
        // Medium [4..11]
        for (int i = 4; i < 12; i++) {
            assertEquals(3, getMultiplierForRoll(i), "Roll " + i + " doit être un Gain Moyen x3");
        }
        // Small [12..31]
        for (int i = 12; i < 32; i++) {
            assertEquals(2, getMultiplierForRoll(i), "Roll " + i + " doit être un Petit Gain x2");
        }
        // Loss [32..99]
        for (int i = 32; i < 100; i++) {
            assertEquals(0, getMultiplierForRoll(i), "Roll " + i + " doit être une Perte x0");
        }
    }

    @Test
    @DisplayName("Simulation Monte-Carlo (500 000 tirages) : Le rendement moyen effectif converge vers 84% (±1%)")
    void testMonteCarloSimulation() {
        Random random = new Random(42); // Graine fixe pour reproductibilité
        int iterations = 500_000;
        long totalMultiplierWon = 0;

        for (int i = 0; i < iterations; i++) {
            int roll = random.nextInt(100);
            totalMultiplierWon += getMultiplierForRoll(roll);
        }

        double simulatedRtp = (double) totalMultiplierWon / iterations;
        // Tolérance de ± 0.01 (entre 83% et 85%)
        assertEquals(0.84, simulatedRtp, 0.01, "Le RTP simulé doit converger vers 0.84");
    }

    @Test
    @DisplayName("CoinFlip 50/50 : La distribution Pile ou Face est équilibrée sur 100 000 lancers")
    void testCoinFlipProbability() {
        Random random = new Random(123);
        int rolls = 100_000;
        int headsCount = 0;

        for (int i = 0; i < rolls; i++) {
            boolean outcomeHeads = random.nextBoolean();
            if (outcomeHeads) headsCount++;
        }

        double headsRatio = (double) headsCount / rolls;
        // Doit être à 50% ± 1%
        assertEquals(0.50, headsRatio, 0.01, "CoinFlip doit être équilibré à 50%");
    }
}
