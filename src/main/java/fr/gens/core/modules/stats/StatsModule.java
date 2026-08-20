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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;


public class StatsModule implements Module, Listener {

    private final CorePlugin plugin;
    private boolean enabled;
    private int taskId;
    private fr.gens.core.database.StatsDAO statsDAO;

    // Cache pour ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â©viter de spammer la BDD
    private final Map<UUID, PlayerStats> statsCache = new HashMap<>();

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

        // TÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢che asynchrone toutes les minutes pour ajouter le playtime et sauvegarder le cache
        taskId = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            for (Player p : Bukkit.getOnlinePlayers()) {
            if (p == null) continue;
                PlayerStats stats = getStats(p.getUniqueId());
                stats.playtimeMinutes += 1;
            }
            saveAllToDatabase();
        }, 1200L, 1200L).getTaskId();

        plugin.getLangManager().sendConsoleMessage("statsmodule.log_1");
    }

    @Override
    public void disable() {
        org.bukkit.event.HandlerList.unregisterAll(this);
        this.enabled = false;
        Bukkit.getScheduler().cancelTask(taskId);
        saveAllToDatabase();
        statsCache.clear();
        plugin.getLangManager().sendConsoleMessage("statsmodule.log_2");
    }

    public PlayerStats getStats(UUID uuid) {
        if (!statsCache.containsKey(uuid)) {
            // Charger depuis BDD
            PlayerStats stats = new PlayerStats();
            try (Connection conn = plugin.getDatabaseManager().getConnection();
                 PreparedStatement pstmt = conn.prepareStatement("SELECT blocks_broken, mobs_killed, playtime_minutes, deaths, player_kills FROM player_global_stats WHERE uuid = ?")) {
                pstmt.setString(1, uuid.toString());
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    stats.blocksBroken = rs.getInt("blocks_broken");
                    stats.mobsKilled = rs.getInt("mobs_killed");
                    stats.playtimeMinutes = rs.getInt("playtime_minutes");
                    stats.deaths = rs.getInt("deaths");
                    stats.playerKills = rs.getInt("player_kills");
                } else {
                    // Initialiser en BDD
                    try (PreparedStatement insert = conn.prepareStatement("INSERT INTO player_global_stats (uuid, blocks_broken, mobs_killed, playtime_minutes, deaths, player_kills) VALUES (?, 0, 0, 0, 0, 0)")) {
                        insert.setString(1, uuid.toString());
                        insert.executeUpdate();
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            statsCache.put(uuid, stats);
        }
        return statsCache.get(uuid);
    }

    private void saveAllToDatabase() {
        if (statsCache.isEmpty()) return;
        
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement pstmt = conn.prepareStatement("UPDATE player_global_stats SET blocks_broken = ?, mobs_killed = ?, playtime_minutes = ?, deaths = ?, player_kills = ?, last_updated = ? WHERE uuid = ?")) {
            
            conn.setAutoCommit(false);
            long now = System.currentTimeMillis();
            for (Map.Entry<UUID, PlayerStats> entry : statsCache.entrySet()) {
                pstmt.setInt(1, entry.getValue().blocksBroken);
                pstmt.setInt(2, entry.getValue().mobsKilled);
                pstmt.setInt(3, entry.getValue().playtimeMinutes);
                pstmt.setInt(4, entry.getValue().deaths);
                pstmt.setInt(5, entry.getValue().playerKills);
                pstmt.setLong(6, now);
                pstmt.setString(7, entry.getKey().toString());
                pstmt.addBatch();
            }
            pstmt.executeBatch();
            conn.commit();
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
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
        PlayerStats stats = getStats(p.getUniqueId());
        
        // Sync vanilla stats
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            int vanillaMobs = p.getStatistic(Statistic.MOB_KILLS);
            int vanillaPlaytime = p.getStatistic(Statistic.PLAY_ONE_MINUTE) / 20 / 60;
            
            int vanillaBlocks = 0;
            for (Material m : Material.values()) {
                if (m.isBlock()) {
                    try {
                        vanillaBlocks += p.getStatistic(Statistic.MINE_BLOCK, m);
                    } catch (IllegalArgumentException ignored) {} // Some materials can't be mined
                }
            }

            int vanillaDeaths = p.getStatistic(Statistic.DEATHS);
            int vanillaPlayerKills = p.getStatistic(Statistic.PLAYER_KILLS);

            stats.blocksBroken = Math.max(stats.blocksBroken, vanillaBlocks);
            stats.mobsKilled = Math.max(stats.mobsKilled, vanillaMobs);
            stats.playtimeMinutes = Math.max(stats.playtimeMinutes, vanillaPlaytime);
            stats.deaths = Math.max(stats.deaths, vanillaDeaths);
            stats.playerKills = Math.max(stats.playerKills, vanillaPlayerKills);
        });
    }

    public static class PlayerStats {
        public int blocksBroken = 0;
        public int mobsKilled = 0;
        public int playtimeMinutes = 0;
        public int deaths = 0;
        public int playerKills = 0;
    }
}


