package fr.gens.core.modules;

import fr.gens.core.CorePlugin;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.LeavesDecayEvent;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

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
        return "Les feuilles disparaissent instantanÃƒÆ’Ã‚Â©ment d'un coup quand un arbre est coupÃƒÆ’Ã‚Â©.";
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void enable() {
        enabled = true;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        plugin.getLangManager().sendConsoleMessage("fastleafdecaymodule.log_1");
    }

    @Override
    public void disable() {
        enabled = false;
        HandlerList.unregisterAll(this);
        plugin.getLangManager().sendConsoleMessage("fastleafdecaymodule.log_2");
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (!enabled) return;
        Block block = event.getBlock();
        if (isLog(block.getType())) {
            plugin.getFoliaLib().getImpl().runAtLocationLater(block.getLocation(), (t2) -> triggerDecay(block), 2L);
        }
    }

    @EventHandler
    public void onLeafDecay(LeavesDecayEvent event) {
        if (!enabled) return;
        event.setCancelled(true);
        triggerDecay(event.getBlock());
    }

    private void triggerDecay(Block startBlock) {
        java.util.function.Consumer<com.tcoded.folialib.wrapper.task.WrappedTask> decayTask = new java.util.function.Consumer<com.tcoded.folialib.wrapper.task.WrappedTask>() {
            private final Set<Block> visited = new HashSet<>();
            private final Queue<Block> queue = new LinkedList<>();
            private int maxLeaves = 200;
            private int count = 0;
            
            {
                // Init queue
                for (int x = -1; x <= 1; x++) {
                    for (int y = -1; y <= 1; y++) {
                        for (int z = -1; z <= 1; z++) {
                            Block neighbor = startBlock.getRelative(x, y, z);
                            if (isLeaf(neighbor.getType())) {
                                queue.add(neighbor);
                                visited.add(neighbor);
                            }
                        }
                    }
                }
            }

            @Override
            public void accept(com.tcoded.folialib.wrapper.task.WrappedTask wrappedTask) {
                if (!enabled) {
                    wrappedTask.cancel();
                    return;
                }
                
                int checksThisTick = 0;
                int maxChecksPerTick = 15; // Limiter le nombre de feuilles traitées par tick pour éviter le lag

                while (!queue.isEmpty() && count < maxLeaves && checksThisTick < maxChecksPerTick) {
                    Block current = queue.poll();
                    checksThisTick++;
                    
                    // Si cette feuille est encore proche d'une bûche, on arrête de détruire ce côté
                    if (isCloseToLog(current)) {
                        continue;
                    }

                    count++;
                    
                    // Détruire la feuille
                    if (isLeaf(current.getType())) {
                        for (org.bukkit.inventory.ItemStack drop : current.getDrops()) {
                            current.getWorld().dropItemNaturally(current.getLocation(), drop);
                        }
                        current.getWorld().spawnParticle(Particle.BLOCK, current.getLocation().add(0.5, 0.5, 0.5), 10, current.getBlockData());
                        current.setType(Material.AIR);
                    }

                    // Propager
                    for (int x = -1; x <= 1; x++) {
                        for (int y = -1; y <= 1; y++) {
                            for (int z = -1; z <= 1; z++) {
                                Block neighbor = current.getRelative(x, y, z);
                                if (isLeaf(neighbor.getType()) && !visited.contains(neighbor)) {
                                    queue.add(neighbor);
                                    visited.add(neighbor);
                                }
                            }
                        }
                    }
                }
                
                if (queue.isEmpty() || count >= maxLeaves) {
                    wrappedTask.cancel();
                }
            }
        };
        plugin.getFoliaLib().getImpl().runAtLocationTimer(startBlock.getLocation(), decayTask, 1L, 1L);
    }

    private boolean isCloseToLog(Block block) {
        // Optimisation : vérifier d'abord les blocs adjacents avant d'étendre la recherche
        for (int d = 1; d <= 3; d++) {
            for (int x = -d; x <= d; x++) {
                for (int y = -d; y <= d; y++) {
                    for (int z = -d; z <= d; z++) {
                        // Ne vérifier que la "couche" extérieure (distance d)
                        if (Math.abs(x) == d || Math.abs(y) == d || Math.abs(z) == d) {
                            if (isLog(block.getRelative(x, y, z).getType())) {
                                return true;
                            }
                        }
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

