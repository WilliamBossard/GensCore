package fr.gens.core.utils;

import fr.gens.core.CorePlugin;

import org.bukkit.Location;
import org.bukkit.entity.Player;



public class TeleportUtil {

    private static final java.util.Map<java.util.UUID, com.tcoded.folialib.wrapper.task.WrappedTask> activeTeleports = new java.util.concurrent.ConcurrentHashMap<>();

    public static void cancelPendingTeleport(java.util.UUID uuid) {
        com.tcoded.folialib.wrapper.task.WrappedTask task = activeTeleports.remove(uuid);
        if (task != null) {
            task.cancel();
        }
    }

    public static void teleportWithCooldown(CorePlugin plugin, Player player, Location target, String destinationName, String bypassPermission) {
        int cooldownSeconds = plugin.getStorageManager().getConfig().getInt("teleport-cooldown", 3);

        fr.gens.core.modules.perks.SoloPerkModule perkModule = (fr.gens.core.modules.perks.SoloPerkModule) plugin.getModuleManager().getModule("solo_perks");
        if (perkModule != null && perkModule.isEnabled() && perkModule.getManager().hasPerk(player.getUniqueId(), fr.gens.core.modules.perks.SoloPerkType.WARMUP_REDUCTION)) {
            cooldownSeconds = Math.max(1, cooldownSeconds / 2);
        }

        final int finalCooldown = cooldownSeconds;
        plugin.getFoliaLib().getScheduler().runAtEntity(player, (t) -> {
            cancelPendingTeleport(player.getUniqueId());

            if (finalCooldown <= 0 || player.hasPermission(bypassPermission) || player.hasPermission("genscore.bypass.cooldown.all")) {
                player.teleportAsync(target).thenAccept(success -> {
                    if (Boolean.TRUE.equals(success)) {
                        player.sendMessage(fr.gens.core.utils.PlaceholderUtils.parseToComponent("<green>Téléportation à " + destinationName + " réussie !</green>"));
                    } else {
                        player.sendMessage(fr.gens.core.utils.PlaceholderUtils.parseToComponent("<red>La téléportation a échoué.</red>"));
                    }
                });
                return;
            }

            Location startLoc = player.getLocation();
            player.sendMessage(fr.gens.core.utils.PlaceholderUtils.parseToComponent("<gold>Téléportation dans " + finalCooldown + " secondes. Ne bougez pas !</gold>"));

            java.util.concurrent.atomic.AtomicInteger timeLeft = new java.util.concurrent.atomic.AtomicInteger(finalCooldown);

            plugin.getFoliaLib().getScheduler().runAtEntityTimer(player, (wrappedTask) -> {
                activeTeleports.put(player.getUniqueId(), wrappedTask);
                if (!player.isOnline()) {
                    wrappedTask.cancel();
                    activeTeleports.remove(player.getUniqueId());
                    return;
                }

                // Vérifier si le joueur a bougé (plus d'un demi-bloc de tolérance ou changement de monde)
                Location pLoc = player.getLocation();
                if (pLoc == null || startLoc.getWorld() == null || !startLoc.getWorld().equals(pLoc.getWorld()) || pLoc.distanceSquared(startLoc) > 0.5) {
                    player.sendActionBar(fr.gens.core.utils.PlaceholderUtils.parseToComponent("<red>Téléportation annulée (mouvement détecté).</red>"));
                    player.sendMessage(fr.gens.core.utils.PlaceholderUtils.parseToComponent("<red>Téléportation annulée, vous avez bougé !</red>"));
                    wrappedTask.cancel();
                    activeTeleports.remove(player.getUniqueId());
                    return;
                }

                if (timeLeft.get() <= 0) {
                    wrappedTask.cancel();
                    activeTeleports.remove(player.getUniqueId());
                    player.teleportAsync(target).thenAccept(success -> {
                        if (Boolean.TRUE.equals(success)) {
                            player.sendActionBar(fr.gens.core.utils.PlaceholderUtils.parseToComponent("<green>Téléportation réussie !</green>"));
                        } else {
                            player.sendMessage(fr.gens.core.utils.PlaceholderUtils.parseToComponent("<red>La téléportation a échoué.</red>"));
                        }
                    });
                    return;
                }

                player.sendActionBar(fr.gens.core.utils.PlaceholderUtils.parseToComponent("<yellow>Téléportation dans " + timeLeft.get() + "...</yellow>"));
                timeLeft.decrementAndGet();
            }, 0L, 20L); // Execute toutes les secondes
        });
    }
}




