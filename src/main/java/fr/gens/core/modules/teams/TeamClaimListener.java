package fr.gens.core.modules.teams;

import fr.gens.core.CorePlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.title.Title;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.vehicle.VehicleDamageEvent;
import org.bukkit.inventory.ItemStack;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class TeamClaimListener implements Listener {

    private final CorePlugin plugin;
    private final Map<UUID, Long> lastDenialMessage = new ConcurrentHashMap<>();

    public TeamClaimListener(CorePlugin plugin) {
        this.plugin = plugin;
    }

    public TeamClaimListener(CorePlugin plugin, TeamClaimManager claimManager) {
        this.plugin = plugin;
    }

    private TeamClaimManager getClaimManager() {
        if (plugin.getTeamManager() != null) {
            return plugin.getTeamManager().getClaimManager();
        }
        return null;
    }

    public boolean canAccess(Player player, String world, int chunkX, int chunkZ) {
        if (player == null) return false;
        if (player.hasPermission("genscore.admin") || player.hasPermission("genscore.claim.bypass")) {
            return true;
        }

        TeamClaimManager claimManager = getClaimManager();
        if (claimManager == null) {
            return true;
        }

        Integer teamId = claimManager.getTeamIdAt(world, chunkX, chunkZ);
        if (teamId == null) {
            return true; // Non revendiqué (Nature Sauvage)
        }

        if (plugin.getTeamManager() == null) {
            return true;
        }

        TeamData team = plugin.getTeamManager().getTeam(teamId);
        if (team == null) {
            return true;
        }

        return team.hasMember(player.getUniqueId());
    }

    public boolean canAccess(Player player, Chunk chunk) {
        if (chunk == null) return true;
        return canAccess(player, chunk.getWorld().getName(), chunk.getX(), chunk.getZ());
    }

    public boolean canAccess(Player player, Location loc) {
        if (loc == null || loc.getWorld() == null) return true;
        return canAccess(player, loc.getWorld().getName(), loc.getBlockX() >> 4, loc.getBlockZ() >> 4);
    }

    private void notifyDenial(Player player, String world, int chunkX, int chunkZ) {
        if (player == null) return;
        long now = System.currentTimeMillis();
        Long last = lastDenialMessage.get(player.getUniqueId());
        if (last != null && now - last < 1000) {
            return; // Anti-spam 1 seconde
        }
        lastDenialMessage.put(player.getUniqueId(), now);

        TeamClaimManager claimManager = getClaimManager();
        if (claimManager == null) return;
        Integer teamId = claimManager.getTeamIdAt(world, chunkX, chunkZ);
        if (teamId == null) return;

        TeamData team = (plugin.getTeamManager() != null) ? plugin.getTeamManager().getTeam(teamId) : null;
        String teamName = (team != null) ? team.getName() : "Inconnue";

        player.sendActionBar(plugin.getLangManager().get("teamclaims.denial",
                Placeholder.parsed("team_name", teamName)));
    }

    private Player resolvePlayer(Entity entity) {
        if (entity instanceof Player p) {
            return p;
        }
        if (entity instanceof Projectile proj && proj.getShooter() instanceof Player p) {
            return p;
        }
        return null;
    }

    private boolean isProtectedBlock(Block block) {
        if (block == null) return false;
        if (block.getState() instanceof Container) return true;

        Material mat = block.getType();
        String name = mat.name();
        return name.contains("CHEST") || name.contains("BARREL") || name.contains("SHULKER") ||
               name.contains("DOOR") || name.contains("GATE") || name.contains("TRAPDOOR") ||
               name.contains("BUTTON") || name.contains("LEVER") || name.contains("PRESSURE_PLATE") ||
               name.contains("ANVIL") || name.contains("FURNACE") || name.contains("SMOKER") ||
               name.contains("HOPPER") || name.contains("DISPENSER") || name.contains("DROPPER") ||
               name.contains("BREWING") || name.contains("ENCHANT") || name.contains("BEACON") ||
               name.contains("BED") || name.contains("REPEATER") || name.contains("COMPARATOR") ||
               name.contains("DAYLIGHT") || name.contains("JUKEBOX") || name.contains("NOTE_BLOCK") ||
               name.contains("BELL") || name.contains("CAMPFIRE") || name.contains("CAULDRON") ||
               name.contains("COMPOSTER") || name.contains("LECTERN") || name.contains("POT") ||
               name.contains("BOOKSHELF") || name.contains("CRAFTER") || name.contains("VAULT") ||
               name.contains("TABLE") || name.contains("LOOM") || name.contains("STONECUTTER") ||
               name.contains("GRINDSTONE") || name.contains("SIGN") || mat == Material.RESPAWN_ANCHOR ||
               mat == Material.CAKE || mat == Material.DRAGON_EGG;
    }

    private boolean isBlockModifyingItem(ItemStack item) {
        if (item == null) return false;
        Material mat = item.getType();
        String name = mat.name();
        return name.contains("BUCKET") || name.contains("HOE") || name.contains("SHOVEL") ||
               name.contains("AXE") || name.contains("FLINT_AND_STEEL") || name.contains("FIRE_CHARGE") ||
               name.contains("BONE_MEAL") || name.contains("DYE") || name.contains("INK_SAC") ||
               name.contains("HONEYCOMB") || name.contains("SPAWN_EGG") || name.contains("ARMOR_STAND") ||
               name.contains("END_CRYSTAL") || name.contains("BOAT") || name.contains("MINECART") ||
               name.contains("LEAD") || name.contains("NAME_TAG");
    }

    private boolean isProtectedEntity(Entity target) {
        return target instanceof Animals ||
               target instanceof Villager ||
               target instanceof ArmorStand ||
               target instanceof ItemFrame ||
               target instanceof Hanging ||
               target instanceof Vehicle ||
               target instanceof org.bukkit.entity.Tameable ||
               target instanceof org.bukkit.entity.Allay ||
               target instanceof org.bukkit.entity.IronGolem ||
               target instanceof org.bukkit.entity.Snowman;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        String world = block.getWorld().getName();
        int cx = block.getX() >> 4;
        int cz = block.getZ() >> 4;

        if (!canAccess(player, world, cx, cz)) {
            event.setCancelled(true);
            notifyDenial(player, world, cx, cz);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        String world = block.getWorld().getName();
        int cx = block.getX() >> 4;
        int cz = block.getZ() >> 4;

        if (!canAccess(player, world, cx, cz)) {
            event.setCancelled(true);
            notifyDenial(player, world, cx, cz);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBucketEmpty(PlayerBucketEmptyEvent event) {
        Block target = event.getBlockClicked().getRelative(event.getBlockFace());
        Player player = event.getPlayer();
        String world = target.getWorld().getName();
        int cx = target.getX() >> 4;
        int cz = target.getZ() >> 4;

        if (!canAccess(player, world, cx, cz)) {
            event.setCancelled(true);
            notifyDenial(player, world, cx, cz);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBucketFill(PlayerBucketFillEvent event) {
        Block target = event.getBlockClicked();
        Player player = event.getPlayer();
        String world = target.getWorld().getName();
        int cx = target.getX() >> 4;
        int cz = target.getZ() >> 4;

        if (!canAccess(player, world, cx, cz)) {
            event.setCancelled(true);
            notifyDenial(player, world, cx, cz);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Block block = event.getClickedBlock();
        if (block == null) return;

        String world = block.getWorld().getName();
        int cx = block.getX() >> 4;
        int cz = block.getZ() >> 4;

        if (event.getAction() == Action.PHYSICAL) {
            if (!canAccess(player, world, cx, cz)) {
                event.setCancelled(true);
            }
            return;
        }

        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            if (!canAccess(player, world, cx, cz)) {
                if (isProtectedBlock(block) || isBlockModifyingItem(event.getItem())) {
                    event.setCancelled(true);
                    notifyDenial(player, world, cx, cz);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityInteract(PlayerInteractEntityEvent event) {
        Entity entity = event.getRightClicked();
        if (entity == null) return;

        Player player = event.getPlayer();
        String world = entity.getWorld().getName();
        int cx = entity.getLocation().getBlockX() >> 4;
        int cz = entity.getLocation().getBlockZ() >> 4;

        if (!canAccess(player, world, cx, cz)) {
            if (isProtectedEntity(entity)) {
                event.setCancelled(true);
                notifyDenial(player, world, cx, cz);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onArmorStandManipulate(PlayerArmorStandManipulateEvent event) {
        ArmorStand stand = event.getRightClicked();
        Player player = event.getPlayer();
        String world = stand.getWorld().getName();
        int cx = stand.getLocation().getBlockX() >> 4;
        int cz = stand.getLocation().getBlockZ() >> 4;

        if (!canAccess(player, world, cx, cz)) {
            event.setCancelled(true);
            notifyDenial(player, world, cx, cz);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onHangingBreak(HangingBreakByEntityEvent event) {
        Player player = resolvePlayer(event.getRemover());
        if (player == null) return;

        Entity hanging = event.getEntity();
        String world = hanging.getWorld().getName();
        int cx = hanging.getLocation().getBlockX() >> 4;
        int cz = hanging.getLocation().getBlockZ() >> 4;

        if (!canAccess(player, world, cx, cz)) {
            event.setCancelled(true);
            notifyDenial(player, world, cx, cz);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onVehicleDamage(VehicleDamageEvent event) {
        Player player = resolvePlayer(event.getAttacker());
        if (player == null) return;

        Entity vehicle = event.getVehicle();
        String world = vehicle.getWorld().getName();
        int cx = vehicle.getLocation().getBlockX() >> 4;
        int cz = vehicle.getLocation().getBlockZ() >> 4;

        if (!canAccess(player, world, cx, cz)) {
            event.setCancelled(true);
            notifyDenial(player, world, cx, cz);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onTakeLecternBook(PlayerTakeLecternBookEvent event) {
        Block block = event.getLectern().getBlock();
        Player player = event.getPlayer();
        String world = block.getWorld().getName();
        int cx = block.getX() >> 4;
        int cz = block.getZ() >> 4;

        if (!canAccess(player, world, cx, cz)) {
            event.setCancelled(true);
            notifyDenial(player, world, cx, cz);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onShearEntity(PlayerShearEntityEvent event) {
        Entity entity = event.getEntity();
        Player player = event.getPlayer();
        String world = entity.getWorld().getName();
        int cx = entity.getLocation().getBlockX() >> 4;
        int cz = entity.getLocation().getBlockZ() >> 4;

        if (!canAccess(player, world, cx, cz)) {
            event.setCancelled(true);
            notifyDenial(player, world, cx, cz);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        Entity target = event.getEntity();
        Player player = resolvePlayer(event.getDamager());
        if (player == null) return;

        if (isProtectedEntity(target)) {
            String world = target.getWorld().getName();
            int cx = target.getLocation().getBlockX() >> 4;
            int cz = target.getLocation().getBlockZ() >> 4;

            if (!canAccess(player, world, cx, cz)) {
                event.setCancelled(true);
                notifyDenial(player, world, cx, cz);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        TeamClaimManager claimManager = getClaimManager();
        if (claimManager == null) return;

        event.blockList().removeIf(b -> claimManager.isChunkClaimed(b.getWorld().getName(), b.getX() >> 4, b.getZ() >> 4));
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        TeamClaimManager claimManager = getClaimManager();
        if (claimManager == null) return;

        event.blockList().removeIf(b -> claimManager.isChunkClaimed(b.getWorld().getName(), b.getX() >> 4, b.getZ() >> 4));
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        TeamClaimManager claimManager = getClaimManager();
        if (claimManager == null) return;

        Chunk sourceChunk = event.getBlock().getChunk();
        Integer sourceTeam = claimManager.getTeamIdAt(sourceChunk);

        for (Block b : event.getBlocks()) {
            Block target = b.getRelative(event.getDirection());
            Chunk targetChunk = target.getChunk();
            Integer targetTeam = claimManager.getTeamIdAt(targetChunk);

            if (targetTeam != null && (sourceTeam == null || !sourceTeam.equals(targetTeam))) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        TeamClaimManager claimManager = getClaimManager();
        if (claimManager == null) return;

        Chunk sourceChunk = event.getBlock().getChunk();
        Integer sourceTeam = claimManager.getTeamIdAt(sourceChunk);

        for (Block b : event.getBlocks()) {
            Chunk targetChunk = b.getChunk();
            Integer targetTeam = claimManager.getTeamIdAt(targetChunk);

            if (targetTeam != null && (sourceTeam == null || !sourceTeam.equals(targetTeam))) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        if (event.getTo() == null) return;
        int fromX = event.getFrom().getBlockX() >> 4;
        int fromZ = event.getFrom().getBlockZ() >> 4;
        int toX = event.getTo().getBlockX() >> 4;
        int toZ = event.getTo().getBlockZ() >> 4;

        if (fromX == toX && fromZ == toZ && event.getFrom().getWorld().equals(event.getTo().getWorld())) {
            return; // Même chunk
        }

        TeamClaimManager claimManager = getClaimManager();
        if (claimManager == null || plugin.getTeamManager() == null) return;

        String fromWorld = event.getFrom().getWorld().getName();
        String toWorld = event.getTo().getWorld().getName();

        Integer fromTeamId = claimManager.getTeamIdAt(fromWorld, fromX, fromZ);
        Integer toTeamId = claimManager.getTeamIdAt(toWorld, toX, toZ);

        if (Objects.equals(fromTeamId, toTeamId)) {
            return; // Même claim ou toujours dans la nature
        }

        Player player = event.getPlayer();

        if (toTeamId != null) {
            // Entrée sur le territoire d'une guilde
            TeamData team = plugin.getTeamManager().getTeam(toTeamId);
            if (team != null) {
                String color = team.getColor();
                if (color == null || color.isBlank() || !color.startsWith("#")) {
                    color = "#2ecc71";
                }

                Component mainTitle = MiniMessage.miniMessage()
                        .deserialize("<color:" + color + "><bold>" + team.getName() + "</bold></color>");
                Component subTitle = plugin.getLangManager().get("teamclaims.title_enter_sub");

                Title.Times times = Title.Times.times(
                        Duration.ofMillis(250),
                        Duration.ofMillis(1750),
                        Duration.ofMillis(350)
                );
                player.showTitle(Title.title(mainTitle, subTitle, times));
            }
        } else if (fromTeamId != null) {
            // Sortie de territoire vers la nature sauvage
            Component mainTitle = plugin.getLangManager().get("teamclaims.title_wilderness_main");
            Component subTitle = plugin.getLangManager().get("teamclaims.title_wilderness_sub");

            Title.Times times = Title.Times.times(
                    Duration.ofMillis(250),
                    Duration.ofMillis(1500),
                    Duration.ofMillis(300)
            );
            player.showTitle(Title.title(mainTitle, subTitle, times));
        }
    }
}
