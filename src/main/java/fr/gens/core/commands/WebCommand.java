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
    private final fr.gens.core.database.WebDAO webDAO;

    public WebCommand(CorePlugin plugin) {
        this.plugin = plugin;
        this.rewardKey = new NamespacedKey(plugin, "web_reward_id");
        this.webDAO = new fr.gens.core.database.WebDAO(plugin);
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

        ItemStack toDeposit = item.clone();
        // Serialiser un exemplaire unitaire (amount=1) pour que la signature base64_data soit identique
        // quelle que soit la quantite de l'item depose (permettant le stacking propre)
        ItemStack singleTemplate = toDeposit.clone();
        singleTemplate.setAmount(1);
        String base64 = plugin.getStorageManager().itemStackToBase64(singleTemplate);
        if (base64 == null) {
            plugin.getLangManager().sendMessage(player, "webcommand.msg_3");
            return;
        }

        final String materialName = toDeposit.getType().name();
        final int initialDepositAmount = toDeposit.getAmount();
        // Minecraft max stack size for this material
        final int mcMaxStack = toDeposit.getType().getMaxStackSize();

        // Retrait immediat sur le thread joueur pour eviter la duplication
        player.getInventory().setItemInMainHand(null);

        plugin.getFoliaLib().getScheduler().runAsync((wrappedTask) -> {
            try {
                fr.gens.core.database.WebDAO localWebDAO = this.webDAO;
                int currentDepositAmount = initialDepositAmount;

                // 1. Tenter le stacking tant que l'item est stackable (mcMaxStack > 1) et qu'il reste de la quantite
                if (mcMaxStack > 1) {
                    while (currentDepositAmount > 0) {
                        java.util.Map<String, Object> existing = localWebDAO.findStackableDeposit(
                            player.getUniqueId().toString(), materialName, base64, mcMaxStack);
                        if (existing == null) break;

                        int existingId = ((Number) existing.get("id")).intValue();
                        int existingAmount = ((Number) existing.get("amount")).intValue();
                        int space = mcMaxStack - existingAmount;
                        if (space <= 0) break;

                        int toAdd = Math.min(space, currentDepositAmount);
                        int newTotal = existingAmount + toAdd;
                        localWebDAO.updateDepositedItemAmount(existingId, player.getUniqueId().toString(), newTotal);
                        currentDepositAmount -= toAdd;
                    }
                }

                if (currentDepositAmount == 0) {
                    // Tout a ete empile avec succes dans un slot existant !
                    plugin.getFoliaLib().getScheduler().runAtEntity(player, (t2) -> {
                        String msg = plugin.getLangManager().getRaw("webcommand.msg_10")
                            .replace("{amount}", String.valueOf(initialDepositAmount))
                            .replace("{material}", materialName);
                        player.sendMessage(fr.gens.core.utils.PlaceholderUtils.parseToComponent(msg));
                    });
                    return;
                }

                // 2. Verifier la limite de slots avant d'inserer une nouvelle ligne
                int depositLimit = plugin.getConfigManager()
                    .getConfig("modules/web.yml")
                    .getInt("web.deposit_limit", 27);
                int usedSlots = localWebDAO.countDepositedSlots(player.getUniqueId().toString());

                if (usedSlots >= depositLimit) {
                    // Restituer le surplus au joueur
                    final int refundAmount = currentDepositAmount;
                    plugin.getFoliaLib().getScheduler().runAtEntity(player, (t2) -> {
                        if (player.isOnline()) {
                            ItemStack refundStack = toDeposit.clone();
                            refundStack.setAmount(refundAmount);
                            player.getInventory().addItem(refundStack).values().forEach(rem ->
                                player.getWorld().dropItemNaturally(player.getLocation(), rem)
                            );
                        }
                        String msg = plugin.getLangManager().getRaw("webcommand.msg_9")
                            .replace("{current}", String.valueOf(usedSlots))
                            .replace("{max}", String.valueOf(depositLimit));
                        player.sendMessage(fr.gens.core.utils.PlaceholderUtils.parseToComponent(msg));
                    });
                    return;
                }

                // 3. Inserer un nouveau slot de depot
                final int newSlotAmount = currentDepositAmount;
                try (java.sql.Connection conn = plugin.getDatabaseManager().getConnection();
                     java.sql.PreparedStatement pstmt = conn.prepareStatement(
                         "INSERT INTO player_web_bets (uuid, material, amount, base64_data) VALUES (?, ?, ?, ?)")) {
                    pstmt.setString(1, player.getUniqueId().toString());
                    pstmt.setString(2, materialName);
                    pstmt.setInt(3, newSlotAmount);
                    pstmt.setString(4, base64);
                    pstmt.executeUpdate();

                    plugin.getFoliaLib().getScheduler().runAtEntity(player, (t2) -> {
                        String msg = plugin.getLangManager().getRaw("webcommand.msg_11")
                            .replace("{amount}", String.valueOf(newSlotAmount))
                            .replace("{material}", materialName);
                        player.sendMessage(fr.gens.core.utils.PlaceholderUtils.parseToComponent(msg));
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
                // Rollback defensif : restituer l'objet
                plugin.getFoliaLib().getScheduler().runAtEntity(player, (t2) -> {
                    if (player.isOnline()) {
                        ItemStack refundStack = toDeposit.clone();
                        refundStack.setAmount(initialDepositAmount);
                        player.getInventory().addItem(refundStack).values().forEach(rem ->
                            player.getWorld().dropItemNaturally(player.getLocation(), rem)
                        );
                    }
                    plugin.getLangManager().sendMessage(player, "webcommand.msg_5");
                });
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
                        ItemStack stack = plugin.getStorageManager().itemStackFromBase64(wItem.base64());
                        if (stack != null) {
                            ItemMeta meta = stack.getItemMeta();
                            if (meta != null) {
                                meta.getPersistentDataContainer().set(rewardKey, PersistentDataType.INTEGER, wItem.id());
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

    @EventHandler
    public void onPlayerJoin(org.bukkit.event.player.PlayerJoinEvent event) {
        Player player = event.getPlayer();
        plugin.getFoliaLib().getScheduler().runAsync((task) -> {
            try (Connection conn = plugin.getDatabaseManager().getConnection();
                 PreparedStatement pstmt = conn.prepareStatement("SELECT id, base64_data FROM player_web_rewards WHERE uuid = ?")) {
                pstmt.setString(1, player.getUniqueId().toString());
                ResultSet rs = pstmt.executeQuery();
                List<WebRewardItem> pending = new ArrayList<>();
                while (rs.next()) {
                    pending.add(new WebRewardItem(rs.getInt("id"), rs.getString("base64_data")));
                }
                if (pending.isEmpty()) return;

                plugin.getFoliaLib().getScheduler().runAtEntity(player, (t2) -> {
                    if (!player.isOnline()) return;
                    List<Integer> deliveredIds = new ArrayList<>();
                    for (WebRewardItem wItem : pending) {
                        ItemStack stack = plugin.getStorageManager().itemStackFromBase64(wItem.base64());
                        if (stack != null) {
                            player.getInventory().addItem(stack).values().forEach(rem -> 
                                player.getWorld().dropItemNaturally(player.getLocation(), rem)
                            );
                            deliveredIds.add(wItem.id());
                        }
                    }
                    if (!deliveredIds.isEmpty()) {
                        player.sendMessage(fr.gens.core.utils.PlaceholderUtils.parseToComponent(
                            "<green>[Boutique Web] Vos achats effectues en ligne vous ont ete distribues !"
                        ));
                        plugin.getFoliaLib().getScheduler().runAsync((t3) -> {
                            try (Connection conn2 = plugin.getDatabaseManager().getConnection()) {
                                for (int delId : deliveredIds) {
                                    try (PreparedStatement delStmt = conn2.prepareStatement("DELETE FROM player_web_rewards WHERE id = ?")) {
                                        delStmt.setInt(1, delId);
                                        delStmt.executeUpdate();
                                    }
                                }
                            } catch (Exception ignored) {}
                        });
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public record WebRewardItem(int id, String base64) {}
}



