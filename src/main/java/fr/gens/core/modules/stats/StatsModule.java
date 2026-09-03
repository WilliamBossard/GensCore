package fr.gens.core.modules.stats;

import fr.gens.core.CorePlugin;
import fr.gens.core.modules.Module;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.Statistic;
import org.bukkit.Material;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;


public class StatsModule implements Module, Listener {

    private final CorePlugin plugin;
    private boolean enabled;
    private com.tcoded.folialib.wrapper.task.WrappedTask task;
    private fr.gens.core.database.StatsDAO statsDAO;

    // Cache pour éviter de spammer la BDD
    private final Map<UUID, PlayerStats> statsCache = new ConcurrentHashMap<>();

    public StatsModule(CorePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "Stats";
    }

    @Override
    public String getDescription() {
        return "Traqueur de statistiques globales (Blocs, Mobs, Playtime)";
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    public fr.gens.core.database.StatsDAO getStatsDAO() {
        return statsDAO;
    }

    @Override
    public void initDatabase(fr.gens.core.utils.DatabaseManager dbManager) {
        dbManager.executeStatement("CREATE TABLE IF NOT EXISTS player_stats (uuid VARCHAR(36) PRIMARY KEY, discord_id VARCHAR(50));");
        dbManager.executeStatement("CREATE TABLE IF NOT EXISTS player_global_stats (uuid VARCHAR(36) PRIMARY KEY, blocks_broken INTEGER DEFAULT 0, mobs_killed INTEGER DEFAULT 0, playtime_minutes INTEGER DEFAULT 0, deaths INTEGER DEFAULT 0, player_kills INTEGER DEFAULT 0, last_updated BIGINT DEFAULT 0);");
        dbManager.executeStatement("CREATE INDEX IF NOT EXISTS idx_global_stats_uuid ON player_global_stats(uuid);");
    }

    @Override
    public void enable() {
        this.enabled = true;
        this.statsDAO = new fr.gens.core.database.StatsDAO(plugin);
        this.statsDAO.initDatabase();
        Bukkit.getPluginManager().registerEvents(this, plugin);

        // Tâche asynchrone toutes les minutes pour ajouter le playtime et sauvegarder le cache
        plugin.getFoliaLib().getScheduler().runTimerAsync((wrappedTask) -> {
            task = wrappedTask;
            for (Player p : Bukkit.getOnlinePlayers()) {
            if (p == null) continue;
                PlayerStats stats = getStats(p.getUniqueId());
                stats.playtimeMinutes += 1;
            }
            saveAllToDatabase();
        }, 1200L, 1200L);

        plugin.getLangManager().sendConsoleMessage("statsmodule.log_1");
    }

    @Override
    public void disable() {
        org.bukkit.event.HandlerList.unregisterAll(this);
        this.enabled = false;
        if (task != null) task.cancel();
        saveAllToDatabase();
        statsCache.clear();
        plugin.getLangManager().sendConsoleMessage("statsmodule.log_2");
    }

    public PlayerStats getStats(UUID uuid) {
        // Retourne toujours depuis le cache. L'initialisation se fait en asynchrone lors du join.
        // Si non présent (ex: l'événement join n'a pas encore fini de charger), on met des stats vides temporaires
        // qui seront fusionnées par la suite.
        return statsCache.computeIfAbsent(uuid, k -> new PlayerStats());
    }

    public PlayerStats getStatsIfCached(UUID uuid) {
        return statsCache.get(uuid);
    }

    private void saveAllToDatabase() {
        if (statsCache.isEmpty()) return;
        Map<UUID, PlayerStats> copy = new java.util.HashMap<>(statsCache);
        statsDAO.saveAllStats(copy);
    }

    private void saveAndRemovePlayerStats(UUID uuid) {
        PlayerStats stats = statsCache.remove(uuid);
        if (stats == null) return;
        statsDAO.savePlayerStats(uuid, stats);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (!enabled) return;
        PlayerStats stats = getStats(event.getPlayer().getUniqueId());
        stats.blocksBroken++;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        if (!enabled) return;
        if (event.getEntity().getKiller() != null) {
            Player killer = event.getEntity().getKiller();
            PlayerStats stats = getStats(killer.getUniqueId());
            if (event.getEntity() instanceof Player) {
                stats.playerKills++;
            } else {
                stats.mobsKilled++;
            }
        }
        
        if (event.getEntity() instanceof Player) {
            Player dead = (Player) event.getEntity();
            PlayerStats stats = getStats(dead.getUniqueId());
            stats.deaths++;
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!enabled) return;
        Player p = event.getPlayer();
        UUID uuid = p.getUniqueId();
        
        // Chargement asynchrone des stats via DAO
        statsDAO.loadPlayerStats(uuid).thenAccept(loadedStats -> {
            // Sync vanilla stats on the next tick to ensure we are on the main thread for Bukkit API calls
            plugin.getFoliaLib().getScheduler().runAtEntity(p, (syncTask) -> {
                if (!p.isOnline()) return;
                
                int vanillaMobs = p.getStatistic(Statistic.MOB_KILLS);
                int vanillaPlaytime = p.getStatistic(Statistic.PLAY_ONE_MINUTE) / 20 / 60;
                
                int vanillaBlocks = 0;
                for (Material m : Material.values()) {
                    if (m.isBlock()) {
                        try {
                            vanillaBlocks += p.getStatistic(Statistic.MINE_BLOCK, m);
                        } catch (IllegalArgumentException ignored) {} 
                    }
                }
        
                int vanillaDeaths = p.getStatistic(Statistic.DEATHS);
                int vanillaPlayerKills = p.getStatistic(Statistic.PLAYER_KILLS);
        
                loadedStats.blocksBroken = Math.max(loadedStats.blocksBroken, vanillaBlocks);
                loadedStats.mobsKilled = Math.max(loadedStats.mobsKilled, vanillaMobs);
                loadedStats.playtimeMinutes = Math.max(loadedStats.playtimeMinutes, vanillaPlaytime);
                loadedStats.deaths = Math.max(loadedStats.deaths, vanillaDeaths);
                loadedStats.playerKills = Math.max(loadedStats.playerKills, vanillaPlayerKills);

                // Si le joueur a miné pendant le chargement, on ajoute
                PlayerStats currentStats = statsCache.get(uuid);
                if (currentStats != null) {
                    loadedStats.blocksBroken += currentStats.blocksBroken;
                    loadedStats.mobsKilled += currentStats.mobsKilled;
                    loadedStats.deaths += currentStats.deaths;
                    loadedStats.playerKills += currentStats.playerKills;
                }
                statsCache.put(uuid, loadedStats);
            });
        });
    }

    @EventHandler
    public void onPlayerQuit(org.bukkit.event.player.PlayerQuitEvent event) {
        if (!enabled) return;
        saveAndRemovePlayerStats(event.getPlayer().getUniqueId());
    }

    public static class PlayerStats {
        public int blocksBroken = 0;
        public int mobsKilled = 0;
        public int playtimeMinutes = 0;
        public int deaths = 0;
        public int playerKills = 0;
    }
}





