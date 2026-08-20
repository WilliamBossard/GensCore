package fr.gens.core.modules.spawners;

import fr.gens.core.CorePlugin;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;


public class SpawnerListener implements Listener {

    private final CorePlugin plugin;
    private final SpawnerModule module;
    private final Map<UUID, Long> vanillaBreakConfirm = new HashMap<>();
    private final NamespacedKey typeKey;
    private final NamespacedKey stackKey;
    private final NamespacedKey expKey;
    private final NamespacedKey speedKey;
    private final NamespacedKey storageKey;

    public SpawnerListener(CorePlugin plugin, SpawnerModule module) {
        this.plugin = plugin;
        this.module = module;
        this.typeKey = new NamespacedKey(plugin, "spawner_type");
        this.stackKey = new NamespacedKey(plugin, "spawner_stack");
        this.expKey = new NamespacedKey(plugin, "spawner_exp");
        this.speedKey = new NamespacedKey(plugin, "spawner_speed");
        this.storageKey = new NamespacedKey(plugin, "spawner_storage");
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (!module.isEnabled()) return;
        
        Block block = event.getBlockPlaced();
        ItemStack item = event.getItemInHand();
        
        if (block.getType() != Material.SPAWNER) return;
        if (!item.hasItemMeta()) return;
        
        ItemMeta meta = item.getItemMeta();
        if (meta.getPersistentDataContainer().has(typeKey, PersistentDataType.STRING)) {
            String type = meta.getPersistentDataContainer().get(typeKey, PersistentDataType.STRING);
            int stack = meta.getPersistentDataContainer().getOrDefault(stackKey, PersistentDataType.INTEGER, 1);
            int expLvl = meta.getPersistentDataContainer().getOrDefault(expKey, PersistentDataType.INTEGER, 0);
            int speedLvl = meta.getPersistentDataContainer().getOrDefault(speedKey, PersistentDataType.INTEGER, 0);
            int storageLvl = meta.getPersistentDataContainer().getOrDefault(storageKey, PersistentDataType.INTEGER, 0);
            
            if (!module.getSpawnerManager().isValidType(type)) {
                event.getPlayer().sendMessage("<red>Type de spawner invalide.");
                return;
            }
            
            SpawnerData data = new SpawnerData(block.getLocation(), type, stack);
            data.setExpLevel(expLvl);
            data.setSpeedLevel(speedLvl);
            data.setStorageLevel(storageLvl);
            data.setLastInteractedPlayer(event.getPlayer().getName());
            module.addSpawner(data);
            
            event.getPlayer().sendMessage("<green>Spawner GensCore posÃƒÆ’Ã‚Â© !");
            
            // Set vanilla spawner type just for visuals
            try {
                CreatureSpawner spawner = (CreatureSpawner) block.getState();
                spawner.setSpawnedType(EntityType.valueOf(type));
                spawner.update();
            } catch (Exception ignored) {}
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        Player player = event.getPlayer();
        
        // Handling Loot Chest break protection
        if (block.getType() == Material.CHEST) {
            SpawnerData data = module.getSpawnerAt(block.getLocation());
            if (data != null && data.isLootChest()) {
                event.setCancelled(true);
                plugin.getLangManager().sendMessage(player, "spawnerlistener.msg_1");
                return;
            }
        }
        
        if (block.getType() != Material.SPAWNER) return;
        
        Location loc = block.getLocation();
        SpawnerData data = module.getSpawnerAt(loc);
        
        if (data != null) {
            if (!module.isEnabled()) {
                event.setCancelled(true);
                plugin.getLangManager().sendMessage(player, "spawnerlistener.msg_2");
                return;
            }
            
            event.setExpToDrop(0);
            
            // Drop spawner item
            ItemStack spawnerItem = SpawnerCommand.createSpawnerItem(
                plugin, data.getType(), data.getStackCount(),
                data.getExpLevel(), data.getSpeedLevel(), data.getStorageLevel()
            );
            block.getWorld().dropItemNaturally(loc, spawnerItem);
            
            // Drop stored exp instantly as orbs
            if (data.getStoredExp() > 0) {
                org.bukkit.entity.ExperienceOrb orb = (org.bukkit.entity.ExperienceOrb) block.getWorld().spawnEntity(loc, EntityType.EXPERIENCE_ORB);
                orb.setExperience(data.getStoredExp());
                data.setStoredExp(0);
            }
            
            // Si le spawner contient des items, on le transforme en coffre !
            if (!data.getStoredItems().isEmpty()) {
                block.setType(Material.CHEST);
                data.setLootChest(true);
                Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> module.saveSpawnerToDB(data));
                module.getSpawnerManager().updateHologram(data);
                plugin.getLangManager().sendMessage(player, "spawnerlistener.msg_3");
                event.setCancelled(true); // Annuler la casse car on a remplacÃƒÆ’Ã‚Â© le block par un coffre
            } else {
                module.removeSpawner(loc);
                plugin.getLangManager().sendMessage(player, "spawnerlistener.msg_4");
            }
        } else {
            // Vanilla spawner conversion
            CreatureSpawner spawner = (CreatureSpawner) block.getState();
            String type = spawner.getSpawnedType().name();
            
            if (module.getSpawnerManager().isValidType(type)) {
                
                if (!module.isEnabled()) {
                    UUID uuid = player.getUniqueId();
                    long lastAttempt = vanillaBreakConfirm.getOrDefault(uuid, 0L);
                    if (System.currentTimeMillis() - lastAttempt > 3000) {
                        event.setCancelled(true);
                        vanillaBreakConfirm.put(uuid, System.currentTimeMillis());
                        plugin.getLangManager().sendMessage(player, "spawnerlistener.msg_5");
                        return;
                    } else {
                        vanillaBreakConfirm.remove(uuid);
                        return; // Let vanilla break occur
                    }
                }
                
                // Silk Touch Check
                if (plugin.getConfigManager().getConfig("modules/spawners.yml").getBoolean("spawners.vanilla-require-silktouch", true)) {
                    ItemStack hand = player.getInventory().getItemInMainHand();
                    if (hand == null || !hand.containsEnchantment(Enchantment.SILK_TOUCH)) {
                        event.setCancelled(true);
                        plugin.getLangManager().sendMessage(player, "spawnerlistener.msg_6");
                        return;
                    }
                }
                
                ItemStack customSpawner = SpawnerCommand.createSpawnerItem(plugin, type, 1);
                block.getWorld().dropItemNaturally(loc, customSpawner);
                event.setExpToDrop(0); // Prevent vanilla exp drop
                plugin.getLangManager().sendMessage(player, "spawnerlistener.msg_7");
            }
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getHand() != org.bukkit.inventory.EquipmentSlot.HAND) return;
        
        Block block = event.getClickedBlock();
        if (block == null) return;
        
        if (block.getType() != Material.SPAWNER && block.getType() != Material.CHEST) return;
        
        SpawnerData data = module.getSpawnerAt(block.getLocation());
        if (data == null) return;
        
        event.setCancelled(true);
        Player player = event.getPlayer();
        
        if (data.isLootChest()) {
            data.setLastInteractedPlayer(player.getName());
            fr.gens.core.modules.spawners.SpawnerLootGui.openGui(player, data, module);
            return;
        }
        
        ItemStack item = event.getItem();
        
        int maxStack = plugin.getConfigManager().getConfig("modules/spawners.yml").getInt("spawners.max-stack", 100000);
        
        // Stacking logic
        if (item != null && item.getType() == Material.SPAWNER && item.hasItemMeta()) {
            if (!module.isEnabled()) {
                plugin.getLangManager().sendMessage(player, "spawnerlistener.msg_8");
                return;
            }
            ItemMeta meta = item.getItemMeta();
            if (meta.getPersistentDataContainer().has(typeKey, PersistentDataType.STRING)) {
                String type = meta.getPersistentDataContainer().get(typeKey, PersistentDataType.STRING);
                if (type.equals(data.getType())) {
                    if (data.getStackCount() < maxStack) {
                        int itemInternalStack = meta.getPersistentDataContainer().getOrDefault(stackKey, PersistentDataType.INTEGER, 1);
                        int physicalAmount = item.getAmount();
                        int spaceLeft = maxStack - data.getStackCount();
                        
                        if (spaceLeft < itemInternalStack) {
                            player.sendMessage("<red>Pas assez de place dans ce spawner pour ajouter cette pile de " + itemInternalStack + " spawner(s). (Espace libre: " + spaceLeft + ")");
                            return;
                        }
                        
                        int physicalItemsToConsume = Math.min(physicalAmount, spaceLeft / itemInternalStack);
                        
                        // S'il ne sneak pas, on ajoute juste 1 item physique. S'il sneak, on ajoute tout ce qu'on peut.
                        if (!player.isSneaking()) {
                            physicalItemsToConsume = 1;
                        }
                        
                        int totalAdded = physicalItemsToConsume * itemInternalStack;
                        data.setStackCount(data.getStackCount() + totalAdded);
                        item.setAmount(item.getAmount() - physicalItemsToConsume);
                        
                        // Fusionner les niveaux d'amÃƒÆ’Ã‚Â©liorations (prendre le plus haut)
                        int expLvl = meta.getPersistentDataContainer().getOrDefault(expKey, PersistentDataType.INTEGER, 0);
                        int speedLvl = meta.getPersistentDataContainer().getOrDefault(speedKey, PersistentDataType.INTEGER, 0);
                        int storageLvl = meta.getPersistentDataContainer().getOrDefault(storageKey, PersistentDataType.INTEGER, 0);
                        
                        if (expLvl > data.getExpLevel()) data.setExpLevel(expLvl);
                        if (speedLvl > data.getSpeedLevel()) data.setSpeedLevel(speedLvl);
                        if (storageLvl > data.getStorageLevel()) data.setStorageLevel(storageLvl);
                        
                        data.setLastInteractedPlayer(player.getName());
                        
                        module.getSpawnerManager().updateHologram(data);
                        player.sendMessage("<green>Vous avez ajoutÃƒÆ’Ã‚Â© " + totalAdded + " spawner(s). (Total: " + data.getStackCount() + "/" + maxStack + ")");
                        return;
                    } else {
                        plugin.getLangManager().sendMessage(player, "spawnerlistener.msg_9");
                        return;
                    }
                } else {
                    plugin.getLangManager().sendMessage(player, "spawnerlistener.msg_10");
                    return;
                }
            }
        }
        
        // Open GUI logic
        data.setLastInteractedPlayer(player.getName());
        SpawnerGui.openGui(player, data);
    }

    @EventHandler(ignoreCancelled = true)
    public void onSpawnerSpawn(org.bukkit.event.entity.SpawnerSpawnEvent event) {
        if (module.getSpawnerAt(event.getSpawner().getLocation()) != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onChunkLoad(org.bukkit.event.world.ChunkLoadEvent event) {
        // Check if there are any spawners in this chunk
        for (SpawnerData data : module.getActiveSpawners().values()) {
            Location loc = data.getLocation();
            if (loc.getWorld().equals(event.getWorld()) && 
                (loc.getBlockX() >> 4) == event.getChunk().getX() && 
                (loc.getBlockZ() >> 4) == event.getChunk().getZ()) {
                module.getSpawnerManager().updateHologram(data);
            }
        }
    }
}

