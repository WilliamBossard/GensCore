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
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.permissions.PermissionAttachmentInfo;

import org.bukkit.command.TabCompleter;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class TeleportHomeModule implements Module, CommandExecutor, TabCompleter {

    private final CorePlugin plugin;
    private boolean enabled = false;
    private final Map<UUID, Map<String, Location>> homes = new HashMap<>();

    public TeleportHomeModule(CorePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "CmdHome";
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
    public void enable() {
        enabled = true;
        loadHomes();
        plugin.getLogger().info("[CmdHome] Activé.");
    }

    @Override
    public void disable() {
        enabled = false;
        saveHomes();
        plugin.getLogger().info("[CmdHome] Désactivé.");
    }

    private void loadHomes() {
        homes.clear();
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM player_homes");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                UUID uuid = UUID.fromString(rs.getString("uuid"));
                String name = rs.getString("name");
                String worldName = rs.getString("world");
                if (Bukkit.getWorld(worldName) != null) {
                    Location loc = new Location(
                            Bukkit.getWorld(worldName),
                            rs.getDouble("x"),
                            rs.getDouble("y"),
                            rs.getDouble("z"),
                            rs.getFloat("yaw"),
                            rs.getFloat("pitch")
                    );
                    homes.computeIfAbsent(uuid, k -> new HashMap<>()).put(name, loc);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void saveHomeToDB(UUID uuid, String name, Location loc) {
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO player_homes (uuid, name, world, x, y, z, yaw, pitch) VALUES (?, ?, ?, ?, ?, ?, ?, ?) " +
                     "ON CONFLICT(uuid, name) DO UPDATE SET world=excluded.world, x=excluded.x, y=excluded.y, z=excluded.z, yaw=excluded.yaw, pitch=excluded.pitch")) {
            ps.setString(1, uuid.toString());
            ps.setString(2, name);
            ps.setString(3, loc.getWorld().getName());
            ps.setDouble(4, loc.getX());
            ps.setDouble(5, loc.getY());
            ps.setDouble(6, loc.getZ());
            ps.setFloat(7, loc.getYaw());
            ps.setFloat(8, loc.getPitch());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void deleteHomeFromDB(UUID uuid, String name) {
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM player_homes WHERE uuid = ? AND name = ?")) {
            ps.setString(1, uuid.toString());
            ps.setString(2, name);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void clearAllHomes() {
        homes.clear();
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM player_homes")) {
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    private void saveHomes() {
        // Sauvegarde synchrone (déjà gérée en temps réel, mais on pourrait tout dump ici si besoin)
    }

    private int getMaxHomes(Player p) {
        int max = plugin.getConfig().getInt("modules.home.default_max", 3); // Default
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
            sender.sendMessage("§cCe module est désactivé.");
            return true;
        }
        if (!(sender instanceof Player)) return true;
        Player p = (Player) sender;

        if (command.getName().equalsIgnoreCase("sethome")) {
            if (!p.hasPermission("genscore.home")) {
                p.sendMessage("§cPermission refusée (genscore.home).");
                return true;
            }
            
            String homeName = args.length > 0 ? args[0] : "maison";
            Map<String, Location> playerHomes = homes.computeIfAbsent(p.getUniqueId(), k -> new HashMap<>());
            
            if (!playerHomes.containsKey(homeName) && playerHomes.size() >= getMaxHomes(p)) {
                p.sendMessage("§cVous avez atteint votre limite de " + getMaxHomes(p) + " homes.");
                return true;
            }

            playerHomes.put(homeName, p.getLocation());
            saveHomeToDB(p.getUniqueId(), homeName, p.getLocation());
            p.sendMessage("§aHome §e" + homeName + " §adéfini et sauvegardé en SQL !");
            return true;
        }

        if (command.getName().equalsIgnoreCase("home")) {
            if (!p.hasPermission("genscore.home")) {
                p.sendMessage("§cPermission refusée (genscore.home).");
                return true;
            }
            if (args.length > 0) {
                String homeName = args[0];
                Map<String, Location> playerHomes = homes.get(p.getUniqueId());
                if (playerHomes != null && playerHomes.containsKey(homeName)) {
                    TeleportUtil.teleportWithCooldown(p, playerHomes.get(homeName), "le home " + homeName, "genscore.bypass.cooldown.home");
                } else {
                    p.sendMessage("§cLe home §e" + homeName + " §cn'existe pas.");
                }
                return true;
            }
            openHomeGui(p);
            return true;
        }

        if (command.getName().equalsIgnoreCase("delhome")) {
            if (!p.hasPermission("genscore.home")) {
                p.sendMessage("§cPermission refusée (genscore.home).");
                return true;
            }
            if (args.length == 0) {
                p.sendMessage("§cUsage: /delhome <nom>");
                return true;
            }
            String homeName = args[0];
            Map<String, Location> playerHomes = homes.get(p.getUniqueId());
            if (playerHomes != null && playerHomes.containsKey(homeName)) {
                playerHomes.remove(homeName);
                deleteHomeFromDB(p.getUniqueId(), homeName);
                p.sendMessage("§cHome " + homeName + " supprimé.");
            } else {
                p.sendMessage("§cLe home §e" + homeName + " §cn'existe pas.");
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
        Inventory inv = Bukkit.createInventory(holder, 27, "§8Mes Homes (" + homes.getOrDefault(player.getUniqueId(), new HashMap<>()).size() + "/" + getMaxHomes(player) + ")");
        holder.setInventory(inv);

        Map<String, Location> playerHomes = homes.getOrDefault(player.getUniqueId(), new HashMap<>());
        
        int slot = 0;
        for (String homeName : playerHomes.keySet()) {
            ItemStack item = new ItemStack(Material.WHITE_BED);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName("§a§l" + homeName);
                List<String> lore = new ArrayList<>();
                lore.add("§7Cliquez pour Gérer ce Home");
                meta.setLore(lore);
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

            String homeName = clicked.getItemMeta().getDisplayName().replace("§a§l", "");
            Map<String, Location> playerHomes = homes.get(p.getUniqueId());
            
            if (playerHomes != null && playerHomes.containsKey(homeName)) {
                openConfirmGui(p, homeName);
            }
        }
    }

    private void openConfirmGui(Player player, String homeName) {
        ConfirmHomeGuiHolder holder = new ConfirmHomeGuiHolder(homeName);
        Inventory inv = Bukkit.createInventory(holder, 9, "§8Gestion: " + homeName);
        holder.setInventory(inv);

        // Bloc vert (Téléportation)
        ItemStack tpItem = new ItemStack(Material.LIME_CONCRETE);
        ItemMeta tpMeta = tpItem.getItemMeta();
        tpMeta.setDisplayName("§a§lSe Téléporter");
        tpItem.setItemMeta(tpMeta);
        inv.setItem(2, tpItem);

        // Bloc rouge (Suppression)
        ItemStack delItem = new ItemStack(Material.RED_CONCRETE);
        ItemMeta delMeta = delItem.getItemMeta();
        delMeta.setDisplayName("§c§lSupprimer");
        delItem.setItemMeta(delMeta);
        inv.setItem(6, delItem);

        // Bouton retour
        ItemStack backItem = new ItemStack(Material.BARRIER);
        ItemMeta backMeta = backItem.getItemMeta();
        backMeta.setDisplayName("§e§lRetour aux Homes");
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
                    TeleportUtil.teleportWithCooldown(p, playerHomes.get(homeName), "le home " + homeName, "genscore.bypass.cooldown.home");
                }
            } else if (clicked.getType() == Material.RED_CONCRETE) {
                Map<String, Location> playerHomes = homes.get(p.getUniqueId());
                if (playerHomes != null) {
                    playerHomes.remove(homeName);
                    deleteHomeFromDB(p.getUniqueId(), homeName);
                    p.sendMessage("§cHome " + homeName + " supprimé.");
                }
                openHomeGui(p);
            } else if (clicked.getType() == Material.BARRIER) {
                openHomeGui(p);
            }
        }
    }
}
