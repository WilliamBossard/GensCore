package fr.gens.core.utils;

import fr.gens.core.CorePlugin;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ActionBarManager {

    private final CorePlugin plugin;
    // Map of UUID -> Map of Module ID -> Action Bar Data
    private final Map<UUID, Map<String, ActionBarMessage>> messages = new ConcurrentHashMap<>();
    private BukkitTask task;

    public ActionBarManager(CorePlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        task = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::tick, 0L, 10L); // every 0.5 sec
    }

    public void stop() {
        if (task != null) {
            task.cancel();
        }
        messages.clear();
    }

    /**
     * Send or update a message in the action bar.
     * @param player The player
     * @param id The id of the module (e.g. "jobs", "quests")
     * @param message The component message
     * @param durationTicks The duration to keep it (in ticks, e.g., 60 for 3 seconds)
     */
    public void sendMessage(Player player, String id, Component message, int durationTicks) {
        UUID uuid = player.getUniqueId();
        messages.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>()).put(id, new ActionBarMessage(message, System.currentTimeMillis() + (durationTicks * 50L)));
        display(player);
    }

    private void display(Player player) {
        UUID uuid = player.getUniqueId();
        Map<String, ActionBarMessage> playerMessages = messages.get(uuid);
        if (playerMessages == null || playerMessages.isEmpty()) return;

        long now = System.currentTimeMillis();
        Component combined = null;

        for (ActionBarMessage msg : playerMessages.values()) {
            if (msg.getExpiresAt() > now) {
                if (combined == null) {
                    combined = msg.getComponent();
                } else {
                    combined = combined.append(Component.text(" §8| ")).append(msg.getComponent());
                }
            }
        }

        if (combined != null) {
            player.sendActionBar(combined);
        }
    }

    private void tick() {
        long now = System.currentTimeMillis();
        for (Iterator<Map.Entry<UUID, Map<String, ActionBarMessage>>> it = messages.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<UUID, Map<String, ActionBarMessage>> entry = it.next();
            UUID uuid = entry.getKey();
            Map<String, ActionBarMessage> playerMessages = entry.getValue();

            boolean changed = false;
            for (Iterator<Map.Entry<String, ActionBarMessage>> msgIt = playerMessages.entrySet().iterator(); msgIt.hasNext(); ) {
                Map.Entry<String, ActionBarMessage> msgEntry = msgIt.next();
                if (msgEntry.getValue().getExpiresAt() <= now) {
                    msgIt.remove();
                    changed = true;
                }
            }

            if (playerMessages.isEmpty()) {
                it.remove();
            } else if (changed) {
                Player p = Bukkit.getPlayer(uuid);
                if (p != null && p.isOnline()) {
                    display(p);
                }
            }
        }
    }

    private static class ActionBarMessage {
        private final Component component;
        private final long expiresAt;

        public ActionBarMessage(Component component, long expiresAt) {
            this.component = component;
            this.expiresAt = expiresAt;
        }

        public Component getComponent() {
            return component;
        }

        public long getExpiresAt() {
            return expiresAt;
        }
    }
}
