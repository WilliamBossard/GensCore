package fr.gens.core.database;

import fr.gens.core.CorePlugin;
import org.bukkit.entity.Player;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.UUID;

public class RewardDAO {

    private final CorePlugin plugin;

    public RewardDAO(CorePlugin plugin) {
        this.plugin = plugin;
    }

    public void addPendingReward(UUID uuid, double amount, String itemData) {
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement stmt = conn.prepareStatement("INSERT INTO genscore_pending_rewards (uuid, amount, item_data) VALUES (?, ?, ?)")) {
            stmt.setString(1, uuid.toString());
            stmt.setDouble(2, amount);
            stmt.setString(3, itemData);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void processPendingRewards(Player player) {
        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            boolean hasRewards = false;
            try (PreparedStatement stmt = conn.prepareStatement("SELECT * FROM genscore_pending_rewards WHERE uuid = ?")) {
                stmt.setString(1, player.getUniqueId().toString());
                ResultSet rs = stmt.executeQuery();
                while (rs.next()) {
                    hasRewards = true;
                    double amount = rs.getDouble("amount");
                    String itemData = rs.getString("item_data");
                    
                    if (amount > 0) {
                        fr.gens.core.modules.EconomyModule eco = (fr.gens.core.modules.EconomyModule) plugin.getModuleManager().getModule("economy");
                        if (eco != null && eco.isEnabled()) {
                            eco.addMoney(player.getUniqueId(), amount);
                            plugin.getLangManager().sendMessage(player, "economy.pending_reward", net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.parsed("amount", String.valueOf(amount)));
                        }
                    }
                    if (itemData != null && !itemData.isEmpty()) {
                        String[] parts = itemData.split(":");
                        if (parts.length == 2) {
                            try {
                                Material mat = Material.valueOf(parts[0]);
                                int count = Integer.parseInt(parts[1]);
                                ItemStack item = new ItemStack(mat, count);
                                
                                HashMap<Integer, ItemStack> excess = player.getInventory().addItem(item);
                                for (ItemStack drop : excess.values()) {
                                    player.getWorld().dropItemNaturally(player.getLocation(), drop);
                                }
                                plugin.getLangManager().sendMessage(player, "guild.reward_received");
                            } catch (Exception e) {
                                plugin.getLangManager().sendMessage(player, "error.invalid_reward");
                            }
                        }
                    }
                }
            }
            if (hasRewards) {
                try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM genscore_pending_rewards WHERE uuid = ?")) {
                    stmt.setString(1, player.getUniqueId().toString());
                    stmt.executeUpdate();
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
