package fr.gens.core.modules.teams;

import fr.gens.core.CorePlugin;
import fr.gens.core.utils.PlaceholderUtils;
import org.bukkit.Chunk;
import org.bukkit.block.Block;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;

public class TeamClaimListener implements Listener {

    private final CorePlugin plugin;
    private final TeamClaimManager claimManager;

    public TeamClaimListener(CorePlugin plugin, TeamClaimManager claimManager) {
        this.plugin = plugin;
        this.claimManager = claimManager;
    }

    private boolean canAccess(Player player, Chunk chunk) {
        if (player.hasPermission("genscore.admin") || player.hasPermission("genscore.claim.bypass")) {
            return true;
        }

        Integer teamId = claimManager.getTeamIdAt(chunk);
        if (teamId == null) {
            return true; // Non revendique
        }

        TeamData team = plugin.getTeamManager().getTeam(teamId);
        if (team == null) {
            return true;
        }

        return team.hasMember(player.getUniqueId());
    }

    private void notifyDenial(Player player, Chunk chunk) {
        Integer teamId = claimManager.getTeamIdAt(chunk);
        if (teamId == null) return;
        TeamData team = plugin.getTeamManager().getTeam(teamId);
        String teamName = (team != null) ? team.getName() : "Inconnue";

        player.sendActionBar(PlaceholderUtils.parseToComponent("<red>Territoire protege par la guilde <yellow>" + teamName + "</yellow>.</red>"));
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Chunk chunk = event.getBlock().getChunk();

        if (!canAccess(player, chunk)) {
            event.setCancelled(true);
            notifyDenial(player, chunk);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        Chunk chunk = event.getBlock().getChunk();

        if (!canAccess(player, chunk)) {
            event.setCancelled(true);
            notifyDenial(player, chunk);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK && event.getAction() != Action.PHYSICAL) {
            return;
        }

        Block block = event.getClickedBlock();
        if (block == null) return;

        Chunk chunk = block.getChunk();
        Player player = event.getPlayer();

        if (!canAccess(player, chunk)) {
            // Verifier si le bloc est interactif
            String type = block.getType().name();
            if (type.contains("CHEST") || type.contains("SHULKER") || type.contains("BARREL") ||
                type.contains("DOOR") || type.contains("GATE") || type.contains("LEVER") ||
                type.contains("BUTTON") || type.contains("HOPPER") || type.contains("FURNACE") ||
                type.contains("DROPPER") || type.contains("DISPENSER") || type.contains("ANVIL") ||
                event.getAction() == Action.PHYSICAL) {
                event.setCancelled(true);
                notifyDenial(player, chunk);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        Entity target = event.getEntity();
        Entity damager = event.getDamager();

        if (!(damager instanceof Player)) return;
        Player player = (Player) damager;

        if (target instanceof Animals || target instanceof Villager || target instanceof ArmorStand || target instanceof ItemFrame) {
            Chunk chunk = target.getLocation().getChunk();
            if (!canAccess(player, chunk)) {
                event.setCancelled(true);
                notifyDenial(player, chunk);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent event) {
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
}
