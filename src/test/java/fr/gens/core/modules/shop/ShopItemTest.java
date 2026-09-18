package fr.gens.core.modules.shop;

import org.bukkit.Material;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

public class ShopItemTest {

    private ShopItem item;

    @BeforeEach
    void setUp() {
        ShopModule.GLOBAL_INFLATION_EXPONENT = 0.5;
        // Base: Buy = 100$, Sell = 30$ (30% margin), TargetStock = 1000
        item = new ShopItem(Material.DIAMOND, 100.0, 30.0);
        item.setStock(1000);
        item.setTargetStock(1000);
    }

    @Test
    @DisplayName("Le prix d'achat et de vente d'équilibre (stock == targetStock) doit être égal au prix de base")
    void testEquilibriumPrices() {
        item.setStock(1000);
        assertEquals(100.0, item.getCurrentBuyPrice(), 0.001);
        assertEquals(30.0, item.getCurrentSellPrice(), 0.001);
    }

    @Test
    @DisplayName("En cas de surabondance (stock > targetStock), le prix de vente doit baisser sans dépasser l'achat")
    void testAbundancePrices() {
        item.setStock(4000); // ratio = 1000 / 4000 = 0.25 -> sqrt(0.25) = 0.5
        assertEquals(100.0, item.getCurrentBuyPrice(), 0.001, "Le prix d'achat reste au prix de base en abondance");
        assertEquals(15.0, item.getCurrentSellPrice(), 0.001, "Le prix de vente est divisé par 2");
        assertTrue(item.getCurrentSellPrice() < item.getCurrentBuyPrice());
    }

    @Test
    @DisplayName("En cas de pénurie extrême (stock = 1), le garde-fou anti-arbitrage doit plafonner le prix de vente à 75% du prix d'achat")
    void testExtremeScarcityAntiArbitrage() {
        item.setStock(1); // ratio = 1000 / 1 = 1000 -> sqrt(1000) ~ 31.62
        double buyPrice = item.getCurrentBuyPrice();
        double sellPrice = item.getCurrentSellPrice();

        assertTrue(buyPrice > 100.0, "Le prix d'achat augmente sous l'effet de la rareté");
        assertTrue(sellPrice <= buyPrice * 0.75 + 0.001, "Le prix de vente ne doit jamais dépasser 75% du prix d'achat");
        assertTrue(sellPrice < buyPrice, "Arbitrage impossible : vente strictement inférieure à l'achat");
    }

    @ParameterizedTest
    @ValueSource(doubles = {0.05, 0.1, 0.2, 0.5, 0.8, 1.0})
    @DisplayName("Pour n'importe quel exposant d'inflation configuré, aucun arbitrage (vente > achat) ne doit être possible")
    void testNoArbitrageUnderVariousExponents(double exponent) {
        ShopModule.GLOBAL_INFLATION_EXPONENT = exponent;

        int[] testStocks = {0, 1, 5, 10, 50, 100, 500, 1000, 2000, 10000};
        for (int stock : testStocks) {
            item.setStock(stock);
            double buy = item.getCurrentBuyPrice();
            double sell = item.getCurrentSellPrice();

            assertTrue(sell <= buy * 0.75 + 0.001,
                    String.format("Échec anti-arbitrage pour stock=%d et exponent=%.2f : buy=%.2f, sell=%.2f",
                            stock, exponent, buy, sell));
        }
    }

    @Test
    @DisplayName("Un objet dont le prix de vente de base est 0 doit toujours avoir un prix de vente actuel de 0")
    void testUnsellableItem() {
        ShopItem unsellable = new ShopItem(Material.BARRIER, 500.0, 0.0);
        unsellable.setStock(10);
        assertEquals(0.0, unsellable.getCurrentSellPrice());
    }

    @Test
    @DisplayName("Un objet de type commande conserve un prix fixe sans inflation")
    void testCommandItemPricing() {
        ShopItem cmdItem = new ShopItem(Material.PAPER, 250.0, 0.0);
        cmdItem.setCommand(true);
        cmdItem.setStock(1);
        assertEquals(250.0, cmdItem.getCurrentBuyPrice(), "Les commandes ont un prix fixe");
    }
}
