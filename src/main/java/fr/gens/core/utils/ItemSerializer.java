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
                byte[] decoded = java.util.Base64.getMimeDecoder().decode(data);
                java.io.ByteArrayInputStream inputStream = new java.io.ByteArrayInputStream(decoded);
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

    public static String itemStackArrayToBase64(ItemStack[] items) {
        if (items == null) return null;
        try {
            java.io.ByteArrayOutputStream outputStream = new java.io.ByteArrayOutputStream();
            java.io.DataOutputStream dataOutput = new java.io.DataOutputStream(outputStream);
            dataOutput.writeInt(items.length);
            for (ItemStack item : items) {
                if (item != null) {
                    byte[] bytes = item.serializeAsBytes();
                    dataOutput.writeInt(bytes.length);
                    dataOutput.write(bytes);
                } else {
                    dataOutput.writeInt(0);
                }
            }
            dataOutput.close();
            return java.util.Base64.getEncoder().encodeToString(outputStream.toByteArray());
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @SuppressWarnings("deprecation")
    public static ItemStack[] itemStackArrayFromBase64(String data) {
        if (data == null || data.isEmpty()) return null;

        if (data.startsWith("rO0AB")) {
            try {
                java.io.ByteArrayInputStream inputStream = new java.io.ByteArrayInputStream(java.util.Base64.getDecoder().decode(data));
                org.bukkit.util.io.BukkitObjectInputStream dataInput = new org.bukkit.util.io.BukkitObjectInputStream(inputStream);
                int size = dataInput.readInt();
                ItemStack[] items = new ItemStack[size];
                for (int i = 0; i < size; i++) {
                    items[i] = (ItemStack) dataInput.readObject();
                }
                dataInput.close();
                return items;
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        try {
            java.io.ByteArrayInputStream inputStream = new java.io.ByteArrayInputStream(java.util.Base64.getDecoder().decode(data));
            java.io.DataInputStream dataInput = new java.io.DataInputStream(inputStream);
            int size = dataInput.readInt();
            ItemStack[] items = new ItemStack[size];
            for (int i = 0; i < size; i++) {
                int len = dataInput.readInt();
                if (len > 0) {
                    byte[] bytes = new byte[len];
                    dataInput.readFully(bytes);
                    items[i] = ItemStack.deserializeBytes(bytes);
                } else {
                    items[i] = null;
                }
            }
            dataInput.close();
            return items;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
