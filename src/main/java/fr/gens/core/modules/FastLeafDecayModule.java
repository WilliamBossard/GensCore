package fr.gens.core.modules;

import fr.gens.core.CorePlugin;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.LeavesDecayEvent;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.Particle;

public class FastLeafDecayModule implements Module, Listener {

    private final CorePlugin plugin;
    private boolean enabled = false;

    public FastLeafDecayModule(CorePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "FastLeafDecay";
    }

    @Override
    public String getDescription() {
        return "Les feuilles disparaissent instantanément d'un coup quand un arbre est coupé.";
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void enable() {
        enabled = true;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        plugin.getLogger().info("[FastLeafDecay] Activé.");
    }

    @Override
    public void disable() {
        enabled = false;
        HandlerList.unregisterAll(this);
        plugin.getLogger().info("[FastLeafDecay] Désactivé.");
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (!enabled) return;
        Block block = event.getBlock();
        if (isLog(block.getType())) {
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> triggerDecay(block), 2L);
        }
    }

    @EventHandler
    public void onLeafDecay(LeavesDecayEvent event) {
        if (!enabled) return;
        event.setCancelled(true);
        triggerDecay(event.getBlock());
    }

    private void triggerDecay(Block startBlock) {
        Set<Block> leavesToBreak = new HashSet<>();
        Queue<Block> queue = new LinkedList<>();
        
        // Trouver les premières feuilles autour du bloc cassé
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    Block neighbor = startBlock.getRelative(x, y, z);
                    if (isLeaf(neighbor.getType())) {
                        queue.add(neighbor);
                        leavesToBreak.add(neighbor);
                    }
                }
            }
        }

        int maxLeaves = 200; // Limite de sécurité
        int count = 0;

        while (!queue.isEmpty() && count < maxLeaves) {
            Block current = queue.poll();
            
            // Si cette feuille est encore proche d'une bûche, on arrête de détruire ce côté
            if (isCloseToLog(current)) {
                leavesToBreak.remove(current);
                continue;
            }

            count++;

            for (int x = -1; x <= 1; x++) {
                for (int y = -1; y <= 1; y++) {
                    for (int z = -1; z <= 1; z++) {
                        Block neighbor = current.getRelative(x, y, z);
                        if (isLeaf(neighbor.getType()) && !leavesToBreak.contains(neighbor)) {
                            queue.add(neighbor);
                            leavesToBreak.add(neighbor);
                        }
                    }
                }
            }
        }

        // Casser les feuilles progressivement (plus smooth) avec particules
        List<Block> leavesList = new ArrayList<>(leavesToBreak);
        new BukkitRunnable() {
            int index = 0;
            final int batchSize = 4; // 4 feuilles par tick

            @Override
            public void run() {
                for (int i = 0; i < batchSize; i++) {
                    if (index >= leavesList.size()) {
                        this.cancel();
                        return;
                    }
                    Block leaf = leavesList.get(index++);
                    if (isLeaf(leaf.getType())) {
                        for (org.bukkit.inventory.ItemStack drop : leaf.getDrops()) {
                            leaf.getWorld().dropItemNaturally(leaf.getLocation(), drop);
                        }
                        leaf.getWorld().spawnParticle(Particle.BLOCK, leaf.getLocation().add(0.5, 0.5, 0.5), 10, leaf.getBlockData());
                        leaf.setType(Material.AIR);
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private boolean isCloseToLog(Block block) {
        for (int x = -3; x <= 3; x++) {
            for (int y = -3; y <= 3; y++) {
                for (int z = -3; z <= 3; z++) {
                    if (isLog(block.getRelative(x, y, z).getType())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean isLeaf(Material material) {
        return material.name().endsWith("_LEAVES");
    }

    private boolean isLog(Material material) {
        return material.name().endsWith("_LOG") || material.name().endsWith("_WOOD");
    }
}
