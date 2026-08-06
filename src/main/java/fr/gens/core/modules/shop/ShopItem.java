package fr.gens.core.modules.shop;

import org.bukkit.Material;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import fr.gens.core.CorePlugin;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ShopItem {
    private Material material;
    private double buyPrice; // Prix pour qu'un joueur ACHÈTE l'item
    private double sellPrice; // Prix pour qu'un joueur VENDE l'item
    private int stock; // Nombre d'items vendus par les joueurs au serveur
    private int targetStock; // Stock d'équilibre pour l'inflation
    
    // Pour les Grades / Permissions
    private boolean isCommand = false;
    private String commandToExecute = "";
    
    @JsonProperty("isEnabled")
    private boolean isEnabled = true;

    public ShopItem() {}

    public ShopItem(Material material, double buyPrice, double sellPrice) {
        this.material = material;
        this.buyPrice = buyPrice;
        this.sellPrice = sellPrice;
        this.stock = 1000;
        this.targetStock = 1000;
    }

    public Material getMaterial() { return material; }
    public void setMaterial(Material material) { this.material = material; }

    public double getBaseBuyPrice() { return buyPrice; }
    public void setBaseBuyPrice(double buyPrice) { this.buyPrice = buyPrice; }

    public double getBaseSellPrice() { return sellPrice; }
    public void setBaseSellPrice(double sellPrice) { this.sellPrice = sellPrice; }

    public int getStock() { return stock; }
    public void setStock(int stock) { this.stock = stock; }

    public int getTargetStock() { return targetStock; }
    public ShopItem setTargetStock(int targetStock) { this.targetStock = targetStock; return this; }

    @JsonProperty("isCommand")
    public boolean isCommand() { return isCommand; }
    
    @JsonProperty("isCommand")
    public ShopItem setCommand(boolean command) { isCommand = command; return this; }

    public boolean isEnabled() { return isEnabled; }
    public ShopItem setEnabled(boolean enabled) { isEnabled = enabled; return this; }

    public String getCommandToExecute() { return commandToExecute; }
    public ShopItem setCommandToExecute(String commandToExecute) { this.commandToExecute = commandToExecute; return this; }

    // Calcul de l'inflation basé sur l'offre et la demande
    // Plus le stock est bas par rapport au targetStock, plus les prix montent (rareté).
    // Plus le stock est haut, plus les prix baissent (abondance).
    public double getCurrentBuyPrice() {
        if (isCommand) return buyPrice;
        if (stock >= targetStock) return buyPrice;
        double ratio = (double) targetStock / Math.max(1, stock);
        double exponent = 0.5;
        if (CorePlugin.getInstance() != null) {
            exponent = CorePlugin.getInstance().getConfig().getDouble("shop.inflation_exponent", 0.5);
        }
        return buyPrice * Math.pow(ratio, exponent);
    }

    public double getCurrentSellPrice() {
        if (sellPrice <= 0) return 0;
        if (stock == 0) return sellPrice * 2;
        double ratio = (double) targetStock / stock;
        return sellPrice * Math.pow(ratio, 0.5);
    }
}
