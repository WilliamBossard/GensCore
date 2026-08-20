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
    
    private fr.gens.core.database.JobsDAO jobsDAO;

    public JobsModule(CorePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "Jobs";
    }

    @Override
    public String getDescription() {
        return "SystÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¨me de mÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â©tiers (Mineur, BÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â»cheron, Chasseur, Fermier).";
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    public fr.gens.core.database.JobsDAO getJobsDAO() {
        return jobsDAO;
    }

    @Override
    public void initDatabase(fr.gens.core.utils.DatabaseManager dbManager) {
        dbManager.executeStatement("CREATE TABLE IF NOT EXISTS player_jobs (uuid VARCHAR(36) NOT NULL, job_name VARCHAR(50) NOT NULL, level INT DEFAULT 1, xp DOUBLE DEFAULT 0, PRIMARY KEY (uuid, job_name));");
    }

    @Override
    public void enable() {
        this.enabled = true;
        
        this.jobsDAO = new fr.gens.core.database.JobsDAO(plugin);
        this.jobsDAO.initDatabase();
        
        this.gui = new JobsGUI(plugin, this);
        Bukkit.getPluginManager().registerEvents(this, plugin);
        Bukkit.getPluginManager().registerEvents(gui, plugin);
        org.bukkit.command.PluginCommand cmd_jobs = plugin.getCommand("jobs");
        if (cmd_jobs != null) cmd_jobs.setExecutor(this);
        
        // Charger les joueurs connectÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â©s
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p == null) continue;
            loadPlayer(p.getUniqueId());
        }
        
        // Auto-Save Task pour le WebPanel (Toutes les 10 secondes)
        plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            for (Player p : Bukkit.getOnlinePlayers()) {
            if (p == null) continue;
                savePlayer(p.getUniqueId());
            }
        }, 200L, 200L); // 10 secondes
        
        plugin.getLangManager().sendConsoleMessage("jobsmodule.log_1");
    }

    @Override
    public void disable() {
        org.bukkit.event.HandlerList.unregisterAll(this);
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
            this.jobsDAO.loadPlayerJobs(uuid, playerXp.get(uuid), playerLevel.get(uuid), activeJobs.get(uuid));
        });
    }
    
    public void savePlayer(UUID uuid) {
        if (!playerXp.containsKey(uuid)) return;
        this.jobsDAO.savePlayerJobs(uuid, playerXp.get(uuid), playerLevel.get(uuid), activeJobs.get(uuid));
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
            // On le supprime de la base de donnÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â©es
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                this.jobsDAO.removePlayerJob(uuid, type);
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
        
        if (currentXp >= nextXp) {
            currentXp -= nextXp;
            currentLevel++;
            playerLevel.get(uuid).put(type, currentLevel);

            player.sendMessage("<dark_gray>[<gold>MÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â©tiers<dark_gray>] <green>Vous passez niveau <white>" + currentLevel + " <green>dans le mÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â©tier " + type.getColor() + type.getDisplayName() + " <green>!");
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
            
            // RÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â©compenses de Level Up (Battle Pass)
            double reward = baseMoney * 10 * currentLevel; // Grosse rÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â©compense
            EconomyModule eco = (EconomyModule) plugin.getModuleManager().getModule("economy");
            if (eco != null && eco.isEnabled()) {
                eco.addMoney(uuid, reward);
                player.sendMessage("<dark_gray>[<gold>MÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â©tiers<dark_gray>] <gray>Vous avez reÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â§u <green>" + reward + "$ <gray>!");
            }
            
            // RÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â©compenses Objets selon le palier
            giveLevelUpItems(player, currentLevel);
        }
        
        playerXp.get(uuid).put(type, currentXp);
        
        // Action Bar Manager
        String formattedXp = String.format("%.1f", currentXp);
        String formattedNext = String.format("%.1f", nextXp);
        Component msg = Component.text("<dark_gray>[" + type.getColor() + type.getDisplayName() + "<dark_gray>] <white>" + formattedXp + " <gray>/ " + formattedNext + " XP");
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
        player.sendMessage("<dark_gray>[<gold>MÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â©tiers<dark_gray>] <gray>Vous avez reÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â§u <yellow>" + amount + "x " + mat.name() + " <gray>!");
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
            // IdÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â©alement vÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â©rifier si c'est mÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â»r, mais on simplifie
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


