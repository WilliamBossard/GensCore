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
                teamGui.openTeamQuestGui(player, team);
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
                if (event.getClick() == org.bukkit.event.inventory.ClickType.RIGHT || event.getClick() == org.bukkit.event.inventory.ClickType.SHIFT_RIGHT) {
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



