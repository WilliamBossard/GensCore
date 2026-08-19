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

import fr.gens.core.modules.discord.DiscordModule;
import io.papermc.paper.event.player.AsyncChatEvent;

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
        plugin.getLangManager().sendConsoleMessage("moderationmodule.log_1");
    }

    @Override
    public void disable() {
        this.enabled = false;
        
        // Save to Database instead of config
        plugin.getDatabaseManager().getModerationDAO().saveFrozen(frozenPlayers);
        plugin.getDatabaseManager().getModerationDAO().saveMutes(mutedPlayers);
        
        plugin.getLangManager().sendConsoleMessage("moderationmodule.log_2");
    }
    
    public void loadFrozenPlayers() {
        frozenPlayers.clear();
        frozenPlayers.addAll(plugin.getDatabaseManager().getModerationDAO().loadFrozen());
        
        // Migration from config if needed
        if (plugin.getConfigManager().getConfig("modules/moderation.yml").contains("moderation.frozen")) {
            List<String> frozen = plugin.getConfigManager().getConfig("modules/moderation.yml").getStringList("moderation.frozen");
            for (String s : frozen) {
                try {
                    frozenPlayers.add(UUID.fromString(s));
                } catch (Exception ignored) {}
            }
            plugin.getConfigManager().getConfig("modules/moderation.yml").set("moderation.frozen", null);
            plugin.getConfigManager().saveConfig("modules/moderation.yml");
        }
    }

    public void loadMutedPlayers() {
        mutedPlayers.clear();
        mutedPlayers.putAll(plugin.getDatabaseManager().getModerationDAO().loadMutes());
        
        // Migration from config if needed
        if (plugin.getConfigManager().getConfig("modules/moderation.yml").contains("moderation.mutes")) {
            List<String> mutes = plugin.getConfigManager().getConfig("modules/moderation.yml").getStringList("moderation.mutes");
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
            plugin.getConfigManager().getConfig("modules/moderation.yml").set("moderation.mutes", null);
            plugin.getConfigManager().saveConfig("modules/moderation.yml");
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
                plugin.getLangManager().sendMessage(sender, "moderationmodule.msg_1");
                return true;
            }
            if (args.length == 0) {
                plugin.getLangManager().sendMessage(sender, "moderationmodule.msg_2");
                return true;
            }
            Player target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                plugin.getLangManager().sendMessage(sender, "moderationmodule.msg_3");
                return true;
            }
            UUID uuid = target.getUniqueId();
            if (frozenPlayers.contains(uuid)) {
                frozenPlayers.remove(uuid);
                target.setGravity(true);
                sender.sendMessage("<green>Le joueur " + target.getName() + " a été dégelé.");
                plugin.getLangManager().sendMessage(target, "moderationmodule.msg_4");
            } else {
                frozenPlayers.add(uuid);
                target.setGravity(false); // Geler en l'air
                sender.sendMessage("<green>Le joueur " + target.getName() + " a été gelé.");
                plugin.getLangManager().sendMessage(target, "moderationmodule.msg_5");
            }
            return true;
        }

        if (command.getName().equalsIgnoreCase("openinv")) {
            if (!sender.hasPermission("genscore.openinv")) {
                plugin.getLangManager().sendMessage(sender, "moderationmodule.msg_6");
                return true;
            }
            if (!(sender instanceof Player)) {
                plugin.getLangManager().sendMessage(sender, "moderationmodule.msg_7");
                return true;
            }
            if (args.length == 0) {
                plugin.getLangManager().sendMessage(sender, "moderationmodule.msg_8");
                return true;
            }
            Player target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                plugin.getLangManager().sendMessage(sender, "moderationmodule.msg_9");
                return true;
            }
            Player p = (Player) sender;
            p.openInventory(target.getInventory());
            return true;
        }

        if (command.getName().equalsIgnoreCase("resetmdp")) {
            if (!sender.hasPermission("genscore.admin")) {
                plugin.getLangManager().sendMessage(sender, "moderationmodule.msg_10");
                return true;
            }
            if (args.length == 0) {
                plugin.getLangManager().sendMessage(sender, "moderationmodule.msg_11");
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
                plugin.getLangManager().sendMessage(sender, "moderationmodule.msg_12");
                return true;
            }
            
            if (plugin.getDatabaseManager().getAuthDAO().getAuthData(targetUUID) == null) {
                plugin.getLangManager().sendMessage(sender, "moderationmodule.msg_13");
                return true;
            }
            
            plugin.getDatabaseManager().getAuthDAO().removeAuthData(targetUUID);
            
            if (targetOnline != null) {
                Module authMod = plugin.getModuleManager().getModule("auth");
                if (authMod != null && authMod.isEnabled() && authMod instanceof AuthModule) {
                    ((AuthModule) authMod).forceLogout(targetUUID);
                }
            }
            
            sender.sendMessage("<green>Le mot de passe de " + args[0] + " a été supprimé.");
            return true;
        }

        if (command.getName().equalsIgnoreCase("mute")) {
            if (!sender.hasPermission("genscore.mute")) {
                plugin.getLangManager().sendMessage(sender, "moderationmodule.msg_14");
                return true;
            }
            if (args.length < 1) {
                plugin.getLangManager().sendMessage(sender, "moderationmodule.msg_15");
                return true;
            }
            
            OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
            if (target == null || target.getUniqueId() == null) {
                plugin.getLangManager().sendMessage(sender, "moderationmodule.msg_16");
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
            sender.sendMessage("<green>Vous avez rendu muet " + target.getName());
            Player online = target.getPlayer();
            if (online != null) {
                online.sendMessage("<red><bold>Vous avez été rendu muet par un modérateur ! Raison : " + reason);
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
                sender.sendMessage("<green>Le joueur " + target.getName() + " n'est plus muet.");
                Player online = target.getPlayer();
                if (online != null) plugin.getLangManager().sendMessage(online, "moderationmodule.msg_17");
                sendDiscordLog("UNMUTE", target.getName(), sender.getName(), "Pardonné", 0);
            }
            return true;
        }

        if (command.getName().equalsIgnoreCase("ban")) {
            if (!sender.hasPermission("genscore.ban")) {
                plugin.getLangManager().sendMessage(sender, "moderationmodule.msg_18");
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
            org.bukkit.profile.PlayerProfile profile = org.bukkit.Bukkit.createProfile(target.getUniqueId(), target.getName());
            org.bukkit.BanList<org.bukkit.profile.PlayerProfile> banList = Bukkit.getBanList(org.bukkit.BanList.Type.PROFILE);
            banList.addBan(profile, reason, expires, sender.getName());
            
            Player online = target.getPlayer();
            if (online != null) {
                online.kick(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize("<red>Vous avez été banni du serveur.<br><white>Raison : " + reason));
            }
            sender.sendMessage("<green>Le joueur " + target.getName() + " a été banni.");
            sendDiscordLog("BAN", target.getName(), sender.getName(), reason, durationMs);
            return true;
        }

        if (command.getName().equalsIgnoreCase("unban")) {
            if (!sender.hasPermission("genscore.ban")) return true;
            if (args.length < 1) return false;
            OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
            if (target != null && target.getName() != null) {
                org.bukkit.profile.PlayerProfile profile = org.bukkit.Bukkit.createProfile(target.getUniqueId(), target.getName());
                org.bukkit.BanList<org.bukkit.profile.PlayerProfile> banList = Bukkit.getBanList(org.bukkit.BanList.Type.PROFILE);
                banList.pardon(profile);
                sender.sendMessage("<green>Le joueur " + target.getName() + " a été débanni.");
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
    public void onAsyncPlayerChat(AsyncChatEvent event) {
        if (!enabled) return;
        if (isMuted(event.getPlayer().getUniqueId())) {
            MuteData data = getMuteData(event.getPlayer().getUniqueId());
            event.getPlayer().sendMessage("<red>Vous êtes rendu muet sur le serveur ! Raison : " + data.reason);
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
