package fr.gens.core.modules.teams;

import fr.gens.core.CorePlugin;
import fr.gens.core.modules.BlueMapModule;
import fr.gens.core.modules.EconomyModule;
import fr.gens.core.utils.PlaceholderUtils;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.potion.PotionEffect;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

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

            if (item.getType() == Material.RED_BED) {
                if (team.getUpgradeLevel("GUILD_HOME") > 0) {
                    if (event.getClick() == ClickType.RIGHT || event.getClick() == ClickType.SHIFT_RIGHT) {
                        if (canManage) {
                            player.closeInventory();
                            player.performCommand("team sethome");
                        } else {
                            player.sendMessage(PlaceholderUtils.parseToComponent("<red>Seuls le chef et les administrateurs peuvent definir le home."));
                        }
                    } else {
                        player.closeInventory();
                        player.performCommand("team home");
                    }
                } else {
                    player.sendMessage(PlaceholderUtils.parseToComponent("<red>Votre guilde n'a pas encore debloque le Home de Guilde."));
                }
                return;
            }

            if (item.getType() == Material.GRAY_BED) {
                player.sendMessage(PlaceholderUtils.parseToComponent("<red>Votre guilde n'a pas encore debloque le Home de Guilde. Achetez-le dans le menu des ameliorations."));
                return;
            }

            if (item.getType() == Material.CHEST) {
                if (team.getVaultSize() > 0) {
                    teamGui.openTeamVaultGui(player, team);
                } else {
                    player.sendMessage(PlaceholderUtils.parseToComponent("<red>Votre guilde n'a pas encore debloque le Coffre de Guilde."));
                }
                return;
            }

            if (item.getType() == Material.BARREL) {
                player.sendMessage(PlaceholderUtils.parseToComponent("<red>Votre guilde n'a pas encore debloque le Coffre de Guilde. Achetez-le dans le menu des ameliorations."));
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
        if (!(event.getInventory().getHolder() instanceof TeamGui.TeamUpgradesGuiHolder)) return;
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
        // Row 1 — old perks (slots 10-14)
        if (slot == 10) { perkKey = "MEMBERS"; perkLabel = "Membres Max"; }
        else if (slot == 11) { perkKey = "CLAIMS"; perkLabel = "Territoire Etendu"; }
        else if (slot == 12) { perkKey = "JOBS"; perkLabel = "Bonus Metiers"; }
        else if (slot == 13) { perkKey = "AH_TAX"; perkLabel = "Reduction Taxe HDV"; }
        else if (slot == 14) { perkKey = "QUESTS"; perkLabel = "Bonus Quetes Coop"; }
        // Row 2 — new perks (slots 19-23)
        else if (slot == 19) { perkKey = "GUILD_HOME"; perkLabel = "Home de Guilde"; }
        else if (slot == 20) { perkKey = "TERRITORY_BUFF"; perkLabel = "Aura Territoriale"; }
        else if (slot == 21) { perkKey = "BANK_INTEREST"; perkLabel = "Interet Bancaire"; }
        else if (slot == 22) { perkKey = "SPAWNER_EFFICIENCY"; perkLabel = "Efficacite Spawners"; }
        else if (slot == 23) { perkKey = "GUILD_VAULT"; perkLabel = "Coffre de Guilde"; }

        // Action directe (clic droit ou niveau max atteint)
        if (slot == 19 && (event.getClick() == ClickType.RIGHT || event.getClick() == ClickType.SHIFT_RIGHT || team.getUpgradeLevel("GUILD_HOME") >= 3)) {
            if (team.getUpgradeLevel("GUILD_HOME") > 0) {
                player.closeInventory();
                player.performCommand("team home");
                return;
            }
        }
        if (slot == 23 && (event.getClick() == ClickType.RIGHT || event.getClick() == ClickType.SHIFT_RIGHT || team.getUpgradeLevel("GUILD_VAULT") >= 3)) {
            if (team.getVaultSize() > 0) {
                teamGui.openTeamVaultGui(player, team);
                return;
            }
        }

        if (perkKey != null) {
            boolean canManagePerk = team.isAdminOrLeader(player.getUniqueId());
            if (!canManagePerk) {
                player.sendMessage(PlaceholderUtils.parseToComponent("<red>Seuls le chef et les administrateurs peuvent acheter des ameliorations."));
                return;
            }
            boolean bought = plugin.getTeamManager().buyPerk(team, perkKey);
            if (bought) {
                player.sendMessage(PlaceholderUtils.parseToComponent("<green>Amelioration " + perkLabel + " achetee avec succes via la banque de guilde !"));
            } else {
                player.sendMessage(PlaceholderUtils.parseToComponent("<red>Fonds insuffisants dans la banque de guilde ou niveau max deja atteint."));
            }
            teamGui.openTeamUpgradesGui(player, team);
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
        // Allow dragging in vault
        if (event.getInventory().getHolder() instanceof TeamGui.TeamVaultGuiHolder) return;
        if (event.getInventory().getHolder() instanceof TeamGui.TeamGuiHolder ||
            event.getInventory().getHolder() instanceof TeamGui.TeamQuestGuiHolder ||
            event.getInventory().getHolder() instanceof TeamGui.TeamUpgradesGuiHolder ||
            event.getInventory().getHolder() instanceof TeamGui.TeamColorGuiHolder ||
            title.startsWith("Guilde : ") ||
            title.contains("Quetes de Guilde") ||
            title.contains("Quete") ||
            title.contains("Ameliorations de Guilde") ||
            title.contains("Couleur BlueMap")) {
            event.setCancelled(true);
        }
    }

    // ---- VAULT: save on close ----
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;
        Inventory inv = event.getInventory();
        if (!(inv.getHolder() instanceof TeamGui.TeamVaultGuiHolder holder)) return;
        TeamData team = holder.getTeam();
        if (team == null) return;

        // Sync inventory contents -> team vault map
        team.getVaultContents().clear();
        int size = inv.getSize();
        for (int slot = 0; slot < size; slot++) {
            ItemStack item = inv.getItem(slot);
            if (item != null && item.getType() != Material.AIR) {
                team.setVaultItem(slot, item.clone());
            }
        }
        plugin.getTeamManager().saveVaultAsync(team);
    }

    // ---- VAULT: click handler (allow all actions, cancel shift-out) ----
    @EventHandler
    public void onVaultClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof TeamGui.TeamVaultGuiHolder)) return;
        if (!(event.getWhoClicked() instanceof Player)) return;

        TeamGui.TeamVaultGuiHolder holder = (TeamGui.TeamVaultGuiHolder) event.getInventory().getHolder();
        TeamData team = holder.getTeam();
        if (team == null) { event.setCancelled(true); return; }

        // Prevent shift-clicking from player inventory INTO the vault if vault is full (Bukkit handles that),
        // but we don't need to restrict further — all members can use the vault.
        // No extra restrictions needed.
    }

    // ---- TERRITORY_BUFF: anti-abuse stabilization and presence warmup ----
    private final Map<UUID, Long> playerTerritoryEntryTime = new ConcurrentHashMap<>();
    public static final long PRESENCE_WARMUP_MS = 15_000L; // 15 secondes de présence continue dans les claims requises

    public void removeTerritoryBuffs(Player player) {
        player.removePotionEffect(org.bukkit.potion.PotionEffectType.REGENERATION);
        player.removePotionEffect(org.bukkit.potion.PotionEffectType.SATURATION);
        player.removePotionEffect(org.bukkit.potion.PotionEffectType.SPEED);
        player.removePotionEffect(org.bukkit.potion.PotionEffectType.HASTE);
    }

    public void updateTerritoryBuffsForPlayer(Player player, Chunk currentChunk) {
        TeamData playerTeam = plugin.getTeamManager().getPlayerTeam(player.getUniqueId());
        TeamClaimManager claimMgr = plugin.getTeamManager().getClaimManager();
        if (claimMgr == null) return;

        TeamData claimOwner = claimMgr.getTeamAt(currentChunk);

        // Si le joueur est sur le territoire de SA propre guilde
        if (claimOwner != null && playerTeam != null && claimOwner.getTeamId() == playerTeam.getTeamId()) {
            if (claimOwner.getUpgradeLevel("TERRITORY_BUFF") <= 0) return;

            // 1. Stabilisation du chunk (5 minutes requises après /team claim)
            if (!claimMgr.isClaimStabilized(currentChunk)) {
                long remSec = (claimMgr.getRemainingStabilizationMillis(currentChunk) + 999L) / 1000L;
                long mins = remSec / 60;
                long secs = remSec % 60;
                player.sendActionBar(PlaceholderUtils.parseToComponent(
                    "<yellow>Aura territoriale : Ancrage du chunk (<gold>" + mins + "m " + secs + "s<yellow> restants)"));
                removeTerritoryBuffs(player);
                playerTerritoryEntryTime.remove(player.getUniqueId());
                return;
            }

            // 2. Warmup de présence continue (15 secondes)
            long now = System.currentTimeMillis();
            Long entryTime = playerTerritoryEntryTime.get(player.getUniqueId());
            if (entryTime == null) {
                playerTerritoryEntryTime.put(player.getUniqueId(), now);
                player.sendActionBar(PlaceholderUtils.parseToComponent("<yellow>Aura territoriale : Synchronisation (15s)..."));
                return;
            }

            if (now - entryTime < PRESENCE_WARMUP_MS) {
                long remSec = (PRESENCE_WARMUP_MS - (now - entryTime) + 999L) / 1000L;
                player.sendActionBar(PlaceholderUtils.parseToComponent(
                    "<yellow>Aura territoriale : Synchronisation (<gold>" + remSec + "s<yellow>)..."));
                return;
            }

            // Chunk stabilisé et 15s de présence écoulées : on applique les effets !
            List<PotionEffect> effects = claimOwner.getTerritoryBuffEffects();
            for (PotionEffect effect : effects) {
                player.addPotionEffect(effect);
            }
        } else {
            // Sortie de territoire : suppression immédiate
            if (playerTerritoryEntryTime.remove(player.getUniqueId()) != null) {
                removeTerritoryBuffs(player);
            }
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        // Only process when actually changing chunk
        if (!event.hasChangedBlock()) return;
        if (event.getFrom().getChunk().equals(event.getTo().getChunk())) return;

        Player player = event.getPlayer();
        updateTerritoryBuffsForPlayer(player, event.getTo().getChunk());
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
        playerTerritoryEntryTime.remove(event.getPlayer().getUniqueId());
        TeamModule module = (TeamModule) plugin.getModuleManager().getModule("teams");
        if (module != null && module.getTeamCommand() != null) {
            module.getTeamCommand().removeInvite(event.getPlayer().getUniqueId());
        }
    }
}
