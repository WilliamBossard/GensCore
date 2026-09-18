package fr.gens.core.modules.quests;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class CraftCalculationTest {

    private int calculateShiftClickYield(int yieldPerCraft, int minIngredients, int spaceAvailable) {
        if (yieldPerCraft <= 0) yieldPerCraft = 1;
        if (minIngredients <= 0) return 0;

        int craftsPossibleBySpace = spaceAvailable / yieldPerCraft;
        int actualCrafts = Math.min(minIngredients, craftsPossibleBySpace);
        if (actualCrafts <= 0) return 0;

        return actualCrafts * yieldPerCraft;
    }

    @Test
    @DisplayName("Craft de torches en lot (Shift-Click) : 8 charbons + 8 bâtons avec inventaire libre = 32 torches")
    void testTorchesBulkCraft() {
        int yieldPerCraft = 4; // 1 charbon + 1 bâton = 4 torches
        int minIngredients = 8;
        int spaceAvailable = 64 * 5; // 5 slots libres

        int totalCrafted = calculateShiftClickYield(yieldPerCraft, minIngredients, spaceAvailable);
        assertEquals(32, totalCrafted, "8 crafts de torches doivent produire exactement 32 torches");
    }

    @Test
    @DisplayName("Craft borné par la place disponible dans l'inventaire")
    void testCraftBoundedByInventorySpace() {
        int yieldPerCraft = 4;
        int minIngredients = 10; // Pourrait faire 40 torches
        int spaceAvailable = 14; // Seulement 14 torches peuvent rentrer

        // 14 / 4 = 3 crafts possibles par manque de place -> 3 * 4 = 12 torches
        int totalCrafted = calculateShiftClickYield(yieldPerCraft, minIngredients, spaceAvailable);
        assertEquals(12, totalCrafted, "Le craft doit être borné à 12 torches par manque de place");
    }

    @Test
    @DisplayName("Inventaire plein (spaceAvailable = 0) : aucun craft ne doit être comptabilisé")
    void testFullInventoryCraft() {
        int yieldPerCraft = 4;
        int minIngredients = 10;
        int spaceAvailable = 0;

        int totalCrafted = calculateShiftClickYield(yieldPerCraft, minIngredients, spaceAvailable);
        assertEquals(0, totalCrafted, "Aucun item ne doit être comptabilisé si l'inventaire est plein");
    }

    @Test
    @DisplayName("Craft standard unitaire (rendement = 1) : 16 fers = 16 lingots")
    void testUnitYieldCraft() {
        int yieldPerCraft = 1;
        int minIngredients = 16;
        int spaceAvailable = 64;

        int totalCrafted = calculateShiftClickYield(yieldPerCraft, minIngredients, spaceAvailable);
        assertEquals(16, totalCrafted);
    }

    @Test
    @DisplayName("Matrice de craft vide ou ingrédients invalides (minIngredients = 0)")
    void testZeroIngredients() {
        int totalCrafted = calculateShiftClickYield(4, 0, 64);
        assertEquals(0, totalCrafted);
    }
}
