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
            meta.setDisplayName("§6§lSpawner de " + type);
            List<String> lore = new ArrayList<>();
            lore.add("§7Type: §f" + type);
            lore.add("§7Stack: §f" + stack);
            
            if (expLvl > 0 || speedLvl > 0 || storageLvl > 0) {
                lore.add("");
                lore.add("§b§lAméliorations :");
                if (expLvl > 0) lore.add(" §8- §7XP: §fNiv." + expLvl);
                if (speedLvl > 0) lore.add(" §8- §7Vitesse: §fNiv." + speedLvl);
                if (storageLvl > 0) lore.add(" §8- §7Stockage: §fNiv." + storageLvl);
            }
            
            lore.add("");
            lore.add("§ePosez ce générateur pour commencer");
            lore.add("§eà accumuler des objets !");
            meta.setLore(lore);

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
            sender.sendMessage("§cVous n'avez pas la permission.");
            return true;
        }

        if (args.length < 3) {
            sender.sendMessage("§cUsage: /spawner give <joueur> <type> [stack]");
            return true;
        }

        if (args[0].equalsIgnoreCase("give")) {
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage("§cJoueur introuvable.");
                return true;
            }

            String type = args[2].toUpperCase();
            if (!module.getSpawnerManager().isValidType(type)) {
                sender.sendMessage("§cCe type de spawner n'existe pas dans la config.");
                return true;
            }

            int stack = 1;
            if (args.length >= 4) {
                try {
                    stack = Integer.parseInt(args[3]);
                } catch (NumberFormatException e) {
                    sender.sendMessage("§cLe stack doit être un nombre.");
                    return true;
                }
            }

            ItemStack spawner = createSpawnerItem(plugin, type, stack);
            target.getInventory().addItem(spawner);
            sender.sendMessage("§aVous avez donné un spawner " + type + " (x" + stack + ") à " + target.getName() + ".");
            target.sendMessage("§aVous avez reçu un spawner " + type + " (x" + stack + ").");
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
