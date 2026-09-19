package fr.gens.core.modules.utils;

import fr.gens.core.CorePlugin;
import fr.gens.core.modules.Module;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

import org.incendo.cloud.annotations.Argument;
import org.incendo.cloud.annotations.Default;
import org.incendo.cloud.annotations.Command;

public class UtilsModule implements Module, Listener {

    private CorePlugin plugin;
    private boolean enabled;
    private final java.util.Map<java.util.UUID, java.util.UUID> inspectingEnderChests = new java.util.concurrent.ConcurrentHashMap<>();
    private final java.util.Map<java.util.UUID, Long> feedPerkCooldowns = new java.util.concurrent.ConcurrentHashMap<>();

    private boolean hasWorkbenchPerk(Player p) {
        fr.gens.core.modules.perks.SoloPerkModule mod = (fr.gens.core.modules.perks.SoloPerkModule) plugin.getModuleManager().getModule("solo_perks");
        return mod != null && mod.isEnabled() && mod.getManager().hasPerk(p.getUniqueId(), fr.gens.core.modules.perks.SoloPerkType.PORTABLE_WORKBENCH);
    }

    private boolean hasFeedPerk(Player p) {
        fr.gens.core.modules.perks.SoloPerkModule mod = (fr.gens.core.modules.perks.SoloPerkModule) plugin.getModuleManager().getModule("solo_perks");
        return mod != null && mod.isEnabled() && mod.getManager().hasPerk(p.getUniqueId(), fr.gens.core.modules.perks.SoloPerkType.FEED_ACCESS);
    }

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
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
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
        inspectingEnderChests.clear();
        this.enabled = false;
        plugin.getLangManager().sendConsoleMessage("utilsmodule.log_2");
    }

    @Command("anvil")
    @SuppressWarnings("deprecation")
    public void executeAnvil(org.bukkit.command.CommandSender sender) {
        if (!(sender instanceof org.bukkit.entity.Player)) return;
        org.bukkit.entity.Player p = (org.bukkit.entity.Player) sender;
        if (!enabled) return;
        if (!p.hasPermission("genscore.anvil")) {
            plugin.getLangManager().sendMessage(p, "utilsmodule.msg_1");
            return;
        }
        plugin.getFoliaLib().getScheduler().runAtEntity(p, task -> p.openAnvil(p.getLocation(), true));
    }

    private void executeCraftingTable(Player p) {
        if (!enabled) return;
        if (!p.hasPermission("genscore.craft") && !hasWorkbenchPerk(p)) {
            p.sendMessage(fr.gens.core.utils.PlaceholderUtils.parseToComponent("<red>Vous n'avez pas la permission ni la maîtrise Établi Portatif.</red>"));
            return;
        }
        plugin.getFoliaLib().getScheduler().runAtEntity(p, task -> p.openWorkbench(p.getLocation(), true));
    }

    @Command("craft")
    public void executeCraft(org.bukkit.command.CommandSender sender) {
        if (!(sender instanceof org.bukkit.entity.Player)) return;
        org.bukkit.entity.Player p = (org.bukkit.entity.Player) sender;
        executeCraftingTable(p);
    }

    @Command("workbench")
    public void executeWorkbench(org.bukkit.command.CommandSender sender) {
        if (!(sender instanceof org.bukkit.entity.Player)) return;
        org.bukkit.entity.Player p = (org.bukkit.entity.Player) sender;
        executeCraftingTable(p);
    }

    @Command("enchanttable")
    @SuppressWarnings("deprecation")
    public void executeEnchantTable(org.bukkit.command.CommandSender sender) {
        if (!(sender instanceof org.bukkit.entity.Player)) return;
        org.bukkit.entity.Player p = (org.bukkit.entity.Player) sender;
        if (!enabled) return;
        if (!p.hasPermission("genscore.enchant")) {
            plugin.getLangManager().sendMessage(p, "utilsmodule.msg_3");
            return;
        }
        plugin.getFoliaLib().getScheduler().runAtEntity(p, task -> p.openEnchanting(p.getLocation(), true));
    }

    @Command("enchanting")
    public void executeEnchanting(org.bukkit.command.CommandSender sender) { 
        if (!(sender instanceof org.bukkit.entity.Player)) return;
        org.bukkit.entity.Player p = (org.bukkit.entity.Player) sender;
        executeEnchantTable(p); 
    }

    @Command("ec [target]")
    public void executeEnderChest(org.bukkit.command.CommandSender sender, @Argument(value = "target", suggestions = "onlinePlayers", description = "Le joueur ciblé") @Default(" ") String targetName) {
        if (!(sender instanceof org.bukkit.entity.Player)) return;
        org.bukkit.entity.Player p = (org.bukkit.entity.Player) sender;
        if (!enabled) return;

        boolean isSelf = targetName == null || targetName.trim().isEmpty() || targetName.equalsIgnoreCase(p.getName());

        if (isSelf) {
            if (!p.hasPermission("genscore.ec")) {
                plugin.getLangManager().sendMessage(p, "utilsmodule.msg_5");
                return;
            }
        } else {
            if (!p.hasPermission("genscore.openinv")) {
                plugin.getLangManager().sendMessage(p, "utilsmodule.msg_6");
                return;
            }
        }

        if (!isSelf) {
            org.bukkit.entity.Player target = org.bukkit.Bukkit.getPlayerExact(targetName);
            if (target == null) {
                plugin.getLangManager().sendMessage(p, "error.player_not_found");
                return;
            }
            plugin.getFoliaLib().getScheduler().runAtEntity(target, tTarget -> {
                org.bukkit.inventory.ItemStack[] contents = target.getEnderChest().getContents();
                org.bukkit.inventory.ItemStack[] snapshot = new org.bukkit.inventory.ItemStack[contents.length];
                for (int i = 0; i < contents.length; i++) {
                    snapshot[i] = contents[i] != null ? contents[i].clone() : null;
                }
                plugin.getFoliaLib().getScheduler().runAtEntity(p, tP -> {
                    org.bukkit.inventory.Inventory mirror = org.bukkit.Bukkit.createInventory(p, 27, fr.gens.core.utils.PlaceholderUtils.parseToComponent("<green>EnderChest: <yellow>" + target.getName()));
                    mirror.setContents(snapshot);
                    inspectingEnderChests.put(p.getUniqueId(), target.getUniqueId());
                    p.openInventory(mirror);
                    p.sendMessage(fr.gens.core.utils.PlaceholderUtils.parseToComponent("<green>Vous regardez l'enderchest de <yellow>" + target.getName() + "<green>."));
                });
            });
        } else {
            plugin.getFoliaLib().getScheduler().runAtEntity(p, task -> p.openInventory(p.getEnderChest()));
        }
    }

    @org.bukkit.event.EventHandler
    public void onInventoryClose(org.bukkit.event.inventory.InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player p)) return;
        java.util.UUID targetUuid = inspectingEnderChests.remove(p.getUniqueId());
        if (targetUuid == null) return;

        Player target = org.bukkit.Bukkit.getPlayer(targetUuid);
        if (target == null || !target.isOnline()) return;

        org.bukkit.inventory.ItemStack[] updated = event.getInventory().getContents();
        org.bukkit.inventory.ItemStack[] copy = new org.bukkit.inventory.ItemStack[updated.length];
        for (int i = 0; i < updated.length; i++) {
            copy[i] = updated[i] != null ? updated[i].clone() : null;
        }
        plugin.getFoliaLib().getScheduler().runAtEntity(target, t -> {
            if (target.isOnline()) {
                target.getEnderChest().setContents(copy);
            }
        });
    }

    @Command("enderchest [target]")
    public void executeEnderChestAlias(org.bukkit.command.CommandSender sender, @Argument(value = "target", suggestions = "onlinePlayers", description = "Le joueur ciblé") @Default(" ") String targetName) { 
        if (!(sender instanceof org.bukkit.entity.Player)) return;
        org.bukkit.entity.Player p = (org.bukkit.entity.Player) sender;
        executeEnderChest(p, targetName); 
    }

    @Command("feed")
    public void executeFeed(org.bukkit.command.CommandSender sender) {
        if (!(sender instanceof org.bukkit.entity.Player)) return;
        org.bukkit.entity.Player p = (org.bukkit.entity.Player) sender;

        boolean hasPerm = p.hasPermission("genscore.feed");
        boolean hasPerk = hasFeedPerk(p);

        if (!hasPerm && !hasPerk) {
            plugin.getLangManager().sendMessage(p, "utilsmodule.msg_7");
            return;
        }

        if (!hasPerm && hasPerk) {
            long now = System.currentTimeMillis();
            long last = feedPerkCooldowns.getOrDefault(p.getUniqueId(), 0L);
            long cd = 15 * 60 * 1000L; // 15 minutes
            if (now - last < cd) {
                long remainingSec = (cd - (now - last)) / 1000L;
                p.sendMessage(fr.gens.core.utils.PlaceholderUtils.parseToComponent("<red>Festin Infini : disponible dans " + (remainingSec / 60) + "m " + (remainingSec % 60) + "s.</red>"));
                return;
            }
            feedPerkCooldowns.put(p.getUniqueId(), now);
        }

        plugin.getFoliaLib().getScheduler().runAtEntity(p, task -> {
            p.setFoodLevel(20);
            p.setSaturation(20.0f);
            plugin.getLangManager().sendMessage(p, "utilsmodule.msg_8");
        });
    }
}
