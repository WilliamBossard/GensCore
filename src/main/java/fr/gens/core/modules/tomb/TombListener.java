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

import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.Bukkit;
import java.util.UUID;

import java.util.Iterator;
import java.util.List;


public class TombListener implements Listener {

    private final CorePlugin plugin;
    private final TombModule module;

    public TombListener(CorePlugin plugin, TombModule module) {
        this.plugin = plugin;
        this.module = module;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    @SuppressWarnings("deprecation")
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (!plugin.getConfigManager().getConfig("modules/tomb.yml").getBoolean("modules.tomb.enabled", true)) return;
        
        Player player = event.getPlayer();
        if (event.getKeepInventory()) return; // Pas de tombe si le joueur garde son inventaire
        
        List<ItemStack> drops = event.getDrops();
        if (drops.isEmpty() && event.getDroppedExp() == 0) return; // Rien à sauver

        Location pLoc = player.getLocation();
        if (pLoc == null) return;
        Location loc = pLoc.getBlock().getLocation();
        // Trouver un emplacement libre (Air, Eau, Lave)
        Block targetBlock = loc.getBlock();
        
        // Si on est dans un bloc solide ou non-remplaçable (herbe haute, dalles, etc.), on monte
        while (!targetBlock.getType().isAir() && targetBlock.getType() != Material.WATER && targetBlock.getType() != Material.LAVA) {
            if (targetBlock.getY() >= targetBlock.getWorld().getMaxHeight() - 1) {
                break;
            }
            targetBlock = targetBlock.getRelative(0, 1, 0);
        }
        
        // Si on est dans l'air/eau, on s'assure d'être sur un bloc solide
        if (targetBlock.getType().isAir() || targetBlock.getType() == Material.WATER || targetBlock.getType() == Material.LAVA) {
            while (targetBlock.getY() > targetBlock.getWorld().getMinHeight()) {
                Block below = targetBlock.getRelative(0, -1, 0);
                if (below.getType().isSolid() || below.getType() == Material.LAVA) {
                    break;
                }
                targetBlock = below;
            }
        }

        // On crée un tableau avec les items
        ItemStack[] contents = drops.toArray(new ItemStack[0]);
        
        int xp = 0;
        if (plugin.getConfigManager().getConfig("modules/tomb.yml").getBoolean("modules.tomb.store_xp", true)) {
            // Calculate total XP
            int level = player.getLevel();
            float expProgress = player.getExp();
            int totalExp = 0;
            if (level <= 15) {
                totalExp = (int) (level * level + 6 * level);
            } else if (level <= 30) {
                totalExp = (int) (2.5 * level * level - 40.5 * level + 360);
            } else {
                totalExp = (int) (4.5 * level * level - 162.5 * level + 2220);
            }
            int expToNextLevel = player.getExpToLevel();
            totalExp += Math.round(expProgress * expToNextLevel);

            int percentage = plugin.getConfigManager().getConfig("modules/tomb.yml").getInt("modules.tomb.xp_keep_percentage", 100);
            xp = (int) (totalExp * (percentage / 100.0f));

            event.setDroppedExp(0);
            event.setNewExp(0);
            event.setNewLevel(0);
            event.setNewTotalExp(0);
        }
        
        long expirationSecs = plugin.getConfigManager().getConfig("modules/tomb.yml").getLong("modules.tomb.expiration_time_seconds", 3600);
        long expirationMs = expirationSecs <= 0 ? -1L : expirationSecs * 1000L;

        // Enregistrer la tombe
        module.getTombManager().createTomb(player.getUniqueId(), targetBlock.getLocation(), contents, xp, expirationMs);
        
        // Placer le bloc
        String blockTypeStr = plugin.getConfigManager().getConfig("modules/tomb.yml").getString("modules.tomb.block_type", "CHEST").toUpperCase();
        Material material;
        try {
            material = Material.valueOf(blockTypeStr);
        } catch (IllegalArgumentException e) {
            material = Material.CHEST;
        }
        targetBlock.setType(material);
        
        if (material == Material.PLAYER_HEAD) {
            org.bukkit.block.BlockState state = targetBlock.getState();
            if (state instanceof org.bukkit.block.Skull) {
                org.bukkit.block.Skull skull = (org.bukkit.block.Skull) state;
                // Use setPlayerProfile for better compatibility since Paper deprecated all Bukkit methods
                // and it preserves Bedrock skins from SkinsRestorer
                skull.setPlayerProfile(player.getPlayerProfile());
                skull.update();
            }
        }

        event.getDrops().clear(); // On empêche les items de tomber par terre
        player.sendMessage(fr.gens.core.utils.PlaceholderUtils.parseToComponent("<green>Votre tombe a été placée en " + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ() + "."));
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
        
        String defaultAccess = plugin.getConfigManager().getConfig("modules/tomb.yml").getString("modules.tomb.default_access", "OWNER_ONLY").toUpperCase();
        String expirationAction = plugin.getConfigManager().getConfig("modules/tomb.yml").getString("modules.tomb.expiration_action", "UNLOCK").toUpperCase();

        boolean canOpen = false;
        
        if (isOwner || player.hasPermission("genscore.tomb.admin")) {
            canOpen = true;
        } else if (defaultAccess.equals("EVERYONE")) {
            canOpen = true;
        } else if (isExpired && expirationAction.equals("UNLOCK")) {
            canOpen = true;
        }

        if (canOpen) {
            // Rendre l'inventaire et auto-équiper si possible
            for (ItemStack item : tomb.getContents()) {
                if (item != null && item.getType() != Material.AIR) {
                    boolean equipped = false;
                    String type = item.getType().name();
                    if (type.endsWith("_HELMET") && (player.getInventory().getHelmet() == null || player.getInventory().getHelmet().getType() == Material.AIR)) {
                        player.getInventory().setHelmet(item);
                        equipped = true;
                    } else if (type.endsWith("_CHESTPLATE") && (player.getInventory().getChestplate() == null || player.getInventory().getChestplate().getType() == Material.AIR)) {
                        player.getInventory().setChestplate(item);
                        equipped = true;
                    } else if (type.endsWith("_LEGGINGS") && (player.getInventory().getLeggings() == null || player.getInventory().getLeggings().getType() == Material.AIR)) {
                        player.getInventory().setLeggings(item);
                        equipped = true;
                    } else if (type.endsWith("_BOOTS") && (player.getInventory().getBoots() == null || player.getInventory().getBoots().getType() == Material.AIR)) {
                        player.getInventory().setBoots(item);
                        equipped = true;
                    } else if (type.equals("SHIELD") && (player.getInventory().getItemInOffHand() == null || player.getInventory().getItemInOffHand().getType() == Material.AIR)) {
                        player.getInventory().setItemInOffHand(item);
                        equipped = true;
                    }

                    if (!equipped) {
                        if (player.getInventory().firstEmpty() != -1) {
                            player.getInventory().addItem(item);
                        } else {
                            player.getWorld().dropItemNaturally(player.getLocation(), item);
                        }
                    }
                }
            }
            if (tomb.getXp() > 0) {
                player.giveExp(tomb.getXp());
            }

            module.getTombManager().removeTomb(tomb.getId());
            player.sendMessage(fr.gens.core.utils.PlaceholderUtils.parseToComponent("<green>Vous avez récupéré le contenu de la tombe."));
        } else {
            player.sendMessage(fr.gens.core.utils.PlaceholderUtils.parseToComponent("<red>Cette tombe ne vous appartient pas."));
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        TombData tomb = module.getTombManager().getTombAt(event.getBlock().getLocation());
        if (tomb != null) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(fr.gens.core.utils.PlaceholderUtils.parseToComponent("<red>Faites un clic droit pour ouvrir la tombe, vous ne pouvez pas la casser."));
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

    @EventHandler
    public void onEntitiesLoad(EntitiesLoadEvent event) {
        NamespacedKey key = new NamespacedKey(plugin, "tomb_id");
        for (Entity entity : event.getEntities()) {
            if (entity.getType() == EntityType.TEXT_DISPLAY && entity.getPersistentDataContainer().has(key, PersistentDataType.STRING)) {
                String storedId = entity.getPersistentDataContainer().get(key, PersistentDataType.STRING);
                try {
                    UUID id = UUID.fromString(storedId);
                    TombData tomb = null;
                    // On ne peut pas faire un module.getTombManager().getTombById() car il n'y a pas cette methode,
                    // mais on peut le recuperer via tombByLocation si on connait la loc, ou iterer.
                    // On va juste iterer, c'est au load du chunk donc pas tres grave
                    // En fait, on a juste besoin de verifier si la tombe existe encore !
                    Block block = entity.getLocation().clone().subtract(0.5, 1.2, 0.5).getBlock();
                    tomb = module.getTombManager().getTombAt(block.getLocation());
                    
                    if (tomb == null || !tomb.getId().equals(id)) {
                        entity.remove(); // Ghost hologram, remove it
                    } else {
                        // Update it
                        TextDisplay display = (TextDisplay) entity;
                        String ownerName = Bukkit.getOfflinePlayer(tomb.getOwnerId()).getName();
                        if (ownerName == null) ownerName = "Inconnu";
                        if (tomb.isExpired() && plugin.getConfigManager().getConfig("modules/tomb.yml").getString("modules.tomb.expiration_action", "UNLOCK").toUpperCase().equals("UNLOCK")) {
                            display.text(fr.gens.core.utils.PlaceholderUtils.parseToComponent("<gray>Tombe de <yellow>" + ownerName + "<br><green>Ouverte à tous"));
                        } else {
                            display.text(fr.gens.core.utils.PlaceholderUtils.parseToComponent("<gray>Tombe de <yellow>" + ownerName + "<br><red>Protégée"));
                        }
                    }
                } catch (Exception e) {
                    entity.remove(); // Bad ID
                }
            }
        }
    }
}

