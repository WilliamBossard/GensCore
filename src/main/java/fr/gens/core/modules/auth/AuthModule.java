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
        plugin.getLogger().info("[Auth] Module activé.");
    }

    @Override
    public void disable() {
        this.enabled = false;
        authenticated.clear();
        plugin.getLogger().info("[Auth] Module désactivé.");
    }

    public void forceLogout(UUID uuid) {
        authenticated.remove(uuid);
        Player p = Bukkit.getPlayer(uuid);
        if (p != null && p.isOnline()) {
            p.sendMessage("§cVotre mot de passe a été modifié.");
            p.sendMessage("§cVous avez été déconnecté. Veuillez vous reconnecter avec /login <mot_de_passe>");
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!enabled || !(sender instanceof Player)) return true;
        Player p = (Player) sender;
        UUID uuid = p.getUniqueId();

        if (label.equalsIgnoreCase("register")) {
            if (authenticated.contains(uuid)) {
                p.sendMessage("§cVous êtes déjà connecté.");
                return true;
            }
            DatabaseManager.AuthData data = plugin.getDatabaseManager().getAuthData(uuid);
            if (data != null) {
                p.sendMessage("§cVous êtes déjà enregistré. Utilisez /login <mot de passe>");
                return true;
            }
            if (args.length < 2) {
                p.sendMessage("§cUsage: /register <mot de passe> <confirmer mot de passe>");
                return true;
            }
            if (!args[0].equals(args[1])) {
                p.sendMessage("§cLes mots de passe ne correspondent pas.");
                return true;
            }
            String password = args[0];
            if (password.length() < 4) {
                p.sendMessage("§cLe mot de passe doit faire au moins 4 caractères.");
                return true;
            }

            String salt = generateSalt();
            String hash = hashPassword(password, salt);
            String ip = p.getAddress().getAddress().getHostAddress();

            plugin.getDatabaseManager().registerPlayer(uuid, hash, salt, ip);
            authenticated.add(uuid);
            p.sendMessage("§aVous avez été enregistré et connecté avec succès !");
            
            fr.gens.core.modules.discord.DiscordModule discord = (fr.gens.core.modules.discord.DiscordModule) plugin.getModuleManager().getModule("discord");
            if (discord != null && discord.isEnabled()) {
                discord.logAuthEvent(p.getName(), "Nouvel Enregistrement", new java.awt.Color(0, 200, 255));
            }
            return true;
        }

        if (label.equalsIgnoreCase("login")) {
            if (authenticated.contains(uuid)) {
                p.sendMessage("§cVous êtes déjà connecté.");
                return true;
            }
            DatabaseManager.AuthData data = plugin.getDatabaseManager().getAuthData(uuid);
            if (data == null) {
                p.sendMessage("§cVous n'êtes pas enregistré. Utilisez /register <mdp> <confirmer mdp>");
                return true;
            }
            if (args.length < 1) {
                p.sendMessage("§cUsage: /login <mot de passe>");
                return true;
            }
            String password = args[0];
            String hash = hashPassword(password, data.salt);

            if (hash.equals(data.hash)) {
                String ip = p.getAddress().getAddress().getHostAddress();
                plugin.getDatabaseManager().updateLogin(uuid, ip);
                authenticated.add(uuid);
                p.sendMessage("§aAuthentification réussie. Bon jeu !");
                
                fr.gens.core.modules.discord.DiscordModule discord = (fr.gens.core.modules.discord.DiscordModule) plugin.getModuleManager().getModule("discord");
                if (discord != null && discord.isEnabled()) {
                    discord.logAuthEvent(p.getName(), "Connexion Réussie", java.awt.Color.GREEN);
                }
            } else {
                p.sendMessage("§cMot de passe incorrect.");
                String discordId = plugin.getDatabaseManager().getDiscordId(uuid);
                if (discordId != null && !discordId.isEmpty()) {
                    p.sendMessage("§eMot de passe oublié ? Utilisez la commande §b/resetpassword §esur notre serveur Discord pour le réinitialiser !");
                }
            }
            return true;
        }

        if (command.getName().equalsIgnoreCase("changemdp")) {
            if (args.length != 2) {
                p.sendMessage("§cUtilisation: /changemdp <ancien> <nouveau>");
                return true;
            }
            if (!authenticated.contains(p.getUniqueId())) {
                p.sendMessage("§cVous devez être connecté pour faire ça.");
                return true;
            }
            String oldPass = args[0];
            String newPass = args[1];

            DatabaseManager.AuthData data = plugin.getDatabaseManager().getAuthData(uuid);
            if (data != null) {
                String oldHash = hashPassword(oldPass, data.salt);
                if (oldHash.equals(data.hash)) {
                    if (newPass.length() < 4) {
                        p.sendMessage("§cLe nouveau mot de passe doit faire au moins 4 caractères.");
                        return true;
                    }
                    String newSalt = generateSalt();
                    String newHash = hashPassword(newPass, newSalt);
                    plugin.getDatabaseManager().updatePassword(uuid, newHash, newSalt);
                    p.sendMessage("§aVotre mot de passe a été mis à jour avec succès !");
                } else {
                    p.sendMessage("§cL'ancien mot de passe est incorrect.");
                }
            }
            return true;
        }

        return false;
    }

    private void requireAuth(Player p) {
        DatabaseManager.AuthData data = plugin.getDatabaseManager().getAuthData(p.getUniqueId());
        if (data == null) {
            p.sendMessage("§c=============================");
            p.sendMessage("§cBienvenue sur GensBien !");
            p.sendMessage("§eVeuillez vous enregistrer avec:");
            p.sendMessage("§b/register <mdp> <confirmer mdp>");
            p.sendMessage("§c=============================");
        } else {
            p.sendMessage("§c=============================");
            p.sendMessage("§cBienvenue sur GensBien !");
            p.sendMessage("§eVeuillez vous connecter avec:");
            p.sendMessage("§b/login <mdp>");
            p.sendMessage("§c=============================");
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
                p.sendMessage("§aConnecté automatiquement.");
                
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
        SecureRandom random = new SecureRandom();
        byte[] saltBytes = new byte[16];
        random.nextBytes(saltBytes);
        return Base64.getEncoder().encodeToString(saltBytes);
    }

    public static String hashPassword(String password, String salt) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(salt.getBytes());
            byte[] hashedBytes = md.digest(password.getBytes());
            return Base64.getEncoder().encodeToString(hashedBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 non supporté", e);
        }
    }
}
