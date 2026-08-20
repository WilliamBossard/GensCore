package fr.gens.core.modules;

import fr.gens.core.CorePlugin;
import fr.gens.core.modules.GuiModule.GensGuiHolder;
import fr.gens.core.utils.TeleportUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.NamespacedKey;

import org.bukkit.command.TabCompleter;





import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;


public class TeleportHomeModule implements Module, CommandExecutor, TabCompleter, Listener {

    private final CorePlugin plugin;
    private boolean enabled = false;
    private final Map<UUID, Map<String, Location>> homes = new HashMap<>();
    
    private fr.gens.core.database.HomeDAO homeDAO;

    public TeleportHomeModule(CorePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "home";
    }

    @Override
    public String getDescription() {
        return "Commandes /home avec GUI sÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â©curisÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â©e, limites et cooldowns.";
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void initDatabase(fr.gens.core.utils.DatabaseManager dbManager) {
        dbManager.executeStatement("CREATE TABLE IF NOT EXISTS spawn_location (id INTEGER PRIMARY KEY DEFAULT 1, world VARCHAR(255) NOT NULL, x DOUBLE NOT NULL, y DOUBLE NOT NULL, z DOUBLE NOT NULL, yaw FLOAT NOT NULL, pitch FLOAT NOT NULL);");
        dbManager.executeStatement("CREATE TABLE IF NOT EXISTS player_homes (uuid VARCHAR(36) NOT NULL, name VARCHAR(50) NOT NULL, world VARCHAR(255) NOT NULL, x DOUBLE NOT NULL, y DOUBLE NOT NULL, z DOUBLE NOT NULL, yaw FLOAT NOT NULL, pitch FLOAT NOT NULL, PRIMARY KEY (uuid, name));");
        dbManager.executeStatement("CREATE INDEX IF NOT EXISTS idx_player_homes_uuid ON player_homes(uuid);");
    }

    @Override
    public void enable() {
        enabled = true;
        
        this.homeDAO = new fr.gens.core.database.HomeDAO(plugin);
        this.homeDAO.initDatabase();
        
        // Chargement lazy : les homes sont chargÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â©s ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â  la connexion du joueur
        Bukkit.getPluginManager().registerEvents(this, plugin);
        // PrÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â©charger les homes des joueurs dÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â©jÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â  connectÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â©s (reload en jeu)
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p == null) continue;
            final UUID uuid = p.getUniqueId();
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> loadHomesForPlayer(uuid));
        }
        plugin.getLangManager().sendConsoleMessage("teleporthomemodule.log_1");
    }

    @Override
    public void registerCommands(fr.gens.core.CorePlugin plugin) {
        org.bukkit.command.PluginCommand sethomeCmd = plugin.getCommand("sethome");
        if (sethomeCmd != null) { sethomeCmd.setExecutor(this); }
        
        org.bukkit.command.PluginCommand homeCmd = plugin.getCommand("home");
        if (homeCmd != null) { homeCmd.setExecutor(this); homeCmd.setTabCompleter(this); }
        
        org.bukkit.command.PluginCommand delhomeCmd = plugin.getCommand("delhome");
        if (delhomeCmd != null) { delhomeCmd.setExecutor(this); delhomeCmd.setTabCompleter(this); }
    }

    @Override
    public void disable() {
        enabled = false;
        homes.clear();
        HandlerList.unregisterAll(this);
        plugin.getLangManager().sendConsoleMessage("teleporthomemodule.log_2");
    }


    private void saveHomeToDB(UUID uuid, String name, Location loc) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            homeDAO.saveHome(uuid, name, loc);
        });
    }

    private void deleteHomeFromDB(UUID uuid, String name) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            homeDAO.deleteHome(uuid, name);
        });
    }

    public void clearAllHomes() {
        homes.clear();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            homeDAO.clearAllHomes();
        });
    }
    // Chargement lazy : uniquement les homes d'un joueur donnÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â©
    private void loadHomesForPlayer(UUID uuid) {
        if (homes.containsKey(uuid)) return; // DÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â©jÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â  chargÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â©
        Map<String, Location> playerHomes = homeDAO.loadPlayerHomes(uuid);
        homes.put(uuid, playerHomes);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (!enabled) return;
        final UUID uuid = event.getPlayer().getUniqueId();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> loadHomesForPlayer(uuid));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        homes.remove(event.getPlayer().getUniqueId());
    }



    private int getMaxHomes(Player p) {
        int max = plugin.getConfigManager().getConfig("modules/teleport.yml").getInt("modules.home.default_max", 3); // Default
        for (PermissionAttachmentInfo perm : p.getEffectivePermissions()) {
            String pName = perm.getPermission();
            if (pName.startsWith("genscore.home.limit.")) {
                try {
                    int val = Integer.parseInt(pName.replace("genscore.home.limit.", ""));
                    if (val > max) max = val;
                } catch (Exception ignored) {}
            }
        }
        if (p.hasPermission("genscore.admin") || p.hasPermission("genscore.home.limit.*")) return 999;
        return max;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!enabled) {
            sender.sendMessage(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize("<red>Ce module est dÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â©sactivÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â©.</red>"));
            return true;
        }
        if (!(sender instanceof Player)) return true;
        Player p = (Player) sender;

        if (command.getName().equalsIgnoreCase("sethome")) {
            if (!p.hasPermission("genscore.home")) {
                plugin.getLangManager().sendMessage(p, "error.no_permission");
                return true;
            }
            
            String homeName = args.length > 0 ? args[0] : "maison";
            Map<String, Location> playerHomes = homes.computeIfAbsent(p.getUniqueId(), k -> new HashMap<>());
            
            if (!playerHomes.containsKey(homeName) && playerHomes.size() >= getMaxHomes(p)) {
                plugin.getLangManager().sendMessage(p, "home.limit_reached",
                    net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.parsed("max", String.valueOf(getMaxHomes(p)))
                );
                return true;
            }

            playerHomes.put(homeName, p.getLocation());
            saveHomeToDB(p.getUniqueId(), homeName, p.getLocation());
            plugin.getLangManager().sendMessage(p, "home.set", 
                net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.parsed("name", homeName)
            );
            return true;
        }

        if (command.getName().equalsIgnoreCase("home")) {
            if (!p.hasPermission("genscore.home")) {
                plugin.getLangManager().sendMessage(p, "error.no_permission");
                return true;
            }
            if (args.length > 0) {
                String homeName = args[0];
                Map<String, Location> playerHomes = homes.get(p.getUniqueId());
                if (playerHomes != null && playerHomes.containsKey(homeName)) {
                    TeleportUtil.teleportWithCooldown(plugin, p, playerHomes.get(homeName), "le home " + homeName, "genscore.bypass.cooldown.home");
                } else {
                    plugin.getLangManager().sendMessage(p, "home.not_found", 
                        net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.parsed("name", homeName)
                    );
                }
                return true;
            }
            openHomeGui(p);
            return true;
        }

        if (command.getName().equalsIgnoreCase("delhome")) {
            if (!p.hasPermission("genscore.home")) {
                plugin.getLangManager().sendMessage(p, "error.no_permission");
                return true;
            }
            if (args.length == 0) {
                p.sendMessage(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize("<red>Usage: /delhome <nom></red>"));
                return true;
            }
            String homeName = args[0];
            Map<String, Location> playerHomes = homes.get(p.getUniqueId());
            if (playerHomes != null && playerHomes.containsKey(homeName)) {
                playerHomes.remove(homeName);
                deleteHomeFromDB(p.getUniqueId(), homeName);
                plugin.getLangManager().sendMessage(p, "home.delete", 
                    net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.parsed("name", homeName)
                );
            } else {
                plugin.getLangManager().sendMessage(p, "home.not_found", 
                    net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.parsed("name", homeName)
                );
            }
            return true;
        }

        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!enabled || !(sender instanceof Player)) return Collections.emptyList();
        Player p = (Player) sender;
        
        if (command.getName().equalsIgnoreCase("home") || command.getName().equalsIgnoreCase("delhome")) {
            if (args.length == 1) {
                Map<String, Location> playerHomes = homes.get(p.getUniqueId());
                if (playerHomes == null) return Collections.emptyList();
                
                List<String> completions = new ArrayList<>();
                for (String homeName : playerHomes.keySet()) {
                    if (homeName.toLowerCase().startsWith(args[0].toLowerCase())) {
                        completions.add(homeName);
                    }
                }
                return completions;
            }
        }
        return Collections.emptyList();
    }

    private void openHomeGui(Player player) {
        HomeGuiHolder holder = new HomeGuiHolder();
        Inventory inv = Bukkit.createInventory(holder, 27, net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize("<dark_gray>Mes Homes (" + homes.getOrDefault(player.getUniqueId(), new HashMap<>()).size() + "/" + getMaxHomes(player) + ")"));
        holder.setInventory(inv);

        Map<String, Location> playerHomes = homes.getOrDefault(player.getUniqueId(), new HashMap<>());
        
        int slot = 0;
        for (String homeName : playerHomes.keySet()) {
            ItemStack item = new ItemStack(Material.WHITE_BED);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.displayName(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize("<green><bold>" + homeName));
                List<String> lore = new ArrayList<>();
                lore.add("<gray>Cliquez pour GÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â©rer ce Home");
                meta.lore(java.util.Optional.ofNullable(lore).orElse(java.util.Collections.emptyList()).stream().map(s -> net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize((String)s)).collect(java.util.stream.Collectors.toList()));
                NamespacedKey key = new NamespacedKey(plugin, "home_name");
                meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, homeName);
                item.setItemMeta(meta);
            }
            inv.setItem(slot++, item);
        }

        player.openInventory(inv);
    }

    private class HomeGuiHolder implements GensGuiHolder {
        private Inventory inventory;

        public void setInventory(Inventory inv) {
            this.inventory = inv;
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }

        @Override
        public void onClick(InventoryClickEvent event) {
            event.setCancelled(true);
            
            Player p = (Player) event.getWhoClicked();
            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || clicked.getType() != Material.WHITE_BED) return;
            if (clicked.getItemMeta() == null) return;

            NamespacedKey key = new NamespacedKey(plugin, "home_name");
            if (!clicked.getItemMeta().getPersistentDataContainer().has(key, PersistentDataType.STRING)) return;
            
            String homeName = clicked.getItemMeta().getPersistentDataContainer().get(key, PersistentDataType.STRING);
            Map<String, Location> playerHomes = homes.get(p.getUniqueId());
            
            if (playerHomes != null && playerHomes.containsKey(homeName)) {
                openConfirmGui(p, homeName);
            }
        }
    }

    private void openConfirmGui(Player player, String homeName) {
        ConfirmHomeGuiHolder holder = new ConfirmHomeGuiHolder(homeName);
        Inventory inv = Bukkit.createInventory(holder, 9, net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize("<dark_gray>Gestion: " + homeName));
        holder.setInventory(inv);

        // Bloc vert (TÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â©lÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â©portation)
        ItemStack tpItem = new ItemStack(Material.LIME_CONCRETE);
        ItemMeta tpMeta = tpItem.getItemMeta();
        tpMeta.displayName(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize("<green><bold>Se TÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â©lÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â©porter"));
        tpItem.setItemMeta(tpMeta);
        inv.setItem(2, tpItem);

        // Bloc rouge (Suppression)
        ItemStack delItem = new ItemStack(Material.RED_CONCRETE);
        ItemMeta delMeta = delItem.getItemMeta();
        delMeta.displayName(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize("<red><bold>Supprimer"));
        delItem.setItemMeta(delMeta);
        inv.setItem(6, delItem);

        // Bouton retour
        ItemStack backItem = new ItemStack(Material.BARRIER);
        ItemMeta backMeta = backItem.getItemMeta();
        backMeta.displayName(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize("<yellow><bold>Retour aux Homes"));
        backItem.setItemMeta(backMeta);
        inv.setItem(8, backItem);

        player.openInventory(inv);
    }

    private class ConfirmHomeGuiHolder implements GensGuiHolder {
        private Inventory inventory;
        private final String homeName;

        public ConfirmHomeGuiHolder(String homeName) {
            this.homeName = homeName;
        }

        public void setInventory(Inventory inv) {
            this.inventory = inv;
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }

        @Override
        public void onClick(InventoryClickEvent event) {
            event.setCancelled(true);
            
            Player p = (Player) event.getWhoClicked();
            ItemStack clicked = event.getCurrentItem();
            if (clicked == null) return;
            
            if (clicked.getType() == Material.LIME_CONCRETE) {
                p.closeInventory();
                Map<String, Location> playerHomes = homes.get(p.getUniqueId());
                if (playerHomes != null && playerHomes.containsKey(homeName)) {
                    TeleportUtil.teleportWithCooldown(plugin, p, playerHomes.get(homeName), "le home " + homeName, "genscore.bypass.cooldown.home");
                }
            } else if (clicked.getType() == Material.RED_CONCRETE) {
                Map<String, Location> playerHomes = homes.get(p.getUniqueId());
                if (playerHomes != null) {
                    playerHomes.remove(homeName);
                    deleteHomeFromDB(p.getUniqueId(), homeName);
                    plugin.getLangManager().sendMessage(p, "home.delete", 
                        net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.parsed("name", homeName)
                    );
                }
                openHomeGui(p);
            } else if (clicked.getType() == Material.BARRIER) {
                openHomeGui(p);
            }
        }
    }
}

