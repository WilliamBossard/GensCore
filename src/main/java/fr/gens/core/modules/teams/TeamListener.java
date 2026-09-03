package fr.gens.core.modules.teams;

import fr.gens.core.CorePlugin;
import org.bukkit.Material;
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
        if (net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(event.getView().title()).startsWith("Guilde : ")) {
            if (event.getClickedInventory() == null) return;
            if (!event.getClickedInventory().equals(event.getView().getTopInventory())) {
                if (event.getAction() == org.bukkit.event.inventory.InventoryAction.MOVE_TO_OTHER_INVENTORY) {
                    event.setCancelled(true);
                }
                return;
            }
            
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
                    plugin.getLangManager().sendMessage(player, "teamlistener.msg_1");
                    plugin.getTeamManager().disbandTeam(team);
                } else {
                    plugin.getTeamManager().removeMember(team, player.getUniqueId());
                    plugin.getLangManager().sendMessage(player, "teamlistener.msg_2");
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
                        plugin.getLangManager().sendMessage(player, "teamlistener.msg_3");
                        teamGui.openTeamGui(player);
                    }
                }
            }
        }
    }
    private void openTeamQuestGui(Player player, TeamData team) {
        org.bukkit.inventory.Inventory inv = org.bukkit.Bukkit.createInventory(null, 27, fr.gens.core.utils.PlaceholderUtils.parseToComponent("<blue><bold>Quête de Guilde"));
        
        ItemStack glass = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        org.bukkit.inventory.meta.ItemMeta glassMeta = glass.getItemMeta();
        glassMeta.displayName(fr.gens.core.utils.PlaceholderUtils.parseToComponent(" "));
        glass.setItemMeta(glassMeta);
        for (int i = 0; i < 27; i++) inv.setItem(i, glass);
        
        fr.gens.core.modules.teams.TeamQuestManager tqm = plugin.getTeamQuestManager();
        if (tqm != null) {
            ItemStack questItem = new ItemStack(Material.NETHER_STAR);
            org.bukkit.inventory.meta.ItemMeta qmeta = questItem.getItemMeta();
            qmeta.displayName(fr.gens.core.utils.PlaceholderUtils.parseToComponent("<gold><bold>Quête Hebdomadaire"));
            java.util.List<String> lore = new java.util.ArrayList<>();
            lore.add("<gray>" + tqm.getDesc());
            lore.add("");
            int progress = tqm.getProgress(team.getTeamId());
            int goal = tqm.getGoal();
            if (progress >= goal) {
                lore.add("<green><bold>TERMINÉE !");
            } else {
                lore.add("<yellow>Progression: <white>" + progress + " <yellow>/ <white>" + goal);
            }
            qmeta.lore(java.util.Optional.ofNullable(lore).orElse(java.util.Collections.emptyList()).stream().map(s -> fr.gens.core.utils.PlaceholderUtils.parseToComponent((String)s)).collect(java.util.stream.Collectors.toList()));
            questItem.setItemMeta(qmeta);
            inv.setItem(13, questItem);
            
            // Add points info
            ItemStack pointsItem = new ItemStack(Material.SUNFLOWER);
            org.bukkit.inventory.meta.ItemMeta pmeta = pointsItem.getItemMeta();
            pmeta.displayName(fr.gens.core.utils.PlaceholderUtils.parseToComponent("<yellow>Points de Guilde"));
            java.util.List<String> plore = new java.util.ArrayList<>();
            plore.add("<gray>Hebdomadaire : <white>" + team.getWeeklyPoints());
            plore.add("<gray>Total : <white>" + team.getTotalPoints());
            pmeta.lore(java.util.Optional.ofNullable(plore).orElse(java.util.Collections.emptyList()).stream().map(s -> fr.gens.core.utils.PlaceholderUtils.parseToComponent((String)s)).collect(java.util.stream.Collectors.toList()));
            pointsItem.setItemMeta(pmeta);
            inv.setItem(22, pointsItem);
        }
        
        player.openInventory(inv);
    }

    @EventHandler
    public void onQuestGuiClick(InventoryClickEvent event) {
        if (net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(event.getView().title()).equals("Quête de Guilde")) {
            if (event.getClickedInventory() == null) return;
            if (!event.getClickedInventory().equals(event.getView().getTopInventory())) {
                if (event.getAction() == org.bukkit.event.inventory.InventoryAction.MOVE_TO_OTHER_INVENTORY) {
                    event.setCancelled(true);
                }
                return;
            }
            
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerJoin(org.bukkit.event.player.PlayerJoinEvent event) {
        // Process pending rewards for the player asynchronously to avoid lagging main thread
        plugin.getFoliaLib().getScheduler().runAsync((wrappedTask) -> {
            TeamModule module = (TeamModule) plugin.getModuleManager().getModule("teams");
            if (module != null) {
                module.getTeamDAO().processPendingRewards(event.getPlayer());
            }
        });
    }

    @EventHandler
    public void onPlayerQuit(org.bukkit.event.player.PlayerQuitEvent event) {
        TeamModule module = (TeamModule) plugin.getModuleManager().getModule("teams");
        if (module != null && module.getTeamCommand() != null) {
            module.getTeamCommand().removeInvite(event.getPlayer().getUniqueId());
        }
    }
}



