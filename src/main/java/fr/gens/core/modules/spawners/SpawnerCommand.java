package fr.gens.core.modules.spawners;

import fr.gens.core.CorePlugin;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public class SpawnerCommand implements CommandExecutor, TabCompleter {

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
                lore.add("<aqua><bold>Améliorations :");
                if (expLvl > 0) lore.add(" <dark_gray>- <gray>XP: <white>Niv." + expLvl);
                if (speedLvl > 0) lore.add(" <dark_gray>- <gray>Vitesse: <white>Niv." + speedLvl);
                if (storageLvl > 0) lore.add(" <dark_gray>- <gray>Stockage: <white>Niv." + storageLvl);
            }
            
            lore.add("");
            lore.add("<yellow>Posez ce générateur pour commencer");
            lore.add("<yellow>à accumuler des objets !");
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

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("genscore.admin.spawner")) {
            plugin.getLangManager().sendMessage(sender, "spawnercommand.msg_1");
            return true;
        }

        if (args.length < 3) {
            plugin.getLangManager().sendMessage(sender, "spawnercommand.msg_2");
            return true;
        }

        if (args[0].equalsIgnoreCase("give")) {
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                plugin.getLangManager().sendMessage(sender, "spawnercommand.msg_3");
                return true;
            }

            String type = args[2].toUpperCase();
            if (!module.getSpawnerManager().isValidType(type)) {
                plugin.getLangManager().sendMessage(sender, "spawnercommand.msg_4");
                return true;
            }

            int stack = 1;
            if (args.length >= 4) {
                try {
                    stack = Integer.parseInt(args[3]);
                } catch (NumberFormatException e) {
                    plugin.getLangManager().sendMessage(sender, "spawnercommand.msg_5");
                    return true;
                }
            }

            ItemStack spawner = createSpawnerItem(plugin, type, stack);
            target.getInventory().addItem(spawner);
            sender.sendMessage("<green>Vous avez donné un spawner " + type + " (x" + stack + ") à " + target.getName() + ".");
            target.sendMessage("<green>Vous avez reçu un spawner " + type + " (x" + stack + ").");
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (!sender.hasPermission("genscore.admin.spawner")) return completions;

        if (args.length == 1) {
            completions.add("give");
        } else if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                completions.add(p.getName());
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
            for (EntityType type : EntityType.values()) {
                if (type.isSpawnable() && type.isAlive()) {
                    completions.add(type.name());
                }
            }
        }
        return completions;
    }
}
