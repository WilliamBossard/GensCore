package fr.gens.core.modules.perks;

import fr.gens.core.CorePlugin;
import fr.gens.core.utils.PlaceholderUtils;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class SoloPerkListener implements Listener {

    private final CorePlugin plugin;
    private final SoloPerkManager manager;
    private com.tcoded.folialib.wrapper.task.WrappedTask periodicTask;

    public SoloPerkListener(CorePlugin plugin, SoloPerkManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    public void startTasks() {
        if (periodicTask != null) {
            periodicTask.cancel();
        }

        // Tâche périodique (toutes les secondes) pour l'Aimant et le Speed Boost
        periodicTask = plugin.getFoliaLib().getScheduler().runTimer(() -> {
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                if (player == null || !player.isOnline()) continue;

                final UUID uuid = player.getUniqueId();
                plugin.getFoliaLib().getScheduler().runAtEntity(player, task -> {
                    if (!player.isOnline() || player.isDead()) return;

                    // 1. Aimant de Récolte (Magnet)
                    if (manager.isPerkActive(uuid, SoloPerkType.MAGNET)) {
                        for (Entity entity : player.getNearbyEntities(5.0, 4.0, 5.0)) {
                            if (entity instanceof Item item && !item.isDead()) {
                                if (item.getPickupDelay() <= 0) {
                                    Vector dir = player.getLocation().add(0, 0.5, 0).toVector().subtract(item.getLocation().toVector());
                                    double distSq = dir.lengthSquared();
                                    if (distSq > 0.25) {
                                        Vector velocity = dir.normalize().multiply(0.45);
                                        item.setVelocity(velocity);
                                    }
                                }
                            }
                        }
                    }

                    // 2. Foulée Légère (Speed Boost hors combat)
                    if (manager.isPerkActive(uuid, SoloPerkType.SPEED_BOOST)) {
                        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 45, 0, true, false, false));
                    }
                });
            }
        }, 20L, 20L);
    }

    public void stopTasks() {
        if (periodicTask != null) {
            periodicTask.cancel();
            periodicTask = null;
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        manager.loadPlayer(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        manager.unloadPlayer(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockDropItem(BlockDropItemEvent event) {
        Player player = event.getPlayer();
        if (player == null) return;
        UUID uuid = player.getUniqueId();

        ItemStack tool = player.getInventory().getItemInMainHand();
        boolean hasSilkTouch = tool.containsEnchantment(Enchantment.SILK_TOUCH);

        boolean autoSmeltActive = !hasSilkTouch && manager.isPerkActive(uuid, SoloPerkType.AUTO_SMELT);
        boolean doubleDropActive = manager.isPerkActive(uuid, SoloPerkType.DOUBLE_DROP);

        for (Item itemEntity : event.getItems()) {
            ItemStack stack = itemEntity.getItemStack();
            if (stack == null || stack.getType() == Material.AIR) continue;

            // 1. Fonte Instantanée (Auto-Smelt)
            if (autoSmeltActive) {
                Material smelted = getSmeltedMaterial(stack.getType());
                if (smelted != null) {
                    stack = stack.withType(smelted);
                }
            }

            // 2. Bénédiction Minérale (5% double drop sur minerais et bois)
            if (doubleDropActive && isEligibleDoubleDrop(stack.getType())) {
                if (ThreadLocalRandom.current().nextDouble() < 0.05) {
                    stack.setAmount(Math.min(stack.getMaxStackSize(), stack.getAmount() * 2));
                    player.sendActionBar(PlaceholderUtils.parseToComponent("<gold>Benediction minerale : recolte doublee !</gold>"));
                }
            }

            itemEntity.setItemStack(stack);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        UUID uuid = player.getUniqueId();

        if (manager.isPerkActive(uuid, SoloPerkType.KEEP_EXP)) {
            int currentLevel = player.getLevel();
            if (currentLevel > 0) {
                int keptLevel = Math.max(1, currentLevel / 2);
                event.setKeepLevel(true);
                player.setLevel(keptLevel);
                event.setDroppedExp(0);
                player.sendMessage(PlaceholderUtils.parseToComponent("<green>Ame Preservee : vous avez conserve " + keptLevel + " niveaux d'XP !</green>"));
            }
        }
    }

    private Material getSmeltedMaterial(Material raw) {
        return switch (raw) {
            case RAW_IRON, IRON_ORE, DEEPSLATE_IRON_ORE -> Material.IRON_INGOT;
            case RAW_COPPER, COPPER_ORE, DEEPSLATE_COPPER_ORE -> Material.COPPER_INGOT;
            case RAW_GOLD, GOLD_ORE, DEEPSLATE_GOLD_ORE, NETHER_GOLD_ORE -> Material.GOLD_INGOT;
            default -> null;
        };
    }

    private boolean isEligibleDoubleDrop(Material mat) {
        if (mat == null) return false;
        String name = mat.name();
        return name.startsWith("RAW_") || name.endsWith("_INGOT") || name.endsWith("_LOG")
                || name.endsWith("_WOOD") || mat == Material.DIAMOND || mat == Material.EMERALD
                || mat == Material.COAL || mat == Material.REDSTONE || mat == Material.LAPIS_LAZULI;
    }
}
