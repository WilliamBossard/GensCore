package fr.gens.core.commands;

import fr.gens.core.CorePlugin;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.NamespacedKey;
import org.bukkit.command.TabCompleter;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class WebCommand implements CommandExecutor, Listener, TabCompleter {

    private final CorePlugin plugin;
    private final NamespacedKey rewardKey;

    public WebCommand(CorePlugin plugin) {
        this.plugin = plugin;
        this.rewardKey = new NamespacedKey(plugin, "web_reward_id");
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) return true;
        Player player = (Player) sender;

        if (args.length == 0) {
            plugin.getLangManager().sendMessage(player, "webcommand.msg_1");
            return true;
        }

        if (args[0].equalsIgnoreCase("deposit")) {
            ItemStack item = player.getInventory().getItemInMainHand();
            if (item == null || item.getType() == Material.AIR) {
                plugin.getLangManager().sendMessage(player, "webcommand.msg_2");
                return true;
            }

            String base64 = plugin.getStorageManager().itemStackToBase64(item);
            if (base64 == null) {
                plugin.getLangManager().sendMessage(player, "webcommand.msg_3");
                return true;
            }

            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                try (Connection conn = plugin.getDatabaseManager().getConnection();
                     PreparedStatement pstmt = conn.prepareStatement("INSERT INTO player_web_bets (uuid, material, amount, base64_data) VALUES (?, ?, ?, ?)")) {
                    pstmt.setString(1, player.getUniqueId().toString());
                    pstmt.setString(2, item.getType().name());
                    pstmt.setInt(3, item.getAmount());
                    pstmt.setString(4, base64);
                    pstmt.executeUpdate();

                    Bukkit.getScheduler().runTask(plugin, () -> {
                        player.getInventory().setItemInMainHand(null);
                        plugin.getLangManager().sendMessage(player, "webcommand.msg_4");
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    plugin.getLangManager().sendMessage(player, "webcommand.msg_5");
                }
            });

        } else if (args[0].equalsIgnoreCase("withdraw")) {
            // Ouvre le GUI des rewards
            openWithdrawGUI(player);
        } else {
            plugin.getLangManager().sendMessage(player, "webcommand.msg_6");
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            if ("deposit".startsWith(args[0].toLowerCase())) completions.add("deposit");
            if ("withdraw".startsWith(args[0].toLowerCase())) completions.add("withdraw");
        }
        return completions;
    }

    private void openWithdrawGUI(Player player) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection conn = plugin.getDatabaseManager().getConnection();
                 PreparedStatement pstmt = conn.prepareStatement("SELECT id, base64_data FROM player_web_rewards WHERE uuid = ?")) {
                pstmt.setString(1, player.getUniqueId().toString());
                ResultSet rs = pstmt.executeQuery();

                List<WebRewardItem> items = new ArrayList<>();
                while (rs.next()) {
                    items.add(new WebRewardItem(rs.getInt("id"), rs.getString("base64_data")));
                }

                Bukkit.getScheduler().runTask(plugin, () -> {
                    Inventory inv = Bukkit.createInventory(null, 54, net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize("<dark_gray>Retraits Web (Récompenses))");
                    int slot = 0;
                    for (WebRewardItem wItem : items) {
                        if (slot >= 54) break;
                        ItemStack stack = plugin.getStorageManager().itemStackFromBase64(wItem.base64);
                        if (stack != null) {
                            ItemMeta meta = stack.getItemMeta();
                            if (meta != null) {
                                meta.getPersistentDataContainer().set(rewardKey, PersistentDataType.INTEGER, wItem.id);
                                stack.setItemMeta(meta);
                            }
                            inv.setItem(slot, stack);
                        }
                        slot++;
                    }
                    player.openInventory(inv);
                });

            } catch (Exception e) {
                e.printStackTrace();
                plugin.getLangManager().sendMessage(player, "webcommand.msg_7");
            }
        });
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getView().getTitle().equals("§8Retraits Web (Récompenses)")) {
            event.setCancelled(true);
            if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR) return;
            
            ItemStack clicked = event.getCurrentItem();
            ItemMeta meta = clicked.getItemMeta();
            if (meta == null || !meta.getPersistentDataContainer().has(rewardKey, PersistentDataType.INTEGER)) return;
            
            int id = meta.getPersistentDataContainer().get(rewardKey, PersistentDataType.INTEGER);
            Player player = (Player) event.getWhoClicked();
            
            // Delete from DB and give to player
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                try (Connection conn = plugin.getDatabaseManager().getConnection();
                     PreparedStatement pstmt = conn.prepareStatement("DELETE FROM player_web_rewards WHERE id = ?")) {
                    pstmt.setInt(1, id);
                    int affected = pstmt.executeUpdate();
                    if (affected > 0) {
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            meta.getPersistentDataContainer().remove(rewardKey);
                            clicked.setItemMeta(meta);
                            player.getInventory().addItem(clicked).forEach((idx, item) -> {
                                player.getWorld().dropItem(player.getLocation(), item);
                            });
                            event.getInventory().remove(clicked);
                            plugin.getLangManager().sendMessage(player, "webcommand.msg_8");
                        });
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }
    }

    public static class WebRewardItem {
        public final int id;
        public final String base64;

        public WebRewardItem(int id, String base64) {
            this.id = id;
            this.base64 = base64;
        }
    }
}
