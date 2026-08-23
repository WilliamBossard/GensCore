package fr.gens.core.modules;

import cloud.commandframework.annotations.Argument;
import cloud.commandframework.annotations.CommandMethod;
import cloud.commandframework.annotations.CommandPermission;
import fr.gens.core.CorePlugin;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.ServicePriority;


import java.util.Map;
import java.util.UUID;


public class EconomyModule implements Module, Listener {

    private final CorePlugin plugin;
    private boolean enabled = false;
    private final Map<UUID, Double> balances = new java.util.concurrent.ConcurrentHashMap<>();
    
    // Cache LRU pour les joueurs hors-ligne (thread-safe)
    private final Map<UUID, Double> offlineCache = java.util.Collections.synchronizedMap(new java.util.LinkedHashMap<UUID, Double>(100, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<UUID, Double> eldest) {
            return size() > 100;
        }
    });
    
    private fr.gens.core.database.EconomyDAO economyDAO;

    public EconomyModule(CorePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "Economy";
    }

    @Override
    public String getDescription() {
        return "Syst\u00e8me d'argent avec /money, /pay, /eco et API Vault.";
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    public fr.gens.core.database.EconomyDAO getEconomyDAO() {
        return economyDAO;
    }

    public double getTotalMoney() {
        return economyDAO.getTotalMoney();
    }

    @Override
    public void initDatabase(fr.gens.core.utils.DatabaseManager dbManager) {
        dbManager.executeStatement("CREATE TABLE IF NOT EXISTS players_economy (" +
                "uuid VARCHAR(36) PRIMARY KEY, " +
                "balance DOUBLE NOT NULL DEFAULT 0.0" +
                ");");
        dbManager.executeStatement("CREATE INDEX IF NOT EXISTS idx_economy_balance ON players_economy(balance DESC);");
    }

    @Override
    public void enable() {
        enabled = true;
        
        this.economyDAO = new fr.gens.core.database.EconomyDAO(plugin);
        this.economyDAO.initDatabase();
        
        loadOnlineBalances();
        Bukkit.getPluginManager().registerEvents(this, plugin);
        
        if (plugin.getServer().getPluginManager().getPlugin("Vault") != null) {
            plugin.getServer().getServicesManager().register(Economy.class, new GensVaultEconomy(this), plugin, ServicePriority.Highest);
            plugin.getLangManager().sendConsoleMessage("economymodule.log_1");
        }
        
        plugin.getLangManager().sendConsoleMessage("economymodule.log_2");
    }

    @Override
    public void disable() {
        enabled = false;
        org.bukkit.event.HandlerList.unregisterAll(this);
        saveBalances();
        plugin.getLangManager().sendConsoleMessage("economymodule.log_3");
    }
    
    @Override
    public void registerCommands(fr.gens.core.CorePlugin plugin) {
        if (plugin.getCommandManager() != null && plugin.getCommandManager().getAnnotationParser() != null) {
            plugin.getCommandManager().getAnnotationParser().parse(this);
        }
    }

    public double getBalance(UUID uuid) {
        if (balances.containsKey(uuid)) {
            return balances.get(uuid);
        }
        if (offlineCache.containsKey(uuid)) {
            return offlineCache.get(uuid);
        }
        
        if (Bukkit.isPrimaryThread()) {
            plugin.getLogger().warning("[EconomyModule] Requete SQL synchrone (Vault) declenchee pour " + uuid);
        }
        
        double bal = this.economyDAO.getBalance(uuid);
        offlineCache.put(uuid, bal);
        return bal;
    }
    
    private void loadOnlineBalances() {
        balances.clear();
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p == null) continue;
            balances.put(p.getUniqueId(), this.economyDAO.getBalance(p.getUniqueId()));
        }
    }
    
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) {
        if (enabled) {
            plugin.getFoliaLib().getImpl().runAsync((wrappedTask) -> {
                double bal = this.economyDAO.getBalance(e.getPlayer().getUniqueId());
                plugin.getFoliaLib().getImpl().runNextTick((t2) -> balances.put(e.getPlayer().getUniqueId(), bal));
            });
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent e) {
        if (enabled) {
            balances.remove(e.getPlayer().getUniqueId());
        }
    }

    private void saveBalances() {}

    private void savePlayerBalance(UUID uuid, double balance) {
        this.economyDAO.savePlayerBalance(uuid, balance);
    }

    public void setBalance(UUID uuid, double amount) {
        if (balances.containsKey(uuid)) {
            balances.put(uuid, amount);
        } else {
            offlineCache.put(uuid, amount);
        }
        plugin.getFoliaLib().getImpl().runAsync((wrappedTask) -> savePlayerBalance(uuid, amount));
    }

    public void addMoney(UUID uuid, double amount) {
        if (balances.containsKey(uuid)) {
            balances.compute(uuid, (k, current) -> {
                double newBal = (current == null ? getBalance(uuid) : current) + amount;
                plugin.getFoliaLib().getImpl().runAsync((wrappedTask) -> savePlayerBalance(uuid, newBal));
                return newBal;
            });
        } else {
            synchronized (offlineCache) {
                double current = getBalance(uuid);
                offlineCache.put(uuid, current + amount);
                plugin.getFoliaLib().getImpl().runAsync((wrappedTask) -> savePlayerBalance(uuid, current + amount));
            }
        }
    }

    public void giveMoney(UUID uuid, double amount) {
        addMoney(uuid, amount);
    }

    public void takeMoney(UUID uuid, double amount) {
        takeMoneyAtomic(uuid, amount);
    }
    
    public boolean takeMoneyAtomic(UUID uuid, double amount) {
        if (balances.containsKey(uuid)) {
            java.util.concurrent.atomic.AtomicBoolean success = new java.util.concurrent.atomic.AtomicBoolean(false);
            balances.compute(uuid, (k, current) -> {
                double bal = (current == null ? getBalance(uuid) : current);
                if (bal >= amount) {
                    success.set(true);
                    double newBal = bal - amount;
                    plugin.getFoliaLib().getImpl().runAsync((wrappedTask) -> savePlayerBalance(uuid, newBal));
                    return newBal;
                }
                return bal;
            });
            return success.get();
        } else {
            synchronized (offlineCache) {
                double bal = getBalance(uuid);
                if (bal >= amount) {
                    double newBal = bal - amount;
                    offlineCache.put(uuid, newBal);
                    plugin.getFoliaLib().getImpl().runAsync((wrappedTask) -> savePlayerBalance(uuid, newBal));
                    return true;
                }
                return false;
            }
        }
    }

    public void setMoney(UUID uuid, double amount) {
        setBalance(uuid, Math.max(0, amount));
    }

    @CommandMethod("money|balance [target]")
    public void executeMoney(CommandSender sender, @Argument(value = "target", defaultValue = "") String targetName) {
        if (!enabled) {
            sender.sendMessage(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize("<red>Ce module est actuellement d\u00e9sactiv\u00e9.</red>"));
            return;
        }
        plugin.getFoliaLib().getImpl().runAsync((wrappedTask) -> {
            if (targetName.isEmpty()) {
                if (!(sender instanceof Player)) return;
                Player p = (Player) sender;
                plugin.getLangManager().sendMessage(p, "economy.balance", net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.parsed("amount", String.format("%.2f", getBalance(p.getUniqueId()))));
            } else {
                OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
                plugin.getLangManager().sendMessage(sender, "economy.balance_other", 
                    net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.parsed("player", target.getName() != null ? target.getName() : "Inconnu"),
                    net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.parsed("amount", String.format("%.2f", getBalance(target.getUniqueId())))
                );
            }
        });
    }

    @CommandMethod("baltop")
    public void executeBaltop(CommandSender sender) {
        if (!enabled) return;
        plugin.getLangManager().sendMessage(sender, "economymodule.msg_1");
        plugin.getFoliaLib().getImpl().runAsync((wrappedTask) -> {
            try {
                Map<UUID, Double> top = this.economyDAO.getTopBalances(10);
                int rank = 1;
                for (Map.Entry<UUID, Double> entry : top.entrySet()) {
                    UUID uuid = entry.getKey();
                    double bal = entry.getValue();
                    OfflinePlayer p = Bukkit.getOfflinePlayer(uuid);
                    String name = p.getName() != null ? p.getName() : "Inconnu";
                    sender.sendMessage(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize("<yellow>" + rank + ". <gray>" + name + " <dark_gray>- <gold>" + String.format("%.2f", bal) + " $"));
                    rank++;
                }
            } catch (Exception e) {
                plugin.getLangManager().sendMessage(sender, "economymodule.msg_2");
                e.printStackTrace();
            }
        });
    }

    @CommandMethod("pay <target> <amount>")
    public void executePay(Player p, @Argument("target") String targetName, @Argument("amount") double amount) {
        if (!enabled) return;
        plugin.getFoliaLib().getImpl().runAsync((wrappedTask) -> {
            OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
            if (!target.hasPlayedBefore() && !target.isOnline()) {
                plugin.getLangManager().sendMessage(p, "error.player_offline");
                return;
            }
            if (amount <= 0) {
                plugin.getLangManager().sendMessage(p, "error.invalid_amount");
                return;
            }
            if (!takeMoneyAtomic(p.getUniqueId(), amount)) {
                plugin.getLangManager().sendMessage(p, "economy.not_enough");
                return;
            }
            giveMoney(target.getUniqueId(), amount);
            plugin.getLangManager().sendMessage(p, "economy.take", 
                net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.parsed("amount", String.valueOf(amount)),
                net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.parsed("player", target.getName() != null ? target.getName() : "Inconnu")
            );
            if (target.isOnline() && target.getPlayer() != null) {
                plugin.getLangManager().sendMessage(target.getPlayer(), "economy.received", 
                    net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.parsed("amount", String.valueOf(amount))
                );
            }
        });
    }

    @CommandMethod("eco <action> <target> <amount>")
    @CommandPermission("genscore.admin")
    public void executeEco(CommandSender sender, @Argument("action") String action, @Argument("target") String targetName, @Argument("amount") double amount) {
        if (!enabled) return;
        plugin.getFoliaLib().getImpl().runAsync((wrappedTask) -> {
            OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
            String tName = target.getName() != null ? target.getName() : "Inconnu";
            
            switch (action.toLowerCase()) {
                case "give":
                    giveMoney(target.getUniqueId(), amount);
                    plugin.getLangManager().sendMessage(sender, "economy.add", 
                        net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.parsed("amount", String.valueOf(amount)),
                        net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.parsed("player", tName)
                    );
                    break;
                case "take":
                    takeMoney(target.getUniqueId(), amount);
                    plugin.getLangManager().sendMessage(sender, "economy.take", 
                        net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.parsed("amount", String.valueOf(amount)),
                        net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.parsed("player", tName)
                    );
                    break;
                case "set":
                    setMoney(target.getUniqueId(), amount);
                    plugin.getLangManager().sendMessage(sender, "economy.set", 
                        net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.parsed("amount", String.valueOf(amount)),
                        net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.parsed("player", tName)
                    );
                    break;
                default:
                    sender.sendMessage(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize("<red>Action inconnue. Utilisez give, take ou set.</red>"));
            }
        });
    }
}
