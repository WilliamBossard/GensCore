package fr.gens.core.commands;

import org.incendo.cloud.annotations.Command;
import fr.gens.core.CorePlugin;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.NamespacedKey;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class WebCommand implements Listener {

    private final CorePlugin plugin;
    private final NamespacedKey rewardKey;

    public WebCommand(CorePlugin plugin) {
        this.plugin = plugin;
        this.rewardKey = new NamespacedKey(plugin, "web_reward_id");
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @Command("web")
    public void executeHelp(org.bukkit.command.CommandSender sender) {
        if (!(sender instanceof org.bukkit.entity.Player)) return;
        org.bukkit.entity.Player player = (org.bukkit.entity.Player) sender;
        plugin.getLangManager().sendMessage(player, "webcommand.msg_1");
    }

    @Command("web deposit")
    public void executeDeposit(org.bukkit.command.CommandSender sender) {
        if (!(sender instanceof org.bukkit.entity.Player)) return;
        org.bukkit.entity.Player player = (org.bukkit.entity.Player) sender;
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || item.getType() == Material.AIR) {
            plugin.getLangManager().sendMessage(player, "webcommand.msg_2");
            return;
        }

        String base64 = plugin.getStorageManager().itemStackToBase64(item);
        if (base64 == null) {
            plugin.getLangManager().sendMessage(player, "webcommand.msg_3");
            return;
        }

        plugin.getFoliaLib().getScheduler().runAsync((wrappedTask) -> {
            try (Connection conn = plugin.getDatabaseManager().getConnection();
                 PreparedStatement pstmt = conn.prepareStatement("INSERT INTO player_web_bets (uuid, material, amount, base64_data) VALUES (?, ?, ?, ?)")) {
                pstmt.setString(1, player.getUniqueId().toString());
                pstmt.setString(2, item.getType().name());
                pstmt.setInt(3, item.getAmount());
                pstmt.setString(4, base64);
                pstmt.executeUpdate();

                plugin.getFoliaLib().getScheduler().runAtEntity(player, (t2) -> {
                    player.getInventory().setItemInMainHand(null);
                    plugin.getLangManager().sendMessage(player, "webcommand.msg_4");
                });
            } catch (Exception e) {
                e.printStackTrace();
                plugin.getLangManager().sendMessage(player, "webcommand.msg_5");
            }
        });
    }

    @Command("web withdraw")
    public void executeWithdraw(org.bukkit.command.CommandSender sender) {
        if (!(sender instanceof org.bukkit.entity.Player)) return;
        org.bukkit.entity.Player player = (org.bukkit.entity.Player) sender;
        openWithdrawGUI(player);
    }

    private void openWithdrawGUI(Player player) {
        plugin.getFoliaLib().getScheduler().runAsync((wrappedTask) -> {
            try (Connection conn = plugin.getDatabaseManager().getConnection();
                 PreparedStatement pstmt = conn.prepareStatement("SELECT id, base64_data FROM player_web_rewards WHERE uuid = ?")) {
                pstmt.setString(1, player.getUniqueId().toString());
                ResultSet rs = pstmt.executeQuery();

                List<WebRewardItem> items = new ArrayList<>();
                while (rs.next()) {
                    items.add(new WebRewardItem(rs.getInt("id"), rs.getString("base64_data")));
                }

                plugin.getFoliaLib().getScheduler().runAtEntity(player, (t2) -> {
                    Inventory inv = Bukkit.createInventory(null, 54, fr.gens.core.utils.PlaceholderUtils.parseToComponent("<dark_gray>Retraits Web (R\u00e9compenses)"));
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
        if (net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(event.getView().title()).equals("Retraits Web (R\u00e9compenses)")) {
            if (event.getClickedInventory() == null) return;
            if (!event.getClickedInventory().equals(event.getView().getTopInventory())) {
                if (event.getAction() == org.bukkit.event.inventory.InventoryAction.MOVE_TO_OTHER_INVENTORY) {
                    event.setCancelled(true);
                }
                return;
            }
            
            event.setCancelled(true);
            if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR) return;
            
            ItemStack clicked = event.getCurrentItem();
            ItemMeta meta = clicked.getItemMeta();
            if (meta == null || !meta.getPersistentDataContainer().has(rewardKey, PersistentDataType.INTEGER)) return;
            
            Integer idObj = meta.getPersistentDataContainer().get(rewardKey, PersistentDataType.INTEGER);
            if (idObj == null) return;
            int id = idObj;
            Player player = (Player) event.getWhoClicked();
            
            // Delete from DB and give to player
            plugin.getFoliaLib().getScheduler().runAsync((wrappedTask) -> {
                try (Connection conn = plugin.getDatabaseManager().getConnection();
                     PreparedStatement pstmt = conn.prepareStatement("DELETE FROM player_web_rewards WHERE id = ?")) {
                    pstmt.setInt(1, id);
                    int affected = pstmt.executeUpdate();
                    if (affected > 0) {
                        plugin.getFoliaLib().getScheduler().runAtEntity(player, (t2) -> {
                            meta.getPersistentDataContainer().remove(rewardKey);
                            clicked.setItemMeta(meta);
                            player.getInventory().addItem(clicked).forEach((idx, itm) -> {
                                player.getWorld().dropItem(player.getLocation(), itm);
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



