package fr.gens.core.modules;

import fr.gens.core.CorePlugin;
import fr.gens.core.modules.GuiModule.GensGuiHolder;
import fr.gens.core.utils.TeleportUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.incendo.cloud.annotations.Argument;
import org.incendo.cloud.annotations.Default;
import org.incendo.cloud.annotations.Command;
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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import java.util.Map;
import java.util.UUID;


public class TeleportHomeModule implements Module, Listener {

    private final CorePlugin plugin;
    private boolean enabled = false;
    private final Map<UUID, Map<String, Location>> homes = new ConcurrentHashMap<>();
    
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
        return "Commandes /home avec GUI sécurisée, limites et cooldowns.";
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
        
        // Chargement lazy : les homes sont chargés à la connexion du joueur
        Bukkit.getPluginManager().registerEvents(this, plugin);
        // Précharger les homes des joueurs déjà connectés (reload en jeu)
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p == null) continue;
            final UUID uuid = p.getUniqueId();
            plugin.getFoliaLib().getScheduler().runAsync((wrappedTask) -> loadHomesForPlayer(uuid));
        }
        plugin.getLangManager().sendConsoleMessage("teleporthomemodule.log_1");
    }

    @Override
    public void registerCommands(fr.gens.core.CorePlugin plugin) {
        if (plugin.getCommandManager() != null && plugin.getCommandManager().getAnnotationParser() != null) {
            plugin.getCommandManager().getAnnotationParser().parse(this);
        }
    }

    @Override
    public void disable() {
        enabled = false;
        homes.clear();
        HandlerList.unregisterAll(this);
        plugin.getLangManager().sendConsoleMessage("teleporthomemodule.log_2");
    }


    private void saveHomeToDB(UUID uuid, String name, Location loc) {
        plugin.getFoliaLib().getScheduler().runAsync((wrappedTask) -> {
            homeDAO.saveHome(uuid, name, loc);
        });
    }

    private void deleteHomeFromDB(UUID uuid, String name) {
        plugin.getFoliaLib().getScheduler().runAsync((wrappedTask) -> {
            homeDAO.deleteHome(uuid, name);
        });
    }

    public void clearAllHomes() {
        homes.clear();
        plugin.getFoliaLib().getScheduler().runAsync((wrappedTask) -> {
            homeDAO.clearAllHomes();
        });
    }
    // Chargement lazy : uniquement les homes d'un joueur donné
    private void loadHomesForPlayer(UUID uuid) {
        if (homes.containsKey(uuid)) return; // Déjà chargé
        Map<String, Location> playerHomes = homeDAO.loadPlayerHomes(uuid);
        homes.put(uuid, playerHomes);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (!enabled) return;
        final UUID uuid = event.getPlayer().getUniqueId();
        plugin.getFoliaLib().getScheduler().runAsync((wrappedTask) -> loadHomesForPlayer(uuid));
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

    @Command("sethome [name]")
    public void executeSetHome(org.bukkit.command.CommandSender sender, @Argument(value = "name", description = "Le nom de la maison") @Default("maison") String homeName) {
        if (!(sender instanceof org.bukkit.entity.Player)) return;
        org.bukkit.entity.Player p = (org.bukkit.entity.Player) sender;
        if (!enabled) {
            p.sendMessage(fr.gens.core.utils.PlaceholderUtils.parseToComponent("<red>Ce module est désactivé.</red>"));
            return;
        }
        if (!p.hasPermission("genscore.home")) {
            plugin.getLangManager().sendMessage(p, "error.no_permission");
            return;
        }
        
        Map<String, Location> playerHomes = homes.computeIfAbsent(p.getUniqueId(), k -> new ConcurrentHashMap<>());
        
        if (!playerHomes.containsKey(homeName) && playerHomes.size() >= getMaxHomes(p)) {
            plugin.getLangManager().sendMessage(p, "home.limit_reached",
                net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.parsed("max", String.valueOf(getMaxHomes(p)))
            );
            return;
        }

        playerHomes.put(homeName, p.getLocation());
        saveHomeToDB(p.getUniqueId(), homeName, p.getLocation());
        plugin.getLangManager().sendMessage(p, "home.set", 
            net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.parsed("name", homeName)
        );
    }

    @org.incendo.cloud.annotations.suggestion.Suggestions("homeNames")
    public java.util.List<String> homeNames(org.incendo.cloud.context.CommandContext<org.bukkit.command.CommandSender> context, String input) {
        if (!(context.sender() instanceof org.bukkit.entity.Player)) return java.util.Collections.emptyList();
        org.bukkit.entity.Player p = (org.bukkit.entity.Player) context.sender();
        Map<String, Location> playerHomes = homes.get(p.getUniqueId());
        if (playerHomes == null) return java.util.Collections.emptyList();
        return new java.util.ArrayList<>(playerHomes.keySet());
    }

    @Command("home [name]")
    public void executeHome(org.bukkit.command.CommandSender sender, @Argument(value = "name", suggestions = "homeNames", description = "Le nom de la maison") @Default("") String homeName) {
        if (!(sender instanceof org.bukkit.entity.Player)) return;
        org.bukkit.entity.Player p = (org.bukkit.entity.Player) sender;
        if (!enabled) {
            p.sendMessage(fr.gens.core.utils.PlaceholderUtils.parseToComponent("<red>Ce module est désactivé.</red>"));
            return;
        }
        if (!p.hasPermission("genscore.home")) {
            plugin.getLangManager().sendMessage(p, "error.no_permission");
            return;
        }
        
        if (homeName != null && !homeName.isEmpty()) {
            Map<String, Location> playerHomes = homes.get(p.getUniqueId());
            if (playerHomes != null && playerHomes.containsKey(homeName)) {
                TeleportUtil.teleportWithCooldown(plugin, p, playerHomes.get(homeName), "le home " + homeName, "genscore.bypass.cooldown.home");
            } else {
                plugin.getLangManager().sendMessage(p, "home.not_found", 
                    net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.parsed("name", homeName)
                );
            }
            return;
        }
        openHomeGui(p);
    }

    @Command("delhome <name>")
    public void executeDelHome(org.bukkit.command.CommandSender sender, @Argument(value = "name", suggestions = "homeNames", description = "Le nom de la maison") String homeName) {
        if (!(sender instanceof org.bukkit.entity.Player)) return;
        org.bukkit.entity.Player p = (org.bukkit.entity.Player) sender;
        if (!enabled) {
            p.sendMessage(fr.gens.core.utils.PlaceholderUtils.parseToComponent("<red>Ce module est désactivé.</red>"));
            return;
        }
        if (!p.hasPermission("genscore.home")) {
            plugin.getLangManager().sendMessage(p, "error.no_permission");
            return;
        }
        
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
    }

    private void openHomeGui(Player player) {
        if (fr.gens.core.utils.FloodgateUtil.isBedrockPlayer(player.getUniqueId())) {
            java.util.List<fr.gens.core.utils.BedrockFormManager.BedrockButton> buttons = new java.util.ArrayList<>();
            Map<String, Location> playerHomes = homes.getOrDefault(player.getUniqueId(), new ConcurrentHashMap<>());
            
            for (String homeName : playerHomes.keySet()) {
                String btnText = "§a" + homeName + "\n§8Cliquez pour gérer ce home";
                buttons.add(new fr.gens.core.utils.BedrockFormManager.BedrockButton(btnText, Material.WHITE_BED, p -> {
                    openConfirmGui(p, homeName);
                }));
            }
            
            fr.gens.core.utils.BedrockFormManager.openSimpleForm(player, "Mes Homes", "Gérez vos " + playerHomes.size() + "/" + getMaxHomes(player) + " homes :", buttons);
            return;
        }

        HomeGuiHolder holder = new HomeGuiHolder();
        Inventory inv = Bukkit.createInventory(holder, 27, fr.gens.core.utils.PlaceholderUtils.parseToComponent("<dark_gray>Mes Homes (" + homes.getOrDefault(player.getUniqueId(), new ConcurrentHashMap<>()).size() + "/" + getMaxHomes(player) + ")"));
        holder.setInventory(inv);

        Map<String, Location> playerHomes = homes.getOrDefault(player.getUniqueId(), new ConcurrentHashMap<>());
        
        int slot = 0;
        for (String homeName : playerHomes.keySet()) {
            ItemStack item = new ItemStack(Material.WHITE_BED);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.displayName(fr.gens.core.utils.PlaceholderUtils.parseToComponent("<green><bold>" + homeName));
                List<String> lore = new ArrayList<>();
                lore.add("<gray>Cliquez pour Gérer ce Home");
                meta.lore(java.util.Optional.ofNullable(lore).orElse(java.util.Collections.emptyList()).stream().map(s -> fr.gens.core.utils.PlaceholderUtils.parseToComponent((String)s)).collect(java.util.stream.Collectors.toList()));
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
        if (fr.gens.core.utils.FloodgateUtil.isBedrockPlayer(player.getUniqueId())) {
            java.util.List<fr.gens.core.utils.BedrockFormManager.BedrockButton> buttons = new java.util.ArrayList<>();
            
            buttons.add(new fr.gens.core.utils.BedrockFormManager.BedrockButton("§a§lSe Téléporter", Material.ENDER_PEARL, p -> {
                Map<String, Location> playerHomes = homes.get(p.getUniqueId());
                if (playerHomes != null && playerHomes.containsKey(homeName)) {
                    TeleportUtil.teleportWithCooldown(plugin, p, playerHomes.get(homeName), "le home " + homeName, "genscore.bypass.cooldown.home");
                }
            }));
            
            buttons.add(new fr.gens.core.utils.BedrockFormManager.BedrockButton("§c§lSupprimer", Material.RED_CONCRETE, p -> {
                Map<String, Location> playerHomes = homes.get(p.getUniqueId());
                if (playerHomes != null && playerHomes.containsKey(homeName)) {
                    playerHomes.remove(homeName);
                    deleteHomeFromDB(p.getUniqueId(), homeName);
                    plugin.getLangManager().sendMessage(p, "home.delete", 
                        net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.parsed("name", homeName)
                    );
                    openHomeGui(p);
                }
            }));
            
            buttons.add(new fr.gens.core.utils.BedrockFormManager.BedrockButton("§e§lRetour aux Homes", Material.BARRIER, p -> {
                openHomeGui(p);
            }));
            
            fr.gens.core.utils.BedrockFormManager.openSimpleForm(player, "Gestion: " + homeName, "Que voulez-vous faire avec le home " + homeName + " ?", buttons);
            return;
        }

        ConfirmHomeGuiHolder holder = new ConfirmHomeGuiHolder(homeName);
        Inventory inv = Bukkit.createInventory(holder, 9, fr.gens.core.utils.PlaceholderUtils.parseToComponent("<dark_gray>Gestion: " + homeName));
        holder.setInventory(inv);

        // Bloc vert (Téléportation)
        ItemStack tpItem = new ItemStack(Material.LIME_CONCRETE);
        ItemMeta tpMeta = tpItem.getItemMeta();
        tpMeta.displayName(fr.gens.core.utils.PlaceholderUtils.parseToComponent("<green><bold>Se Téléporter"));
        tpItem.setItemMeta(tpMeta);
        inv.setItem(2, tpItem);

        // Bloc rouge (Suppression)
        ItemStack delItem = new ItemStack(Material.RED_CONCRETE);
        ItemMeta delMeta = delItem.getItemMeta();
        delMeta.displayName(fr.gens.core.utils.PlaceholderUtils.parseToComponent("<red><bold>Supprimer"));
        delItem.setItemMeta(delMeta);
        inv.setItem(6, delItem);

        // Bouton retour
        ItemStack backItem = new ItemStack(Material.BARRIER);
        ItemMeta backMeta = backItem.getItemMeta();
        backMeta.displayName(fr.gens.core.utils.PlaceholderUtils.parseToComponent("<yellow><bold>Retour aux Homes"));
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

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!enabled) return;
        if (event.getView().getTopInventory().getHolder() instanceof GensGuiHolder) {
            GensGuiHolder holder = (GensGuiHolder) event.getView().getTopInventory().getHolder();
            if (event.getClickedInventory() != null && event.getClickedInventory().equals(event.getView().getTopInventory())) {
                holder.onClick(event);
            } else if (event.getAction() == org.bukkit.event.inventory.InventoryAction.MOVE_TO_OTHER_INVENTORY || event.getAction() == org.bukkit.event.inventory.InventoryAction.HOTBAR_SWAP) {
                event.setCancelled(true);
            }
        }
    }
}




