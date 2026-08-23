package fr.gens.core.modules.spawners;

import fr.gens.core.CorePlugin;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import cloud.commandframework.annotations.Argument;
import cloud.commandframework.annotations.CommandMethod;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;


public class SpawnerCommand {

    private final CorePlugin plugin;
    private final SpawnerModule module;

    public SpawnerCommand(CorePlugin plugin, SpawnerModule module) {
        this.plugin = plugin;
        this.module = module;
    }

    public static ItemStack createSpawnerItem(CorePlugin plugin, String type, int stack) {
        return createSpawnerItem(plugin, type, stack, 0, 0, 0);
    }

    public static ItemStack createSpawnerItem(CorePlugin plugin, String type, int stack, int expLvl, int speedLvl, int storageLvl) {
        ItemStack item = new ItemStack(Material.SPAWNER, 1);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize("<gold><bold>Spawner de " + type));
            List<String> lore = new ArrayList<>();
            lore.add("<gray>Type: <white>" + type);
            lore.add("<gray>Stack: <white>" + stack);
            
            if (expLvl > 0 || speedLvl > 0 || storageLvl > 0) {
                lore.add("");
                lore.add("<aqua><bold>AmÃƒÆ’Ã‚Â©liorations :");
                if (expLvl > 0) lore.add(" <dark_gray>- <gray>XP: <white>Niv." + expLvl);
                if (speedLvl > 0) lore.add(" <dark_gray>- <gray>Vitesse: <white>Niv." + speedLvl);
                if (storageLvl > 0) lore.add(" <dark_gray>- <gray>Stockage: <white>Niv." + storageLvl);
            }
            
            lore.add("");
            lore.add("<yellow>Posez ce gÃƒÆ’Ã‚Â©nÃƒÆ’Ã‚Â©rateur pour commencer");
            lore.add("<yellow>ÃƒÆ’Ã‚Â  accumuler des objets !");
            meta.lore(java.util.Optional.ofNullable(lore).orElse(java.util.Collections.emptyList()).stream().map(s -> net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize((String)s)).collect(java.util.stream.Collectors.toList()));

            NamespacedKey typeKey = new NamespacedKey(plugin, "spawner_type");
            NamespacedKey stackKey = new NamespacedKey(plugin, "spawner_stack");
            NamespacedKey expKey = new NamespacedKey(plugin, "spawner_exp");
            NamespacedKey speedKey = new NamespacedKey(plugin, "spawner_speed");
            NamespacedKey storageKey = new NamespacedKey(plugin, "spawner_storage");

            meta.getPersistentDataContainer().set(typeKey, PersistentDataType.STRING, type);
            meta.getPersistentDataContainer().set(stackKey, PersistentDataType.INTEGER, stack);
            meta.getPersistentDataContainer().set(expKey, PersistentDataType.INTEGER, expLvl);
            meta.getPersistentDataContainer().set(speedKey, PersistentDataType.INTEGER, speedLvl);
            meta.getPersistentDataContainer().set(storageKey, PersistentDataType.INTEGER, storageLvl);

            item.setItemMeta(meta);
        }
        return item;
    }

    @CommandMethod("spawner give <player> <type> [stack]")
    public void executeSpawnerGive(CommandSender sender, @Argument("player") String playerName, @Argument("type") String type, @Argument(value = "stack", defaultValue = "1") int stack) {
        if (!sender.hasPermission("genscore.admin.spawner")) {
            plugin.getLangManager().sendMessage(sender, "spawnercommand.msg_1");
            return;
        }

        Player target = Bukkit.getPlayer(playerName);
        if (target == null) {
            plugin.getLangManager().sendMessage(sender, "spawnercommand.msg_3");
            return;
        }

        type = type.toUpperCase();
        if (!module.getSpawnerManager().isValidType(type)) {
            plugin.getLangManager().sendMessage(sender, "spawnercommand.msg_4");
            return;
        }

        ItemStack spawner = createSpawnerItem(plugin, type, stack);
        target.getInventory().addItem(spawner);
        sender.sendMessage(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize("<green>Vous avez donnÃƒÆ’Ã‚Â© un spawner " + type + " (x" + stack + ") ÃƒÆ’Ã‚Â  " + target.getName() + "."));
        target.sendMessage(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize("<green>Vous avez reÃƒÆ’Ã‚Â§u un spawner " + type + " (x" + stack + ")."));
    }
}

