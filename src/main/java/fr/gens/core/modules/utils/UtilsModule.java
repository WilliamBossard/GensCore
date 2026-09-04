package fr.gens.core.modules.utils;

import fr.gens.core.CorePlugin;
import fr.gens.core.modules.Module;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

import cloud.commandframework.annotations.Argument;
import cloud.commandframework.annotations.CommandMethod;


public class UtilsModule implements Module, Listener {

    private CorePlugin plugin;
    private boolean enabled;

    public UtilsModule(CorePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "utils";
    }

    @Override
    public String getDescription() {
        return "Commandes utilitaires du serveur";
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void initDatabase(fr.gens.core.utils.DatabaseManager dbManager) {
        dbManager.executeStatement("CREATE TABLE IF NOT EXISTS player_transactions_history (id INTEGER PRIMARY KEY AUTOINCREMENT, uuid VARCHAR(36) NOT NULL, type VARCHAR(10) NOT NULL, material VARCHAR(50) NOT NULL, amount INTEGER NOT NULL, price DOUBLE NOT NULL, timestamp BIGINT NOT NULL);");
        dbManager.executeStatement("CREATE TABLE IF NOT EXISTS player_minigame_cooldowns (uuid VARCHAR(36), game_id VARCHAR(50), last_played BIGINT DEFAULT 0, PRIMARY KEY(uuid, game_id));");
        dbManager.executeStatement("CREATE TABLE IF NOT EXISTS player_profiles (uuid VARCHAR(36) PRIMARY KEY, username VARCHAR(16) NOT NULL);");
        dbManager.executeStatement("CREATE INDEX IF NOT EXISTS idx_transactions_uuid ON player_transactions_history(uuid);");
    }

    @Override
    public void enable() {
        this.enabled = true;
        plugin.getLangManager().sendConsoleMessage("utilsmodule.log_1");
    }

    @Override
    public void registerCommands(CorePlugin plugin) {
        if (plugin.getCommandManager() != null && plugin.getCommandManager().getAnnotationParser() != null) {
            plugin.getCommandManager().getAnnotationParser().parse(this);
        }
    }

    @Override
    public void disable() {
        org.bukkit.event.HandlerList.unregisterAll(this);
        this.enabled = false;
        plugin.getLangManager().sendConsoleMessage("utilsmodule.log_2");
    }

    @CommandMethod("anvil")
    public void executeAnvil(org.bukkit.command.CommandSender sender) {
        if (!(sender instanceof org.bukkit.entity.Player)) return;
        org.bukkit.entity.Player p = (org.bukkit.entity.Player) sender;
        if (!enabled) return;
        if (!p.hasPermission("genscore.anvil")) {
            plugin.getLangManager().sendMessage(p, "utilsmodule.msg_1");
            return;
        }
        p.openInventory(org.bukkit.Bukkit.createInventory(p, org.bukkit.event.inventory.InventoryType.ANVIL));
    }

    @CommandMethod("craftingtable")
    public void executeCraftingTable(org.bukkit.command.CommandSender sender) {
        if (!(sender instanceof org.bukkit.entity.Player)) return;
        org.bukkit.entity.Player p = (org.bukkit.entity.Player) sender;
        if (!enabled) return;
        if (!p.hasPermission("genscore.craft")) {
            plugin.getLangManager().sendMessage(p, "utilsmodule.msg_2");
            return;
        }
        p.openInventory(org.bukkit.Bukkit.createInventory(p, org.bukkit.event.inventory.InventoryType.WORKBENCH));
    }

    @CommandMethod("craft")
    public void executeCraft(org.bukkit.command.CommandSender sender) { 
        if (!(sender instanceof org.bukkit.entity.Player)) return;
        org.bukkit.entity.Player p = (org.bukkit.entity.Player) sender;
        executeCraftingTable(p); 
    }

    @CommandMethod("workbench")
    public void executeWorkbench(org.bukkit.command.CommandSender sender) { 
        if (!(sender instanceof org.bukkit.entity.Player)) return;
        org.bukkit.entity.Player p = (org.bukkit.entity.Player) sender;
        executeCraftingTable(p); 
    }

    @CommandMethod("enchanttable")
    public void executeEnchantTable(org.bukkit.command.CommandSender sender) {
        if (!(sender instanceof org.bukkit.entity.Player)) return;
        org.bukkit.entity.Player p = (org.bukkit.entity.Player) sender;
        if (!enabled) return;
        if (!p.hasPermission("genscore.enchant")) {
            plugin.getLangManager().sendMessage(p, "utilsmodule.msg_3");
            return;
        }
        p.openInventory(org.bukkit.Bukkit.createInventory(p, org.bukkit.event.inventory.InventoryType.ENCHANTING));
    }

    @CommandMethod("enchanting")
    public void executeEnchanting(org.bukkit.command.CommandSender sender) { 
        if (!(sender instanceof org.bukkit.entity.Player)) return;
        org.bukkit.entity.Player p = (org.bukkit.entity.Player) sender;
        executeEnchantTable(p); 
    }

    @CommandMethod("ec [target]")
    public void executeEnderChest(org.bukkit.command.CommandSender sender, @Argument(value = "target", defaultValue = "", suggestions = "onlinePlayers", description = "Le joueur ciblé") String targetName) {
        if (!(sender instanceof org.bukkit.entity.Player)) return;
        org.bukkit.entity.Player p = (org.bukkit.entity.Player) sender;
        if (!enabled) return;
        if (!p.hasPermission("genscore.ec")) {
            plugin.getLangManager().sendMessage(p, "utilsmodule.msg_4");
            return;
        }
        if (targetName != null && !targetName.isEmpty()) {
            if (!p.hasPermission("genscore.admin")) {
                plugin.getLangManager().sendMessage(p, "utilsmodule.msg_5");
                return;
            }
            Player target = org.bukkit.Bukkit.getPlayer(targetName);
            if (target == null) {
                plugin.getLangManager().sendMessage(p, "utilsmodule.msg_6");
                return;
            }
            p.openInventory(target.getEnderChest());
            p.sendMessage(fr.gens.core.utils.PlaceholderUtils.parseToComponent("<green>Vous regardez l'enderchest de <yellow>" + target.getName() + "<green>."));
        } else {
            p.openInventory(p.getEnderChest());
        }
    }

    @CommandMethod("enderchest [target]")
    public void executeEnderChestAlias(org.bukkit.command.CommandSender sender, @Argument(value = "target", defaultValue = "", suggestions = "onlinePlayers", description = "Le joueur ciblé") String targetName) { 
        if (!(sender instanceof org.bukkit.entity.Player)) return;
        org.bukkit.entity.Player p = (org.bukkit.entity.Player) sender;
        executeEnderChest(p, targetName); 
    }

    @CommandMethod("feed")
    public void executeFeed(org.bukkit.command.CommandSender sender) {
        if (!(sender instanceof org.bukkit.entity.Player)) return;
        org.bukkit.entity.Player p = (org.bukkit.entity.Player) sender;
        if (!p.hasPermission("genscore.feed")) {
            plugin.getLangManager().sendMessage(p, "utilsmodule.msg_7");
            return;
        }
        p.setFoodLevel(20);
        p.setSaturation(20.0f);
        plugin.getLangManager().sendMessage(p, "utilsmodule.msg_8");
    }
}


