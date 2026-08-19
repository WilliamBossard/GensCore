package fr.gens.core.modules.spawners;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.block.Hopper;
import org.bukkit.entity.Display;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.TextDisplay;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

public class SpawnerManager {

    private final SpawnerModule module;
    private final Random random = new Random();
    private final Map<Location, TextDisplay> holograms = new ConcurrentHashMap<>();

    public SpawnerManager(SpawnerModule module) {
        this.module = module;
    }

    public void loadTypes() {
        // Nothing to load from config anymore, we use EntityType dynamically
    }

    public boolean isValidType(String name) {
        try {
            EntityType.valueOf(name);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
    
    private int getBaseExp(EntityType type) {
        return switch (type) {
            case BLAZE, EVOKER, ELDER_GUARDIAN, GUARDIAN -> 10;
            case SLIME, MAGMA_CUBE -> 3; // roughly
            case CHICKEN, PIG, SHEEP, COW, MOOSHROOM -> 1;
            case WITHER -> 50;
            case ENDER_DRAGON -> 500;
            case IRON_GOLEM, SNOW_GOLEM, VILLAGER -> 0; // passive don't drop exp normally
            default -> 5; // standard hostile mob exp
        };
    }
    
    // Calculate max storage based on level
    public int getMaxStorageItems(int level) {
        return 500 * (int)Math.pow(2, level); // 500, 1000, 2000...
    }
    
    public int getMaxStorageExp(int level) {
        return 1000 * (int)Math.pow(2, level); // 1000, 2000, 4000...
    }
    
    public int getDelayTicks(int level) {
        // Default 600 ticks (30s)
        int ticks = 600 - (level * 50);
        return Math.max(ticks, 100); // minimum 5s
    }

    public void generateTick() {
        boolean hoppersEnabled = module.getPlugin().getConfig().getBoolean("spawners.hoppers", false);
        long now = System.currentTimeMillis();
        
        for (SpawnerData data : module.getActiveSpawners().values()) {
            if (data.isLootChest()) continue;
            
            Location loc = data.getLocation();
            
            boolean isLoaded = loc.getWorld().isChunkLoaded(loc.getBlockX() >> 4, loc.getBlockZ() >> 4);
            
            // Check if it's time to tick this spawner based on its speed level
            long delayMillis = getDelayTicks(data.getSpeedLevel()) * 50L; // Convert ticks to milliseconds
            if (now - data.getLastGenerateMillis() < delayMillis) {
                continue; // Not its turn yet
            }
            
            data.setLastGenerateMillis(now); // Reset timer
            
            EntityType type;
            try {
                type = EntityType.valueOf(data.getType());
            } catch (Exception e) {
                continue;
            }
            
            int expPerSpawn = getBaseExp(type);
            int generatedExp = 0;
            Map<String, Integer> generatedItems = new HashMap<>();
            
            for (int i = 0; i < data.getStackCount(); i++) {
                generatedExp += expPerSpawn;
                List<ItemStack> loot = getDropsForType(type, data.getLocation());
                for (ItemStack item : loot) {
                    if (item != null && item.getType() != Material.AIR && item.getAmount() > 0) {
                        String matStr = item.getType().name();
                        generatedItems.put(matStr, generatedItems.getOrDefault(matStr, 0) + item.getAmount());
                    }
                }
            }
            
            // Handle limits
            int currentExp = data.getStoredExp();
            int maxExp = getMaxStorageExp(data.getExpLevel());
            if (currentExp < maxExp) {
                int toAdd = Math.min(generatedExp, maxExp - currentExp);
                data.addExp(toAdd);
            }
            
            // Handle Hoppers if enabled (ONLY if chunk is loaded)
            if (hoppersEnabled && !generatedItems.isEmpty() && isLoaded) {
                Block below = loc.getBlock().getRelative(0, -1, 0);
                if (below.getType() == Material.HOPPER) {
                    Hopper hopper = (Hopper) below.getState();
                    for (Map.Entry<String, Integer> entry : generatedItems.entrySet()) {
                        Material mat = Material.getMaterial(entry.getKey());
                        if (mat != null) {
                            HashMap<Integer, ItemStack> left = hopper.getInventory().addItem(new ItemStack(mat, entry.getValue()));
                            if (!left.isEmpty()) {
                                for (ItemStack remaining : left.values()) {
                                    addToInternalWithLimit(data, entry.getKey(), remaining.getAmount());
                                }
                            }
                        }
                    }
                    generatedItems.clear(); // Handled
                }
            }
            
            // Add remaining to internal storage
            for (Map.Entry<String, Integer> entry : generatedItems.entrySet()) {
                addToInternalWithLimit(data, entry.getKey(), entry.getValue());
            }
            
            // Particles and Holograms (ONLY if loaded)
            if (isLoaded) {
                loc.getWorld().spawnParticle(Particle.FLAME, loc.clone().add(0.5, 0.5, 0.5), 3, 0.2, 0.2, 0.2, 0.05);
                updateHologram(data);
            }
        }
    }
    
    private void addDrop(List<ItemStack> drops, Material mat, int amount) {
        if (amount > 0) {
            drops.add(new ItemStack(mat, amount));
        }
    }

    private boolean chance(double percent) {
        return random.nextDouble() * 100.0 < percent;
    }

    private List<ItemStack> getDropsForType(EntityType type, Location loc) {
        List<ItemStack> drops = new ArrayList<>();
        
        switch (type) {
            case ZOMBIE -> {
                addDrop(drops, Material.ROTTEN_FLESH, random.nextInt(3)); // 0-2
                if (chance(2.5)) addDrop(drops, Material.IRON_INGOT, 1);
                if (chance(2.5)) addDrop(drops, Material.CARROT, 1);
                if (chance(2.5)) addDrop(drops, Material.POTATO, 1);
            }
            case SKELETON -> {
                addDrop(drops, Material.BONE, random.nextInt(3)); // 0-2
                addDrop(drops, Material.ARROW, random.nextInt(3)); // 0-2
                if (chance(8.5)) addDrop(drops, Material.BOW, 1);
            }
            case SPIDER, CAVE_SPIDER -> {
                addDrop(drops, Material.STRING, random.nextInt(3)); // 0-2
                if (chance(33.0)) addDrop(drops, Material.SPIDER_EYE, 1);
            }
            case CREEPER -> {
                addDrop(drops, Material.GUNPOWDER, random.nextInt(3)); // 0-2
            }
            case BLAZE -> {
                addDrop(drops, Material.BLAZE_ROD, random.nextInt(2)); // 0-1
            }
            case ENDERMAN -> {
                addDrop(drops, Material.ENDER_PEARL, random.nextInt(2)); // 0-1
            }
            case PIGLIN -> {
                if (chance(50.0)) addDrop(drops, Material.GOLD_NUGGET, 1);
                if (chance(10.0)) addDrop(drops, Material.GOLD_INGOT, 1);
            }
            case ZOMBIFIED_PIGLIN -> {
                addDrop(drops, Material.ROTTEN_FLESH, random.nextInt(2)); // 0-1
                addDrop(drops, Material.GOLD_NUGGET, random.nextInt(2)); // 0-1
                if (chance(2.5)) addDrop(drops, Material.GOLD_INGOT, 1);
                if (chance(8.5)) addDrop(drops, Material.GOLDEN_SWORD, 1);
            }
            case IRON_GOLEM -> {
                addDrop(drops, Material.IRON_INGOT, 3 + random.nextInt(3)); // 3-5
                addDrop(drops, Material.POPPY, random.nextInt(3)); // 0-2
            }
            case COW -> {
                addDrop(drops, Material.BEEF, 1 + random.nextInt(3)); // 1-3
                addDrop(drops, Material.LEATHER, random.nextInt(3)); // 0-2
            }
            case MOOSHROOM -> {
                addDrop(drops, Material.BEEF, 1 + random.nextInt(3)); // 1-3
                addDrop(drops, Material.LEATHER, random.nextInt(3)); // 0-2
                addDrop(drops, Material.RED_MUSHROOM, 1 + random.nextInt(2)); // 1-2
            }
            case PIG -> addDrop(drops, Material.PORKCHOP, 1 + random.nextInt(3)); // 1-3
            case SHEEP -> {
                addDrop(drops, Material.MUTTON, 1 + random.nextInt(2)); // 1-2
                addDrop(drops, Material.WHITE_WOOL, 1);
            }
            case CHICKEN -> {
                addDrop(drops, Material.CHICKEN, 1);
                addDrop(drops, Material.FEATHER, random.nextInt(3)); // 0-2
            }
            case VILLAGER -> addDrop(drops, Material.EMERALD, random.nextInt(2)); // 0-1
            case SLIME -> addDrop(drops, Material.SLIME_BALL, random.nextInt(3)); // 0-2
            case WITCH -> {
                if (chance(25.0)) addDrop(drops, Material.GLASS_BOTTLE, random.nextInt(3));
                if (chance(25.0)) addDrop(drops, Material.GLOWSTONE_DUST, random.nextInt(3));
                if (chance(25.0)) addDrop(drops, Material.GUNPOWDER, random.nextInt(3));
                if (chance(25.0)) addDrop(drops, Material.REDSTONE, random.nextInt(3));
                if (chance(25.0)) addDrop(drops, Material.SPIDER_EYE, random.nextInt(3));
                if (chance(25.0)) addDrop(drops, Material.SUGAR, random.nextInt(3));
                if (chance(25.0)) addDrop(drops, Material.STICK, random.nextInt(3));
            }
            case WITHER_SKELETON -> {
                addDrop(drops, Material.COAL, random.nextInt(2)); // 0-1
                addDrop(drops, Material.BONE, random.nextInt(3)); // 0-2
                if (chance(2.5)) addDrop(drops, Material.WITHER_SKELETON_SKULL, 1);
            }
            case GHAST -> {
                addDrop(drops, Material.GHAST_TEAR, random.nextInt(2)); // 0-1
                addDrop(drops, Material.GUNPOWDER, random.nextInt(3)); // 0-2
            }
            case MAGMA_CUBE -> {
                addDrop(drops, Material.MAGMA_CREAM, random.nextInt(2)); // 0-1
            }
            default -> {}
        }
        
        return drops;
    }
    
    private void addToInternalWithLimit(SpawnerData data, String material, int amount) {
        int maxItems = getMaxStorageItems(data.getStorageLevel());
        int currentTotal = data.getStoredItems().values().stream().mapToInt(Integer::intValue).sum();
        
        if (currentTotal < maxItems) {
            int toAdd = Math.min(amount, maxItems - currentTotal);
            data.addItem(material, toAdd);
        }
    }

    public void updateHologram(SpawnerData data) {
        if (!module.getPlugin().getConfig().getBoolean("spawners.holograms", true)) return;
        
        Location loc = data.getLocation();
        if (!loc.getWorld().isChunkLoaded(loc.getBlockX() >> 4, loc.getBlockZ() >> 4)) return;
        
        TextDisplay display = holograms.get(loc);
        if (display == null || !display.isValid()) {
            Location spawnLoc = loc.clone().add(0.5, 1.2, 0.5);
            display = (TextDisplay) loc.getWorld().spawnEntity(spawnLoc, EntityType.TEXT_DISPLAY);
            display.setBillboard(Display.Billboard.CENTER);
            display.setDefaultBackground(false);
            display.setBackgroundColor(org.bukkit.Color.fromARGB(0, 0, 0, 0));
            display.setPersistent(false); // Eviter les hologrammes fantômes après un redémarrage
            holograms.put(loc, display);
        }
        
        int totalItems = data.getStoredItems().values().stream().mapToInt(Integer::intValue).sum();
        int maxItems = getMaxStorageItems(data.getStorageLevel());
        int maxExp = getMaxStorageExp(data.getExpLevel());
        
        String text;
        if (!module.isEnabled()) {
            text = "<red><bold>Spawner Désactivé\n<gray>(Les productions sont suspendues)";
        } else if (data.isLootChest()) {
            text = "<yellow><bold>Coffre de Récupération\n" +
                   (totalItems == 0 && data.getStoredExp() == 0 ? "<red>Vide" : "<white>Stock: <yellow>" + totalItems + " items\n<green>Exp: <green>" + data.getStoredExp() + " XP");
        } else {
            text = "<gold><bold>" + data.getType() + " <gray>(x" + data.getStackCount() + ")\n" +
                   (totalItems >= maxItems ? "<red>Stock plein: " : "<white>Stock: ") + "<yellow>" + totalItems + "/" + maxItems + " items\n" +
                   (data.getStoredExp() >= maxExp ? "<red>Exp plein: " : "<green>Exp: ") + "<green>" + data.getStoredExp() + "/" + maxExp + " XP";
        }
        display.setText(text);
    }
    
    public void removeHologram(Location loc) {
        TextDisplay display = holograms.remove(loc);
        if (display != null && display.isValid()) {
            display.remove();
        }
    }
    
    public void clearAllHolograms() {
        for (TextDisplay display : holograms.values()) {
            if (display.isValid()) display.remove();
        }
        holograms.clear();
    }
}
