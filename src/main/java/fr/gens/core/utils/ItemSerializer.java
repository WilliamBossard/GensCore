package fr.gens.core.utils;

import org.bukkit.inventory.ItemStack;

public class ItemSerializer {

    public static String toBase64(ItemStack item) {
        if (item == null) return null;
        return java.util.Base64.getEncoder().encodeToString(item.serializeAsBytes());
    }

    @SuppressWarnings("deprecation")
    public static ItemStack fromBase64(String data) {
        if (data == null || data.isEmpty()) return null;
        
        // Si les données proviennent de l'ancien format (Base64Coder + BukkitObjectOutputStream)
        if (data.startsWith("rO0AB") || data.contains("\n") || data.contains("\r")) {
            try {
                java.io.ByteArrayInputStream inputStream = new java.io.ByteArrayInputStream(org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder.decodeLines(data));
                org.bukkit.util.io.BukkitObjectInputStream dataInput = new org.bukkit.util.io.BukkitObjectInputStream(inputStream);
                ItemStack item = (ItemStack) dataInput.readObject();
                dataInput.close();
                return item;
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }
        
        // Nouveau format natif Paper
        try {
            return ItemStack.deserializeBytes(java.util.Base64.getDecoder().decode(data));
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
