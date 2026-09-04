package fr.gens.core.modules.moderation;

import fr.gens.core.CorePlugin;
import fr.gens.core.modules.Module;
import fr.gens.core.modules.auth.AuthModule;
import org.bukkit.Bukkit;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.inventory.ItemStack;

import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import cloud.commandframework.annotations.Argument;
import cloud.commandframework.annotations.CommandMethod;
import cloud.commandframework.annotations.specifier.Greedy;
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
import fr.gens.core.modules.discord.DiscordModule;
import io.papermc.paper.event.player.AsyncChatEvent;

import java.util.concurrent.ConcurrentHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;


public class ModerationModule implements Module, Listener {

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
    private final Set<UUID> frozenPlayers = ConcurrentHashMap.newKeySet();
    private final Map<UUID, MuteData> mutedPlayers = new ConcurrentHashMap<>();
    
    private fr.gens.core.database.ModerationDAO moderationDAO;

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

    public fr.gens.core.database.ModerationDAO getModerationDAO() {
        return moderationDAO;
    }

    @Override
    public void initDatabase(fr.gens.core.utils.DatabaseManager dbManager) {
        dbManager.executeStatement("CREATE TABLE IF NOT EXISTS moderation_mutes (uuid VARCHAR(36) PRIMARY KEY, expiration BIGINT NOT NULL, reason TEXT NOT NULL);");
        dbManager.executeStatement("CREATE TABLE IF NOT EXISTS moderation_frozen (uuid VARCHAR(36) PRIMARY KEY);");
    }

    @Override
    public void enable() {
        this.enabled = true;
        
        this.moderationDAO = new fr.gens.core.database.ModerationDAO(plugin);
        this.moderationDAO.initDatabase();
        


        loadFrozenPlayers();
        loadMutedPlayers();

        Bukkit.getPluginManager().registerEvents(this, plugin);
        plugin.getLangManager().sendConsoleMessage("moderationmodule.log_1");
    }

    @Override
    public void disable() {
        org.bukkit.event.HandlerList.unregisterAll(this);
        this.enabled = false;
        
        // Save to Database instead of config
        this.moderationDAO.saveFrozen(frozenPlayers);
        this.moderationDAO.saveMutes(mutedPlayers);
        
        plugin.getLangManager().sendConsoleMessage("moderationmodule.log_2");
    }

    @Override
    public void registerCommands(fr.gens.core.CorePlugin plugin) {
        if (plugin.getCommandManager() != null && plugin.getCommandManager().getAnnotationParser() != null) {
            plugin.getCommandManager().getAnnotationParser().parse(this);
        }
    }
    
    public void loadFrozenPlayers() {
        frozenPlayers.clear();
        frozenPlayers.addAll(this.moderationDAO.loadFrozen());
        
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
        mutedPlayers.putAll(this.moderationDAO.loadMutes());
        
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

    @CommandMethod("freeze <target>")
    public void executeFreeze(CommandSender sender, @Argument("target") String targetName) {
        if (!enabled) return;
        if (!sender.hasPermission("genscore.freeze")) {
            plugin.getLangManager().sendMessage(sender, "moderationmodule.msg_1");
            return;
        }
        Player target = Bukkit.getPlayer(targetName);
        if (target == null) {
            plugin.getLangManager().sendMessage(sender, "moderationmodule.msg_3");
            return;
        }
        UUID uuid = target.getUniqueId();
        
        // Exécution sur le thread du joueur ciblé pour éviter les crashs AsyncCatcher sur Folia
        plugin.getFoliaLib().getScheduler().runAtEntity(target, (t) -> {
            if (frozenPlayers.contains(uuid)) {
                frozenPlayers.remove(uuid);
                target.setGravity(true);
                removeFreezeEffects(target);
                
                if (sender instanceof Player) {
                    plugin.getFoliaLib().getScheduler().runAtEntity((Player) sender, (s) -> {
                        sender.sendMessage(fr.gens.core.utils.PlaceholderUtils.parseToComponent("<green>Le joueur " + target.getName() + " a été dégelé."));
                    });
                } else {
                    sender.sendMessage(fr.gens.core.utils.PlaceholderUtils.parseToComponent("<green>Le joueur " + target.getName() + " a été dégelé."));
                }
                plugin.getLangManager().sendMessage(target, "moderationmodule.msg_4");
            } else {
                frozenPlayers.add(uuid);
                target.setGravity(false);
                applyFreezeEffects(target);
                
                if (sender instanceof Player) {
                    plugin.getFoliaLib().getScheduler().runAtEntity((Player) sender, (s) -> {
                        sender.sendMessage(fr.gens.core.utils.PlaceholderUtils.parseToComponent("<green>Le joueur " + target.getName() + " a été gelé."));
                    });
                } else {
                    sender.sendMessage(fr.gens.core.utils.PlaceholderUtils.parseToComponent("<green>Le joueur " + target.getName() + " a été gelé."));
                }
                plugin.getLangManager().sendMessage(target, "moderationmodule.msg_5");
            }
        });
    }

    @CommandMethod("openinv <target>")
    public void executeOpenInv(org.bukkit.command.CommandSender sender, @Argument("target") String targetName) {
        if (!(sender instanceof org.bukkit.entity.Player)) return;
        org.bukkit.entity.Player p = (org.bukkit.entity.Player) sender;
        if (!enabled) return;
        if (!p.hasPermission("genscore.openinv")) {
            plugin.getLangManager().sendMessage(p, "moderationmodule.msg_6");
            return;
        }
        Player target = Bukkit.getPlayer(targetName);
        if (target == null) {
            plugin.getLangManager().sendMessage(p, "moderationmodule.msg_9");
            return;
        }
        
        if (!plugin.getFoliaLib().isFolia()) {
            p.openInventory(target.getInventory());
            return;
        }

        // Impossible d'ouvrir l'inventaire d'une autre région sur Folia. On utilise un clone.
        plugin.getFoliaLib().getScheduler().runAtEntity(target, (t) -> {
            ItemStack[] contents = target.getInventory().getContents();
            
            plugin.getFoliaLib().getScheduler().runAtEntity(p, (s) -> {
                org.bukkit.inventory.Inventory inv = Bukkit.createInventory(null, 45, net.kyori.adventure.text.Component.text("Inv: " + target.getName()));
                inv.setContents(contents);
                p.openInventory(inv);
                p.sendMessage(fr.gens.core.utils.PlaceholderUtils.parseToComponent("<yellow>Lecture seule (Clone Folia)"));
            });
        });
    }

    @EventHandler
    public void onModerationInventoryClick(org.bukkit.event.inventory.InventoryClickEvent event) {
        if (event.getView().title() != null) {
            String title = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(event.getView().title());
            if (title.startsWith("Inv: ")) {
                event.setCancelled(true);
            }
        }
    }


    @CommandMethod("resetmdp <target>")
    public void executeResetMdp(CommandSender sender, @Argument("target") String targetName) {
        if (!enabled) return;
        if (!sender.hasPermission("genscore.admin")) {
            plugin.getLangManager().sendMessage(sender, "moderationmodule.msg_10");
            return;
        }
        
        Player targetOnline = Bukkit.getPlayer(targetName);
        UUID targetUUID = null;
        
        if (targetOnline != null) {
            targetUUID = targetOnline.getUniqueId();
        } else {
            for (OfflinePlayer op : Bukkit.getOfflinePlayers()) {
        if (op == null) continue;
                String name = op.getName();
                if (name != null && name.equalsIgnoreCase(targetName)) {
                    targetUUID = op.getUniqueId();
                    break;
                }
            }
        }
        
        if (targetUUID == null) {
            plugin.getLangManager().sendMessage(sender, "moderationmodule.msg_12");
            return;
        }
        
        final UUID finalUUID = targetUUID;
        plugin.getFoliaLib().getScheduler().runAsync((wrappedTask) -> {
            fr.gens.core.modules.auth.AuthModule authModule = (fr.gens.core.modules.auth.AuthModule) plugin.getModuleManager().getModule("auth");
            if (authModule == null || authModule.getAuthDAO().getAuthData(finalUUID) == null) {
                plugin.getLangManager().sendMessage(sender, "moderationmodule.msg_41");
                return;
            }
            
            authModule.getAuthDAO().removeAuthData(finalUUID);
            
            if (targetOnline != null) {
                plugin.getFoliaLib().getScheduler().runAtEntity(targetOnline, (t) -> {
                    Module authMod = plugin.getModuleManager().getModule("auth");
                    if (authMod != null && authMod.isEnabled() && authMod instanceof AuthModule) {
                        ((AuthModule) authMod).forceLogout(finalUUID);
                    }
                });
            }
            
            sender.sendMessage(fr.gens.core.utils.PlaceholderUtils.parseToComponent("<green>Le mot de passe de " + targetName + " a été supprimé."));
        });
    }

    @CommandMethod("mute <target> [args]")
    public void executeMute(CommandSender sender, @Argument("target") String targetName, @Argument(value = "args", defaultValue = "") @Greedy String argsString) {
        if (!enabled) return;
        if (!sender.hasPermission("genscore.mute")) {
            plugin.getLangManager().sendMessage(sender, "moderationmodule.msg_14");
            return;
        }
        
        plugin.getFoliaLib().getScheduler().runAsync((wrappedTask) -> {
            OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
            if (target == null || target.getUniqueId() == null) {
                plugin.getLangManager().sendMessage(sender, "moderationmodule.msg_16");
                return;
            }

            String[] parts = argsString.isEmpty() ? new String[0] : argsString.split(" ");
            long duration = 0;
            String reason = "Aucune raison";
            
            if (parts.length > 0) {
                duration = parseDuration(parts[0]);
                if (duration > 0) {
                    if (parts.length > 1) {
                        StringBuilder sb = new StringBuilder();
                        for (int i = 1; i < parts.length; i++) sb.append(parts[i]).append(" ");
                        reason = sb.toString().trim();
                    }
                } else {
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < parts.length; i++) sb.append(parts[i]).append(" ");
                    reason = sb.toString().trim();
                }
            }
            
            mutePlayer(target.getUniqueId(), reason, duration);
            sender.sendMessage(fr.gens.core.utils.PlaceholderUtils.parseToComponent("<green>Vous avez rendu muet " + (target.getName() != null ? target.getName() : targetName)));
            Player online = target.getPlayer();
            if (online != null) {
                final String finalReason = reason;
                plugin.getFoliaLib().getScheduler().runAtEntity(online, (t) -> {
                    online.sendMessage(fr.gens.core.utils.PlaceholderUtils.parseToComponent("<red><bold>Vous avez été rendu muet par un modérateur ! Raison : " + finalReason));
                });
            }
            if (target.getName() != null) sendDiscordLog("MUTE", target.getName(), sender.getName(), reason, duration);
        });
    }

    @CommandMethod("unmute <target>")
    public void executeUnmute(CommandSender sender, @Argument("target") String targetName) {
        if (!enabled) return;
        if (!sender.hasPermission("genscore.mute")) return;
        
        plugin.getFoliaLib().getScheduler().runAsync((wrappedTask) -> {
            OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
            if (target != null && target.getUniqueId() != null) {
                unmutePlayer(target.getUniqueId());
                sender.sendMessage(fr.gens.core.utils.PlaceholderUtils.parseToComponent("<green>Le joueur " + (target.getName() != null ? target.getName() : targetName) + " n'est plus muet."));
                Player online = target.getPlayer();
                if (online != null) {
                    plugin.getFoliaLib().getScheduler().runAtEntity(online, (t) -> {
                        plugin.getLangManager().sendMessage(online, "moderationmodule.msg_17");
                    });
                }
                sendDiscordLog("UNMUTE", target.getName() != null ? target.getName() : targetName, sender.getName(), "Pardonné", 0);
            }
        });
    }

    @CommandMethod("ban <target> [args]")
    public void executeBan(CommandSender sender, @Argument("target") String targetName, @Argument(value = "args", defaultValue = "") @Greedy String argsString) {
        if (!enabled) return;
        if (!sender.hasPermission("genscore.ban")) {
            plugin.getLangManager().sendMessage(sender, "moderationmodule.msg_18");
            return;
        }
        
        plugin.getFoliaLib().getScheduler().runAsync((wrappedTask) -> {
            OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
            if (target == null || target.getUniqueId() == null) return;

            String[] parts = argsString.isEmpty() ? new String[0] : argsString.split(" ");
            long durationMs = 0;
            String reason = "Aucune raison";
            
            if (parts.length > 0) {
                durationMs = parseDuration(parts[0]);
                if (durationMs > 0) {
                    if (parts.length > 1) {
                        StringBuilder sb = new StringBuilder();
                        for (int i = 1; i < parts.length; i++) sb.append(parts[i]).append(" ");
                        reason = sb.toString().trim();
                    }
                } else {
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < parts.length; i++) sb.append(parts[i]).append(" ");
                    reason = sb.toString().trim();
                }
            }
            
            java.util.Date expires = durationMs > 0 ? new java.util.Date(System.currentTimeMillis() + durationMs) : null;
            com.destroystokyo.paper.profile.PlayerProfile profile = org.bukkit.Bukkit.createProfile(target.getUniqueId(), target.getName());
            org.bukkit.ban.ProfileBanList banList = Bukkit.getBanList(io.papermc.paper.ban.BanListType.PROFILE);
            banList.addBan(profile, reason, expires, sender.getName());
            
            Player online = target.getPlayer();
            if (online != null) {
                final String finalReason = reason;
                plugin.getFoliaLib().getScheduler().runAtEntity(online, (t) -> {
                    online.kick(fr.gens.core.utils.PlaceholderUtils.parseToComponent("<red>Vous avez été banni du serveur.<br><white>Raison : " + finalReason));
                });
            }
            sender.sendMessage(fr.gens.core.utils.PlaceholderUtils.parseToComponent("<green>Le joueur " + (target.getName() != null ? target.getName() : targetName) + " a été banni."));
            sendDiscordLog("BAN", target.getName() != null ? target.getName() : targetName, sender.getName(), reason, durationMs);
        });
    }

    @CommandMethod("kick <target> [args]")
    public void executeKick(org.bukkit.command.CommandSender sender, @Argument("target") String targetName, @Argument(value = "args", defaultValue = "") @cloud.commandframework.annotations.specifier.Greedy String argsString) {
        if (!enabled) return;
        if (!sender.hasPermission("genscore.kick")) {
            plugin.getLangManager().sendMessage(sender, "moderationmodule.msg_18");
            return;
        }
        
        plugin.getFoliaLib().getScheduler().runAsync((wrappedTask) -> {
            Player target = Bukkit.getPlayer(targetName);
            if (target == null) {
                plugin.getLangManager().sendMessage(sender, "moderationmodule.msg_3");
                return;
            }

            String[] parts = argsString.isEmpty() ? new String[0] : argsString.split(" ");
            String reason = "Aucune raison";
            if (parts.length > 0) {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < parts.length; i++) sb.append(parts[i]).append(" ");
                reason = sb.toString().trim();
            }
            
            final String finalReason = reason;
            plugin.getFoliaLib().getScheduler().runAtEntity(target, (t) -> {
                target.kick(plugin.getLangManager().get("moderationmodule.kick_screen").replaceText(net.kyori.adventure.text.TextReplacementConfig.builder().matchLiteral("<reason>").replacement(finalReason).build()));
            });
            
            String msg = plugin.getLangManager().getRaw("moderationmodule.msg_19");
            if (msg != null) sender.sendMessage(fr.gens.core.utils.PlaceholderUtils.parseToComponent(msg.replace("<target>", target.getName())));
            sendDiscordLog("KICK", target.getName(), sender.getName(), reason, 0);
        });
    }

    @CommandMethod("unban <target>")
    public void executeUnban(CommandSender sender, @Argument("target") String targetName) {
        if (!enabled) return;
        if (!sender.hasPermission("genscore.ban")) return;
        plugin.getFoliaLib().getScheduler().runAsync((wrappedTask) -> {
            OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
            if (target != null && target.getName() != null) {
                com.destroystokyo.paper.profile.PlayerProfile profile = org.bukkit.Bukkit.createProfile(target.getUniqueId(), target.getName());
                org.bukkit.ban.ProfileBanList banList = Bukkit.getBanList(io.papermc.paper.ban.BanListType.PROFILE);
                banList.pardon(profile);
                sender.sendMessage(fr.gens.core.utils.PlaceholderUtils.parseToComponent("<green>Le joueur " + target.getName() + " a été débanni."));
                sendDiscordLog("UNBAN", target.getName(), sender.getName(), "Pardonné", 0);
            }
        });
    }

    // --- EVÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢âÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€šÃ‚Â°NEMENTS POUR LE FREEZE ET MUTE ---

    @EventHandler(priority = EventPriority.LOWEST)
    public void onAsyncPlayerChat(AsyncChatEvent event) {
        if (!enabled) return;
        if (isMuted(event.getPlayer().getUniqueId())) {
            MuteData data = getMuteData(event.getPlayer().getUniqueId());
            event.getPlayer().sendMessage(plugin.getLangManager().get("moderationmodule.mute_screen").replaceText(net.kyori.adventure.text.TextReplacementConfig.builder().matchLiteral("<reason>").replacement(data.reason).build()));
            event.setCancelled(true);
        }
    }

    /** Applies movement-blocking potion effects to a frozen player. */
    private void applyFreezeEffects(Player p) {
        p.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, Integer.MAX_VALUE, 255, false, false, false));
        p.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, Integer.MAX_VALUE, 250, false, false, false));
    }

    /** Removes only freeze-specific potion effects without affecting pre-existing ones. */
    private void removeFreezeEffects(Player p) {
        PotionEffect slow = p.getPotionEffect(PotionEffectType.SLOWNESS);
        if (slow != null && slow.getAmplifier() == 255) {
            p.removePotionEffect(PotionEffectType.SLOWNESS);
        }
        PotionEffect jump = p.getPotionEffect(PotionEffectType.JUMP_BOOST);
        if (jump != null && jump.getAmplifier() == 250) {
            p.removePotionEffect(PotionEffectType.JUMP_BOOST);
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






