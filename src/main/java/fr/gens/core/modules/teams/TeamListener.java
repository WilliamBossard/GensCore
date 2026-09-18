package fr.gens.core.modules.teams;

import fr.gens.core.CorePlugin;
import fr.gens.core.utils.PlaceholderUtils;
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
        if (event.getInventory().getHolder() instanceof TeamGui.TeamGuiHolder ||
            net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(event.getView().title()).startsWith("Guilde : ")) {
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

            if (item.getType() == Material.NETHER_STAR) {
                teamGui.openTeamUpgradesGui(player, team);
                return;
            }

            if (item.getType() == Material.GRASS_BLOCK) {
                if (isLeader) {
                    player.closeInventory();
                    player.performCommand("team claim");
                } else {
                    player.sendMessage(PlaceholderUtils.parseToComponent("<red>Seul le chef de guilde peut revendiquer un territoire."));
                }
                return;
            }

            if (item.getType() == Material.GOLD_INGOT || item.getType() == Material.EXPERIENCE_BOTTLE) {
                fr.gens.core.modules.EconomyModule ecoMod = (fr.gens.core.modules.EconomyModule) plugin.getModuleManager().getModule("economy");
                boolean isEco = (ecoMod != null && ecoMod.isEnabled());
                if (isEco) {
                    player.sendMessage(PlaceholderUtils.parseToComponent("<green>[Guilde] Solde de la banque : <gold>" + String.format("%.2f", team.getBankBalance()) + " $"));
                    player.sendMessage(PlaceholderUtils.parseToComponent("<gray>Utilisez <yellow>/team deposit <montant><gray> pour alimenter la banque."));
                } else {
                    player.sendMessage(PlaceholderUtils.parseToComponent("<green>[Guilde] Solde de la banque : <yellow>" + team.getBankXp() + " Niveaux d'XP"));
                    player.sendMessage(PlaceholderUtils.parseToComponent("<gray>Utilisez <yellow>/team depositxp <niveaux><gray> pour alimenter la banque."));
                }
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
    public void onUpgradesGuiClick(InventoryClickEvent event) {
        if (event.getInventory().getHolder() instanceof TeamGui.TeamUpgradesGuiHolder ||
            net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(event.getView().title()).equals("Améliorations de Guilde")) {
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
            if (item == null || item.getType() == Material.AIR || item.getType() == Material.BLACK_STAINED_GLASS_PANE) return;

            TeamData team = plugin.getTeamManager().getPlayerTeam(player.getUniqueId());
            if (team == null) return;
            boolean isLeader = team.getLeaderUuid().equals(player.getUniqueId());

            if (item.getType() == Material.ARROW) {
                teamGui.openTeamGui(player);
                return;
            }

            String perkKey = null;
            String perkLabel = "";
            int slot = event.getSlot();
            if (slot == 11) {
                perkKey = "MEMBERS";
                perkLabel = "Membres Max";
            } else if (slot == 12) {
                perkKey = "CLAIMS";
                perkLabel = "Territoire Étendu";
            } else if (slot == 13) {
                perkKey = "JOBS";
                perkLabel = "Bonus Métiers";
            } else if (slot == 14) {
                perkKey = "AH_TAX";
                perkLabel = "Réduction Taxe HDV";
            } else if (slot == 15) {
                perkKey = "QUESTS";
                perkLabel = "Bonus Quêtes Coop";
            }

            if (perkKey != null) {
                if (!isLeader) {
                    player.sendMessage(PlaceholderUtils.parseToComponent("<red>Seul le chef de guilde peut acheter des améliorations."));
                    return;
                }
                boolean bought = plugin.getTeamManager().buyPerk(team, perkKey);
                if (bought) {
                    player.sendMessage(PlaceholderUtils.parseToComponent("<green>Amélioration " + perkLabel + " achetée avec succès via la banque de guilde !"));
                } else {
                    player.sendMessage(PlaceholderUtils.parseToComponent("<red>Fonds insuffisants dans la banque de guilde ou niveau max déjà atteint."));
                }
                teamGui.openTeamUpgradesGui(player, team);
            }
        }
    }

    @EventHandler
    public void onQuestGuiClick(InventoryClickEvent event) {
        if (event.getInventory().getHolder() instanceof TeamGui.TeamQuestGuiHolder ||
            net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(event.getView().title()).equals("Quête de Guilde")) {
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
    public void onInventoryDrag(org.bukkit.event.inventory.InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof TeamGui.TeamGuiHolder ||
            event.getInventory().getHolder() instanceof TeamGui.TeamQuestGuiHolder ||
            event.getInventory().getHolder() instanceof TeamGui.TeamUpgradesGuiHolder ||
            net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(event.getView().title()).startsWith("Guilde : ") ||
            net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(event.getView().title()).equals("Quête de Guilde") ||
            net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(event.getView().title()).equals("Améliorations de Guilde")) {
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



