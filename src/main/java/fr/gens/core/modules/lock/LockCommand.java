package fr.gens.core.modules.lock;

import fr.gens.core.CorePlugin;
import org.bukkit.entity.Player;
import cloud.commandframework.annotations.Argument;
import cloud.commandframework.annotations.CommandMethod;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.UUID;


public class LockCommand {
    private final CorePlugin plugin;
    private final LockModule lockModule;
    // Map pour savoir qui a tapÃƒÆ’Ã‚Â© quelle commande avant de cliquer sur un bloc
    public static final Map<UUID, String> pendingActions = new ConcurrentHashMap<>();

    public LockCommand(CorePlugin plugin, LockModule lockModule) {
        this.plugin = plugin;
        this.lockModule = lockModule;
    }

    @CommandMethod("lock [action]")
    public void executeLock(Player player, @Argument(value = "action", defaultValue = "") String action) {
        if (!lockModule.isEnabled()) {
            player.sendMessage(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize("<red>Ce module est actuellement désactivé.</red>"));
            return;
        }

        if (action.isEmpty()) {
            plugin.getLangManager().sendMessage(player, "lockcommand.msg_1");
            pendingActions.put(player.getUniqueId(), "private");
            return;
        }

        String sub = action.toLowerCase();
        
        if (sub.equals("private")) {
            plugin.getLangManager().sendMessage(player, "lockcommand.msg_2");
            pendingActions.put(player.getUniqueId(), "private");
            return;
        }

        if (sub.equals("unlock")) {
            plugin.getLangManager().sendMessage(player, "lockcommand.msg_3");
            pendingActions.put(player.getUniqueId(), "unlock");
            return;
        }

        if (sub.equals("guild")) {
            plugin.getLangManager().sendMessage(player, "lockcommand.msg_4");
            pendingActions.put(player.getUniqueId(), "guild");
            return;
        }

        plugin.getLangManager().sendMessage(player, "lockcommand.msg_5");
    }
}

