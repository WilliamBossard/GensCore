package fr.gens.core.modules.tomb;

import fr.gens.core.CorePlugin;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Location;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class TombListener implements Listener {

    private final CorePlugin plugin;
    private final TombModule module;

    public TombListener(CorePlugin plugin, TombModule module) {
        this.plugin = plugin;
        this.module = module;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (!plugin.getConfig().getBoolean("modules.tomb.enabled", true)) return;
        
        Player player = event.getPlayer();
        if (event.getKeepInventory()) return; // Pas de tombe si le joueur garde son inventaire
        
        List<ItemStack> drops = event.getDrops();
        if (drops.isEmpty() && event.getDroppedExp() == 0) return; // Rien à sauver

        Location loc = player.getLocation().getBlock().getLocation();
        
        // Trouver un bloc d'air libre pour poser la tombe
        Block targetBlock = loc.getBlock();
        if (targetBlock.getType() != Material.AIR && targetBlock.getType() != Material.WATER) {
            targetBlock = loc.clone().add(0, 1, 0).getBlock();
        }

        // On crée un tableau avec les items
        ItemStack[] contents = drops.toArray(new ItemStack[0]);
        
        int xp = 0;
        if (plugin.getConfig().getBoolean("modules.tomb.store_xp", true)) {
            xp = event.getDroppedExp();
            event.setDroppedExp(0);
        }
        
        long expirationSecs = plugin.getConfig().getLong("modules.tomb.expiration_time_seconds", 3600);

        // Enregistrer la tombe
        module.getTombManager().createTomb(player.getUniqueId(), targetBlock.getLocation(), contents, xp, expirationSecs * 1000L);
        
        // Placer le bloc
        String blockTypeStr = plugin.getConfig().getString("modules.tomb.block_type", "CHEST").toUpperCase();
        Material material;
        try {
            material = Material.valueOf(blockTypeStr);
        } catch (IllegalArgumentException e) {
            material = Material.CHEST;
        }
        targetBlock.setType(material);

        event.getDrops().clear(); // On empêche les items de tomber par terre
        player.sendMessage(MiniMessage.miniMessage().deserialize("<green>Votre tombe a été placée en " + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ() + "."));
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        
        Block block = event.getClickedBlock();
        if (block == null) return;

        TombData tomb = module.getTombManager().getTombAt(block.getLocation());
        if (tomb == null) return;

        event.setCancelled(true); // On annule l'ouverture classique (ex: menu coffre)

        Player player = event.getPlayer();
        boolean isOwner = player.getUniqueId().equals(tomb.getOwnerId());
        boolean isExpired = tomb.isExpired();
        
        String defaultAccess = plugin.getConfig().getString("modules.tomb.default_access", "OWNER_ONLY").toUpperCase();
        String expirationAction = plugin.getConfig().getString("modules.tomb.expiration_action", "UNLOCK").toUpperCase();

        boolean canOpen = false;
        
        if (isOwner) {
            canOpen = true;
        } else if (defaultAccess.equals("EVERYONE")) {
            canOpen = true;
        } else if (isExpired && expirationAction.equals("UNLOCK")) {
            canOpen = true;
        }

        if (canOpen) {
            // Rendre l'inventaire
            for (ItemStack item : tomb.getContents()) {
                if (item != null && item.getType() != Material.AIR) {
                    if (player.getInventory().firstEmpty() != -1) {
                        player.getInventory().addItem(item);
                    } else {
                        player.getWorld().dropItemNaturally(player.getLocation(), item);
                    }
                }
            }
            if (tomb.getXp() > 0) {
                player.giveExp(tomb.getXp());
            }

            module.getTombManager().removeTomb(tomb.getId());
            player.sendMessage(MiniMessage.miniMessage().deserialize("<green>Vous avez récupéré le contenu de la tombe."));
        } else {
            player.sendMessage(MiniMessage.miniMessage().deserialize("<red>Cette tombe ne vous appartient pas."));
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        TombData tomb = module.getTombManager().getTombAt(event.getBlock().getLocation());
        if (tomb != null) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(MiniMessage.miniMessage().deserialize("<red>Faites un clic droit pour ouvrir la tombe, vous ne pouvez pas la casser."));
        }
    }

    @EventHandler
    public void onBlockExplode(BlockExplodeEvent event) {
        Iterator<Block> iterator = event.blockList().iterator();
        while (iterator.hasNext()) {
            Block block = iterator.next();
            if (module.getTombManager().getTombAt(block.getLocation()) != null) {
                iterator.remove();
            }
        }
    }

    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event) {
        Iterator<Block> iterator = event.blockList().iterator();
        while (iterator.hasNext()) {
            Block block = iterator.next();
            if (module.getTombManager().getTombAt(block.getLocation()) != null) {
                iterator.remove();
            }
        }
    }
}
