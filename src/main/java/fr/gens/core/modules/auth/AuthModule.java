package fr.gens.core.modules.auth;

import fr.gens.core.CorePlugin;
import fr.gens.core.modules.Module;
import fr.gens.core.database.AuthDAO;
import io.papermc.paper.event.player.AsyncChatEvent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
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
import java.util.ArrayList;
import java.util.Base64;


import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.mindrot.jbcrypt.BCrypt;

import cloud.commandframework.annotations.Argument;
import cloud.commandframework.annotations.CommandMethod;

public class AuthModule implements Module, Listener {

    private CorePlugin plugin;
    private boolean enabled;
    private AuthDAO authDAO;
    private final Set<UUID> authenticated = java.util.concurrent.ConcurrentHashMap.newKeySet();
    private final long SESSION_TIMEOUT = 30L * 24L * 60L * 60L * 1000L; // 30 days

    // Rate-limiting sur /login
    private final Map<UUID, Integer> loginAttempts = new java.util.concurrent.ConcurrentHashMap<>();
    private final Map<UUID, Long>    loginLockout  = new java.util.concurrent.ConcurrentHashMap<>();
    private static final int  MAX_LOGIN_ATTEMPTS = 5;
    private static final long LOGIN_LOCKOUT_MS   = 5L * 60 * 1000; // 5 minutes

    // TÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ches planifiées gérées par ce module
    private final java.util.List<com.tcoded.folialib.wrapper.task.WrappedTask> taskIds = new ArrayList<>();

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

    public AuthDAO getAuthDAO() {
        return authDAO;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void initDatabase(fr.gens.core.utils.DatabaseManager dbManager) {
        dbManager.executeStatement("CREATE TABLE IF NOT EXISTS genscore_auth (uuid VARCHAR(36) PRIMARY KEY, password_hash VARCHAR(255) NOT NULL, salt VARCHAR(255) NOT NULL, last_ip VARCHAR(50), last_login BIGINT DEFAULT 0);");
    }

    @Override
    public void enable() {
        this.enabled = true;
        this.authDAO = new AuthDAO(plugin);
        this.authDAO.initDatabase();
        Bukkit.getPluginManager().registerEvents(this, plugin);
        plugin.getLangManager().sendConsoleMessage("authmodule.log_1");
    }

    @Override
    public void disable() {
        org.bukkit.event.HandlerList.unregisterAll(this);
        this.enabled = false;
        authenticated.clear();
        loginAttempts.clear();
        loginLockout.clear();
        // Annuler uniquement les tÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ches de ce module (et non toutes les tÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¢ches du plugin)
        taskIds.forEach(com.tcoded.folialib.wrapper.task.WrappedTask::cancel);
        taskIds.clear();
        plugin.getLangManager().sendConsoleMessage("authmodule.log_2");
    }
    
    @Override
    public void registerCommands(fr.gens.core.CorePlugin plugin) {
        System.out.println("[DEBUG] Registering commands for AuthModule...");
        if (plugin.getCommandManager() != null && plugin.getCommandManager().getAnnotationParser() != null) {
            plugin.getCommandManager().getAnnotationParser().parse(this);
            System.out.println("[DEBUG] Commands registered for AuthModule!");
        }
    }

    public void forceLogout(UUID uuid) {
        authenticated.remove(uuid);
        Player p = Bukkit.getPlayer(uuid);
        if (p != null && p.isOnline()) {
            plugin.getLangManager().sendMessage(p, "authmodule.msg_1");
            plugin.getLangManager().sendMessage(p, "authmodule.msg_2");
        }
    }

    public void executeRegister(org.bukkit.command.CommandSender sender, String password, String confirm) {
        if (!(sender instanceof org.bukkit.entity.Player)) return;
        org.bukkit.entity.Player p = (org.bukkit.entity.Player) sender;
        if (!enabled) return;
        UUID uuid = p.getUniqueId();

        if (authenticated.contains(uuid)) {
            plugin.getLangManager().sendMessage(p, "authmodule.msg_3");
            return;
        }
        plugin.getFoliaLib().getScheduler().runAsync((wrappedTask) -> {
            AuthDAO.AuthData data = authDAO.getAuthData(uuid);
            if (data != null) {
                plugin.getFoliaLib().getScheduler().runAtEntity(p, (t) -> plugin.getLangManager().sendMessage(p, "authmodule.msg_4"));
                return;
            }
            if (!password.equals(confirm)) {
                plugin.getFoliaLib().getScheduler().runAtEntity(p, (t) -> plugin.getLangManager().sendMessage(p, "authmodule.msg_6"));
                return;
            }
            if (password.length() < 8) {
                plugin.getFoliaLib().getScheduler().runAtEntity(p, (t) -> plugin.getLangManager().sendMessage(p, "authmodule.msg_7"));
                return;
            }

            String salt = ""; // Non utilisé pour BCrypt mais gardé pour compatibilité BDD
            String hash = BCrypt.hashpw(password, BCrypt.gensalt());
            java.net.InetSocketAddress addr = p.getAddress();
            String ip = (addr != null && addr.getAddress() != null) ? addr.getAddress().getHostAddress() : "0.0.0.0";

            authDAO.registerPlayer(uuid, hash, salt, ip);
            authenticated.add(uuid);
            plugin.getFoliaLib().getScheduler().runAtEntity(p, (t) -> plugin.getLangManager().sendMessage(p, "authmodule.msg_8"));
            
            fr.gens.core.modules.discord.DiscordModule discord = (fr.gens.core.modules.discord.DiscordModule) plugin.getModuleManager().getModule("discord");
            if (discord != null && discord.isEnabled()) {
                discord.logAuthEvent(p.getName(), "Nouvel Enregistrement", new java.awt.Color(0, 200, 255));
            }
        });
    }

    public void executeLogin(org.bukkit.command.CommandSender sender, String password) {
        if (!(sender instanceof org.bukkit.entity.Player)) return;
        org.bukkit.entity.Player p = (org.bukkit.entity.Player) sender;
        if (!enabled) return;
        UUID uuid = p.getUniqueId();

        if (authenticated.contains(uuid)) {
            plugin.getLangManager().sendMessage(p, "authmodule.msg_9");
            return;
        }

        plugin.getFoliaLib().getScheduler().runAsync((wrappedTask) -> {
            // --- Rate-limiting : vérifier le lockout avant toute chose ---
            if (loginLockout.containsKey(uuid)) {
                long remaining = (loginLockout.get(uuid) + LOGIN_LOCKOUT_MS) - System.currentTimeMillis();
                if (remaining > 0) {
                    long minutes = remaining / 60000;
                    plugin.getFoliaLib().getScheduler().runAtEntity(p, (t) -> {
                        p.sendMessage(fr.gens.core.utils.PlaceholderUtils.parseToComponent(
                            "<red>Trop de tentatives. Réessayez dans <bold>" + (minutes + 1) + " min</bold>.</red>"
                        ));
                    });
                    return;
                } else {
                    loginLockout.remove(uuid);
                    loginAttempts.remove(uuid);
                }
            }
            // --- Fin rate-limiting ---

            AuthDAO.AuthData data = authDAO.getAuthData(uuid);
            if (data == null) {
                plugin.getFoliaLib().getScheduler().runAtEntity(p, (t) -> plugin.getLangManager().sendMessage(p, "authmodule.msg_10"));
                return;
            }
            
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
                java.net.InetSocketAddress addr = p.getAddress();
                String ip = (addr != null && addr.getAddress() != null) ? addr.getAddress().getHostAddress() : "0.0.0.0";
                
                if (needsMigration) {
                    String newHash = BCrypt.hashpw(password, BCrypt.gensalt());
                    authDAO.updatePassword(uuid, newHash, "");
                    plugin.getFoliaLib().getScheduler().runAtEntity(p, (t) -> plugin.getLangManager().sendMessage(p, "authmodule.msg_12"));
                }
                
                authDAO.updateLogin(uuid, ip);
                authenticated.add(uuid);
                // Réinitialiser les compteurs d'échecs
                loginAttempts.remove(uuid);
                loginLockout.remove(uuid);
                
                plugin.getFoliaLib().getScheduler().runAtEntity(p, (t) -> plugin.getLangManager().sendMessage(p, "authmodule.msg_13"));
                
                fr.gens.core.modules.discord.DiscordModule discord = (fr.gens.core.modules.discord.DiscordModule) plugin.getModuleManager().getModule("discord");
                if (discord != null && discord.isEnabled()) {
                    discord.logAuthEvent(p.getName(), "Connexion Réussie", java.awt.Color.GREEN);
                }
            } else {
                // Incrémenter le compteur d'échecs
                int attempts = loginAttempts.merge(uuid, 1, Integer::sum);
                if (attempts >= MAX_LOGIN_ATTEMPTS) {
                    loginLockout.put(uuid, System.currentTimeMillis());
                    loginAttempts.remove(uuid);
                    plugin.getFoliaLib().getScheduler().runAtEntity(p, (t) -> {
                        p.sendMessage(fr.gens.core.utils.PlaceholderUtils.parseToComponent(
                            "<red><bold>Compte temporairement verrouillé</bold> (5 tentatives). Réessayez dans 5 minutes.</red>"
                        ));
                    });
                } else {
                    plugin.getFoliaLib().getScheduler().runAtEntity(p, (t) -> plugin.getLangManager().sendMessage(p, "authmodule.msg_14"));
                    fr.gens.core.modules.stats.StatsModule statsModule = (fr.gens.core.modules.stats.StatsModule) plugin.getModuleManager().getModule("stats");
                    String discordId = statsModule != null ? statsModule.getStatsDAO().getDiscordId(uuid) : null;
                    if (discordId != null && !discordId.isEmpty()) {
                        plugin.getFoliaLib().getScheduler().runAtEntity(p, (t) -> plugin.getLangManager().sendMessage(p, "authmodule.msg_15"));
                    }
                }
            }
        });
    }

    @CommandMethod("changemdp <oldPass> <newPass>")
    public void executeChangeMdp(org.bukkit.command.CommandSender sender, @Argument("oldPass") String oldPass, @Argument("newPass") String newPass) {
        if (!(sender instanceof org.bukkit.entity.Player)) return;
        org.bukkit.entity.Player p = (org.bukkit.entity.Player) sender;
        if (!enabled) return;
        UUID uuid = p.getUniqueId();

        if (!authenticated.contains(uuid)) {
            plugin.getLangManager().sendMessage(p, "authmodule.msg_17");
            return;
        }

        plugin.getFoliaLib().getScheduler().runAsync((wrappedTask) -> {
            AuthDAO.AuthData data = authDAO.getAuthData(uuid);
            if (data != null) {
                boolean isOldPasswordCorrect = false;
                if (data.hash.startsWith("$2a$") || data.hash.startsWith("$2b$") || data.hash.startsWith("$2y$")) {
                    isOldPasswordCorrect = BCrypt.checkpw(oldPass, data.hash);
                } else {
                    String oldHash = oldHashPassword(oldPass, data.salt);
                    isOldPasswordCorrect = oldHash.equals(data.hash);
                }
                
                if (isOldPasswordCorrect) {
                    if (newPass.length() < 8) {
                        plugin.getFoliaLib().getScheduler().runAtEntity(p, (t) -> plugin.getLangManager().sendMessage(p, "authmodule.msg_18"));
                        return;
                    }
                    String newHash = BCrypt.hashpw(newPass, BCrypt.gensalt());
                    authDAO.updatePassword(uuid, newHash, "");
                    plugin.getFoliaLib().getScheduler().runAtEntity(p, (t) -> plugin.getLangManager().sendMessage(p, "authmodule.msg_19"));
                } else {
                    plugin.getFoliaLib().getScheduler().runAtEntity(p, (t) -> plugin.getLangManager().sendMessage(p, "authmodule.msg_20"));
                }
            }
        });
    }

    private void requireAuth(Player p) {
        plugin.getFoliaLib().getScheduler().runAsync((wrappedTask) -> {
            AuthDAO.AuthData data = authDAO.getAuthData(p.getUniqueId());
            plugin.getFoliaLib().getScheduler().runAtEntity(p, (t) -> {
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
            });
        });
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (!enabled) return;
        Player p = event.getPlayer();
        UUID uuid = p.getUniqueId();
        
        plugin.getFoliaLib().getScheduler().runAsync((wrappedTask) -> {
            AuthDAO.AuthData data = authDAO.getAuthData(uuid);
            if (data != null) {
                java.net.InetSocketAddress addr2 = p.getAddress();
                String currentIp = (addr2 != null && addr2.getAddress() != null) ? addr2.getAddress().getHostAddress() : "0.0.0.0";
                long timeSinceLastLogin = System.currentTimeMillis() - data.lastLogin;
                
                if (currentIp.equals(data.lastIp) && timeSinceLastLogin < SESSION_TIMEOUT) {
                    authenticated.add(uuid);
                    authDAO.updateLogin(uuid, currentIp);
                    
                    plugin.getFoliaLib().getScheduler().runAtEntity(p, (t) -> {
                        plugin.getLangManager().sendMessage(p, "authmodule.msg_31");
                    });
                    
                    fr.gens.core.modules.discord.DiscordModule discord = (fr.gens.core.modules.discord.DiscordModule) plugin.getModuleManager().getModule("discord");
                    if (discord != null && discord.isEnabled()) {
                        discord.logAuthEvent(p.getName(), "Connexion Automatique", java.awt.Color.GREEN);
                    }
                    return;
                }
            }
            
            // Not authenticated
            requireAuth(p);
        });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        authenticated.remove(uuid);
        loginAttempts.remove(uuid);
        loginLockout.remove(uuid);
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
    public void onChat(AsyncChatEvent event) {
        if (!enabled) return;
        if (!isAuth(event.getPlayer())) {
            event.setCancelled(true);
            requireAuth(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onCommandPreprocess(PlayerCommandPreprocessEvent event) {
        String message = event.getMessage();
        if (message.length() <= 1) return;
        String cmdArgs = message.substring(1);
        String cmdName = cmdArgs.split(" ")[0].toLowerCase();

        // 1. Auth check
        if (cmdName.equals("login") || cmdName.equals("l") || cmdName.equals("register") || cmdName.equals("reg")) {
            event.setCancelled(true);
            String[] args = cmdArgs.split(" ");
            if (args.length < 2) {
                plugin.getLangManager().sendMessage(event.getPlayer(), "error.invalid_syntax");
                return;
            }
            if (cmdName.equals("login") || cmdName.equals("l")) {
                executeLogin(event.getPlayer(), args[1]);
            } else {
                executeRegister(event.getPlayer(), args[1], args.length > 2 ? args[2] : args[1]);
            }
            return;
        }

        if (!isAuth(event.getPlayer())) {
            event.setCancelled(true);
            requireAuth(event.getPlayer());
            return;
        }

        // 2. Force execution via Cloud (Paper 26.2+ fix)
        cloud.commandframework.paper.PaperCommandManager<org.bukkit.command.CommandSender> mgr = plugin.getCommandManager().getPaperCommandManager();
        if (mgr != null) {
            try {
                cloud.commandframework.CommandTree<org.bukkit.command.CommandSender> tree = mgr.getCommandTree();
                if (tree != null) {
                    java.util.Collection<cloud.commandframework.Command<org.bukkit.command.CommandSender>> cmds = mgr.getCommands();
                    boolean isCloudCmd = false;
                    for (cloud.commandframework.Command<org.bukkit.command.CommandSender> c : cmds) {
                        if (c.getArguments().isEmpty()) continue;
                        cloud.commandframework.arguments.CommandArgument<org.bukkit.command.CommandSender, ?> firstArg = c.getArguments().get(0);
                        if (firstArg.getName().equalsIgnoreCase(cmdName)) {
                            isCloudCmd = true;
                            break;
                        }
                        if (firstArg instanceof cloud.commandframework.arguments.StaticArgument) {
                            cloud.commandframework.arguments.StaticArgument<?> staticArg = (cloud.commandframework.arguments.StaticArgument<?>) firstArg;
                            if (staticArg.getAlternativeAliases().contains(cmdName.toLowerCase())) {
                                isCloudCmd = true;
                                break;
                            }
                        }
                    }
                    
                    if (isCloudCmd) {
                        event.setCancelled(true); // Stop Bukkit from trying to run it
                        mgr.executeCommand(event.getPlayer(), cmdArgs).whenComplete((res, err) -> {
                            if (err != null) {
                                if (err instanceof java.util.concurrent.CompletionException) {
                                    err = err.getCause();
                                }
                                
                                if (err instanceof cloud.commandframework.exceptions.InvalidSyntaxException) {
                                    event.getPlayer().sendMessage(fr.gens.core.utils.PlaceholderUtils.parseToComponent("<red>Erreur de syntaxe. Utilisation correcte : <yellow>" + ((cloud.commandframework.exceptions.InvalidSyntaxException) err).getCorrectSyntax()));
                                } else if (err instanceof cloud.commandframework.exceptions.NoPermissionException) {
                                    event.getPlayer().sendMessage(fr.gens.core.utils.PlaceholderUtils.parseToComponent("<red>Vous n'avez pas la permission d'exécuter cette commande."));
                                } else if (err instanceof cloud.commandframework.exceptions.ArgumentParseException) {
                                    event.getPlayer().sendMessage(fr.gens.core.utils.PlaceholderUtils.parseToComponent("<red>Argument invalide : " + err.getCause().getMessage()));
                                } else if (err instanceof cloud.commandframework.exceptions.InvalidCommandSenderException) {
                                    event.getPlayer().sendMessage(fr.gens.core.utils.PlaceholderUtils.parseToComponent("<red>Vous ne pouvez pas exécuter cette commande."));
                                } else if (err instanceof cloud.commandframework.exceptions.CommandExecutionException) {
                                    event.getPlayer().sendMessage(fr.gens.core.utils.PlaceholderUtils.parseToComponent("<red>Une erreur interne est survenue lors de l'exécution de la commande."));
                                    err.getCause().printStackTrace();
                                } else if (!(err instanceof cloud.commandframework.exceptions.NoSuchCommandException)) {
                                    err.printStackTrace();
                                }
                            }
                        });
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
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
        // N'est plus appelé qu'historiquement, BCrypt gÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¨re le hash maintenant.
        // On retourne la version BCrypt par défaut.
        return BCrypt.hashpw(password, BCrypt.gensalt());
    }
}










