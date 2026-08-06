package fr.gens.core.utils;

import fr.gens.core.CorePlugin;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class TeleportUtil {

    public static void teleportWithCooldown(Player player, Location target, String destinationName, String bypassPermission) {
        int cooldownSeconds = CorePlugin.getInstance().getStorageManager().getConfig().getInt("teleport-cooldown", 3);

        if (cooldownSeconds <= 0 || player.hasPermission(bypassPermission) || player.hasPermission("genscore.bypass.cooldown.all")) {
            player.teleport(target);
            player.sendMessage(MiniMessage.miniMessage().deserialize("<green>Téléportation à " + destinationName + " réussie !</green>"));
            return;
        }

        Location startLoc = player.getLocation();
        player.sendMessage(MiniMessage.miniMessage().deserialize("<gold>Téléportation dans " + cooldownSeconds + " secondes. Ne bougez pas !</gold>"));

        new BukkitRunnable() {
            int timeLeft = cooldownSeconds;

            @Override
            public void run() {
                if (!player.isOnline()) {
                    this.cancel();
                    return;
                }

                // Vérifier si le joueur a bougé (plus d'un demi-bloc de tolérance)
                if (player.getLocation().distanceSquared(startLoc) > 0.5) {
                    player.sendActionBar(MiniMessage.miniMessage().deserialize("<red>Téléportation annulée (mouvement détecté).</red>"));
                    player.sendMessage(MiniMessage.miniMessage().deserialize("<red>Téléportation annulée, vous avez bougé !</red>"));
                    this.cancel();
                    return;
                }

                if (timeLeft <= 0) {
                    player.teleport(target);
                    player.sendActionBar(MiniMessage.miniMessage().deserialize("<green>Téléportation réussie !</green>"));
                    this.cancel();
                    return;
                }

                player.sendActionBar(MiniMessage.miniMessage().deserialize("<yellow>Téléportation dans " + timeLeft + "...</yellow>"));
                timeLeft--;
            }
        }.runTaskTimer(CorePlugin.getInstance(), 0L, 20L); // Execute toutes les secondes
    }
}
