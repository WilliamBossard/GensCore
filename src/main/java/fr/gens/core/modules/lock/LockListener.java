package fr.gens.core.modules.lock;

import fr.gens.core.CorePlugin;
import fr.gens.core.modules.teams.TeamData;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.UUID;


public class LockListener implements Listener {
    private final CorePlugin plugin;
    private final LockModule lockModule;

    public LockListener(CorePlugin plugin, LockModule lockModule) {
        this.plugin = plugin;
        this.lockModule = lockModule;
    }

    private boolean isLockable(Material mat) {
        return mat == Material.CHEST || mat == Material.TRAPPED_CHEST ||
               mat == Material.BARREL || mat == Material.FURNACE ||
               mat == Material.BLAST_FURNACE || mat == Material.SMOKER ||
               mat.name().endsWith("SHULKER_BOX") || mat == Material.DROPPER ||
               mat == Material.DISPENSER || mat == Material.BREWING_STAND;
    }

    private boolean canAccess(Player player, LockData lock) {
        if (player.hasPermission("genscore.lock.bypass")) return true;

        // Si le verrou est strictement privÃƒÆ’Ã‚Â© (-2), seul le propriÃƒÆ’Ã‚Â©taire peut l'ouvrir.
        if (lock.getTeamId() == -2) {
            return lock.getOwnerUuid() != null && lock.getOwnerUuid().equals(player.getUniqueId());
        }

        // Si le verrou appartient ÃƒÆ’Ã‚Â  une guilde (teamId > 0)
        if (lock.getTeamId() > 0) {
            TeamData team = plugin.getTeamManager().getTeam(lock.getTeamId());
            if (team != null && team.hasMember(player.getUniqueId())) {
                return true;
            }
        }

        // Si le verrou est personnel (-1) mais que le joueur a rejoint une guilde
        // On autorise les membres de la guilde du propriÃƒÆ’Ã‚Â©taire ÃƒÆ’Ã‚Â  ouvrir ses coffres personnels (partage).
        if (lock.getTeamId() == -1 && lock.getOwnerUuid() != null) {
            if (lock.getOwnerUuid().equals(player.getUniqueId())) return true;
            
            TeamData ownerTeam = plugin.getTeamManager().getPlayerTeam(lock.getOwnerUuid());
            TeamData playerTeam = plugin.getTeamManager().getPlayerTeam(player.getUniqueId());
            if (ownerTeam != null && playerTeam != null && ownerTeam.getTeamId() == playerTeam.getTeamId()) {
                return true; // Ils sont dans la mÃƒÆ’Ã‚Âªme guilde !
            }
        }

        return lock.getOwnerUuid() != null && lock.getOwnerUuid().equals(player.getUniqueId());
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Block block = event.getBlock();
        if (!isLockable(block.getType())) return;

        Player player = event.getPlayer();
        TeamData team = plugin.getTeamManager().getPlayerTeam(player.getUniqueId());

        if (team != null && team.isAutoLock()) {
            lockModule.createLock(block.getLocation(), null, team.getTeamId());
            player.sendMessage(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize("<yellow>[Verrous] <green>Conteneur verrouillÃƒÆ’Ã‚Â© pour l'ÃƒÆ’Ã‚Â©quipe <yellow>" + team.getName() + "<green>."));
        } else {
            lockModule.createLock(block.getLocation(), player.getUniqueId(), -1);
            plugin.getLangManager().sendMessage(player, "locklistener.msg_1");
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getClickedBlock() == null) return;
        Block block = event.getClickedBlock();
        if (!isLockable(block.getType())) return;

        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        
        // Gestion des actions en attente (/lock et /unlock via clic gauche)
        if (event.getAction() == Action.LEFT_CLICK_BLOCK && LockCommand.pendingActions.containsKey(uuid)) {
            String action = LockCommand.pendingActions.remove(uuid);
            event.setCancelled(true);
            LockData currentLock = lockModule.getLock(block.getLocation());
            
            if (action.equals("guild")) {
                if (currentLock != null) {
                    plugin.getLangManager().sendMessage(player, "locklistener.msg_2");
                } else {
                    TeamData team = plugin.getTeamManager().getPlayerTeam(uuid);
                    if (team != null) {
                        lockModule.createLock(block.getLocation(), null, team.getTeamId());
                        player.sendMessage(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize("<green>VerrouillÃƒÆ’Ã‚Â© pour la guilde " + team.getName() + "."));
                    } else {
                        plugin.getLangManager().sendMessage(player, "locklistener.msg_3");
                    }
                }
            } else if (action.equals("private")) {
                if (currentLock != null && !canAccess(player, currentLock)) {
                    plugin.getLangManager().sendMessage(player, "locklistener.msg_4");
                } else {
                    lockModule.createLock(block.getLocation(), uuid, -1);
                    plugin.getLangManager().sendMessage(player, "locklistener.msg_5");
                }
            } else if (action.equals("unlock")) {
                if (currentLock == null) {
                    plugin.getLangManager().sendMessage(player, "locklistener.msg_6");
                } else if (canAccess(player, currentLock)) {
                    lockModule.removeLock(block.getLocation());
                    plugin.getLangManager().sendMessage(player, "locklistener.msg_7");
                } else {
                    plugin.getLangManager().sendMessage(player, "locklistener.msg_8");
                }
            }
            return;
        }

        // EmpÃƒÆ’Ã‚Âªcher l'ouverture si verrouillÃƒÆ’Ã‚Â© (Clic droit)
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            LockData lock = lockModule.getLock(block.getLocation());
            if (lock != null) {
                if (!canAccess(player, lock)) {
                    event.setCancelled(true);
                    plugin.getLangManager().sendMessage(player, "locklistener.msg_9");
                }
            }
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (!isLockable(block.getType())) return;

        LockData lock = lockModule.getLock(block.getLocation());
        if (lock != null) {
            Player player = event.getPlayer();
            if (!canAccess(player, lock)) {
                event.setCancelled(true);
                plugin.getLangManager().sendMessage(player, "locklistener.msg_10");
            } else {
                lockModule.removeLock(block.getLocation());
                plugin.getLangManager().sendMessage(player, "locklistener.msg_11");
            }
        }
    }

    @EventHandler
    public void onInventoryMove(InventoryMoveItemEvent event) {
        // EmpÃƒÆ’Ã‚Âªcher les Hoppers/Minecart-Hoppers d'aspirer depuis un bloc verrouillÃƒÆ’Ã‚Â©
        if (event.getSource().getLocation() != null) {
            LockData lock = lockModule.getLock(event.getSource().getLocation());
            if (lock != null) {
                event.setCancelled(true); // Bloquer l'aspiration !
            }
        }
        
        // EmpÃƒÆ’Ã‚Âªcher les Hoppers de pousser dans un bloc verrouillÃƒÆ’Ã‚Â© (optionnel mais recommandÃƒÆ’Ã‚Â©)
        if (event.getDestination().getLocation() != null) {
            LockData lock = lockModule.getLock(event.getDestination().getLocation());
            if (lock != null) {
                event.setCancelled(true); // Bloquer l'insertion !
            }
        }
    }
}

