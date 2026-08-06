package fr.gens.core.modules.moderation;

import fr.gens.core.CorePlugin;
import fr.gens.core.modules.Module;
import fr.gens.core.modules.auth.AuthModule;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.BanList;
import fr.gens.core.modules.discord.DiscordModule;

public class ModerationModule implements Module, CommandExecutor, TabCompleter, Listener {

    public static class MuteData {
        public String reason;
        public long expiration; // 0 = permanent
        public MuteData(String reason, long expiration) {
            this.reason = reason;
            this.expiration = expiration;
        }
    }

    private final CorePlugin plugin;
    private boolean enabled;
    private final Set<UUID> frozenPlayers = new HashSet<>();
    private final Map<UUID, MuteData> mutedPlayers = new HashMap<>();

    public ModerationModule(CorePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "Moderation";
    }

    @Override
    public String getDescription() {
        return "Commandes d'administration (Freeze, OpenInv, ResetMDP)";
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void enable() {
        this.enabled = true;
        
        plugin.getCommand("freeze").setExecutor(this);
        plugin.getCommand("freeze").setTabCompleter(this);
        
        plugin.getCommand("openinv").setExecutor(this);
        plugin.getCommand("openinv").setTabCompleter(this);
        
        plugin.getCommand("resetmdp").setExecutor(this);
        plugin.getCommand("resetmdp").setTabCompleter(this);
        
        plugin.getCommand("mute").setExecutor(this);
        plugin.getCommand("mute").setTabCompleter(this);
        plugin.getCommand("unmute").setExecutor(this);
        plugin.getCommand("unmute").setTabCompleter(this);
        plugin.getCommand("ban").setExecutor(this);
        plugin.getCommand("ban").setTabCompleter(this);
        plugin.getCommand("unban").setExecutor(this);
        plugin.getCommand("unban").setTabCompleter(this);

        loadFrozenPlayers();
        loadMutedPlayers();

        Bukkit.getPluginManager().registerEvents(this, plugin);
        plugin.getLogger().info("[Moderation] Module activé.");
    }

    @Override
    public void disable() {
        this.enabled = false;
        List<String> frozen = new ArrayList<>();
        for (UUID uuid : frozenPlayers) {
            frozen.add(uuid.toString());
        }
        plugin.getConfig().set("moderation.frozen", frozen);
        
        List<String> mutes = new ArrayList<>();
        for (Map.Entry<UUID, MuteData> entry : mutedPlayers.entrySet()) {
            mutes.add(entry.getKey().toString() + ";" + entry.getValue().expiration + ";" + entry.getValue().reason);
        }
        plugin.getConfig().set("moderation.mutes", mutes);
        
        plugin.saveConfig();
        plugin.getLogger().info("[Moderation] Module désactivé.");
    }
    
    public void loadFrozenPlayers() {
        if (plugin.getConfig().contains("moderation.frozen")) {
            List<String> frozen = plugin.getConfig().getStringList("moderation.frozen");
            for (String s : frozen) {
                try {
                    frozenPlayers.add(UUID.fromString(s));
                } catch (Exception ignored) {}
            }
        }
    }

    public void loadMutedPlayers() {
        if (plugin.getConfig().contains("moderation.mutes")) {
            List<String> mutes = plugin.getConfig().getStringList("moderation.mutes");
            for (String s : mutes) {
                try {
                    String[] parts = s.split(";", 3);
                    if (parts.length >= 2) {
                        UUID uuid = UUID.fromString(parts[0]);
                        long exp = Long.parseLong(parts[1]);
                        String reason = parts.length == 3 ? parts[2] : "";
                        mutedPlayers.put(uuid, new MuteData(reason, exp));
                    }
                } catch (Exception ignored) {}
            }
        }
    }

    public boolean isMuted(UUID uuid) {
        if (!mutedPlayers.containsKey(uuid)) return false;
        MuteData data = mutedPlayers.get(uuid);
        if (data.expiration > 0 && System.currentTimeMillis() > data.expiration) {
            mutedPlayers.remove(uuid);
            return false;
        }
        return true;
    }

    public MuteData getMuteData(UUID uuid) {
        if (!isMuted(uuid)) return null;
        return mutedPlayers.get(uuid);
    }

    public void mutePlayer(UUID uuid, String reason, long durationMs) {
        long exp = durationMs > 0 ? System.currentTimeMillis() + durationMs : 0;
        mutedPlayers.put(uuid, new MuteData(reason != null ? reason : "Aucune", exp));
    }

    public void unmutePlayer(UUID uuid) {
        mutedPlayers.remove(uuid);
    }

    private long parseDuration(String arg) {
        try {
            if (arg.endsWith("m")) return Long.parseLong(arg.replace("m", "")) * 60 * 1000;
            if (arg.endsWith("h")) return Long.parseLong(arg.replace("h", "")) * 60 * 60 * 1000;
            if (arg.endsWith("d")) return Long.parseLong(arg.replace("d", "")) * 24 * 60 * 60 * 1000;
        } catch (Exception e) {}
        return 0; // Permanent or parse error
    }

    private void sendDiscordLog(String action, String player, String admin, String reason, long durationMs) {
        Module mod = plugin.getModuleManager().getModule("discord");
        if (mod instanceof DiscordModule && mod.isEnabled()) {
            DiscordModule discord = (DiscordModule) mod;
            String dur = durationMs > 0 ? "Temporaire" : "Permanent";
            String msg = "Action : **" + action + "**\nJoueur : " + player + "\nAdmin : " + admin + "\nRaison : " + reason + "\nDurée : " + dur;
            discord.sendBotLogEmbed(action, msg, java.awt.Color.RED);
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!enabled) return true;

        if (command.getName().equalsIgnoreCase("freeze")) {
            if (!sender.hasPermission("genscore.freeze")) {
                sender.sendMessage("§cVous n'avez pas la permission.");
                return true;
            }
            if (args.length == 0) {
                sender.sendMessage("§cUsage: /freeze <joueur>");
                return true;
            }
            Player target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                sender.sendMessage("§cJoueur introuvable ou hors ligne.");
                return true;
            }
            UUID uuid = target.getUniqueId();
            if (frozenPlayers.contains(uuid)) {
                frozenPlayers.remove(uuid);
                target.setGravity(true);
                sender.sendMessage("§aLe joueur " + target.getName() + " a été dégelé.");
                target.sendMessage("§a§lVous avez été dégelé par un administrateur.");
            } else {
                frozenPlayers.add(uuid);
                target.setGravity(false); // Geler en l'air
                sender.sendMessage("§aLe joueur " + target.getName() + " a été gelé.");
                target.sendMessage("§c§lVous avez été gelé par un administrateur !");
            }
            return true;
        }

        if (command.getName().equalsIgnoreCase("openinv")) {
            if (!sender.hasPermission("genscore.openinv")) {
                sender.sendMessage("§cVous n'avez pas la permission.");
                return true;
            }
            if (!(sender instanceof Player)) {
                sender.sendMessage("§cSeul un joueur peut utiliser cette commande.");
                return true;
            }
            if (args.length == 0) {
                sender.sendMessage("§cUsage: /openinv <joueur>");
                return true;
            }
            Player target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                sender.sendMessage("§cJoueur introuvable ou hors ligne.");
                return true;
            }
            Player p = (Player) sender;
            p.openInventory(target.getInventory());
            return true;
        }

        if (command.getName().equalsIgnoreCase("resetmdp")) {
            if (!sender.hasPermission("genscore.admin")) {
                sender.sendMessage("§cVous n'avez pas la permission.");
                return true;
            }
            if (args.length == 0) {
                sender.sendMessage("§cUsage: /resetmdp <joueur>");
                return true;
            }
            
            Player targetOnline = Bukkit.getPlayer(args[0]);
            UUID targetUUID = null;
            
            if (targetOnline != null) {
                targetUUID = targetOnline.getUniqueId();
            } else {
                for (OfflinePlayer op : Bukkit.getOfflinePlayers()) {
                    if (op.getName() != null && op.getName().equalsIgnoreCase(args[0])) {
                        targetUUID = op.getUniqueId();
                        break;
                    }
                }
            }
            
            if (targetUUID == null) {
                sender.sendMessage("§cJoueur jamais vu sur le serveur.");
                return true;
            }
            
            if (plugin.getDatabaseManager().getAuthData(targetUUID) == null) {
                sender.sendMessage("§cCe joueur n'a pas de mot de passe enregistré.");
                return true;
            }
            
            plugin.getDatabaseManager().removeAuthData(targetUUID);
            
            if (targetOnline != null) {
                Module authMod = plugin.getModuleManager().getModule("auth");
                if (authMod != null && authMod.isEnabled() && authMod instanceof AuthModule) {
                    ((AuthModule) authMod).forceLogout(targetUUID);
                }
            }
            
            sender.sendMessage("§aLe mot de passe de " + args[0] + " a été supprimé.");
            return true;
        }

        if (command.getName().equalsIgnoreCase("mute")) {
            if (!sender.hasPermission("genscore.mute")) {
                sender.sendMessage("§cVous n'avez pas la permission.");
                return true;
            }
            if (args.length < 1) {
                sender.sendMessage("§cUsage: /mute <joueur> [durée] [raison]");
                return true;
            }
            
            OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
            if (target == null || target.getUniqueId() == null) {
                sender.sendMessage("§cJoueur introuvable.");
                return true;
            }

            long duration = 0;
            String reason = "Aucune raison";
            if (args.length > 1) {
                duration = parseDuration(args[1]);
                if (duration > 0) {
                    if (args.length > 2) {
                        StringBuilder sb = new StringBuilder();
                        for (int i = 2; i < args.length; i++) sb.append(args[i]).append(" ");
                        reason = sb.toString().trim();
                    }
                } else {
                    StringBuilder sb = new StringBuilder();
                    for (int i = 1; i < args.length; i++) sb.append(args[i]).append(" ");
                    reason = sb.toString().trim();
                }
            }
            
            mutePlayer(target.getUniqueId(), reason, duration);
            sender.sendMessage("§aVous avez rendu muet " + target.getName());
            Player online = target.getPlayer();
            if (online != null) {
                online.sendMessage("§c§lVous avez été rendu muet par un modérateur ! Raison : " + reason);
            }
            sendDiscordLog("MUTE", target.getName(), sender.getName(), reason, duration);
            return true;
        }

        if (command.getName().equalsIgnoreCase("unmute")) {
            if (!sender.hasPermission("genscore.mute")) return true;
            if (args.length < 1) return false;
            OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
            if (target != null && target.getUniqueId() != null) {
                unmutePlayer(target.getUniqueId());
                sender.sendMessage("§aLe joueur " + target.getName() + " n'est plus muet.");
                Player online = target.getPlayer();
                if (online != null) online.sendMessage("§a§lVous avez retrouvé l'usage de la parole.");
                sendDiscordLog("UNMUTE", target.getName(), sender.getName(), "Pardonné", 0);
            }
            return true;
        }

        if (command.getName().equalsIgnoreCase("ban")) {
            if (!sender.hasPermission("genscore.ban")) {
                sender.sendMessage("§cVous n'avez pas la permission.");
                return true;
            }
            if (args.length < 1) return false;
            
            OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
            if (target == null || target.getUniqueId() == null) return true;

            long durationMs = 0;
            String reason = "Aucune raison";
            if (args.length > 1) {
                durationMs = parseDuration(args[1]);
                if (durationMs > 0) {
                    if (args.length > 2) {
                        StringBuilder sb = new StringBuilder();
                        for (int i = 2; i < args.length; i++) sb.append(args[i]).append(" ");
                        reason = sb.toString().trim();
                    }
                } else {
                    StringBuilder sb = new StringBuilder();
                    for (int i = 1; i < args.length; i++) sb.append(args[i]).append(" ");
                    reason = sb.toString().trim();
                }
            }
            
            java.util.Date expires = durationMs > 0 ? new java.util.Date(System.currentTimeMillis() + durationMs) : null;
            Bukkit.getBanList(org.bukkit.BanList.Type.NAME).addBan(target.getName(), reason, expires, sender.getName());
            
            Player online = target.getPlayer();
            if (online != null) {
                online.kickPlayer("§cVous avez été banni du serveur.\n§fRaison : " + reason);
            }
            sender.sendMessage("§aLe joueur " + target.getName() + " a été banni.");
            sendDiscordLog("BAN", target.getName(), sender.getName(), reason, durationMs);
            return true;
        }

        if (command.getName().equalsIgnoreCase("unban")) {
            if (!sender.hasPermission("genscore.ban")) return true;
            if (args.length < 1) return false;
            OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
            if (target != null && target.getName() != null) {
                Bukkit.getBanList(org.bukkit.BanList.Type.NAME).pardon(target.getName());
                sender.sendMessage("§aLe joueur " + target.getName() + " a été débanni.");
                sendDiscordLog("UNBAN", target.getName(), sender.getName(), "Pardonné", 0);
            }
            return true;
        }

        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (!enabled) return completions;

        if (command.getName().equalsIgnoreCase("freeze") && sender.hasPermission("genscore.freeze")) {
            if (args.length == 1) {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (p.getName().toLowerCase().startsWith(args[0].toLowerCase())) {
                        completions.add(p.getName());
                    }
                }
            }
        }
        else if (command.getName().equalsIgnoreCase("openinv") && sender.hasPermission("genscore.openinv")) {
            if (args.length == 1) {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (p.getName().toLowerCase().startsWith(args[0].toLowerCase())) {
                        completions.add(p.getName());
                    }
                }
            }
        }
        else if (command.getName().equalsIgnoreCase("resetmdp") && sender.hasPermission("genscore.admin")) {
            if (args.length == 1) {
                for (OfflinePlayer op : Bukkit.getOfflinePlayers()) {
                    if (op.getName() != null && op.getName().toLowerCase().startsWith(args[0].toLowerCase())) {
                        completions.add(op.getName());
                    }
                }
            }
        }
        else if ((command.getName().equalsIgnoreCase("ban") || command.getName().equalsIgnoreCase("mute")) && sender.hasPermission("genscore.ban")) {
            if (args.length == 1) {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (p.getName().toLowerCase().startsWith(args[0].toLowerCase())) {
                        completions.add(p.getName());
                    }
                }
            } else if (args.length == 2) {
                if ("1h".startsWith(args[1].toLowerCase())) completions.add("1h");
                if ("1d".startsWith(args[1].toLowerCase())) completions.add("1d");
                if ("7d".startsWith(args[1].toLowerCase())) completions.add("7d");
            }
        }
        else if ((command.getName().equalsIgnoreCase("unban") || command.getName().equalsIgnoreCase("unmute")) && sender.hasPermission("genscore.ban")) {
            if (args.length == 1) {
                for (OfflinePlayer op : Bukkit.getOfflinePlayers()) {
                    if (op.getName() != null && op.getName().toLowerCase().startsWith(args[0].toLowerCase())) {
                        completions.add(op.getName());
                    }
                }
            }
        }
        return completions;
    }

    // --- EVÉNEMENTS POUR LE FREEZE ET MUTE ---

    @EventHandler(priority = EventPriority.LOWEST)
    public void onAsyncPlayerChat(AsyncPlayerChatEvent event) {
        if (!enabled) return;
        if (isMuted(event.getPlayer().getUniqueId())) {
            MuteData data = getMuteData(event.getPlayer().getUniqueId());
            event.getPlayer().sendMessage("§cVous êtes rendu muet sur le serveur ! Raison : " + data.reason);
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        if (frozenPlayers.contains(event.getPlayer().getUniqueId())) {
            // Empêcher les mouvements de caméra ou X/Z/Y mais autoriser la caméra
            if (event.getFrom().getX() != event.getTo().getX() || 
                event.getFrom().getY() != event.getTo().getY() || 
                event.getFrom().getZ() != event.getTo().getZ()) {
                
                Location newLoc = event.getFrom();
                newLoc.setPitch(event.getTo().getPitch());
                newLoc.setYaw(event.getTo().getYaw());
                event.setTo(newLoc);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (frozenPlayers.contains(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockBreak(BlockBreakEvent event) {
        if (frozenPlayers.contains(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (frozenPlayers.contains(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        if (frozenPlayers.contains(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player) {
            if (frozenPlayers.contains(event.getWhoClicked().getUniqueId())) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player) {
            if (frozenPlayers.contains(event.getEntity().getUniqueId())) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player) {
            if (frozenPlayers.contains(event.getDamager().getUniqueId())) {
                event.setCancelled(true);
            }
        }
    }
}
