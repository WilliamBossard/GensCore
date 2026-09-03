package fr.gens.core.utils;

import fr.gens.core.CorePlugin;

import org.bukkit.Location;
import org.bukkit.entity.Player;



public class TeleportUtil {

    public static void teleportWithCooldown(CorePlugin plugin, Player player, Location target, String destinationName, String bypassPermission) {
        int cooldownSeconds = plugin.getStorageManager().getConfig().getInt("teleport-cooldown", 3);

        plugin.getFoliaLib().getScheduler().runAtEntity(player, (t) -> {
            if (cooldownSeconds <= 0 || player.hasPermission(bypassPermission) || player.hasPermission("genscore.bypass.cooldown.all")) {
                player.teleportAsync(target);
                player.sendMessage(fr.gens.core.utils.PlaceholderUtils.parseToComponent("<green>Téléportation à " + destinationName + " réussie !</green>"));
                return;
            }

            Location startLoc = player.getLocation();
            player.sendMessage(fr.gens.core.utils.PlaceholderUtils.parseToComponent("<gold>Téléportation dans " + cooldownSeconds + " secondes. Ne bougez pas !</gold>"));

            java.util.concurrent.atomic.AtomicInteger timeLeft = new java.util.concurrent.atomic.AtomicInteger(cooldownSeconds);

            plugin.getFoliaLib().getScheduler().runAtEntityTimer(player, (wrappedTask) -> {
                if (!player.isOnline()) {
                    wrappedTask.cancel();
                    return;
                }

                // Vérifier si le joueur a bougé (plus d'un demi-bloc de tolérance)
                Location pLoc = player.getLocation();
                if (pLoc != null && pLoc.distanceSquared(startLoc) > 0.5) {
                    player.sendActionBar(fr.gens.core.utils.PlaceholderUtils.parseToComponent("<red>Téléportation annulée (mouvement détecté).</red>"));
                    player.sendMessage(fr.gens.core.utils.PlaceholderUtils.parseToComponent("<red>Téléportation annulée, vous avez bougé !</red>"));
                    wrappedTask.cancel();
                    return;
                }

                if (timeLeft.get() <= 0) {
                    player.teleportAsync(target);
                    player.sendActionBar(fr.gens.core.utils.PlaceholderUtils.parseToComponent("<green>Téléportation réussie !</green>"));
                    wrappedTask.cancel();
                    return;
                }

                player.sendActionBar(fr.gens.core.utils.PlaceholderUtils.parseToComponent("<yellow>Téléportation dans " + timeLeft.get() + "...</yellow>"));
                timeLeft.decrementAndGet();
            }, 0L, 20L); // Execute toutes les secondes
        });
    }
}




