package fr.gens.core.modules.auth;

import fr.gens.core.CorePlugin;
import fr.gens.core.modules.Module;
import fr.gens.core.utils.DatabaseManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.mindrot.jbcrypt.BCrypt;

import org.bukkit.command.CommandExecutor;

public class AuthModule implements Module, Listener, CommandExecutor {

    private CorePlugin plugin;
    private boolean enabled;
    private final Set<UUID> authenticated = new HashSet<>();
    private final long SESSION_TIMEOUT = 30L * 24L * 60L * 60L * 1000L; // 30 days

    public AuthModule(CorePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "auth";
    }

    @Override
    public String getDescription() {
        return "Module d'authentification";
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void enable() {
        this.enabled = true;
        Bukkit.getPluginManager().registerEvents(this, plugin);
        plugin.getLangManager().sendConsoleMessage("authmodule.log_1");
    }

    @Override
    public void disable() {
        this.enabled = false;
        authenticated.clear();
        Bukkit.getScheduler().cancelTasks(plugin);
        plugin.getLangManager().sendConsoleMessage("authmodule.log_2");
    }
    
    @Override
    public void registerCommands(fr.gens.core.CorePlugin plugin) {
        if (plugin.getCommand("register") != null) plugin.getCommand("register").setExecutor(this);
        if (plugin.getCommand("login") != null) plugin.getCommand("login").setExecutor(this);
        if (plugin.getCommand("changemdp") != null) plugin.getCommand("changemdp").setExecutor(this);
    }

    public void forceLogout(UUID uuid) {
        authenticated.remove(uuid);
        Player p = Bukkit.getPlayer(uuid);
        if (p != null && p.isOnline()) {
            plugin.getLangManager().sendMessage(p, "authmodule.msg_1");
            plugin.getLangManager().sendMessage(p, "authmodule.msg_2");
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!enabled || !(sender instanceof Player)) return true;
        Player p = (Player) sender;
        UUID uuid = p.getUniqueId();

        if (label.equalsIgnoreCase("register")) {
            if (authenticated.contains(uuid)) {
                plugin.getLangManager().sendMessage(p, "authmodule.msg_3");
                return true;
            }
            DatabaseManager.AuthData data = plugin.getDatabaseManager().getAuthData(uuid);
            if (data != null) {
                plugin.getLangManager().sendMessage(p, "authmodule.msg_4");
                return true;
            }
            if (args.length < 2) {
                plugin.getLangManager().sendMessage(p, "authmodule.msg_5");
                return true;
            }
            if (!args[0].equals(args[1])) {
                plugin.getLangManager().sendMessage(p, "authmodule.msg_6");
                return true;
            }
            String password = args[0];
            if (password.length() < 4) {
                plugin.getLangManager().sendMessage(p, "authmodule.msg_7");
                return true;
            }

            String salt = ""; // Non utilisé pour BCrypt mais gardé pour compatibilité BDD
            String hash = BCrypt.hashpw(password, BCrypt.gensalt());
            String ip = p.getAddress().getAddress().getHostAddress();

            plugin.getDatabaseManager().registerPlayer(uuid, hash, salt, ip);
            authenticated.add(uuid);
            plugin.getLangManager().sendMessage(p, "authmodule.msg_8");
            
            fr.gens.core.modules.discord.DiscordModule discord = (fr.gens.core.modules.discord.DiscordModule) plugin.getModuleManager().getModule("discord");
            if (discord != null && discord.isEnabled()) {
                discord.logAuthEvent(p.getName(), "Nouvel Enregistrement", new java.awt.Color(0, 200, 255));
            }
            return true;
        }

        if (label.equalsIgnoreCase("login")) {
            if (authenticated.contains(uuid)) {
                plugin.getLangManager().sendMessage(p, "authmodule.msg_9");
                return true;
            }
            DatabaseManager.AuthData data = plugin.getDatabaseManager().getAuthData(uuid);
            if (data == null) {
                plugin.getLangManager().sendMessage(p, "authmodule.msg_10");
                return true;
            }
            if (args.length < 1) {
                plugin.getLangManager().sendMessage(p, "authmodule.msg_11");
                return true;
            }
            String password = args[0];
            
            boolean isAuthenticated = false;
            boolean needsMigration = false;
            
            if (data.hash.startsWith("$2a$") || data.hash.startsWith("$2b$") || data.hash.startsWith("$2y$")) {
                // Mot de passe BCrypt
                isAuthenticated = BCrypt.checkpw(password, data.hash);
            } else {
                // Ancien mot de passe SHA-256
                String oldHash = oldHashPassword(password, data.salt);
                if (oldHash.equals(data.hash)) {
                    isAuthenticated = true;
                    needsMigration = true;
                }
            }

            if (isAuthenticated) {
                String ip = p.getAddress().getAddress().getHostAddress();
                
                if (needsMigration) {
                    String newHash = BCrypt.hashpw(password, BCrypt.gensalt());
                    plugin.getDatabaseManager().updatePassword(uuid, newHash, "");
                    plugin.getLangManager().sendMessage(p, "authmodule.msg_12");
                }
                
                plugin.getDatabaseManager().updateLogin(uuid, ip);
                authenticated.add(uuid);
                plugin.getLangManager().sendMessage(p, "authmodule.msg_13");
                
                fr.gens.core.modules.discord.DiscordModule discord = (fr.gens.core.modules.discord.DiscordModule) plugin.getModuleManager().getModule("discord");
                if (discord != null && discord.isEnabled()) {
                    discord.logAuthEvent(p.getName(), "Connexion Réussie", java.awt.Color.GREEN);
                }
            } else {
                plugin.getLangManager().sendMessage(p, "authmodule.msg_14");
                String discordId = plugin.getDatabaseManager().getDiscordId(uuid);
                if (discordId != null && !discordId.isEmpty()) {
                    plugin.getLangManager().sendMessage(p, "authmodule.msg_15");
                }
            }
            return true;
        }

        if (command.getName().equalsIgnoreCase("changemdp")) {
            if (args.length != 2) {
                plugin.getLangManager().sendMessage(p, "authmodule.msg_16");
                return true;
            }
            if (!authenticated.contains(p.getUniqueId())) {
                plugin.getLangManager().sendMessage(p, "authmodule.msg_17");
                return true;
            }
            String oldPass = args[0];
            String newPass = args[1];

            DatabaseManager.AuthData data = plugin.getDatabaseManager().getAuthData(uuid);
            if (data != null) {
                boolean isOldPasswordCorrect = false;
                if (data.hash.startsWith("$2a$") || data.hash.startsWith("$2b$") || data.hash.startsWith("$2y$")) {
                    isOldPasswordCorrect = BCrypt.checkpw(oldPass, data.hash);
                } else {
                    String oldHash = oldHashPassword(oldPass, data.salt);
                    isOldPasswordCorrect = oldHash.equals(data.hash);
                }
                
                if (isOldPasswordCorrect) {
                    if (newPass.length() < 4) {
                        plugin.getLangManager().sendMessage(p, "authmodule.msg_18");
                        return true;
                    }
                    String newHash = BCrypt.hashpw(newPass, BCrypt.gensalt());
                    plugin.getDatabaseManager().updatePassword(uuid, newHash, "");
                    plugin.getLangManager().sendMessage(p, "authmodule.msg_19");
                } else {
                    plugin.getLangManager().sendMessage(p, "authmodule.msg_20");
                }
            }
            return true;
        }

        return false;
    }

    private void requireAuth(Player p) {
        DatabaseManager.AuthData data = plugin.getDatabaseManager().getAuthData(p.getUniqueId());
        if (data == null) {
            plugin.getLangManager().sendMessage(p, "authmodule.msg_21");
            plugin.getLangManager().sendMessage(p, "authmodule.msg_22");
            plugin.getLangManager().sendMessage(p, "authmodule.msg_23");
            plugin.getLangManager().sendMessage(p, "authmodule.msg_24");
            plugin.getLangManager().sendMessage(p, "authmodule.msg_25");
        } else {
            plugin.getLangManager().sendMessage(p, "authmodule.msg_26");
            plugin.getLangManager().sendMessage(p, "authmodule.msg_27");
            plugin.getLangManager().sendMessage(p, "authmodule.msg_28");
            plugin.getLangManager().sendMessage(p, "authmodule.msg_29");
            plugin.getLangManager().sendMessage(p, "authmodule.msg_30");
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (!enabled) return;
        Player p = event.getPlayer();
        UUID uuid = p.getUniqueId();
        
        DatabaseManager.AuthData data = plugin.getDatabaseManager().getAuthData(uuid);
        if (data != null) {
            String currentIp = p.getAddress().getAddress().getHostAddress();
            long timeSinceLastLogin = System.currentTimeMillis() - data.lastLogin;
            
            if (currentIp.equals(data.lastIp) && timeSinceLastLogin < SESSION_TIMEOUT) {
                authenticated.add(uuid);
                plugin.getDatabaseManager().updateLogin(uuid, currentIp);
                plugin.getLangManager().sendMessage(p, "authmodule.msg_31");
                
                fr.gens.core.modules.discord.DiscordModule discord = (fr.gens.core.modules.discord.DiscordModule) plugin.getModuleManager().getModule("discord");
                if (discord != null && discord.isEnabled()) {
                    discord.logAuthEvent(p.getName(), "Connexion Automatique", java.awt.Color.GREEN);
                }
                return;
            }
        }
        
        // Not authenticated
        requireAuth(p);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        authenticated.remove(event.getPlayer().getUniqueId());
    }

    // Blocking events
    private boolean isAuth(Player p) {
        return authenticated.contains(p.getUniqueId());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onMove(PlayerMoveEvent event) {
        if (!enabled) return;
        if (!isAuth(event.getPlayer())) {
            Location from = event.getFrom();
            Location to = event.getTo();
            if (to != null && (from.getBlockX() != to.getBlockX() || from.getBlockY() != to.getBlockY() || from.getBlockZ() != to.getBlockZ())) {
                event.setTo(from);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncPlayerChatEvent event) {
        if (!enabled) return;
        if (!isAuth(event.getPlayer())) {
            event.setCancelled(true);
            requireAuth(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onCommandPreprocess(PlayerCommandPreprocessEvent event) {
        if (!enabled) return;
        if (!isAuth(event.getPlayer())) {
            String cmd = event.getMessage().toLowerCase();
            if (!cmd.startsWith("/login") && !cmd.startsWith("/register")) {
                event.setCancelled(true);
                requireAuth(event.getPlayer());
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInteract(PlayerInteractEvent event) {
        if (!enabled) return;
        if (!isAuth(event.getPlayer())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBlockBreak(BlockBreakEvent event) {
        if (!enabled) return;
        if (!isAuth(event.getPlayer())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (!enabled) return;
        if (!isAuth(event.getPlayer())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!enabled) return;
        if (event.getWhoClicked() instanceof Player) {
            if (!isAuth((Player) event.getWhoClicked())) event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDrop(PlayerDropItemEvent event) {
        if (!enabled) return;
        if (!isAuth(event.getPlayer())) event.setCancelled(true);
    }

    // Security Utilities

    public static String generateSalt() {
        // Gardé pour rétrocompatibilité lors d'anciens resetmdp non-BCrypt si jamais
        SecureRandom random = new SecureRandom();
        byte[] saltBytes = new byte[16];
        random.nextBytes(saltBytes);
        return Base64.getEncoder().encodeToString(saltBytes);
    }

    public static String oldHashPassword(String password, String salt) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(salt.getBytes());
            byte[] hashedBytes = md.digest(password.getBytes());
            return Base64.getEncoder().encodeToString(hashedBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 non supporté", e);
        }
    }

    public static String hashPassword(String password, String salt) {
        // N'est plus appelé qu'historiquement, BCrypt gère le hash maintenant.
        // On retourne la version BCrypt par défaut.
        return BCrypt.hashpw(password, BCrypt.gensalt());
    }
}
