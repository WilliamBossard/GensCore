package fr.gens.core.modules;

import fr.gens.core.CorePlugin;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.plugin.ServicePriority;

import org.bukkit.plugin.ServicePriority;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class EconomyModule implements Module, CommandExecutor, TabCompleter {

    private final CorePlugin plugin;
    private boolean enabled = false;
    private final Map<UUID, Double> balances = new HashMap<>();

    public EconomyModule(CorePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "Economy";
    }

    @Override
    public String getDescription() {
        return "Système d'argent avec /money, /pay, /eco et API Vault.";
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void enable() {
        enabled = true;
        loadBalances();
        
        // Enregistrer l'API Vault si Vault est présent
        if (plugin.getServer().getPluginManager().getPlugin("Vault") != null) {
            plugin.getServer().getServicesManager().register(Economy.class, new GensVaultEconomy(this), plugin, ServicePriority.Highest);
            plugin.getLangManager().sendConsoleMessage("economymodule.log_1");
        }
        
        plugin.getLangManager().sendConsoleMessage("economymodule.log_2");
    }

    @Override
    public void disable() {
        enabled = false;
        saveBalances();
        plugin.getLangManager().sendConsoleMessage("economymodule.log_3");
    }
    
    @Override
    public void registerCommands(fr.gens.core.CorePlugin plugin) {
        org.bukkit.command.PluginCommand ecoCmd = plugin.getCommand("eco");
        if (ecoCmd != null) { ecoCmd.setExecutor(this); ecoCmd.setTabCompleter(this); }

        org.bukkit.command.PluginCommand payCmd = plugin.getCommand("pay");
        if (payCmd != null) { payCmd.setExecutor(this); payCmd.setTabCompleter(this); }

        if (plugin.getCommand("money") != null) plugin.getCommand("money").setExecutor(this);
        if (plugin.getCommand("balance") != null) plugin.getCommand("balance").setExecutor(this);
        if (plugin.getCommand("baltop") != null) plugin.getCommand("baltop").setExecutor(this);
    }

    public double getBalance(UUID uuid) {
        return balances.getOrDefault(uuid, 0.0);
    }
    private void loadBalances() {
        balances.clear();
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT uuid, balance FROM players_economy");
             ResultSet rs = ps.executeQuery()) {
            
            while (rs.next()) {
                balances.put(UUID.fromString(rs.getString("uuid")), rs.getDouble("balance"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void saveBalances() {
        // Sauvegarde de cache vers DB (non utilisé car on sauvegarde en temps réel)
    }

    private void savePlayerBalance(UUID uuid, double balance) {
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO players_economy (uuid, balance) VALUES (?, ?) " +
                     "ON CONFLICT(uuid) DO UPDATE SET balance=excluded.balance")) {
            ps.setString(1, uuid.toString());
            ps.setDouble(2, balance);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void setBalance(UUID uuid, double amount) {
        balances.put(uuid, amount);
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> savePlayerBalance(uuid, amount));
    }

    public void addMoney(UUID uuid, double amount) {
        setBalance(uuid, getBalance(uuid) + amount);
    }

    public void giveMoney(UUID uuid, double amount) {
        setBalance(uuid, getBalance(uuid) + amount);
    }

    public void takeMoney(UUID uuid, double amount) {
        setBalance(uuid, Math.max(0, getBalance(uuid) - amount));
    }

    public void setMoney(UUID uuid, double amount) {
        setBalance(uuid, Math.max(0, amount));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!enabled) {
            sender.sendMessage(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize("<red>Ce module est actuellement désactivé.</red>"));
            return true;
        }

        if (command.getName().equalsIgnoreCase("money") || command.getName().equalsIgnoreCase("balance")) {
            if (args.length == 0) {
                if (!(sender instanceof Player)) return true;
                Player p = (Player) sender;
                plugin.getLangManager().sendMessage(p, "economy.balance", net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.parsed("amount", String.format("%.2f", getBalance(p.getUniqueId()))));
            } else {
                OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
                plugin.getLangManager().sendMessage(sender, "economy.balance_other", 
                    net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.parsed("player", target.getName() != null ? target.getName() : "Inconnu"),
                    net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.parsed("amount", String.format("%.2f", getBalance(target.getUniqueId())))
                );
            }
            return true;
        }

        if (command.getName().equalsIgnoreCase("baltop")) {
            plugin.getLangManager().sendMessage(sender, "economymodule.msg_1");
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                try (Connection conn = plugin.getDatabaseManager().getConnection();
                     PreparedStatement ps = conn.prepareStatement("SELECT uuid, balance FROM players_economy ORDER BY balance DESC LIMIT 10");
                     ResultSet rs = ps.executeQuery()) {
                    
                    int rank = 1;
                    while (rs.next()) {
                        UUID uuid = UUID.fromString(rs.getString("uuid"));
                        double bal = rs.getDouble("balance");
                        OfflinePlayer p = Bukkit.getOfflinePlayer(uuid);
                        String name = p.getName() != null ? p.getName() : "Inconnu";
                        sender.sendMessage("§e" + rank + ". §7" + name + " §8- §6" + String.format("%.2f", bal) + " $");
                        rank++;
                    }
                } catch (SQLException e) {
                    plugin.getLangManager().sendMessage(sender, "economymodule.msg_2");
                    e.printStackTrace();
                }
            });
            return true;
        }

        if (command.getName().equalsIgnoreCase("pay")) {
            if (!(sender instanceof Player)) return true;
            Player p = (Player) sender;
            if (args.length < 2) {
                p.sendMessage(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize("<red>Usage: /pay <joueur> <montant></red>"));
                return true;
            }
            OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
            if (!target.hasPlayedBefore() && !target.isOnline()) {
                plugin.getLangManager().sendMessage(p, "error.player_offline");
                return true;
            }
            try {
                double amount = Double.parseDouble(args[1]);
                if (amount <= 0) {
                    plugin.getLangManager().sendMessage(p, "error.invalid_amount");
                    return true;
                }
                if (getBalance(p.getUniqueId()) < amount) {
                    plugin.getLangManager().sendMessage(p, "economy.not_enough");
                    return true;
                }
                takeMoney(p.getUniqueId(), amount);
                giveMoney(target.getUniqueId(), amount);
                plugin.getLangManager().sendMessage(p, "economy.take", 
                    net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.parsed("amount", String.valueOf(amount)),
                    net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.parsed("player", target.getName() != null ? target.getName() : "Inconnu")
                );
                if (target.isOnline()) {
                    plugin.getLangManager().sendMessage(target.getPlayer(), "economy.received", 
                        net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.parsed("amount", String.valueOf(amount))
                    );
                }
            } catch (NumberFormatException e) {
                plugin.getLangManager().sendMessage(p, "error.invalid_amount");
            }
            return true;
        }

        if (command.getName().equalsIgnoreCase("eco")) {
            if (!sender.hasPermission("genscore.admin")) {
                plugin.getLangManager().sendMessage(sender, "error.no_permission");
                return true;
            }
            if (args.length < 3) {
                sender.sendMessage(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize("<red>Usage: /eco <give|take|set> <joueur> <montant></red>"));
                return true;
            }
            String action = args[0].toLowerCase();
            OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
            String tName = target.getName() != null ? target.getName() : "Inconnu";
            try {
                double amount = Double.parseDouble(args[2]);
                switch (action) {
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
            } catch (NumberFormatException e) {
                plugin.getLangManager().sendMessage(sender, "error.invalid_amount");
            }
            return true;
        }

        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!enabled) return Collections.emptyList();
        
        if (command.getName().equalsIgnoreCase("eco")) {
            if (!sender.hasPermission("genscore.admin")) return Collections.emptyList();
            if (args.length == 1) {
                List<String> list = new ArrayList<>();
                list.add("give"); list.add("take"); list.add("set");
                return list;
            }
            if (args.length == 2) {
                return null; // Noms des joueurs
            }
        }
        
        if (command.getName().equalsIgnoreCase("pay")) {
            if (args.length == 1) return null; // Noms des joueurs
        }
        
        return Collections.emptyList();
    }
}
