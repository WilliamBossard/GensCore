package fr.gens.core.utils;

import fr.gens.core.CorePlugin;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;


public class TeleportUtil {

    public static void teleportWithCooldown(CorePlugin plugin, Player player, Location target, String destinationName, String bypassPermission) {
        int cooldownSeconds = plugin.getStorageManager().getConfig().getInt("teleport-cooldown", 3);

        if (cooldownSeconds <= 0 || player.hasPermission(bypassPermission) || player.hasPermission("genscore.bypass.cooldown.all")) {
            player.teleport(target);
            player.sendMessage(MiniMessage.miniMessage().deserialize("<green>TÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â©lÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â©portation ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â  " + destinationName + " rÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â©ussie !</green>"));
            return;
        }

        Location startLoc = player.getLocation();
        player.sendMessage(MiniMessage.miniMessage().deserialize("<gold>TÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â©lÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â©portation dans " + cooldownSeconds + " secondes. Ne bougez pas !</gold>"));

        new BukkitRunnable() {
            int timeLeft = cooldownSeconds;

            @Override
            public void run() {
                if (!player.isOnline()) {
                    this.cancel();
                    return;
                }

                // Vérifier si le joueur a bougé (plus d'un demi-bloc de tolérance)
                Location pLoc = player.getLocation();
                if (pLoc != null && pLoc.distanceSquared(startLoc) > 0.5) {
                    player.sendActionBar(MiniMessage.miniMessage().deserialize("<red>TÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â©lÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â©portation annulÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â©e (mouvement dÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â©tectÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â©).</red>"));
                    player.sendMessage(MiniMessage.miniMessage().deserialize("<red>TÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â©lÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â©portation annulÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â©e, vous avez bougÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â© !</red>"));
                    this.cancel();
                    return;
                }

                if (timeLeft <= 0) {
                    player.teleport(target);
                    player.sendActionBar(MiniMessage.miniMessage().deserialize("<green>TÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â©lÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â©portation rÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â©ussie !</green>"));
                    this.cancel();
                    return;
                }

                player.sendActionBar(MiniMessage.miniMessage().deserialize("<yellow>TÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â©lÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â©portation dans " + timeLeft + "...</yellow>"));
                timeLeft--;
            }
        }.runTaskTimer(plugin, 0L, 20L); // Execute toutes les secondes
    }
}

