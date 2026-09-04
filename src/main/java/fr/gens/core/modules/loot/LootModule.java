package fr.gens.core.modules.loot;

import fr.gens.core.CorePlugin;
import fr.gens.core.modules.Module;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.loot.LootContext;
import org.bukkit.loot.LootTable;
import org.bukkit.loot.Lootable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


public class LootModule implements Module, Listener {

    private final CorePlugin plugin;
    private boolean enabled;
    private LootManager lootManager;

    private com.tcoded.folialib.wrapper.task.WrappedTask particleTask = null;
    private final Map<UUID, Long> breakConfirms = new ConcurrentHashMap<>();
    private final Map<UUID, Location> lastBrokenLocation = new ConcurrentHashMap<>();
    private final Map<Inventory, Location> openVirtualInventories = new ConcurrentHashMap<>();
    private final Map<Inventory, UUID> openVirtualInventoriesUUID = new ConcurrentHashMap<>();

    // Configuration values
    private boolean preventHoppers = true;
    private boolean preventBreak = false;
    private boolean particlesEnabled = true;
    private int breakConfirmTime = 3;
    private net.kyori.adventure.text.Component breakConfirmMsg = fr.gens.core.utils.PlaceholderUtils.parseToComponent("<yellow>Casse-le encore une fois dans les 3 secondes pour confirmer !");
    private net.kyori.adventure.text.Component chestBrokenMsg = fr.gens.core.utils.PlaceholderUtils.parseToComponent("<green>Coffre Lootr retiré !");
    private net.kyori.adventure.text.Component cannotBreakMsg = fr.gens.core.utils.PlaceholderUtils.parseToComponent("<red>Tu ne peux pas casser ce coffre !");
    private net.kyori.adventure.text.Component inventoryTitle = fr.gens.core.utils.PlaceholderUtils.parseToComponent("<dark_gray>[<gold><dark_gray>] <yellow>Coffre à Butin");

    public LootModule(CorePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "lootr";
    }

    @Override
    public String getDescription() {
        return "Système de coffres instanciés pour chaque joueur";
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void initDatabase(fr.gens.core.utils.DatabaseManager dbManager) {
        dbManager.executeStatement("CREATE TABLE IF NOT EXISTS genscore_pending_rewards (id INTEGER PRIMARY KEY AUTOINCREMENT, uuid VARCHAR(36) NOT NULL, amount DOUBLE, command TEXT, message TEXT, item_data TEXT);");
        dbManager.executeStatement("CREATE INDEX IF NOT EXISTS idx_pending_rewards_uuid ON genscore_pending_rewards(uuid);");
    }

    @Override
    public void enable() {
        this.enabled = true;
        this.lootManager = new LootManager(plugin);
        
        loadConfig();

        Bukkit.getPluginManager().registerEvents(this, plugin);

        if (particlesEnabled) {
            startParticleTask();
        }

        plugin.getLangManager().sendConsoleMessage("lootmodule.log_1");
    }

    public void loadConfig() {
        FileConfiguration config = plugin.getConfig();
        if (!config.contains("lootr.prevent-hopper")) {
            config.set("lootr.prevent-hopper", true);
            config.set("lootr.prevent-break", false);
            config.set("lootr.particles-enabled", true);
            config.set("lootr.break-confirm-time", 3);
            config.set("lootr.messages.break-confirm", "<yellow>Casse-le encore une fois dans les 3 secondes pour confirmer !");
            config.set("lootr.messages.chest-broken", "<green>Coffre Lootr retiré !");
            config.set("lootr.messages.cannot-break", "<red>Tu ne peux pas casser ce coffre !");
            config.set("lootr.inventory.title", "<dark_gray>[<gold><dark_gray>] <yellow>Coffre à Butin");
            plugin.getConfigManager().saveConfig("modules/lootr.yml");
        }
        
        preventHoppers = config.getBoolean("lootr.prevent-hopper", true);
        preventBreak = config.getBoolean("lootr.prevent-break", false);
        particlesEnabled = config.getBoolean("lootr.particles-enabled", true);
        breakConfirmTime = config.getInt("lootr.break-confirm-time", 3);
        breakConfirmMsg = fr.gens.core.utils.PlaceholderUtils.parseToComponent(config.getString("lootr.messages.break-confirm", "&eCasse-le encore une fois dans les 3 secondes pour confirmer !"));
        chestBrokenMsg = fr.gens.core.utils.PlaceholderUtils.parseToComponent(config.getString("lootr.messages.chest-broken", "&aCoffre Lootr retiré !"));
        cannotBreakMsg = fr.gens.core.utils.PlaceholderUtils.parseToComponent(config.getString("lootr.messages.cannot-break", "&cTu ne peux pas casser ce coffre !"));
        inventoryTitle = fr.gens.core.utils.PlaceholderUtils.parseToComponent(config.getString("lootr.inventory.title", "&8[&6&8] &eCoffre à Butin"));
    }

    @Override
    public void disable() {
        org.bukkit.event.HandlerList.unregisterAll(this);
        this.enabled = false;
        if (particleTask != null) {
            particleTask.cancel();
            particleTask = null;
        }
        
        // Force close all virtual inventories to save them
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p == null) continue;
            if (openVirtualInventories.containsKey(p.getOpenInventory().getTopInventory())) {
                p.closeInventory();
            }
        }
        
        if (lootManager != null) {
            lootManager.saveChests();
        }
        plugin.getLangManager().sendConsoleMessage("lootmodule.log_2");
    }

    private void startParticleTask() {
        particleTask = plugin.getFoliaLib().getScheduler().runTimer(() -> {
            for (Player p : Bukkit.getOnlinePlayers()) {
            if (p == null) continue;
                Location pLoc = p != null ? p.getLocation() : null;
            if (pLoc == null) return;
                for (Map.Entry<String, LootManager.LootChestData> entry : lootManager.getChestsCache().entrySet()) {
                    Location chestLoc = lootManager.stringToLoc(entry.getKey());
                    if (chestLoc != null && chestLoc.getWorld().equals(pLoc.getWorld())) {
                        if (chestLoc.distanceSquared(pLoc) < 400) { // 20 blocks radius
                            if (!lootManager.hasPlayerLooted(p.getUniqueId(), chestLoc)) {
                                p.spawnParticle(Particle.HAPPY_VILLAGER, chestLoc.clone().add(0.5, 0.5, 0.5), 2, 0.4, 0.4, 0.4, 0);
                            }
                        }
                    }
                }
            }
        }, 20L, 20L); // Every second
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        
        Block block = event.getClickedBlock();
        if (block == null) return;
        
        BlockState state = block.getState();
        Location loc = block.getLocation();
        Player p = event.getPlayer();

        boolean isLootrChest = lootManager.isLootChest(loc);
        
        // Convert naturally generated chest to Lootr chest
        if (!isLootrChest && state instanceof Lootable && state instanceof Container) {
            Lootable lootable = (Lootable) state;
            if (lootable.getLootTable() != null) {
                LootTable table = lootable.getLootTable();
                String tableName = table.getKey().toString();
                long seed = lootable.getSeed();
                int size = ((Container) state).getInventory().getSize();
                
                lootManager.addLootChest(loc, tableName, seed, size);
                
                // Remove vanilla loot table so it doesn't generate normally
                lootable.setLootTable(null);
                state.update();
                
                isLootrChest = true;
            }
        }

        if (isLootrChest) {
            event.setCancelled(true);
            openLootrInventory(p, loc);
        }
    }

    private void openLootrInventory(Player p, Location loc) {
        LootManager.LootChestData data = lootManager.getLootChestData(loc);
        if (data == null) return;

        Inventory inv = Bukkit.createInventory(null, data.getSize(), inventoryTitle);
        boolean hasLootedBefore = lootManager.hasPlayerLooted(p.getUniqueId(), loc);

        if (hasLootedBefore) {
            // Load their saved inventory
            ItemStack[] savedItems = lootManager.getPlayerLoot(p.getUniqueId(), loc);
            if (savedItems != null) {
                inv.setContents(savedItems);
            }
        } else {
            // Generate new inventory for them
            NamespacedKey key = NamespacedKey.fromString(data.getLootTable());
            if (key != null) {
                LootTable table = Bukkit.getLootTable(key);
                if (table != null) {
                    LootContext context = new LootContext.Builder(loc).lootedEntity(p).build();
                    // We must fill a temporary inventory to not affect vanilla mechanics if needed, or fill directly
                    table.fillInventory(inv, new Random(), context);
                }
            }
            // Add to database marking that they looted it (by saving the initial generated state)
            lootManager.savePlayerLoot(p.getUniqueId(), loc, inv.getContents());
        }

        openVirtualInventories.put(inv, loc);
        openVirtualInventoriesUUID.put(inv, p.getUniqueId());
        p.openInventory(inv);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        Inventory inv = event.getInventory();
        if (openVirtualInventories.containsKey(inv)) {
            Location loc = openVirtualInventories.get(inv);
            UUID uuid = openVirtualInventoriesUUID.get(inv);
            
            // Save the current state of their instance
            lootManager.savePlayerLoot(uuid, loc, inv.getContents());
            
            openVirtualInventories.remove(inv);
            openVirtualInventoriesUUID.remove(inv);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        Location loc = block.getLocation();

        if (lootManager.isLootChest(loc)) {
            Player p = event.getPlayer();
            
            if (preventBreak) {
                event.setCancelled(true);
                p.sendMessage(cannotBreakMsg);
                return;
            }

            UUID uuid = p.getUniqueId();
            long now = System.currentTimeMillis();

            if (breakConfirms.containsKey(uuid) && lastBrokenLocation.containsKey(uuid)) {
                if (lastBrokenLocation.get(uuid).equals(loc)) {
                    long lastTime = breakConfirms.get(uuid);
                    if (now - lastTime < breakConfirmTime * 1000L) {
                        // Confirm break
                        breakConfirms.remove(uuid);
                        lastBrokenLocation.remove(uuid);
                        lootManager.removeLootChest(loc);
                        p.sendMessage(chestBrokenMsg);
                        return; // Let it break normally
                    }
                }
            }

            // Require confirmation
            event.setCancelled(true);
            breakConfirms.put(uuid, now);
            lastBrokenLocation.put(uuid, loc);
            p.sendMessage(breakConfirmMsg);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        event.blockList().removeIf(block -> lootManager.isLootChest(block.getLocation()));
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        for (Block block : event.getBlocks()) {
            if (lootManager.isLootChest(block.getLocation())) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        for (Block block : event.getBlocks()) {
            if (lootManager.isLootChest(block.getLocation())) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryMoveItem(InventoryMoveItemEvent event) {
        if (!preventHoppers) return;
        
        Location sourceLoc = null;
        if (event.getSource().getLocation() != null) {
            sourceLoc = event.getSource().getLocation();
        } else if (event.getSource().getHolder() instanceof org.bukkit.block.BlockState) {
            sourceLoc = ((org.bukkit.block.BlockState) event.getSource().getHolder()).getLocation();
        }

        if (sourceLoc != null && lootManager.isLootChest(sourceLoc)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerQuit(org.bukkit.event.player.PlayerQuitEvent event) {
        if (!enabled) return;
        lootManager.removePlayerCache(event.getPlayer().getUniqueId());
    }
}






