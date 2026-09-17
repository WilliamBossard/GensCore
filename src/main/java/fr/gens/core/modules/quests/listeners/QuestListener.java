package fr.gens.core.modules.quests.listeners;

import fr.gens.core.modules.quests.QuestModule;
import fr.gens.core.modules.quests.QuestType;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityBreedEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerJoinEvent;

import org.bukkit.event.player.PlayerShearEntityEvent;
import org.bukkit.event.inventory.FurnaceExtractEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.ItemStack;


public class QuestListener implements Listener {

    private final QuestModule questModule;

    public QuestListener(QuestModule questModule) {
        this.questModule = questModule;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        // Load data from DB asynchronously
        questModule.loadPlayerData(event.getPlayer().getUniqueId(), event.getPlayer().getName());
        questModule.checkPendingRewards(event.getPlayer());
    }

    @EventHandler
    public void onQuit(org.bukkit.event.player.PlayerQuitEvent event) {
        questModule.unloadPlayerData(event.getPlayer().getUniqueId());
    }

    @EventHandler(ignoreCancelled = true, priority = org.bukkit.event.EventPriority.HIGH)
    public void onBlockBreak(BlockBreakEvent event) {
        questModule.handleQuestProgress(event.getPlayer(), QuestType.BREAK, event.getBlock().getType().name(), 1);
    }

    @EventHandler(ignoreCancelled = true, priority = org.bukkit.event.EventPriority.HIGH)
    public void onBlockPlace(BlockPlaceEvent event) {
        questModule.handleQuestProgress(event.getPlayer(), QuestType.PLACE, event.getBlock().getType().name(), 1);
    }

    @EventHandler(ignoreCancelled = true, priority = org.bukkit.event.EventPriority.HIGH)
    public void onEntityDeath(EntityDeathEvent event) {
        if (event.getEntity().getKiller() != null) {
            questModule.handleQuestProgress(event.getEntity().getKiller(), QuestType.KILL, event.getEntityType().name(), 1);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = org.bukkit.event.EventPriority.HIGH)
    public void onCraft(CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        if (event.getRecipe() == null || event.getRecipe().getResult() == null) return;

        Player p = (Player) event.getWhoClicked();
        ItemStack resultItem = event.getRecipe().getResult();
        if (resultItem.getType().isAir()) return;

        int yieldPerCraft = resultItem.getAmount();
        if (yieldPerCraft <= 0) yieldPerCraft = 1;

        int totalCrafted = 0;

        if (event.isShiftClick()) {
            CraftingInventory inv = event.getInventory();
            ItemStack[] matrix = inv.getMatrix();
            int minIngredients = Integer.MAX_VALUE;
            for (ItemStack item : matrix) {
                if (item != null && !item.getType().isAir()) {
                    minIngredients = Math.min(minIngredients, item.getAmount());
                }
            }
            if (minIngredients == Integer.MAX_VALUE || minIngredients <= 0) return;

            // Space in player storage inventory
            int spaceAvailable = 0;
            int maxStack = resultItem.getMaxStackSize();
            for (ItemStack is : p.getInventory().getStorageContents()) {
                if (is == null || is.getType().isAir()) {
                    spaceAvailable += maxStack;
                } else if (is.isSimilar(resultItem)) {
                    spaceAvailable += Math.max(0, maxStack - is.getAmount());
                }
            }

            int craftsPossibleBySpace = spaceAvailable / yieldPerCraft;
            int actualCrafts = Math.min(minIngredients, craftsPossibleBySpace);
            if (actualCrafts <= 0) return;

            totalCrafted = actualCrafts * yieldPerCraft;
        } else {
            // Normal click: 1 craft operation occurs if cursor can hold the result
            ItemStack cursor = event.getCursor();
            if (cursor == null || cursor.getType().isAir()) {
                totalCrafted = yieldPerCraft;
            } else if (cursor.isSimilar(resultItem) && cursor.getAmount() + yieldPerCraft <= cursor.getMaxStackSize()) {
                totalCrafted = yieldPerCraft;
            } else {
                return;
            }
        }

        if (totalCrafted > 0) {
            questModule.handleQuestProgress(p, QuestType.CRAFT, resultItem.getType().name(), totalCrafted);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = org.bukkit.event.EventPriority.HIGH)
    public void onFish(PlayerFishEvent event) {
        if (event.getState() == PlayerFishEvent.State.CAUGHT_FISH && event.getCaught() instanceof org.bukkit.entity.Item) {
            org.bukkit.entity.Item item = (org.bukkit.entity.Item) event.getCaught();
            questModule.handleQuestProgress(event.getPlayer(), QuestType.FISH, item.getItemStack().getType().name(), 1);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = org.bukkit.event.EventPriority.HIGH)
    public void onShear(PlayerShearEntityEvent event) {
        questModule.handleQuestProgress(event.getPlayer(), QuestType.SHEAR, event.getEntity().getType().name(), 1);
    }

    @EventHandler(ignoreCancelled = true, priority = org.bukkit.event.EventPriority.HIGH)
    public void onFurnaceExtract(FurnaceExtractEvent event) {
        questModule.handleQuestProgress(event.getPlayer(), QuestType.COOK, event.getItemType().name(), event.getItemAmount());
    }

    @EventHandler(ignoreCancelled = true, priority = org.bukkit.event.EventPriority.HIGH)
    public void onConsume(PlayerItemConsumeEvent event) {
        questModule.handleQuestProgress(event.getPlayer(), QuestType.CONSUME, event.getItem().getType().name(), 1);
    }

    @EventHandler(ignoreCancelled = true, priority = org.bukkit.event.EventPriority.HIGH)
    public void onBreed(EntityBreedEvent event) {
        if (event.getBreeder() instanceof Player) {
            questModule.handleQuestProgress((Player) event.getBreeder(), QuestType.BREED, event.getEntity().getType().name(), 1);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = org.bukkit.event.EventPriority.HIGH)
    public void onPickup(org.bukkit.event.entity.EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            questModule.handleQuestProgress(player, QuestType.PICKUP, event.getItem().getItemStack().getType().name(), event.getItem().getItemStack().getAmount());
        }
    }
}



