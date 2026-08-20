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
    public void onBlockBreak(BlockBreakEvent event) {
        questModule.handleQuestProgress(event.getPlayer(), QuestType.BREAK, event.getBlock().getType().name(), 1);
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        questModule.handleQuestProgress(event.getPlayer(), QuestType.PLACE, event.getBlock().getType().name(), 1);
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (event.getEntity().getKiller() != null) {
            questModule.handleQuestProgress(event.getEntity().getKiller(), QuestType.KILL, event.getEntityType().name(), 1);
        }
    }

    @EventHandler
    public void onCraft(CraftItemEvent event) {
        if (event.getWhoClicked() instanceof Player) {
            Player p = (Player) event.getWhoClicked();
            Material result = event.getRecipe().getResult().getType();
            // Approximative calculation for shift-click craft could be complex, simple 1 for now or calculate from itemstack
            int amount = event.getRecipe().getResult().getAmount();
            questModule.handleQuestProgress(p, QuestType.CRAFT, result.name(), amount);
        }
    }

    @EventHandler
    public void onFish(PlayerFishEvent event) {
        if (event.getState() == PlayerFishEvent.State.CAUGHT_FISH && event.getCaught() instanceof org.bukkit.entity.Item) {
            org.bukkit.entity.Item item = (org.bukkit.entity.Item) event.getCaught();
            questModule.handleQuestProgress(event.getPlayer(), QuestType.FISH, item.getItemStack().getType().name(), 1);
        }
    }

    @EventHandler
    public void onShear(PlayerShearEntityEvent event) {
        questModule.handleQuestProgress(event.getPlayer(), QuestType.SHEAR, event.getEntity().getType().name(), 1);
    }

    @EventHandler
    public void onFurnaceExtract(FurnaceExtractEvent event) {
        questModule.handleQuestProgress(event.getPlayer(), QuestType.COOK, event.getItemType().name(), event.getItemAmount());
    }

    @EventHandler
    public void onConsume(PlayerItemConsumeEvent event) {
        questModule.handleQuestProgress(event.getPlayer(), QuestType.CONSUME, event.getItem().getType().name(), 1);
    }

    @EventHandler
    public void onBreed(EntityBreedEvent event) {
        if (event.getBreeder() instanceof Player) {
            questModule.handleQuestProgress((Player) event.getBreeder(), QuestType.BREED, event.getEntity().getType().name(), 1);
        }
    }

    @EventHandler
    public void onPickup(org.bukkit.event.entity.EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            questModule.handleQuestProgress(player, QuestType.PICKUP, event.getItem().getItemStack().getType().name(), event.getItem().getItemStack().getAmount());
        }
    }
}

