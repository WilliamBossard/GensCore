package fr.gens.core.modules.teams;

import fr.gens.core.CorePlugin;
import fr.gens.core.modules.BlueMapModule;
import fr.gens.core.modules.EconomyModule;
import fr.gens.core.utils.PlaceholderUtils;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
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
        String title = PlainTextComponentSerializer.plainText().serialize(event.getView().title());
        if (event.getInventory().getHolder() instanceof TeamGui.TeamGuiHolder || title.startsWith("Guilde : ")) {
            if (event.getClickedInventory() == null) return;
            if (!event.getClickedInventory().equals(event.getView().getTopInventory())) {
                if (event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
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

            if (item.getType() == Material.PAINTING) {
                if (isLeader) {
                    teamGui.openTeamColorGui(player, team);
                } else {
                    player.sendMessage(PlaceholderUtils.parseToComponent("<red>Seul le chef de guilde peut modifier la couleur BlueMap."));
                }
                return;
            }

            boolean canManage = team.isAdminOrLeader(player.getUniqueId());

            if (item.getType() == Material.GRASS_BLOCK) {
                if (canManage) {
                    player.closeInventory();
                    player.performCommand("team claim");
                } else {
                    player.sendMessage(PlaceholderUtils.parseToComponent("<red>Seuls le chef et les administrateurs peuvent revendiquer un territoire."));
                }
                return;
            }

            if (item.getType() == Material.GOLD_INGOT || item.getType() == Material.EXPERIENCE_BOTTLE) {
                EconomyModule ecoMod = (EconomyModule) plugin.getModuleManager().getModule("economy");
                boolean isEco = (ecoMod != null && ecoMod.isEnabled());
                if (isEco) {
                    player.sendMessage(PlaceholderUtils.parseToComponent("<green>[Guilde] Solde de la banque : <gold>" + String.format("%.2f", team.getBankBalance()) + " $"));
                    player.sendMessage(PlaceholderUtils.parseToComponent("<gray>Utilisez <yellow>/team deposit <montant><gray> pour alimenter la banque."));
                    if (isLeader) {
                        player.sendMessage(PlaceholderUtils.parseToComponent("<gray>Utilisez <yellow>/team withdraw <montant><gray> pour retirer des fonds."));
                    }
                } else {
                    player.sendMessage(PlaceholderUtils.parseToComponent("<green>[Guilde] Solde de la banque : <yellow>" + team.getBankXp() + " Niveaux d'XP"));
                    player.sendMessage(PlaceholderUtils.parseToComponent("<gray>Utilisez <yellow>/team depositxp <niveaux><gray> pour alimenter la banque."));
                    if (isLeader) {
                        player.sendMessage(PlaceholderUtils.parseToComponent("<gray>Utilisez <yellow>/team withdrawxp <niveaux><gray> pour retirer des niveaux."));
                    }
                }
                return;
            }

            if (item.getType() == Material.REPEATER && canManage) {
                team.setAutoLock(!team.isAutoLock());
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

            if (item.getType() == Material.PLAYER_HEAD) {
                SkullMeta meta = (SkullMeta) item.getItemMeta();
                if (meta != null && meta.getOwningPlayer() != null) {
                    UUID targetUuid = meta.getOwningPlayer().getUniqueId();
                    String targetName = meta.getOwningPlayer().getName() != null ? meta.getOwningPlayer().getName() : "ce joueur";
                    if (isLeader) {
                        if (event.getClick() == ClickType.LEFT) {
                            if (!targetUuid.equals(player.getUniqueId())) {
                                if (team.isAdmin(targetUuid)) {
                                    plugin.getTeamManager().demoteAdmin(team, targetUuid);
                                    player.sendMessage(PlaceholderUtils.parseToComponent("<yellow>" + targetName + " a été rétrogradé au rang de Membre."));
                                } else {
                                    plugin.getTeamManager().promoteAdmin(team, targetUuid);
                                    player.sendMessage(PlaceholderUtils.parseToComponent("<green>" + targetName + " a été promu Administrateur de la guilde !"));
                                }
                                teamGui.openTeamGui(player);
                            }
                        } else if (event.getClick() == ClickType.RIGHT || event.getClick() == ClickType.SHIFT_RIGHT) {
                            if (team.canManageMembers(player.getUniqueId(), targetUuid)) {
                                plugin.getTeamManager().removeMember(team, targetUuid);
                                plugin.getLangManager().sendMessage(player, "teamlistener.msg_3");
                                teamGui.openTeamGui(player);
                            }
                        }
                    } else if (team.isAdmin(player.getUniqueId())) {
                        if (event.getClick() == ClickType.RIGHT || event.getClick() == ClickType.SHIFT_RIGHT) {
                            if (team.canManageMembers(player.getUniqueId(), targetUuid)) {
                                plugin.getTeamManager().removeMember(team, targetUuid);
                                plugin.getLangManager().sendMessage(player, "teamlistener.msg_3");
                                teamGui.openTeamGui(player);
                            } else {
                                player.sendMessage(PlaceholderUtils.parseToComponent("<red>Vous ne pouvez pas exclure ce joueur."));
                            }
                        }
                    }
                }
            }
        }
    }

    @EventHandler
    public void onUpgradesGuiClick(InventoryClickEvent event) {
        String title = PlainTextComponentSerializer.plainText().serialize(event.getView().title());
        if (event.getInventory().getHolder() instanceof TeamGui.TeamUpgradesGuiHolder || title.equals("Améliorations de Guilde")) {
            if (event.getClickedInventory() == null) return;
            if (!event.getClickedInventory().equals(event.getView().getTopInventory())) {
                if (event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
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
                boolean canManagePerk = team.isAdminOrLeader(player.getUniqueId());
                if (!canManagePerk) {
                    player.sendMessage(PlaceholderUtils.parseToComponent("<red>Seuls le chef et les administrateurs peuvent acheter des améliorations."));
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
        String title = PlainTextComponentSerializer.plainText().serialize(event.getView().title());
        if (event.getInventory().getHolder() instanceof TeamGui.TeamQuestGuiHolder || title.contains("Quêtes de Guilde") || title.contains("Quête")) {
            if (event.getClickedInventory() == null) return;
            if (!event.getClickedInventory().equals(event.getView().getTopInventory())) {
                if (event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
                    event.setCancelled(true);
                }
                return;
            }

            event.setCancelled(true);
            if (!(event.getWhoClicked() instanceof Player)) return;
            Player player = (Player) event.getWhoClicked();
            ItemStack item = event.getCurrentItem();
            if (item == null) return;

            if (item.getType() == Material.ARROW) {
                teamGui.openTeamGui(player);
            }
        }
    }

    @EventHandler
    public void onColorGuiClick(InventoryClickEvent event) {
        String title = PlainTextComponentSerializer.plainText().serialize(event.getView().title());
        if (event.getInventory().getHolder() instanceof TeamGui.TeamColorGuiHolder || title.contains("Couleur BlueMap")) {
            if (event.getClickedInventory() == null) return;
            if (!event.getClickedInventory().equals(event.getView().getTopInventory())) {
                if (event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
                    event.setCancelled(true);
                }
                return;
            }

            event.setCancelled(true);
            if (!(event.getWhoClicked() instanceof Player)) return;
            Player player = (Player) event.getWhoClicked();
            ItemStack item = event.getCurrentItem();
            if (item == null || item.getType() == Material.AIR || item.getType() == Material.BLACK_STAINED_GLASS_PANE || item.getType() == Material.PAINTING) return;

            TeamData team = plugin.getTeamManager().getPlayerTeam(player.getUniqueId());
            if (team == null) return;

            if (item.getType() == Material.ARROW) {
                teamGui.openTeamGui(player);
                return;
            }

            boolean canManageColor = team.isAdminOrLeader(player.getUniqueId());
            if (!canManageColor) {
                player.sendMessage(PlaceholderUtils.parseToComponent("<red>Seuls le chef et les administrateurs peuvent modifier la couleur du territoire."));
                return;
            }

            for (TeamGui.ColorEntry entry : TeamGui.getColorPalette()) {
                if (entry.material == item.getType()) {
                    team.setColor(entry.hex);
                    plugin.getTeamManager().saveColorAsync(team);
                    BlueMapModule bmm = (BlueMapModule) plugin.getModuleManager().getModule("bluemap");
                    if (bmm != null && bmm.isEnabled()) {
                        bmm.updateAllTeamTerritories();
                    }
                    player.sendMessage(PlaceholderUtils.parseToComponent("<green>Couleur de territoire mise à jour : <yellow>" + entry.hex));
                    teamGui.openTeamGui(player);
                    return;
                }
            }
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        String title = PlainTextComponentSerializer.plainText().serialize(event.getView().title());
        if (event.getInventory().getHolder() instanceof TeamGui.TeamGuiHolder ||
            event.getInventory().getHolder() instanceof TeamGui.TeamQuestGuiHolder ||
            event.getInventory().getHolder() instanceof TeamGui.TeamUpgradesGuiHolder ||
            event.getInventory().getHolder() instanceof TeamGui.TeamColorGuiHolder ||
            title.startsWith("Guilde : ") ||
            title.contains("Quêtes de Guilde") ||
            title.contains("Quête") ||
            title.contains("Améliorations de Guilde") ||
            title.contains("Couleur BlueMap")) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        plugin.getFoliaLib().getScheduler().runAsync((wrappedTask) -> {
            TeamModule module = (TeamModule) plugin.getModuleManager().getModule("teams");
            if (module != null) {
                module.getTeamDAO().processPendingRewards(event.getPlayer());
            }
        });
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        TeamModule module = (TeamModule) plugin.getModuleManager().getModule("teams");
        if (module != null && module.getTeamCommand() != null) {
            module.getTeamCommand().removeInvite(event.getPlayer().getUniqueId());
        }
    }
}
