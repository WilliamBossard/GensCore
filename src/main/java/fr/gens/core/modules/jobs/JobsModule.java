package fr.gens.core.modules.jobs;

import fr.gens.core.CorePlugin;
import fr.gens.core.modules.EconomyModule;
import fr.gens.core.modules.Module;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import net.kyori.adventure.text.Component;
import java.util.Random;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class JobsModule implements Module, Listener, CommandExecutor {

    private final CorePlugin plugin;
    private boolean enabled;
    private JobsGUI gui;
    
    // UUID -> (JobType -> XP)
    private final Map<UUID, Map<JobType, Double>> playerXp = new ConcurrentHashMap<>();
    private final Map<UUID, Map<JobType, Integer>> playerLevel = new ConcurrentHashMap<>();
    private final Map<UUID, Map<JobType, Boolean>> activeJobs = new ConcurrentHashMap<>();

    public JobsModule(CorePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "Jobs";
    }

    @Override
    public String getDescription() {
        return "Système de métiers (Mineur, Bûcheron, Chasseur, Fermier).";
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void enable() {
        this.enabled = true;
        this.gui = new JobsGUI(plugin, this);
        Bukkit.getPluginManager().registerEvents(this, plugin);
        Bukkit.getPluginManager().registerEvents(gui, plugin);
        plugin.getCommand("jobs").setExecutor(this);
        
        // Charger les joueurs connectés
        for (Player p : Bukkit.getOnlinePlayers()) {
            loadPlayer(p.getUniqueId());
        }
        
        // Auto-Save Task pour le WebPanel (Toutes les 10 secondes)
        plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            for (Player p : Bukkit.getOnlinePlayers()) {
                savePlayer(p.getUniqueId());
            }
        }, 200L, 200L); // 10 secondes
        
        plugin.getLangManager().sendConsoleMessage("jobsmodule.log_1");
    }

    @Override
    public void disable() {
        this.enabled = false;
        // Sauvegarder tout
        for (UUID uuid : playerXp.keySet()) {
            savePlayer(uuid);
        }
        playerXp.clear();
        playerLevel.clear();
        activeJobs.clear();
        plugin.getLangManager().sendConsoleMessage("jobsmodule.log_2");
    }
    
    public void loadPlayer(UUID uuid) {
        playerXp.put(uuid, new HashMap<>());
        playerLevel.put(uuid, new HashMap<>());
        activeJobs.put(uuid, new HashMap<>());
        
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection conn = plugin.getDatabaseManager().getConnection();
                 PreparedStatement ps = conn.prepareStatement("SELECT * FROM player_jobs WHERE uuid = ?")) {
                ps.setString(1, uuid.toString());
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    String jobName = rs.getString("job_name");
                    JobType type = JobType.valueOf(jobName);
                    double xp = rs.getDouble("xp");
                    int level = rs.getInt("level");
                    
                    playerXp.get(uuid).put(type, xp);
                    playerLevel.get(uuid).put(type, level);
                    // On considère qu'ils sont actifs si la ligne existe, mais on va juste charger l'XP pour l'instant.
                    // Ah, on doit stocker les jobs actifs.
                    // Si on a une ligne, le joueur a rejoint le job.
                    activeJobs.get(uuid).put(type, true);
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }
    
    public void savePlayer(UUID uuid) {
        if (!playerXp.containsKey(uuid)) return;
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO player_jobs (uuid, job_name, level, xp) VALUES (?, ?, ?, ?) " +
                     "ON CONFLICT(uuid, job_name) DO UPDATE SET level = excluded.level, xp = excluded.xp")) {
            for (Map.Entry<JobType, Double> entry : playerXp.get(uuid).entrySet()) {
                JobType type = entry.getKey();
                if (!activeJobs.get(uuid).getOrDefault(type, false)) continue;
                ps.setString(1, uuid.toString());
                ps.setString(2, type.name());
                ps.setInt(3, playerLevel.get(uuid).getOrDefault(type, 1));
                ps.setDouble(4, entry.getValue());
                ps.addBatch();
            }
            ps.executeBatch();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    public boolean hasJob(UUID uuid, JobType type) {
        return activeJobs.getOrDefault(uuid, new HashMap<>()).getOrDefault(type, false);
    }
    
    public int getActiveJobsCount(UUID uuid) {
        int count = 0;
        if (activeJobs.containsKey(uuid)) {
            for (boolean active : activeJobs.get(uuid).values()) {
                if (active) count++;
            }
        }
        return count;
    }
    
    public void joinJob(UUID uuid, JobType type) {
        activeJobs.computeIfAbsent(uuid, k -> new HashMap<>()).put(type, true);
        playerLevel.computeIfAbsent(uuid, k -> new HashMap<>()).putIfAbsent(type, 1);
        playerXp.computeIfAbsent(uuid, k -> new HashMap<>()).putIfAbsent(type, 0.0);
        
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> savePlayer(uuid));
    }
    
    public void leaveJob(UUID uuid, JobType type) {
        if (activeJobs.containsKey(uuid)) {
            activeJobs.get(uuid).put(type, false);
            // On le supprime de la base de données
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                try (Connection conn = plugin.getDatabaseManager().getConnection();
                     PreparedStatement ps = conn.prepareStatement("DELETE FROM player_jobs WHERE uuid = ? AND job_name = ?")) {
                    ps.setString(1, uuid.toString());
                    ps.setString(2, type.name());
                    ps.executeUpdate();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            });
            playerXp.get(uuid).remove(type);
            playerLevel.get(uuid).remove(type);
        }
    }
    
    public int getLevel(UUID uuid, JobType type) {
        return playerLevel.getOrDefault(uuid, new HashMap<>()).getOrDefault(type, 1);
    }
    
    public double getXp(UUID uuid, JobType type) {
        return playerXp.getOrDefault(uuid, new HashMap<>()).getOrDefault(type, 0.0);
    }
    
    public double getXpNeededForNextLevel(int currentLevel) {
        return currentLevel * 1000.0;
    }
    
    public void addXp(Player player, JobType type, double amount, double baseMoney) {
        if (!hasJob(player.getUniqueId(), type)) return;
        
        UUID uuid = player.getUniqueId();
        double currentXp = getXp(uuid, type);
        int currentLevel = getLevel(uuid, type);
        
        currentXp += amount;
        double nextXp = getXpNeededForNextLevel(currentLevel);
        
        boolean leveledUp = false;
        if (currentXp >= nextXp) {
            currentXp -= nextXp;
            currentLevel++;
            playerLevel.get(uuid).put(type, currentLevel);
            leveledUp = true;
            
            player.sendMessage("§8[§6Métiers§8] §aVous passez niveau §f" + currentLevel + " §adans le métier " + type.getColor() + type.getDisplayName() + " §a!");
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
            
            // Récompenses de Level Up (Battle Pass)
            double reward = baseMoney * 10 * currentLevel; // Grosse récompense
            EconomyModule eco = (EconomyModule) plugin.getModuleManager().getModule("economy");
            if (eco != null && eco.isEnabled()) {
                eco.addMoney(uuid, reward);
                player.sendMessage("§8[§6Métiers§8] §7Vous avez reçu §a" + reward + "$ §7!");
            }
            
            // Récompenses Objets selon le palier
            giveLevelUpItems(player, currentLevel);
        }
        
        playerXp.get(uuid).put(type, currentXp);
        
        // Action Bar Manager
        String formattedXp = String.format("%.1f", currentXp);
        String formattedNext = String.format("%.1f", nextXp);
        Component msg = Component.text("§8[" + type.getColor() + type.getDisplayName() + "§8] §f" + formattedXp + " §7/ " + formattedNext + " XP");
        plugin.getActionBarManager().sendMessage(player, "jobs", msg, 40);
    }
    
    private void giveLevelUpItems(Player player, int level) {
        Random r = new Random();
        int amount = r.nextInt(3) + 1;
        Material mat;
        if (level < 10) {
            mat = r.nextBoolean() ? Material.IRON_INGOT : Material.COAL;
        } else if (level < 20) {
            mat = r.nextBoolean() ? Material.GOLD_INGOT : Material.LAPIS_LAZULI;
        } else {
            mat = r.nextBoolean() ? Material.DIAMOND : Material.EMERALD;
        }
        player.getInventory().addItem(new ItemStack(mat, amount));
        player.sendMessage("§8[§6Métiers§8] §7Vous avez reçu §e" + amount + "x " + mat.name() + " §7!");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player && command.getName().equalsIgnoreCase("jobs")) {
            gui.openGUI((Player) sender);
            return true;
        }
        return false;
    }
    
    // --- EVENTS ---

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) {
        if (!enabled) return;
        loadPlayer(e.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent e) {
        if (!enabled) return;
        savePlayer(e.getPlayer().getUniqueId());
        playerXp.remove(e.getPlayer().getUniqueId());
        playerLevel.remove(e.getPlayer().getUniqueId());
        activeJobs.remove(e.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent e) {
        if (!enabled) return;
        Player p = e.getPlayer();
        Material m = e.getBlock().getType();
        
        if (m.name().endsWith("_ORE") || m == Material.STONE || m == Material.DEEPSLATE || m == Material.DIORITE || m == Material.GRANITE || m == Material.ANDESITE) {
            addXp(p, JobType.MINEUR, 1.5, 0.5);
        }
        else if (m.name().endsWith("_LOG") || m.name().endsWith("_WOOD")) {
            addXp(p, JobType.BUCHERON, 2.0, 0.7);
        }
        else if (m == Material.WHEAT || m == Material.CARROTS || m == Material.POTATOES || m == Material.BEETROOTS) {
            // Idéalement vérifier si c'est mûr, mais on simplifie
            addXp(p, JobType.FERMIER, 1.0, 0.3);
        }
    }
    
    @EventHandler
    public void onEntityDeath(EntityDeathEvent e) {
        if (!enabled) return;
        if (e.getEntity().getKiller() != null) {
            Player p = e.getEntity().getKiller();
            addXp(p, JobType.CHASSEUR, 5.0, 2.0);
        }
    }
}
