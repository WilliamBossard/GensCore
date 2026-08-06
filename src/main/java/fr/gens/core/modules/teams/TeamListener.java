package fr.gens.core.modules.teams;

import fr.gens.core.CorePlugin;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.UUID;

public class TeamListener implements Listener {
    private final CorePlugin plugin;
    private final TeamGui teamGui;

    public TeamListener(CorePlugin plugin, TeamGui teamGui) {
        this.plugin = plugin;
        this.teamGui = teamGui;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getView().getTitle().startsWith("§8Guilde : ")) {
            event.setCancelled(true);
            if (!(event.getWhoClicked() instanceof Player)) return;
            Player player = (Player) event.getWhoClicked();
            ItemStack item = event.getCurrentItem();
            if (item == null) return;

            TeamData team = plugin.getTeamManager().getPlayerTeam(player.getUniqueId());
            if (team == null) return;
            boolean isLeader = team.getLeaderUuid().equals(player.getUniqueId());

            if (item.getType() == Material.ENCHANTED_BOOK) {
                openTeamQuestGui(player, team);
                return;
            }

            if (item.getType() == Material.REPEATER && isLeader) {
                team.setAutoLock(!team.isAutoLock());
                // Refresh
                teamGui.openTeamGui(player);
                return;
            }

            if (item.getType() == Material.BARRIER) {
                if (isLeader) {
                    player.sendMessage("§cEn tant que chef, vous devez utiliser /team disband ou transférer le lead (à venir). La guilde est supprimée.");
                    plugin.getTeamManager().disbandTeam(team);
                } else {
                    plugin.getTeamManager().removeMember(team, player.getUniqueId());
                    player.sendMessage("§aVous avez quitté la guilde.");
                }
                player.closeInventory();
                return;
            }

            if (item.getType() == Material.PLAYER_HEAD && isLeader) {
                SkullMeta meta = (SkullMeta) item.getItemMeta();
                if (meta.getOwningPlayer() != null) {
                    UUID targetUuid = meta.getOwningPlayer().getUniqueId();
                    if (!targetUuid.equals(player.getUniqueId())) {
                        plugin.getTeamManager().removeMember(team, targetUuid);
                        player.sendMessage("§cMembre expulsé.");
                        teamGui.openTeamGui(player);
                    }
                }
            }
        }
    }
    private void openTeamQuestGui(Player player, TeamData team) {
        org.bukkit.inventory.Inventory inv = org.bukkit.Bukkit.createInventory(null, 27, "§9§lQuête de Guilde");
        
        ItemStack glass = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        org.bukkit.inventory.meta.ItemMeta glassMeta = glass.getItemMeta();
        glassMeta.setDisplayName(" ");
        glass.setItemMeta(glassMeta);
        for (int i = 0; i < 27; i++) inv.setItem(i, glass);
        
        fr.gens.core.modules.teams.TeamQuestManager tqm = plugin.getTeamQuestManager();
        if (tqm != null) {
            ItemStack questItem = new ItemStack(Material.NETHER_STAR);
            org.bukkit.inventory.meta.ItemMeta qmeta = questItem.getItemMeta();
            qmeta.setDisplayName("§6§lQuête Hebdomadaire");
            java.util.List<String> lore = new java.util.ArrayList<>();
            lore.add("§7" + tqm.getDesc());
            lore.add("");
            int progress = tqm.getProgress(team.getTeamId());
            int goal = tqm.getGoal();
            if (progress >= goal) {
                lore.add("§a§lTERMINÉE !");
            } else {
                lore.add("§eProgression: §f" + progress + " §e/ §f" + goal);
            }
            qmeta.setLore(lore);
            questItem.setItemMeta(qmeta);
            inv.setItem(13, questItem);
            
            // Add points info
            ItemStack pointsItem = new ItemStack(Material.SUNFLOWER);
            org.bukkit.inventory.meta.ItemMeta pmeta = pointsItem.getItemMeta();
            pmeta.setDisplayName("§ePoints de Guilde");
            java.util.List<String> plore = new java.util.ArrayList<>();
            plore.add("§7Hebdomadaire : §f" + team.getWeeklyPoints());
            plore.add("§7Total : §f" + team.getTotalPoints());
            pmeta.setLore(plore);
            pointsItem.setItemMeta(pmeta);
            inv.setItem(22, pointsItem);
        }
        
        player.openInventory(inv);
    }

    @EventHandler
    public void onQuestGuiClick(InventoryClickEvent event) {
        if (event.getView().getTitle().equals("§9§lQuête de Guilde")) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerJoin(org.bukkit.event.player.PlayerJoinEvent event) {
        // Process pending rewards for the player asynchronously to avoid lagging main thread
        org.bukkit.Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            plugin.getDatabaseManager().processPendingRewards(event.getPlayer());
        });
    }
}
