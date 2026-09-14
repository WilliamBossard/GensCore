package fr.gens.core.modules;

import fr.gens.core.CorePlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
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
            plugin.getFoliaLib().getScheduler().runAtLocationLater(block.getLocation(), (t2) -> triggerDecay(block), 2L);
        }
    }

    @EventHandler
    public void onLeafDecay(LeavesDecayEvent event) {
        if (!enabled) return;
        event.setCancelled(true);
        triggerDecay(event.getBlock());
    }

    private void triggerDecay(Block startBlock) {
        org.bukkit.World world = startBlock.getWorld();
        int sx = startBlock.getX();
        int sy = startBlock.getY();
        int sz = startBlock.getZ();

        java.util.function.Consumer<com.tcoded.folialib.wrapper.task.WrappedTask> decayTask = new java.util.function.Consumer<com.tcoded.folialib.wrapper.task.WrappedTask>() {
            private final Set<Block> visited = new HashSet<>();
            private final Queue<Block> queue = new LinkedList<>();
            private int maxLeaves = 200;
            private int count = 0;
            
            {
                // Init queue avec vérification préalable de la région Folia
                for (int x = -1; x <= 1; x++) {
                    for (int y = -1; y <= 1; y++) {
                        for (int z = -1; z <= 1; z++) {
                            int nx = sx + x;
                            int ny = sy + y;
                            int nz = sz + z;
                            Location loc = new Location(world, nx, ny, nz);
                            if (plugin.getFoliaLib().isFolia() && !Bukkit.isOwnedByCurrentRegion(loc)) {
                                // NOTE ARCHITECTURALE (Folia Cross-Region) :
                                // Si le bloc voisin se trouve dans une autre région de threads, on planifie
                                // la décomposition sur le scheduler de cette région cible plutôt que de l'ignorer.
                                plugin.getFoliaLib().getScheduler().runAtLocation(loc, (t) -> {
                                    if (!enabled) return;
                                    Block otherBlock = loc.getBlock();
                                    if (isLeaf(otherBlock.getType())) {
                                        triggerDecay(otherBlock);
                                    }
                                });
                                continue;
                            }
                            Block neighbor = world.getBlockAt(nx, ny, nz);
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
                    
                    // Si le bloc actuel n'est pas possédé par la région locale, on délègue à sa région
                    if (plugin.getFoliaLib().isFolia() && !Bukkit.isOwnedByCurrentRegion(current.getLocation())) {
                        Location cLoc = current.getLocation();
                        plugin.getFoliaLib().getScheduler().runAtLocation(cLoc, (t) -> {
                            if (!enabled) return;
                            Block otherBlock = cLoc.getBlock();
                            if (isLeaf(otherBlock.getType())) {
                                triggerDecay(otherBlock);
                            }
                        });
                        continue;
                    }

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

                    // Propager aux voisins en vérifiant la région AVANT d'accéder au bloc
                    int cx = current.getX();
                    int cy = current.getY();
                    int cz = current.getZ();
                    for (int x = -1; x <= 1; x++) {
                        for (int y = -1; y <= 1; y++) {
                            for (int z = -1; z <= 1; z++) {
                                int nx = cx + x;
                                int ny = cy + y;
                                int nz = cz + z;
                                Location loc = new Location(world, nx, ny, nz);
                                if (plugin.getFoliaLib().isFolia() && !Bukkit.isOwnedByCurrentRegion(loc)) {
                                    // Propagation cross-region via runAtLocation pour ne pas abandonner les demi-arbres
                                    plugin.getFoliaLib().getScheduler().runAtLocation(loc, (t) -> {
                                        if (!enabled) return;
                                        Block otherBlock = loc.getBlock();
                                        if (isLeaf(otherBlock.getType())) {
                                            triggerDecay(otherBlock);
                                        }
                                    });
                                    continue;
                                }
                                Block neighbor = world.getBlockAt(nx, ny, nz);
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
        plugin.getFoliaLib().getScheduler().runAtLocationTimer(startBlock.getLocation(), decayTask, 1L, 1L);
    }

    private boolean isCloseToLog(Block block) {
        if (block.getBlockData() instanceof org.bukkit.block.data.type.Leaves leaves) {
            if (leaves.isPersistent()) return true; // Placé par un joueur
            return leaves.getDistance() <= 6; // Calcul natif du moteur Minecraft (1 à 7)
        }
        return false;
    }

    private boolean isLeaf(Material material) {
        return org.bukkit.Tag.LEAVES.isTagged(material);
    }

    private boolean isLog(Material material) {
        return org.bukkit.Tag.LOGS.isTagged(material);
    }
}




