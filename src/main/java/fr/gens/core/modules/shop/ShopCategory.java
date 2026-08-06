package fr.gens.core.modules.shop;

import org.bukkit.Material;
import java.util.ArrayList;
import java.util.List;

public class ShopCategory {
    private String id;
    private String displayName;
    private Material icon;
    private List<ShopItem> items;

    public ShopCategory() {
        this.items = new ArrayList<>();
    }

    public ShopCategory(String id, String displayName, Material icon) {
        this.id = id;
        this.displayName = displayName;
        this.icon = icon;
        this.items = new ArrayList<>();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public Material getIcon() { return icon; }
    public void setIcon(Material icon) { this.icon = icon; }

    public List<ShopItem> getItems() { return items; }
    public void setItems(List<ShopItem> items) { this.items = items; }

    public void addItem(ShopItem item) {
        this.items.add(item);
    }

    public ShopItem getItem(Material material) {
        for (ShopItem item : items) {
            if (item.getMaterial() == material) return item;
        }
        return null;
    }

    public void removeItem(Material material) {
        items.removeIf(item -> item.getMaterial() == material);
    }
}
