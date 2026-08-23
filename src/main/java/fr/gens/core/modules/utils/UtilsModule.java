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
    public void executeAnvil(Player p) {
        if (!enabled) return;
        if (!p.hasPermission("genscore.anvil")) {
            plugin.getLangManager().sendMessage(p, "utilsmodule.msg_1");
            return;
        }
        p.openInventory(org.bukkit.Bukkit.createInventory(p, org.bukkit.event.inventory.InventoryType.ANVIL));
    }

    @CommandMethod("craftingtable")
    public void executeCraftingTable(Player p) {
        if (!enabled) return;
        if (!p.hasPermission("genscore.craft")) {
            plugin.getLangManager().sendMessage(p, "utilsmodule.msg_2");
            return;
        }
        p.openInventory(org.bukkit.Bukkit.createInventory(p, org.bukkit.event.inventory.InventoryType.WORKBENCH));
    }

    @CommandMethod("craft")
    public void executeCraft(Player p) { executeCraftingTable(p); }

    @CommandMethod("workbench")
    public void executeWorkbench(Player p) { executeCraftingTable(p); }

    @CommandMethod("enchanttable")
    public void executeEnchantTable(Player p) {
        if (!enabled) return;
        if (!p.hasPermission("genscore.enchant")) {
            plugin.getLangManager().sendMessage(p, "utilsmodule.msg_3");
            return;
        }
        p.openInventory(org.bukkit.Bukkit.createInventory(p, org.bukkit.event.inventory.InventoryType.ENCHANTING));
    }

    @CommandMethod("enchanting")
    public void executeEnchanting(Player p) { executeEnchantTable(p); }

    @CommandMethod("ec [target]")
    public void executeEnderChest(Player p, @Argument(value = "target", defaultValue = "") String targetName) {
        if (!enabled) return;
        if (!p.hasPermission("genscore.ec")) {
            plugin.getLangManager().sendMessage(p, "utilsmodule.msg_4");
            return;
        }
        if (!targetName.isEmpty()) {
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
            p.sendMessage(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize("<green>Vous regardez l'enderchest de <yellow>" + target.getName() + "<green>."));
        } else {
            p.openInventory(p.getEnderChest());
        }
    }

    @CommandMethod("enderchest [target]")
    public void executeEnderChestAlias(Player p, @Argument(value = "target", defaultValue = "") String targetName) { executeEnderChest(p, targetName); }

    @CommandMethod("feed")
    public void executeFeed(Player p) {
        if (!enabled) return;
        if (!p.hasPermission("genscore.feed")) {
            plugin.getLangManager().sendMessage(p, "utilsmodule.msg_7");
            return;
        }
        p.setFoodLevel(20);
        p.setSaturation(20.0f);
        plugin.getLangManager().sendMessage(p, "utilsmodule.msg_8");
    }
}


